package com.colormagic.kids.presentation.components

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colormagic.kids.domain.model.BrushSize

// === ToolButton ===
// Square rounded button used in the coloring tool row (Brush / Eraser / Undo).
// Selected state inverts to brand primary so it visually wins the focus.
@Composable
fun ToolButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fill = if (selected) MaterialTheme.colorScheme.primary else BrandTokens.SubtleSurface
    val ink = if (selected) Color.White else BrandTokens.HeadingInk
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = fill,
        modifier = modifier
            .size(width = 92.dp, height = 76.dp)
            .border(2.dp, borderColor, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ink,
                modifier = Modifier.size(26.dp)
            )
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
