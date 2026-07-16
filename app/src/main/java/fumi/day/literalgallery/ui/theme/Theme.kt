package fumi.day.literalgallery.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF8A65),
    background = Color.Black,
    surface = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFBF360C),
    background = Color.White,
    surface = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

fun parseColor(hex: String): Color? {
    if (hex.isBlank()) return null
    return try {
        Color(hex.toColorInt())
    } catch (e: IllegalArgumentException) {
        null
    }
}

@Composable
fun LiteralGalleryTheme(
    keyColorHex: String = "",
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val baseColors = if (darkTheme) DarkColors else LightColors
    val keyColor = parseColor(keyColorHex)
    val colorScheme = if (keyColor != null) {
        baseColors.copy(primary = keyColor, secondary = keyColor, tertiary = keyColor)
    } else {
        baseColors
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
