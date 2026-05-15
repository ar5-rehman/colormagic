package com.colormagic.kids.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.colormagic.kids.presentation.screens.coloring.ColoringScreen
import com.colormagic.kids.presentation.screens.createsketch.CreateSketchScreen
import com.colormagic.kids.presentation.screens.gallery.GalleryScreen
import com.colormagic.kids.presentation.screens.home.HomeScreen
import com.colormagic.kids.presentation.screens.loading.LoadingScreen
import com.colormagic.kids.presentation.screens.parents.ParentsScreen
import com.colormagic.kids.presentation.screens.purchasesuccess.PurchaseSuccessScreen
import com.colormagic.kids.presentation.screens.savesuccess.SaveSuccessScreen
import com.colormagic.kids.presentation.screens.settings.SettingsScreen
import com.colormagic.kids.presentation.screens.sketchpreview.SketchPreviewScreen
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
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.START_DESTINATION.route,
        modifier = modifier
    ) {
        composable(TopLevelDestination.HOME.route) {
            HomeScreen(
                onCreateNewSketch = { navController.navigate(Screen.CreateSketch.route) },
                onOpenGallery = { navController.navigateToTopLevel(TopLevelDestination.GALLERY) },
                onOpenParentArea = { navController.navigateToTopLevel(TopLevelDestination.PARENTS) }
            )
        }
        composable(TopLevelDestination.GALLERY.route) {
            GalleryScreen(
                onStartNewArt = { navController.navigate(Screen.CreateSketch.route) }
            )
        }
        composable(TopLevelDestination.PARENTS.route) {
            ParentsScreen(
                onManageSubscription = { navController.navigate(Screen.Subscription.route) },
                onLeaveTab = { navController.navigateToTopLevel(TopLevelDestination.HOME) }
            )
        }

        composable(Screen.Subscription.route) {
            SubscriptionScreen(
                onBack = { navController.popBackStack() },
                onPurchaseSuccessful = {
                    navController.navigate(Screen.PurchaseSuccess.route) {
                        popUpTo(Screen.Subscription.route) { inclusive = true }
                    }
                }
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
                    navController.navigate(Screen.CreateSketch.route) {
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

        composable(Screen.CreateSketch.route) {
            CreateSketchScreen(
                onMakeSketchRequested = {
                    navController.navigate(Screen.Loading.route)
                }
            )
        }

        composable(Screen.Loading.route) {
            LoadingScreen(
                onComplete = {
                    // Replace Loading with SketchPreview so back goes to CreateSketch.
                    navController.navigate(Screen.SketchPreview.route) {
                        popUpTo(Screen.Loading.route) { inclusive = true }
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
                        Screen.CreateSketch.route,
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
                onGoToGallery = {
                    // Jump to the Gallery tab and clear the create-flow stack.
                    navController.navigate(TopLevelDestination.GALLERY.route) {
                        popUpTo(TopLevelDestination.HOME.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onCreateAnother = {
                    // Start a fresh prompt, leaving Home as the only thing under it.
                    navController.navigate(Screen.CreateSketch.route) {
                        popUpTo(TopLevelDestination.HOME.route) { inclusive = false }
                    }
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
