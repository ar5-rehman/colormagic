package com.colormagic.kids.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.colormagic.kids.presentation.screens.home.HomeScreen
import com.colormagic.kids.presentation.screens.settings.SettingsScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.START_DESTINATION.route,
        modifier = modifier
    ) {
        composable(TopLevelDestination.HOME.route) { HomeScreen() }
        composable(TopLevelDestination.SETTINGS.route) { SettingsScreen() }
        // Add nested feature routes here as they are defined.
    }
}
