package hu.rayworks.vizit.v10.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import hu.rayworks.vizit.R
import hu.rayworks.vizit.auth.AuthActionState
import hu.rayworks.vizit.auth.AuthOperation
import hu.rayworks.vizit.auth.AuthViewModel
import hu.rayworks.vizit.v10.data.AppState
import hu.rayworks.vizit.v10.data.AuthMode
import hu.rayworks.vizit.v10.data.Gate
import hu.rayworks.vizit.v10.data.PRIVACY_URL
import hu.rayworks.vizit.v10.data.TERMS_URL
import hu.rayworks.vizit.v10.ui.LocalActions
import hu.rayworks.vizit.v10.ui.components.Banner
import hu.rayworks.vizit.v10.ui.components.EmptyState
import hu.rayworks.vizit.v10.ui.components.OutlinedField
import hu.rayworks.vizit.v10.ui.components.PrimaryButton
import hu.rayworks.vizit.v10.ui.components.Spinner
import hu.rayworks.vizit.v10.ui.components.TextLink
import hu.rayworks.vizit.v10.ui.components.Tone
import hu.rayworks.vizit.v10.ui.components.noRippleClickable
import hu.rayworks.vizit.v10.ui.icons.VIcons
import hu.rayworks.vizit.v10.ui.theme.V

private val EMAIL = Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)

private fun emailIssue(e: String): String? = when {
    e.isBlank() -> "Add meg az e-mail-címedet."
    !EMAIL.matches(e.trim()) -> "Az e-mail-cím formátuma nem megfelelő."
    else -> null
}

private fun passwordIssue(p: String): String? = when {
    p.isEmpty() -> "Add meg a jelszavadat."
    p.length < 8 -> "A jelszó legalább 8 karakter hosszú legyen."
    p.none { it.isLetter() } -> "A jelszó tartalmazzon legalább egy betűt."
    p.none { it.isDigit() } -> "A jelszó tartalmazzon legalább egy számot."
    else -> null
}

/** Márkajel: fehér lapon a VIZIT logó, alatta a felirat és a szlogen. */
@Composable
private fun BrandLockup() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) { Image(painterResource(R.drawable.vizit_logo), contentDescription = null, modifier = Modifier.size(52.dp)) }
        Text("VIZIT", fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.18.em)
        Text("Egy érintés. Egy kapcsolat.", fontSize = 14.sp, color = V.sub)
    }
}

/** Google „G” négy színnel (a gomb ikonja). */
@Composable
private fun GoogleMark() {
    Canvas(Modifier.size(18.dp)) {
        val sw = 3.2.dp.toPx()
        val inset = sw / 2
        val sz = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw)
        val tl = Offset(inset, inset)
        drawArc(Color(0xFFEA4335), 200f, 110f, false, tl, sz, style = Stroke(sw))
        drawArc(Color(0xFFFBBC05), 150f, 50f, false, tl, sz, style = Stroke(sw))
        drawArc(Color(0xFF34A853), 40f, 110f, false, tl, sz, style = Stroke(sw))
        drawArc(Color(0xFF4285F4), -10f, 50f, false, tl, sz, style = Stroke(sw))
        drawLine(Color(0xFF4285F4), Offset(size.width / 2, size.height / 2), Offset(size.width - inset, size.height / 2), strokeWidth = sw)
    }
}

/** Jogi hozzájárulás: jelölőnégyzet + Adatkezelés / ÁSZF linkek. */
@Composable
private fun Consent(checked: Boolean, onToggle: () -> Unit) {
    val actions = LocalActions.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onToggle)
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val shape = RoundedCornerShape(6.dp)
            Box(
                Modifier
                    .size(24.dp)
                    .clip(shape)
                    .then(if (checked) Modifier.background(V.blueFill) else Modifier.border(2.dp, V.outline, shape)),
                contentAlignment = Alignment.Center,
            ) { if (checked) Icon(VIcons.checkBold, contentDescription = null, tint = V.onBlueFill, modifier = Modifier.size(16.dp)) }
            Text("Elfogadom az adatkezelési tájékoztatót és az ÁSZF-et.", fontSize = 14.sp, modifier = Modifier.weight(1f))
        }
        Row(Modifier.padding(start = 28.dp)) {
            TextLink("Adatkezelés") { actions.openUrl(PRIVACY_URL) }
            TextLink("ÁSZF") { actions.openUrl(TERMS_URL) }
        }
    }
}

