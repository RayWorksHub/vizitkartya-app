package hu.rayworks.vizit.ui.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** User-selectable appearance. Light is the product default. */
enum class ThemeMode(val storageValue: String) {
    LIGHT("LIGHT"),
    DARK("DARK"),
    SYSTEM("SYSTEM"),
    ;

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: LIGHT
    }
}

/**
 * Root VIZIT theme. Provides the design tokens plus a Material 3 colour scheme
 * derived from them, so both VIZIT components and any remaining Material
 * components resolve to the same palette.
 */
@Composable
fun VizitTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colors = if (dark) VizitDarkColors else VizitLightColors
    val typography = VizitTypography()

    val scheme = if (dark) {
        darkColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            primaryContainer = colors.primarySubtle,
            onPrimaryContainer = colors.textPrimary,
            secondary = colors.accent,
            onSecondary = colors.onAccent,
            background = colors.canvas,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.elevated,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.border,
            outlineVariant = colors.divider,
            error = colors.error,
            onError = colors.textOnBrand,
            errorContainer = colors.errorSubtle,
            onErrorContainer = colors.error,
            scrim = colors.ink,
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            primaryContainer = colors.primarySubtle,
            onPrimaryContainer = colors.primary,
            secondary = colors.accent,
            onSecondary = colors.onAccent,
            background = colors.canvas,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.sunken,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.border,
            outlineVariant = colors.divider,
            error = colors.error,
            onError = colors.textOnBrand,
            errorContainer = colors.errorSubtle,
            onErrorContainer = colors.error,
            scrim = colors.ink,
        )
    }

    // Edge-to-edge: keep system bar icons readable against the current theme.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }

    CompositionLocalProvider(
        LocalVizitColors provides colors,
        LocalVizitTypography provides typography,
        LocalVizitSpacing provides VizitSpacing(),
        LocalVizitRadius provides VizitRadius(),
        LocalVizitElevation provides VizitElevation(),
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = materialTypographyFrom(typography),
            content = content,
        )
    }
}

/** Shorthand accessors: `Vizit.colors.primary`, `Vizit.type.h2`, `Vizit.space.md`. */
object Vizit {
    val colors: VizitColors
        @Composable @ReadOnlyComposable get() = LocalVizitColors.current
    val type: VizitTypography
        @Composable @ReadOnlyComposable get() = LocalVizitTypography.current
    val space: VizitSpacing
        @Composable @ReadOnlyComposable get() = LocalVizitSpacing.current
    val radius: VizitRadius
        @Composable @ReadOnlyComposable get() = LocalVizitRadius.current
    val elevation: VizitElevation
        @Composable @ReadOnlyComposable get() = LocalVizitElevation.current
}
