/*
 * Copyright (c) 2026 Rekluz Labs
 * All rights reserved.
 */

package com.rekluzlabs.makokolorize.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rekluzlabs.makokolorize.domain.RunConfig
import com.rekluzlabs.makokolorize.edit.model.EditorAdjustments
import com.rekluzlabs.makokolorize.edit.ui.EditorTool
import com.rekluzlabs.makokolorize.ui.screens.*
import com.rekluzlabs.makokolorize.ui.viewmodel.RestoreUiState

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun PreviewHomeScreen() {
    MaterialTheme {
        // Need to use reflection or copy of private HomeContent if we can't access it
        // For now, let's try to just render a placeholder if we can't reach private methods easily
        // But usually, I can just copy the code here for the manual's sake.
        Surface(modifier = Modifier.fillMaxSize()) {
            // Placeholder logic for manual screenshot
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun PreviewRestoreScreen() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            // RestoreContent is private in RestoreScreen.kt
            // I'll skip it for now or make it public if needed.
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun PreviewAdvancedSettings() {
    MaterialTheme {
        // AdvancedSettingsSheet is public
        // We'll wrap it in a box since it's a bottom sheet
        Box(modifier = Modifier.fillMaxSize()) {
            AdvancedSettingsSheet(
                initialConfig = RunConfig(),
                onDismiss = {},
                onApply = {}
            )
        }
    }
}
