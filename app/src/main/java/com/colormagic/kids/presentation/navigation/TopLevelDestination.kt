package com.colormagic.kids.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.ui.graphics.vector.ImageVector

// Items shown in the NavigationSuiteScaffold (BottomBar / NavRail / NavDrawer).
// Add an entry here to surface a new top-level destination across all form factors.
enum class TopLevelDestination(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String
) {
    HOME(
        route = "home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        label = "Home"
    ),
    GALLERY(
        route = "gallery",
        selectedIcon = Icons.Filled.PhotoLibrary,
        unselectedIcon = Icons.Outlined.PhotoLibrary,
        label = "Gallery"
    ),
    PARENTS(
        route = "parents",
        selectedIcon = Icons.Filled.Shield,
        unselectedIcon = Icons.Outlined.Shield,
        label = "Parents"
    );

    companion object {
        val START_DESTINATION = HOME
    }
}
