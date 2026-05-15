package com.colormagic.kids.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colormagic.kids.presentation.components.BrandTokens

// Tablet-only top navigation: brand wordmark on the left, top-level
// destinations as text tabs in the middle, profile + settings circle
// affordances on the right. Replaces both the bottom bar and nav rail
// when the window is wider than compact.
@Composable
fun BrandTopNavBar(
    selected: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
    onProfile: () -> Unit = {},
    onSettings: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ColorMagic Kids",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = MaterialTheme.typography.headlineLarge.fontFamily
            )

            Spacer(Modifier.weight(1f))

            TopLevelDestination.entries.forEach { destination ->
                NavTab(
                    label = destination.label,
                    selected = destination == selected,
                    onClick = { onSelect(destination) }
                )
                Spacer(Modifier.width(8.dp))
            }

            Spacer(Modifier.weight(1f))

            CircleAction(
                icon = Icons.Filled.Person,
                contentDescription = "Profile",
                onClick = onProfile,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                background = MaterialTheme.colorScheme.primaryContainer
            )
            Spacer(Modifier.width(10.dp))
            CircleAction(
                icon = Icons.Filled.Settings,
                contentDescription = "Settings",
                onClick = onSettings,
                tint = BrandTokens.HeadingInk,
                background = BrandTokens.SubtleSurface
            )
        }
    }
}

@Composable
private fun NavTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val ink = if (selected) MaterialTheme.colorScheme.primary else BrandTokens.MutedInk
    Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
        Text(
            text = label,
            fontSize = 17.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = ink,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun CircleAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color,
    background: Color
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = background,
        modifier = Modifier.size(40.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
