package com.colormagic.kids.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Magic Pastel Tactile surface — a pill / card with a darker visible bottom band
 * that simulates a soft 3D press affordance. The [fill] face sits on top, the
 * [edge] face sticks out below it by [edgeThickness].
 *
 * Total laid-out height = [height] + [edgeThickness].
 */
@Composable
fun TactileSurface(
    onClick: () -> Unit,
    fill: Color,
    edge: Color,
    shape: Shape,
    height: Dp,
    modifier: Modifier = Modifier,
    edgeThickness: Dp = 6.dp,
    contentColor: Color = LocalContentColor.current,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.height(height + edgeThickness)) {
        // Darker back face — its visible portion is the band sticking out below the front face.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .offset(y = edgeThickness)
                .clip(shape)
                .background(edge)
        )
        // Front face — the actual interactive surface.
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = shape,
            color = fill,
            contentColor = contentColor,
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
                content = content
            )
        }
    }
}
