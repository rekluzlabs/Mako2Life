package com.rekluzlabs.makokolorize.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rekluzlabs.makokolorize.data.image.ImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ResultScreen(
    originalUri: Uri,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val colorized = ResultHolder.colorizedBitmap

    var original by remember { mutableStateOf<Bitmap?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveSuccess by remember { mutableStateOf<String?>(null) }
    var showPreview by remember { mutableStateOf(false) }

    LaunchedEffect(originalUri) {
        val repo = ImageRepository(context)
        repo.loadBitmap(originalUri).onSuccess { original = it }
    }

    fun saveBitmap(uri: Uri, format: Bitmap.CompressFormat) {
        val bmp = colorized ?: return
        scope.launch {
            saveSuccess = try {
                withContext(Dispatchers.IO) {
                    val out = context.contentResolver.openOutputStream(uri)
                        ?: return@withContext "Could not write to that location"
                    out.use { stream ->
                        if (!bmp.compress(format, 95, stream)) {
                            return@use "Failed to save image"
                        }
                    }
                    "Saved successfully as ${if (format == Bitmap.CompressFormat.JPEG) "JPEG" else "PNG"}"
                }
            } catch (e: Exception) {
                Log.e("ResultScreen", "Save failed", e)
                "Save failed: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    val jpegLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/jpeg")
    ) { uri ->
        if (uri != null) saveBitmap(uri, Bitmap.CompressFormat.JPEG)
    }

    val pngLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        if (uri != null) saveBitmap(uri, Bitmap.CompressFormat.PNG)
    }

    Box(modifier = Modifier.fillMaxSize()) {
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

            Spacer(modifier = Modifier.height(16.dp))

            Text("Original", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            original?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Original photograph",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Colorized", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            colorized?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "AI colorized result",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showPreview = true },
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            saveSuccess?.let { msg ->
                val isError = msg.startsWith("Save failed") || msg.startsWith("Could not") || msg.startsWith("Failed")
                Text(
                    msg,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = onBack) {
                    Text("Back")
                }
                Button(
                    onClick = { showSaveDialog = true },
                    enabled = colorized != null
                ) {
                    Text("Save")
                }
            }
        }

        if (showPreview && colorized != null) {
            Dialog(
                onDismissRequest = { showPreview = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { showPreview = false },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = colorized.asImageBitmap(),
                        contentDescription = "Colorized preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(onClick = { showPreview = false }) {
                            Text("Close")
                        }
                        Button(onClick = {
                            showPreview = false
                            showSaveDialog = true
                        }) {
                            Text("Save")
                        }
                    }
                }
            }
        }

        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text("Save Image") },
                text = { Text("Choose image format:") },
                confirmButton = {
                    TextButton(onClick = {
                        showSaveDialog = false
                        jpegLauncher.launch("image/jpeg")
                    }) { Text("JPEG") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showSaveDialog = false
                        pngLauncher.launch("image/png")
                    }) { Text("PNG") }
                }
            )
        }
    }
}
