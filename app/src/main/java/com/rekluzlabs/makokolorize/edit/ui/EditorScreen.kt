/*
 * Copyright (c) 2026 Rekluz Labs
 * All rights reserved.
 */

package com.rekluzlabs.makokolorize.edit.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rekluzlabs.makokolorize.edit.model.CropRect
import com.rekluzlabs.makokolorize.edit.model.EditorAdjustments
import com.rekluzlabs.makokolorize.edit.viewmodel.EditorViewModel

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onSave: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    val adj by viewModel.adjustments.collectAsStateWithLifecycle()
    val preview by viewModel.previewOutput.collectAsStateWithLifecycle()
    val uncroppedPreview by viewModel.uncroppedPreviewOutput.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    var activeTool by remember { mutableStateOf(EditorTool.SATURATION) }

    var showConfirmDialog by remember { mutableStateOf(false) }

    BackHandler { onDismiss() }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        EditorTopBar(
            canUndo = viewModel.canUndo(),
            onUndo = { viewModel.undo() },
            onReset = { 
                viewModel.reset()
                scale = 1f
                offset = Offset.Zero
            },
            onDismiss = onDismiss
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
                .clipToBounds()
                .pointerInput(activeTool) {
                    // Disable zoom gestures while cropping to avoid conflicts
                    if (activeTool != EditorTool.CROP) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                val maxOffsetX = (size.width * (scale - 1)) / 2
                                val maxOffsetY = (size.height * (scale - 1)) / 2
                                offset = Offset(
                                    x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                    y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                )
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val displayBitmap = if (activeTool == EditorTool.CROP) uncroppedPreview else preview
            
            displayBitmap?.let { bmp ->
                var imageSize by remember { mutableStateOf(IntSize.Zero) }
                
                Box(
                    modifier = Modifier
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                        .aspectRatio(bmp.width.toFloat() / bmp.height.toFloat())
                        .fillMaxSize()
                        .onGloballyPositioned { imageSize = it.size }
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    if (activeTool == EditorTool.CROP && imageSize != IntSize.Zero) {
                        CropOverlay(
                            cropRect = adj.cropRect,
                            onCropRectChange = { viewModel.updateAdjustments(adj.copy(cropRect = it)) },
                            onDragEnd = { viewModel.commitToUndoStack() }
                        )
                    }
                }
            }
            if (isSaving) {
                CircularProgressIndicator()
            }
        }

        EditorToolbar(
            activeTool = activeTool,
            adj = adj,
            onToolSelected = { activeTool = it },
            onAdjChange = { viewModel.updateAdjustments(it) },
            onSliderDragEnd = { viewModel.commitToUndoStack() }
        )

        EditorBottomBar(
            isSaving = isSaving,
            onSave = { showConfirmDialog = true }
        )
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Save Changes") },
            text = { Text("Do you want to apply these edits to your photo?") },
            confirmButton = {
                Button(onClick = {
                    showConfirmDialog = false
                    viewModel.saveResult(onSave)
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("No")
                }
            }
        )
    }
}

