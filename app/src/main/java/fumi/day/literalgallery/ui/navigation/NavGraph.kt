package fumi.day.literalgallery.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import fumi.day.literalgallery.ui.grid.GalleryGridScreen
import fumi.day.literalgallery.ui.grid.GalleryGridViewModel
import fumi.day.literalgallery.ui.settings.SettingsScreen
import fumi.day.literalgallery.ui.viewer.MediaViewerScreen

object Routes {
    const val GALLERY_GRAPH = "gallery"
    const val GRID = "gallery/grid"
    const val VIEWER = "gallery/viewer/{mediaKey}"
    const val SETTINGS = "settings"
    fun viewer(mediaKey: String) = "gallery/viewer/$mediaKey"
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.GALLERY_GRAPH) {
        navigation(startDestination = Routes.GRID, route = Routes.GALLERY_GRAPH) {
            composable(Routes.GRID) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Routes.GALLERY_GRAPH)
                }
                val viewModel: GalleryGridViewModel = hiltViewModel(parentEntry)
                GalleryGridScreen(
                    viewModel = viewModel,
                    onOpen = { mediaKey -> navController.navigate(Routes.viewer(mediaKey)) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }
            composable(
                route = Routes.VIEWER,
                arguments = listOf(navArgument("mediaKey") { type = NavType.StringType })
            ) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Routes.GALLERY_GRAPH)
                }
                val viewModel: GalleryGridViewModel = hiltViewModel(parentEntry)
                val mediaKey = backStackEntry.arguments!!.getString("mediaKey")!!
                MediaViewerScreen(
                    mediaKey = mediaKey,
                    viewModel = viewModel
                )
            }
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
