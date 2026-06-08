package com.rekluzlabs.makokolorize.ui.screens

import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rekluzlabs.makokolorize.data.image.ImageRepository
import com.rekluzlabs.makokolorize.edit.processor.ImageAdjustmentProcessor
import com.rekluzlabs.makokolorize.edit.ui.EditorScreen
import com.rekluzlabs.makokolorize.edit.viewmodel.EditorViewModel
import com.rekluzlabs.makokolorize.ui.components.ImageComparison
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    originalUri: Uri,
    resultUri: Uri,
    onBack: () -> Unit,
    onReRun: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imageRepository = remember { ImageRepository(context) }

    var currentResultUri by remember { mutableStateOf(resultUri) }
    var showEditor by remember { mutableStateOf(false) }
    var isLoadingBitmap by remember { mutableStateOf(false) }
    var bitmapToEdit by remember { mutableStateOf<Bitmap?>(null) }

    var showSaveDialog by remember { mutableStateOf(false) }
    var saveSuccess by remember { mutableStateOf<String?>(null) }
    var saveQuality by remember { mutableFloatStateOf(95f) }
    var selectedFormat by remember { mutableStateOf(Bitmap.CompressFormat.JPEG) }

    fun saveBitmapToGallery(targetUri: Uri, format: Bitmap.CompressFormat, quality: Int) {
        scope.launch {
            saveSuccess = try {
                withContext(Dispatchers.IO) {
                    val input = context.contentResolver.openInputStream(currentResultUri)
                        ?: return@withContext "Could not read result"
                    val bmp = BitmapFactory.decodeStream(input)
                    input.close()
                    if (bmp == null) return@withContext "Failed to decode result"

                    val out = context.contentResolver.openOutputStream(targetUri)
                        ?: return@withContext "Could not write to that location"
                    out.use { stream ->
                        if (!bmp.compress(format, quality, stream)) {
                            return@use "Failed to save image"
                        }
                    }
                    bmp.recycle()
                    null
                }
            } catch (e: Exception) {
                Log.e("ResultScreen", "Save failed", e)
                "Save failed: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun shareBitmap() {
        scope.launch {
            try {
                // Ensure we have a valid File object
                val file = if (currentResultUri.scheme == "file") {
                    File(currentResultUri.path ?: throw Exception("Invalid file path"))
                } else {
                    // Fallback: Copy to a temporary file in the shared directory
                    withContext(Dispatchers.IO) {
                        val sharedDir = File(context.cacheDir, "share")
                        sharedDir.mkdirs()
                        val tempFile = File(sharedDir, "makokolorize_${System.currentTimeMillis()}.png")
                        context.contentResolver.openInputStream(currentResultUri)?.use { input ->
                            tempFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        } ?: throw Exception("Could not open stream for URI")
                        tempFile
                    }
                }

                if (!file.exists()) {
                    throw Exception("File does not exist: ${file.absolutePath}")
                }

                val shareUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, shareUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    // Set clip data for better compatibility with some apps
                    clipData = ClipData.newRawUri("Colorized Photo", shareUri)
                }

                val chooser = Intent.createChooser(intent, "Share Colorized Photo")
                context.startActivity(chooser)
            } catch (e: Exception) {
                Log.e("ResultScreen", "Share failed", e)
                withContext(Dispatchers.Main) {
                    saveSuccess = "Share failed: ${e.localizedMessage ?: "Unknown error"}"
                }
            }
        }
    }

    val jpegLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/jpeg")
    ) { uri ->
        if (uri != null) saveBitmapToGallery(uri, Bitmap.CompressFormat.JPEG, saveQuality.toInt())
    }

    val pngLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        if (uri != null) saveBitmapToGallery(uri, Bitmap.CompressFormat.PNG, 100)
    }

    LaunchedEffect(showEditor) {
        if (showEditor && bitmapToEdit == null) {
            isLoadingBitmap = true
            imageRepository.loadBitmap(currentResultUri).onSuccess {
                bitmapToEdit = it
            }.onFailure {
                showEditor = false
            }
            isLoadingBitmap = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Before/After comparison fills available space
            Box(modifier = Modifier.weight(1f)) {
                androidx.compose.runtime.key(currentResultUri) {
                    ImageComparison(
                        beforeUri = originalUri,
                        afterUri = currentResultUri,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                if (isLoadingBitmap) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            // Bottom action bar
            Column(
                modifier = Modifier.navigationBarsPadding()
            ) {
                // MAKO Edit button positioned directly above the Save button
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    FloatingActionButton(
                        onClick = { showEditor = true },
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null)
                            Text("MAKO Edit")
                        }
                    }
                }

                saveSuccess?.let { msg ->
                    val isError = msg.startsWith("Save failed") || msg.startsWith("Could not") || msg.startsWith("Failed")
                    Text(
                        msg,
                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        fontSize = 13.sp
                    )
                }

                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("\u2190 Back")
                    }
                    Button(
                        onClick = { showSaveDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                    Button(
                        onClick = { shareBitmap() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Share")
                    }
                }
            }
        }
    }

    if (showEditor && bitmapToEdit != null) {
        Dialog(
            onDismissRequest = { showEditor = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val dialogViewModelStoreOwner = remember {
                object : ViewModelStoreOwner {
                    override val viewModelStore: ViewModelStore = ViewModelStore()
                }
            }

            DisposableEffect(dialogViewModelStoreOwner) {
                onDispose { dialogViewModelStoreOwner.viewModelStore.clear() }
            }

            CompositionLocalProvider(
                LocalViewModelStoreOwner provides dialogViewModelStoreOwner
            ) {
                val factory = remember { EditorViewModel.Factory(ImageAdjustmentProcessor()) }
                val editorViewModel: EditorViewModel = viewModel(factory = factory)

                LaunchedEffect(bitmapToEdit) {
                    bitmapToEdit?.let { editorViewModel.init(it) }
                }

                EditorScreen(
                    viewModel = editorViewModel,
                    onSave = { resultBitmap ->
                        scope.launch {
                            imageRepository.saveProcessedBitmap(resultBitmap).onSuccess { newUri ->
                                currentResultUri = newUri
                                bitmapToEdit = resultBitmap
                                showEditor = false
                            }
                        }
                    },
                    onDismiss = { showEditor = false }
                )
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Image") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Choose image format and quality:")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedFormat == Bitmap.CompressFormat.JPEG,
                            onClick = { selectedFormat = Bitmap.CompressFormat.JPEG },
                            label = { Text("JPEG") }
                        )
                        FilterChip(
                            selected = selectedFormat == Bitmap.CompressFormat.PNG,
                            onClick = { selectedFormat = Bitmap.CompressFormat.PNG },
                            label = { Text("PNG") }
                        )
                    }
                    if (selectedFormat == Bitmap.CompressFormat.JPEG) {
                        Column {
                            val qualityLabel = when {
                                saveQuality < 40 -> "Low"
                                saveQuality < 80 -> "Balanced"
                                else -> "High"
                            }
                            Text(
                                text = "Quality: ${saveQuality.toInt()}% ($qualityLabel)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = saveQuality,
                                onValueChange = { saveQuality = it },
                                valueRange = 1f..100f,
                                steps = 99,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    } else {
                        Text(
                            text = "PNG is lossless (maximum quality)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showSaveDialog = false
                    if (selectedFormat == Bitmap.CompressFormat.JPEG) {
                        jpegLauncher.launch("colorized_${System.currentTimeMillis()}.jpg")
                    } else {
                        pngLauncher.launch("colorized_${System.currentTimeMillis()}.png")
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

