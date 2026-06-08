/*
 * Copyright (c) 2026 Rekluz Labs
 * All rights reserved.
 */

package com.rekluzlabs.makokolorize.processor

data class ProcessingSettings(
    val autoLevelsEnabled: Boolean = true,
    val autoLevelsStrength: Float = 1.0f,
    val contrastBoost: Float = 1.0f,
    val adaptiveSCUNetEnabled: Boolean = true,
    val scunetStrengthOverride: Float? = null,
    val skipSecondSCUNetPass: Boolean = false,
    val grayscaleBlendStrength: Float = 0.85f,
    val saturationBoost: Float = 1.0f,
    val warmthBias: Float = 0.0f
)
