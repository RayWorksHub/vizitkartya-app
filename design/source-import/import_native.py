#!/usr/bin/env python3
"""One-time import of the user's native ZIP wizard. Not an app build step."""
from pathlib import Path
import base64, hashlib, re, subprocess, zlib
ROOT=Path.cwd()
A='app/src/main/java/hu/rayworks/vizit/'
I='ios/Vizit/App/'
def put(path,text):
    p=ROOT/path; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text,encoding='utf-8')
def change(path,old,new,count=1):
    p=ROOT/path; s=p.read_text()
    assert s.count(old)==count,(path,old[:120],s.count(old),count)
    p.write_text(s.replace(old,new),encoding='utf-8')
def blob(sha):
    b=subprocess.check_output(['git','cat-file','blob',sha])
    assert hashlib.sha1(b'blob '+str(len(b)).encode()+b'\0'+b).hexdigest()==sha
    return b.decode()
source=''.join(''.join((ROOT/f'design/source-import/swift.{n:02d}.b64').read_text().split()) for n in range(4))
swift=zlib.decompress(base64.b64decode(source,validate=True))
assert hashlib.sha256(swift).hexdigest()=='949092f44615cd031fa617501f481fc026d17bf33019fd196daaa35e9b7255f2'
put(I+'ZIPWizardUI.swift',swift.decode().replace('"ZIPV"','"V"'))
p=ROOT/(I+'ProfileEditor.swift'); s=p.read_text(); marker='private struct V10WizardVisualGroup'
assert s.count(marker)==1
p.write_text(s[:s.index(marker)],encoding='utf-8')
# Exact Android UI blobs verified against the uploaded ZIP after package renaming.
blobs={
'ui/wizard/Wizard.kt':'9a2fa1000f1e675ac18b4c162e549a52ab29a094',
'ui/wizard/WizardIntro.kt':'46a3cd63f6d8058fc79f04127d95cf73dcb47381',
'ui/wizard/WizardBlocks.kt':'9c0959e90d2d88d9065d2011794f69f649ac0a1c',
'ui/wizard/WizardState.kt':'b93a292416590707cf59a1e79f157ac7eb119ef2',
'ui/wizard/ImageLoad.kt':'52cfd8aab8810f7e96b470490f7c2c1f005c2e52',
'ui/components/Common.kt':'e9497b43db49b1fcc1b1989c1939d1bf60fc797d',
'ui/components/Gradients.kt':'674f7216199bc6afdfacbe85f766f89a7b9f58f1',
'ui/icons/VizitIcons.kt':'65b1aba0d9e32a4a664961900ca5d82dfa2114ba',
'ui/theme/Tokens.kt':'f8a5c609d9e62857e43bcac7995c27a24241fb11',
}
for path,sha in blobs.items(): put(A+'v10/'+path,blob(sha))
p=ROOT/(A+'v10/ui/components/Common.kt'); s=p.read_text()
s=s[:s.index('@Composable\nfun AvatarImage')]
s=s.replace('import hu.rayworks.vizit.R\n','').replace('import hu.rayworks.vizit.v10.data.ToastMsg\n','')
p.write_text(s)
p=ROOT/(A+'v10/ui/wizard/WizardBlocks.kt'); s=p.read_text()
s=s.replace('import hu.rayworks.vizit.R\n','')
s=s.replace('photo -> listOf("Mostani kép" to { wiz.setPic(kind, Pic.Res(R.drawable.avatar)) }, "Feltöltés" to { c.pickImage(kind) })','photo -> listOf("Feltöltés" to { c.pickImage(kind) })')
s=s.replace('else -> listOf("Feltöltés" to { c.pickImage(kind) }, "Minta" to { wiz.setPic(kind, Pic.Res(R.drawable.sample_logo)) })','else -> listOf("Feltöltés" to { c.pickImage(kind) })')
assert 'R.drawable' not in s
p.write_text(s)
change(A+'v10/ui/wizard/WizardState.kt','SlugState(true, "Szabad")','SlugState(true, "Formátum rendben")')
# Immutable presentation models only; never restore the old account state/backend.
m=blob('e01061c09be80cd3dd7f024adefda7e1cc25cebc')
start=m.index('enum class DesignLayout'); end=m.index('const val ACCOUNT_NAME')
assert end>start
model=m[start:end]
assert 'class AppState' not in model and 'SAMPLE_PROFILES' not in model
put(A+'v10/data/Model.kt','''package hu.rayworks.vizit.v10.data
import androidx.annotation.DrawableRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import hu.rayworks.vizit.v10.ui.wizard.WizardState
import hu.rayworks.vizit.BuildConfig

'''+model+'''
/** Presentation-only bridge; every save uses the current authenticated repository. */
class AppState(val wizard: WizardState) {
    var wizardOpen by mutableStateOf(true)
    var saving by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var onPublish: ((Profile) -> Unit)? = null
    var onExit: (() -> Unit)? = null
    fun addPublished(profile: Profile, isPublic: Boolean) {
        if (!saving && wizard.valid("done") && wizard.type != null) onPublish?.invoke(profile.copy(isPublic = isPublic))
    }
    fun closeWizard(force: Boolean) {
        if (saving) return
        if (wizard.dirty && !force) wizard.confirmOpen = true else onExit?.invoke()
    }
}
''')
put(A+'ui/screens/ProfileWizardScreen.kt',r'''package hu.rayworks.vizit.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.luminance
import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.data.card.CardColorway
import hu.rayworks.vizit.data.card.CardPresentation
import hu.rayworks.vizit.ui.design.Vizit
import hu.rayworks.vizit.v10.data.AppState
import hu.rayworks.vizit.v10.data.Pic
import hu.rayworks.vizit.v10.data.Profile
import hu.rayworks.vizit.v10.ui.theme.ThemeState
import hu.rayworks.vizit.v10.ui.wizard.WizardHost
import hu.rayworks.vizit.v10.ui.wizard.WizardState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/** The supplied native block pager, attached to the existing owner-isolated save path. */
@Composable
fun ProfileWizardScreen(
    onSave: suspend (ContactProfile) -> String?,
    presentation: CardPresentation,
    onAppearanceChange: (CardPresentation) -> Unit,
    onDone: () -> Unit,
    isAdditional: Boolean = false,
    onCancel: (() -> Unit)? = null,
) {
    val bridge = remember { AppState(WizardState(takenSlugs = { emptyList() })) }
    val scope = rememberCoroutineScope()
    val save by rememberUpdatedState(onSave)
    val done by rememberUpdatedState(onDone)
    val cancel by rememberUpdatedState(onCancel)
    val appearance by rememberUpdatedState(onAppearanceChange)
    val currentPresentation by rememberUpdatedState(presentation)
    val dark = Vizit.colors.canvas.luminance() < 0.5f
    SideEffect {
        ThemeState.dark = dark
        bridge.onExit = {
            if (cancel != null) cancel?.invoke()
            else { bridge.wizard.confirmOpen = false; bridge.wizard.introShown = true }
        }
        bridge.onPublish = { value ->
            if (!bridge.saving) {
                bridge.saving = true
                bridge.error = null
                scope.launch {
                    try {
                        val draft = withContext(Dispatchers.Default) { value.productionProfile() }
                        val issue = save(draft)
                        if (issue == null) {
                            val colorway = when (value.presetId) {
                                "markakek" -> CardColorway.BRAND
                                "smaragd" -> CardColorway.EMERALD
                                "ametiszt" -> CardColorway.AMETHYST
                                else -> CardColorway.INK
                            }
                            appearance(currentPresentation.copy(colorway = colorway))
                            // The repository owns the actual synchronization status.
                            // Never report a simulated or timer-based publication.
                            done()
                        } else bridge.error = issue
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        bridge.error = error.localizedMessage ?: "A névjegy mentése nem sikerült."
                    } finally { bridge.saving = false }
                }
            }
        }
    }
    BackHandler { bridge.closeWizard(false) }
    Box(Modifier.fillMaxSize()) {
        WizardHost(bridge)
        if (bridge.saving) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.15f)).clickable {},
                contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
    }
    bridge.error?.let { message ->
        AlertDialog(
            onDismissRequest = { bridge.error = null },
            title = { Text("A névjegy mentése nem sikerült") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { bridge.error = null }) { Text("Rendben") } },
        )
    }
}

private fun Profile.productionProfile(): ContactProfile = ContactProfile(
    fullName = name, company = company, jobTitle = title, phone = phone, email = email,
    address = address, bio = bio, website = checkedUrl(web), publicSlug = slug, isPublic = isPublic,
    photoBase64 = photo.jpeg(), logoBase64 = logo.jpeg(),
    linkedIn = checkedUrl(socials["linkedin"].orEmpty()),
    facebook = checkedUrl(socials["facebook"].orEmpty()),
    instagram = checkedUrl(socials["instagram"].orEmpty()),
    youtube = checkedUrl(socials["youtube"].orEmpty()),
    tiktok = checkedUrl(socials["tiktok"].orEmpty()),
    x = checkedUrl(socials["x"].orEmpty()),
    github = checkedUrl(socials["github"].orEmpty()),
    customSocial = checkedUrl(socials["other"].orEmpty()),
)

private fun checkedUrl(value: String): String {
    val v = value.trim()
    if (v.isEmpty()) return ""
    val uri = Uri.parse(v)
    require(uri.scheme == "https" && !uri.host.isNullOrBlank() && v.none(Char::isWhitespace)) {
        "Érvényes, https:// kezdetű webcímet adj meg."
    }
    return v
}

private fun Pic?.jpeg(): String {
    if (this == null) return ""
    require(this is Pic.Bmp) { "Válassz képet a készülékedről." }
    val source = bitmap.asAndroidBitmap()
    for (side in listOf(512, 384, 256)) {
        val scale = minOf(1f, side.toFloat() / max(source.width, source.height))
        val resized = Bitmap.createScaledBitmap(source,
            max(1, (source.width * scale).roundToInt()), max(1, (source.height * scale).roundToInt()), true)
        try {
            for (quality in listOf(82, 65, 45)) {
                val output = ByteArrayOutputStream()
                check(resized.compress(Bitmap.CompressFormat.JPEG, quality, output))
                if (output.size() <= 256 * 1024) return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
            }
        } finally { if (resized !== source) resized.recycle() }
    }
    error("A kiválasztott kép túl nagy. Válassz másik képet.")
}
''')
# Add source to the app target without changing signing or deployment identity.
p=ROOT/'ios/VIZIT.xcodeproj/project.pbxproj'; s=p.read_text()
file_id='F10320261003000000000001'; build_id='F10320261003000000000002'
assert file_id not in s
s=s.replace('objects = {','objects = {\n\t\t'+file_id+' = { isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = "Vizit/App/ZIPWizardUI.swift"; sourceTree = SOURCE_ROOT; };\n\t\t'+build_id+' = { isa = PBXBuildFile; fileRef = '+file_id+'; };',1)
pattern=r'(F8E70A210CD774FE36882A2A\s*=\s*\{[^}]*?files\s*=\s*\()'
s,n=re.subn(pattern,lambda m:m[1]+build_id+', ',s,count=1); assert n==1
p.write_text(s)
# Registration completion is a distinct route, not a banner above stale fields.
path=I+'VizitApp.swift'
change(path,'    private let uiTesting: Bool','    private let uiTesting: Bool\n    private let signupLoginKey = "vizit.require-login-after-signup"')
change(path,'    private func bootstrap() async {\n        guard let cloud else { return }','''    private func bootstrap() async {
        guard let cloud else { return }
        if UserDefaults.standard.bool(forKey: signupLoginKey) {
            try? await cloud.logout()
            clearUser()
            authStatus = .signedOut
            return
        }''')
