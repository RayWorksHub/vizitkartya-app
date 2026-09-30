package hu.rayworks.vizit.v10.ui.wizard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import hu.rayworks.vizit.v10.data.PRESETS
import hu.rayworks.vizit.v10.data.Pic
import hu.rayworks.vizit.v10.data.Profile
import hu.rayworks.vizit.v10.ui.icons.VIcons
import hu.rayworks.vizit.v10.ui.theme.B1
import hu.rayworks.vizit.v10.ui.theme.B2
import hu.rayworks.vizit.v10.ui.theme.B3
import hu.rayworks.vizit.v10.ui.theme.B4
import hu.rayworks.vizit.v10.ui.theme.BlockColor
import java.text.Normalizer

enum class ProfileType { Business, Private }

/** Egy blokk állapota: még nem, most, kész, kihagyva, újranyitva (a CSS st-* osztályai). */
enum class BlockStatus { Todo, Active, Done, Skip, Open }

class WBlock(
    val id: String,
    val title: String,
    val icon: () -> ImageVector,
    val bigIcon: () -> ImageVector,
    val color: BlockColor,
    val items: List<String>,
    val optional: Boolean = false,
)

/** A négy blokk, sorrendben. Magánszemélynél a „Céges adatok” kimarad. */
val BLOCKS = listOf(
    WBlock("personal", "Személyes adatok", { VIcons.person }, { VIcons.personThin }, B1, listOf("Profilkép", "Név", "Telefon")),
    WBlock("company", "Céges adatok", { VIcons.briefcase }, { VIcons.briefcaseThin }, B2, listOf("Logó", "Cégnév", "Beosztás", "Hely", "Bemutatkozás")),
    WBlock("online", "Online elérés", { VIcons.globe }, { VIcons.globeThin }, B3, listOf("E-mail", "Weboldal", "Közösségi profilok"), optional = true),
    WBlock("done", "Befejezés", { VIcons.check }, { VIcons.checkThin }, B4, listOf("Profil címe", "Szín", "Láthatóság")),
)

/** Az adott blokk mezői sorrendben (Enter / „Tovább” a billentyűzeten a következőre ugrik). */
fun WizardState.fieldKeys(blockId: String): List<String> = when (blockId) {
    "personal" -> listOf("name", "phone")
    "company" -> listOf("company", "role", "place", "bio")
    "online" -> listOf("email", "web") + socialOrder.map { "s-$it" }
    "done" -> listOf("slug")
    else -> emptyList()
}

data class SlugState(val ok: Boolean, val msg: String)

/** Görgetési kérés a blokk-lapozónak. */
data class ScrollReq(val blockId: String, val instant: Boolean, val nonce: Long = System.nanoTime())

/**
 * A varázsló teljes állapota és logikája – a prototípus makeWizard() függvényének egy az egyben átirata.
 * A felület (Wizard.kt, WizardBlocks.kt, WizardIntro.kt) csak ezt olvassa és ezen hív.
 */
