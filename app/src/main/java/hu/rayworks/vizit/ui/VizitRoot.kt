package hu.rayworks.vizit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import hu.rayworks.vizit.VizitViewModel
import hu.rayworks.vizit.auth.AuthSessionState
import hu.rayworks.vizit.auth.AuthViewModel
import hu.rayworks.vizit.ui.design.Vizit
import hu.rayworks.vizit.ui.design.components.VizitButton
import hu.rayworks.vizit.ui.design.components.VizitButtonStyle
import hu.rayworks.vizit.ui.design.components.VizitEmptyState
import hu.rayworks.vizit.ui.design.components.VizitErrorState
import hu.rayworks.vizit.ui.design.components.VizitLoadingState
import hu.rayworks.vizit.ui.screens.AuthScreen

private const val LOCAL_DEBUG_PROFILE_OWNER_ID = "local-dev-profile"

/**
 * Session gate. Decides between the auth surface, the app, and the small set of
 * blocking states (booting, backend missing, refresh failed, legal gate) — each
 * of which now gets a proper designed state rather than a bare centred string.
 */
@Composable
fun VizitRoot(vizitViewModel: VizitViewModel, authViewModel: AuthViewModel) {
    val session by authViewModel.sessionState.collectAsState()
    val currentSession = session

    LaunchedEffect(currentSession, authViewModel.debugLocalProfile) {
        when {
            authViewModel.debugLocalProfile -> vizitViewModel.bindProfileOwner(
                userId = LOCAL_DEBUG_PROFILE_OWNER_ID,
                enableCloudSync = false,
            )

            currentSession is AuthSessionState.Authenticated -> vizitViewModel.bindProfileOwner(
                userId = currentSession.userId,
                enableCloudSync = true,
            )

            currentSession is AuthSessionState.RefreshFailed -> {
                currentSession.cachedUserId?.let { userId ->
                    vizitViewModel.bindProfileOwner(userId = userId, enableCloudSync = true)
                }
            }
        }
    }

    if (authViewModel.debugLocalProfile) {
        if (vizitViewModel.hasOfflineProfileSession) {
            VizitApp(viewModel = vizitViewModel, authViewModel = authViewModel)
        } else {
            Booting("Helyi profil betöltése…")
        }
        return
    }

    if (
        authViewModel.passwordRecovery &&
        (
            currentSession is AuthSessionState.Authenticated ||
                currentSession is AuthSessionState.LegalAcceptanceRequired ||
                currentSession is AuthSessionState.LegalAcceptanceCheckFailed
            )
    ) {
        AuthScreen(viewModel = authViewModel, forceNewPassword = true)
        return
    }

    when (currentSession) {
        is AuthSessionState.Authenticated ->
            if (vizitViewModel.hasOfflineProfileSession) {
                VizitApp(viewModel = vizitViewModel, authViewModel = authViewModel)
            } else {
                Booting("Névjegy betöltése…")
            }

        AuthSessionState.Initializing -> Booting("Betöltés…")

        AuthSessionState.BackendUnavailable -> Screen {
            VizitEmptyState(
                icon = Icons.Outlined.CloudOff,
                title = "Ez a build nem használható",
                message = "A VIZIT backend ebben a buildben még nincs konfigurálva.",
                actionLabel = if (authViewModel.canUseDebugLocalProfile) {
                    "Helyi DEV tesztprofil használata"
                } else {
                    null
                },
                onAction = if (authViewModel.canUseDebugLocalProfile) {
                    authViewModel::useDebugLocalProfile
                } else {
                    null
                },
            )
        }

        is AuthSessionState.RefreshFailed ->
            if (vizitViewModel.hasOfflineProfileSession) {
                VizitApp(
                    viewModel = vizitViewModel,
                    authViewModel = authViewModel,
                    offlineMode = true,
                )
            } else {
                Screen {
                    VizitErrorState(
                        title = "A munkamenet lejárt",
                        message = currentSession.message,
                        retryLabel = "Újra bejelentkezem",
                        onRetry = authViewModel::logout,
                    )
                }
            }

        is AuthSessionState.LegalAcceptanceRequired -> AuthScreen(
            viewModel = authViewModel,
            forceLegalAcceptance = true,
        )

        is AuthSessionState.LegalAcceptanceCheckFailed -> Screen {
            VizitErrorState(
                title = "Nem sikerült ellenőrizni a feltételeket",
                message = currentSession.message,
                onRetry = authViewModel::retryLegalAcceptanceCheck,
            )
            VizitButton(
                text = "Kijelentkezés",
                onClick = authViewModel::logout,
                style = VizitButtonStyle.Tertiary,
            )
        }

        AuthSessionState.SignedOut -> AuthScreen(viewModel = authViewModel)
    }
}

@Composable
private fun Booting(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().background(Vizit.colors.canvas),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        VizitLoadingState(message = message)
    }
}

@Composable
private fun Screen(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Vizit.colors.canvas)
            .padding(Vizit.space.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        content()
    }
}