change(path,'            let current = try await cloud.validSession()\n            userEmail','''            let current = try await cloud.validSession()
            guard !UserDefaults.standard.bool(forKey: signupLoginKey) else {
                clearUser(); authStatus = .signedOut; return
            }
            userEmail''')
change(path,'            let session = try await cloud.login(email: email, password: password)','''            let session = try await cloud.login(email: email, password: password)
            UserDefaults.standard.removeObject(forKey: self.signupLoginKey)''')
change(path,'            let session = try await cloud.googleLogin()','''            let session = try await cloud.googleLogin()
            UserDefaults.standard.removeObject(forKey: self.signupLoginKey)''')
change(path,'''            self.authStatus = .verificationSent(email.trimmingCharacters(in: .whitespacesAndNewlines))
            self.message = "Ha ez új e-mail-cím, elküldtük a megerősítő levelet. Ha már van fiókod, lépj be vagy kérj új jelszót."''','''            UserDefaults.standard.set(true, forKey: self.signupLoginKey)
            self.clearUser()
            self.message = nil
            self.authStatus = .verificationSent(email.trimmingCharacters(in: .whitespacesAndNewlines))''')
change(path,'    func handleCallback(_ url: URL) async {','''    func handleCallback(_ url: URL) async {
        let query = URLComponents(url: url, resolvingAgainstBaseURL: false)?.queryItems ?? []
        let fragment = URLComponents(string: "https://callback.invalid/?" + (url.fragment ?? ""))?.queryItems ?? []
        let signup = query.contains { $0.name == "flow" && $0.value == "signup" }
            || (query + fragment).contains { $0.name == "type" && $0.value == "signup" }
        if signup { UserDefaults.standard.set(true, forKey: signupLoginKey) }''')