@Composable
private fun CropOverlay(
    cropRect: CropRect,
    onCropRectChange: (CropRect) -> Unit,
    onDragEnd: () -> Unit
) {
    // Crucial: Use updated state so the drag listener always sees the latest values
    val currentCropRect by rememberUpdatedState(cropRect)
    val currentOnChange by rememberUpdatedState(onCropRectChange)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = { currentOnDragEnd() },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val dx = dragAmount.x / size.width
                            val dy = dragAmount.y / size.height
                            
                            val touchX = change.position.x / size.width
                            val touchY = change.position.y / size.height
                            
                            // Hit area threshold in normalized coordinates
                            // Increased significantly to 0.25f (25% of image width/height) for easier grabbing
                            val threshold = 0.25f
                            
                            val isLeft = touchX < currentCropRect.left + threshold
                            val isRight = touchX > currentCropRect.right - threshold
                            val isTop = touchY < currentCropRect.top + threshold
                            val isBottom = touchY > currentCropRect.bottom - threshold
                            
                            var newRect = currentCropRect
                            
                            // Handle corner priority for better diagonal dragging
                            if (isLeft && isTop) {
                                newRect = newRect.copy(
                                    left = (newRect.left + dx).coerceIn(0f, newRect.right - 0.1f),
                                    top = (newRect.top + dy).coerceIn(0f, newRect.bottom - 0.1f)
                                )
                            } else if (isRight && isTop) {
                                newRect = newRect.copy(
                                    right = (newRect.right + dx).coerceIn(newRect.left + 0.1f, 1f),
                                    top = (newRect.top + dy).coerceIn(0f, newRect.bottom - 0.1f)
                                )
                            } else if (isLeft && isBottom) {
                                newRect = newRect.copy(
                                    left = (newRect.left + dx).coerceIn(0f, newRect.right - 0.1f),
                                    bottom = (newRect.bottom + dy).coerceIn(newRect.top + 0.1f, 1f)
                                )
                            } else if (isRight && isBottom) {
                                newRect = newRect.copy(
                                    right = (newRect.right + dx).coerceIn(newRect.left + 0.1f, 1f),
                                    bottom = (newRect.bottom + dy).coerceIn(newRect.top + 0.1f, 1f)
                                )
                            } else {
                                // Single edge or move
                                if (isLeft && !isRight) newRect = newRect.copy(left = (newRect.left + dx).coerceIn(0f, newRect.right - 0.1f))
                                if (isRight && !isLeft) newRect = newRect.copy(right = (newRect.right + dx).coerceIn(newRect.left + 0.1f, 1f))
                                if (isTop && !isBottom) newRect = newRect.copy(top = (newRect.top + dy).coerceIn(0f, newRect.bottom - 0.1f))
                                if (isBottom && !isTop) newRect = newRect.copy(bottom = (newRect.bottom + dy).coerceIn(newRect.top + 0.1f, 1f))
                                
                                if (!isLeft && !isRight && !isTop && !isBottom) {
                                    val moveX = dx.coerceIn(-newRect.left, 1f - newRect.right)
                                    val moveY = dy.coerceIn(-newRect.top, 1f - newRect.bottom)
                                    newRect = newRect.copy(
                                        left = newRect.left + moveX,
                                        right = newRect.right + moveX,
                                        top = newRect.top + moveY,
                                        bottom = newRect.bottom + moveY
                                    )
                                }
                            }
                            
                            currentOnChange(newRect)
                        }
                    )
                }
        ) {
            val rect = Rect(
                currentCropRect.left * size.width,
                currentCropRect.top * size.height,
                currentCropRect.right * size.width,
                currentCropRect.bottom * size.height
            )

            // Dim outside
            drawRect(color = Color.Black.copy(alpha = 0.5f))
            
            // Clear inside (using blend mode)
            // Actually easier to just draw 4 rects around
            drawRect(Color.Black.copy(alpha = 0.5f), size = androidx.compose.ui.geometry.Size(size.width, rect.top)) // Top
            drawRect(Color.Black.copy(alpha = 0.5f), topLeft = Offset(0f, rect.bottom), size = androidx.compose.ui.geometry.Size(size.width, size.height - rect.bottom)) // Bottom
            drawRect(Color.Black.copy(alpha = 0.5f), topLeft = Offset(0f, rect.top), size = androidx.compose.ui.geometry.Size(rect.left, rect.bottom - rect.top)) // Left
            drawRect(Color.Black.copy(alpha = 0.5f), topLeft = Offset(rect.right, rect.top), size = androidx.compose.ui.geometry.Size(size.width - rect.right, rect.bottom - rect.top)) // Right

            // Border
            drawRect(
                color = Color.White,
                topLeft = rect.topLeft,
                size = rect.size,
                style = Stroke(width = 2.dp.toPx())
            )
            
            // Corner handles - LARGE
            val handleSize = 24.dp.toPx() 
            val handleColor = Color.White
            
            fun drawHandle(center: Offset) {
                // Outer circle for visibility
                drawCircle(Color.Black.copy(alpha = 0.3f), radius = handleSize / 2 + 1.dp.toPx(), center = center)
                // Main handle
                drawCircle(handleColor, radius = handleSize / 2, center = center)
            }
            
            // TL, TR, BL, BR
            drawHandle(rect.topLeft)
            drawHandle(rect.topRight)
            drawHandle(rect.bottomLeft)
            drawHandle(rect.bottomRight)
        }
    }
}

private fun Offset.toSize() = androidx.compose.ui.geometry.Size(x, y)

@Composable
private fun EditorTopBar(
    canUndo: Boolean,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
        Text(
            text = "MAKO Editor",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        IconButton(onClick = onUndo, enabled = canUndo) {
            Icon(Icons.Default.Undo, contentDescription = "Undo")
        }
        TextButton(onClick = onReset) {
            Text("Reset")
        }
    }
}

@Composable
private fun EditorToolbar(
    activeTool: EditorTool,
    adj: EditorAdjustments,
    onToolSelected: (EditorTool) -> Unit,
    onAdjChange: (EditorAdjustments) -> Unit,
    onSliderDragEnd: () -> Unit
) {
    val tools = EditorTool.entries
    val activeIndex = tools.indexOf(activeTool)

    Column(modifier = Modifier.fillMaxWidth()) {
        ScrollableTabRow(
            selectedTabIndex = activeIndex,
            edgePadding = 12.dp
        ) {
            tools.forEach { tool ->
                Tab(
                    selected = activeTool == tool,
                    onClick = { onToolSelected(tool) },
                    text = { Text(tool.label, style = MaterialTheme.typography.labelMedium) }
                )
            }
        }

        if (activeTool != EditorTool.CROP) {
            val (value, range, getUpdatedAdjustment) = adj.sliderBinding(activeTool)

            Slider(
                value = value,
                onValueChange = { newValue ->
                    onAdjChange(getUpdatedAdjustment(newValue))
                },
                valueRange = range,
                onValueChangeFinished = onSliderDragEnd,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            )
        } else {
            // Spacer to keep the toolbar height consistent
            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

@Composable
private fun EditorBottomBar(isSaving: Boolean, onSave: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Button(
            onClick = onSave,
            enabled = !isSaving,
            modifier = Modifier.height(48.dp)
        ) {
            Text("Confirm")
        }
    }
}
