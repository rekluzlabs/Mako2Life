/*
 * Copyright (c) 2026 Rekluz Labs
 * All rights reserved.
 */

package com.rekluzlabs.makokolorize.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log

/**
 * Handles the crop → restore → blend loop for CodeFormer.
 *
 * For each detected face rect:
 *   1. Crop the face patch from the full image (with optional dilation padding).
 *   2. Run CodeFormer on the 512×512 crop.
 *   3. Build a soft blend mask with eroded/dilated edges.
 *   4. Composite the restored patch back onto the full image.
 *
 * This is the piece that was previously missing: CodeFormerRunner was being
 * called on the entire image, which gave CodeFormer a tiny distorted face with
 * no alignment and skipped the blend-back step entirely.
 *
 * @param runner  A CodeFormerRunner wrapping the loaded CodeFormerModel.
 */
class FaceBlender(private val runner: CodeFormerRunner) {

    companion object {
        private const val TAG = "FaceBlender"
    }

    /**
     * @param base           Full-resolution image to restore faces in.
     * @param faceRects      Bounding boxes from the face detector, in pixel coords of [base].
     * @param fidelityWeight CodeFormer fidelity (0 = max enhance, 1 = max faithful).
     * @param maskDilation   Positive = expand the blend region (softer edge into hair/bg).
     *                       Negative = shrink (erosion, tighter edge). Range [-20, 20].
     * @param upscaleFace    If true, uses higher quality interpolation when blending back.
     * @return               A new bitmap with all detected faces restored and blended.
     */
    fun restoreFaces(
        base: Bitmap,
        faceRects: List<RectF>,
        fidelityWeight: Float = 0.5f,
        maskDilation: Int = 0,
        upscaleFace: Boolean = false
    ): Bitmap {
        if (faceRects.isEmpty()) {
            Log.d(TAG, "No faces detected, returning original")
            return base.copy(base.config ?: Bitmap.Config.ARGB_8888, false)
        }

        Log.d(TAG, "Restoring ${faceRects.size} face(s), fidelity=$fidelityWeight, dilation=$maskDilation, upscale=$upscaleFace")

        val result = base.copy(Bitmap.Config.ARGB_8888, true)

        // In batch mode: collect all (patchRect, restoredBitmap) first, then composite.
        // In sequential mode: composite each patch immediately onto the running result.
        val patches: List<Pair<Rect, Bitmap>> = faceRects.mapIndexed { i, rawRect ->
            val dilated = dilateRect(rawRect, maskDilation, base.width, base.height)
            val crop = cropBitmap(base, dilated)

            Log.d(TAG, "Face $i: raw=${rawRect.toShortString()}, patch=${dilated}, crop=${crop.width}x${crop.height}")

            val restored = runner.process(crop, fidelityWeight)
            crop.recycle()

            Pair(dilated, restored)
        }

        patches.forEach { (patchRect, restored) ->
            blendPatch(result, restored, patchRect, maskDilation)
            restored.recycle()
        }

        return result
    }

    // ── Private helpers ─────────────────────────────────────────────────────────

    /**
     * Expand or contract [rect] by [dilation] pixels on all sides, clamped to the image bounds.
     * Positive dilation → larger patch (softer blend into background/hair).
     * Negative dilation (erosion) → tighter patch (sharper boundary, less background).
     */
    private fun dilateRect(rect: RectF, dilation: Int, imageW: Int, imageH: Int): Rect {
        val d = dilation.toFloat()
        return Rect(
            (rect.left   - d).toInt().coerceAtLeast(0),
            (rect.top    - d).toInt().coerceAtLeast(0),
            (rect.right  + d).toInt().coerceAtMost(imageW),
            (rect.bottom + d).toInt().coerceAtMost(imageH)
        )
    }

    private fun cropBitmap(src: Bitmap, rect: Rect): Bitmap {
        val w = (rect.width()).coerceAtLeast(1)
        val h = (rect.height()).coerceAtLeast(1)
        return Bitmap.createBitmap(src, rect.left, rect.top, w, h)
    }

