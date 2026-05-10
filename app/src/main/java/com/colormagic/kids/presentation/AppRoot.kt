package com.colormagic.kids.presentation

import androidx.compose.foundation.layout.Row
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
import com.colormagic.kids.presentation.navigation.BrandNavRail
import com.colormagic.kids.presentation.navigation.RootDestination
import com.colormagic.kids.presentation.navigation.TopLevelDestination
import com.colormagic.kids.presentation.navigation.navigateToTopLevel
import com.colormagic.kids.presentation.screens.onboarding.OnboardingScreen
import com.colormagic.kids.presentation.screens.splash.SplashScreen

// Two-tier navigation:
//   Outer NavHost owns the pre-app flow (Splash → Onboarding → Main shell).
//   MainScaffold renders a custom brand-styled bottom bar (compact) or rail
//   (medium / expanded) wrapping the inner AppNavGraph.
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
        Row(modifier = Modifier.fillMaxSize()) {
            BrandNavRail(
                selected = currentTopLevel,
                onSelect = { navController.navigateToTopLevel(it) }
            )
            AppNavGraph(navController = navController)
        }
    }
}

@Composable
private fun NavHostController.currentTopLevelDestination(): TopLevelDestination? {
    val backStackEntry by currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route ?: return null
    return TopLevelDestination.entries.firstOrNull { it.route == route }
}
