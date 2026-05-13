package com.colormagic.kids.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Header with a circular back affordance, a slotted title (callers control
// the typography for full flexibility), and an optional trailing slot.
// Use [centered] when the title should be horizontally centered between
// the back button and the trailing element (mirror-balanced).
@Composable
fun BrandTopBar(
    onBack: () -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    centered: Boolean = false,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircleBackButton(onClick = onBack)
        Spacer(Modifier.width(12.dp))
        if (centered) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) { title() }
            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                trailing?.invoke()
            }
        } else {
            Box(modifier = Modifier.weight(1f)) { title() }
            trailing?.invoke()
        }
    }
}

@Composable
private fun CircleBackButton(
    onClick: () -> Unit,
    tint: Color = BrandTokens.HeadingInk
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = BrandTokens.SubtleSurface,
        modifier = Modifier.size(40.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
