/*
 * Copyright (c) 2026 Rekluz Labs
 * All rights reserved.
 */

package com.rekluzlabs.makokolorize.edit.ui

import com.rekluzlabs.makokolorize.edit.model.EditorAdjustments

enum class EditorTool(val label: String) {
    BRIGHTNESS("Brightness"),
    CONTRAST("Contrast"),
    SATURATION("Saturation"),
    WARMTH("Warmth"),
    TINT("Tint"),
    HIGHLIGHTS("Highlights"),
    SHADOWS("Shadows"),
    SHARPNESS("Sharpness"),
    CROP("Crop")
}

fun EditorAdjustments.sliderBinding(tool: EditorTool): Triple<Float, ClosedFloatingPointRange<Float>, (Float) -> EditorAdjustments> = when (tool) {
    EditorTool.BRIGHTNESS  -> Triple(brightness, -1f..1f)  { copy(brightness = it) }
    EditorTool.CONTRAST    -> Triple(contrast - 1f, -0.5f..0.5f) { copy(contrast = 1f + it) }
    EditorTool.SATURATION  -> Triple(saturation - 1f, -1f..1f) { copy(saturation = 1f + it) }
    EditorTool.WARMTH      -> Triple(warmth, -1f..1f)  { copy(warmth = it) }
    EditorTool.TINT        -> Triple(tint, -1f..1f)    { copy(tint = it) }
    EditorTool.HIGHLIGHTS  -> Triple(highlights, -1f..0f) { copy(highlights = it) }
    EditorTool.SHADOWS     -> Triple(shadows, 0f..1f)  { copy(shadows = it) }
    EditorTool.SHARPNESS   -> Triple(sharpness, 0f..1f) { copy(sharpness = it) }
    EditorTool.CROP        -> Triple(0f, 0f..1f) { this }
}
