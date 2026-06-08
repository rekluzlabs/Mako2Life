package com.rekluzlabs.makokolorize.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import com.rekluzlabs.makokolorize.data.image.ImageRepository
import com.rekluzlabs.makokolorize.data.model.ModelRepository
import com.rekluzlabs.makokolorize.ml.CodeFormerModel
import com.rekluzlabs.makokolorize.ml.CodeFormerRunner
import com.rekluzlabs.makokolorize.ml.FaceBlender
import com.rekluzlabs.makokolorize.ml.RealEsrganUpscaler
import com.rekluzlabs.makokolorize.ml.ScunetRunner
import com.rekluzlabs.makokolorize.processor.ColorCast
import com.rekluzlabs.makokolorize.processor.ImageDiagnostics
import com.rekluzlabs.makokolorize.processor.LevelsMode
import com.rekluzlabs.makokolorize.processor.adjustSaturation
import com.rekluzlabs.makokolorize.processor.adjustWarmth
import com.rekluzlabs.makokolorize.processor.autoLevelsWithStrength
import com.rekluzlabs.makokolorize.util.ThermalMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer

class ColorizeUseCase(private val context: Context) {

    private val modelRepository = ModelRepository(context)

    private var ddcolorSession: OrtSession? = null
    private var ddcolorInputSize: Int = 256
    private var isDdcolorDynamic: Boolean = false
    private var ddcolorInputChannels: Int = 3
    private var ddcolorOutputChannels: Int = 2

    private var codeFormerModel: CodeFormerModel? = null
    private var realEsrganUpscaler: RealEsrganUpscaler? = null

    fun cleanup() {
        ddcolorSession?.close()
        ddcolorSession = null
        codeFormerModel?.close()
        codeFormerModel = null
        realEsrganUpscaler?.close()
        realEsrganUpscaler = null
    }

    suspend fun isModelReady(): Boolean =
        modelRepository.isScunetModelDownloaded() &&
                modelRepository.isCodeformerModelDownloaded() &&
                modelRepository.isRealEsrganDownloaded()

