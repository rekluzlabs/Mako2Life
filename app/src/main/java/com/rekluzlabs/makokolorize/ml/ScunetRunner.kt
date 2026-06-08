package com.rekluzlabs.makokolorize.ml

import android.graphics.Bitmap

class ScunetRunner(modelPath: String) : AutoCloseable {

    private val model = ScunetModel(modelPath)

    fun run(
        bitmap: Bitmap,
        strength: Float,
        advancedNoiseRemoval: Boolean,
        fastMode: Boolean = false,
        downsample: Boolean = false,
        onProgress: (Float) -> Unit = {}
    ): Bitmap {
        // Set tile config based on fastMode
        if (fastMode) {
            DenoiseRunner.setTileConfig(512, 32)
        } else {
            DenoiseRunner.setTileConfig(256, 16)
        }

        val input = if (downsample) {
            Bitmap.createScaledBitmap(bitmap, bitmap.width / 2, bitmap.height / 2, true)
        } else {
            bitmap
        }

        val hasSecondPass = advancedNoiseRemoval
        var result = runSinglePass(input, strength) { p ->
            onProgress(p * (if (hasSecondPass) 0.5f else 1f))
        }
        if (hasSecondPass) {
            result = runSinglePass(result, strength * 0.5f) { p ->
                onProgress(0.5f + p * 0.5f)
            }
        }

        return if (downsample) {
            val upscaled = Bitmap.createScaledBitmap(result, bitmap.width, bitmap.height, true)
            result.recycle()
            if (input != bitmap) input.recycle()
            upscaled
        } else {
            result
        }
    }

    private fun runSinglePass(
        bitmap: Bitmap,
        strength: Float,
        onProgress: (Float) -> Unit = {}
    ): Bitmap {
        val w = bitmap.width
        val h = bitmap.height

        val originalFloats = DenoiseRunner.bitmapToFloatChw(bitmap)

        val tileSize = DenoiseRunner.getTileSize()
        val overlap = DenoiseRunner.getTileOverlap()

        val outputFloats = FloatArray(3 * h * w)
        val weights = FloatArray(h * w)

        val xCoords = DenoiseRunner.tileCoords(w, tileSize, overlap)
        val yCoords = DenoiseRunner.tileCoords(h, tileSize, overlap)
        val totalTiles = xCoords.size * yCoords.size
        var tilesDone = 0

        for (tileY in yCoords) {
            for (tileX in xCoords) {
                // Extracted tile is GUARANTEED to be 3 * tileSize * tileSize
                val tileInput = DenoiseRunner.extractTilePadded(
                    originalFloats, w, h, tileX, tileY, tileSize
                )

                // Model processes a perfect tileSize x tileSize block
                val tileOutput = model.run(tileInput, tileSize, tileSize)

                // Blend the full tile, dropping pixels that fall outside image boundaries
                DenoiseRunner.blendTileFull(
                    outputFloats, weights, tileOutput,
                    w, h, tileX, tileY, tileSize
                )

                tilesDone++
                onProgress(tilesDone.toFloat() / totalTiles.toFloat())
            }
        }

        DenoiseRunner.normalizeByWeights(outputFloats, originalFloats, weights, w, h)

        val blended = if (strength < 1f) {
            DenoiseRunner.blendStrength(originalFloats, outputFloats, strength)
        } else {
            outputFloats
        }

        return DenoiseRunner.floatChwToBitmap(blended, w, h)
    }

    override fun close() {
        model.close()
    }
}
