package hu.rayworks.vizit.ui.design

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * VIZIT design tokens.
 *
 * Generated from the VIZIT Design System 2026 Figma library
 * (https://www.figma.com/design/PWzvA7sVoYIBqgfw6MCpRf) — Figma is the visual
 * source of truth. Every colour, spacing step, radius and elevation used in the
 * app must come from here; no ad-hoc hex values or magic paddings in screens.
 */
@Immutable
data class VizitColors(
    // Background & surface
    val canvas: Color,
    val surface: Color,
    val elevated: Color,
    val sunken: Color,
    val inverse: Color,
    val iconSurface: Color,
    // Brand
    val primary: Color,
    val primaryPressed: Color,
    val primarySubtle: Color,
    val onPrimary: Color,
    val accent: Color,
    val onAccent: Color,
    val ink: Color,
    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textOnBrand: Color,
    val textDisabled: Color,
    // Border
    val border: Color,
    val borderStrong: Color,
    val borderFocus: Color,
    val divider: Color,
    // State
    val success: Color,
    val successSubtle: Color,
    val warning: Color,
    val warningSubtle: Color,
    val error: Color,
    val errorSubtle: Color,
    val info: Color,
    val infoSubtle: Color,
    // Control
    val controlTrack: Color,
    val controlDisabled: Color,
    val skeletonBase: Color,
    val skeletonSheen: Color,
    val isDark: Boolean,
)

val VizitLightColors = VizitColors(
    canvas = Color(0xFFF6F7F9),
    surface = Color(0xFFFFFFFF),
    elevated = Color(0xFFFFFFFF),
    sunken = Color(0xFFEDEFF3),
    inverse = Color(0xFF061B46),
    iconSurface = Color(0xFFFFFFFF),
    primary = Color(0xFF0B5CE8),
    primaryPressed = Color(0xFF0848BC),
    primarySubtle = Color(0xFFE8F0FE),
    onPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFF0FBEE6),
    onAccent = Color(0xFF04122E),
    ink = Color(0xFF061B46),
    textPrimary = Color(0xFF0C1729),
    textSecondary = Color(0xFF4A5568),
    textMuted = Color(0xFF6B7688),
    textOnBrand = Color(0xFFFFFFFF),
    textDisabled = Color(0xFFA6AEBB),
    border = Color(0xFFE2E6EC),
    borderStrong = Color(0xFFC9D0DA),
    borderFocus = Color(0xFF0B5CE8),
    divider = Color(0xFFEDEFF3),
    success = Color(0xFF12855A),
    successSubtle = Color(0xFFE4F6EE),
    warning = Color(0xFFA8690A),
    warningSubtle = Color(0xFFFDF3E0),
    error = Color(0xFFC62828),
    errorSubtle = Color(0xFFFDEAEA),
    info = Color(0xFF0B5CE8),
    infoSubtle = Color(0xFFE8F0FE),
    controlTrack = Color(0xFFDDE2E9),
    controlDisabled = Color(0xFFEDEFF3),
    skeletonBase = Color(0xFFE8EBF0),
    skeletonSheen = Color(0xFFF3F5F8),
    isDark = false,
)

/**
 * Dark is a designed theme, not an inversion of light: it has its own surface
 * hierarchy (canvas -> surface -> elevated) and a lighter primary so contrast
 * against the dark canvas stays above 4.5:1.
 */
val VizitDarkColors = VizitColors(
    canvas = Color(0xFF0A0F1C),
    surface = Color(0xFF131B2C),
    elevated = Color(0xFF1B2437),
    sunken = Color(0xFF060A14),
    inverse = Color(0xFFE8ECF2),
    iconSurface = Color(0xFF1B2437),
    primary = Color(0xFF4A90FF),
    primaryPressed = Color(0xFF6BA5FF),
    primarySubtle = Color(0xFF14264A),
    onPrimary = Color(0xFF04122E),
    accent = Color(0xFF38D6FF),
    onAccent = Color(0xFF04122E),
    ink = Color(0xFF061B46),
    textPrimary = Color(0xFFF2F5FA),
    textSecondary = Color(0xFFA3AFC2),
    textMuted = Color(0xFF8592A6),
    textOnBrand = Color(0xFFFFFFFF),
    textDisabled = Color(0xFF5A6577),
    border = Color(0xFF263149),
    borderStrong = Color(0xFF35415C),
    borderFocus = Color(0xFF4A90FF),
    divider = Color(0xFF1E2739),
    success = Color(0xFF34D399),
    successSubtle = Color(0xFF0E2C22),
    warning = Color(0xFFFBBF24),
    warningSubtle = Color(0xFF33260A),
    error = Color(0xFFFF6B6B),
    errorSubtle = Color(0xFF3A1516),
    info = Color(0xFF4A90FF),
    infoSubtle = Color(0xFF14264A),
    controlTrack = Color(0xFF2A3548),
    controlDisabled = Color(0xFF1A2334),
    skeletonBase = Color(0xFF1A2334),
    skeletonSheen = Color(0xFF232E44),
    isDark = true,
)

/** 4 / 8 / 12 / 16 / 20 / 24 / 32 / 40 / 48 — no other values are permitted. */
@Immutable
data class VizitSpacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 40.dp,
    val huge: Dp = 48.dp,
)

/** Deliberate radius ladder — not "everything is 24dp". */
@Immutable
data class VizitRadius(
    val xs: Dp = 6.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 28.dp,
    val full: Dp = 999.dp,
)

/** Three levels only. */
@Immutable
data class VizitElevation(
    val flat: Dp = 0.dp,
    val raised: Dp = 1.dp,
    val floating: Dp = 8.dp,
    val card: Dp = 16.dp,
)

val LocalVizitColors = staticCompositionLocalOf { VizitLightColors }
val LocalVizitSpacing = staticCompositionLocalOf { VizitSpacing() }
val LocalVizitRadius = staticCompositionLocalOf { VizitRadius() }
val LocalVizitElevation = staticCompositionLocalOf { VizitElevation() }

/** Minimum interactive target, enforced on every control. */
val VizitMinTouchTarget: Dp = 48.dp
