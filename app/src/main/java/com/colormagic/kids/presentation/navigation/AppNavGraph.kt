package com.colormagic.kids.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.colormagic.kids.presentation.screens.home.HomeScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    isTablet: Boolean,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(isTablet = isTablet)
        }
        // Add new composable destinations here as features are defined.
    }
}
