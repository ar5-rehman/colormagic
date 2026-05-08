package com.colormagic.kids.presentation.navigation

// Use Screen for nested routes inside a feature (e.g. detail screens drilled into
// from a top-level destination). Top-level entries live in [TopLevelDestination].
sealed class Screen(val route: String) {
    // Add nested feature routes here, e.g.:
    // data class ColoringDetail(val id: String) : Screen("coloring/$id") {
    //     companion object { const val ROUTE = "coloring/{id}" }
    // }
}
