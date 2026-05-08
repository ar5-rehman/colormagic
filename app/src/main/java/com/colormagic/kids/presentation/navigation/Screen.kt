package com.colormagic.kids.presentation.navigation

// Add a new object for each screen/destination as features are implemented.
sealed class Screen(val route: String) {
    object Home : Screen("home")
}
