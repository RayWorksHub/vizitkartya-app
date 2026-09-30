package hu.rayworks.vizit.v10.data

import androidx.annotation.DrawableRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import android.net.Uri
import hu.rayworks.vizit.BuildConfig
import hu.rayworks.vizit.NfcSharePhase
import hu.rayworks.vizit.R

/** Melyik elrendezés fut: B = egy képernyő (alapértelmezett), A = három fül. */
enum class DesignLayout { A, B }

object DesignConfig {
    /** Itt lehet átváltani. Futás közben: hosszan nyomd a VIZIT feliratot → Prototípus lap. */
    val DEFAULT_LAYOUT = DesignLayout.B
}

enum class HomeTab { Share, Profile, More }

enum class SheetKind { Menu, Profiles, Edit, Share, Prototype }

/** Kép: beépített (Mostani kép / Minta logó) vagy a galériából választott. */
sealed interface Pic {
    data class Res(@DrawableRes val id: Int) : Pic
    data class Bmp(val bitmap: ImageBitmap) : Pic
}

enum class CardLayout(val label: String) { Portrait("Álló"), Landscape("Fekvő") }

enum class QrMode(val label: String) { Profile("Profil"), Contact("Kontakt"), Photo("Fényképes") }

/** A profil szinkron állapota (éles: LOCAL_ONLY, SYNCED, PENDING, SYNCING, RETRY_SCHEDULED, CONFLICT). */
enum class SyncStatus(val label: String) {
    Synced("Rendben"), Pending("Függőben"), Syncing("Folyamatban"), Retry("Újra"), Conflict("Ütközés"), LocalOnly("Helyi"),
}

/** A telefon NFC-képessége. */
enum class NfcDevice(val label: String) { Ready("Kész"), SystemOff("Kikapcsolva"), Unsupported("Nincs NFC") }

/** Kártyaszínvilágok (éles: 6 séma, a varázsló az első 4-et kínálja). */
data class Preset(val id: String, val name: String, val c1: Color, val c2: Color, val c3: Color, val acc: Color)

val PRESETS = listOf(
    Preset("tinta", "Tinta", Color(0xFF0C2C63), Color(0xFF071F4C), Color(0xFF05163A), Color(0xFF0FBEE6)),
    Preset("markakek", "Márkakék", Color(0xFF1668F0), Color(0xFF0B5CE8), Color(0xFF0742A8), Color(0xFF7FD9FF)),
    Preset("smaragd", "Smaragd", Color(0xFF0E6B4A), Color(0xFF0A5138), Color(0xFF063526), Color(0xFF4FE0A8)),
    Preset("ametiszt", "Ametiszt", Color(0xFF5B2E9E), Color(0xFF452278), Color(0xFF2C1550), Color(0xFFC9A6FF)),
    Preset("rez", "Réz", Color(0xFF9A4A18), Color(0xFF7A3A12), Color(0xFF50250B), Color(0xFFFFBE7A)),
    Preset("grafit", "Grafit", Color(0xFF2B3240), Color(0xFF1D222D), Color(0xFF12161E), Color(0xFF8FD8F0)),
)

fun preset(id: String): Preset = PRESETS.firstOrNull { it.id == id } ?: PRESETS.first()

data class Social(val id: String, val label: String, val placeholder: String)

val SOCIALS = listOf(
    Social("linkedin", "LinkedIn", "linkedin.com/in/…"),
    Social("facebook", "Facebook", "facebook.com/…"),
    Social("instagram", "Instagram", "instagram.com/…"),
    Social("tiktok", "TikTok", "tiktok.com/@…"),
    Social("youtube", "YouTube", "youtube.com/@…"),
    Social("x", "X", "x.com/…"),
    Social("github", "GitHub", "github.com/…"),
    Social("other", "Egyéb", "https://…"),
)

/** Az Adatláthatóság hat kapcsolója (a teljes név mindig látszik). */
val VIS_FIELDS = listOf(
    "company" to "Cég és beosztás",
    "email" to "E-mail-cím",
    "phone" to "Telefonszám",
    "web" to "Weboldal",
    "social" to "Közösségi profilok",
    "address" to "Lakcím",
)

