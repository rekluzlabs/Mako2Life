package com.rekluzlabs.makokolorize.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rekluzlabs.makokolorize.R
import com.rekluzlabs.makokolorize.data.image.ImageRepository
import com.rekluzlabs.makokolorize.domain.ColorizeUseCase
import com.rekluzlabs.makokolorize.domain.RunConfig
import com.rekluzlabs.makokolorize.ui.components.AppProgressIndicator
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import android.util.Log

object ResultHolder {
    var colorizedBitmap: Bitmap? = null
}

@Composable
fun MainScreen(
    imageUri: Uri,
    onResultReady: (RunConfig) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loadingBitmap by remember { mutableStateOf(true) }
    var processing by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var error by remember { mutableStateOf<String?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    var lastConfig by remember { mutableStateOf(RunConfig()) }
    var showPreflightSheet by remember { mutableStateOf(false) }

    LaunchedEffect(imageUri) {
        loadingBitmap = true
        error = null
        val repo = ImageRepository(context)
        repo.loadBitmap(imageUri)
            .onSuccess {
                bitmap = it
                loadingBitmap = false
            }
            .onFailure { e ->
                Log.e("MainScreen", "Failed to load bitmap", e)
                error = "Failed to load image: ${e.localizedMessage}"
                loadingBitmap = false
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher_foreground),
            contentDescription = "App Logo",
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(16.dp))
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Makokolorize",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (loadingBitmap) {
            CircularProgressIndicator(modifier = Modifier.padding(32.dp))
            Text("Preparing image...", fontSize = 14.sp)
        } else if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Selected image",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Fit
            )
        } else if (error != null) {
            Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (processing) {
            AppProgressIndicator(progress = progress)
            Spacer(modifier = Modifier.height(8.dp))
            Text("${(progress * 100).toInt()}%", fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = {
                job?.cancel()
                processing = false
                progress = 0f
                error = null
            }) {
                Text("Cancel")
            }
        } else if (bitmap != null) {
            Button(
                onClick = { showPreflightSheet = true }
            ) {
                Text("Begin Colorization")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!processing) {
            TextButton(onClick = onBack) {
                Text("Back")
            }
        }
    }

    if (showPreflightSheet) {
        PreflightSheet(
            initialConfig = lastConfig,
            onDismiss = { showPreflightSheet = false },
            onStart = { config ->
                showPreflightSheet = false
                lastConfig = config
                val bm = bitmap ?: return@PreflightSheet
                processing = true
                progress = 0f
                error = null
                job = scope.launch {
                    val useCase = ColorizeUseCase(context)
                    useCase.execute(bm, { p -> progress = p }, config.vibrancy)
                        .onSuccess { result ->
                            ResultHolder.colorizedBitmap = result
                            processing = false
                            onResultReady(config)
                        }
                        .onFailure { e ->
                            error = e.message
                            processing = false
                        }
                }
            }
        )
    }
}
