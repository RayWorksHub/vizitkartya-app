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

    if (authViewModel.debugLocalProfile) {
        VizitApp(viewModel = vizitViewModel)
        return
    }

    if (authViewModel.passwordRecovery && session is AuthSessionState.Authenticated) {
        AuthScreen(viewModel = authViewModel, forceNewPassword = true)
        return
    }

    when (session) {
        AuthSessionState.Authenticated -> VizitApp(viewModel = vizitViewModel)
        AuthSessionState.Initializing -> CenteredStatus { CircularProgressIndicator() }
        AuthSessionState.BackendUnavailable -> CenteredStatus {
            Text("A VIZIT backend ebben a buildben még nincs konfigurálva.", textAlign = TextAlign.Center)
            if (authViewModel.canUseDebugLocalProfile) {
                Button(onClick = authViewModel::useDebugLocalProfile) { Text("Helyi DEV tesztprofil használata") }
            }
        }
        is AuthSessionState.RefreshFailed,
        AuthSessionState.SignedOut -> AuthScreen(viewModel = authViewModel)
    }
}

@Composable
private fun CenteredStatus(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) { content() }
}
