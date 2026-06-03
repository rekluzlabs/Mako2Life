package com.rekluzlabs.makokolorize.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
    var codeFormerFidelity by remember { mutableFloatStateOf(initialConfig.codeFormerFidelity) }

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
            Text(
                text = "DDColor Input Size: ${ddcolorInputSize}px",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(512, 768, 1024).forEach { size ->
                    FilterChip(
                        selected = ddcolorInputSize == size,
                        onClick = { ddcolorInputSize = size },
                        label = { Text("${size}") }
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

            // Pipeline toggles (disabled — no models loaded)
            Text(
                text = "Pipeline",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = false,
                    onClick = { },
                    enabled = false,
                    label = { Text("Upscaling") }
                )
                FilterChip(
                    selected = false,
                    onClick = { },
                    enabled = false,
                    label = { Text("Face Restore") }
                )
                FilterChip(
                    selected = false,
                    onClick = { },
                    enabled = false,
                    label = { Text("Denoising") }
                )
            }
            Text(
                text = "Not available — only DDColor model is loaded",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )

            // CodeFormer fidelity (always visible, disabled)
            Text(
                text = "CodeFormer Fidelity: ${"%.2f".format(codeFormerFidelity)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("0.0", fontSize = 12.sp, modifier = Modifier.width(24.dp))
                Slider(
                    value = codeFormerFidelity,
                    onValueChange = { },
                    enabled = false,
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
                                codeFormerFidelity = codeFormerFidelity
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


