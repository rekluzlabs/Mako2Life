package com.rekluzlabs.makokolorize.data.model

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class ModelRepository(private val context: Context) {

    private val modelDir: File
        get() = File(context.filesDir, "models")

    private val modelFile: File
        get() = File(modelDir, MODEL_FILENAME)

    fun isModelDownloaded(): Boolean {
        // We check if the file exists and is close to the expected size (within 1MB)
        // This prevents the app from trying to load a partially downloaded 934MB file.
        return modelFile.exists() && 
               modelFile.length() >= (MODEL_SIZE_BYTES - 1_000_000L)
    }

    fun deleteInvalidModel() {
        if (modelFile.exists()) {
            modelFile.delete()
        }
    }

    fun getModelPath(): String = modelFile.absolutePath

    fun getFreeSpaceBytes(): Long = modelDir.parentFile?.freeSpace ?: 0L

    suspend fun downloadModel(onProgress: (Float) -> Unit): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            ensureActive()
            modelDir.mkdirs()

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder().url(MODEL_URL).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Download failed (${response.code})"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty response"))
            val contentLength = body.contentLength()

            body.byteStream().use { input ->
                FileOutputStream(modelFile).use { output ->
                    val buffer = ByteArray(8192)
                    var totalRead = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        ensureActive()
                        output.write(buffer, 0, read)
                        totalRead += read
                        if (contentLength > 0) {
                            onProgress(totalRead.toFloat() / contentLength.toFloat())
                        }
                    }
                }
            }

            if (!modelFile.exists() || modelFile.length() == 0L) {
                return@withContext Result.failure(Exception("Download produced empty file"))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (modelFile.exists()) modelFile.delete()
            Result.failure(e)
        }
    }

    companion object {
        private const val MODEL_FILENAME = "ddcolor.onnx"

        const val MODEL_URL = "https://github.com/rekluzlabs/Makokolor/releases/download/ddcolor/ddcolor.onnx"
        const val MODEL_SIZE_BYTES = 934_000_000L
        const val REQUIRED_FREE_SPACE = 1_000_000_000L
    }
}
