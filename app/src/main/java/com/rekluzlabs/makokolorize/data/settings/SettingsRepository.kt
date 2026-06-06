package com.rekluzlabs.makokolorize.data.settings

import android.content.Context
import android.graphics.Bitmap

class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var outputFormat: Bitmap.CompressFormat
        get() = when (prefs.getString(KEY_OUTPUT_FORMAT, "jpeg")) {
            "png" -> Bitmap.CompressFormat.PNG
            else -> Bitmap.CompressFormat.JPEG
        }
        set(value) = prefs.edit().putString(
            KEY_OUTPUT_FORMAT,
            if (value == Bitmap.CompressFormat.PNG) "png" else "jpeg"
        ).apply()

    var jpegQuality: Int
        get() = prefs.getInt(KEY_JPEG_QUALITY, 95).coerceIn(1, 100)
        set(value) = prefs.edit().putInt(KEY_JPEG_QUALITY, value.coerceIn(1, 100)).apply()

    var keepScreenAwake: Boolean
        get() = prefs.getBoolean(KEY_KEEP_AWAKE, false)
        set(value) = prefs.edit().putBoolean(KEY_KEEP_AWAKE, value).apply()

    var saveOriginalAlongsideResult: Boolean
        get() = prefs.getBoolean(KEY_SAVE_ORIGINAL, false)
        set(value) = prefs.edit().putBoolean(KEY_SAVE_ORIGINAL, value).apply()

    var tileSizeOverride: Int
        get() = prefs.getInt(KEY_TILE_SIZE, 0).coerceAtLeast(0)
        set(value) = prefs.edit().putInt(KEY_TILE_SIZE, value.coerceAtLeast(0)).apply()

    companion object {
        private const val PREFS_NAME = "makokolorize_settings"
        private const val KEY_OUTPUT_FORMAT = "output_format"
        private const val KEY_JPEG_QUALITY = "jpeg_quality"
        private const val KEY_KEEP_AWAKE = "keep_awake"
        private const val KEY_SAVE_ORIGINAL = "save_original"
        private const val KEY_TILE_SIZE = "tile_size"
    }
}