@Composable
private fun EnvelopeCard(email: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(V.blueSoft)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(VIcons.mail, contentDescription = null, tint = V.blue, modifier = Modifier.size(24.dp))
        Text(email.ifBlank { "Ellenőrizd a postafiókodat." }, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * Belépési képernyők (éles AuthScreen hét módja): Bejelentkezés, Regisztráció, megerősítő levél,
 * elfelejtett jelszó, helyreállító levél, új jelszó, jogi elfogadás. Valódi ellenőrzésekkel,
 * háttér nélkül: sikeres kitöltés után az app megnyílik.
 */
@Composable
fun AuthScreen(app: AppState, mode: AuthMode, viewModel: AuthViewModel) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    var pw2 by remember { mutableStateOf("") }
    var consent by remember { mutableStateOf(false) }
    var validationError by remember(mode) { mutableStateOf<String?>(null) }
    val actionState = viewModel.actionState
    val busy = actionState is AuthActionState.Loading

    LaunchedEffect(actionState) {
        when (val state = actionState) {
            is AuthActionState.Success -> {
                when (state.operation) {
                    AuthOperation.REGISTER -> {
                        // Registration must never leave the filled form behind.
                        // Keep only the address needed by the confirmation card.
                        name = ""
                        pw = ""
                        pw2 = ""
                        consent = false
                        app.authBanner = state.message to true
                        app.gate = Gate.Auth(AuthMode.EmailSent)
                    }
                    AuthOperation.PASSWORD_RESET_REQUEST -> {
                        app.authBanner = state.message to true
                        app.gate = Gate.Auth(AuthMode.ResetSent)
                    }
                    AuthOperation.PASSWORD_UPDATE,
                    AuthOperation.LEGAL_ACCEPTANCE -> {
                        app.toast(state.message)
                        app.gate = null
                    }
                    AuthOperation.LOGIN,
                    AuthOperation.GOOGLE_SIGN_IN -> app.toast(state.message)
                    AuthOperation.EMAIL_CONFIRMATION -> {
                        // The confirmation deep link always returns to a clean
                        // login form on Android, never straight into the wizard.
                        name = ""
                        email = ""
                        pw = ""
                        pw2 = ""
                        consent = false
                        app.authBanner = state.message to true
                        app.gate = Gate.Auth(AuthMode.Login)
                    }
                    AuthOperation.LOGOUT,
                    AuthOperation.DELETE_ACCOUNT -> {
                        app.authBanner = state.message to true
                        app.gate = Gate.Auth(AuthMode.Login)
                    }
                }
                viewModel.clearActionState()
            }
            else -> Unit
        }
    }

    fun edited() {
        validationError = null
        if (viewModel.actionState is AuthActionState.Error) viewModel.clearActionState()
    }
    fun go(m: AuthMode) {
        validationError = null
        if (m == AuthMode.Login) {
            name = ""
            email = ""
            consent = false
        }
        pw = ""
        pw2 = ""
        viewModel.clearActionState()
        app.authBanner = null
        app.gate = Gate.Auth(m)
    }
    fun submit(issue: String?, then: () -> Unit) {
        validationError = issue
        if (issue == null) then()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(V.bg)
            .noRippleClickable { }
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BrandLockup()
        Spacer(Modifier.height(8.dp))
        val title = when (mode) {
            AuthMode.Login -> "Bejelentkezés"
            AuthMode.Register -> "Fiók létrehozása"
            AuthMode.EmailSent -> "Erősítsd meg az e-mail-címedet"
            AuthMode.Forgot -> "Jelszó helyreállítása"
            AuthMode.ResetSent -> "Ellenőrizd a postafiókodat"
            AuthMode.NewPassword -> "Új jelszó"
            AuthMode.Legal -> "Adatkezelés és feltételek"
        }
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)

        Column(Modifier.widthIn(max = 420.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            val banner = app.authBanner
            if (banner != null) Banner(banner.first, if (banner.second) Tone.Success else Tone.Error)

            when (mode) {
                AuthMode.Login -> {
                    OutlinedField("E-mail-cím", email, keyboardType = KeyboardType.Email, placeholder = "nev@pelda.hu") { email = it; edited() }
                    OutlinedField("Jelszó", pw, password = true, imeAction = ImeAction.Done) { pw = it; edited() }
                }
                AuthMode.Register -> {
                    OutlinedField("Név", name, placeholder = "Teljes neved") { name = it; edited() }
                    OutlinedField("E-mail-cím", email, keyboardType = KeyboardType.Email, placeholder = "nev@pelda.hu") { email = it; edited() }
                    OutlinedField("Jelszó", pw, password = true, supporting = "Legalább 8 karakter, betű és szám") { pw = it; edited() }
                    OutlinedField("Jelszó újra", pw2, password = true, imeAction = ImeAction.Done) { pw2 = it; edited() }
                    Consent(consent) { consent = !consent; edited() }
                }
                AuthMode.EmailSent, AuthMode.ResetSent -> {
                    EnvelopeCard(email)
                    Text("Nyisd meg a levélben kapott linket.", fontSize = 14.sp, color = V.sub, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                AuthMode.Forgot -> {
                    OutlinedField("E-mail-cím", email, keyboardType = KeyboardType.Email, placeholder = "nev@pelda.hu", imeAction = ImeAction.Done) { email = it; edited() }
                }
                AuthMode.NewPassword -> {
                    OutlinedField("Új jelszó", pw, password = true, supporting = "Legalább 8 karakter, betű és szám") { pw = it; edited() }
                    OutlinedField("Jelszó újra", pw2, password = true, imeAction = ImeAction.Done) { pw2 = it; edited() }
                }
                AuthMode.Legal -> Consent(consent) { consent = !consent; edited() }
            }

            val err = validationError ?: (actionState as? AuthActionState.Error)?.message
            if (err != null) Banner(err, Tone.Error)

            val primary: Pair<String, () -> Unit>? = when (mode) {
                AuthMode.Login -> "Bejelentkezés" to {
                    submit(emailIssue(email) ?: (if (pw.isEmpty()) "Add meg a jelszavadat." else null)) {
                        viewModel.login(email.trim(), pw)
                    }
                }
                AuthMode.Register -> "Regisztráció" to {
                    val issue = when {
                        name.isBlank() -> "Add meg a nevedet."
                        name.trim().length < 2 -> "A név legalább 2 karakter hosszú legyen."
                        name.trim().length > 100 -> "A név legfeljebb 100 karakter hosszú lehet."
                        else -> emailIssue(email) ?: passwordIssue(pw) ?: when {
                            pw2.isEmpty() -> "Ismételd meg a jelszavadat."
                            pw2 != pw -> "A két jelszó nem egyezik."
                            !consent -> "A regisztrációhoz fogadd el az adatkezelési tájékoztatót és az ÁSZF-et."
                            else -> null
                        }
                    }
                    submit(issue) { viewModel.register(name.trim(), email.trim(), pw, pw2, consent) }
                }
                AuthMode.Forgot -> "Helyreállító e-mail küldése" to {
                    submit(emailIssue(email)) { viewModel.requestPasswordReset(email.trim()) }
                }
                AuthMode.NewPassword -> "Új jelszó mentése" to {
                    val issue = passwordIssue(pw) ?: when {
                        pw2.isEmpty() -> "Ismételd meg az új jelszavadat."
                        pw2 != pw -> "A két jelszó nem egyezik."
                        else -> null
                    }
                    submit(issue) { viewModel.updatePassword(pw, pw2) }
                }
                AuthMode.Legal -> "Elfogadás és tovább" to {
                    submit(if (!consent) "A folytatáshoz fogadd el az adatkezelési tájékoztatót és az ÁSZF-et." else null) {
                        viewModel.acceptLegalDocuments(consent)
                    }
                }
                else -> null
            }
            if (primary != null) {
                if (busy) {
                    Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) { Spinner() }
                } else {
                    PrimaryButton(primary.first, onClick = primary.second)
                }
            }

            if (mode == AuthMode.Login) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).height(1.dp).background(V.line))
                    Text("vagy", fontSize = 13.sp, color = V.sub, modifier = Modifier.padding(horizontal = 12.dp))
                    Box(Modifier.weight(1f).height(1.dp).background(V.line))
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .border(1.dp, V.outline, RoundedCornerShape(26.dp))
                        .clickable(enabled = !busy && viewModel.googleSignInEnabled) {
                            context.findActivity()?.let(viewModel::signInWithGoogle)
                                ?: viewModel.reportUiError("A Google-belépés most nem indítható el.")
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GoogleMark()
                    Spacer(Modifier.size(10.dp))
                    Text("Folytatás Google-fiókkal", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = V.ink)
                }
            }

            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                when (mode) {
                    AuthMode.Login -> {
                        TextLink("Elfelejtett jelszó") { go(AuthMode.Forgot) }
                        TextLink("Nincs még fiókod? Regisztráció") { go(AuthMode.Register) }
                    }
                    AuthMode.Register, AuthMode.EmailSent, AuthMode.Forgot, AuthMode.ResetSent -> TextLink("Vissza a bejelentkezéshez") { go(AuthMode.Login) }
                    else -> Unit
                }
            }
        }
    }
}

