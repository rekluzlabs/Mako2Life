package com.rekluzlabs.makokolorize.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rekluzlabs.makokolorize.R
import com.rekluzlabs.makokolorize.data.model.ModelRepository
import com.rekluzlabs.makokolorize.service.DownloadService
import com.rekluzlabs.makokolorize.ui.components.AppProgressIndicator
import kotlinx.coroutines.launch

sealed class DownloadState {
    data object Checking : DownloadState()
    data object Ready : DownloadState()
    data class Downloading(
        val label: String,
        val description: String,
        val filename: String,
        val fileProgress: Float,
        val totalProgress: Float,
        val totalDownloadedBytes: Long,
        val totalSize: Long,
        val completedModels: List<String> = emptyList()
    ) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

@Composable
fun SplashScreen(
    onNavigate: () -> Unit,
    context: android.content.Context
) {
    val repo = remember { ModelRepository(context) }
    var state by remember { mutableStateOf<DownloadState>(DownloadState.Checking) }
    val serviceStatus by DownloadService.downloadStatus.collectAsState()
    
    // "Stay Awake" logic
    val currentView = LocalView.current
    DisposableEffect(state) {
        val keepOn = state is DownloadState.Downloading
        currentView.keepScreenOn = keepOn
        onDispose {
            currentView.keepScreenOn = false
        }
    }

    // Permission launcher for Android 13+ notifications
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            DownloadService.startService(context)
        } else {
            Toast.makeText(context, "Notification permission is required for background downloads", Toast.LENGTH_SHORT).show()
            DownloadService.startService(context) // Still start, but notification might be hidden
        }
    }

    LaunchedEffect(serviceStatus) {
        when (val status = serviceStatus) {
            is DownloadService.DownloadStatus.Progress -> {
                state = DownloadState.Downloading(
                    status.label, status.description, status.filename,
                    status.fileProgress, status.totalProgress,
                    status.totalDownloadedBytes, status.totalSize, status.completedModels
                )
            }
            is DownloadService.DownloadStatus.Success -> {
                Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
                onNavigate()
            }
            is DownloadService.DownloadStatus.Error -> {
                state = DownloadState.Error(status.message)
                Toast.makeText(context, "Download failed: ${status.message}", Toast.LENGTH_LONG).show()
            }
            is DownloadService.DownloadStatus.Idle -> {
                // Keep current state unless we just started
                if (state == DownloadState.Checking) {
                    if (repo.isModelDownloaded() && repo.isScunetModelDownloaded() && 
                        repo.isCodeformerModelDownloaded() && repo.isRealEsrganDownloaded()) {
                        onNavigate()
                    } else {
                        state = DownloadState.Ready
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (state is DownloadState.Downloading) {
            val currentState = state as DownloadState.Downloading
            
            NeuralPulseAnimation(modifier = Modifier.size(120.dp))
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Initializing AI Core",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            DownloadChecklist(
                currentLabel = currentState.label,
                completedModels = currentState.completedModels
            )
            Spacer(modifier = Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Current: ${currentState.label}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = currentState.description,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            AppProgressIndicator(
                progress = currentState.totalProgress,
                modifier = Modifier.fillMaxWidth()
            )
            val totalSizeMb = currentState.totalSize / 1_000_000
            val downloadedMb = currentState.totalDownloadedBytes / 1_000_000
            Text(
                text = "$downloadedMb / $totalSizeMb MB Downloaded",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = "This one-time setup ensures the highest quality restoration. You can minimize the app, the download will continue in the background.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 24.dp)
            )

        } else {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(32.dp))
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "makokolorize",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Colorize your black & white photos",
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(48.dp))

            when (val currentState = state) {
                is DownloadState.Checking -> {
                    CircularProgressIndicator(strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Checking AI resources...", style = MaterialTheme.typography.bodyMedium)
                }

                is DownloadState.Ready -> {
                    val freeSpace = repo.getFreeSpaceBytes()
                    var totalToDownload = 0L
                    var downloadedCount = 0
                    
                    if (repo.isModelDownloaded()) downloadedCount++ else totalToDownload += ModelRepository.MODEL_SIZE_BYTES
                    if (repo.isScunetModelDownloaded()) downloadedCount++ else totalToDownload += ModelRepository.SCUNET_MODEL_SIZE_BYTES
                    if (repo.isCodeformerModelDownloaded()) downloadedCount++ else totalToDownload += ModelRepository.CODEFORMER_MODEL_SIZE_BYTES
                    if (repo.isRealEsrganDownloaded()) downloadedCount++ else totalToDownload += ModelRepository.REALESRGAN_MODEL_SIZE_BYTES
                    
                    val remainingCount = 4 - downloadedCount
                    val totalToDownloadMb = totalToDownload / 1_000_000

                    if (remainingCount in 1..3) {
                        Text(
                            text = if (remainingCount == 1) "1 AI model has yet to be downloaded" 
                                   else "$remainingCount AI models have yet to be downloaded",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "($downloadedCount of 4 models already on device)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (freeSpace < ModelRepository.REQUIRED_FREE_SPACE) {
                        Text(
                            "Storage Low: ${freeSpace / 1_000_000}MB free, need approx. ${totalToDownloadMb + 100}MB",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                DownloadService.startService(context)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.8f).height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = if (downloadedCount > 0) "Continue Download ($totalToDownloadMb MB)" 
                                   else "Download AI Models ($totalToDownloadMb MB)", 
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.download_warning_note),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                is DownloadState.Error -> {
                    Text(
                        currentState.message,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = {
                        DownloadService.startService(context)
                    }) {
                        Text("Retry Download")
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun NeuralPulseAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "neuralPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "pulse"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(3000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "rotation"
    )

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = pulse; scaleY = pulse; rotationZ = rotation }) {
            drawCircle(brush = Brush.sweepGradient(listOf(Color(0xFF6200EE), Color(0xFF03DAC6), Color(0xFF6200EE))), style = Stroke(width = 8.dp.toPx()))
        }
        Box(modifier = Modifier.size(60.dp).graphicsLayer { scaleX = pulse * 0.9f; scaleY = pulse * 0.9f }.clip(CircleShape).background(Brush.radialGradient(colors = listOf(Color(0xFF03DAC6), Color(0xFF6200EE).copy(alpha = 0.5f)))))
        Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
    }
}

@Composable
fun DownloadChecklist(currentLabel: String, completedModels: List<String>) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChecklistItem("SCUNet", "Denoising", completedModels.contains("SCUNet"), currentLabel == "SCUNet", Modifier.weight(1f))
            ChecklistItem("CodeFormer", "Face Restore", completedModels.contains("CodeFormer"), currentLabel == "CodeFormer", Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChecklistItem("DDColor", "Colorization", completedModels.contains("DDColor"), currentLabel == "DDColor", Modifier.weight(1f))
            ChecklistItem("Real-ESRGAN", "HD Upscaling", completedModels.contains("Real-ESRGAN"), currentLabel == "Real-ESRGAN", Modifier.weight(1f))
        }
    }
}

@Composable
private fun ChecklistItem(label: String, desc: String, isCompleted: Boolean, isCurrent: Boolean, modifier: Modifier = Modifier) {
    val alpha by androidx.compose.animation.core.animateFloatAsState(targetValue = if (isCompleted || isCurrent) 1f else 0.3f, label = "alpha")
    Row(modifier = modifier.alpha(alpha), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = when { isCompleted -> Icons.Default.CheckCircle; isCurrent -> Icons.Default.HourglassTop; else -> Icons.Default.CloudDownload },
            contentDescription = null,
            tint = if (isCompleted) Color(0xFF4CAF50) else if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            Text(text = desc, fontSize = 10.sp, lineHeight = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
