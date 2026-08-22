package hu.rayworks.vizit

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import hu.rayworks.vizit.auth.AuthCallback
import hu.rayworks.vizit.auth.AuthCallbackParser
import hu.rayworks.vizit.auth.AuthViewModel
import hu.rayworks.vizit.data.remote.SupabaseProvider
import hu.rayworks.vizit.ui.VizitRoot
import hu.rayworks.vizit.ui.theme.VizitTheme
import io.github.jan.supabase.auth.handleDeeplinks

class MainActivity : ComponentActivity() {
    private val viewModel: VizitViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleAuthIntent(intent)
        setContent {
            VizitTheme {
                VizitRoot(vizitViewModel = viewModel, authViewModel = authViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshNfcStatus()
    }

    override fun onStop() {
        viewModel.stopNfcShare()
        super.onStop()
    }

    private fun handleAuthIntent(intent: Intent?) {
        val actualIntent = intent ?: return
        val rawUrl = actualIntent.dataString.orEmpty()
        when (val callback = AuthCallbackParser.parse(rawUrl, BuildConfig.AUTH_SCHEME)) {
            AuthCallback.PasswordRecovery -> authViewModel.markPasswordRecovery()
            is AuthCallback.Error -> {
                authViewModel.reportDeepLinkErrorCode(callback.code)
                return
            }
            AuthCallback.Generic -> Unit
            null -> return
        }
        SupabaseProvider.getOrNull()?.handleDeeplinks(
            actualIntent,
            onError = authViewModel::reportDeepLinkError,
        )
    }
}
