package com.rekluzlabs.makokolorize.ui.screens

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.DisposableEffect
import coil.compose.AsyncImage
import com.rekluzlabs.makokolorize.data.image.ImageRepository
import com.rekluzlabs.makokolorize.domain.ColorizeUseCase
import com.rekluzlabs.makokolorize.domain.RunConfig
import com.rekluzlabs.makokolorize.ui.viewmodel.RestoreEffect
import com.rekluzlabs.makokolorize.ui.viewmodel.RestoreUiEvent
import com.rekluzlabs.makokolorize.ui.viewmodel.RestoreUiState
import com.rekluzlabs.makokolorize.ui.viewmodel.RestoreViewModel
import com.rekluzlabs.makokolorize.ui.viewmodel.RestoreViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreRoute(
    imageUri: Uri,
    onResultReady: (Uri) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val owner = context as ComponentActivity
    val factory = remember(imageUri) {
        RestoreViewModelFactory(
            imageRepository = ImageRepository(context),
            colorizeUseCase = ColorizeUseCase(context),
            imageUri = imageUri
        )
    }
    @Suppress("UNCHECKED_CAST")
    val viewModel = remember(owner, imageUri) {
        ViewModelProvider(owner, factory).get(
            "restore_${imageUri}", RestoreViewModel::class.java
        ) as RestoreViewModel
    }
    val state by viewModel.state.collectAsState()

    // Keep screen awake during processing
    val currentView = LocalView.current
    DisposableEffect(state.isProcessing) {
        if (state.isProcessing) {
            currentView.keepScreenOn = true
        }
        onDispose {
            currentView.keepScreenOn = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is RestoreEffect.NavigateToResult -> onResultReady(effect.resultUri)
            }
        }
    }

    RestoreContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RestoreContent(
    state: RestoreUiState,
    onEvent: (RestoreUiEvent) -> Unit,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Photo preview - top ~60%
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f),
                contentAlignment = Alignment.Center
            ) {
                if (state.error != null) {
                    Text(
                        state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    AsyncImage(
                        model = state.imageUri,
                        contentDescription = "Selected photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // Bottom panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Auto Button Full Restore
                Button(
                    onClick = { onEvent(RestoreUiEvent.OneButtonFullRestore) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "Auto Restore",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "To use Auto Restore, choose any one or all of the options below then click Restore.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Individual model toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    FilterChip(
                        selected = state.config.denoisingEnabled,
                        onClick = { onEvent(RestoreUiEvent.ToggleDenoise) },
                        label = { Text("Denoise", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1976D2),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = state.config.colorizeEnabled,
                        onClick = { onEvent(RestoreUiEvent.ToggleColorize) },
                        label = { Text("Colorize", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1976D2),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = state.config.faceRestoreEnabled,
                        onClick = { onEvent(RestoreUiEvent.ToggleFaceRestore) },
                        label = { Text("Face Restore", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1976D2),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = state.config.upscalingEnabled,
                        onClick = { onEvent(RestoreUiEvent.ToggleUpscale) },
                        label = { Text("Upscale", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1976D2),
                            selectedLabelColor = Color.White
                        )
                    )
                }

                HorizontalDivider()

                // Back + Advanced + Restore buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onBack) {
                        Text(
                            "Back",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    OutlinedButton(onClick = { onEvent(RestoreUiEvent.ShowAdvancedSheet) }) {
                        Text(
                            "Advanced",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Open advanced settings",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Button(
                        onClick = { onEvent(RestoreUiEvent.ClickRestore) },
                        enabled = !state.isProcessing,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(42.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            "Restore",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Processing overlay
        if (state.isProcessing) {
            ProcessingOverlay(
                progress = state.progress,
                config = state.config,
                onCancel = { onEvent(RestoreUiEvent.CancelProcessing) }
            )
        }
    }

    if (state.showAdvancedSheet) {
        AdvancedSettingsSheet(
            initialConfig = state.config,
            onDismiss = { onEvent(RestoreUiEvent.DismissAdvancedSheet) },
            onApply = { config -> onEvent(RestoreUiEvent.ApplyAdvancedConfig(config)) }
        )
    }
}

private data class ModelStep(
    val name: String,
    val subtitle: String,
    val enabled: Boolean
)

private enum class StepState {
    WAITING, PROCESSING, COMPLETE
}

@Composable
private fun ProcessingOverlay(
    progress: Float,
    config: RunConfig,
    onCancel: () -> Unit
) {
    val steps = remember(config) {
        listOf(
            ModelStep("SCUNet", "Denoise", config.denoisingEnabled),
            ModelStep("CodeFormer", "Face Restore", config.faceRestoreEnabled),
            ModelStep("DDColor", "Colorize", config.colorizeEnabled),
            ModelStep("RealESRGAN", "Upscale", config.upscalingEnabled)
        )
    }

    val enabledSteps = steps.filter { it.enabled }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Restoring your photo\nplease be patient\u2026",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                steps.forEach { step ->
                    val status = if (!step.enabled) StepState.COMPLETE
                    else {
                        val idx = enabledSteps.indexOf(step)
                        val total = enabledSteps.size
                        val stepStart = idx.toFloat() / total
                        val stepEnd = (idx + 1).toFloat() / total
                        when {
                            progress >= stepEnd -> StepState.COMPLETE
                            progress >= stepStart -> StepState.PROCESSING
                            else -> StepState.WAITING
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (status) {
                            StepState.COMPLETE -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Complete",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            StepState.PROCESSING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            StepState.WAITING -> {
                                Icon(
                                    imageVector = Icons.Outlined.Circle,
                                    contentDescription = "Waiting",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = step.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (status == StepState.WAITING)
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = step.subtitle,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = if (status == StepState.WAITING) 0.3f else 0.6f
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (progress >= 0f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // Thermal warning state
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Device is Overheating",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Pausing to let your phone cool down...",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(onClick = onCancel) {
                    Text("Cancel")
                }
            }
        }
    }
}
