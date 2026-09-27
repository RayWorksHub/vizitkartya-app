package hu.rayworks.vizit.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.data.PhotoProcessor
import hu.rayworks.vizit.data.card.CardColorway
import hu.rayworks.vizit.data.card.CardPresentation
import hu.rayworks.vizit.ui.design.Vizit
import hu.rayworks.vizit.ui.design.components.VizitButton
import hu.rayworks.vizit.ui.design.components.VizitButtonStyle
import hu.rayworks.vizit.ui.design.components.VizitTextField
import hu.rayworks.vizit.ui.util.rememberProfilePhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer

/** First profile only. The device's own status/navigation bars frame this screen. */
@Composable
fun ProfileWizardScreen(
    onSave: suspend (ContactProfile) -> String?,
    presentation: CardPresentation,
    onAppearanceChange: (CardPresentation) -> Unit,
    onDone: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var type by rememberSaveable { mutableStateOf("") }
    var step by rememberSaveable { mutableStateOf(0) }
    var draft by remember { mutableStateOf(ContactProfile()) }
    var style by remember { mutableStateOf(CardColorway.INK) }
    var slugEdited by rememberSaveable { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var published by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var skipped by remember { mutableStateOf(setOf<String>()) }
    var returnToReview by remember { mutableStateOf(false) }
    val steps = if (type == "private") listOf("type", "identity", "photo", "contact", "social", "look", "done")
        else listOf("type", "identity", "photo", "logo", "contact", "social", "bio", "look", "done")
    val key = steps[step.coerceIn(steps.indices)]
    val names = mapOf("type" to "Névjegy típusa", "identity" to "Alapadatok", "photo" to "Profilkép",
        "logo" to "Céges logó", "contact" to "Elérhetőségek", "social" to "Közösségi profilok",
        "bio" to "Bemutatkozás", "look" to "Stílus", "done" to "Befejezés")
    val optional = setOf("photo", "logo", "contact", "social", "bio")
    val photo = rememberProfilePhoto(draft.photoBase64)
    val logo = rememberProfilePhoto(draft.logoBase64)

    fun pick(kind: String, selected: android.net.Uri?) {
        if (selected == null) return
        scope.launch {
            loading = true; error = ""
            runCatching { withContext(Dispatchers.IO) { PhotoProcessor.loadSquareJpegBase64(context, selected) } }
                .onSuccess { image -> draft = if (kind == "photo") draft.copy(photoBase64 = image) else draft.copy(logoBase64 = image) }
                .onFailure { error = "A kiválasztott kép nem dolgozható fel. Válassz JPG, PNG vagy WebP képet." }
            loading = false
        }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { pick("photo", it) }
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { pick("logo", it) }
    val has = when (key) {
        "photo" -> draft.photoBase64.isNotBlank()
        "logo" -> draft.logoBase64.isNotBlank()
        "contact" -> listOf(draft.email, draft.phone, draft.website, draft.address).any(String::isNotBlank)
        "social" -> listOf(draft.linkedIn, draft.facebook, draft.instagram, draft.youtube, draft.tiktok, draft.x, draft.github, draft.customSocial).any(String::isNotBlank)
        "bio" -> draft.bio.isNotBlank()
        else -> true
    }
    val valid = when (key) {
        "type" -> type.isNotBlank()
        "identity" -> draft.fullName.trim().length >= 2 && (type == "private" || draft.company.isNotBlank())
        "contact" -> draft.email.isBlank() || Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$").matches(draft.email.trim())
        "done" -> draft.publicSlug.matches(Regex("^[a-z0-9]+(?:-[a-z0-9]+)*$")) && draft.publicSlug.length in 3..50
        else -> true
    }
    fun advance(skip: Boolean) {
        if (skip) draft = when (key) {
            "photo" -> draft.copy(photoBase64 = "")
            "logo" -> draft.copy(logoBase64 = "")
            "contact" -> draft.copy(phone = "", email = "", website = "", address = "")
            "social" -> draft.copy(linkedIn = "", facebook = "", instagram = "", youtube = "", tiktok = "", x = "", github = "", customSocial = "")
            "bio" -> draft.copy(bio = "")
            else -> draft
        }
        skipped = if (skip) skipped + key else skipped - key
        step = if (returnToReview) steps.lastIndex else (step + 1).coerceAtMost(steps.lastIndex)
        returnToReview = false; error = ""
        if (steps[step] == "done" && !slugEdited) draft = draft.copy(publicSlug = wizardSlug(
            if (type == "business") draft.company else draft.fullName))
    }
    if (published) {
        Column(Modifier.fillMaxSize().background(Vizit.colors.canvas)
            .windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)
            .padding(24.dp), verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Elkészült a névjegyed", style = Vizit.type.h2, color = Vizit.colors.textPrimary)
            Spacer(Modifier.height(12.dp))
            Text(if (draft.isPublic) "A névjegyed publikálva. Most már megoszthatod a profilcímedet."
                else "A névjegyed elmentve. A nyilvános profilt később is bekapcsolhatod.")
            Spacer(Modifier.height(24.dp))
            VizitButton("Tovább az áttekintéshez", onClick = onDone, modifier = Modifier.fillMaxWidth())
        }
        return
    }
    BackHandler(enabled = step > 0) { step = if (returnToReview) steps.lastIndex else step - 1; returnToReview = false; error = "" }
    Column(Modifier.fillMaxSize().background(Vizit.colors.canvas).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars).imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(names[key].orEmpty(), style = Vizit.type.h2, color = Vizit.colors.textPrimary)
            Text(if (key == "done") "Kész" else "${step + 1} / ${steps.lastIndex}", color = Vizit.colors.textMuted)
        }
        LinearProgressIndicator(progress = { (step + 1f) / steps.lastIndex.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (key != "type") ProfileCard(profile = draft, presentation = presentation.copy(colorway = style))
            when (key) {
                "type" -> {
                    Text("Milyen névjegyet készítesz?", style = Vizit.type.h2, color = Vizit.colors.textPrimary)
                    WizardChoice("Vállalkozói névjegy", "Vállalkozás, beosztás, logó és bemutatkozás", type == "business") { type = "business" }
                    WizardChoice("Magánszemély", "Személyes kapcsolatokhoz, csak a lényeg", type == "private") {
                        type = "private"; draft = draft.copy(company = "", jobTitle = "", bio = "", logoBase64 = "")
                    }
                }
                "identity" -> {
                    Text(if (type == "business") "Mutatkozz be" else "Hogy hívnak?", style = Vizit.type.h2)
                    VizitTextField(draft.fullName, { draft = draft.copy(fullName = it) }, "Teljes név *")
                    if (type == "business") {
                        VizitTextField(draft.company, { draft = draft.copy(company = it) }, "Vállalkozás / szervezet *")
                        VizitTextField(draft.jobTitle, { draft = draft.copy(jobTitle = it) }, "Beosztás · nem kötelező")
                    }
                }
                "photo", "logo" -> {
                    Text(if (key == "photo") "Profilkép" else "Céges logó", style = Vizit.type.h2)
                    Text(if (key == "photo") "A partnereid könnyebben felismernek." else "A logó a profilkép sarkán jelenik meg.")
                    val bitmap = if (key == "photo") photo else logo
                    if (bitmap != null) Image(bitmap, contentDescription = if (key == "photo") "Profilkép" else "Céges logó",
                        modifier = Modifier.fillMaxWidth().height(140.dp), contentScale = ContentScale.Fit)
                    VizitButton(if (bitmap == null) "Kép kiválasztása" else "Másik kép", onClick = {
                        (if (key == "photo") photoPicker else logoPicker).launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }, enabled = !loading, loading = loading, modifier = Modifier.fillMaxWidth())
                    if (bitmap != null) VizitButton("Eltávolítás", onClick = { draft = if (key == "photo") draft.copy(photoBase64 = "") else draft.copy(logoBase64 = "") },
                        style = VizitButtonStyle.Tertiary, modifier = Modifier.fillMaxWidth())
                }
                "contact" -> {
                    Text("Hol érnek el?", style = Vizit.type.h2)
                    Text("Csak azt add meg, amit nyilvánosan is megosztanál.")
                    VizitTextField(draft.email, { draft = draft.copy(email = it) }, "Nyilvános e-mail", keyboardType = KeyboardType.Email)
                    VizitTextField(draft.phone, { draft = draft.copy(phone = it) }, "Telefonszám", keyboardType = KeyboardType.Phone)
                    VizitTextField(draft.website, { draft = draft.copy(website = it) }, "Weboldal", keyboardType = KeyboardType.Uri)
                    if (type == "business") VizitTextField(draft.address, { draft = draft.copy(address = it) }, "Hely / cím")
                    if (!valid) Text("Érvényes e-mail-címet adj meg.", color = Vizit.colors.error)
                }
                "social" -> {
                    Text("Közösségi profilok", style = Vizit.type.h2)
                    Text("Add meg, amelyiket használod. A többit később is hozzáadhatod.")
                    VizitTextField(draft.linkedIn, { draft = draft.copy(linkedIn = it) }, "LinkedIn")
                    VizitTextField(draft.facebook, { draft = draft.copy(facebook = it) }, "Facebook")
                    VizitTextField(draft.instagram, { draft = draft.copy(instagram = it) }, "Instagram")
                    VizitTextField(draft.youtube, { draft = draft.copy(youtube = it) }, "YouTube")
                    VizitTextField(draft.tiktok, { draft = draft.copy(tiktok = it) }, "TikTok")
                    VizitTextField(draft.x, { draft = draft.copy(x = it) }, "X")
                    VizitTextField(draft.github, { draft = draft.copy(github = it) }, "GitHub")
                    VizitTextField(draft.customSocial, { draft = draft.copy(customSocial = it) }, "Egyéb hivatkozás")
                }
                "bio" -> {
                    Text("Pár mondat rólad", style = Vizit.type.h2)
                    VizitTextField(draft.bio, { draft = draft.copy(bio = it.take(420)) }, "Rövid bemutatkozás", singleLine = false)
                    Text("${draft.bio.length}/420", color = Vizit.colors.textMuted)
                }
                "look" -> {
                    Text("Válassz stílust", style = Vizit.type.h2)
                    CardColorway.entries.take(4).forEach { color ->
                        WizardChoice(color.label, "Kártya színvilága", style == color) { style = color }
                    }
                }
                "done" -> {
                    Text("Elkészült a névjegyed", style = Vizit.type.h2)
                    Text("Nézd át, és mentsd el. Később minden adatot módosíthatsz.")
                    VizitTextField(draft.publicSlug, { slugEdited = true; draft = draft.copy(publicSlug = wizardSlug(it)) }, "Profilcím: vizitkartyam.hu/…")
                    Text("A cím foglaltságát szinkronizáláskor ellenőrizzük.", color = Vizit.colors.textMuted)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Nyilvános profil", modifier = Modifier.weight(1f))
                        Switch(checked = draft.isPublic, onCheckedChange = { draft = draft.copy(isPublic = it) })
                    }
                    if (skipped.isNotEmpty()) {
                        Text("Később beállíthatod", style = Vizit.type.label)
                        skipped.filter { it in steps }.forEach { missing ->
                            VizitButton(names[missing].orEmpty(), onClick = { step = steps.indexOf(missing); returnToReview = true },
                                style = VizitButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
            if (error.isNotBlank()) Text(error, color = Vizit.colors.error)
        }
        Column(Modifier.fillMaxWidth().background(Vizit.colors.surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (step > 0) VizitButton("Vissza", onClick = {
                step = if (returnToReview) steps.lastIndex else step - 1; returnToReview = false; error = ""
            }, style = VizitButtonStyle.Tertiary, modifier = Modifier.fillMaxWidth())
            VizitButton(if (key == "done") (if (draft.isPublic) "Névjegy publikálása" else "Névjegy mentése")
                else (if (returnToReview) "Mentés" else "Tovább"), onClick = {
                if (key != "done") advance(false) else scope.launch {
                    loading = true; error = ""
                    val prepared = wizardUrls(draft)
                    if (prepared == null) error = "A webes és közösségi hivatkozások teljes, https:// kezdetű címek legyenek."
                    else {
                        val issue = runCatching { onSave(prepared) }.getOrElse { it.localizedMessage ?: "A mentés nem sikerült." }
                        if (issue == null) { onAppearanceChange(presentation.copy(colorway = style)); published = true } else error = issue
                    }
                    loading = false
                }
            }, enabled = valid && (key !in optional || has) && !loading, loading = loading, modifier = Modifier.fillMaxWidth())
            if (key in optional) VizitButton("Később állítom be", onClick = { advance(true) }, style = VizitButtonStyle.Tertiary,
                modifier = Modifier.fillMaxWidth(), enabled = !loading)
        }
    }
}

@Composable
private fun WizardChoice(title: String, detail: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(if (selected) Vizit.colors.primarySubtle else Vizit.colors.surface,
        RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Column { Text(title, style = Vizit.type.label); Text(detail, color = Vizit.colors.textSecondary) }
        if (selected) Text("✓", color = Vizit.colors.primary)
    }
}

private fun wizardSlug(value: String): String = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
    .replace(Regex("[\\u0300-\\u036f]"), "").replace(Regex("[^a-z0-9]+"), "-")
    .trim('-').take(50)

private fun wizardUrls(p: ContactProfile): ContactProfile? {
    fun url(value: String): String? = value.trim().takeIf(String::isNotBlank)?.let {
        val result = if (it.contains("://")) it else "https://$it"
        result.takeIf { link -> link.startsWith("https://") }
    } ?: ""
    val links = listOf(p.website,p.linkedIn,p.facebook,p.instagram,p.youtube,p.tiktok,p.x,p.github,p.customSocial).map(::url)
    if (links.any { it == null }) return null
    return p.copy(website=links[0]!!,linkedIn=links[1]!!,facebook=links[2]!!,instagram=links[3]!!,
        youtube=links[4]!!,tiktok=links[5]!!,x=links[6]!!,github=links[7]!!,customSocial=links[8]!!)
}
