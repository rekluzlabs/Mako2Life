package com.rekluzlabs.makokolorize.ui.screens

import android.graphics.Bitmap
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rekluzlabs.makokolorize.data.settings.SettingsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settings = remember { SettingsRepository(context) }

    var outputFormat by remember { mutableStateOf(settings.outputFormat) }
    var jpegQuality by remember { mutableIntStateOf(settings.jpegQuality) }
    var keepAwake by remember { mutableStateOf(settings.keepScreenAwake) }
    var saveOriginal by remember { mutableStateOf(settings.saveOriginalAlongsideResult) }
    var tileSizeText by remember { mutableStateOf(settings.tileSizeOverride.toString()) }

    val view = LocalView.current

    DisposableEffect(keepAwake) {
        val window = (view.context as? android.app.Activity)?.window
        if (keepAwake) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = {
                        settings.outputFormat = outputFormat
                        settings.jpegQuality = jpegQuality
                        settings.keepScreenAwake = keepAwake
                        settings.saveOriginalAlongsideResult = saveOriginal
                        settings.tileSizeOverride = tileSizeText.toIntOrNull() ?: 0
                        onBack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Output format
            SettingSectionHeader("Output Format")
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = { outputFormat = Bitmap.CompressFormat.JPEG },
                    enabled = outputFormat == Bitmap.CompressFormat.JPEG
                ) { Text("JPEG") }
                TextButton(
                    onClick = { outputFormat = Bitmap.CompressFormat.PNG },
                    enabled = outputFormat == Bitmap.CompressFormat.PNG
                ) { Text("PNG") }
            }

            // JPEG quality
            if (outputFormat == Bitmap.CompressFormat.JPEG) {
                SettingSectionHeader("JPEG Quality: $jpegQuality")
                Slider(
                    value = jpegQuality.toFloat(),
                    onValueChange = { jpegQuality = it.toInt() },
                    valueRange = 1f..100f,
                    steps = 98,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Keep screen awake
            SettingToggle(
                title = "Keep screen awake",
                description = "Prevent screen from turning off during processing",
                checked = keepAwake,
                onCheckedChange = { keepAwake = it }
            )

            // Save original alongside result
            SettingToggle(
                title = "Save original alongside result",
                description = "Save both original and colorized images when exporting",
                checked = saveOriginal,
                onCheckedChange = { saveOriginal = it }
            )

            // Tile size override
            SettingSectionHeader("Tile Size Override (0 = auto)")
            OutlinedTextField(
                value = tileSizeText,
                onValueChange = { tileSizeText = it.filter { c -> c.isDigit() } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.width(120.dp),
                placeholder = { Text("0") }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SettingToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(
                description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