change(path,'            if flow == "signup" {','            if signup {')
change(path,'                self.message = "Az e-mail-címed megerősítve. Most jelentkezz be, és utána létrehozhatod az első profilodat."','                self.message = nil')
change(path,'    func googleLogin() async {','''    func returnToLoginAfterRegistration() {
        message = nil
        authStatus = .signedOut
    }

    func googleLogin() async {''')
change(path,'            try await self.cloud?.changePassword(password)','''            try await self.cloud?.changePassword(password)
            UserDefaults.standard.removeObject(forKey: self.signupLoginKey)''')
change(path,'''            case .signedOut, .verificationSent:
                AuthScreen()''','''            case .signedOut:
                AuthScreen().id("auth-clean-login")

            case .verificationSent(let email):
                RegistrationVerificationScreen(email: email) {
                    store.returnToLoginAfterRegistration()
                }.id("auth-verification")''')
# DEBUG-only fixture exercises the real route without production emails or credentials.
change(path,'        if uiTesting {\n            do {','''        #if DEBUG
        if ProcessInfo.processInfo.arguments.contains("--ui-test-verification") {
            authStatus = .verificationSent("verification@example.com")
            return
        }
        #endif
        if uiTesting {
            do {''')
p=ROOT/(I+'AuthScreen.swift'); s=p.read_text()
start=s.index('                        if let address = verificationAddress {'); end=s.index('                        fields',start)
s=s[:start]+s[end:]
start=s.index('    private var verificationAddress: String? {'); end=s.index('    private var buildVersionLabel',start)
s=s[:start]+s[end:]
s+='''
/// Separate post-registration route: form fields and passwords do not survive here.
struct RegistrationVerificationScreen: View {
    let email: String
    let onLogin: () -> Void
    var body: some View {
        VizitScreen {
            ScrollView {
                VStack(spacing: VizitSpace.lg) {
                    VizitIconChip(systemImage: "envelope.badge", tint: VizitColor.primary,
                                  background: VizitColor.primarySubtle, size: 72)
                    Text("Ellenőrizd az e-mail-fiókodat")
                        .font(VizitFont.h1).foregroundStyle(VizitColor.textPrimary)
                    Text("Új fiók esetén a megerősítő hivatkozás erre a címre érkezik:")
                        .foregroundStyle(VizitColor.textSecondary)
                    Text(email).font(VizitFont.label).textSelection(.enabled)
                    Text("Nyisd meg a levélben kapott linket, majd jelentkezz be az alkalmazásban. Ha már van fiókod, lépj be vagy kérj új jelszót.")
                        .font(VizitFont.bodySmall).foregroundStyle(VizitColor.textSecondary)
                    VizitButton(title: "Vissza a bejelentkezéshez", action: onLogin)
                        .accessibilityIdentifier("auth.verification.login")
                }
                .multilineTextAlignment(.center)
                .padding(VizitSpace.xl)
                .padding(.top, VizitSpace.xl)
                .frame(maxWidth: 540).frame(maxWidth: .infinity)
            }
        }
    }
}
'''
p.write_text(s)
# Android explicit-login latch: a signup token cannot enter onboarding, even on restart.
path=A+'auth/AuthViewModel.kt'
change(path,'    var registrationConfirmationInProgress by mutableStateOf(false)','''    private val confirmationPrefs = application.getSharedPreferences("vizit-auth-flow", 0)
    var requiresExplicitLogin by mutableStateOf(confirmationPrefs.getBoolean("signup-login", false))
        private set
    private fun requireExplicitLogin(value: Boolean) {
        confirmationPrefs.edit().putBoolean("signup-login", value).apply()
        requiresExplicitLogin = value
    }
    var registrationConfirmationInProgress by mutableStateOf(false)''')
