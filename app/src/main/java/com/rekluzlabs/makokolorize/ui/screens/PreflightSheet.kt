package com.rekluzlabs.makokolorize.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rekluzlabs.makokolorize.domain.RunConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreflightSheet(
    initialConfig: RunConfig,
    onDismiss: () -> Unit,
    onStart: (RunConfig) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var ddcolorInputSize by remember { mutableStateOf(initialConfig.ddcolorInputSize) }
    var vibrancy by remember { mutableFloatStateOf(initialConfig.vibrancy) }
    var upscalingEnabled by remember { mutableStateOf(initialConfig.upscalingEnabled) }
    var faceRestoreEnabled by remember { mutableStateOf(initialConfig.faceRestoreEnabled) }
    var denoisingEnabled by remember { mutableStateOf(initialConfig.denoisingEnabled) }
    var codeFormerFidelity by remember { mutableFloatStateOf(0.7f) }
    var scunetStrength by remember { mutableFloatStateOf(initialConfig.scunetStrength) }
    var scunetAdvancedNoiseRemoval by remember { mutableStateOf(initialConfig.scunetAdvancedNoiseRemoval) }
    var showSizeInfo by remember { mutableStateOf(false) }

    val anrAvailable = denoisingEnabled && scunetStrength >= 0.10f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Pre-flight Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // DDColor input size
            val sizeLabel = when (ddcolorInputSize) {
                512 -> "Fast"
                768 -> "Balanced"
                1024 -> "Detailed"
                else -> "$ddcolorInputSize"
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "DDColor Input Size: $sizeLabel (${ddcolorInputSize}px)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = { showSizeInfo = true },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (showSizeInfo) {
                AlertDialog(
                    onDismissRequest = { showSizeInfo = false },
                    title = { Text("DDColor Input Size") },
                    text = {
                        Text("Before colorizing your photo, the AI resizes it to this resolution internally for analysis. A higher value means the AI sees finer detail when choosing colors, which improves accuracy for portraits and complex images. A lower value processes faster and uses less memory. Your final image is always saved at full resolution.\n\n⚠️ Higher values require significantly more memory and processing power. On older or lower-end devices, choosing 1024 may cause slower performance or the app to crash. If you experience issues, try a lower value.")
                    },
                    confirmButton = {
                        TextButton(onClick = { showSizeInfo = false }) {
                            Text("Got it")
                        }
                    }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(512, 768, 1024).forEach { size ->
                    val chipLabel = when (size) {
                        512 -> "Fast"
                        768 -> "Balanced"
                        1024 -> "Detailed"
                        else -> "$size"
                    }
                    FilterChip(
                        selected = ddcolorInputSize == size,
                        onClick = { ddcolorInputSize = size },
                        label = { Text(chipLabel) }
                    )
                }
            }

            // Color vibrancy (moved from MainScreen)
            Text(
                text = "Color Vibrancy: ${"%.1f".format(vibrancy)}x",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

            // SCUNet denoising strength
            Text(
                text = "SCUNet Denoising Strength: ${"%.0f".format(scunetStrength * 100)}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (denoisingEnabled) 1f else 0.4f
                )
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("0%", fontSize = 12.sp, modifier = Modifier.width(28.dp))
                Slider(
                    value = scunetStrength,
                    onValueChange = {
                        if (denoisingEnabled) {
                            scunetStrength = it
                            if (it < 0.10f) scunetAdvancedNoiseRemoval = false
                        }
                    },
                    valueRange = 0f..1f,
                    steps = 19,
                    enabled = denoisingEnabled,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f)
                )
                Text("100%", fontSize = 12.sp, modifier = Modifier.width(32.dp))
            }

            // Advanced noise removal toggle
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.alpha(if (anrAvailable) 1f else 0.4f)
            ) {
                FilterChip(
                    selected = scunetAdvancedNoiseRemoval,
                    onClick = { if (anrAvailable) scunetAdvancedNoiseRemoval = !scunetAdvancedNoiseRemoval },
                    enabled = anrAvailable,
                    label = { Text("Advanced Noise Removal") }
                )
            }
            if (anrAvailable) {
                Text(
                    text = "Runs a second SCUNet pass for stronger denoising",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            } else {
                Text(
                    text = if (scunetStrength < 0.10f) {
                        "Increase denoising strength above 10% to enable"
                    } else {
                        "Enable Denoising pipeline toggle to use SCUNet"
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pipeline toggles
            Text(
                text = "Pipeline",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = upscalingEnabled,
                    onClick = { upscalingEnabled = !upscalingEnabled },
                    enabled = false, // Keep false if RealESRGAN is not yet implemented
                    label = { Text("Upscaling") }
                )
                FilterChip(
                    selected = faceRestoreEnabled,
                    onClick = { faceRestoreEnabled = !faceRestoreEnabled },
                    enabled = true,
                    label = { Text("Face Restore") }
                )
                FilterChip(
                    selected = denoisingEnabled,
                    onClick = { denoisingEnabled = !denoisingEnabled },
                    enabled = true,
                    label = { Text(if (denoisingEnabled) "Denoising ON" else "Denoising OFF") },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0x66FF0000),
                        labelColor = Color(0xFFFF4444),
                        selectedContainerColor = Color(0x6600CC00),
                        selectedLabelColor = Color(0xFF00CC00)
                    )
                )
            }
            Text(
                text = "Pipeline: DDColor → Face Restore → SCUNet",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )

            // CodeFormer fidelity
            Text(
                text = "CodeFormer Fidelity: ${"%.2f".format(codeFormerFidelity)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (faceRestoreEnabled) 1f else 0.4f
                )
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("0.0", fontSize = 12.sp, modifier = Modifier.width(24.dp))
                Slider(
                    value = codeFormerFidelity,
                    onValueChange = { codeFormerFidelity = it },
                    enabled = faceRestoreEnabled,
                    valueRange = 0f..1f,
                    steps = 99,
                    modifier = Modifier.weight(1f)
                )
                Text("1.0", fontSize = 12.sp, modifier = Modifier.width(24.dp))
            }

            // Estimated time hint
            val estimate = "~${ddcolorInputSize / 512 * 30}s on this device"
            Text(
                text = estimate,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onStart(
                            RunConfig(
                                ddcolorInputSize = ddcolorInputSize,
                                vibrancy = vibrancy,
                                upscalingEnabled = upscalingEnabled,
                                faceRestoreEnabled = faceRestoreEnabled,
                                denoisingEnabled = denoisingEnabled,
                                codeFormerFidelity = codeFormerFidelity,
                                scunetStrength = scunetStrength,
                                scunetAdvancedNoiseRemoval = scunetAdvancedNoiseRemoval
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Start Processing")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}


