package com.colormagic.kids.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val PillColor = Color(0xFFC8B6E8) // calibrated medium lavender from the mock
private val UnselectedInk = Color(0xFF6F6E76)
private val SelectedInk = Color(0xFF311B92) // deep lavender ink for selected label

@Composable
fun BrandBottomBar(
    selected: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopLevelDestination.entries.forEach { destination ->
                BrandBottomItem(
                    destination = destination,
                    selected = destination == selected,
                    onClick = { onSelect(destination) }
                )
            }
        }
    }
}

@Composable
private fun BrandBottomItem(
    destination: TopLevelDestination,
    selected: Boolean,
    onClick: () -> Unit
) {
    val pillShape = RoundedCornerShape(50)
    val container = if (selected) PillColor else Color.Transparent
    val ink = if (selected) SelectedInk else UnselectedInk
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .clip(pillShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .background(container, pillShape)
            .padding(horizontal = 22.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
            contentDescription = destination.label,
            tint = ink,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = ink
        )
    }
}

@Composable
fun BrandNavRail(
    selected: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 24.dp, horizontal = 12.dp)
                .width(96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopLevelDestination.entries.forEach { destination ->
                BrandBottomItem(
                    destination = destination,
                    selected = destination == selected,
                    onClick = { onSelect(destination) }
                )
            }
        }
    }
}
