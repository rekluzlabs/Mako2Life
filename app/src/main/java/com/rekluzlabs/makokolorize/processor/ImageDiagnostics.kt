/*
 * Copyright (c) 2026 Rekluz Labs
 * All rights reserved.
 */

package com.rekluzlabs.makokolorize.processor

import android.graphics.Bitmap
import kotlin.math.abs

enum class ColorCast {
    SEPIA,
    NEUTRAL,
    WARM,
    COOL
}

data class DiagnosticStats(
    val dominantCast: ColorCast,
    val averageSaturation: Float
)

object ImageDiagnostics {

    fun analyze(bitmap: Bitmap): DiagnosticStats {
        val sampleWidth = if (bitmap.width > bitmap.height) 120 else (120f * bitmap.width / bitmap.height).toInt()
        val sampleHeight = if (bitmap.height > bitmap.width) 120 else (120f * bitmap.height / bitmap.width).toInt()

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, sampleWidth, sampleHeight, false)
        val pixels = IntArray(sampleWidth * sampleHeight)
        scaledBitmap.getPixels(pixels, 0, sampleWidth, 0, 0, sampleWidth, sampleHeight)

        if (scaledBitmap != bitmap) scaledBitmap.recycle()

        var totalR = 0L; var totalG = 0L; var totalB = 0L
        var totalSat = 0f

        for (px in pixels) {
            val r = (px shr 16) and 0xFF
            val g = (px shr 8) and 0xFF
            val b = px and 0xFF
            totalR += r; totalG += g; totalB += b
            val max = maxOf(r, maxOf(g, b))
            val min = minOf(r, minOf(g, b))
            if (max > 0) totalSat += (max - min).toFloat() / max
        }

        val count = pixels.size.toFloat()
        val avgR = totalR / count
        val avgG = totalG / count
        val avgB = totalB / count
        val avgSaturation = totalSat / count

        val dominantCast = when {
            avgR > avgG + 15 && avgG > avgB + 20 && avgR in 60.0..230.0 -> ColorCast.SEPIA
            abs(avgR - avgG) < 8 && abs(avgG - avgB) < 8 && abs(avgR - avgB) < 8 -> ColorCast.NEUTRAL
            avgR > avgB + 10 -> ColorCast.WARM
            avgB > avgR + 10 -> ColorCast.COOL
            else -> ColorCast.NEUTRAL
        }

        return DiagnosticStats(dominantCast, avgSaturation)
    }
}
