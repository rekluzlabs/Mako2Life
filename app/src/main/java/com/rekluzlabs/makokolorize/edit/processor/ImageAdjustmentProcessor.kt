/*
 * Copyright (c) 2026 Rekluz Labs
 * All rights reserved.
 */

package com.rekluzlabs.makokolorize.edit.processor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.rekluzlabs.makokolorize.edit.model.EditorAdjustments

class ImageAdjustmentProcessor {

    fun applyAdjustments(source: Bitmap, adj: EditorAdjustments): Bitmap {
        val cropped = applyCrop(source, adj.cropRect)
        var result = applyColorMatrix(cropped, adj)
        result = applyToneCurve(result, adj.highlights, adj.shadows)
        if (adj.sharpness > 0f) result = applySharpen(result, adj.sharpness)
        return result
    }

    private fun applyCrop(source: Bitmap, crop: com.rekluzlabs.makokolorize.edit.model.CropRect): Bitmap {
        if (crop.isDefault()) return source

        val left = (crop.left * source.width).toInt().coerceIn(0, source.width - 1)
        val top = (crop.top * source.height).toInt().coerceIn(0, source.height - 1)
        val right = (crop.right * source.width).toInt().coerceIn(left + 1, source.width)
        val bottom = (crop.bottom * source.height).toInt().coerceIn(top + 1, source.height)

        return Bitmap.createBitmap(source, left, top, right - left, bottom - top)
    }

    private fun applyColorMatrix(source: Bitmap, adj: EditorAdjustments): Bitmap {
        val b = adj.brightness * 255f
        val c = adj.contrast
        val translate = 0.5f * (1f - c) * 255f + b

        val matrix = ColorMatrix(floatArrayOf(
            c,  0f, 0f, 0f, translate,
            0f,  c, 0f, 0f, translate,
            0f, 0f,  c, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))

        matrix.postConcat(ColorMatrix().also { it.setSaturation(adj.saturation) })

        val rShift = adj.warmth * 25f
        val bShift = -adj.warmth * 20f
        val gShift = adj.tint * 15f

        matrix.postConcat(ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, rShift,
            0f, 1f, 0f, 0f, gShift,
            0f, 0f, 1f, 0f, bShift,
            0f, 0f, 0f, 1f, 0f
        )))

        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        Canvas(result).drawBitmap(source, 0f, 0f, Paint().apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        })
        return result
    }

    private fun applyToneCurve(source: Bitmap, highlights: Float, shadows: Float): Bitmap {
        if (highlights == 0f && shadows == 0f) return source

        val lut = IntArray(256) { i ->
            val f = i / 255f
            val lifted = f + shadows * (1f - f) * (1f - f)
            val recovered = lifted - highlights * lifted * lifted
            (recovered.coerceIn(0f, 1f) * 255f).toInt()
        }

        val w = source.width
        val h = source.height
        val result = source.copy(source.config ?: Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = lut[(p shr 16) and 0xFF]
            val g = lut[(p shr 8) and 0xFF]
            val b = lut[p and 0xFF]
            pixels[i] = -0x1000000 or (r shl 16) or (g shl 8) or b
        }

        result.setPixels(pixels, 0, w, 0, 0, w, h)
        return result
    }

    private fun applySharpen(source: Bitmap, strength: Float): Bitmap {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        val out = IntArray(w * h)

        // Optimized sharpening loop - avoids list creation and redundant shifts
        for (y in 1 until h - 1) {
            val row = y * w
            val prevRow = row - w
            val nextRow = row + w
            for (x in 1 until w - 1) {
                val idx = row + x
                val center = pixels[idx]
                
                // Direct access instead of list/iterator
                val n1 = pixels[prevRow + x]
                val n2 = pixels[nextRow + x]
                val n3 = pixels[row + x - 1]
                val n4 = pixels[row + x + 1]

                // Process R, G, B separately with bit masking
                // Center * (1 + 4*strength) - (Neighbors Sum) * strength
                val boost = 1f + 4f * strength
                
                fun sharpenChannel(shift: Int): Int {
                    val c = (center shr shift) and 0xFF
                    val neighborsAvg = ((n1 shr shift) and 0xFF) + 
                                     ((n2 shr shift) and 0xFF) + 
                                     ((n3 shr shift) and 0xFF) + 
                                     ((n4 shr shift) and 0xFF)
                    return (c * boost - (neighborsAvg * strength)).toInt().coerceIn(0, 255)
                }

                out[idx] = -0x1000000 or (sharpenChannel(16) shl 16) or (sharpenChannel(8) shl 8) or sharpenChannel(0)
            }
        }

        val result = source.copy(source.config ?: Bitmap.Config.ARGB_8888, true)
        result.setPixels(out, 0, w, 0, 0, w, h)
        return result
    }
}
