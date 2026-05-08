package com.colormagic.kids.presentation.screens.home.tablet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.colormagic.kids.presentation.screens.home.HomeViewModel

@Composable
fun HomeTabletScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Color Magic Kids — Tablet")
    }
}
