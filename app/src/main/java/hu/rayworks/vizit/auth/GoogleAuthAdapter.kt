package hu.rayworks.vizit.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import hu.rayworks.vizit.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken

class GoogleAuthAdapter(private val client: SupabaseClient) {
    val isEnabled: Boolean get() = BuildConfig.GOOGLE_SIGN_IN_ENABLED && BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    suspend fun signIn(activity: Activity) {
        check(isEnabled) { "Google Sign-In is not configured." }
        val option = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = CredentialManager.create(activity).getCredential(activity, request).credential
        check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Unexpected credential type."
        }
        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
        client.auth.signInWith(IDToken) {
            idToken = googleCredential.idToken
            provider = Google
        }
        checkNotNull(client.auth.currentSessionOrNull()?.user?.id) {
            "Supabase session was not created after Google Sign-In."
        }
    }
}
