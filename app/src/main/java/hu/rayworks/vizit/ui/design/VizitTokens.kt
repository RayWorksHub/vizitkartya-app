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
    val primaryFill: Color,
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
    val outline: Color,
    val borderFocus: Color,
    val divider: Color,
    // State
    val success: Color,
    val successSubtle: Color,
    val warning: Color,
    val warningSubtle: Color,
    val error: Color,
    val errorFill: Color,
    val errorSubtle: Color,
    val info: Color,
    val infoSubtle: Color,
    // Control
    val controlTrack: Color,
    val controlDisabled: Color,
    val skeletonBase: Color,
    val skeletonSheen: Color,
    val nfcContainer: Color,
    val onNfcContainer: Color,
    val isDark: Boolean,
)

val VizitLightColors = VizitColors(
    canvas = Color(0xFFF7F8FE),
    surface = Color(0xFFFFFFFF),
    elevated = Color(0xFFFFFFFF),
    sunken = Color(0xFFF3F5F9),
    inverse = Color(0xFF0E1733),
    iconSurface = Color(0xFFFFFFFF),
    primary = Color(0xFF2A5BD7),
    primaryFill = Color(0xFF2A5BD7),
    primaryPressed = Color(0xFF204AB6),
    primarySubtle = Color(0xFFE8EEFC),
    onPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFF0FBEE6),
    onAccent = Color(0xFFFFFFFF),
    ink = Color(0xFF0E1733),
    textPrimary = Color(0xFF0E1733),
    textSecondary = Color(0xFF677087),
    textMuted = Color(0xFF677087),
    textOnBrand = Color(0xFFFFFFFF),
    textDisabled = Color(0xFF8A90A0),
    border = Color(0xFFE3E7EF),
    borderStrong = Color(0xFFC7CCD8),
    outline = Color(0xFF79808F),
    borderFocus = Color(0xFF2A5BD7),
    divider = Color(0xFFE3E7EF),
    success = Color(0xFF1F9D57),
    successSubtle = Color(0xFFE4F6EE),
    warning = Color(0xFFB86F0E),
    warningSubtle = Color(0xFFFBF0DF),
    error = Color(0xFFD13B3B),
    errorFill = Color(0xFFD13B3B),
    errorSubtle = Color(0xFFFDECEC),
    info = Color(0xFF2A5BD7),
    infoSubtle = Color(0xFFE8EEFC),
    controlTrack = Color(0xFFE3E5EA),
    controlDisabled = Color(0xFFE3E5EA),
    skeletonBase = Color(0xFFEEF1F6),
    skeletonSheen = Color(0xFFF3F5F8),
    nfcContainer = Color(0xFFDCE4FC),
    onNfcContainer = Color(0xFF0E2A6E),
    isDark = false,
)

/**
 * Dark is a designed theme, not an inversion of light: it has its own surface
 * hierarchy (canvas -> surface -> elevated) and a lighter primary so contrast
 * against the dark canvas stays above 4.5:1.
 */
val VizitDarkColors = VizitColors(
    canvas = Color(0xFF111318),
    surface = Color(0xFF1B1E26),
    elevated = Color(0xFF20242C),
    sunken = Color(0xFF111318),
    inverse = Color(0xFFE4E7EF),
    iconSurface = Color(0xFF20242C),
    primary = Color(0xFF9DB8FF),
    primaryFill = Color(0xFF3F6FE8),
    primaryPressed = Color(0xFFB2C8FF),
    primarySubtle = Color(0xFF243150),
    onPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFF38D6FF),
    onAccent = Color(0xFF111318),
    ink = Color(0xFF0E1733),
    textPrimary = Color(0xFFE4E7EF),
    textSecondary = Color(0xFF9AA2B3),
    textMuted = Color(0xFF9AA2B3),
    textOnBrand = Color(0xFFFFFFFF),
    textDisabled = Color(0xFF6E7587),
    border = Color(0xFF2C313C),
    borderStrong = Color(0xFF4A5060),
    outline = Color(0xFF8C92A0),
    borderFocus = Color(0xFF9DB8FF),
    divider = Color(0xFF2C313C),
    success = Color(0xFF5CCB8C),
    successSubtle = Color(0xFF1D3A2B),
    warning = Color(0xFFF0B35C),
    warningSubtle = Color(0xFF3D2E17),
    error = Color(0xFFFF8A80),
    errorFill = Color(0xFFC8453F),
    errorSubtle = Color(0xFF45201F),
    info = Color(0xFF9DB8FF),
    infoSubtle = Color(0xFF243150),
    controlTrack = Color(0xFF2E333D),
    controlDisabled = Color(0xFF2E333D),
    skeletonBase = Color(0xFF262B35),
    skeletonSheen = Color(0xFF2E333D),
    nfcContainer = Color(0xFF2A3A63),
    onNfcContainer = Color(0xFFD6E0FF),
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
