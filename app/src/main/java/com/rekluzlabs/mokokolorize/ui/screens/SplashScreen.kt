package com.rekluzlabs.makokolorize.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
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
    data class Downloading(val progress: Float) : DownloadState()
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
        if (repo.isModelDownloaded()) {
            onNavigate()
        } else {
            state = DownloadState.Ready
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher_foreground),
            contentDescription = "App Logo",
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(32.dp))
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Makokolorize",
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
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Checking for model...")
            }

            is DownloadState.Ready -> {
                val freeSpace = repo.getFreeSpaceBytes()
                if (freeSpace < ModelRepository.REQUIRED_FREE_SPACE) {
                    Text(
                        "Low storage: ${freeSpace / 1_000_000}MB free, need 1GB",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(onClick = {
                    state = DownloadState.Downloading(0f)
                    scope.launch {
                        repo.downloadModel { progress ->
                            state = DownloadState.Downloading(progress)
                        }.onSuccess {
                            onNavigate()
                        }.onFailure { e ->
                            state = DownloadState.Error(e.message ?: "Download failed")
                        }
                    }
                }) {
                    Text(stringResource(R.string.download_model))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.download_warning_note),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            is DownloadState.Downloading -> {
                AppProgressIndicator(progress = currentState.progress)
                Spacer(modifier = Modifier.height(8.dp))
                Text("${(currentState.progress * 100).toInt()}%")
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.downloading_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            is DownloadState.Error -> {
                Text(
                    currentState.message,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    state = DownloadState.Downloading(0f)
                    scope.launch {
                        repo.downloadModel { progress ->
                            state = DownloadState.Downloading(progress)
                        }.onSuccess {
                            onNavigate()
                        }.onFailure { e ->
                            state = DownloadState.Error(e.message ?: "Download failed")
                        }
                    }
                }) {
                    Text(stringResource(R.string.retry))
                }
            }
        }
    }
}
