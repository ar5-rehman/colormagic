package com.colormagic.kids.presentation.screens.home

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.colormagic.kids.presentation.screens.home.phone.HomePhoneScreen
import com.colormagic.kids.presentation.screens.home.tablet.HomeTabletScreen

// Coordinator composable: selects the correct layout based on device type.
// All business logic stays in HomeViewModel — only the layout differs.
@Composable
fun HomeScreen(
    isTablet: Boolean,
    viewModel: HomeViewModel = hiltViewModel()
) {
    if (isTablet) {
        HomeTabletScreen(viewModel = viewModel)
    } else {
        HomePhoneScreen(viewModel = viewModel)
    }
}