/* ================================================================== állapot képernyők */

@Composable
private fun StateFrame(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(V.bg)
            .noRippleClickable { }
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { content() }
    }
}

/** Betöltés, betöltési hiba, lejárt munkamenet, sikertelen feltétel-ellenőrzés (éles VizitRoot állapotai). */
@Composable
fun GateScreen(app: AppState, gate: Gate, viewModel: AuthViewModel) {
    when (gate) {
        is Gate.Auth -> AuthScreen(app, gate.mode, viewModel)
        Gate.Loading -> {
            StateFrame {
                BrandLockup()
                Spacer(Modifier.height(28.dp))
                Spinner()
                Spacer(Modifier.height(12.dp))
                Text("Névjegy ellenőrzése…", fontSize = 14.sp, color = V.sub)
            }
        }
        Gate.ProfileError -> StateFrame {
            EmptyState(VIcons.warning, "A névjegy most nem tölthető be", "Próbáld újra, amikor van internetkapcsolat.", Tone.Error, "Újrapróbálás", {
                app.gate = Gate.Loading
                app.runtime?.onRetryProfileLoad?.invoke()
            })
        }
        Gate.SessionExpired -> StateFrame {
            EmptyState(VIcons.timer, "A munkamenet lejárt", "A helyi profil továbbra is használható.", Tone.Warn, "Újra bejelentkezem", {
                app.runtime?.onLogout?.invoke()
                app.gate = Gate.Auth(AuthMode.Login)
            })
            TextLink("Folytatás offline") {
                app.offline = true
                app.gate = null
            }
        }
        Gate.LegalFailed -> StateFrame {
            EmptyState(VIcons.shield, "Nem sikerült ellenőrizni a feltételeket", "Ehhez internetkapcsolat szükséges.", Tone.Error, "Újrapróbálás", {
                app.gate = Gate.Loading
                app.runtime?.onRetryLegalAcceptance?.invoke()
            })
            TextLink("Kijelentkezés") { app.logout() }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
