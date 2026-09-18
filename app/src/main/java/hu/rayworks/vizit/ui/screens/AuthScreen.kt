package hu.rayworks.vizit.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import hu.rayworks.vizit.auth.AuthActionState
import hu.rayworks.vizit.auth.AuthOperation
import hu.rayworks.vizit.auth.AuthScreenMode
import hu.rayworks.vizit.auth.AuthViewModel
import hu.rayworks.vizit.ui.design.Vizit
import hu.rayworks.vizit.ui.design.components.VizitBanner
import hu.rayworks.vizit.ui.design.components.VizitBrandLockup
import hu.rayworks.vizit.ui.design.components.VizitButton
import hu.rayworks.vizit.ui.design.components.VizitButtonStyle
import hu.rayworks.vizit.ui.design.components.VizitPasswordField
import hu.rayworks.vizit.ui.design.components.VizitTextField
import hu.rayworks.vizit.ui.design.components.VizitTone

/**
 * One auth surface for every mode (login, register, forgot, reset, legal gate).
 * The brand lockup anchors the top, the form sits on the canvas without a card
 * around it, and exactly one primary action is offered at a time.
 */
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    forceNewPassword: Boolean = false,
    forceLegalAcceptance: Boolean = false,
) {
    val colors = Vizit.colors
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
    var name by rememberSaveable { mutableStateOf("") }
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

    fun clearError() {
        if (action is AuthActionState.Error) viewModel.clearActionState()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .padding(horizontal = Vizit.space.xl),
        verticalArrangement = Arrangement.spacedBy(Vizit.space.md),
    ) {
        Spacer(Modifier.height(Vizit.space.xxl))
        VizitBrandLockup(modifier = Modifier.fillMaxWidth())

        Column(verticalArrangement = Arrangement.spacedBy(Vizit.space.xs)) {
            Text(mode.title(), style = Vizit.type.h1, color = colors.textPrimary)
            mode.description()?.let {
                Text(it, style = Vizit.type.body, color = colors.textSecondary)
            }
        }

        Spacer(Modifier.height(Vizit.space.xxs))

        if (mode == AuthScreenMode.EMAIL_VERIFICATION_SENT || mode == AuthScreenMode.PASSWORD_RESET_SENT) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.primarySubtle, RoundedCornerShape(Vizit.radius.lg))
                    .padding(Vizit.space.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Vizit.space.sm),
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Outlined.MarkEmailRead,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(36.dp),
                )
                Text(
                    text = if (email.isNotBlank()) email else "Ellenőrizd a postafiókodat.",
                    style = Vizit.type.bodyStrong,
                    color = colors.textPrimary,
                )
            }
        }

        if (mode == AuthScreenMode.REGISTER) {
            VizitTextField(
                value = name,
                onValueChange = { name = it; clearError() },
                label = "Név",
                placeholder = "Teljes neved",
                enabled = !loading,
                imeAction = ImeAction.Next,
            )
        }

        if (mode.requiresEmail()) {
            VizitTextField(
                value = email,
                onValueChange = { email = it; clearError() },
                label = "E-mail-cím",
                placeholder = "nev@pelda.hu",
                enabled = !loading,
                keyboardType = KeyboardType.Email,
                imeAction = if (mode == AuthScreenMode.FORGOT_PASSWORD) ImeAction.Done else ImeAction.Next,
            )
        }

        if (mode.requiresPassword()) {
            VizitPasswordField(
                value = password,
                onValueChange = { password = it; clearError() },
                label = if (mode == AuthScreenMode.NEW_PASSWORD) "Új jelszó" else "Jelszó",
                enabled = !loading,
                imeAction = if (mode.requiresConfirmation()) ImeAction.Next else ImeAction.Done,
            )
        }

        if (mode.requiresConfirmation()) {
            VizitPasswordField(
                value = confirmation,
                onValueChange = { confirmation = it; clearError() },
                label = "Jelszó újra",
                enabled = !loading,
                imeAction = ImeAction.Done,
            )
        }

        if (mode == AuthScreenMode.REGISTER || mode == AuthScreenMode.LEGAL_ACCEPTANCE) {
            LegalAcceptanceSection(
                checked = legalAccepted,
                onCheckedChange = { legalAccepted = it; viewModel.clearActionState() },
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
            VizitButton(
                text = mode.primaryActionLabel(),
                onClick = {
                    viewModel.clearActionState()
                    when (mode) {
                        AuthScreenMode.LOGIN -> viewModel.login(email, password)
                        AuthScreenMode.REGISTER ->
                            viewModel.register(name, email, password, confirmation, legalAccepted)

                        AuthScreenMode.FORGOT_PASSWORD -> viewModel.requestPasswordReset(email)
                        AuthScreenMode.NEW_PASSWORD -> viewModel.updatePassword(password, confirmation)
                        AuthScreenMode.LEGAL_ACCEPTANCE -> viewModel.acceptLegalDocuments(legalAccepted)
                        else -> Unit
                    }
                },
                enabled = !loading &&
                    (
                        (mode != AuthScreenMode.REGISTER && mode != AuthScreenMode.LEGAL_ACCEPTANCE) ||
                            viewModel.legalDocumentsReady
                        ),
                loading = loading,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        when (action) {
            is AuthActionState.Error -> VizitBanner(text = action.message, tone = VizitTone.Error)
            is AuthActionState.Success -> VizitBanner(text = action.message, tone = VizitTone.Success)
            else -> Unit
        }

        if (mode == AuthScreenMode.LOGIN && viewModel.googleSignInEnabled) {
            AuthDividerLabel()
            VizitButton(
                text = "Folytatás Google-fiókkal",
                onClick = {
                    context.findActivity()?.let(viewModel::signInWithGoogle)
                        ?: viewModel.reportUiError("A Google-belépés ezen a képernyőn nem indítható el.")
                },
                style = VizitButtonStyle.Secondary,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (!forceNewPassword && !forceLegalAcceptance) {
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

        Spacer(Modifier.height(Vizit.space.xxl))
    }
}

@Composable
private fun AuthDividerLabel() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Vizit.space.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Vizit.space.sm),
    ) {
        Box(modifier = Modifier.weight(1f).height(1.dp).background(Vizit.colors.divider))
        Text("vagy", style = Vizit.type.caption, color = Vizit.colors.textMuted)
        Box(modifier = Modifier.weight(1f).height(1.dp).background(Vizit.colors.divider))
    }
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
    val colors = Vizit.colors
    val context = LocalContext.current
    val interactive = enabled && documentsReady

    Column(verticalArrangement = Arrangement.spacedBy(Vizit.space.xs)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = interactive, role = Role.Checkbox) { onCheckedChange(!checked) }
                .semantics {
                    toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
                    contentDescription =
                        "Elolvastam és elfogadom az adatkezelési tájékoztatót és az ÁSZF-et."
                }
                .padding(vertical = Vizit.space.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Vizit.space.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        if (checked) colors.primary else Color.Transparent,
                        RoundedCornerShape(Vizit.radius.xs),
                    )
                    .border(
                        if (checked) 0.dp else 1.5.dp,
                        if (interactive) colors.borderStrong else colors.border,
                        RoundedCornerShape(Vizit.radius.xs),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (checked) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = colors.textOnBrand,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Text(
                text = "Elolvastam és elfogadom az adatkezelési tájékoztatót és az ÁSZF-et.",
                style = Vizit.type.bodySmall,
                color = if (interactive) colors.textPrimary else colors.textDisabled,
                modifier = Modifier.weight(1f),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Vizit.space.md)) {
            LegalLink("Adatkezelés", interactive) {
                if (!context.openWebUrl(privacyPolicyUrl)) onOpenFailed()
            }
            LegalLink("ÁSZF", interactive) {
                if (!context.openWebUrl(termsUrl)) onOpenFailed()
            }
        }

        if (!documentsReady) {
            VizitBanner(
                text = "A folytatás a végleges jogi dokumentumok beállításáig nem aktiválható.",
                tone = VizitTone.Warning,
            )
        }
    }
}

