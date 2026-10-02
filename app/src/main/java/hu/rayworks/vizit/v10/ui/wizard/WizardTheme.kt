package hu.rayworks.vizit.v10.ui.wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import hu.rayworks.vizit.v10.data.AppState
import hu.rayworks.vizit.v10.ui.theme.ThemeState
import hu.rayworks.vizit.v10.ui.theme.V

/** The ZIP root's actual theme providers, without its demo state or actions. */
@Composable
fun ThemedWizardHost(app: AppState) {
    val scheme = if (ThemeState.dark) {
        darkColorScheme(primary = V.blueFill, onPrimary = V.onBlueFill,
            background = V.bg, surface = V.surface, onSurface = V.ink, outline = V.outline)
    } else {
        lightColorScheme(primary = V.blueFill, onPrimary = V.onBlueFill,
            background = V.bg, surface = V.surface, onSurface = V.ink, outline = V.outline)
    }
    val text = TextStyle(fontSize = 15.sp, lineHeight = 1.35.em, color = V.ink,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None))
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalContentColor provides V.ink, LocalTextStyle provides text) {
            Box(Modifier.fillMaxSize().background(V.bg)) { WizardHost(app) }
        }
    }
}
