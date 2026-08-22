package hu.rayworks.vizit.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import hu.rayworks.vizit.auth.AuthActionState
import hu.rayworks.vizit.auth.AuthOperation
import hu.rayworks.vizit.auth.AuthScreenMode
import hu.rayworks.vizit.auth.AuthViewModel
import hu.rayworks.vizit.ui.components.VizitBrandLockup

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    forceNewPassword: Boolean = false,
    forceLegalAcceptance: Boolean = false,
) {
    val context = LocalContext.current
    var mode by rememberSaveable(forceNewPassword, forceLegalAcceptance) {
        mutableStateOf(
            when {
                forceNewPassword -> AuthScreenMode.NEW_PASSWORD
                forceLegalAcceptance -> AuthScreenMode.LEGAL_ACCEPTANCE
                else -> AuthScreenMode.LOGIN
            },
        )
    }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var legalAccepted by rememberSaveable { mutableStateOf(false) }
    val action = viewModel.actionState
    val loading = action is AuthActionState.Loading

    LaunchedEffect(action) {
        when ((action as? AuthActionState.Success)?.operation) {
            AuthOperation.REGISTER -> {
                password = ""
                confirmation = ""
                mode = AuthScreenMode.EMAIL_VERIFICATION_SENT
            }

            AuthOperation.PASSWORD_RESET_REQUEST -> mode = AuthScreenMode.PASSWORD_RESET_SENT
            else -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        VizitBrandLockup(modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(18.dp))
        Text(mode.title(), style = MaterialTheme.typography.headlineMedium)
        mode.description()?.let { description ->
            Text(
                text = description,
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(24.dp))

        if (mode.requiresEmail()) {
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    if (action is AuthActionState.Error) viewModel.clearActionState()
                },
                label = { Text("E-mail-cím") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = if (mode == AuthScreenMode.FORGOT_PASSWORD) ImeAction.Done else ImeAction.Next,
                ),
                singleLine = true,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
        }

        if (mode.requiresPassword()) {
            PasswordField(
                value = password,
                onValueChange = {
                    password = it
                    if (action is AuthActionState.Error) viewModel.clearActionState()
                },
                label = if (mode == AuthScreenMode.NEW_PASSWORD) "Új jelszó" else "Jelszó",
                enabled = !loading,
                imeAction = if (mode.requiresConfirmation()) ImeAction.Next else ImeAction.Done,
            )
        }

        if (mode.requiresConfirmation()) {
            Spacer(Modifier.height(12.dp))
            PasswordField(
                value = confirmation,
                onValueChange = {
                    confirmation = it
                    if (action is AuthActionState.Error) viewModel.clearActionState()
                },
                label = "Jelszó újra",
                enabled = !loading,
                imeAction = ImeAction.Done,
            )
        }

        if (mode == AuthScreenMode.REGISTER || mode == AuthScreenMode.LEGAL_ACCEPTANCE) {
            LegalAcceptanceSection(
                checked = legalAccepted,
                onCheckedChange = {
                    legalAccepted = it
                    viewModel.clearActionState()
                },
                documentsReady = viewModel.legalDocumentsReady,
                privacyPolicyUrl = viewModel.privacyPolicyUrl,
                termsUrl = viewModel.termsUrl,
                enabled = !loading,
                onOpenFailed = {
                    viewModel.reportUiError("A dokumentum nem nyitható meg ezen az eszközön.")
                },
            )
        }

        if (mode.hasPrimaryAction()) {
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    viewModel.clearActionState()
                    when (mode) {
                        AuthScreenMode.LOGIN -> viewModel.login(email, password)
                        AuthScreenMode.REGISTER ->
                            viewModel.register(email, password, confirmation, legalAccepted)

                        AuthScreenMode.FORGOT_PASSWORD -> viewModel.requestPasswordReset(email)
                        AuthScreenMode.NEW_PASSWORD -> viewModel.updatePassword(password, confirmation)
                        AuthScreenMode.LEGAL_ACCEPTANCE -> viewModel.acceptLegalDocuments(legalAccepted)
                        else -> Unit
                    }
                },
                enabled = !loading &&
                    (mode != AuthScreenMode.REGISTER && mode != AuthScreenMode.LEGAL_ACCEPTANCE ||
                        viewModel.legalDocumentsReady),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(mode.primaryActionLabel())
                }
            }
        }

        AuthStatus(action)

        if (mode == AuthScreenMode.LOGIN && viewModel.googleSignInEnabled) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    context.findActivity()?.let(viewModel::signInWithGoogle)
                        ?: viewModel.reportUiError("A Google-belépés ezen a képernyőn nem indítható el.")
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Folytatás Google-fiókkal")
            }
        }

        if (!forceNewPassword && !forceLegalAcceptance) {
            Spacer(Modifier.height(10.dp))
            AuthNavigation(
                mode = mode,
                canUseDebugLocalProfile = viewModel.canUseDebugLocalProfile,
                onModeChange = {
                    viewModel.clearActionState()
                    password = ""
                    confirmation = ""
                    mode = it
                },
                onUseDebugLocalProfile = viewModel::useDebugLocalProfile,
            )
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    imeAction: ImeAction,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction,
        ),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }, enabled = enabled) {
                Icon(
                    imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (visible) "Jelszó elrejtése" else "Jelszó megjelenítése",
                )
            }
        },
        singleLine = true,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun LegalAcceptanceSection(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    documentsReady: Boolean,
    privacyPolicyUrl: String,
    termsUrl: String,
    enabled: Boolean,
    onOpenFailed: () -> Unit,
) {
    val context = LocalContext.current
    Spacer(Modifier.height(12.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled && documentsReady,
        )
        Text(
            text = "Elolvastam és elfogadom az adatkezelési tájékoztatót és az ÁSZF-et.",
            modifier = Modifier.weight(1f),
        )
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        TextButton(
            onClick = { if (!context.openWebUrl(privacyPolicyUrl)) onOpenFailed() },
            enabled = enabled && documentsReady,
        ) {
            Text("Adatkezelési tájékoztató")
        }
        TextButton(
            onClick = { if (!context.openWebUrl(termsUrl)) onOpenFailed() },
            enabled = enabled && documentsReady,
        ) {
            Text("Általános Szerződési Feltételek")
        }
    }
    if (!documentsReady) {
        Text(
            text = "A folytatás a végleges jogi dokumentumok beállításáig nem aktiválható.",
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun AuthStatus(action: AuthActionState) {
    when (action) {
        is AuthActionState.Error -> Text(
            text = action.message,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 14.dp),
        )

        is AuthActionState.Success -> Text(
            text = action.message,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 14.dp),
        )

        else -> Unit
    }
}

@Composable
private fun AuthNavigation(
    mode: AuthScreenMode,
    canUseDebugLocalProfile: Boolean,
    onModeChange: (AuthScreenMode) -> Unit,
    onUseDebugLocalProfile: () -> Unit,
) {
    when (mode) {
        AuthScreenMode.LOGIN -> {
            TextButton(onClick = { onModeChange(AuthScreenMode.FORGOT_PASSWORD) }) {
                Text("Elfelejtett jelszó")
            }
            TextButton(onClick = { onModeChange(AuthScreenMode.REGISTER) }) {
                Text("Nincs még fiókod? Regisztráció")
            }
            if (canUseDebugLocalProfile) {
                TextButton(onClick = onUseDebugLocalProfile) {
                    Text("DEV: helyi tesztprofil")
                }
            }
        }

        AuthScreenMode.EMAIL_VERIFICATION_SENT,
        AuthScreenMode.PASSWORD_RESET_SENT,
        AuthScreenMode.REGISTER,
        AuthScreenMode.FORGOT_PASSWORD -> TextButton(onClick = { onModeChange(AuthScreenMode.LOGIN) }) {
            Text("Vissza a bejelentkezéshez")
        }

        AuthScreenMode.NEW_PASSWORD,
        AuthScreenMode.LEGAL_ACCEPTANCE -> Unit
    }
}

private fun AuthScreenMode.title(): String = when (this) {
    AuthScreenMode.LOGIN -> "Bejelentkezés"
    AuthScreenMode.REGISTER -> "Fiók létrehozása"
    AuthScreenMode.EMAIL_VERIFICATION_SENT -> "Erősítsd meg az e-mail-címedet"
    AuthScreenMode.FORGOT_PASSWORD -> "Jelszó helyreállítása"
    AuthScreenMode.PASSWORD_RESET_SENT -> "Ellenőrizd a postafiókodat"
    AuthScreenMode.NEW_PASSWORD -> "Új jelszó"
    AuthScreenMode.LEGAL_ACCEPTANCE -> "Adatkezelés és feltételek"
}

private fun AuthScreenMode.description(): String? = when (this) {
    AuthScreenMode.EMAIL_VERIFICATION_SENT ->
        "A megerősítő link megnyitása után visszatérhetsz a VIZIT alkalmazásba."

    AuthScreenMode.PASSWORD_RESET_SENT ->
        "A helyreállító linkkel biztonságosan beállíthatod az új jelszavadat."

    AuthScreenMode.NEW_PASSWORD -> "Adj meg egy új, legalább 8 karakteres jelszót."
    AuthScreenMode.LEGAL_ACCEPTANCE ->
        "A profil használata előtt olvasd el és fogadd el a jelenlegi dokumentumokat."

    else -> null
}

private fun AuthScreenMode.requiresEmail(): Boolean =
    this == AuthScreenMode.LOGIN ||
        this == AuthScreenMode.REGISTER ||
        this == AuthScreenMode.FORGOT_PASSWORD

private fun AuthScreenMode.requiresPassword(): Boolean =
    this == AuthScreenMode.LOGIN ||
        this == AuthScreenMode.REGISTER ||
        this == AuthScreenMode.NEW_PASSWORD

private fun AuthScreenMode.requiresConfirmation(): Boolean =
    this == AuthScreenMode.REGISTER || this == AuthScreenMode.NEW_PASSWORD

private fun AuthScreenMode.hasPrimaryAction(): Boolean = when (this) {
    AuthScreenMode.LOGIN,
    AuthScreenMode.REGISTER,
    AuthScreenMode.FORGOT_PASSWORD,
    AuthScreenMode.NEW_PASSWORD,
    AuthScreenMode.LEGAL_ACCEPTANCE -> true

    AuthScreenMode.EMAIL_VERIFICATION_SENT,
    AuthScreenMode.PASSWORD_RESET_SENT -> false
}

private fun AuthScreenMode.primaryActionLabel(): String = when (this) {
    AuthScreenMode.LOGIN -> "Bejelentkezés"
    AuthScreenMode.REGISTER -> "Regisztráció"
    AuthScreenMode.FORGOT_PASSWORD -> "Helyreállító e-mail küldése"
    AuthScreenMode.NEW_PASSWORD -> "Új jelszó mentése"
    AuthScreenMode.LEGAL_ACCEPTANCE -> "Elfogadás és tovább"
    else -> "Tovább"
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Context.openWebUrl(url: String): Boolean {
    if (url.isBlank()) return false
    return runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }.isSuccess
}