    suspend fun execute(
        bitmap: Bitmap,
        onProgress: (Float, String?) -> Unit,
        config: RunConfig = RunConfig()
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        val thermalMonitor = ThermalMonitor(context)
        
        try {
            ensureActive()
            onProgress(0f, "Starting...")

            Log.d("ColorizeUseCase", "=== Starting restoration pipeline ===")
            Log.d("ColorizeUseCase", "Input bitmap: ${bitmap.width}x${bitmap.height}")
            
            var currentBitmap = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)

            // Helper to check thermals and wait if needed
            val checkThermals = suspend { stage: String ->
                var state = thermalMonitor.getCurrentState()
                if (state == ThermalMonitor.ThermalState.CRITICAL) {
                    onProgress(-1f, "Device is too hot. Cooling down...")
                    Log.w("ColorizeUseCase", "Thermal CRITICAL before $stage. Pausing.")
                    while (thermalMonitor.getCurrentState() == ThermalMonitor.ThermalState.CRITICAL) {
                        delay(2000) // Wait 2 seconds and check again
                        ensureActive()
                    }
                    onProgress(0f, "Resuming $stage...")
                }
            }

            // 1. SCUNet Denoising
            if (config.denoisingEnabled) {
                checkThermals("Denoising")
                onProgress(0.1f, "Cleaning up noise (SCUNet)...")
                val runner = ScunetRunner(modelRepository.getScunetModelPath())
                val result = runner.use {
                    it.run(
                        currentBitmap,
                        config.scunetStrength,
                        config.scunetAdvancedNoiseRemoval,
                        config.scunetFastMode,
                        config.scunetDownsample
                    ) { _ -> }
                }
                if (currentBitmap != bitmap) currentBitmap.recycle()
                currentBitmap = result
            }
            ensureActive()

            // 2. CodeFormer Face Restore
            if (config.faceRestoreEnabled) {
                checkThermals("Face Restore")
                onProgress(0.3f, "Restoring faces (CodeFormer)...")
                if (codeFormerModel == null) {
                    codeFormerModel = CodeFormerModel(modelRepository.getCodeformerModelPath())
                }
                val runner = CodeFormerRunner(codeFormerModel!!)
                val blender = FaceBlender(runner)

                val faceRects = detectFaces(currentBitmap, config)
                Log.d("ColorizeUseCase", "Detected ${faceRects.size} face(s)")

                val result = blender.restoreFaces(
                    base            = currentBitmap,
                    faceRects       = faceRects,
                    fidelityWeight  = config.codeFormerFidelity,
                    maskDilation    = config.maskDilation,
                    upscaleFace     = config.codeFormerUpscaleFace
                )
                if (currentBitmap != bitmap) currentBitmap.recycle()
                currentBitmap = result
            }
            ensureActive()

            // 3. DDColor Colorization
            if (config.colorizeEnabled) {
                checkThermals("Colorization")
                onProgress(0.5f, "Colorizing...")
                if (ddcolorSession == null) loadDdcolorModel()
                
                // Only override if the model supports dynamic shapes
                if (isDdcolorDynamic) {
                    ddcolorInputSize = config.ddcolorInputSize
                } else {
                    Log.w("ColorizeUseCase", "Model is fixed-size ($ddcolorInputSize), ignoring user preference (${config.ddcolorInputSize})")
                }

                val grayInput = normalizeToGrayscale(currentBitmap)
                val output = runDdcolorInference(grayInput)
                val colorized = postProcess(output, grayInput, config.vibrancy)
                
                grayInput.recycle()
                if (currentBitmap != bitmap) currentBitmap.recycle()
                currentBitmap = colorized
            }
            ensureActive()

            // 4. RealESRGAN Upscaling
            if (config.upscalingEnabled) {
                checkThermals("Upscaling")
                onProgress(0.8f, "Upscaling ${config.upscaleScale}x (RealESRGAN)...")
                if (realEsrganUpscaler == null) {
                    realEsrganUpscaler = RealEsrganUpscaler(modelRepository.getRealEsrganPath())
                    realEsrganUpscaler!!.loadModel()
                }
                val upscaled = realEsrganUpscaler!!.upscale(currentBitmap, config.upscaleScale)
                if (currentBitmap != bitmap) currentBitmap.recycle()
                currentBitmap = upscaled
            }
            ensureActive()

            // 5. Final Post-processing
            onProgress(0.9f, "Finalizing image...")
            val finalOutputBitmap = withContext(Dispatchers.Default) {
                val stats = ImageDiagnostics.analyze(currentBitmap)

                var output = currentBitmap

                if (config.autoLevelsEnabled) {
                    val levelsMode = when {
                        stats.dominantCast == ColorCast.SEPIA -> LevelsMode.INDEPENDENT
                        stats.dominantCast == ColorCast.NEUTRAL -> LevelsMode.COMBINED
                        else -> LevelsMode.LUMINANCE
                    }
                    output = autoLevelsWithStrength(output, config.autoLevelsStrength, levelsMode)
                }

                if (config.saturationBoost != 1.0f) output = adjustSaturation(output, config.saturationBoost)
                if (config.warmthBias != 0.0f) output = adjustWarmth(output, config.warmthBias)

                output
            }
            if (currentBitmap != bitmap && currentBitmap != finalOutputBitmap) currentBitmap.recycle()
            currentBitmap = finalOutputBitmap

            onProgress(1f, "Finished")
            Log.d("ColorizeUseCase", "=== Restoration pipeline complete ===")
            Result.success(currentBitmap)
        } catch (e: OutOfMemoryError) {
            Log.e("ColorizeUseCase", "Out of memory", e)
            Result.failure(Exception("Device ran out of memory. Try a smaller image."))
        } catch (e: Exception) {
            Log.e("ColorizeUseCase", "Error during colorization", e)
            Result.failure(e)
        }
    }

    /**
     * Returns face bounding boxes in pixel coordinates of [bitmap] using ML Kit.
     */
    private suspend fun detectFaces(bitmap: Bitmap, config: RunConfig): List<RectF> =
        suspendCancellableCoroutine { continuation ->
            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(
                    if (config.mlKitAccurateMode) FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE
                    else FaceDetectorOptions.PERFORMANCE_MODE_FAST
                )
                .setMinFaceSize(config.mlKitMinFaceSize)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .build()

            val detector = FaceDetection.getClient(options)
            val image = InputImage.fromBitmap(bitmap, 0)

            detector.process(image)
                .addOnSuccessListener { faces ->
                    val result = faces.map { RectF(it.boundingBox) }
                    continuation.resume(result)
                }
                .addOnFailureListener { e ->
                    Log.e("ColorizeUseCase", "ML Kit Face detection failed", e)
                    continuation.resume(emptyList())
                }
            
            continuation.invokeOnCancellation {
                detector.close()
            }
        }

    // ── Model loading ────────────────────────────────────────────────────────────

    private fun loadDdcolorModel() {
        try {
            val env = OrtEnvironment.getEnvironment()
            val opts = OrtSession.SessionOptions()
            opts.setIntraOpNumThreads(4)
            ddcolorSession = env.createSession(modelRepository.getModelPath(), opts)

            val s = ddcolorSession!!
            val inputInfo = s.inputInfo
            val firstInput = inputInfo.values.first()
            val tensorInfo = firstInput.info as TensorInfo
            val shape = tensorInfo.shape

            if (shape.size >= 4) {
                val c = shape[1].toInt()
                if (c in 1..3) ddcolorInputChannels = c

                val sz = shape[2].toInt()
                if (sz <= 0) {
                    isDdcolorDynamic = true
                    Log.d("ColorizeUseCase", "DDColor detected dynamic input shape")
                } else if (sz in 32..2048) {
                    ddcolorInputSize = sz
                    isDdcolorDynamic = false
                    Log.d("ColorizeUseCase", "DDColor detected fixed input shape: $sz")
                }
            }

            val outputInfo = s.outputInfo
            val firstOutput = outputInfo.values.first()
            val outputTensorInfo = firstOutput.info as TensorInfo
            val outputShape = outputTensorInfo.shape
            if (outputShape.size >= 4) {
                val c = outputShape[1].toInt()
                if (c in 1..3) ddcolorOutputChannels = c
            }
        } catch (e: Exception) {
            Log.e("ColorizeUseCase", "Failed to load DDColor model", e)
            ddcolorSession?.close()
            ddcolorSession = null
            throw e
        }
    }

    // ── DDColor inference ────────────────────────────────────────────────────────

    private fun runDdcolorInference(bitmap: Bitmap): FloatArray {
        val s = ddcolorSession ?: throw IllegalStateException("DDColor model not loaded")
        val env = OrtEnvironment.getEnvironment()

        var inputSize = ddcolorInputSize
        if (inputSize <= 0) inputSize = 256

        val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val pixels = IntArray(inputSize * inputSize)
        resized.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        resized.recycle()

        val inputChannels = ddcolorInputChannels
        val inputData = FloatArray(inputChannels * inputSize * inputSize)

        for (i in pixels.indices) {
            val px = pixels[i]
            val r = (px shr 16) and 0xFF
            val g = (px shr 8) and 0xFF
            val b = px and 0xFF
            val gray = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
            for (c in 0 until inputChannels) {
                inputData[c * inputSize * inputSize + i] = gray
            }
        }

        val shape = longArrayOf(1L, inputChannels.toLong(), inputSize.toLong(), inputSize.toLong())
        val tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputData), shape)

        val inputName = s.inputNames?.first() ?: throw IllegalStateException("DDColor no input name")
        val result = try {
            s.run(mapOf(inputName to tensor))
        } catch (e: Exception) {
            tensor.close()
            throw e
        }
        val outputTensor = result.get(0) as OnnxTensor
        val buf = outputTensor.floatBuffer
        val out = FloatArray(buf.remaining())
        buf.get(out)

        tensor.close()
        result.close()

        return out
    }

    // ── Post-processing ──────────────────────────────────────────────────────────

    private fun postProcess(
        output: FloatArray,
        original: Bitmap,
        vibrancy: Float = 1.0f
    ): Bitmap {
        return if (ddcolorOutputChannels == 2) {
            postProcessLAB(output, original, vibrancy)
        } else {
            postProcessRGB(output, original)
        }
    }

    private fun postProcessLAB(
        output: FloatArray,
        original: Bitmap,
        vibrancy: Float = 1.0f
    ): Bitmap {
        val origWidth = original.width
        val origHeight = original.height
        var modelSize = ddcolorInputSize
        if (modelSize <= 0) modelSize = 256

        val pixelsPerChannel = modelSize * modelSize
        val aChannel = FloatArray(pixelsPerChannel)
        val bChannel = FloatArray(pixelsPerChannel)
        for (i in 0 until pixelsPerChannel) {
            aChannel[i] = output[i]
            bChannel[i] = output[pixelsPerChannel + i]
        }

        val origPixels = IntArray(origWidth * origHeight)
        original.getPixels(origPixels, 0, origWidth, 0, 0, origWidth, origHeight)

        val resultPixels = IntArray(origWidth * origHeight)

        for (y in 0 until origHeight) {
            for (x in 0 until origWidth) {
                val idx = y * origWidth + x
                val sx = (x.toFloat() * modelSize / origWidth).toInt().coerceIn(0, modelSize - 1)
                val sy = (y.toFloat() * modelSize / origHeight).toInt().coerceIn(0, modelSize - 1)
                val si = sy * modelSize + sx

                val px = origPixels[idx]
                val r8 = (px shr 16) and 0xFF
                val g8 = (px shr 8) and 0xFF
                val b8 = px and 0xFF

                val l = srgbToLabL(r8, g8, b8)
                val aPred = (aChannel[si] * vibrancy).coerceIn(-128f, 127f)
                val bPred = (bChannel[si] * vibrancy).coerceIn(-128f, 127f)

                resultPixels[idx] = labToSrgb(l, aPred.toDouble(), bPred.toDouble())
            }
        }

        return Bitmap.createBitmap(resultPixels, origWidth, origHeight, Bitmap.Config.ARGB_8888)
    }

    private fun srgbToLabL(r: Int, g: Int, b: Int): Double {
        val rr = srgbToLinear(r / 255.0)
        val gg = srgbToLinear(g / 255.0)
        val bb = srgbToLinear(b / 255.0)
        val x = 0.4124564 * rr + 0.3575761 * gg + 0.1804375 * bb
        val y = 0.2126729 * rr + 0.7151522 * gg + 0.0721750 * bb
        val z = 0.0193339 * rr + 0.1191920 * gg + 0.9503041 * bb
        val fy = labF(y / 1.0)
        return 116.0 * fy - 16.0
    }

    private fun labToSrgb(l: Double, a: Double, b: Double): Int {
        val fy = (l + 16.0) / 116.0
        val fx = a / 500.0 + fy
        val fz = fy - b / 200.0
        val x = 0.95047 * labFInv(fx)
        val y = 1.0 * labFInv(fy)
        val z = 1.08883 * labFInv(fz)
        val rr = linearToSrgb((3.2404542 * x - 1.5371385 * y - 0.4985314 * z).coerceIn(0.0, 1.0))
        val gg = linearToSrgb((-0.9692660 * x + 1.8760108 * y + 0.0415560 * z).coerceIn(0.0, 1.0))
        val bb = linearToSrgb((0.0556434 * x - 0.2040259 * y + 1.0572252 * z).coerceIn(0.0, 1.0))
        return (0xFF shl 24) or ((rr * 255.0 + 0.5).toInt().coerceIn(0, 255) shl 16) or
                ((gg * 255.0 + 0.5).toInt().coerceIn(0, 255) shl 8) or
                ((bb * 255.0 + 0.5).toInt().coerceIn(0, 255))
    }

    private fun srgbToLinear(c: Double) =
        if (c <= 0.04045) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)

    private fun linearToSrgb(c: Double) =
        if (c <= 0.0031308) 12.92 * c else 1.055 * Math.pow(c, 1.0 / 2.4) - 0.055

    private fun labF(t: Double): Double {
        val delta = 6.0 / 29.0
        return if (t > delta * delta * delta) Math.pow(t, 1.0 / 3.0) else t / (3.0 * delta * delta) + 4.0 / 29.0
    }

    private fun labFInv(t: Double): Double {
        val delta = 6.0 / 29.0
        return if (t > delta) t * t * t else 3.0 * delta * delta * (t - 4.0 / 29.0)
    }

    private fun postProcessRGB(output: FloatArray, original: Bitmap): Bitmap {
        val elementsPerChannel = output.size / 3
        val size = kotlin.math.sqrt(elementsPerChannel.toDouble()).toInt()
        val normalize = { value: Float -> (value * 255f).coerceIn(0f, 255f) }
        val pixels = IntArray(size * size)
        for (i in 0 until size * size) {
            val r = normalize(output[i]).toInt()
            val g = normalize(output[size * size + i]).toInt()
            val b = normalize(output[2 * size * size + i]).toInt()
            pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        val colorized = Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
        val result = Bitmap.createScaledBitmap(colorized, original.width, original.height, true)
        colorized.recycle()
        return result
    }

    private fun normalizeToGrayscale(bitmap: Bitmap): Bitmap {
        val grayscale = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(grayscale)
        val paint = Paint()
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return grayscale
    }
}
