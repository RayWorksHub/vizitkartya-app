package hu.rayworks.vizit.v10.data
import androidx.annotation.DrawableRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import hu.rayworks.vizit.v10.ui.wizard.WizardState
import hu.rayworks.vizit.BuildConfig

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
    val cloudSynced: Boolean = true,
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
