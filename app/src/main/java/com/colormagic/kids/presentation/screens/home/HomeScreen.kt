package com.colormagic.kids.presentation.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.colormagic.kids.presentation.adaptive.isCompactWidth
import com.colormagic.kids.presentation.adaptive.isExpandedWidth

// Single adaptive composable. Use currentWindowAdaptiveInfo() to branch on
// width buckets (Compact / Medium / Expanded) when the layout truly differs.
// For finer responsiveness within a region, use BoxWithConstraints.
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val adaptive = currentWindowAdaptiveInfo()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Color Magic Kids",
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = when {
                    adaptive.isCompactWidth -> "Compact width — phone portrait"
                    adaptive.isExpandedWidth -> "Expanded width — large tablet / desktop"
                    else -> "Medium width — landscape phone / small tablet"
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
