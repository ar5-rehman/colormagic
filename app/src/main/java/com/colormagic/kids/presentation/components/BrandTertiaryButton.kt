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

// Tertiary CTA — same pill + tactile press as the primary but in the
// brand pastel blue. Used for "Try Another", "Create Another", and other
// alternative actions that should still feel like an explicit decision.
@Composable
fun BrandTertiaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    height: Dp = 60.dp,
    edgeThickness: Dp = 6.dp,
    enabled: Boolean = true
) {
    val fill = if (enabled) Color(0xFF9FD8FA) else BrandTokens.SubtleSurface
    val edge = if (enabled) Color(0xFF4FA8C7) else BrandTokens.SubtleOutline
    val ink = if (enabled) Color(0xFF01579B) else BrandTokens.MutedInk

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
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = label,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                color = ink,
                textAlign = TextAlign.Center
            )
        }
    }
}
