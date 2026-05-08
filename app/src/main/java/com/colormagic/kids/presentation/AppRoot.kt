package com.colormagic.kids.presentation

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.colormagic.kids.presentation.navigation.AppNavGraph
import com.colormagic.kids.presentation.navigation.TopLevelDestination

// Root composable. NavigationSuiteScaffold automatically swaps between
// BottomBar (Compact) → NavRail (Medium) → NavDrawer (Expanded) based on width.
// You author the nav once, the framework handles the form factor.
@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val currentTopLevel = navController.currentTopLevelDestination()

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                item(
                    selected = destination == currentTopLevel,
                    onClick = { navController.navigateToTopLevel(destination) },
                    icon = {
                        Icon(
                            imageVector = if (destination == currentTopLevel) destination.selectedIcon
                            else destination.unselectedIcon,
                            contentDescription = destination.label
                        )
                    },
                    label = { Text(destination.label) }
                )
            }
        }
    ) {
        AppNavGraph(navController = navController)
    }
}

@Composable
private fun NavHostController.currentTopLevelDestination(): TopLevelDestination? {
    val backStackEntry by currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route ?: return null
    return TopLevelDestination.entries.firstOrNull { it.route == route }
}

private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
