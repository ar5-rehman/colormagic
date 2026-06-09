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
import com.colormagic.kids.presentation.screens.subscription.SubscriptionScreen

// Two-tier navigation:
//   Outer NavHost owns the pre-app flow (Splash → Onboarding → Main shell).
//   MainScaffold renders the form-factor-appropriate nav chrome:
//     • Compact width  → bottom bar
//     • Medium/Expanded → top nav bar (shown only on top-level destinations)
//   Nested screens render their own back-affordance inside their content.
@Composable
fun AppRoot(
    appEntryViewModel: AppEntryViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val rootNavController = rememberNavController()

    NavHost(
        navController = rootNavController,
        startDestination = RootDestination.SPLASH
    ) {
        composable(RootDestination.SPLASH) {
            // The splash decides the next route: Main for returning users,
            // Onboarding on first launch / fresh install.
            SplashScreen(
                onReady = { route ->
                    rootNavController.navigate(route) {
                        popUpTo(RootDestination.SPLASH) { inclusive = true }
                    }
                }
            )
        }
        composable(RootDestination.ONBOARDING) {
            OnboardingScreen(
                onStartCreating = {
                    rootNavController.navigate(RootDestination.PAYWALL) {
                        popUpTo(RootDestination.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(RootDestination.PAYWALL) {
            // Launch paywall. Dismissible via X — kid lands in Main on the
            // free tier. Subscribe also lands in Main (PurchaseSuccess is
            // shown by the inner graph when "Manage Subscription" is used,
            // but on cold-start we keep the path short).
            val enterMain = {
                // Mark the first-run flow as seen so onboarding + this paywall
                // never show again on this install.
                appEntryViewModel.markOnboardingComplete()
                rootNavController.navigate(RootDestination.MAIN) {
                    popUpTo(RootDestination.PAYWALL) { inclusive = true }
                }
            }
            SubscriptionScreen(
                onBack = enterMain,
                onPurchaseSuccessful = enterMain,
                dismissAsClose = true
            )
        }
        composable(RootDestination.MAIN) {
            // Wrap the whole app shell so the parent's screen-time limit can
            // overlay a gentle "time for a break" screen anywhere in the app.
            ScreenTimeGuard(modifier = Modifier.fillMaxSize()) { MainScaffold() }
        }
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
                // Bottom nav is only meaningful on top-level destinations
                // (Home / Gallery / Parents). On nested workflow screens like
                // CreateSketch, Loading, Coloring etc. the bar would just be
                // a confusing dead-end — hide it and let the screen's own
                // back affordance handle navigation.
                if (currentTopLevel != null) {
                    BrandBottomBar(
                        selected = currentTopLevel,
                        onSelect = { navController.navigateToTopLevel(it) }
                    )
                }
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
                    onProfile = { navController.navigateToTopLevel(TopLevelDestination.PARENTS) },
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
