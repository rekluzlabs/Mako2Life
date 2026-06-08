package com.rekluzlabs.makokolorize.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    status: String? = null
) {
    // 1. Smoothly animates the progress bar matching the download stream
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300), // Smooths out jumps in network speed
        label = "progressPercentage"
    )

    // 2. Continuous pulsing animation loop for text graphics
    val infiniteTransition = rememberInfiniteTransition(label = "pulseText")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Customized high-visibility progress track
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp) // Thicker bar to stand out on screen
                .clip(RoundedCornerShape(8.dp)), // Smooth capsule corners
            color = MaterialTheme.colorScheme.primary, // Vibrant matching accent color
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) // Faded background track
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Pulsing live percentage calculation graphic
        Text(
            text = "${(progress * 100).toInt()}% Complete",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.alpha(pulseAlpha) // Applies the glowing pulse animation
        )

        if (status != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = status,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
