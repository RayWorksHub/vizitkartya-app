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
        val callback = when (val parsed = AuthCallbackParser.parse(rawUrl, BuildConfig.AUTH_SCHEME)) {
            AuthCallback.PasswordRecovery, AuthCallback.Generic -> parsed

            is AuthCallback.Error -> {
                authViewModel.reportDeepLinkErrorCode(parsed.code)
                return
            }

            null -> return
        }
        val client = SupabaseProvider.getOrNull()
        if (client == null) {
            authViewModel.reportDeepLinkError(
                IllegalStateException("Supabase is not configured"),
            )
            return
        }
        client.handleDeeplinks(
            actualIntent,
            onSessionSuccess = {
                // A PKCE code is single-use. Enter recovery mode only after
                // Supabase exchanged it for a real session successfully.
                authViewModel.reportDeepLinkSuccess(
                    isPasswordRecovery = callback == AuthCallback.PasswordRecovery,
                )
            },
            onError = authViewModel::reportDeepLinkError,
        )
    }
}
