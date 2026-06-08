/*
 * Copyright (c) 2026 Rekluz Labs
 * All rights reserved.
 */

package com.rekluzlabs.makokolorize.util

import android.content.Context
import android.os.PowerManager
import android.util.Log

/**
 * Monitors device thermal status using Android PowerManager.
 * Helps prevent overheating during heavy AI workloads.
 */
class ThermalMonitor(context: Context) {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    /**
     * Returns the current thermal headroom.
     * 1.0 means the device is at its thermal limit before throttling.
     * Higher values mean the device is increasingly likely to throttle or shut down.
     */
    fun getThermalHeadroom(): Float {
        return try {
            // getThermalHeadroom(seconds) - we look at immediate state (0)
            powerManager.getThermalHeadroom(0)
        } catch (e: Exception) {
            Log.w("ThermalMonitor", "Thermal API not supported on this device", e)
            0f
        }
    }

    /**
     * Severity levels for thermal state.
     */
    enum class ThermalState {
        NORMAL,     // Headroom < 0.7
        WARNING,    // Headroom 0.7 - 0.9
        CRITICAL    // Headroom > 0.9
    }

    fun getCurrentState(): ThermalState {
        val headroom = getThermalHeadroom()
        Log.d("ThermalMonitor", "Current thermal headroom: $headroom")
        return when {
            headroom > 0.9f -> ThermalState.CRITICAL
            headroom > 0.7f -> ThermalState.WARNING
            else -> ThermalState.NORMAL
        }
    }

    fun isCritical(): Boolean = getCurrentState() == ThermalState.CRITICAL
}
