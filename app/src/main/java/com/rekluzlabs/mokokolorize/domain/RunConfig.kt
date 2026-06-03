package com.rekluzlabs.makokolorize.domain

data class RunConfig(
    val ddcolorInputSize: Int = 512,
    val vibrancy: Float = 1.0f,
    val upscalingEnabled: Boolean = false,
    val faceRestoreEnabled: Boolean = false,
    val denoisingEnabled: Boolean = false,
    val codeFormerFidelity: Float = 0.5f
)
