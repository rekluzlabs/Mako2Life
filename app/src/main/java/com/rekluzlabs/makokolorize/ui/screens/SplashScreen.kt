package com.rekluzlabs.makokolorize.ui.screens

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rekluzlabs.makokolorize.R
import com.rekluzlabs.makokolorize.data.model.ModelRepository
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
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<DownloadState>(DownloadState.Checking) }

    LaunchedEffect(Unit) {
        if (repo.isModelDownloaded() && repo.isScunetModelDownloaded() && repo.isCodeformerModelDownloaded() && repo.isRealEsrganDownloaded()) {
            onNavigate()
        } else {
            state = DownloadState.Ready
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
            
            // AI Neural Pulse Animation
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

            // Progress Checklist
            DownloadChecklist(
                currentLabel = currentState.label,
                completedModels = currentState.completedModels
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Active File Info Card
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

            // Main Progress Section
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
                text = "This one-time setup ensures the highest quality restoration. Please keep the app open.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 24.dp)
            )

        } else {
            // Branding header
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
                    if (!repo.isModelDownloaded()) totalToDownload += ModelRepository.MODEL_SIZE_BYTES
                    if (!repo.isScunetModelDownloaded()) totalToDownload += ModelRepository.SCUNET_MODEL_SIZE_BYTES
                    if (!repo.isCodeformerModelDownloaded()) totalToDownload += ModelRepository.CODEFORMER_MODEL_SIZE_BYTES
                    if (!repo.isRealEsrganDownloaded()) totalToDownload += ModelRepository.REALESRGAN_MODEL_SIZE_BYTES
                    
                    val totalToDownloadMb = totalToDownload / 1_000_000

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
                            scope.launch {
                                downloadModels(repo) { s -> state = s }
                                    .onSuccess { onNavigate() }
                                    .onFailure { e -> state = DownloadState.Error(e.message ?: "Network error") }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.8f).height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Download AI Models ($totalToDownloadMb MB)", fontWeight = FontWeight.Bold)
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
                        scope.launch {
                            downloadModels(repo) { s -> state = s }
                                .onSuccess { onNavigate() }
                                .onFailure { e -> state = DownloadState.Error(e.message ?: "Retry failed") }
                        }
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
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        // Outer glowing ring
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { 
            scaleX = pulse
            scaleY = pulse
            rotationZ = rotation
        }) {
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(Color(0xFF6200EE), Color(0xFF03DAC6), Color(0xFF6200EE))
                ),
                style = Stroke(width = 8.dp.toPx())
            )
        }
        
        // Inner pulsing core
        Box(
            modifier = Modifier
                .size(60.dp)
                .graphicsLayer { scaleX = pulse * 0.9f; scaleY = pulse * 0.9f }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF03DAC6), Color(0xFF6200EE).copy(alpha = 0.5f))
                    )
                )
        )
        
        Icon(
            imageVector = Icons.Default.CloudDownload,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(30.dp)
        )
    }
}

@Composable
fun DownloadChecklist(
    currentLabel: String,
    completedModels: List<String>
) {
    val models = listOf(
        "DDColor" to "Colorization Engine",
        "SCUNet" to "Denoising Engine",
        "CodeFormer" to "Face Restoration",
        "Real-ESRGAN" to "Ultra-HD Upscaling"
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        models.forEach { (label, desc) ->
            val isCompleted = completedModels.contains(label)
            val isCurrent = currentLabel == label
            
            val alpha by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isCompleted || isCurrent) 1f else 0.3f,
                label = "alpha"
            )
            
            Row(
                modifier = Modifier.fillMaxWidth().alpha(alpha),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        isCompleted -> Icons.Default.CheckCircle
                        isCurrent -> Icons.Default.HourglassTop
                        else -> Icons.Default.CloudDownload
                    },
                    contentDescription = null,
                    tint = if (isCompleted) Color(0xFF4CAF50) else if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = desc,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private suspend fun downloadModels(
    repo: ModelRepository,
    onState: (DownloadState) -> Unit
): Result<Unit> {
    val totalSize = (if (!repo.isModelDownloaded()) ModelRepository.MODEL_SIZE_BYTES else 0) +
                    (if (!repo.isScunetModelDownloaded()) ModelRepository.SCUNET_MODEL_SIZE_BYTES else 0) +
                    (if (!repo.isCodeformerModelDownloaded()) ModelRepository.CODEFORMER_MODEL_SIZE_BYTES else 0) +
                    (if (!repo.isRealEsrganDownloaded()) ModelRepository.REALESRGAN_MODEL_SIZE_BYTES else 0)
    
    var completedBytes = 0L
    val completedList = mutableListOf<String>()

    if (!repo.isModelDownloaded()) {
        repo.downloadModel { progress ->
            val currentFileBytes = (progress * ModelRepository.MODEL_SIZE_BYTES).toLong()
            onState(DownloadState.Downloading(
                "DDColor", "This will be used for colorizing images", ModelRepository.MODEL_FILENAME,
                progress, (completedBytes + currentFileBytes).toFloat() / totalSize,
                completedBytes + currentFileBytes, totalSize, completedList.toList()
            ))
        }.getOrElse { return Result.failure(it) }
        completedBytes += ModelRepository.MODEL_SIZE_BYTES
    }
    completedList.add("DDColor")

    if (!repo.isScunetModelDownloaded()) {
        repo.downloadScunetModel { progress ->
            val currentFileBytes = (progress * ModelRepository.SCUNET_MODEL_SIZE_BYTES).toLong()
            onState(DownloadState.Downloading(
                "SCUNet", "This will be used for removing noise and blur", ModelRepository.SCUNET_MODEL_FILENAME,
                progress, (completedBytes + currentFileBytes).toFloat() / totalSize,
                completedBytes + currentFileBytes, totalSize, completedList.toList()
            ))
        }.getOrElse { return Result.failure(it) }
        completedBytes += ModelRepository.SCUNET_MODEL_SIZE_BYTES
    }
    completedList.add("SCUNet")

    if (!repo.isCodeformerModelDownloaded()) {
        repo.downloadCodeformerModel { progress ->
            val currentFileBytes = (progress * ModelRepository.CODEFORMER_MODEL_SIZE_BYTES).toLong()
            onState(DownloadState.Downloading(
                "CodeFormer", "This will be used for restoring and enhancing faces", ModelRepository.CODEFORMER_MODEL_FILENAME,
                progress, (completedBytes + currentFileBytes).toFloat() / totalSize,
                completedBytes + currentFileBytes, totalSize, completedList.toList()
            ))
        }.getOrElse { return Result.failure(it) }
        completedBytes += ModelRepository.CODEFORMER_MODEL_SIZE_BYTES
    }
    completedList.add("CodeFormer")

    if (!repo.isRealEsrganDownloaded()) {
        repo.downloadRealEsrganModel { progress ->
            val currentFileBytes = (progress * ModelRepository.REALESRGAN_MODEL_SIZE_BYTES).toLong()
            onState(DownloadState.Downloading(
                "Real-ESRGAN", "This will be used for high-quality upscaling", ModelRepository.REALESRGAN_MODEL_FILENAME,
                progress, (completedBytes + currentFileBytes).toFloat() / totalSize,
                completedBytes + currentFileBytes, totalSize, completedList.toList()
            ))
        }.getOrElse { return Result.failure(it) }
    }

    return Result.success(Unit)
}
