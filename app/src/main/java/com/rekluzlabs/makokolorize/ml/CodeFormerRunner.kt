package com.rekluzlabs.makokolorize.ml

import android.graphics.Bitmap
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import java.nio.FloatBuffer

class CodeFormerRunner(private val model: CodeFormerModel) {

    companion object {
        private const val MODEL_SIZE = 512
        private const val SIZE_SQ = MODEL_SIZE * MODEL_SIZE
    }

    /**
     * Runs CodeFormer on [bitmap], which should already be a face-aligned crop.
     * The caller (FaceBlender) is responsible for cropping, alignment, and
     * blending the result back into the full image.
     *
     * [fidelityWeight]: 0.0 = max enhancement (less faithful to input),
     *                   1.0 = max fidelity (closest to input).
     */
    fun process(bitmap: Bitmap, fidelityWeight: Float = 0.5f): Bitmap {
        val scaled = if (bitmap.width == MODEL_SIZE && bitmap.height == MODEL_SIZE) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, MODEL_SIZE, MODEL_SIZE, true)
        }

        val inputTensor = bitmapToTensor(scaled)
        return try {
            val (outputData, shape) = model.run(inputTensor, fidelityWeight)
            dataToBitmap(outputData, shape)
        } finally {
            inputTensor.close()
            if (scaled != bitmap) scaled.recycle()
        }
    }

    private fun bitmapToTensor(bitmap: Bitmap): OnnxTensor {
        val pixels = IntArray(SIZE_SQ)
        bitmap.getPixels(pixels, 0, MODEL_SIZE, 0, 0, MODEL_SIZE, MODEL_SIZE)

        val r = FloatArray(SIZE_SQ)
        val g = FloatArray(SIZE_SQ)
        val b = FloatArray(SIZE_SQ)

        for (i in pixels.indices) {
            val px = pixels[i]
            r[i] = ((px shr 16 and 0xFF) / 255f - 0.5f) / 0.5f
            g[i] = ((px shr 8  and 0xFF) / 255f - 0.5f) / 0.5f
            b[i] = ((px        and 0xFF) / 255f - 0.5f) / 0.5f
        }

        val floatBuf = FloatBuffer.allocate(3 * SIZE_SQ)
        floatBuf.put(r)
        floatBuf.put(g)
        floatBuf.put(b)
        floatBuf.rewind()

        return OnnxTensor.createTensor(
            OrtEnvironment.getEnvironment(),
            floatBuf,
            longArrayOf(1, 3, MODEL_SIZE.toLong(), MODEL_SIZE.toLong())
        )
    }

    private fun dataToBitmap(data: FloatArray, shape: LongArray): Bitmap {
        val pixels = IntArray(SIZE_SQ)

        // CodeFormer always outputs in [-1, 1] (same normalization as its input).
        // The previous runtime branch (checking min < -0.1f) was fragile — a bright
        // image (e.g., snow/white background) could produce a minimum near 0, causing
        // the decoder to apply the wrong formula and produce washed-out output.
        val isNHWC = shape.size >= 4 && shape[3] == 3L

        for (i in 0 until SIZE_SQ) {
            val rRaw: Float
            val gRaw: Float
            val bRaw: Float

            if (isNHWC) {
                rRaw = data[i * 3 + 0]
                gRaw = data[i * 3 + 1]
                bRaw = data[i * 3 + 2]
            } else {
                rRaw = data[i]
                gRaw = data[i + SIZE_SQ]
                bRaw = data[i + SIZE_SQ * 2]
            }

            val r = ((rRaw * 0.5f + 0.5f) * 255f).toInt().coerceIn(0, 255)
            val g = ((gRaw * 0.5f + 0.5f) * 255f).toInt().coerceIn(0, 255)
            val b = ((bRaw * 0.5f + 0.5f) * 255f).toInt().coerceIn(0, 255)

            pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        return Bitmap.createBitmap(pixels, MODEL_SIZE, MODEL_SIZE, Bitmap.Config.ARGB_8888)
    }
}
