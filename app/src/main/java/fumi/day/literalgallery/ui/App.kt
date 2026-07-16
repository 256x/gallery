package fumi.day.literalgallery.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import fumi.day.literalgallery.ui.navigation.NavGraph
import fumi.day.literalgallery.ui.permission.PermissionGate
import fumi.day.literalgallery.ui.theme.LiteralGalleryTheme
import fumi.day.literalgallery.ui.theme.ThemeViewModel

@Composable
fun App(themeViewModel: ThemeViewModel = hiltViewModel()) {
    val userPrefs by themeViewModel.userPrefs.collectAsState()
    LiteralGalleryTheme(keyColorHex = userPrefs.keyColorHex) {
        // Without this, screens that don't paint their own background (e.g. the grid)
        // show through to the native window background, which stays light regardless
        // of the Compose dark color scheme.
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            PermissionGate {
                NavGraph(navController = rememberNavController())
            }
        }
    }
}
