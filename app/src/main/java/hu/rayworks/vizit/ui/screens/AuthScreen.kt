package hu.rayworks.vizit.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import hu.rayworks.vizit.auth.AuthActionState
import hu.rayworks.vizit.auth.AuthScreenMode
import hu.rayworks.vizit.auth.AuthViewModel

@Composable
fun AuthScreen(viewModel: AuthViewModel, forceNewPassword: Boolean = false) {
    val context = LocalContext.current
    var mode by rememberSaveable(forceNewPassword) {
        mutableStateOf(if (forceNewPassword) AuthScreenMode.NEW_PASSWORD else AuthScreenMode.LOGIN)
    }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    val action = viewModel.actionState
    val loading = action is AuthActionState.Loading

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("VIZIT", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text(
            when (mode) {
                AuthScreenMode.LOGIN -> "Bejelentkezés"
                AuthScreenMode.REGISTER -> "Fiók létrehozása"
                AuthScreenMode.FORGOT_PASSWORD -> "Jelszó helyreállítása"
                AuthScreenMode.NEW_PASSWORD -> "Új jelszó"
            },
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(24.dp))

        if (mode != AuthScreenMode.NEW_PASSWORD) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-mail-cím") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
        }

        if (mode == AuthScreenMode.LOGIN || mode == AuthScreenMode.REGISTER || mode == AuthScreenMode.NEW_PASSWORD) {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(if (mode == AuthScreenMode.NEW_PASSWORD) "Új jelszó" else "Jelszó") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (mode == AuthScreenMode.REGISTER || mode == AuthScreenMode.NEW_PASSWORD) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = confirmation,
                onValueChange = { confirmation = it },
                label = { Text("Jelszó újra") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                viewModel.clearActionState()
                when (mode) {
                    AuthScreenMode.LOGIN -> viewModel.login(email, password)
                    AuthScreenMode.REGISTER -> viewModel.register(email, password, confirmation)
                    AuthScreenMode.FORGOT_PASSWORD -> viewModel.requestPasswordReset(email)
                    AuthScreenMode.NEW_PASSWORD -> viewModel.updatePassword(password, confirmation)
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (loading) CircularProgressIndicator()
            else Text(
                when (mode) {
                    AuthScreenMode.LOGIN -> "Bejelentkezés"
                    AuthScreenMode.REGISTER -> "Regisztráció"
                    AuthScreenMode.FORGOT_PASSWORD -> "Helyreállító e-mail küldése"
                    AuthScreenMode.NEW_PASSWORD -> "Új jelszó mentése"
                },
            )
        }

        when (val state = action) {
            is AuthActionState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 14.dp))
            is AuthActionState.Success -> Text(state.message, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 14.dp))
            else -> Unit
        }

        if (mode == AuthScreenMode.LOGIN && viewModel.googleSignInEnabled) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { context.findActivity()?.let(viewModel::signInWithGoogle) },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Folytatás Google-fiókkal") }
        }

        if (!forceNewPassword) {
            Spacer(Modifier.height(10.dp))
            when (mode) {
                AuthScreenMode.LOGIN -> {
                    TextButton(onClick = { viewModel.clearActionState(); mode = AuthScreenMode.FORGOT_PASSWORD }) { Text("Elfelejtett jelszó") }
                    TextButton(onClick = { viewModel.clearActionState(); mode = AuthScreenMode.REGISTER }) { Text("Nincs még fiókod? Regisztráció") }
                    if (viewModel.canUseDebugLocalProfile) {
                        TextButton(onClick = viewModel::useDebugLocalProfile) { Text("DEV: helyi tesztprofil") }
                    }
                }
                else -> TextButton(onClick = { viewModel.clearActionState(); mode = AuthScreenMode.LOGIN }) { Text("Vissza a bejelentkezéshez") }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
