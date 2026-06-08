/*
 * Copyright (c) 2026 Rekluz Labs
 * All rights reserved.
 */

package com.rekluzlabs.makokolorize.edit.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rekluzlabs.makokolorize.edit.model.EditorAdjustments
import com.rekluzlabs.makokolorize.edit.processor.ImageAdjustmentProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditorViewModel(
    private val processor: ImageAdjustmentProcessor
) : ViewModel() {

    private lateinit var fullResBitmap: Bitmap
    private lateinit var previewBitmap: Bitmap

    private val _adjustments = MutableStateFlow(EditorAdjustments())
    val adjustments: StateFlow<EditorAdjustments> = _adjustments.asStateFlow()

    private val _previewOutput = MutableStateFlow<Bitmap?>(null)
    val previewOutput: StateFlow<Bitmap?> = _previewOutput.asStateFlow()

    private val _uncroppedPreviewOutput = MutableStateFlow<Bitmap?>(null)
    val uncroppedPreviewOutput: StateFlow<Bitmap?> = _uncroppedPreviewOutput.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val undoStack = ArrayDeque<EditorAdjustments>()

    fun init(bitmap: Bitmap) {
        fullResBitmap = bitmap
        val scale = 800f / maxOf(bitmap.width, bitmap.height)
        previewBitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else {
            bitmap
        }
        
        undoStack.clear()
        undoStack.addLast(EditorAdjustments())
        renderPreview(EditorAdjustments())
    }

    fun updateAdjustments(adj: EditorAdjustments) {
        _adjustments.value = adj
        renderPreview(adj)
    }

    fun commitToUndoStack() {
        undoStack.addLast(_adjustments.value)
        if (undoStack.size > 20) undoStack.removeFirst()
    }

    fun undo() {
        if (undoStack.size > 1) {
            undoStack.removeLast()
            val prev = undoStack.last()
            _adjustments.value = prev
            renderPreview(prev)
        }
    }

    fun reset() {
        val reset = EditorAdjustments()
        undoStack.addLast(reset)
        _adjustments.value = reset
        renderPreview(reset)
    }

    fun canUndo() = undoStack.size > 1

    private fun renderPreview(adj: EditorAdjustments) {
        viewModelScope.launch {
            delay(60) // Debounce UI slider jitter
            val (cropped, uncropped) = withContext(Dispatchers.Default) {
                val cropped = processor.applyAdjustments(previewBitmap, adj)
                
                // Only render uncropped if we aren't already looking at it (i.e. if a crop is active)
                val uncropped = if (adj.cropRect.isDefault()) {
                    cropped
                } else {
                    processor.applyAdjustments(previewBitmap, adj.copy(cropRect = com.rekluzlabs.makokolorize.edit.model.CropRect()))
                }

                Pair(cropped, uncropped)
            }
            _previewOutput.value = cropped
            _uncroppedPreviewOutput.value = uncropped
        }
    }

    fun saveResult(onComplete: (Bitmap) -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = withContext(Dispatchers.Default) {
                processor.applyAdjustments(fullResBitmap, _adjustments.value)
            }
            _isSaving.value = false
            onComplete(result)
        }
    }

    class Factory(private val processor: ImageAdjustmentProcessor) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditorViewModel(processor) as T
        }
    }
}
