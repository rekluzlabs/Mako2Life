package com.rekluzlabs.makokolorize.domain

data class RunConfig(
    val ddcolorInputSize: Int = 256,
    val vibrancy: Float = 1.0f,
    val upscalingEnabled: Boolean = false,
    val faceRestoreEnabled: Boolean = false,
    val denoisingEnabled: Boolean = false,
    val colorizeEnabled: Boolean = true,
    
    // CodeFormer / ADetailer Settings
    val codeFormerFidelity: Float = 0.7f,
    val codeFormerUpscaleFace: Boolean = false,
    val codeFormerUpscaleBackground: Boolean = false,
    val adetailerModel: String = "face_yolov8n",
    val detectionConfidence: Float = 0.3f,
    val maskDilation: Int = 0,
    val mlKitAccurateMode: Boolean = true,
    val mlKitMinFaceSize: Float = 0.1f,
    
    val scunetStrength: Float = 1.0f,
    val scunetAdvancedNoiseRemoval: Boolean = false,
    val scunetFastMode: Boolean = false,
    val scunetDownsample: Boolean = false,
    val scunetBeforeUpscale: Boolean = false,
    val upscaleScale: Int = 2,

    // New Processing Settings
    val autoLevelsEnabled: Boolean = true,
    val autoLevelsStrength: Float = 1.0f,
    val contrastBoost: Float = 1.0f,
    val adaptiveSCUNetEnabled: Boolean = true,
    val scunetStrengthOverride: Float? = null,
    val skipSecondSCUNetPass: Boolean = false,
    val grayscaleBlendStrength: Float = 0.85f,
    val saturationBoost: Float = 1.0f,
    val warmthBias: Float = 0.0f
) {
    fun withFullRestore() = copy(
        denoisingEnabled = true,
        colorizeEnabled = true,
        upscalingEnabled = true,
        faceRestoreEnabled = true,
        scunetStrength = 1.0f,
        vibrancy = 1.0f,
        codeFormerFidelity = 0.7f,
        scunetAdvancedNoiseRemoval = false,
        upscaleScale = 2
    )

    fun withColorizeOnly() = copy(
        denoisingEnabled = false,
        colorizeEnabled = true,
        upscalingEnabled = false,
        faceRestoreEnabled = false,
        vibrancy = 1.0f,
        ddcolorInputSize = 512
    )

    fun withUpscaleOnly() = copy(
        denoisingEnabled = false,
        colorizeEnabled = false,
        upscalingEnabled = true,
        faceRestoreEnabled = false,
        upscaleScale = 2
    )

    fun withDefaults() = RunConfig()
}
