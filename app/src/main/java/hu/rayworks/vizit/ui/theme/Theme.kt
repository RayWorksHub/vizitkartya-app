package hu.rayworks.vizit.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = VizitBlue,
    onPrimary = Color.White,
    primaryContainer = VizitIce,
    onPrimaryContainer = VizitNavy,
    secondary = VizitTeal,
    onSecondary = VizitNavy,
    background = VizitCloud,
    onBackground = VizitNavy,
    surface = Color.White,
    onSurface = VizitNavy,
)

private val DarkColors = darkColorScheme(
    primary = VizitTealLight,
    onPrimary = VizitNavy,
    primaryContainer = VizitBlue,
    onPrimaryContainer = Color.White,
    secondary = VizitTeal,
    background = VizitNavy,
    onBackground = Color(0xFFE4EDF2),
    surface = VizitDarkSurface,
    onSurface = Color(0xFFE4EDF2),
)

@Composable
fun VizitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}
