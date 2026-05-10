package com.colormagic.kids.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Brand primary CTA. Pill-shaped, tactile (lavender front + deeper edge),
// optionally takes a leading icon. Used for the main action on every screen.
// When [enabled] is false, the surfaces drop to a muted grey palette and clicks no-op.
@Composable
fun BrandPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    height: Dp = 64.dp,
    edgeThickness: Dp = 8.dp,
    enabled: Boolean = true
) {
    val fill = if (enabled) MaterialTheme.colorScheme.primary else BrandTokens.SubtleSurface
    val edge = if (enabled) BrandTokens.PrimaryEdge else BrandTokens.SubtleOutline
    val ink = if (enabled) Color.White else BrandTokens.MutedInk

    TactileSurface(
        onClick = onClick,
        enabled = enabled,
        fill = fill,
        edge = edge,
        shape = RoundedCornerShape(50),
        height = height,
        edgeThickness = edgeThickness,
        contentColor = ink,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = ink,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(Modifier.width(12.dp))
            }
            Text(
                text = label,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
                color = ink,
                textAlign = TextAlign.Center
            )
        }
    }
}