change(path,'            repositoryOrThrow().login(email, password)','''            repositoryOrThrow().login(email, password)
            requireExplicitLogin(false)''')
change(path,'            adapter.signIn(activity)','''            adapter.signIn(activity)
            requireExplicitLogin(false)''')
change(path,'            passwordRecovery = false','''            passwordRecovery = false
            requireExplicitLogin(false)''')
change(path,'        registrationConfirmationInProgress = callback == AuthCallback.SignupConfirmation','''        registrationConfirmationInProgress = callback == AuthCallback.SignupConfirmation
        if (registrationConfirmationInProgress) requireExplicitLogin(true)''')
change(path,'            repositoryOrThrow().register(','            requireExplicitLogin(true)\n            repositoryOrThrow().register(')
path=A+'ui/VizitRoot.kt'
change(path,'    LaunchedEffect(currentSession, authViewModel.debugLocalProfile) {','''    LaunchedEffect(currentSession, authViewModel.debugLocalProfile,
        authViewModel.requiresExplicitLogin, authViewModel.registrationConfirmationInProgress) {''')
change(path,'''        when {
            authViewModel.debugLocalProfile''','''        when {
            authViewModel.requiresExplicitLogin || authViewModel.registrationConfirmationInProgress ->
                vizitViewModel.clearProfileOwnerBinding()
            authViewModel.debugLocalProfile''')
