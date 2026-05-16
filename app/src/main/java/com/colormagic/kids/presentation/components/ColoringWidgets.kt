package com.colormagic.kids.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colormagic.kids.domain.model.BrushSize

// === ToolButton ===
// Square rounded button used in the coloring tool/action rows.
// Three visual states:
//   • selected  → filled brand primary (loud, this is the active tool)
//   • enabled   → grey neutral (tap target, but quiet)
//   • disabled  → muted ink + container so the kid sees "nothing to do here"
//
// Two icon flavours:
//   ToolButton(icon: ImageVector)  → Material icon, tinted to the ink colour.
//                                    Used for action tools (Fill / Eraser /
//                                    Undo / Redo).
//   ToolButton(iconPainter: Painter) → full-colour drawable, NOT tinted.
//                                      Used for the 3D brush art so the
//                                      gradients survive.
@Composable
fun ToolButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    ToolButtonShell(
        label = label,
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier
    ) { ink ->
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ink,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
fun ToolButton(
    label: String,
    iconPainter: Painter,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    ToolButtonShell(
        label = label,
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier
    ) { _ ->
        // No tint — the 3D brush icons carry their own gradients/colours.
        // Slightly larger than a Material icon so the artwork detail reads.
        Image(
            painter = iconPainter,
            contentDescription = null,
            modifier = Modifier.size(38.dp)
        )
    }
}

@Composable
private fun ToolButtonShell(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (inkColor: Color) -> Unit
) {
    val fill = when {
        selected -> MaterialTheme.colorScheme.primary
        !enabled -> BrandTokens.SubtleSurface.copy(alpha = 0.55f)
        else -> BrandTokens.SubtleSurface
    }
    val ink = when {
        selected -> Color.White
        !enabled -> BrandTokens.MutedInk.copy(alpha = 0.45f)
        else -> BrandTokens.HeadingInk
    }
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        color = fill,
        modifier = modifier
            .size(width = 92.dp, height = 84.dp)
            .border(2.dp, borderColor, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            icon(ink)
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = ink
            )
        }
    }
}

// === ColorSwatch ===
// Circular swatch in the horizontal palette. Selected swatch gets a
// lavender focus ring.
@Composable
fun ColorSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val outer = 64.dp
    val inner = 50.dp
    Box(
        modifier = modifier.size(outer),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(outer)
                    .border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = color,
            modifier = Modifier.size(inner)
        ) {}
    }
}

// === BrushSizeDot ===
// Compact dot picker — the dot grows with the size step so the selector
// itself communicates the meaning.
@Composable
fun BrushSizeDot(
    size: BrushSize,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dotSize = when (size) {
        BrushSize.XSmall -> 8.dp
        BrushSize.Small -> 14.dp
        BrushSize.Medium -> 22.dp
        BrushSize.Large -> 28.dp
    }
    val dotColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)

    Box(
        modifier = modifier
            .size(44.dp)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = dotColor,
            modifier = Modifier.size(dotSize)
        ) {}
    }
}
