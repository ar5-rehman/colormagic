package com.colormagic.kids.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.compose.runtime.getValue
import com.colormagic.kids.data.telemetry.appTelemetry
import com.colormagic.kids.presentation.screens.coloring.ColoringScreen
import com.colormagic.kids.presentation.screens.createsketch.CreateSketchScreen
import com.colormagic.kids.presentation.screens.gallery.GalleryScreen
import com.colormagic.kids.presentation.screens.home.HomeScreen
import com.colormagic.kids.presentation.screens.loading.LoadingScreen
import com.colormagic.kids.presentation.screens.parents.ParentsScreen
import com.colormagic.kids.presentation.screens.purchasesuccess.PurchaseSuccessScreen
import com.colormagic.kids.presentation.screens.savesuccess.SaveSuccessScreen
import com.colormagic.kids.presentation.screens.settings.SettingsScreen
import com.colormagic.kids.presentation.screens.support.SupportScreen
import com.colormagic.kids.presentation.screens.sketchpreview.SketchPreviewScreen
import com.colormagic.kids.presentation.screens.credits.GetCreditsScreen
import com.colormagic.kids.presentation.screens.subscription.SubscriptionScreen

// Inner navigation graph rendered inside MainScaffold.
//
// Flow for the "create a coloring page" feature:
//   Home → CreateSketch → Loading → SketchPreview → Coloring → SaveSuccess
// `popUpTo` is used to evict intermediate screens that shouldn't be returned
// to via the back button (Loading, SaveSuccess kills Coloring).
@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    // One place logs every screen — adding a new destination automatically
    // shows up in Firebase Analytics with no extra wiring per screen. We use
    // the route string (stable, code-defined) rather than the user-facing
    // label, so renames in copy don't break funnel comparisons over time.
    val context = LocalContext.current
    val telemetry = remember(context) { context.appTelemetry() }
    val currentEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(currentEntry?.destination?.route) {
        currentEntry?.destination?.route?.let(telemetry::logScreenView)
    }

    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.START_DESTINATION.route,
        modifier = modifier
    ) {
        composable(TopLevelDestination.HOME.route) {
            HomeScreen(
                onCreateNewSketch = { navController.navigate(Screen.CreateSketch.routeFor()) },
                // Category card on Home deep-links into CreateSketch with a
                // random prompt from that category pre-filled.
                onCategoryClick = { cat ->
                    navController.navigate(Screen.CreateSketch.routeFor(cat.id))
                },
                onOpenGallery = { navController.navigateToTopLevel(TopLevelDestination.GALLERY) },
                onOpenParentArea = { navController.navigateToTopLevel(TopLevelDestination.PARENTS) },
                onGetCredits = { navController.navigate(Screen.GetCredits.route) }
            )
        }
        composable(TopLevelDestination.GALLERY.route) {
            GalleryScreen(
                onStartNewArt = { navController.navigate(Screen.CreateSketch.routeFor()) },
                onOpenParents = { navController.navigateToTopLevel(TopLevelDestination.PARENTS) }
            )
        }
        composable(TopLevelDestination.PARENTS.route) {
            ParentsScreen(
                onManageSubscription = { navController.navigate(Screen.Subscription.route) },
                onOpenSupport = { navController.navigate(Screen.Support.route) },
                onGetCredits = { navController.navigate(Screen.GetCredits.route) },
                onLeaveTab = { navController.navigateToTopLevel(TopLevelDestination.HOME) }
            )
        }

        composable(Screen.Support.route) {
            SupportScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.GetCredits.route) {
            GetCreditsScreen(
                onBack = { navController.popBackStack() },
                onGoToPremium = { navController.navigate(Screen.Subscription.route) }
            )
        }

        composable(Screen.Subscription.route) {
            SubscriptionScreen(
                onBack = { navController.popBackStack() },
                onPurchaseSuccessful = {
                    navController.navigate(Screen.PurchaseSuccess.route) {
                        popUpTo(Screen.Subscription.route) { inclusive = true }
                    }
                },
                // Header avatar → Parent area (gated). Available here because
                // this Subscription instance lives inside the main app graph.
                onProfile = { navController.navigateToTopLevel(TopLevelDestination.PARENTS) }
            )
        }

        composable(Screen.PurchaseSuccess.route) {
            PurchaseSuccessScreen(
                onBackToHome = {
                    navController.popBackStack(
                        TopLevelDestination.HOME.route,
                        inclusive = false
                    )
                },
                onCreateSketch = {
                    navController.navigate(Screen.CreateSketch.routeFor()) {
                        popUpTo(TopLevelDestination.HOME.route) { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onManageSubscription = { navController.navigate(Screen.Subscription.route) },
                onDeleteAllArtwork = { /* TODO: wire to repository deleteAll */ }
            )
        }

        composable(
            route = Screen.CreateSketch.ROUTE_PATTERN,
            arguments = listOf(
                navArgument(Screen.CreateSketch.ARG_CATEGORY) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) {
            CreateSketchScreen(
                onBack = { navController.popBackStack() },
                onMakeSketchRequested = { prompt ->
                    navController.navigate(Screen.Loading.routeFor(prompt))
                },
                onUpgrade = { navController.navigate(Screen.Subscription.route) },
                onGetCredits = { navController.navigate(Screen.GetCredits.route) }
            )
        }

        composable(
            route = Screen.Loading.ROUTE_PATTERN,
            arguments = listOf(
                navArgument(Screen.Loading.ARG_PROMPT) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) {
            LoadingScreen(
                onSketchReady = {
                    // Replace Loading with SketchPreview so back goes to CreateSketch.
                    navController.navigate(Screen.SketchPreview.route) {
                        popUpTo(Screen.Loading.ROUTE_PATTERN) { inclusive = true }
                    }
                },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(Screen.SketchPreview.route) {
            SketchPreviewScreen(
                onBack = { navController.popBackStack() },
                onColorThis = {
                    navController.navigate(Screen.Coloring.route)
                },
                onTryAnother = {
                    // Go back to CreateSketch so the kid can edit the prompt.
                    navController.popBackStack(
                        Screen.CreateSketch.ROUTE_PATTERN,
                        inclusive = false
                    )
                }
            )
        }

        composable(Screen.Coloring.route) {
            ColoringScreen(
                onBack = { navController.popBackStack() },
                onSaved = {
                    // After Save we kill Coloring so back from SaveSuccess
                    // doesn't bounce the kid back into the editor.
                    navController.navigate(Screen.SaveSuccess.route) {
                        popUpTo(Screen.Coloring.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.SaveSuccess.route) {
            SaveSuccessScreen(
                // Two-step pattern to leave a clean back-stack:
                //   1. popBackStack to Home WITHOUT saveState — drops the
                //      whole create-flow (Create / SketchPreview / Save-
                //      Success) and crucially does NOT bookmark it under
                //      Home's saved-state key. Otherwise the bottom-nav's
                //      `restoreState = true` later resurrects SaveSuccess
                //      when the kid taps Home from Gallery.
                //   2. navigateToTopLevel to the actual destination — uses
                //      the standard saveState/restoreState pattern so
                //      future bottom-nav swaps between top-level screens
                //      work normally.
                onGoToGallery = {
                    navController.popBackStack(
                        TopLevelDestination.HOME.route,
                        inclusive = false
                    )
                    navController.navigateToTopLevel(TopLevelDestination.GALLERY)
                },
                onCreateAnother = {
                    navController.popBackStack(
                        TopLevelDestination.HOME.route,
                        inclusive = false
                    )
                    navController.navigate(Screen.CreateSketch.routeFor())
                }
            )
        }
    }
}

internal fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