class WizardState(
    private val takenSlugs: () -> List<String>,
    initialName: String = "",
) {
    var type by mutableStateOf<ProfileType?>(null)
    var started by mutableStateOf(false)
    var dirty = false

    var name by mutableStateOf(initialName)
    var phone by mutableStateOf("")
    var photo by mutableStateOf<Pic?>(null)
    var company by mutableStateOf("")
    var role by mutableStateOf("")
    var place by mutableStateOf("")
    var bio by mutableStateOf("")
    var logo by mutableStateOf<Pic?>(null)
    var email by mutableStateOf("")
    var web by mutableStateOf("")
    val socials = mutableStateMapOf<String, String>()
    val socialOrder = mutableStateListOf<String>()
    var preset by mutableStateOf("tinta")
    var slug by mutableStateOf("")
    var slugEdited = false
    var isPublic by mutableStateOf(true)

    val bs = mutableStateMapOf(
        "personal" to BlockStatus.Todo,
        "company" to BlockStatus.Todo,
        "online" to BlockStatus.Todo,
        "done" to BlockStatus.Todo,
    )

    /** A „Kezdjük meg…” kezdőképernyő látszik-e. */
    var introShown by mutableStateOf(true)
    var confirmOpen by mutableStateOf(false)

    /** Mezőhibák: name, email, photo, logo. */
    val errors = mutableStateMapOf<String, String>()

    /** Melyik mezőben van a kurzor (a jobb felső gomb ebből dönt). */
    var focusedKey by mutableStateOf<String?>(null)
    var focusedBlock by mutableStateOf<String?>(null)

    var scrollRequest by mutableStateOf<ScrollReq?>(null)
    var focusRequest by mutableStateOf<String?>(null)

    val flow: List<WBlock> get() = BLOCKS.filter { type != ProfileType.Private || it.id != "company" }
    fun fidx(id: String) = flow.indexOfFirst { it.id == id }
    val isBiz: Boolean get() = type == ProfileType.Business
    fun status(id: String): BlockStatus = bs[id] ?: BlockStatus.Todo
    val activeBlock: WBlock? get() = flow.firstOrNull { status(it.id) == BlockStatus.Active }

    fun emailOk(v: String) = v.isBlank() || EMAIL.matches(v.trim())
    private fun taken() = listOf("admin", "vizit") + takenSlugs()

    fun initials(n: String = name): String {
        val w = n.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (w.isEmpty()) return ""
        return (w.first().take(1) + (if (w.size > 1) w.last().take(1) else "")).uppercase()
    }

    private fun slugify(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("[\\u0300-\\u036f]"), "")
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(40)

    fun suggested(): String {
        val base = slugify(if (isBiz && company.isNotBlank()) company else name).ifEmpty { "nevjegy" }
        val t = taken()
        if (base !in t) return base
        var n = 2
        while ("$base-$n" in t) n++
        return "$base-$n"
    }

    fun slugState(): SlugState = when {
        slug.length < 3 -> SlugState(false, "Legalább 3 karakter")
        !SLUG.matches(slug) -> SlugState(false, "Kisbetű, szám, kötőjel")
        slug in taken() -> SlugState(false, "Foglalt")
        else -> SlugState(true, "Szabad")
    }

    /** HAS: van-e már valami az opcionális blokkban (ha nincs: „Kihagyom”). */
    fun has(id: String): Boolean = when (id) {
        "online" -> email.isNotBlank() || web.isNotBlank() || socialOrder.any { !socials[it].isNullOrBlank() }
        else -> true
    }

    /** VALID: engedélyezett-e a blokk gombja. */
    fun valid(id: String): Boolean = when (id) {
        "personal" -> name.isNotBlank()
        "company" -> company.isNotBlank()
        "online" -> emailOk(email)
        "done" -> slugState().ok && name.isNotBlank() && emailOk(email) && (!isBiz || company.isNotBlank())
        else -> true
    }

    /** A blokk gombjának felirata („Tovább” / „Kihagyom” / „Publikálás” / „Mentés”). */
    fun actionLabel(b: WBlock): String = when {
        b.id == "done" -> if (isPublic) "Publikálás" else "Mentés"
        b.optional && !has(b.id) -> "Kihagyom"
        else -> "Tovább"
    }

    fun isSkip(b: WBlock) = b.optional && !has(b.id)

    fun nextOf(id: String): WBlock? = flow.getOrNull(fidx(id) + 1)

    /* ---------------------------------------------------------------- műveletek */

    fun chooseType(t: ProfileType) {
        if (type == t) return
        type = t
        dirty = true
        if (t == ProfileType.Private && status("company") == BlockStatus.Active) {
            bs["company"] = BlockStatus.Todo
            if (status("online") == BlockStatus.Todo) bs["online"] = BlockStatus.Active
        }
        if (t == ProfileType.Business && started && status("company") == BlockStatus.Todo && status("personal") != BlockStatus.Active) {
            bs["company"] = BlockStatus.Active
            listOf("online", "done").forEach { if (status(it) == BlockStatus.Active) bs[it] = BlockStatus.Todo }
        }
        if (!slugEdited && status("done") != BlockStatus.Todo) slug = suggested()
        // típusváltás után mindig legyen egy aktív blokk (különben nem lehetne publikálni)
        if (started && activeBlock == null) {
            flow.firstOrNull { status(it.id) == BlockStatus.Todo || status(it.id) == BlockStatus.Skip }?.let { b ->
                if (b.id == "done" && (!slugEdited || slug.isEmpty())) slug = suggested()
                bs[b.id] = BlockStatus.Active
                scrollRequest = ScrollReq(b.id, instant = true)
            }
        }
    }

    /** A kezdőképernyőről indulva: az első blokk aktív lesz, a lapozó odaugrik, a kezdőképernyő kicsúszik. */
    fun begin() {
        if (!started) {
            started = true
            bs["personal"] = BlockStatus.Active
        }
        scrollRequest = ScrollReq(activeBlock?.id ?: "personal", instant = true)
        introShown = false
    }

    /** „Tovább” / „Kihagyom”: a blokk kész (vagy kihagyva), a következő aktív, a lapozó odagörget. */
    fun next(id: String, typing: Boolean) {
        val fl = flow
        val n = fidx(id)
        if (n < 0 || n >= fl.size - 1) return
        val b = fl[n]
        bs[id] = if (b.optional && !has(id)) BlockStatus.Skip else BlockStatus.Done
        // a következő még nem kész blokk lesz aktív (ha közben már kitöltött blokk jön, átugorjuk)
        val nb = fl.drop(n + 1).firstOrNull { status(it.id) == BlockStatus.Todo || status(it.id) == BlockStatus.Skip } ?: fl[n + 1]
        if (nb.id == "done" && (!slugEdited || slug.isEmpty())) slug = suggested()
        if (status(nb.id) == BlockStatus.Todo || status(nb.id) == BlockStatus.Skip) bs[nb.id] = BlockStatus.Active
        if (typing && nb.id != "done") focusRequest = fieldKeys(nb.id).firstOrNull()
        scrollRequest = ScrollReq(nb.id, instant = false)
    }

    /** „Mégis kitöltöm” egy kihagyott blokkon. */
    fun reopen(id: String) {
        bs[id] = BlockStatus.Open
        scrollRequest = ScrollReq(id, instant = false)
    }

    fun jump(id: String) {
        scrollRequest = ScrollReq(id, instant = false)
    }

    /** Publikálás: az új profil, vagy null (és hibajelzés), ha hiányzik a név. */
    fun buildProfile(): Profile? {
        if (name.isBlank()) {
            if (status("personal") != BlockStatus.Active) bs["personal"] = BlockStatus.Open
            errors["name"] = "Add meg a neved."
            scrollRequest = ScrollReq("personal", instant = false)
            return null
        }
        fun https(v: String): String {
            val t = v.trim()
            return if (t.isEmpty() || t.startsWith("https://")) t else "https://" + t.removePrefix("http://")
        }
        return Profile(
            id = "n" + System.currentTimeMillis(),
            label = if (isBiz && company.isNotBlank()) company.trim() else "Személyes",
            real = false,
            name = name.trim(),
            title = if (isBiz) role.trim() else "",
            company = if (isBiz) company.trim() else "",
            bio = if (isBiz) bio.trim() else "",
            phone = phone.trim(),
            email = email.trim(),
            web = https(web),
            address = if (isBiz) place.trim() else "",
            socials = socialOrder.associateWith { https(socials[it] ?: "") }.filterValues { it.isNotEmpty() },
            photo = photo,
            logo = if (isBiz) logo else null,
            isPublic = isPublic,
            slug = slug,
            presetId = preset,
            tag = "Új",
            fresh = true,
        )
    }

    /* ---------------------------------------------------------------- bevitel */

    private fun autoSlug() {
        if (!slugEdited && status("done") != BlockStatus.Todo) slug = suggested()
    }

    fun input(key: String, v: String) {
        dirty = true
        when {
            key == "slug" -> {
                slug = v.lowercase().replace(Regex("\\s+"), "-")
                slugEdited = true
            }
            key.startsWith("s-") -> socials[key.removePrefix("s-")] = v
            else -> {
                when (key) {
                    "name" -> {
                        name = v
                        errors.remove("name")
                    }
                    "phone" -> phone = v
                    "company" -> company = v
                    "role" -> role = v
                    "place" -> place = v
                    "bio" -> bio = v.take(420)
                    "email" -> email = v
                    "web" -> web = v
                }
                if (key == "name" || key == "company") autoSlug()
            }
        }
    }

    fun value(key: String): String = when {
        key.startsWith("s-") -> socials[key.removePrefix("s-")] ?: ""
        else -> when (key) {
            "name" -> name
            "phone" -> phone
            "company" -> company
            "role" -> role
            "place" -> place
            "bio" -> bio
            "email" -> email
            "web" -> web
            "slug" -> slug
            else -> ""
        }
    }

    fun toggleSocial(id: String) {
        val at = socialOrder.indexOf(id)
        if (at >= 0) {
            socialOrder.removeAt(at)
            socials.remove(id)
        } else {
            socialOrder.add(id)
            if (socials[id] == null) socials[id] = ""
            focusRequest = "s-$id"
        }
        dirty = true
    }

    fun setPic(kind: String, pic: Pic?) {
        if (kind == "photo") photo = pic else logo = pic
        if (pic != null) dirty = true
        errors.remove(kind)
    }

    companion object {
        private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")
        private val SLUG = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")
    }
}
