package com.colormagic.kids.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.colormagic.kids.presentation.adaptive.isCompactWidth
import com.colormagic.kids.presentation.navigation.AppNavGraph
import com.colormagic.kids.presentation.navigation.BrandBottomBar
import com.colormagic.kids.presentation.navigation.BrandTopNavBar
import com.colormagic.kids.presentation.navigation.RootDestination
import com.colormagic.kids.presentation.navigation.Screen
import com.colormagic.kids.presentation.navigation.TopLevelDestination
import com.colormagic.kids.presentation.navigation.navigateToTopLevel
import com.colormagic.kids.presentation.screens.onboarding.OnboardingScreen
import com.colormagic.kids.presentation.screens.splash.SplashScreen

// Two-tier navigation:
//   Outer NavHost owns the pre-app flow (Splash → Onboarding → Main shell).
//   MainScaffold renders the form-factor-appropriate nav chrome:
//     • Compact width  → bottom bar
//     • Medium/Expanded → top nav bar (shown only on top-level destinations)
//   Nested screens render their own back-affordance inside their content.
@Composable
fun AppRoot() {
    val rootNavController = rememberNavController()

    NavHost(
        navController = rootNavController,
        startDestination = RootDestination.SPLASH
    ) {
        composable(RootDestination.SPLASH) {
            SplashScreen(
                onTimeout = {
                    rootNavController.navigate(RootDestination.ONBOARDING) {
                        popUpTo(RootDestination.SPLASH) { inclusive = true }
                    }
                }
            )
        }
        composable(RootDestination.ONBOARDING) {
            OnboardingScreen(
                onStartCreating = {
                    rootNavController.navigate(RootDestination.MAIN) {
                        popUpTo(RootDestination.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(RootDestination.MAIN) { MainScaffold() }
    }
}

@Composable
private fun MainScaffold() {
    val navController = rememberNavController()
    val currentTopLevel = navController.currentTopLevelDestination()
    val info = currentWindowAdaptiveInfo()

    if (info.isCompactWidth) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                BrandBottomBar(
                    selected = currentTopLevel,
                    onSelect = { navController.navigateToTopLevel(it) }
                )
            }
        ) { padding ->
            AppNavGraph(
                navController = navController,
                modifier = Modifier.padding(padding)
            )
        }
    } else {
        // Tablet / expanded: the top nav bar is only shown when on a
        // top-level destination. Nested screens (CreateSketch, Coloring,
        // SaveSuccess, …) supply their own header so the layout breathes.
        Column(modifier = Modifier.fillMaxSize()) {
            if (currentTopLevel != null) {
                BrandTopNavBar(
                    selected = currentTopLevel,
                    onSelect = { navController.navigateToTopLevel(it) },
                    onSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            AppNavGraph(
                navController = navController,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun NavHostController.currentTopLevelDestination(): TopLevelDestination? {
    val backStackEntry by currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route ?: return null
    return TopLevelDestination.entries.firstOrNull { it.route == route }
}
