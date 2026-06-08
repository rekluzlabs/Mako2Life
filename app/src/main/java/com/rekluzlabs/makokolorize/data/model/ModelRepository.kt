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
        get() = File(context.getExternalFilesDir(null), "models")

    private val modelFile: File
        get() = resolveModelFile(context, MODEL_FILENAME)

    private val scunetModelFile: File
        get() = resolveModelFile(context, SCUNET_MODEL_FILENAME)

    private val codeformerModelFile: File
        get() = resolveModelFile(context, CODEFORMER_MODEL_FILENAME)

    private val realesrganModelFile: File
        get() = resolveModelFile(context, REALESRGAN_MODEL_FILENAME)

    fun isModelDownloaded(): Boolean {
        if (!modelFile.exists() || modelFile.length() == 0L) return false
        return modelFile.length() >= (MODEL_SIZE_BYTES - 1024L)
    }

    fun isScunetModelDownloaded(): Boolean {
        if (!scunetModelFile.exists() || scunetModelFile.length() == 0L) return false
        return scunetModelFile.length() >= (SCUNET_MODEL_SIZE_BYTES - 1024L)
    }

    fun isCodeformerModelDownloaded(): Boolean {
        if (!codeformerModelFile.exists() || codeformerModelFile.length() == 0L) return false
        return codeformerModelFile.length() >= (CODEFORMER_MODEL_SIZE_BYTES - 1024L)
    }

    fun isRealEsrganDownloaded(): Boolean {
        if (!realesrganModelFile.exists() || realesrganModelFile.length() == 0L) return false
        return realesrganModelFile.length() >= (REALESRGAN_MODEL_SIZE_BYTES - 1024L)
    }

    fun deleteInvalidModel() {
        if (modelFile.exists()) {
            modelFile.delete()
        }
    }

    fun deleteInvalidScunetModel() {
        if (scunetModelFile.exists()) {
            scunetModelFile.delete()
        }
    }

    fun deleteInvalidCodeformerModel() {
        if (codeformerModelFile.exists()) {
            codeformerModelFile.delete()
        }
    }

    fun deleteInvalidRealEsrganModel() {
        if (realesrganModelFile.exists()) {
            realesrganModelFile.delete()
        }
    }

    fun getModelPath(): String = modelFile.absolutePath

    fun getScunetModelPath(): String = scunetModelFile.absolutePath

    fun getCodeformerModelPath(): String = codeformerModelFile.absolutePath

    fun getRealEsrganPath(): String = realesrganModelFile.absolutePath

    fun getFreeSpaceBytes(): Long = modelDir.parentFile?.freeSpace ?: 0L

    suspend fun downloadModel(onProgress: (Float) -> Unit): Result<Unit> =
        downloadFile(MODEL_URL, modelFile, onProgress)

    suspend fun downloadScunetModel(onProgress: (Float) -> Unit): Result<Unit> =
        downloadFile(SCUNET_MODEL_URL, scunetModelFile, onProgress)

    suspend fun downloadCodeformerModel(onProgress: (Float) -> Unit): Result<Unit> =
        downloadFile(CODEFORMER_MODEL_URL, codeformerModelFile, onProgress)

    suspend fun downloadRealEsrganModel(onProgress: (Float) -> Unit): Result<Unit> =
        downloadFile(REALESRGAN_MODEL_URL, realesrganModelFile, onProgress)

    private suspend fun downloadFile(
        url: String,
        file: File,
        onProgress: (Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            ensureActive()
            modelDir.mkdirs()

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Download failed (${response.code})"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty response"))
            val contentLength = body.contentLength()

            body.byteStream().use { input ->
                FileOutputStream(file).use { output ->
                    val buffer = ByteArray(8192)
                    var totalRead = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        ensureActive()
                        output.write(buffer, 0, read)
                        totalRead += read
                        if (contentLength > 0) {
                            onProgress(totalRead.toFloat() / contentLength.toFloat())
                        } else {
                            // Fallback if server doesn't return content-length header
                            onProgress(-1f)
                        }
                    }
                }
            }

            if (contentLength > 0 && file.length() < contentLength) {
                file.delete()
                return@withContext Result.failure(Exception("Download interrupted: incomplete file size."))
            }

            if (!file.exists() || file.length() == 0L) {
                return@withContext Result.failure(Exception("Download produced empty file"))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (file.exists()) file.delete()
            Result.failure(e)
        }
    }

    companion object {
        fun resolveModelFile(context: Context, fileName: String): File {
            val externalFile = File(context.getExternalFilesDir(null), "models/$fileName")
            if (externalFile.exists()) return externalFile
            val internalFile = File(context.filesDir, "models/$fileName")
            if (internalFile.exists()) return internalFile
            return externalFile
        }

        const val MODEL_FILENAME = "ddcolor_paper_tiny.onnx"
        const val SCUNET_MODEL_FILENAME = "SCUNet-PSNR.onnx"
        const val CODEFORMER_MODEL_FILENAME = "codeformer.onnx"
        const val REALESRGAN_MODEL_FILENAME = "realesr-general-x4v3.onnx"

        const val MODEL_URL = "https://huggingface.co/RekluzLabs/MakoKolor/resolve/main/ddcolor_paper_tiny.onnx"
        const val MODEL_SIZE_BYTES = 264_000_000L
        
        const val SCUNET_MODEL_URL = "https://huggingface.co/RekluzLabs/MakoKolor/resolve/main/SCUNet-PSNR.onnx"
        const val SCUNET_MODEL_SIZE_BYTES = 89_000_000L
        
        const val CODEFORMER_MODEL_URL = "https://huggingface.co/RekluzLabs/MakoKolor/resolve/main/codeformer.onnx"
        const val CODEFORMER_MODEL_SIZE_BYTES = 368_000_000L

        const val REALESRGAN_MODEL_URL = "https://huggingface.co/RekluzLabs/MakoKolor/resolve/main/realesr-general-x4v3.onnx"
        const val REALESRGAN_MODEL_SIZE_BYTES = 4_870_000L
        
        const val REQUIRED_FREE_SPACE = 1_000_000_000L // 1GB
    }
}
