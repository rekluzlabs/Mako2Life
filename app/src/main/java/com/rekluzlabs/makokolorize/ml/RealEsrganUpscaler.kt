/*
 * Copyright (c) 2026 Rekluz Labs
 * All rights reserved.
 */

package com.rekluzlabs.makokolorize.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File
import java.nio.FloatBuffer
import kotlin.math.ceil

class RealEsrganUpscaler(private val modelPath: String) : AutoCloseable {

    private var session: OrtSession? = null
    private val env = OrtEnvironment.getEnvironment()
    private var inputName: String = "input"
    private var modelInputSize: Int = -1

    fun loadModel() {
        if (session != null) return
        val opts = OrtSession.SessionOptions()
        val s = env.createSession(modelPath, opts)
        session = s
        inputName = s.inputNames?.firstOrNull() ?: "input"

        try {
            val info = s.inputInfo.values.first().info as ai.onnxruntime.TensorInfo
            val shape = info.shape
            if (shape.size >= 4 && shape[2] > 0) {
                modelInputSize = shape[2].toInt()
                android.util.Log.d("RealEsrgan", "Detected fixed input size: $modelInputSize")
            }
        } catch (e: Exception) {
            android.util.Log.w("RealEsrgan", "Could not detect model shape", e)
        }
    }

    /**
     * Upscales [input] to [requestedScale] using the RealESRGAN-x4 model.
     * If requestedScale is not 4, the x4 result will be resized to fit.
     */
    fun upscale(input: Bitmap, requestedScale: Int = 4): Bitmap {
        val s = session ?: error("Model not loaded")

        // If model has a fixed input size (e.g. 64), we must adjust our tiling
        val useTileSize = if (modelInputSize > 0) {
            (modelInputSize - 2 * OVERLAP).coerceAtLeast(16)
        } else {
            TILE_SIZE
        }
        val useOverlap = if (modelInputSize > 0) {
            (modelInputSize - useTileSize) / 2
        } else {
            OVERLAP
        }

        val internalOutW = input.width * MODEL_SCALE
        val internalOutH = input.height * MODEL_SCALE
        val fullOutputBitmap = Bitmap.createBitmap(internalOutW, internalOutH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(fullOutputBitmap)

        val cols = ceil(input.width.toDouble() / useTileSize).toInt()
        val rows = ceil(input.height.toDouble() / useTileSize).toInt()

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                var srcX = col * useTileSize - useOverlap
                var srcY = row * useTileSize - useOverlap
                
                if (modelInputSize > 0) {
                    if (srcX + modelInputSize > input.width) srcX = input.width - modelInputSize
                    if (srcY + modelInputSize > input.height) srcY = input.height - modelInputSize
                }
                
                val finalSrcX = srcX.coerceAtLeast(0)
                val finalSrcY = srcY.coerceAtLeast(0)
                val srcX2 = (finalSrcX + (if (modelInputSize > 0) modelInputSize else useTileSize + 2 * useOverlap)).coerceAtMost(input.width)
                val srcY2 = (finalSrcY + (if (modelInputSize > 0) modelInputSize else useTileSize + 2 * useOverlap)).coerceAtMost(input.height)

                val tileW = srcX2 - finalSrcX
                val tileH = srcY2 - finalSrcY
                if (tileW <= 0 || tileH <= 0) continue

                val tile = Bitmap.createBitmap(input, finalSrcX, finalSrcY, tileW, tileH)
                
                val processedTile = if (modelInputSize > 0 && (tileW != modelInputSize || tileH != modelInputSize)) {
                    val padded = Bitmap.createBitmap(modelInputSize, modelInputSize, Bitmap.Config.ARGB_8888)
                    val tc = Canvas(padded)
                    tc.drawBitmap(tile, 0f, 0f, null)
                    tile.recycle()
                    padded
                } else {
                    tile
                }

                val upscaledTile = runTile(s, processedTile)

                val activeLeft = col * useTileSize
                val activeTop  = row * useTileSize
                val activeRight = ((col + 1) * useTileSize).coerceAtMost(input.width)
                val activeBottom = ((row + 1) * useTileSize).coerceAtMost(input.height)

                val cropLeft = (activeLeft - finalSrcX) * MODEL_SCALE
                val cropTop  = (activeTop - finalSrcY) * MODEL_SCALE
                val cropW = (activeRight - activeLeft) * MODEL_SCALE
                val cropH = (activeBottom - activeTop) * MODEL_SCALE

                if (cropW > 0 && cropH > 0) {
                    val cropped = Bitmap.createBitmap(upscaledTile, cropLeft, cropTop, cropW, cropH)
                    canvas.drawBitmap(cropped, (activeLeft * MODEL_SCALE).toFloat(), (activeTop * MODEL_SCALE).toFloat(), null)
                    cropped.recycle()
                }

                processedTile.recycle()
                upscaledTile.recycle()
            }
        }

        return if (requestedScale != MODEL_SCALE) {
            val targetW = input.width * requestedScale
            val targetH = input.height * requestedScale
            Bitmap.createScaledBitmap(fullOutputBitmap, targetW, targetH, true).also {
                fullOutputBitmap.recycle()
            }
        } else {
            fullOutputBitmap
        }
    }

    private fun runTile(session: OrtSession, tile: Bitmap): Bitmap {
        val w = tile.width
        val h = tile.height
        val pixels = IntArray(w * h)
        tile.getPixels(pixels, 0, w, 0, 0, w, h)

        val tensorData = FloatArray(3 * h * w)
        for (i in pixels.indices) {
            val p = pixels[i]
            tensorData[i]             = ((p shr 16 and 0xFF) / 255f)  // R
            tensorData[h * w + i]     = ((p shr 8  and 0xFF) / 255f)  // G
            tensorData[2 * h * w + i] = ((p        and 0xFF) / 255f)  // B
        }

        val inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(tensorData), longArrayOf(1, 3, h.toLong(), w.toLong()))
        val results = session.run(mapOf(inputName to inputTensor))
        
        val outputTensor = results[0] as OnnxTensor
        val outputData = outputTensor.floatBuffer
        
        val outW = w * MODEL_SCALE
        val outH = h * MODEL_SCALE
        val outPixels = IntArray(outW * outH)
        
        val pixelsPerChannel = outW * outH
        
        for (y in 0 until outH) {
            for (x in 0 until outW) {
                val idx = y * outW + x
                val r = (outputData.get(idx) * 255f).coerceIn(0f, 255f).toInt()
                val g = (outputData.get(pixelsPerChannel + idx) * 255f).coerceIn(0f, 255f).toInt()
                val b = (outputData.get(2 * pixelsPerChannel + idx) * 255f).coerceIn(0f, 255f).toInt()
                outPixels[idx] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        inputTensor.close()
        results.close()

        return Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888).also {
            it.setPixels(outPixels, 0, outW, 0, 0, outW, outH)
        }
    }

    override fun close() {
        session?.close()
        session = null
    }

    companion object {
        private const val TILE_SIZE = 256
        private const val OVERLAP = 16
        private const val MODEL_SCALE = 4
    }
}
