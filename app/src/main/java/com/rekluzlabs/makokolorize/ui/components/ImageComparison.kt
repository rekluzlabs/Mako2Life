package com.rekluzlabs.makokolorize.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun ImageComparison(
    beforeUri: Uri?,
    afterUri: Uri?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var dividerPosition by remember { mutableFloatStateOf(0.5f) }

    // Use afterUri as the primary source for aspect ratio, as it might be cropped
    val imageUri = afterUri ?: beforeUri
    val aspectRatio by produceState(1f, imageUri) {
        val uri = imageUri ?: return@produceState
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(input, null, opts)
                if (opts.outWidth > 0 && opts.outHeight > 0) {
                    value = opts.outWidth.toFloat() / opts.outHeight.toFloat()
                }
            }
        } catch (_: Exception) {}
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
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
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .aspectRatio(aspectRatio)
        ) {
            afterUri?.let {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(it)
                        .crossfade(true)
                        .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                        .diskCachePolicy(coil.request.CachePolicy.DISABLED)
                        .build(),
                    contentDescription = "After",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            beforeUri?.let {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            val splitX = size.width * dividerPosition
                            clipRect(right = splitX) {
                                this@drawWithContent.drawContent()
                            }
                        }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(it)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Before",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    PillLabel(
                        text = "Before",
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = 12.dp, y = 12.dp)
                    )
                }
            }

            if (afterUri != null) {
                PillLabel(
                    text = "After",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-12).dp, y = 12.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            // When zoomed in, we only allow moving the divider if not panning? 
                            // Actually, let's keep it simple: 1-finger drag always moves divider.
                            // detectTransformGestures usually requires 2 fingers for zoom.
                            change.consume()
                            val width = size.width.toFloat()
                            dividerPosition = ((dividerPosition * width + dragAmount) / width)
                                .coerceIn(0f, 1f)
                        }
                    }
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val splitX = size.width * dividerPosition
                val h = size.height
                drawLine(
                    color = Color.White,
                    start = Offset(splitX, 0f),
                    end = Offset(splitX, h),
                    strokeWidth = 3f
                )
                val handleRadius = 16f
                drawCircle(
                    color = Color.White,
                    radius = handleRadius,
                    center = Offset(splitX, h / 2f)
                )
                drawCircle(
                    color = Color(0xFF666666),
                    radius = handleRadius - 3f,
                    center = Offset(splitX, h / 2f)
                )
            }
        }
    }
}

@Composable
private fun PillLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}
