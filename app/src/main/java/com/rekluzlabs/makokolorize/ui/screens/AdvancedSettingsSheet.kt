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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rekluzlabs.makokolorize.domain.RunConfig
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsSheet(
    initialConfig: RunConfig,
    onDismiss: () -> Unit,
    onApply: (RunConfig) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var denoisingEnabled by remember { mutableStateOf(initialConfig.denoisingEnabled) }
    var scunetStrength by remember { mutableFloatStateOf(initialConfig.scunetStrength) }
    var scunetAdvancedNoiseRemoval by remember { mutableStateOf(initialConfig.scunetAdvancedNoiseRemoval) }
    var scunetSecondPassStrength by remember { mutableFloatStateOf(0.3f) }

    var colorizeEnabled by remember { mutableStateOf(initialConfig.colorizeEnabled) }
    var vibrancy by remember { mutableFloatStateOf(initialConfig.vibrancy) }

    var upscalingEnabled by remember { mutableStateOf(initialConfig.upscalingEnabled) }
    var upscaleScale by remember { mutableStateOf(initialConfig.upscaleScale) }

    // ADetailer / CodeFormer Settings
    var faceRestoreEnabled by remember { mutableStateOf(initialConfig.faceRestoreEnabled) }
    var adetailerModel by remember { mutableStateOf(initialConfig.adetailerModel) }
    var detectionConfidence by remember { mutableFloatStateOf(initialConfig.detectionConfidence) }
    var maskDilation by remember { mutableIntStateOf(initialConfig.maskDilation) }
    var codeFormerFidelity by remember { mutableFloatStateOf(initialConfig.codeFormerFidelity) }
    var maskMergeMode by remember { mutableStateOf(initialConfig.maskMergeMode) }
    
    var codeFormerUpscaleFace by remember { mutableStateOf(initialConfig.codeFormerUpscaleFace) }
    var codeFormerUpscaleBackground by remember { mutableStateOf(initialConfig.codeFormerUpscaleBackground) }

    fun snapVibrancy(v: Float) = if (kotlin.math.abs(v - 1.0f) <= 0.05f) 1.0f else v

    ModalBottomSheet(
        onDismissRequest = {
            onApply(
                RunConfig(
                    ddcolorInputSize = initialConfig.ddcolorInputSize,
                    vibrancy = vibrancy,
                    upscalingEnabled = upscalingEnabled,
                    faceRestoreEnabled = faceRestoreEnabled,
                    denoisingEnabled = denoisingEnabled,
                    colorizeEnabled = colorizeEnabled,
                    codeFormerFidelity = codeFormerFidelity,
                    adetailerModel = adetailerModel,
                    detectionConfidence = detectionConfidence,
                    maskDilation = maskDilation,
                    maskMergeMode = maskMergeMode,
                    codeFormerUpscaleFace = codeFormerUpscaleFace,
                    codeFormerUpscaleBackground = codeFormerUpscaleBackground,
                    scunetStrength = scunetStrength,
                    scunetAdvancedNoiseRemoval = scunetAdvancedNoiseRemoval,
                    upscaleScale = upscaleScale
                )
            )
            onDismiss()
        },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Pipeline Settings",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── SCUNet - Denoise ──
            ModelSection(
                title = "SCUNet  —  Denoise",
                enabled = denoisingEnabled,
                onToggle = { denoisingEnabled = it },
                infoTitle = "SCUNet — Denoise",
                infoText = {
                    Text(
                        "0.1 – 0.3 — barely touches the image, good for lightly noisy photos\n\n" +
                        "0.4 – 0.6 — the sweet spot for most old photos, removes grain without over-smoothing\n\n" +
                        "0.7 – 0.9 — aggressive, works well on heavily damaged scans\n\n" +
                        "1.0 — full model output, no blending with original, can look plasticky"
                    )
                }
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Strength",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Light", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Moderate", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Strong", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("0.1", fontSize = 11.sp, modifier = Modifier.width(24.dp))
                    Slider(
                        value = scunetStrength,
                        onValueChange = { scunetStrength = (it * 20).roundToInt() / 20f },
                        valueRange = 0.1f..1.0f,
                        steps = 17,
                        modifier = Modifier.weight(1f)
                    )
                    Text("1.0", fontSize = 11.sp, modifier = Modifier.width(24.dp))
                }
                Text(
                    text = "%.2f".format(scunetStrength),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Advanced removal",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilterChip(
                        selected = scunetAdvancedNoiseRemoval,
                        onClick = { if (scunetStrength >= 0.10f) scunetAdvancedNoiseRemoval = !scunetAdvancedNoiseRemoval },
                        enabled = scunetStrength >= 0.10f,
                        label = { Text(if (scunetAdvancedNoiseRemoval) "on" else "off", fontSize = 12.sp) }
                    )
                }

                if (scunetAdvancedNoiseRemoval && scunetStrength >= 0.10f) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("0.1", fontSize = 11.sp, modifier = Modifier.width(24.dp))
                        Slider(
                            value = scunetSecondPassStrength,
                            onValueChange = { scunetSecondPassStrength = (it * 20).roundToInt() / 20f },
                            valueRange = 0.1f..0.5f,
                            steps = 7,
                            modifier = Modifier.weight(1f),
                            enabled = scunetAdvancedNoiseRemoval
                        )
                        Text("0.5", fontSize = 11.sp, modifier = Modifier.width(24.dp))
                    }
                    Text(
                        text = "%.2f".format(scunetSecondPassStrength),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── DDColor - Colorize ──
            ModelSection(
                title = "DDColor  —  Colorize",
                enabled = colorizeEnabled,
                onToggle = { colorizeEnabled = it },
                infoTitle = "DDColor — Colorize",
                infoText = {
                    Text(
                        "0.5 – 0.8 — desaturated, almost sepia-like, good for subtle colorization\n\n" +
                        "0.9 – 1.1 — neutral, closest to what the model predicts naturally\n\n" +
                        "1.2 – 1.5 — noticeably more colorful, good for landscapes and clothing\n\n" +
                        "1.6 – 2.0 — highly saturated, can look unnatural on skin tones but striking on scenery"
                    )
                }
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Vibrancy",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Muted", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Natural", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Vivid", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("0.5", fontSize = 11.sp, modifier = Modifier.width(24.dp))
                    Slider(
                        value = vibrancy,
                        onValueChange = { vibrancy = (it * 20).roundToInt() / 20f },
                        onValueChangeFinished = { vibrancy = snapVibrancy(vibrancy) },
                        valueRange = 0.5f..2.0f,
                        steps = 29,
                        modifier = Modifier.weight(1f)
                    )
                    Text("2.0", fontSize = 11.sp, modifier = Modifier.width(24.dp))
                }
                Text(
                    text = "%.2f".format(vibrancy),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── ADetailer / CodeFormer - Restoration ──
            ModelSection(
                title = "ADetailer  —  Restoration",
                enabled = faceRestoreEnabled,
                onToggle = { faceRestoreEnabled = it },
                infoTitle = "ADetailer Restoration",
                infoText = {
                    Text(
                        "Inspired by ADetailer, this pipeline detects specific objects (faces, hands, etc.) " +
                        "and runs high-fidelity restoration on each cropped patch.\n\n" +
                        "Denoising Strength (Fidelity): Controls AI creativity. 0.0 is original, 1.0 is full AI repaint.\n\n" +
                        "Mask Dilation: Expands the restoration area to blend better with hair and backgrounds."
                    )
                }
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Model Selection
                Text("Target Model", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("face_yolov8n", "hand_yolov8n", "person_yolov8s-seg").forEach { m ->
                        FilterChip(
                            selected = adetailerModel == m,
                            onClick = { adetailerModel = m },
                            label = { 
                                Text(
                                    when(m) {
                                        "face_yolov8n" -> "Face"
                                        "hand_yolov8n" -> "Hand"
                                        else -> "Person"
                                    }, fontSize = 11.sp
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Confidence Threshold
                Text("Detection Confidence: ${(detectionConfidence * 100).toInt()}%", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = detectionConfidence,
                    onValueChange = { detectionConfidence = (it * 100).roundToInt() / 100f },
                    valueRange = 0.0f..1.0f,
                    steps = 99
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Denoising Strength (Fidelity)
                // Note: We use 1.0 - fidelity to match ADetailer's "Denoising Strength" terminology
                Text("Inpaint Denoising Strength: ${"%.2f".format(1.0f - codeFormerFidelity)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = 1.0f - codeFormerFidelity,
                    onValueChange = { codeFormerFidelity = 1.0f - ((it * 100).roundToInt() / 100f) },
                    valueRange = 0f..1f,
                    steps = 99
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Mask Dilation
                Text("Mask Erosion (-) / Dilation (+): $maskDilation", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = maskDilation.toFloat(),
                    onValueChange = { maskDilation = it.roundToInt() },
                    valueRange = -20f..20f,
                    steps = 40
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Merge Mode
                Text("Mask Merge Mode", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Sequential", "Batch").forEach { mode ->
                        FilterChip(
                            selected = maskMergeMode == mode,
                            onClick = { maskMergeMode = mode },
                            label = { Text(mode, fontSize = 11.sp) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Upscale Face Patch", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Switch(checked = codeFormerUpscaleFace, onCheckedChange = { codeFormerUpscaleFace = it })
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── RealESRGAN - Upscale ──
            ModelSection(
                title = "RealESRGAN  —  Upscale",
                enabled = upscalingEnabled,
                onToggle = { upscalingEnabled = it },
                infoTitle = "RealESRGAN — Upscale",
                infoText = {
                    Text(
                        "RealESRGAN increases image resolution using AI.\n\n" +
                        "2x — doubles the image dimensions, good balance of quality and speed\n\n" +
                        "4x — quadruples dimensions, significantly sharper but slower and uses more memory"
                    )
                }
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Scale",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 4).forEach { scale ->
                        FilterChip(
                            selected = upscaleScale == scale,
                            onClick = { upscaleScale = scale },
                            label = { Text("${scale}x") }
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            TextButton(
                onClick = {
                    denoisingEnabled = true
                    scunetStrength = 0.5f
                    scunetAdvancedNoiseRemoval = false
                    colorizeEnabled = true
                    vibrancy = 1.0f
                    upscalingEnabled = false
                    upscaleScale = 2
                    faceRestoreEnabled = false
                    codeFormerFidelity = 0.7f
                    adetailerModel = "face_yolov8n"
                    detectionConfidence = 0.3f
                    maskDilation = 0
                    maskMergeMode = "Sequential"
                    codeFormerUpscaleFace = false
                    codeFormerUpscaleBackground = false
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    "Reset to Defaults",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ModelSection(
    title: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    infoTitle: String,
    infoText: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    var showInfo by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.width(2.dp))
                IconButton(
                    onClick = { showInfo = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info about $title",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle
            )
        }
        if (enabled) {
            content()
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(infoTitle) },
            text = infoText,
            confirmButton = {
                TextButton(onClick = { showInfo = false }) { Text("Got it") }
            }
        )
    }
}