change(path,'    when (currentSession) {','''    if (authViewModel.requiresExplicitLogin) {
        AuthScreen(viewModel = authViewModel)
        return
    }

    when (currentSession) {''')
path=A+'ui/screens/AuthScreen.kt'
change(path,'    val action = viewModel.actionState','''    var verificationEmail by rememberSaveable { mutableStateOf("") }
    val action = viewModel.actionState''')
change(path,'''            AuthOperation.REGISTER -> {
                password = ""
                confirmation = ""
                mode = AuthScreenMode.EMAIL_VERIFICATION_SENT
            }''','''            AuthOperation.REGISTER -> {
                verificationEmail = email.trim()
                name = ""; email = ""; password = ""; confirmation = ""; legalAccepted = false
                mode = AuthScreenMode.EMAIL_VERIFICATION_SENT
            }
            AuthOperation.EMAIL_CONFIRMATION -> {
                name = ""; email = ""; password = ""; confirmation = ""; legalAccepted = false
                verificationEmail = ""
                mode = AuthScreenMode.LOGIN
            }''')
change(path,'text = if (email.isNotBlank()) email else "Ellenőrizd a postafiókodat.",','''text = if (mode == AuthScreenMode.EMAIL_VERIFICATION_SENT) verificationEmail
                        else email.ifBlank { "Ellenőrizd a postafiókodat." },''')
# Passwords must not be serialized into Android saved-instance state.
p=ROOT/path; s=p.read_text()
for name in ['password','confirmation']: s=s.replace('var '+name+' by rememberSaveable','var '+name+' by remember')
if 'import androidx.compose.runtime.remember\n' not in s:
    s=s.replace('import androidx.compose.runtime.Composable','import androidx.compose.runtime.remember\nimport androidx.compose.runtime.Composable')
p.write_text(s)
# Independent versions. No signing, backend, NFC, deletion, or RLS changes.
change('config/android-version.properties','versionName=10.0.2','versionName=10.0.3')
change('config/android-version.properties','versionCode=10000007','versionCode=10000008')
change('app/build.gradle.kts','versionName = "10.0.2"','versionName = "10.0.3"')
change('app/build.gradle.kts','versionCode = 10000007','versionCode = 10000008')
change('config/ios-version.properties','marketingVersion=10.0.0','marketingVersion=10.0.1')
change('ios/VIZIT.xcodeproj/project.pbxproj','MARKETING_VERSION = "10.0.0"','MARKETING_VERSION = "10.0.1"',count=2)
for path in ['docs/RELEASE.md','ios/README.md','ios/UITests/VizitUITests.swift']:
    p=ROOT/path; s=p.read_text().replace('10.0.0','10.0.1')
    if path=='docs/RELEASE.md': s=s.replace('10.0.2','10.0.3').replace('10000007','10000008')
    p.write_text(s)