data class Profile(
    val id: String,
    val label: String,
    val real: Boolean,
    val name: String,
    val title: String = "",
    val company: String = "",
    val bio: String = "",
    val phone: String = "",
    val email: String = "",
    val web: String = "",
    val address: String = "",
    val socials: Map<String, String> = emptyMap(),
    val photo: Pic? = null,
    val logo: Pic? = null,
    val isPublic: Boolean = false,
    val slug: String = "",
    val customDomain: String = "",
    val domainVerified: Boolean = false,
    val presetId: String = "tinta",
    val layout: CardLayout = CardLayout.Portrait,
    val showPhoto: Boolean = true,
    val showQr: Boolean = false,
    val showSocial: Boolean = false,
    val vis: Map<String, Boolean> = VIS_FIELDS.associate { it.first to true },
    val tag: String? = null,
    val fresh: Boolean = false,
) {
    val preset: Preset get() = preset(presetId)
    /** A címke pöttyének színe = a kártya színvilága. */
    val color: Color get() = preset.c1
    val short: String get() = if (slug.isNotEmpty()) "vizitkartyam.hu/$slug" else "vizitkartyam.hu/…"
    val url: String get() = "${BuildConfig.PUBLIC_PROFILE_BASE_URL.trimEnd('/')}/$slug"

    fun hasValue(key: String): Boolean = when (key) {
        "company" -> company.isNotBlank() || title.isNotBlank()
        "email" -> email.isNotBlank()
        "phone" -> phone.isNotBlank()
        "web" -> web.isNotBlank()
        "social" -> socials.values.any { it.isNotBlank() }
        "address" -> address.isNotBlank()
        else -> false
    }

    /** Megosztáskor is látszik-e a mező (ki van töltve és nincs elrejtve). */
    fun shows(key: String): Boolean = hasValue(key) && vis[key] != false

    val visibleCount: Int get() = VIS_FIELDS.count { vis[it.first] != false }

    val initials: String
        get() {
            val w = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            return if (w.isEmpty()) "V" else w.take(2).joinToString("") { it.take(1) }.uppercase()
        }
}

const val ACCOUNT_NAME = "Csukárdi Rajmund"
const val ACCOUNT_EMAIL = "info@rayworks.hu"
const val PROFILE_URL = "https://www.vizitkartyam.hu/rayworks-solutions"
val APP_VERSION: String get() = "VIZIT ${BuildConfig.VERSION_NAME}"
const val PRIVACY_URL = "https://www.vizitkartyam.hu/adatvedelem"
const val TERMS_URL = "https://www.vizitkartyam.hu/felhasznalasi-feltetelek"
const val CRM_URL = "https://www.vizitkartyam.hu/auth/sign-in?next=%2Fdashboard%2Fcrm"
const val EDITOR_URL = "https://www.vizitkartyam.hu/auth/sign-in?next=%2Fdashboard%2Fprofile"

/** Az első profil valódi (az élő oldal), a másik kettő minta. */
val SAMPLE_PROFILES = listOf(
    Profile(
        id = "rw", label = "RayWorks", real = true,
        name = ACCOUNT_NAME, title = "INFORMATIKUS - TANÁR", company = "RayWorks | IT Solutions",
        bio = "Informatikai megoldások kis és középvállalkozásoknak.",
        phone = "+36 70 298 0003", email = ACCOUNT_EMAIL, web = "https://www.rayworks.hu", address = "Budapest",
        photo = Pic.Res(R.drawable.avatar),
        isPublic = true, slug = "rayworks-solutions", presetId = "markakek", showQr = true,
    ),
    Profile(
        id = "ed", label = "Oktatás", real = false,
        name = ACCOUNT_NAME, title = "Informatikatanár",
        phone = "+36 70 298 0003", email = ACCOUNT_EMAIL, address = "Budapest",
        presetId = "smaragd", tag = "Minta",
    ),
    Profile(
        id = "pr", label = "Magán", real = false,
        name = ACCOUNT_NAME, phone = "+36 70 298 0003", email = ACCOUNT_EMAIL,
        presetId = "rez", tag = "Minta",
    ),
)

data class ToastMsg(val text: String, val id: Long)

/** Kérés a profil-lapozónak: ugorjon erre az oldalra (a nonce miatt ugyanarra is újra lefut). */
data class GoTo(val index: Int, val animate: Boolean, val nonce: Long = System.nanoTime())

