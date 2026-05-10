package com.colormagic.kids.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.colormagic.kids.presentation.screens.createsketch.CreateSketchScreen
import com.colormagic.kids.presentation.screens.gallery.GalleryScreen
import com.colormagic.kids.presentation.screens.home.HomeScreen
import com.colormagic.kids.presentation.screens.loading.LoadingScreen
import com.colormagic.kids.presentation.screens.parents.ParentsScreen

// Inner navigation graph rendered inside MainScaffold (which provides the
// brand bottom bar / nav rail). Each top-level entry matches a [TopLevelDestination];
// nested feature routes (Screen.*) live below.
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
        composable(TopLevelDestination.GALLERY.route) { GalleryScreen() }
        composable(TopLevelDestination.PARENTS.route) { ParentsScreen() }

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
                    // TODO: route to the result/canvas screen when it lands.
                    // For now: pop back to Home so the user sees the brand shell.
                    navController.popBackStack(
                        TopLevelDestination.HOME.route,
                        inclusive = false
                    )
                },
                onCancel = { navController.popBackStack() }
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
