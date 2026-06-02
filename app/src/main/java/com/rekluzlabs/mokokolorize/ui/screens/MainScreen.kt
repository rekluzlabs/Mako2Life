package com.rekluzlabs.makokolorize.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rekluzlabs.makokolorize.data.image.ImageRepository
import com.rekluzlabs.makokolorize.domain.ColorizeUseCase
import com.rekluzlabs.makokolorize.ui.components.AppProgressIndicator
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

object ResultHolder {
    var colorizedBitmap: Bitmap? = null
}

@Composable
fun MainScreen(
    imageUri: Uri,
    onResultReady: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var processing by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var error by remember { mutableStateOf<String?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    var vibrancy by remember { mutableStateOf(1.0f) }

    LaunchedEffect(imageUri) {
        val repo = ImageRepository(context)
        repo.loadBitmap(imageUri).onSuccess { bitmap = it }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Makokolorize",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        bitmap?.let { bm ->
            Image(
                bitmap = bm.asImageBitmap(),
                contentDescription = "Selected image",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!processing) {
            Text("Color Vibrancy", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("0.5x", fontSize = 12.sp, modifier = Modifier.width(28.dp))
                Slider(
                    value = vibrancy,
                    onValueChange = { vibrancy = it },
                    valueRange = 0.5f..2.0f,
                    steps = 14,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f)
                )
                Text("2.0x", fontSize = 12.sp, modifier = Modifier.width(28.dp))
            }
            Text(
                "${"%.1f".format(vibrancy)}x",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

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
        } else {
            Button(
                onClick = {
                    val bm = bitmap ?: return@Button
                    processing = true
                    progress = 0f
                    error = null
                    job = scope.launch {
                        val useCase = ColorizeUseCase(context)
                        useCase.execute(bm, { p -> progress = p }, vibrancy)
                            .onSuccess { result ->
                                ResultHolder.colorizedBitmap = result
                                processing = false
                                onResultReady()
                            }
                            .onFailure { e ->
                                error = e.message
                                processing = false
                            }
                    }
                },
                enabled = bitmap != null
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
}
