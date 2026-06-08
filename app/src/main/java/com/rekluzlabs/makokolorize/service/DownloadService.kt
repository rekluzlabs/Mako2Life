package com.rekluzlabs.makokolorize.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.rekluzlabs.makokolorize.R
import com.rekluzlabs.makokolorize.data.model.ModelRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class DownloadService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repo: ModelRepository
    private lateinit var notificationManager: NotificationManager

    companion object {
        private const val CHANNEL_ID = "model_download_channel"
        private const val NOTIFICATION_ID = 101

        private val _downloadStatus = MutableStateFlow<DownloadStatus>(DownloadStatus.Idle)
        val downloadStatus = _downloadStatus.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, DownloadService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, DownloadService::class.java)
            context.stopService(intent)
        }
    }

    sealed class DownloadStatus {
        data object Idle : DownloadStatus()
        data class Progress(
            val label: String,
            val description: String,
            val filename: String,
            val fileProgress: Float,
            val totalProgress: Float,
            val totalDownloadedBytes: Long,
            val totalSize: Long,
            val completedModels: List<String>
        ) : DownloadStatus()
        data class Success(val message: String) : DownloadStatus()
        data class Error(val message: String) : DownloadStatus()
    }

    override fun onCreate() {
        super.onCreate()
        repo = ModelRepository(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification("Initializing...", 0f))
        
        serviceScope.launch {
            try {
                downloadAllModels()
                _downloadStatus.value = DownloadStatus.Success("All 4 AI models downloaded successfully!")
            } catch (e: Exception) {
                _downloadStatus.value = DownloadStatus.Error(e.message ?: "Download failed")
            } finally {
                stopForeground(true)
                stopSelf()
            }
        }
        
        return START_NOT_STICKY
    }

    private suspend fun downloadAllModels() {
        val totalSize = (if (!repo.isModelDownloaded()) ModelRepository.MODEL_SIZE_BYTES else 0) +
                        (if (!repo.isScunetModelDownloaded()) ModelRepository.SCUNET_MODEL_SIZE_BYTES else 0) +
                        (if (!repo.isCodeformerModelDownloaded()) ModelRepository.CODEFORMER_MODEL_SIZE_BYTES else 0) +
                        (if (!repo.isRealEsrganDownloaded()) ModelRepository.REALESRGAN_MODEL_SIZE_BYTES else 0)
        
        if (totalSize == 0L) return

        var completedBytes = 0L
        val completedList = mutableListOf<String>()

        val models = listOf(
            Triple("SCUNet", "Removing noise", suspend { repo.downloadScunetModel { p -> updateProgress("SCUNet", "This will be used for removing noise and blur", ModelRepository.SCUNET_MODEL_FILENAME, p, completedBytes, totalSize, completedList) } }),
            Triple("CodeFormer", "Restoring faces", suspend { repo.downloadCodeformerModel { p -> updateProgress("CodeFormer", "This will be used for restoring and enhancing faces", ModelRepository.CODEFORMER_MODEL_FILENAME, p, completedBytes, totalSize, completedList) } }),
            Triple("DDColor", "Colorizing images", suspend { repo.downloadModel { p -> updateProgress("DDColor", "This will be used for colorizing images", ModelRepository.MODEL_FILENAME, p, completedBytes, totalSize, completedList) } }),
            Triple("Real-ESRGAN", "HD upscaling", suspend { repo.downloadRealEsrganModel { p -> updateProgress("Real-ESRGAN", "This will be used for high-quality upscaling", ModelRepository.REALESRGAN_MODEL_FILENAME, p, completedBytes, totalSize, completedList) } })
        )

        for ((label, desc, downloadCall) in models) {
            val isDownloaded = when(label) {
                "DDColor" -> repo.isModelDownloaded()
                "SCUNet" -> repo.isScunetModelDownloaded()
                "CodeFormer" -> repo.isCodeformerModelDownloaded()
                "Real-ESRGAN" -> repo.isRealEsrganDownloaded()
                else -> false
            }

            if (!isDownloaded) {
                downloadCall().getOrThrow()
                completedBytes += when(label) {
                    "DDColor" -> ModelRepository.MODEL_SIZE_BYTES
                    "SCUNet" -> ModelRepository.SCUNET_MODEL_SIZE_BYTES
                    "CodeFormer" -> ModelRepository.CODEFORMER_MODEL_SIZE_BYTES
                    "Real-ESRGAN" -> ModelRepository.REALESRGAN_MODEL_SIZE_BYTES
                    else -> 0L
                }
            }
            completedList.add(label)
        }
    }

    private fun updateProgress(
        label: String,
        description: String,
        filename: String,
        fileProgress: Float,
        completedBytes: Long,
        totalSize: Long,
        completedList: List<String>
    ) {
        val currentFileBytes = (fileProgress * when(label) {
            "DDColor" -> ModelRepository.MODEL_SIZE_BYTES
            "SCUNet" -> ModelRepository.SCUNET_MODEL_SIZE_BYTES
            "CodeFormer" -> ModelRepository.CODEFORMER_MODEL_SIZE_BYTES
            "Real-ESRGAN" -> ModelRepository.REALESRGAN_MODEL_SIZE_BYTES
            else -> 0L
        }).toLong()
        
        val totalProgress = (completedBytes + currentFileBytes).toFloat() / totalSize
        
        _downloadStatus.value = DownloadStatus.Progress(
            label, description, filename, fileProgress, totalProgress,
            completedBytes + currentFileBytes, totalSize, completedList.toList()
        )
        
        notificationManager.notify(NOTIFICATION_ID, createNotification("Downloading $label...", totalProgress))
    }

    private fun createNotification(content: String, progress: Float): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Makokolorize AI Setup")
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher_foreground) // Use appropriate icon
            .setProgress(100, (progress * 100).toInt(), false)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Model Downloads",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