/** Teljes képernyős oldalak (jobbról beúszó verem). */
sealed interface Page {
    data object Settings : Page
    data object Appearance : Page
    data object Analytics : Page
    data class QrFull(val index: Int) : Page
    data object NfcSend : Page
    data object Scanner : Page
    data object Hub : Page
    data object HubVosz : Page
    data object HubEdu : Page
    data class HubCourse(val id: String) : Page
    data object HubHelp : Page
    data object HubToolkit : Page
}

val Page.fades: Boolean get() = this is Page.QrFull || this == Page.NfcSend || this == Page.Scanner

enum class AuthMode { Login, Register, EmailSent, Forgot, ResetSent, NewPassword, Legal }

/** Mindent eltakaró belépési és hibaállapotok. null = a bejelentkezett app látszik. */
sealed interface Gate {
    data class Auth(val mode: AuthMode) : Gate
    data object Loading : Gate
    data object ProfileError : Gate
    data object SessionExpired : Gate
    data object LegalFailed : Gate
}

/** Megerősítő párbeszédablak. requireText: ezt kell beírni a megerősítéshez (pl. TÖRLÉS). */
data class DialogSpec(
    val title: String,
    val body: String? = null,
    val confirm: String,
    val dismiss: String? = "Mégse",
    val danger: Boolean = false,
    val icon: ImageVector? = null,
    val requireText: String? = null,
    val onConfirm: () -> Unit,
)

/** Az app teljes (demó) állapota. Minden változás azonnal újrarajzolja a felületet. */
class RuntimeBindings(
    val onProfileSelected: (String) -> Unit = {},
    val onSaveProfile: (Profile) -> Unit = {},
    val onBeginAdditionalProfile: () -> String? = { null },
    val onCancelAdditionalProfile: () -> Unit = {},
    val onCreateAdditionalProfile: (Profile, Boolean) -> Unit = { _, _ -> },
    val onDeleteActiveProfile: () -> Unit = {},
    val onPresentationChanged: (Profile) -> Unit = {},
    val onAutomaticSyncChanged: (Boolean) -> Unit = {},
    val onRetrySync: () -> Unit = {},
    val onResolveConflict: (Boolean) -> Unit = {},
    val onStartNfcShare: () -> String? = { null },
    val onStopNfcShare: () -> Unit = {},
    val onOpenNfcSettings: () -> Unit = {},
    val onThemeChanged: (hu.rayworks.vizit.v10.ui.theme.ThemeMode) -> Unit = {},
    val onExportAccount: (Uri) -> Unit = {},
    val onRetryProfileLoad: () -> Unit = {},
    val onRetryLegalAcceptance: () -> Unit = {},
    val onUseDebugLocalProfile: () -> Unit = {},
    val onLogout: () -> Unit = {},
    val onDeleteAccount: () -> Unit = {},
)

/**
 * A ZIP felületének állapota. Production módban a képernyőállapotot a valódi
 * ViewModelek töltik, a műveleteket pedig a RuntimeBindings továbbítja.
 */
