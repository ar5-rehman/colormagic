package com.colormagic.kids.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Pill-shaped progress bar with a brand pastel gradient and a sparkle that
// rides the leading edge. [progress] is clamped to 0f..1f.
@Composable
fun MagicProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 36.dp,
    trackColor: Color = Color(0xFFE6E2EC),
    gradientStart: Color = Color(0xFFB3E5FC), // pastel blue
    gradientEnd: Color = Color(0xFFB7E1B9),   // pastel green
    sparkleTint: Color = Color(0xFF1F1F1F)
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(clamped)
                .clip(RoundedCornerShape(50))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(gradientStart, gradientEnd)
                    )
                )
        ) {
            // Sparkle indicator pinned to the leading edge of the fill.
            if (clamped > 0.04f) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = sparkleTint,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp)
                        .size(18.dp)
                )
            }
        }
    }
}