    /**
     * Composite [patch] (scaled to [destRect] size) onto [canvas] using a
     * radial soft mask so the edges feather into the surrounding image.
     *
     * The feather radius scales with maskDilation: more dilation → wider feather.
     * At maskDilation = 0 the feather is ~15% of the shorter patch dimension.
     */
    private fun blendPatch(
        target: Bitmap,
        patch: Bitmap,
        destRect: Rect,
        maskDilation: Int
    ) {
        val destW = destRect.width().coerceAtLeast(1)
        val destH = destRect.height().coerceAtLeast(1)

        val scaledPatch = if (patch.width == destW && patch.height == destH) {
            patch
        } else {
            Bitmap.createScaledBitmap(patch, destW, destH, true)
        }

        val maskBitmap = buildSoftMask(destW, destH, maskDilation)

        val srcPixels  = IntArray(destW * destH)
        val dstPixels  = IntArray(destW * destH)
        val maskPixels = IntArray(destW * destH)

        scaledPatch.getPixels(srcPixels,  0, destW, 0, 0, destW, destH)
        maskBitmap.getPixels(maskPixels, 0, destW, 0, 0, destW, destH)

        // Read existing pixels from target at the destination rect
        target.getPixels(dstPixels, 0, destW, destRect.left, destRect.top, destW, destH)

        val out = IntArray(destW * destH)
        for (i in out.indices) {
            // mask alpha is stored in the R channel (greyscale mask)
            val alpha = (maskPixels[i] shr 16) and 0xFF
            val t = alpha / 255f

            val sR = (srcPixels[i] shr 16) and 0xFF
            val sG = (srcPixels[i] shr  8) and 0xFF
            val sB =  srcPixels[i]         and 0xFF

            val dR = (dstPixels[i] shr 16) and 0xFF
            val dG = (dstPixels[i] shr  8) and 0xFF
            val dB =  dstPixels[i]         and 0xFF

            val oR = (sR * t + dR * (1f - t)).toInt().coerceIn(0, 255)
            val oG = (sG * t + dG * (1f - t)).toInt().coerceIn(0, 255)
            val oB = (sB * t + dB * (1f - t)).toInt().coerceIn(0, 255)

            out[i] = (0xFF shl 24) or (oR shl 16) or (oG shl 8) or oB
        }

        target.setPixels(out, 0, destW, destRect.left, destRect.top, destW, destH)

        if (scaledPatch != patch) scaledPatch.recycle()
        maskBitmap.recycle()
    }

    /**
     * Build a greyscale soft-mask bitmap [w]×[h].
     *
     * Center is white (alpha = 1.0, full restored patch), edges fade to black
     * (alpha = 0, full original image). The feather width is:
     *
     *   feather = min(w, h) * 0.15  +  abs(maskDilation) * 0.5
     *
     * Positive dilation → wider feather band (the extra padding we added to the
     * patch rect transitions smoothly into the background).
     * Negative dilation (erosion) → narrower feather (sharper patch boundary).
     */
    private fun buildSoftMask(w: Int, h: Int, maskDilation: Int): Bitmap {
        val minDim = minOf(w, h).toFloat()
        val feather = (minDim * 0.15f + kotlin.math.abs(maskDilation) * 0.5f).coerceAtLeast(4f)

        val pixels = IntArray(w * h)
        val cx = w / 2f
        val cy = h / 2f
        val rx = w / 2f
        val ry = h / 2f

        for (y in 0 until h) {
            for (x in 0 until w) {
                // Normalised distance from centre in ellipse space [0..1]
                val dx = (x - cx) / rx
                val dy = (y - cy) / ry
                val dist = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                // dist < innerEdge → fully opaque; dist > outerEdge → fully transparent
                val innerEdge = 1f - (feather / minDim)
                val alpha = when {
                    dist <= innerEdge -> 255
                    dist >= 1f        -> 0
                    else -> ((1f - (dist - innerEdge) / (1f - innerEdge)) * 255f).toInt().coerceIn(0, 255)
                }

                // Store as greyscale ARGB (R=G=B=alpha)
                pixels[y * w + x] = (0xFF shl 24) or (alpha shl 16) or (alpha shl 8) or alpha
            }
        }

        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }
}
