package hu.rayworks.vizit.v10.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import hu.rayworks.vizit.v10.data.AppState
import hu.rayworks.vizit.v10.data.AuthMode
import hu.rayworks.vizit.v10.data.DesignLayout
import hu.rayworks.vizit.v10.data.Gate
import hu.rayworks.vizit.v10.data.HomeTab
import hu.rayworks.vizit.v10.data.Page
import hu.rayworks.vizit.v10.data.SyncStatus
import hu.rayworks.vizit.v10.ui.auth.GateScreen
import hu.rayworks.vizit.v10.ui.components.DialogHost
import hu.rayworks.vizit.v10.ui.components.ToastHost
import hu.rayworks.vizit.v10.ui.home.HomeA
import hu.rayworks.vizit.v10.ui.home.HomeB
import hu.rayworks.vizit.v10.ui.pages.PageHost
import hu.rayworks.vizit.v10.ui.sheets.EditSheet
import hu.rayworks.vizit.v10.ui.sheets.MenuSheet
import hu.rayworks.vizit.v10.ui.sheets.PrototypeSheet
import hu.rayworks.vizit.v10.ui.sheets.ProfilesSheet
import hu.rayworks.vizit.v10.ui.sheets.ShareSheet
import hu.rayworks.vizit.v10.ui.theme.ThemeMode
import hu.rayworks.vizit.v10.ui.theme.ThemeState
import hu.rayworks.vizit.v10.ui.theme.V
import hu.rayworks.vizit.v10.ui.wizard.WizardHost
import hu.rayworks.vizit.auth.AuthViewModel
import kotlinx.coroutines.delay

/**
 * Gyökér: főképernyő (A vagy B), fölötte a teljes képernyős oldalak, a lapok, a varázsló,
 * a belépési / hibaállapotok, a párbeszédablak és legfelül az üzenet (toast).
 */
@Composable
fun VizitApp(app: AppState, authViewModel: AuthViewModel) {
    val context = LocalContext.current
    val actions = remember(context) { Actions(context, app) }

    val systemDark = isSystemInDarkTheme()
    val wantDark = when (ThemeState.mode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> systemDark
    }
    if (ThemeState.dark != wantDark) ThemeState.dark = wantDark

    // A szinkron demó: „folyamatban” után rendben (offline: függőben marad), újra online: indul.
    LaunchedEffect(app.sync) {
        if (!app.productionMode && app.sync == SyncStatus.Syncing) {
            delay(1200)
            app.sync = if (app.offline) SyncStatus.Pending else SyncStatus.Synced
        }
    }
    LaunchedEffect(app.offline) {
        if (!app.productionMode && !app.offline && app.sync == SyncStatus.Pending && app.autoSync) app.sync = SyncStatus.Syncing
    }

    val scheme = if (ThemeState.dark) {
        darkColorScheme(primary = V.blueFill, onPrimary = V.onBlueFill, background = V.bg, surface = V.surface, onSurface = V.ink, outline = V.outline)
    } else {
        lightColorScheme(primary = V.blueFill, onPrimary = V.onBlueFill, background = V.bg, surface = V.surface, onSurface = V.ink, outline = V.outline)
    }
    val baseText = TextStyle(
        fontSize = 15.sp,
        lineHeight = 1.35.em,
        color = V.ink,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    )

    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalActions provides actions, LocalContentColor provides V.ink, LocalTextStyle provides baseText) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(V.bg)
            ) {
                when (app.layout) {
                    DesignLayout.B -> HomeB(app)
                    DesignLayout.A -> HomeA(app)
                }
                PageHost(app)
                if (app.layout == DesignLayout.B) MenuSheet(app)
                ProfilesSheet(app)
                EditSheet(app)
                ShareSheet(app)
                if (!app.productionMode) PrototypeSheet(app)
                WizardHost(app)

                var lastGate by remember { mutableStateOf(app.gate) }
                if (app.gate != null) lastGate = app.gate
                AnimatedVisibility(visible = app.gate != null, enter = fadeIn(tween(220)), exit = fadeOut(tween(220))) {
                    lastGate?.let { GateScreen(app, it, authViewModel) }
                }

                DialogHost(app)
                ToastHost(app.toast, bottomPadding = if (app.layout == DesignLayout.B) 132.dp else 98.dp)
            }
        }

        val top = app.pages.lastOrNull()
        SystemBars(
            dark = ThemeState.dark || top == Page.NfcSend || top == Page.Scanner ||
                (app.wizardOpen && app.wizard?.introShown == true && app.gate == null)
        )

        val gate = app.gate
        val authBack = gate is Gate.Auth && gate.mode in listOf(AuthMode.Register, AuthMode.Forgot, AuthMode.EmailSent, AuthMode.ResetSent)
        BackHandler(
            enabled = app.dialog != null || authBack || (gate == null && (app.wizardOpen || app.sheet != null || app.pages.isNotEmpty() ||
                (app.layout == DesignLayout.A && app.tab != HomeTab.Share)))
        ) {
            when {
                app.dialog != null -> app.dialog = null
                authBack -> {
                    app.authBanner = null
                    app.gate = Gate.Auth(AuthMode.Login)
                }
                app.wizardOpen -> {
                    val w = app.wizard
                    if (w != null && w.confirmOpen) w.confirmOpen = false else app.closeWizard(force = false)
                }
                app.sheet != null -> app.sheet = null
                app.pages.isNotEmpty() -> app.pop()
                else -> app.tab = HomeTab.Share
            }
        }
    }
}

/** Sötét háttér (sötét téma, NFC-küldés, beolvasás, varázsló kezdőlap) alatt világos rendszerikonok. */
@Composable
private fun SystemBars(dark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        val c = WindowCompat.getInsetsController(window, view)
        c.isAppearanceLightStatusBars = !dark
        c.isAppearanceLightNavigationBars = !dark
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
