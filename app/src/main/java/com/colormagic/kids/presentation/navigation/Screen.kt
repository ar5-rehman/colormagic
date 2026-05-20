package com.colormagic.kids.presentation.navigation

import android.net.Uri

// Routes for nested feature screens drilled into from a top-level destination.
// Top-level entries (bottom nav) live in [TopLevelDestination].
sealed class Screen(val route: String) {
    // CreateSketch optionally carries a category key (see CategoryIdeas) so
    // Home → category card can deep-link in with a random prompt prefilled.
    data object CreateSketch : Screen("create-sketch?category={category}") {
        const val ARG_CATEGORY = "category"
        const val ROUTE_PATTERN = "create-sketch?category={category}"
        const val ROUTE_PLAIN = "create-sketch"
        fun routeFor(category: String? = null): String =
            if (category.isNullOrBlank()) ROUTE_PLAIN
            else "create-sketch?category=${Uri.encode(category)}"
    }

    // Loading carries the kid's prompt as a (URL-encoded) query argument so
    // the LoadingViewModel can run the real backend generation.
    data object Loading : Screen("loading") {
        const val ARG_PROMPT = "prompt"
        const val ROUTE_PATTERN = "loading?prompt={prompt}"
        fun routeFor(prompt: String): String = "loading?prompt=${Uri.encode(prompt)}"
    }

    data object SketchPreview : Screen("sketch-preview")
    data object Coloring : Screen("coloring")
    data object SaveSuccess : Screen("save-success")
    data object Subscription : Screen("subscription")
    data object PurchaseSuccess : Screen("purchase-success")
    data object Settings : Screen("settings")
}
