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

    fun process(bitmap: Bitmap, fidelityWeight: Float = 0.5f): Bitmap {
        val scaled = if (bitmap.width == MODEL_SIZE && bitmap.height == MODEL_SIZE) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, MODEL_SIZE, MODEL_SIZE, true)
        }
        
        val inputTensor = bitmapToTensor(scaled)
        val (outputData, shape) = model.run(inputTensor, fidelityWeight)
        
        // Detect range
        val min = outputData.minOrNull() ?: 0f
        val isNormalizedNegOneToOne = min < -0.1f
        
        val outputBitmap = dataToBitmap(outputData, isNormalizedNegOneToOne, shape)
        
        inputTensor.close()
        if (scaled != bitmap) scaled.recycle()
        
        return if (outputBitmap.width == bitmap.width && outputBitmap.height == bitmap.height) {
            outputBitmap
        } else {
            Bitmap.createScaledBitmap(outputBitmap, bitmap.width, bitmap.height, true).also {
                outputBitmap.recycle()
            }
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
            // Standard CodeFormer expects RGB order normalized to [-1, 1]
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

    private fun dataToBitmap(data: FloatArray, isNormalizedNegOneToOne: Boolean, shape: LongArray): Bitmap {
        val pixels = IntArray(SIZE_SQ)
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
            
            val r: Int
            val g: Int
            val b: Int
            
            if (isNormalizedNegOneToOne) {
                r = ((rRaw * 0.5f + 0.5f) * 255f).toInt().coerceIn(0, 255)
                g = ((gRaw * 0.5f + 0.5f) * 255f).toInt().coerceIn(0, 255)
                b = ((bRaw * 0.5f + 0.5f) * 255f).toInt().coerceIn(0, 255)
            } else {
                r = (rRaw * 255f).toInt().coerceIn(0, 255)
                g = (gRaw * 255f).toInt().coerceIn(0, 255)
                b = (bRaw * 255f).toInt().coerceIn(0, 255)
            }

            pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        return Bitmap.createBitmap(pixels, MODEL_SIZE, MODEL_SIZE, Bitmap.Config.ARGB_8888)
    }
}
