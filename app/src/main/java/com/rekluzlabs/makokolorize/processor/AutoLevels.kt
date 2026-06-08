/*
 * Copyright (c) 2026 Rekluz Labs
 * All rights reserved.
 */

package com.rekluzlabs.makokolorize.processor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

enum class LevelsMode {
    INDEPENDENT,
    LUMINANCE,
    COMBINED
}

fun autoLevels(bitmap: Bitmap, clipPercent: Float = 0.01f, mode: LevelsMode = LevelsMode.INDEPENDENT): Bitmap {
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

    val histR = IntArray(256)
    val histG = IntArray(256)
    val histB = IntArray(256)
    val histL = IntArray(256)

    for (px in pixels) {
        val r = (px shr 16) and 0xFF
        val g = (px shr 8) and 0xFF
        val b = px and 0xFF
        histR[r]++; histG[g]++; histB[b]++
        if (mode == LevelsMode.LUMINANCE) {
            val lum = (0.299f * r + 0.587f * g + 0.114f * b).toInt().coerceIn(0, 255)
            histL[lum]++
        }
    }

    val clipCount = (pixels.size * clipPercent).toInt()

    fun findMin(hist: IntArray): Int {
        var count = 0
        for (i in 0..255) { count += hist[i]; if (count > clipCount) return i }
        return 0
    }

    fun findMax(hist: IntArray): Int {
        var count = 0
        for (i in 255 downTo 0) { count += hist[i]; if (count > clipCount) return i }
        return 255
    }

    val resultLimits = when (mode) {
        LevelsMode.INDEPENDENT -> arrayOf(
            findMin(histR), findMax(histR),
            findMin(histG), findMax(histG),
            findMin(histB), findMax(histB)
        )
        LevelsMode.COMBINED -> {
            val combinedHist = IntArray(256) { i -> histR[i] + histG[i] + histB[i] }
            val min = findMin(combinedHist); val max = findMax(combinedHist)
            arrayOf(min, max, min, max, min, max)
        }
        LevelsMode.LUMINANCE -> {
            val min = findMin(histL); val max = findMax(histL)
            arrayOf(min, max, min, max, min, max)
        }
    }

    val minR = resultLimits[0]; val maxR = resultLimits[1]
    val minG = resultLimits[2]; val maxG = resultLimits[3]
    val minB = resultLimits[4]; val maxB = resultLimits[5]

    val rangeR = (maxR - minR).takeIf { it > 0 } ?: 1
    val rangeG = (maxG - minG).takeIf { it > 0 } ?: 1
    val rangeB = (maxB - minB).takeIf { it > 0 } ?: 1

    val lutR = IntArray(256) { i -> ((i - minR) * 255f / rangeR).toInt().coerceIn(0, 255) }
    val lutG = IntArray(256) { i -> ((i - minG) * 255f / rangeG).toInt().coerceIn(0, 255) }
    val lutB = IntArray(256) { i -> ((i - minB) * 255f / rangeB).toInt().coerceIn(0, 255) }

    for (i in pixels.indices) {
        val a = (pixels[i] shr 24) and 0xFF
        val r = lutR[(pixels[i] shr 16) and 0xFF]
        val g = lutG[(pixels[i] shr 8) and 0xFF]
        val b = lutB[pixels[i] and 0xFF]
        pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    result.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    return result
}

fun autoLevelsWithStrength(bitmap: Bitmap, strength: Float, mode: LevelsMode): Bitmap {
    val stretched = autoLevels(bitmap, mode = mode)
    if (strength >= 1.0f) return stretched
    val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    canvas.drawBitmap(bitmap, 0f, 0f, null)
    val paint = Paint().apply { alpha = (strength * 255).toInt() }
    canvas.drawBitmap(stretched, 0f, 0f, paint)
    return result
}

fun adjustSaturation(bitmap: Bitmap, saturation: Float): Bitmap {
    if (saturation == 1.0f) return bitmap
    val paint = Paint().apply {
        colorFilter = ColorMatrixColorFilter(ColorMatrix().also { it.setSaturation(saturation) })
    }
    val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    Canvas(result).drawBitmap(bitmap, 0f, 0f, paint)
    return result
}

fun adjustWarmth(bitmap: Bitmap, warmth: Float): Bitmap {
    if (warmth == 0.0f) return bitmap
    val rBoost = (warmth * 20).toInt().coerceIn(-40, 40)
    val bBoost = (-warmth * 15).toInt().coerceIn(-30, 30)
    val matrix = ColorMatrix(floatArrayOf(
        1f, 0f, 0f, 0f, rBoost.toFloat(),
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, bBoost.toFloat(),
        0f, 0f, 0f, 1f, 0f
    ))
    val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) }
    val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    Canvas(result).drawBitmap(bitmap, 0f, 0f, paint)
    return result
}
