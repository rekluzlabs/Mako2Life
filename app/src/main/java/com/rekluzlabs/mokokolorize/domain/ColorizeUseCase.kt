package com.rekluzlabs.makokolorize.domain

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import com.rekluzlabs.makokolorize.data.model.ModelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer

class ColorizeUseCase(context: Context) {

    private val modelRepository = ModelRepository(context)
    private var session: OrtSession? = null
    private var modelInputSize: Int = 256
    private var modelInputChannels: Int = 3
    private var modelOutputChannels: Int = 2

    suspend fun isModelReady(): Boolean = modelRepository.isModelDownloaded()

    suspend fun execute(
        bitmap: Bitmap,
        onProgress: (Float) -> Unit,
        vibrancy: Float = 1.0f
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            ensureActive()
            onProgress(0f)

            Log.d("ColorizeUseCase", "=== Starting colorization ===")
            Log.d("ColorizeUseCase", "Input bitmap: ${bitmap.width}x${bitmap.height}")

            if (session == null) loadModel()
            ensureActive()
            onProgress(0.2f)

            val output = runInference(bitmap)
            ensureActive()
            onProgress(0.8f)

            val result = postProcess(output, bitmap, vibrancy)
            onProgress(1f)

            Log.d("ColorizeUseCase", "=== Colorization complete ===")
            Result.success(result)
        } catch (e: OutOfMemoryError) {
            Log.e("ColorizeUseCase", "Out of memory", e)
            Result.failure(Exception("Device ran out of memory. Try a smaller image."))
        } catch (e: Exception) {
            Log.e("ColorizeUseCase", "Error during colorization", e)
            Result.failure(e)
        }
    }

    private fun loadModel() {
        try {
            val env = OrtEnvironment.getEnvironment()
            val opts = OrtSession.SessionOptions()
            opts.setIntraOpNumThreads(4)
            session = env.createSession(modelRepository.getModelPath(), opts)

            val inputInfo = session!!.inputInfo
            val firstInput = inputInfo.values.first()
            val tensorInfo = firstInput.info as TensorInfo
            val shape = tensorInfo.shape

            if (shape.size >= 4) {
                val c = shape[1].toInt()
                if (c in 1..3) modelInputChannels = c
                else Log.w("ColorizeUseCase", "Unexpected input channels: $c, keeping default $modelInputChannels")

                val s = shape[2].toInt()
                if (s in 32..2048) modelInputSize = s
                else Log.w("ColorizeUseCase", "Unexpected input size: $s (dynamic?), keeping default $modelInputSize")
            }

            val outputInfo = session!!.outputInfo
            val firstOutput = outputInfo.values.first()
            val outputTensorInfo = firstOutput.info as TensorInfo
            val outputShape = outputTensorInfo.shape
            if (outputShape.size >= 4) {
                val c = outputShape[1].toInt()
                if (c in 1..3) modelOutputChannels = c
                else Log.w("ColorizeUseCase", "Unexpected output channels: $c, keeping default $modelOutputChannels")
            }

            Log.d("ColorizeUseCase", "Model loaded. Input: ${shape.contentToString()}, Output: ${outputShape.contentToString()}")
            Log.d("ColorizeUseCase", "Using input size=$modelInputSize, inputChannels=$modelInputChannels, outputChannels=$modelOutputChannels")
        } catch (e: Exception) {
            Log.e("ColorizeUseCase", "Failed to load model", e)
            session?.close()
            session = null
            throw e
        }
    }

    private fun runInference(bitmap: Bitmap): FloatArray {
        val s = session ?: throw IllegalStateException("Model not loaded")
        val env = OrtEnvironment.getEnvironment()

        var inputSize = modelInputSize
        if (inputSize <= 0 || inputSize > 2048) {
            Log.w("ColorizeUseCase", "Invalid model input size: $inputSize, defaulting to 256")
            inputSize = 256
        }
        Log.d("ColorizeUseCase", "Resizing to model input size: ${inputSize}x${inputSize}")

        val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

        val pixels = IntArray(inputSize * inputSize)
        resized.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        resized.recycle()

        val inputChannels = modelInputChannels
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

        Log.d("ColorizeUseCase", "Input range: min=${inputData.minOrNull()}, max=${inputData.maxOrNull()}")

        val shape = longArrayOf(1L, inputChannels.toLong(), inputSize.toLong(), inputSize.toLong())

        Log.d("ColorizeUseCase", "Creating input tensor with shape ${shape.contentToString()}...")
        val tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputData), shape)

        Log.d("ColorizeUseCase", "Running inference...")

        val inputName = s.inputNames?.first() ?: throw IllegalStateException("No input name found")
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

        Log.d("ColorizeUseCase", "Output size: ${out.size}, expected channels: $modelOutputChannels")

        tensor.close()
        result.close()

        return out
    }

    private fun postProcess(output: FloatArray, original: Bitmap, vibrancy: Float = 1.0f): Bitmap {
        Log.d("ColorizeUseCase", "=== POST PROCESS === vibrancy=$vibrancy")
        Log.d("ColorizeUseCase", "Output array size: ${output.size}, channels: $modelOutputChannels")

        return if (modelOutputChannels == 2) {
            postProcessLAB(output, original, vibrancy)
        } else {
            postProcessRGB(output, original)
        }
    }

    private fun postProcessLAB(output: FloatArray, original: Bitmap, vibrancy: Float = 1.0f): Bitmap {
        val origWidth = original.width
        val origHeight = original.height
        var modelSize = modelInputSize
        if (modelSize <= 0) modelSize = 256

        val pixelsPerChannel = modelSize * modelSize
        if (output.size < pixelsPerChannel * 2) {
            Log.e("ColorizeUseCase", "Output too small: ${output.size} < ${pixelsPerChannel * 2}, fallback to RGB")
            return postProcessRGB(output, original)
        }

        val aChannel = FloatArray(pixelsPerChannel)
        val bChannel = FloatArray(pixelsPerChannel)
        for (i in 0 until pixelsPerChannel) {
            aChannel[i] = output[i]
            bChannel[i] = output[pixelsPerChannel + i]
        }

        val totalPixels = origWidth.toLong() * origHeight.toLong()
        val maxPixels = 4_000_000L
        val scaleFactor = if (totalPixels > maxPixels) {
            kotlin.math.sqrt(maxPixels.toDouble() / totalPixels).toFloat()
        } else 1f

        val procWidth: Int
        val procHeight: Int
        val useScaled: Bitmap
        if (scaleFactor < 1f) {
            procWidth = (origWidth * scaleFactor).toInt().coerceAtLeast(256)
            procHeight = (origHeight * scaleFactor).toInt().coerceAtLeast(256)
            useScaled = Bitmap.createScaledBitmap(original, procWidth, procHeight, true)
            Log.d("ColorizeUseCase", "Downscaling post-process from ${origWidth}x${origHeight} to ${procWidth}x${procHeight}")
        } else {
            procWidth = origWidth
            procHeight = origHeight
            useScaled = original
        }

        val origPixels = IntArray(procWidth * procHeight)
        useScaled.getPixels(origPixels, 0, procWidth, 0, 0, procWidth, procHeight)
        if (useScaled != original) useScaled.recycle()

        val resultPixels = IntArray(procWidth * procHeight)

        for (y in 0 until procHeight) {
            for (x in 0 until procWidth) {
                val idx = y * procWidth + x

                val sx = (x.toFloat() * modelSize / procWidth).toInt().coerceIn(0, modelSize - 1)
                val sy = (y.toFloat() * modelSize / procHeight).toInt().coerceIn(0, modelSize - 1)
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

        val colorized = Bitmap.createBitmap(resultPixels, procWidth, procHeight, Bitmap.Config.ARGB_8888)
        val finalResult = if (procWidth != origWidth || procHeight != origHeight) {
            Log.d("ColorizeUseCase", "Upscaling result back to ${origWidth}x${origHeight}")
            Bitmap.createScaledBitmap(colorized, origWidth, origHeight, true).also { colorized.recycle() }
        } else colorized
        return finalResult
    }

    private fun srgbToLabL(r: Int, g: Int, b: Int): Double {
        val rr = srgbToLinear(r / 255.0)
        val gg = srgbToLinear(g / 255.0)
        val bb = srgbToLinear(b / 255.0)

        val x = 0.4124564 * rr + 0.3575761 * gg + 0.1804375 * bb
        val y = 0.2126729 * rr + 0.7151522 * gg + 0.0721750 * bb
        val z = 0.0193339 * rr + 0.1191920 * gg + 0.9503041 * bb

        val fx = labF(x / 0.95047)
        val fy = labF(y / 1.0)
        val fz = labF(z / 1.08883)

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

    private fun srgbToLinear(c: Double): Double {
        return if (c <= 0.04045) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
    }

    private fun linearToSrgb(c: Double): Double {
        return if (c <= 0.0031308) 12.92 * c else 1.055 * Math.pow(c, 1.0 / 2.4) - 0.055
    }

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

        Log.d("ColorizeUseCase", "RGB post-process, detected size=$size")

        val outputMax = output.maxOrNull() ?: 1f
        val outputMin = output.minOrNull() ?: 0f
        val outputRange = outputMax - outputMin

        val normalize = { value: Float ->
            if (outputRange > 0.001f) {
                ((value - outputMin) / outputRange * 255f).coerceIn(0f, 255f)
            } else {
                128f
            }
        }

        val pixels = IntArray(size * size)
        for (i in 0 until size * size) {
            val r = normalize(output[i]).toInt()
            val g = normalize(output[size * size + i]).toInt()
            val b = normalize(output[2 * size * size + i]).toInt()
            pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        val colorized = Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)

        val result = if (colorized.width != original.width || colorized.height != original.height) {
            Log.d("ColorizeUseCase", "Upscaling to ${original.width}x${original.height}")
            Bitmap.createScaledBitmap(colorized, original.width, original.height, true)
        } else {
            colorized
        }

        colorized.recycle()
        return result
    }
}
