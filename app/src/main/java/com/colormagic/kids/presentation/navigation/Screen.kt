package com.colormagic.kids.presentation.navigation

// Routes for nested feature screens drilled into from a top-level destination.
// Top-level entries (bottom nav) live in [TopLevelDestination].
sealed class Screen(val route: String) {
    data object CreateSketch : Screen("create-sketch")
    data object Loading : Screen("loading")
}
