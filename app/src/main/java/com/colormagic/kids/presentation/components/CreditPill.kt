package com.colormagic.kids.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
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

// Compact informational pill — small badge-style chip used for status
// like "Sketches left: 3" or "Uses 1 sketch credit". Two variants:
//   Primary → lavender brand container (loud)
//   Subtle  → neutral grey (quieter, sub-actions)
@Composable
fun CreditPill(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Star,
    style: CreditPillStyle = CreditPillStyle.Primary
) {
    val container = when (style) {
        CreditPillStyle.Primary -> MaterialTheme.colorScheme.primaryContainer
        CreditPillStyle.Subtle -> BrandTokens.SubtleSurface
    }
    val ink = when (style) {
        CreditPillStyle.Primary -> MaterialTheme.colorScheme.onPrimaryContainer
        CreditPillStyle.Subtle -> BrandTokens.MutedInk
    }
    val iconBg = when (style) {
        CreditPillStyle.Primary -> MaterialTheme.colorScheme.primary
        CreditPillStyle.Subtle -> BrandTokens.MutedInk
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = container,
        modifier = modifier.height(34.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = ink
            )
        }
    }
}

enum class CreditPillStyle { Primary, Subtle }