put('app/src/test/java/hu/rayworks/vizit/v10/WizardParityTest.kt','''package hu.rayworks.vizit.v10
import hu.rayworks.vizit.v10.ui.wizard.*
import org.junit.Assert.*
import org.junit.Test
class WizardParityTest {
    @Test fun privateFlowUsesThreeRealBlocksAndNoDemoIdentity() {
        val w = WizardState({ emptyList() })
        assertEquals("", w.name)
        w.chooseType(ProfileType.Private); w.begin()
        assertEquals(listOf("personal", "online", "done"), w.flow.map { it.id })
        assertEquals(listOf("name", "phone"), w.fieldKeys("personal"))
        assertFalse(w.valid("personal"))
        w.input("name", "Teszt Elek"); w.next("personal", false)
        assertEquals(BlockStatus.Active, w.status("online"))
        assertEquals("Kihagyom", w.actionLabel(w.flow[1]))
        w.next("online", false)
        assertEquals(BlockStatus.Skip, w.status("online"))
        assertTrue(w.valid("done")); assertNotNull(w.buildProfile())
        w.reopen("online"); assertEquals(BlockStatus.Open, w.status("online"))
        assertEquals("Teszt Elek", w.name)
    }
    @Test fun businessFlowKeepsCompanyFieldsInTheirOwnBlock() {
        val w = WizardState({ emptyList() }); w.chooseType(ProfileType.Business); w.begin()
        assertEquals(listOf("personal", "company", "online", "done"), w.flow.map { it.id })
        assertEquals(listOf("company", "role", "place", "bio"), w.fieldKeys("company"))
        assertFalse(w.valid("company")); w.input("company", "Teszt Kft."); assertTrue(w.valid("company"))
        w.input("email", "hibas"); assertFalse(w.valid("online"))
        w.input("email", "test@example.com"); assertTrue(w.valid("online"))
    }
}
''')
p=ROOT/'ios/NativeTests/NativeIntegrationTests.swift'; s=p.read_text()
s+='''
final class ZIPWizardParityTests: XCTestCase {
    @MainActor func testPrivateWizardHasThreeBlocksAndNoDemoName() {
        let w = ZIPWizardState(takenSlugs: { [] })
        XCTAssertTrue(w.name.isEmpty)
        w.chooseType(.individual); w.begin()
        XCTAssertEqual(w.flow.map(\\.id), ["personal", "online", "done"])
        XCTAssertFalse(w.valid("personal"))
        w.input("name", "Teszt Elek")
        XCTAssertTrue(w.valid("personal"))
    }
    @MainActor func testBusinessWizardKeepsFourBlocks() {
        let w = ZIPWizardState(takenSlugs: { [] })
        w.chooseType(.business); w.begin()
        XCTAssertEqual(w.flow.map(\\.id), ["personal", "company", "online", "done"])
        XCTAssertFalse(w.valid("company"))
    }
}
'''
p.write_text(s)
p=ROOT/'ios/UITests/VizitUITests.swift'; s=p.read_text()
start=s.index('    func testCreatePersistAndShowQR()'); s=s[:start]+'''
    func testRegistrationSuccessDoesNotKeepFormFields() {
        let app = XCUIApplication()
        app.launchArguments = ["--ui-test-verification"]
        app.launch()
        XCTAssertTrue(app.buttons["auth.verification.login"].waitForExistence(timeout: 10))
        XCTAssertFalse(app.textFields["auth.name"].exists)
        XCTAssertFalse(app.secureTextFields["auth.password"].exists)
        XCTAssertFalse(app.secureTextFields["auth.confirmation"].exists)
        app.buttons["auth.verification.login"].tap()
        XCTAssertTrue(app.buttons["auth.submit"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["Üdv újra!"].exists)
        XCTAssertEqual(app.textFields["auth.email"].value as? String, "nev@pelda.hu")
    }

    func testCreatePersistAndShowQR() {
        let app = launchClean()
        XCTAssertTrue(app.buttons["wizard.private"].waitForExistence(timeout: 10))
        app.buttons["wizard.private"].tap()
        let name = app.textFields["wizard.fullName"]
        XCTAssertTrue(name.waitForExistence(timeout: 5))
        name.tap(); name.typeText("Teszt Elek")
        app.buttons["wizard.action.personal"].firstMatch.tap()
        let online = app.buttons["wizard.action.online"].firstMatch
        XCTAssertTrue(online.waitForExistence(timeout: 5)); online.tap()
        let publish = app.buttons["wizard.action.done"].firstMatch
        XCTAssertTrue(publish.waitForExistence(timeout: 5)); publish.tap()
        XCTAssertTrue(app.staticTexts["card.name"].waitForExistence(timeout: 5))
        XCTAssertEqual(app.staticTexts["card.name"].label, "Teszt Elek")
        app.terminate(); app.launchArguments = ["--ui-testing"]; app.launch()
        XCTAssertTrue(app.staticTexts["card.name"].waitForExistence(timeout: 5))
        XCTAssertEqual(app.staticTexts["card.name"].label, "Teszt Elek")
        app.buttons["Megosztás"].tap()
        XCTAssertTrue(app.images["share.qr"].waitForExistence(timeout: 5))
        let shot = XCTAttachment(screenshot: app.screenshot())
        shot.name = "ZIP-wizard-created-contact-QR"; shot.lifetime = .keepAlways; add(shot)
    }

    func testEmptyProfileShowsValidation() {
        let app = launchClean()
        XCTAssertTrue(app.buttons["wizard.private"].waitForExistence(timeout: 10))
        app.buttons["wizard.private"].tap()
        let next = app.buttons["wizard.action.personal"].firstMatch
        XCTAssertTrue(next.waitForExistence(timeout: 5))
        XCTAssertFalse(next.isEnabled)
        XCTAssertTrue(app.textFields["wizard.field.phone"].exists)
        XCTAssertFalse(app.textFields["wizard.field.company"].exists)
        let shot = XCTAttachment(screenshot: app.screenshot())
        shot.name = "ZIP-wizard-personal-block"; shot.lifetime = .keepAlways; add(shot)
    }

    func testBackKeepsDraftWithoutPublishing() {
        let app = launchClean()
        XCTAssertTrue(app.buttons["wizard.private"].waitForExistence(timeout: 10))
        app.buttons["wizard.private"].tap()
        let name = app.textFields["wizard.fullName"]
        XCTAssertTrue(name.waitForExistence(timeout: 5))
        name.tap(); name.typeText("Nem mentett adat")
        app.buttons["Bezárás"].tap()
        XCTAssertTrue(app.buttons["Maradok"].waitForExistence(timeout: 5))
        app.buttons["Maradok"].tap()
        XCTAssertEqual(name.value as? String, "Nem mentett adat")
        app.terminate(); app.launchArguments = ["--ui-testing"]; app.launch()
        XCTAssertTrue(app.buttons["wizard.private"].waitForExistence(timeout: 5))
        XCTAssertFalse(app.staticTexts["card.name"].exists)
    }
}
'''
p.write_text(s)
put('docs/ZIP-WIZARD-PARITY.md','''# Native ZIP source parity — 2026-10-03

Authoritative sources are the user-uploaded native archives, not the earlier HTML.
- VizitTeljesIOS(1).zip: SHA-256 af2b2c86c1792b39c57815209538cb7823a10a943d32cd11ce6f321e224f5288
- VizitTeljes(4).zip: SHA-256 de0bb755e8e5e8bf6277af564a150d1416bcbb9a16a86aa685c6faa2ca010819

## Confirmed discrepancy
The production app copied the navy intro and grouped progress but rendered the old
individual-step form and large card preview underneath. The archives instead provide
horizontal, independently scrolling colored data blocks with their own actions.

## Repaired scope
- Original personal/company/online/completion blocks, fields, images, transitions,
  optional-step behavior, keyboard action and exit confirmation on both platforms.
- Private flow: 3 blocks. Business flow: 4 blocks.
- Registration completion becomes a separate route; signup confirmation requires an
  explicit login. A persisted latch prevents a temporary signup session from opening
  the wizard on app restart.

## Deliberate production adapters
- Native UI namespaced; no prototype account/users/auth/cloud/timer-based publication.
- Real image selection only; no sample avatar/logo actions.
- Existing owner-isolated storage, sync, NFC, deletion and backend retained.
- Client slug format is not presented as authoritative global availability.
- iOS 17+ original scroll pager; iOS 16 fallback uses identical block contents.
- Local-first save returns to the real sync status, not a fake publication success.

## Evidence and limitations
Source parse alone is not an iOS build or a device E2E. Standard Android/iOS CI must
pass before merge/release. Native UI tests retain screenshot evidence. Physical email
callback, NFC and end-to-end cloud behavior still require device verification.
This change does not claim every Home/Settings/Analytics screen matches the archives.
''')
subprocess.run(['python3','scripts/sync-version.py','--check-android'],check=True)
subprocess.run(['python3','scripts/sync-version.py','--check-ios'],check=True)
print('Native source import complete; ordinary CI must verify compilation and behavior.')