@Composable
private fun LegalLink(text: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        style = Vizit.type.label,
        color = if (enabled) Vizit.colors.primary else Vizit.colors.textDisabled,
        modifier = Modifier
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = Vizit.space.xs),
    )
}

@Composable
private fun AuthNavigation(
    mode: AuthScreenMode,
    canUseDebugLocalProfile: Boolean,
    onModeChange: (AuthScreenMode) -> Unit,
    onUseDebugLocalProfile: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Vizit.space.xxs),
    ) {
        when (mode) {
            AuthScreenMode.LOGIN -> {
                VizitButton(
                    text = "Elfelejtett jelszó",
                    onClick = { onModeChange(AuthScreenMode.FORGOT_PASSWORD) },
                    style = VizitButtonStyle.Tertiary,
                )
                VizitButton(
                    text = "Nincs még fiókod? Regisztráció",
                    onClick = { onModeChange(AuthScreenMode.REGISTER) },
                    style = VizitButtonStyle.Tertiary,
                )
                if (canUseDebugLocalProfile) {
                    VizitButton(
                        text = "DEV: helyi tesztprofil",
                        onClick = onUseDebugLocalProfile,
                        style = VizitButtonStyle.Tertiary,
                    )
                }
            }

            AuthScreenMode.EMAIL_VERIFICATION_SENT,
            AuthScreenMode.PASSWORD_RESET_SENT,
            AuthScreenMode.REGISTER,
            AuthScreenMode.FORGOT_PASSWORD -> VizitButton(
                text = "Vissza a bejelentkezéshez",
                onClick = { onModeChange(AuthScreenMode.LOGIN) },
                style = VizitButtonStyle.Tertiary,
            )

            AuthScreenMode.NEW_PASSWORD,
            AuthScreenMode.LEGAL_ACCEPTANCE -> Unit
        }
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
    AuthScreenMode.LOGIN -> "Lépj be, és add át a névjegyed egyetlen érintéssel."
    AuthScreenMode.REGISTER -> "Pár adat, és kész is a digitális névjegyed."
    AuthScreenMode.EMAIL_VERIFICATION_SENT ->
        "Megerősítő e-mailt küldtünk. Ellenőrizd a postafiókodat, majd nyisd meg a levélben kapott linket."

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
    return runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }.isSuccess
}
