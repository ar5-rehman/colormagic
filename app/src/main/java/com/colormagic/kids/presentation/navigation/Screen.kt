package com.colormagic.kids.presentation.navigation

import android.net.Uri

// Routes for nested feature screens drilled into from a top-level destination.
// Top-level entries (bottom nav) live in [TopLevelDestination].
sealed class Screen(val route: String) {
    // CreateSketch optionally carries a category key (see CategoryIdeas) so
    // Home → category card can deep-link in with a random prompt prefilled.
    data object CreateSketch : Screen("create-sketch?category={category}&prompt={prompt}") {
        const val ARG_CATEGORY = "category"
        const val ARG_PROMPT = "prompt"
        const val ROUTE_PATTERN = "create-sketch?category={category}&prompt={prompt}"
        const val ROUTE_PLAIN = "create-sketch"

        /** [category] deep-links a category card; [prompt] prefills an exact
         *  idea (e.g. Home's "Today's magic idea"). */
        fun routeFor(category: String? = null, prompt: String? = null): String {
            val params = buildList {
                if (!category.isNullOrBlank()) add("category=${Uri.encode(category)}")
                if (!prompt.isNullOrBlank()) add("prompt=${Uri.encode(prompt)}")
            }
            return if (params.isEmpty()) ROUTE_PLAIN
            else "create-sketch?" + params.joinToString("&")
        }
    }

    // Loading carries the kid's prompt as a (URL-encoded) query argument so
    // the LoadingViewModel can run the real backend generation.
    data object Loading : Screen("loading") {
        const val ARG_PROMPT = "prompt"
        const val ARG_IS_CHALLENGE = "isChallenge"
        const val ROUTE_PATTERN = "loading?prompt={prompt}&isChallenge={isChallenge}"
        fun routeFor(prompt: String, isChallenge: Boolean = false): String =
            "loading?prompt=${Uri.encode(prompt)}&isChallenge=$isChallenge"
    }

    data object SketchPreview : Screen("sketch-preview")
    data object Coloring : Screen("coloring")
    data object SaveSuccess : Screen("save-success")
    data object Subscription : Screen("subscription")
    data object PurchaseSuccess : Screen("purchase-success")
    data object Settings : Screen("settings")
    data object GetCredits : Screen("get-credits")
    data object Support : Screen("support")
    data object DailyChallenge : Screen("daily-challenge")
}