class AppState(
    initialProfiles: List<Profile> = SAMPLE_PROFILES,
    val productionMode: Boolean = false,
) {
    val profiles = mutableStateListOf<Profile>().apply { addAll(initialProfiles) }

    var runtime: RuntimeBindings? = null

    private var selectedIndex by mutableIntStateOf(0)

    /** A „Profil” fül ezt mutatja, ha éppen az „Új profil” kártya van kiválasztva. */
    var lastProfileIndex by mutableIntStateOf(0)
        private set

    /** A kiválasztott kártya indexe. == profiles.size → az „Új profil” kártya. */
    var selected: Int
        get() = selectedIndex
        set(value) {
            val safeValue = value.coerceIn(0, profiles.size)
            selectedIndex = safeValue
            if (safeValue < profiles.size) {
                lastProfileIndex = safeValue
                if (!synchronizingProfiles) runtime?.onProfileSelected?.invoke(profiles[safeValue].id)
            }
        }

    /** A kiválasztott profil, vagy null, ha az „Új profil” kártyán állunk. */
    val current: Profile? get() = profiles.getOrNull(selected)

    /** Amit a szerkesztő, a megjelenés és a statisztika mutat: a kiválasztott, vagy az utoljára kiválasztott profil. */
    val focus: Profile get() = current ?: profiles[lastProfileIndex.coerceIn(0, profiles.lastIndex)]
    val focusIndex: Int get() = if (selected < profiles.size) selected else lastProfileIndex.coerceIn(0, profiles.lastIndex)

    private var synchronizingProfiles = false

    var goToRequest by mutableStateOf<GoTo?>(null)
        private set

    var layout by mutableStateOf(DesignConfig.DEFAULT_LAYOUT)
    var tab by mutableStateOf(HomeTab.Share)
    var nfcOn by mutableStateOf(true)
    var nfcDevice by mutableStateOf(NfcDevice.Ready)
    var nfcSharePhase by mutableStateOf(NfcSharePhase.IDLE)
    var sheet by mutableStateOf<SheetKind?>(null)
    var editSection by mutableStateOf<String?>(null)
    var toast by mutableStateOf<ToastMsg?>(null)

    var wizard by mutableStateOf<hu.rayworks.vizit.v10.ui.wizard.WizardState?>(null)
    var wizardOpen by mutableStateOf(false)
    private var initialProfileWizard = false

    // Éles funkciók demó állapota
    var offline by mutableStateOf(false)
    var sync by mutableStateOf(SyncStatus.Synced)
    var autoSync by mutableStateOf(true)
    var cameraAllowed by mutableStateOf(true)
    var legalRequired by mutableStateOf(false)
    var qrMode by mutableStateOf(QrMode.Profile)
    var multiProfileEnabled by mutableStateOf(true)
    var businessPortalEnabled by mutableStateOf(true)
    var analyticsEnabled by mutableStateOf(true)
    var qrScannerEnabled by mutableStateOf(true)

    val pages = mutableStateListOf<Page>()
    var navForward by mutableStateOf(true)
        private set
    var gate by mutableStateOf<Gate?>(null)
    var authBanner by mutableStateOf<Pair<String, Boolean>?>(null) // szöveg, siker?
    var dialog by mutableStateOf<DialogSpec?>(null)

    /** Kurzus leckék („ai:1.1”) és eszköztár útmutatók teljesítése. */
    val lessonsDone = mutableStateMapOf("ai:1.1" to true, "ai:1.2" to true, "security:1.1" to true)
    val guidesDone = mutableStateMapOf<String, Boolean>()

    fun toast(text: String) {
        toast = ToastMsg(text, System.nanoTime())
    }

    /* ------------------------------------------------------------ navigáció */

    fun push(page: Page) {
        if (productionMode) {
            val unavailable = when (page) {
                Page.Analytics -> !analyticsEnabled
                Page.Scanner -> !qrScannerEnabled
                Page.Hub, Page.HubVosz, Page.HubEdu, Page.HubHelp, Page.HubToolkit,
                is Page.HubCourse -> !businessPortalEnabled
                else -> false
            }
            if (unavailable) {
                toast("Ez a funkció jelenleg nem érhető el.")
                return
            }
        }
        if (page == Page.NfcSend) {
            val issue = runtime?.onStartNfcShare?.invoke()
            if (issue != null) {
                toast(issue)
                return
            }
        }
        sheet = null
        navForward = true
        pages.add(page)
    }

    fun pop() {
        if (pages.isEmpty()) return
        if (pages.last() == Page.NfcSend) runtime?.onStopNfcShare?.invoke()
        navForward = false
        pages.removeAt(pages.lastIndex)
    }

    /** goTo(i) a prototípusból: a lapozó odagörget, a kiválasztás azonnal átáll. */
    fun goTo(index: Int, animate: Boolean = true) {
        selected = index
        goToRequest = GoTo(index, animate)
    }

    /** Kattintás a „Profiljaid” lapon. Az A elrendezésben, ha nem a Megosztás fülön vagyunk, csak kiválaszt. */
    fun pickFromSheet(index: Int) {
        sheet = null
        if (layout == DesignLayout.A && tab != HomeTab.Share) {
            selected = index
            toast(if (index < profiles.size) profiles[index].label + " profil kiválasztva" else "Új profil")
        } else {
            goTo(index)
        }
    }

    /** Automatikus szinkron: bekapcsoláskor a függő vagy megszakadt szinkron azonnal indul. */
    fun changeAutoSync(on: Boolean) {
        autoSync = on
        runtime?.onAutomaticSyncChanged?.invoke(on)
        if (!productionMode && on && !offline && (sync == SyncStatus.Retry || sync == SyncStatus.Pending)) {
            sync = SyncStatus.Syncing
        }
    }

    fun setNfc(on: Boolean) {
        if (productionMode) {
            runtime?.onOpenNfcSettings?.invoke()
            return
        }
        if (on && nfcDevice == NfcDevice.SystemOff) {
            runtime?.onOpenNfcSettings?.invoke()
            return
        }
        nfcOn = on
        toast(if (on) "NFC bekapcsolva" else "NFC kikapcsolva")
    }

    fun openEdit(section: String?) {
        editSection = section
        sheet = SheetKind.Edit
    }

    /** A szerkesztő mindig a fókuszban lévő profilt írja. */
    fun updateFocus(change: (Profile) -> Profile) {
        val i = focusIndex
        val before = profiles[i]
        val after = change(before)
        profiles[i] = after
        if (
            before.presetId != after.presetId || before.layout != after.layout ||
            before.showPhoto != after.showPhoto || before.showQr != after.showQr ||
            before.showSocial != after.showSocial || before.vis != after.vis
        ) {
            runtime?.onPresentationChanged?.invoke(after)
        }
    }

    fun saveFocus() {
        runtime?.onSaveProfile?.invoke(focus)
        if (!productionMode) {
            sheet = null
            if (offline) sync = SyncStatus.Pending else if (autoSync) sync = SyncStatus.Syncing
            toast("A névjegy mentve.")
        }
    }

    fun retrySync() {
        runtime?.onRetrySync?.invoke()
        if (!productionMode) sync = SyncStatus.Syncing
    }

    fun resolveConflict(keepLocal: Boolean) {
        runtime?.onResolveConflict?.invoke(keepLocal)
        if (!productionMode) sync = SyncStatus.Syncing
    }

    fun changeTheme(mode: hu.rayworks.vizit.v10.ui.theme.ThemeMode) {
        hu.rayworks.vizit.v10.ui.theme.ThemeState.mode = mode
        runtime?.onThemeChanged?.invoke(mode)
    }

    fun exportAccount(uri: Uri) {
        runtime?.onExportAccount?.invoke(uri)
    }

    fun openFullQr(index: Int) {
        if (index != selected) goTo(index)
        qrMode = if (profileQrAvailable(profiles[index])) QrMode.Profile else QrMode.Contact
        push(Page.QrFull(index))
    }

    fun switchLayout(to: DesignLayout) {
        if (layout == to) return
        layout = to
        tab = HomeTab.Share
    }

    /* ------------------------------------------------------------ QR tartalmak */

    val synced: Boolean get() = sync == SyncStatus.Synced

    /** Profil QR: nyilvános profil, profilcím és sikeres szinkron kell hozzá. */
    fun profileQrAvailable(p: Profile): Boolean = p.isPublic && p.slug.length >= 3 && synced && (p.real || p.fresh)

    fun publicUrl(p: Profile): String =
        if (p.domainVerified && p.customDomain.isNotBlank()) "https://" + p.customDomain.trim() else p.url

    /** A kártyán látszó QR: a profil linkje, ha elérhető; különben a Kontakt QR. */
    fun cardQr(p: Profile): String = if (profileQrAvailable(p)) publicUrl(p) else contactVCard(p) ?: p.short

    /** Kontakt QR: vCard 3.0 kép nélkül, csak a látható mezőkkel, legfeljebb 2200 bájt. */
    fun contactVCard(p: Profile): String? {
        if (p.name.isBlank()) return null
        if (!p.shows("phone") && !p.shows("email")) return null
        fun esc(s: String) = s.trim().replace("\\", "\\\\").replace(",", "\\,").replace(";", "\\;").replace("\n", "\\n")
        val words = p.name.trim().split(Regex("\\s+"))
        val family = words.first()
        val given = words.drop(1).joinToString(" ")
        val b = StringBuilder("BEGIN:VCARD\r\nVERSION:3.0\r\n")
        b.append("N:").append(esc(family)).append(';').append(esc(given)).append(";;;\r\n")
        b.append("FN:").append(esc(p.name)).append("\r\n")
        if (p.shows("company")) {
            if (p.company.isNotBlank()) b.append("ORG:").append(esc(p.company)).append("\r\n")
            if (p.title.isNotBlank()) b.append("TITLE:").append(esc(p.title)).append("\r\n")
        }
        if (p.shows("phone")) b.append("TEL;TYPE=CELL:").append(esc(p.phone)).append("\r\n")
        if (p.shows("email")) b.append("EMAIL;TYPE=INTERNET:").append(esc(p.email)).append("\r\n")
        if (p.shows("web")) b.append("URL:").append(esc(p.web)).append("\r\n")
        if (p.shows("address")) b.append("ADR;TYPE=WORK:;;").append(esc(p.address)).append(";;;;\r\n")
        if (p.shows("social")) SOCIALS.forEach { s ->
            val v = p.socials[s.id]
            if (!v.isNullOrBlank()) b.append("URL;TYPE=").append(s.label.uppercase()).append(':').append(esc(v)).append("\r\n")
        }
        if (profileQrAvailable(p)) b.append("URL:").append(publicUrl(p)).append("\r\n")
        b.append("END:VCARD")
        val out = b.toString()
        return if (out.toByteArray(Charsets.UTF_8).size <= 2200) out else null
    }

    /** Szöveges megosztás (éles: név, beosztás, cég, telefon, e-mail, web, közösségi linkek). */
    fun shareText(p: Profile): String = buildList {
        add(p.name)
        if (p.shows("company")) {
            if (p.title.isNotBlank()) add(p.title)
            if (p.company.isNotBlank()) add(p.company)
        }
        if (p.shows("phone")) add(p.phone)
        if (p.shows("email")) add(p.email)
        if (p.shows("web")) add(p.web)
        if (p.shows("social")) SOCIALS.forEach { s -> p.socials[s.id]?.takeIf { it.isNotBlank() }?.let { add(s.label + ": " + it) } }
        if (profileQrAvailable(p)) add(publicUrl(p))
    }.joinToString("\n")

    /* ------------------------------------------------------------ profilok */

    /** Új profil csak online, szinkronizált fiókhoz (éles üzenetekkel). */
    fun openWizard() {
        sheet = null
        if (!multiProfileEnabled) {
            toast("A többprofilos funkció most nem érhető el.")
            return
        }
        runtime?.onBeginAdditionalProfile?.invoke()?.let {
            toast(it)
            return
        }
        when {
            offline -> {
                toast("Új profilt csak bejelentkezett, online fiókhoz lehet létrehozni.")
                return
            }
            sync == SyncStatus.Pending || sync == SyncStatus.Syncing || sync == SyncStatus.Conflict -> {
                toast("Előbb várd meg a jelenlegi profil szinkronizálását.")
                return
            }
            else -> Unit
        }
        initialProfileWizard = false
        wizard = hu.rayworks.vizit.v10.ui.wizard.WizardState(
            takenSlugs = { profiles.map { it.slug }.filter { it.isNotEmpty() } },
            initialName = focus.name,
        )
        wizardOpen = true
    }

    /** Első, még üres fiókprofil létrehozása ugyanazzal a teljes ZIP-varázslóval. */
    fun openInitialProfileWizard() {
        if (wizardOpen) return
        initialProfileWizard = true
        wizard = hu.rayworks.vizit.v10.ui.wizard.WizardState(
            takenSlugs = { profiles.map { it.slug }.filter(String::isNotEmpty) },
        )
        wizardOpen = true
    }

    /** close(force) a prototípusból: ha van beírt adat és nem kényszerített, előbb megerősítést kér. */
    fun closeWizard(force: Boolean) {
        val w = wizard ?: return
        if (!force && w.dirty) {
            w.confirmOpen = true
            return
        }
        w.confirmOpen = false
        w.introShown = false
        wizardOpen = false
        if (productionMode && !initialProfileWizard) runtime?.onCancelAdditionalProfile?.invoke()
        initialProfileWizard = false
    }

    /** Publikálás után: új kártya a lapozó végére, odagörgetés, üzenet. */
    fun addPublished(profile: Profile, isPublic: Boolean) {
        if (productionMode) {
            if (initialProfileWizard) {
                runtime?.onSaveProfile?.invoke(profile.copy(isPublic = isPublic))
            } else {
                runtime?.onCreateAdditionalProfile?.invoke(profile, isPublic)
            }
            return
        }
        profiles.add(profile)
        val i = profiles.size - 1
        if (layout == DesignLayout.A && tab != HomeTab.Share) {
            selected = i
            tab = HomeTab.Share
        }
        goTo(i)
        toast(if (isPublic) "Új profil publikálva: " + profile.short else "Új profil elmentve")
        closeWizard(force = true)
    }

    fun askDeleteActive() {
        val p = focus
        if (profiles.size < 2) {
            toast("A profil nem törölhető.")
            return
        }
        dialog = DialogSpec(
            title = "Profil törlése?",
            body = "A(z) „${p.label}” profil végleg törlődik.",
            confirm = "Törlés",
            danger = true,
            onConfirm = {
                if (productionMode) {
                    sheet = null
                    runtime?.onDeleteActiveProfile?.invoke()
                } else {
                    val i = profiles.indexOf(p)
                    if (i >= 0) {
                        profiles.removeAt(i)
                        val next = i.coerceAtMost(profiles.lastIndex)
                        sheet = null
                        goTo(next, animate = false)
                        toast("A profil törölve.")
                    }
                }
            },
        )
    }

    /* ------------------------------------------------------------ fiók */

    fun logout() {
        if (productionMode) {
            runtime?.onLogout?.invoke()
            return
        }
        sheet = null
        pages.clear()
        authBanner = "Kijelentkeztél." to true
        gate = Gate.Auth(AuthMode.Login)
    }

    fun signedIn() {
        authBanner = null
        gate = if (legalRequired) Gate.Auth(AuthMode.Legal) else null
    }

    fun askDeleteAccount() {
        dialog = DialogSpec(
            title = "Biztosan törlöd a fiókodat?",
            body = "A művelet végleges. Törlődik a fiók, a felhőprofil, a média és a helyi adatok.",
            confirm = "Végleges törlés",
            dismiss = "Mégsem",
            danger = true,
            requireText = "TÖRLÉS",
            onConfirm = {
                if (productionMode) {
                    runtime?.onDeleteAccount?.invoke()
                } else {
                    pages.clear()
                    sheet = null
                    authBanner = "A fiók és a helyi profil törlése befejeződött." to true
                    gate = Gate.Auth(AuthMode.Login)
                }
            },
        )
    }

    /** Az adatexport tartalma (éles: GET /api/account → vizit-adataim.json). */
    fun exportJson(): String {
        fun q(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
        val items = profiles.joinToString(",\n") { p ->
            "    {\"label\": ${q(p.label)}, \"name\": ${q(p.name)}, \"title\": ${q(p.title)}, \"company\": ${q(p.company)}, " +
                "\"phone\": ${q(p.phone)}, \"email\": ${q(p.email)}, \"website\": ${q(p.web)}, \"address\": ${q(p.address)}, " +
                "\"public\": ${p.isPublic}, \"slug\": ${q(p.slug)}}"
        }
        return "{\n  \"account\": {\"name\": ${q(ACCOUNT_NAME)}, \"email\": ${q(ACCOUNT_EMAIL)}},\n  \"profiles\": [\n$items\n  ],\n" +
            "  \"consents\": {\"privacy\": \"2026-09-22\", \"terms\": \"2026-09-22\"}\n}\n"
    }

    /** A valódi profilkatalógus atomikus betöltése a lapozóba. */
    fun replaceProfiles(values: List<Profile>, activeId: String?) {
        if (values.isEmpty()) return
        synchronizingProfiles = true
        try {
            profiles.clear()
            profiles.addAll(values)
            val active = values.indexOfFirst { it.id == activeId }.takeIf { it >= 0 } ?: 0
            selectedIndex = active
            lastProfileIndex = active
            goToRequest = GoTo(active, animate = false)
        } finally {
            synchronizingProfiles = false
        }
    }

    /** Külső megjelenés-változás csak a kártyamezőket írja felül. */
    fun applyPresentation(source: Profile) {
        if (profiles.isEmpty()) return
        val i = focusIndex
        profiles[i] = profiles[i].copy(
            presetId = source.presetId,
            layout = source.layout,
            showPhoto = source.showPhoto,
            showQr = source.showQr,
            showSocial = source.showSocial,
            vis = source.vis,
        )
    }
}
