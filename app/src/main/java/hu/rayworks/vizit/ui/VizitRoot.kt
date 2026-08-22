package hu.rayworks.vizit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import hu.rayworks.vizit.VizitViewModel
import hu.rayworks.vizit.auth.AuthSessionState
import hu.rayworks.vizit.auth.AuthViewModel
import hu.rayworks.vizit.ui.screens.AuthScreen

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
            VizitApp(viewModel = vizitViewModel)
        } else {
            CenteredStatus { CircularProgressIndicator() }
        }
        return
    }

    if (authViewModel.passwordRecovery && currentSession is AuthSessionState.Authenticated) {
        AuthScreen(viewModel = authViewModel, forceNewPassword = true)
        return
    }

    when (currentSession) {
        is AuthSessionState.Authenticated -> if (vizitViewModel.hasOfflineProfileSession) {
            VizitApp(viewModel = vizitViewModel)
        } else {
            CenteredStatus { CircularProgressIndicator() }
        }
        AuthSessionState.Initializing -> CenteredStatus { CircularProgressIndicator() }
        AuthSessionState.BackendUnavailable -> CenteredStatus {
            Text("A VIZIT backend ebben a buildben még nincs konfigurálva.", textAlign = TextAlign.Center)
            if (authViewModel.canUseDebugLocalProfile) {
                Button(onClick = authViewModel::useDebugLocalProfile) { Text("Helyi DEV tesztprofil használata") }
            }
        }
        is AuthSessionState.RefreshFailed -> if (vizitViewModel.hasOfflineProfileSession) {
            VizitApp(viewModel = vizitViewModel, offlineMode = true)
        } else {
            CenteredStatus {
                Text(currentSession.message, textAlign = TextAlign.Center)
                Button(onClick = authViewModel::logout) { Text("Újra bejelentkezem") }
            }
        }
        AuthSessionState.SignedOut -> AuthScreen(viewModel = authViewModel)
    }
}

private const val LOCAL_DEBUG_PROFILE_OWNER_ID = "local-dev-profile"

@Composable
private fun CenteredStatus(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) { content() }
}
