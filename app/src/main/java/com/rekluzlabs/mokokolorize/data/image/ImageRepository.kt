package com.rekluzlabs.makokolorize.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ImageRepository(private val context: Context) {

    suspend fun loadBitmap(uri: Uri): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            } ?: return@withContext Result.failure(Exception("Failed to decode image"))

            val corrected = correctOrientation(uri, bitmap)
            Result.success(corrected)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun correctOrientation(uri: Uri, bitmap: Bitmap): Bitmap {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)
                val rotation = when (exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
                if (rotation != 0f) {
                    val matrix = Matrix().apply { postRotate(rotation) }
                    val rotated = Bitmap.createBitmap(
                        bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                    )
                    if (rotated != bitmap) bitmap.recycle()
                    return rotated
                }
            }
        } catch (_: Exception) { }
        return bitmap
    }

    suspend fun saveBitmap(bitmap: Bitmap, format: Bitmap.CompressFormat): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "exports")
            dir.mkdirs()
            val ext = if (format == Bitmap.CompressFormat.JPEG) "jpg" else "png"
            val file = File(dir, "mako_colorized_${System.currentTimeMillis()}.$ext")
            FileOutputStream(file).use { out ->
                bitmap.compress(format, 95, out)
            }
            Result.success(Uri.fromFile(file))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
