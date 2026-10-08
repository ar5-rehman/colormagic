package com.colormagic.kids.presentation.adaptive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Side-by-side (two-pane) layouts only from Expanded width (840dp+, landscape
 * tablets). On portrait tablets and foldables (Medium, 600–840dp) two panes
 * leave each column ~300dp, which wraps button labels one word per line —
 * those widths use the single-column layout inside [ReadableWidth] instead.
 */
val WindowAdaptiveInfo.useTwoPaneLayout: Boolean
    get() = isExpandedWidth

/** Widest a single-column screen grows before it's centred with side margins. */
val ReadableMaxWidth: Dp = 680.dp

/** Centres single-column content at [maxWidth] on wide screens; a no-op on phones. */
@Composable
fun ReadableWidth(
    maxWidth: Dp = ReadableMaxWidth,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            content()
        }
    }
}
