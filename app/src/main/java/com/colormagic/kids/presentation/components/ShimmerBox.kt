package com.colormagic.kids.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

// Animated placeholder used while a sketch image (or any wide tile) is
// still being fetched. Reads as a gentle pastel sweep — fits the brand,
// signals "we're getting it" without showing a misleading bundled image.
//
// Used by:
//   • SketchPreviewScreen — covers the area until Coil resolves the URL.
//   • SketchCanvas        — covers the canvas until the network bitmap +
//                           fillable mask are computed.
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    baseColor: Color = Color(0xFFEDE7F6),
    highlight: Color = Color(0xFFF7F3FE)
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer-progress"
    )

    // Sweep a soft highlight across a base pastel band. Using a fractional
    // gradient (0..1 normalised to a wide range) keeps the sweep speed
    // independent of the parent box's actual pixel size.
    val brush = Brush.linearGradient(
        colors = listOf(baseColor, highlight, baseColor),
        start = Offset(progress * 1000f, 0f),
        end = Offset(progress * 1000f + 600f, 0f)
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}
