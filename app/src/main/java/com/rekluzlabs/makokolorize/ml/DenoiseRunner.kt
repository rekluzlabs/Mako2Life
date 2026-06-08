package com.rekluzlabs.makokolorize.ml

import android.graphics.Bitmap

object DenoiseRunner {

    private var currentTileSize = 256
    private var currentOverlap = 16

    fun setTileConfig(size: Int, overlap: Int) {
        currentTileSize = size
        currentOverlap = overlap
    }

    fun getTileSize() = currentTileSize
    fun getTileOverlap() = currentOverlap

    fun bitmapToFloatChw(bitmap: Bitmap): FloatArray {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val floats = FloatArray(3 * h * w)
        for (i in 0 until h * w) {
            val px = pixels[i]
            floats[0 * h * w + i] = ((px shr 16) and 0xFF) / 255f
            floats[1 * h * w + i] = ((px shr 8) and 0xFF) / 255f
            floats[2 * h * w + i] = (px and 0xFF) / 255f
        }
        return floats
    }

    fun floatChwToBitmap(floats: FloatArray, width: Int, height: Int): Bitmap {
        val pixels = IntArray(width * height)
        for (i in 0 until width * height) {
            val r = (floats[0 * height * width + i].coerceIn(0f, 1f) * 255f + 0.5f).toInt()
            val g = (floats[1 * height * width + i].coerceIn(0f, 1f) * 255f + 0.5f).toInt()
            val b = (floats[2 * height * width + i].coerceIn(0f, 1f) * 255f + 0.5f).toInt()
            pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, width, 0, 0, width, height)
        return out
    }

    fun blendTileFull(
        output: FloatArray,
        weights: FloatArray,
        tileFloats: FloatArray,
        outW: Int,
        outH: Int,
        tileX: Int,
        tileY: Int,
        tileSize: Int
    ) {
        for (c in 0 until 3) {
            for (y in 0 until tileSize) {
                val dstY = tileY + y
                // If the tile extends past the bottom of the actual image, skip writing those pixels
                if (dstY < 0 || dstY >= outH) continue

                for (x in 0 until tileSize) {
                    val dstX = tileX + x
                    // If the tile extends past the right of the actual image, skip writing those pixels
                    if (dstX < 0 || dstX >= outW) continue

                    val wx = gaussianWeight(x, tileSize)
                    val wy = gaussianWeight(y, tileSize)
                    val w = (wx * wy).coerceAtLeast(0.001f) // Prevent absolute zero weights

                    val dstIdx = c * outH * outW + dstY * outW + dstX
                    val srcIdx = c * tileSize * tileSize + y * tileSize + x

                    output[dstIdx] += tileFloats[srcIdx] * w
                    if (c == 0) {
                        weights[dstY * outW + dstX] += w
                    }
                }
            }
        }
    }

    fun normalizeByWeights(output: FloatArray, original: FloatArray, weights: FloatArray, w: Int, h: Int) {
        for (i in 0 until w * h) {
            val wt = weights[i]
            if (wt > 0f) {
                for (c in 0 until 3) {
                    output[c * h * w + i] /= wt
                }
            } else {
                for (c in 0 until 3) {
                    output[c * h * w + i] = original[c * h * w + i]
                }
            }
        }
    }

    fun blendStrength(original: FloatArray, denoised: FloatArray, strength: Float): FloatArray {
        val result = FloatArray(original.size)
        for (i in original.indices) {
            result[i] = original[i] * (1f - strength) + denoised[i] * strength
        }
        return result
    }

    fun tileCoords(total: Int, tileSize: Int, overlap: Int): List<Int> {
        if (total <= tileSize) return listOf(0)

        val coords = mutableListOf<Int>()
        val step = tileSize - overlap
        var pos = 0

        while (pos < total) {
            coords.add(pos)
            if (pos + tileSize >= total) break
            pos += step
        }

        val finalEdgeCoord = total - tileSize
        if (!coords.contains(finalEdgeCoord) && finalEdgeCoord >= 0) {
            coords.add(finalEdgeCoord)
        }

        return coords.sorted()
    }

    fun extractTilePadded(
        srcFloats: FloatArray,
        srcW: Int,
        srcH: Int,
        tileX: Int,
        tileY: Int,
        tileSize: Int
    ): FloatArray {
        val tile = FloatArray(3 * tileSize * tileSize)
        for (c in 0 until 3) {
            for (y in 0 until tileSize) {
                val srcRow = (tileY + y).coerceIn(0, srcH - 1)
                for (x in 0 until tileSize) {
                    val srcCol = (tileX + x).coerceIn(0, srcW - 1)
                    tile[c * tileSize * tileSize + y * tileSize + x] =
                        srcFloats[c * srcH * srcW + srcRow * srcW + srcCol]
                }
            }
        }
        return tile
    }


    private fun gaussianWeight(pos: Int, size: Int): Float {
        val center = (size - 1) / 2f
        val sigma = size / 6f
        val d = (pos - center) / sigma
        return Math.exp(-0.5 * d * d).toFloat()
    }
}
