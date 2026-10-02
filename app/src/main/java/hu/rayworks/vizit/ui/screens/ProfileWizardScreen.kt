package hu.rayworks.vizit.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Briefcase
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private data class WizardStepCopy(
    val eyebrow: String? = null,
    val title: String,
    val subtitle: String? = null,
)

// Native implementation of the supplied vizit-nevjegyvarazslo.html phone UI.
// The desktop control panel, simulated phone frame and status bar are excluded.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileWizardScreen(
    onSave: suspend (ContactProfile) -> String?,
    presentation: CardPresentation,
    onAppearanceChange: (CardPresentation) -> Unit,
    onDone: () -> Unit,
    isAdditional: Boolean = false,
    onCancel: (() -> Unit)? = null,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var type by rememberSaveable { mutableStateOf("") }
    var stepIndex by rememberSaveable { mutableStateOf(0) }
    var direction by rememberSaveable { mutableStateOf(1) }
    var draft by remember { mutableStateOf(ContactProfile(isPublic = true)) }
    var style by remember { mutableStateOf(CardColorway.INK) }
    var selectedSocial by remember { mutableStateOf(setOf<String>()) }
    var slugEdited by rememberSaveable { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var published by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var skipped by remember { mutableStateOf(setOf<String>()) }
    var returnToReview by remember { mutableStateOf(false) }

    val steps = if (type == "private") {
        listOf("type", "identity", "photo", "contact", "social", "look", "done")
    } else {
        listOf("type", "identity", "photo", "logo", "contact", "social", "bio", "look", "done")
    }
    val key = steps[stepIndex.coerceIn(steps.indices)]
    val total = steps.size - 1
    val optional = setOf("photo", "logo", "contact", "social", "bio")
    val photo = rememberProfilePhoto(draft.photoBase64)
    val logo = rememberProfilePhoto(draft.logoBase64)

    val copy = when (key) {
        "type" -> WizardStepCopy(
            eyebrow = if (isAdditional) "Új névjegyed" else "Első névjegyed",
            title = "Milyen névjegyet készítesz?",
            subtitle = "Később bármikor létrehozhatsz egy másikat is.",
        )
        "identity" -> if (type == "private") {
            WizardStepCopy(title = "Hogy hívnak?", subtitle = "Így jelenik meg a neved a névjegyeden.")
        } else {
            WizardStepCopy(title = "Mutatkozz be", subtitle = "Ez kerül a névjegyed tetejére.")
        }
        "photo" -> WizardStepCopy(
            title = "Profilkép",
            subtitle = "Egy arc többet mond egy névnél. A partnereid könnyebben felismernek.",
        )
        "logo" -> WizardStepCopy(
            title = "Céges logó",
            subtitle = "A profilkép jobb alsó sarkában jelenik meg, a névjegyen és a nyilvános profilon is.",
        )
        "contact" -> WizardStepCopy(
            title = "Hol érnek el?",
            subtitle = "Csak azt add meg, amit nyilvánosan is megosztanál.",
        )
        "social" -> WizardStepCopy(
            title = "Közösségi profilok",
            subtitle = "Koppints arra, amit hozzáadnál. Bármikor bővítheted.",
        )
        "bio" -> WizardStepCopy(
            title = "Pár mondat rólad",
            subtitle = "Mivel foglalkozol, miben tudsz segíteni? A nyilvános profilodon jelenik meg.",
        )
        "look" -> WizardStepCopy(
            title = "Válassz stílust",
            subtitle = "Egyedi színeket és színátmenetet később a Megjelenés menüben állíthatsz be.",
        )
        else -> WizardStepCopy(
            eyebrow = "Utolsó lépés",
            title = "Elkészült a névjegyed",
            subtitle = "Nézd át, és publikáld. Minden adatot később is módosíthatsz.",
        )
    }

    fun pick(kind: String, selected: Uri?) {
        if (selected == null) return
        scope.launch {
            loading = true
            error = ""
            runCatching {
                withContext(Dispatchers.IO) {
                    PhotoProcessor.loadSquareJpegBase64(context, selected)
                }
            }.onSuccess { image ->
                draft = if (kind == "photo") draft.copy(photoBase64 = image)
                else draft.copy(logoBase64 = image)
            }.onFailure {
                error = "A kiválasztott kép nem dolgozható fel. Válassz JPG, PNG vagy WebP képet."
            }
            loading = false
        }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        pick("photo", it)
    }
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        pick("logo", it)
    }

    val hasValue = when (key) {
        "photo" -> draft.photoBase64.isNotBlank()
        "logo" -> draft.logoBase64.isNotBlank()
        "contact" -> listOf(draft.email, draft.phone, draft.website, draft.address).any(String::isNotBlank)
        "social" -> selectedSocial.any { socialValue(draft, it).isNotBlank() }
        "bio" -> draft.bio.isNotBlank()
        else -> true
    }
    val valid = when (key) {
        "type" -> type.isNotBlank()
        "identity" -> draft.fullName.trim().length >= 2 && (type == "private" || draft.company.isNotBlank())
        "contact" -> draft.email.isBlank() ||
            Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$").matches(draft.email.trim())
        "done" -> draft.publicSlug.matches(Regex("^[a-z0-9]+(?:-[a-z0-9]+)*$")) &&
            draft.publicSlug.length in 3..50
        else -> true
    }

    fun prepareDoneSlug() {
        if (!slugEdited) {
            draft = draft.copy(
                publicSlug = wizardSlug(if (type == "business") draft.company else draft.fullName),
            )
        }
    }

    fun advance(skip: Boolean) {
        if (skip) {
            draft = when (key) {
                "photo" -> draft.copy(photoBase64 = "")
                "logo" -> draft.copy(logoBase64 = "")
                "contact" -> draft.copy(phone = "", email = "", website = "", address = "")
                "social" -> draft.copy(
                    linkedIn = "", facebook = "", instagram = "", youtube = "",
                    tiktok = "", x = "", github = "", customSocial = "",
                )
                "bio" -> draft.copy(bio = "")
                else -> draft
            }
            if (key == "social") selectedSocial = emptySet()
        }
        skipped = if (skip) skipped + key else skipped - key
        direction = 1
        stepIndex = if (returnToReview) steps.lastIndex else (stepIndex + 1).coerceAtMost(steps.lastIndex)
        returnToReview = false
        error = ""
        if (steps[stepIndex] == "done") prepareDoneSlug()
    }

    fun goBack() {
        direction = -1
        stepIndex = if (returnToReview) steps.lastIndex else (stepIndex - 1).coerceAtLeast(0)
        returnToReview = false
        error = ""
    }

    BackHandler(enabled = stepIndex > 0 || onCancel != null) {
        if (stepIndex > 0) goBack() else onCancel?.invoke()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Vizit.colors.canvas)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding(),
    ) {
        WizardTopBar(
            step = stepIndex,
            total = total,
            done = key == "done",
            canClose = stepIndex == 0 && isAdditional && onCancel != null,
            onBack = ::goBack,
            onClose = { onCancel?.invoke() },
        )

        if (published) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    Modifier.size(54.dp).background(Vizit.colors.successSubtle, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null, tint = Vizit.colors.success)
                }
                Text("Publikálva", style = Vizit.type.h2, color = Vizit.colors.textPrimary)
                Text(
                    if (draft.isPublic) {
                        "A névjegyed él. Oszd meg a címét, vagy mutasd a QR-kódot."
                    } else {
                        "A névjegyed elkészült. A nyilvános profilt a Beállításokban kapcsolhatod be."
                    },
                    color = Vizit.colors.textSecondary,
                )
                ProfileCard(profile = draft, presentation = presentation.copy(colorway = style))
                VizitButton("Tovább az áttekintéshez", onClick = onDone, modifier = Modifier.fillMaxWidth())
            }
            return@Column
        }

        val easing = CubicBezierEasing(0.2f, 0.7f, 0.2f, 1f)
        AnimatedContent(
            targetState = key,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                (
                    slideInHorizontally(tween(280, easing = easing)) { if (direction >= 0) 24 else -24 } +
                        fadeIn(tween(180))
                    ) togetherWith
                    (
                        slideOutHorizontally(tween(180, easing = easing)) { if (direction >= 0) -12 else 12 } +
                            fadeOut(tween(140))
                        )
            },
            label = "wizard-step",
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                WizardHeading(copy)

                if (key != "type" && key != "done") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Élő előnézet", style = Vizit.type.caption, color = Vizit.colors.textMuted)
                        WizardCompactCard(
                            profile = draft,
                            colorway = style,
                            business = type != "private",
                            photo = photo,
                            logo = logo,
                        )
                    }
                }

                when (key) {
                    "type" -> {
                        WizardTypeChoice(
                            title = "Vállalkozói névjegy",
                            detail = "Cégnek vagy egyéni vállalkozásnak: vállalkozás neve, beosztás, céges logó, bemutatkozás.",
                            tags = listOf("8 lépés", "kb. 2 perc"),
                            business = true,
                            selected = type == "business",
                        ) {
                            type = "business"
                            error = ""
                        }
                        WizardTypeChoice(
                            title = "Magánszemély",
                            detail = "Személyes kapcsolatokhoz. Csak a lényeg, gyorsan kész.",
                            tags = listOf("6 lépés", "kb. 1 perc"),
                            business = false,
                            selected = type == "private",
                        ) {
                            type = "private"
                            draft = draft.copy(company = "", jobTitle = "", bio = "", logoBase64 = "")
                            error = ""
                        }
                    }

                    "identity" -> WizardBlock {
                        VizitTextField(draft.fullName, { draft = draft.copy(fullName = it) }, "Teljes név *")
                        if (type != "private") {
                            VizitTextField(draft.company, { draft = draft.copy(company = it) }, "Vállalkozás / szervezet *")
                            VizitTextField(draft.jobTitle, { draft = draft.copy(jobTitle = it) }, "Beosztás · nem kötelező")
                        }
                    }

                    "photo" -> WizardBlock {
                        Column(
                            Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            WizardPhotoAvatar(draft, photo, logo, showLogo = false)
                            VizitButton(
                                if (photo == null) "Kép kiválasztása" else "Másik kép",
                                onClick = {
                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                enabled = !loading,
                                loading = loading,
                            )
                            if (photo != null) {
                                VizitButton(
                                    "Eltávolítás",
                                    onClick = { draft = draft.copy(photoBase64 = "") },
                                    style = VizitButtonStyle.Tertiary,
                                )
                            }
                            Text(
                                "JPG, PNG vagy WebP, legfeljebb 25 MB. A nagy képet automatikusan kisebbre méretezzük.",
                                style = Vizit.type.bodySmall,
                                color = Vizit.colors.textMuted,
                            )
                        }
                    }

                    "logo" -> WizardBlock {
                        Column(
                            Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            WizardPhotoAvatar(draft, photo, logo, showLogo = true)
                            VizitButton(
                                if (logo == null) "Logó feltöltése" else "Logó cseréje",
                                onClick = {
                                    logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                enabled = !loading,
                                loading = loading,
                            )
                            if (logo != null) {
                                VizitButton(
                                    "Eltávolítás",
                                    onClick = { draft = draft.copy(logoBase64 = "") },
                                    style = VizitButtonStyle.Tertiary,
                                )
                            }
                            Text(
                                "Átlátszó hátterű PNG mutat a legjobban.",
                                style = Vizit.type.bodySmall,
                                color = Vizit.colors.textMuted,
                            )
                        }
                    }

                    "contact" -> WizardBlock {
                        VizitTextField(
                            draft.email,
                            { draft = draft.copy(email = it) },
                            "Nyilvános e-mail",
                            keyboardType = KeyboardType.Email,
                        )
                        VizitTextField(
                            draft.phone,
                            { draft = draft.copy(phone = it) },
                            "Telefonszám",
                            keyboardType = KeyboardType.Phone,
                        )
                        VizitTextField(
                            draft.website,
                            { draft = draft.copy(website = it) },
                            "Weboldal",
                            keyboardType = KeyboardType.Uri,
                        )
                        if (type != "private") {
                            VizitTextField(draft.address, { draft = draft.copy(address = it) }, "Hely / cím")
                        }
                        if (!valid) {
                            Text(
                                "Ez nem tűnik érvényes e-mail-címnek.",
                                style = Vizit.type.bodySmall,
                                color = Vizit.colors.error,
                            )
                        }
                    }

                    "social" -> WizardBlock {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            wizardSocials.forEach { social ->
                                val selected = social.id in selectedSocial
                                Row(
                                    Modifier
                                        .clip(CircleShape)
                                        .background(if (selected) Vizit.colors.primarySubtle else Vizit.colors.surface)
                                        .border(
                                            1.dp,
                                            if (selected) Color.Transparent else Vizit.colors.borderStrong,
                                            CircleShape,
                                        )
                                        .clickable {
                                            selectedSocial = if (selected) selectedSocial - social.id
                                            else selectedSocial + social.id
                                            if (selected) draft = setSocialValue(draft, social.id, "")
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        if (selected) Icons.Outlined.Check else Icons.Outlined.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (selected) Vizit.colors.primary else Vizit.colors.textPrimary,
                                    )
                                    Text(
                                        social.label,
                                        fontSize = 14.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (selected) Vizit.colors.primary else Vizit.colors.textPrimary,
                                    )
                                }
                            }
                        }
                        wizardSocials.filter { it.id in selectedSocial }.forEach { social ->
                            VizitTextField(
                                socialValue(draft, social.id),
                                { draft = setSocialValue(draft, social.id, it) },
                                social.label,
                                keyboardType = KeyboardType.Uri,
                            )
                        }
                    }

                    "bio" -> WizardBlock {
                        VizitTextField(
                            draft.bio,
                            { draft = draft.copy(bio = it.take(420)) },
                            "Rövid bemutatkozás · nem kötelező",
                            singleLine = false,
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Két-három mondat elég.", style = Vizit.type.bodySmall, color = Vizit.colors.textMuted)
                            Text("${draft.bio.length}/420", style = Vizit.type.bodySmall, color = Vizit.colors.textMuted)
                        }
                    }

                    "look" -> {
                        CardColorway.entries.take(4).chunked(2).forEach { row ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                row.forEach { colorway ->
                                    WizardStyleSwatch(
                                        colorway = colorway,
                                        selected = style == colorway,
                                        modifier = Modifier.weight(1f),
                                    ) { style = colorway }
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }

                    "done" -> {
                        ProfileCard(profile = draft, presentation = presentation.copy(colorway = style))
                        WizardBlock {
                            VizitTextField(
                                draft.publicSlug,
                                {
                                    slugEdited = true
                                    draft = draft.copy(publicSlug = wizardSlug(it))
                                },
                                "A névjegyed címe",
                            )
                            Text(
                                "A név alapján javasoltuk. Később is módosítható.",
                                style = Vizit.type.bodySmall,
                                color = Vizit.colors.textMuted,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Nyilvános profil", style = Vizit.type.label)
                                    Text(
                                        if (draft.isPublic) {
                                            "A névjegy a hivatkozással bárki számára megnyitható."
                                        } else {
                                            "Csak te látod. QR-rel és NFC-vel ettől még átadhatod."
                                        },
                                        style = Vizit.type.bodySmall,
                                        color = Vizit.colors.textSecondary,
                                    )
                                }
                                Switch(
                                    checked = draft.isPublic,
                                    onCheckedChange = { draft = draft.copy(isPublic = it) },
                                )
                            }
                        }

                        val missing = skipped.filter { it in steps }
                        if (missing.isNotEmpty()) {
                            WizardBlock {
                                Text(
                                    "KÉSŐBB BEÁLLÍTHATOD",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Vizit.colors.textMuted,
                                    letterSpacing = 1.1.sp,
                                )
                                missing.forEach { item ->
                                    Row(
                                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(wizardTitle(item), style = Vizit.type.body)
                                            Text(
                                                wizardTodoHint(item),
                                                style = Vizit.type.bodySmall,
                                                color = Vizit.colors.textMuted,
                                            )
                                        }
                                        VizitButton(
                                            "Beállítás",
                                            onClick = {
                                                direction = -1
                                                stepIndex = steps.indexOf(item)
                                                returnToReview = true
                                            },
                                            style = VizitButtonStyle.Secondary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (error.isNotBlank()) {
                    Text(error, style = Vizit.type.bodySmall, color = Vizit.colors.error)
                }

                Spacer(Modifier.height(8.dp))
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .background(Vizit.colors.canvas)
                .border(1.dp, Vizit.colors.divider)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            VizitButton(
                if (key == "done") {
                    if (draft.isPublic) "Névjegy publikálása" else "Névjegy mentése"
                } else if (returnToReview) {
                    "Mentés"
                } else {
                    "Tovább"
                },
                onClick = {
                    if (key != "done") {
                        advance(false)
                    } else {
                        scope.launch {
                            loading = true
                            error = ""
                            val prepared = wizardUrls(draft)
                            if (prepared == null) {
                                error = "A webes és közösségi hivatkozások teljes, https:// kezdetű címek legyenek."
                            } else {
                                val issue = runCatching { onSave(prepared) }
                                    .getOrElse { it.localizedMessage ?: "A mentés nem sikerült." }
                                if (issue == null) {
                                    onAppearanceChange(presentation.copy(colorway = style))
                                    published = true
                                } else {
                                    error = issue
                                }
                            }
                            loading = false
                        }
                    }
                },
                enabled = valid && (key !in optional || hasValue) && !loading,
                loading = loading,
                modifier = Modifier.fillMaxWidth(),
            )
            if (key in optional) {
                VizitButton(
                    "Később állítom be",
                    onClick = { advance(true) },
                    style = VizitButtonStyle.Tertiary,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading,
                )
            }
        }
    }
}

@Composable
private fun WizardTopBar(
    step: Int,
    total: Int,
    done: Boolean,
    canClose: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            when {
                step > 0 -> Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Vissza",
                    modifier = Modifier.size(22.dp).clickable(onClick = onBack),
                    tint = Vizit.colors.textSecondary,
                )
                canClose -> Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Bezárás",
                    modifier = Modifier.size(22.dp).clickable(onClick = onClose),
                    tint = Vizit.colors.textSecondary,
                )
            }
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(total.coerceAtLeast(1)) { index ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(
                                if (index <= step.coerceAtMost(total - 1) || done) {
                                    Vizit.colors.primary
                                } else {
                                    Vizit.colors.controlTrack
                                },
                                CircleShape,
                            ),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (done) "Kész" else "${step + 1} / $total",
                style = Vizit.type.caption,
                color = Vizit.colors.textMuted,
            )
        }
        Spacer(Modifier.size(44.dp))
    }
}

@Composable
private fun WizardHeading(copy: WizardStepCopy) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        copy.eyebrow?.let {
            Text(
                it.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = Vizit.colors.primary,
            )
        }
        Text(
            copy.title,
            fontSize = 26.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Vizit.colors.textPrimary,
        )
        copy.subtitle?.let {
            Text(it, style = Vizit.type.body, color = Vizit.colors.textSecondary)
        }
    }
}

@Composable
private fun WizardBlock(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Vizit.colors.surface, RoundedCornerShape(16.dp))
            .border(1.dp, Vizit.colors.border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun WizardTypeChoice(
    title: String,
    detail: String,
    tags: List<String>,
    business: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Vizit.colors.surface, RoundedCornerShape(16.dp))
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) Vizit.colors.primary else Vizit.colors.border,
                RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(44.dp)
                .background(Vizit.colors.primarySubtle, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (business) Icons.Outlined.Briefcase else Icons.Outlined.Person,
                contentDescription = null,
                tint = Vizit.colors.primary,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Vizit.colors.textPrimary)
            Text(detail, style = Vizit.type.bodySmall, color = Vizit.colors.textSecondary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 6.dp),
            ) {
                tags.forEach { tag ->
                    Text(
                        tag,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Vizit.colors.textSecondary,
                        modifier = Modifier
                            .background(Vizit.colors.sunken, CircleShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
        Box(
            Modifier
                .size(22.dp)
                .background(if (selected) Vizit.colors.primary else Color.Transparent, CircleShape)
                .border(
                    1.5.dp,
                    if (selected) Vizit.colors.primary else Vizit.colors.borderStrong,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun WizardCompactCard(
    profile: ContactProfile,
    colorway: CardColorway,
    business: Boolean,
    photo: ImageBitmap?,
    logo: ImageBitmap?,
) {
    val subtitle = if (business) {
        listOf(profile.jobTitle, profile.company).filter(String::isNotBlank).joinToString(" · ")
    } else ""
    Box(
        Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Color(colorway.gradientStart), Color(colorway.gradientEnd)))),
    ) {
        Box(
            Modifier
                .size(width = 4.dp, height = 112.dp)
                .background(Color(colorway.accent)),
        )
        Row(
            Modifier.fillMaxSize().padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            WizardMiniAvatar(profile, photo, logo)
            Column(Modifier.weight(1f)) {
                Text(
                    profile.resolvedDisplayName.ifBlank { "A neved" },
                    style = Vizit.type.h3,
                    color = Color.White,
                    maxLines = 1,
                )
                if (subtitle.isNotBlank()) {
                    Text(subtitle, style = Vizit.type.bodySmall, color = Color.White.copy(alpha = 0.68f), maxLines = 1)
                }
                Spacer(Modifier.weight(1f))
                if (profile.phone.isNotBlank()) {
                    Text(profile.phone, style = Vizit.type.caption, color = Color.White.copy(alpha = 0.82f))
                }
                if (profile.email.isNotBlank()) {
                    Text(profile.email, style = Vizit.type.caption, color = Color.White.copy(alpha = 0.82f), maxLines = 1)
                }
            }
            Column(Modifier.height(84.dp), verticalArrangement = Arrangement.Bottom) {
                Text(
                    "VIZIT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                    color = Color(colorway.accent),
                )
            }
        }
    }
}

@Composable
private fun WizardMiniAvatar(
    profile: ContactProfile,
    photo: ImageBitmap?,
    logo: ImageBitmap?,
) {
    Box(Modifier.size(44.dp)) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
                .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (photo != null) {
                Image(photo, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Text(
                    profile.initials.ifBlank { "V" },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.92f),
                )
            }
        }
        if (logo != null) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(18.dp)
                    .background(Color.White, RoundedCornerShape(4.dp))
                    .padding(2.dp),
            ) {
                Image(logo, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
        }
    }
}

@Composable
private fun WizardPhotoAvatar(
    profile: ContactProfile,
    photo: ImageBitmap?,
    logo: ImageBitmap?,
    showLogo: Boolean,
) {
    Box(Modifier.size(148.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(132.dp)
                .clip(CircleShape)
                .background(Vizit.colors.primarySubtle),
            contentAlignment = Alignment.Center,
        ) {
            if (photo != null) {
                Image(photo, contentDescription = "Profilkép", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Text(
                    profile.initials.ifBlank { "V" },
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = Vizit.colors.primary,
                )
            }
        }
        if (showLogo) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(50.dp)
                    .background(Vizit.colors.surface, RoundedCornerShape(14.dp))
                    .border(1.5.dp, Vizit.colors.borderStrong, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (logo != null) {
                    Image(
                        logo,
                        contentDescription = "Céges logó",
                        modifier = Modifier.fillMaxSize().padding(5.dp),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Icon(Icons.Outlined.Add, contentDescription = null, tint = Vizit.colors.textMuted)
                }
            }
        }
    }
}

@Composable
private fun WizardStyleSwatch(
    colorway: CardColorway,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .background(Vizit.colors.surface, RoundedCornerShape(16.dp))
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) Vizit.colors.primary else Vizit.colors.border,
                RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(listOf(Color(colorway.gradientStart), Color(colorway.gradientEnd)))),
        ) {
            Box(Modifier.width(4.dp).height(56.dp).background(Color(colorway.accent)))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(colorway.label, style = Vizit.type.label, color = Vizit.colors.textPrimary)
            if (colorway == CardColorway.INK) {
                Text("Alap", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Vizit.colors.textMuted)
            }
        }
    }
}

private data class WizardSocial(val id: String, val label: String)

private val wizardSocials = listOf(
    WizardSocial("linkedin", "LinkedIn"),
    WizardSocial("facebook", "Facebook"),
    WizardSocial("instagram", "Instagram"),
    WizardSocial("youtube", "YouTube"),
    WizardSocial("tiktok", "TikTok"),
    WizardSocial("x", "X"),
    WizardSocial("github", "GitHub"),
    WizardSocial("custom", "Egyéb"),
)

private fun socialValue(profile: ContactProfile, id: String): String = when (id) {
    "linkedin" -> profile.linkedIn
    "facebook" -> profile.facebook
    "instagram" -> profile.instagram
    "youtube" -> profile.youtube
    "tiktok" -> profile.tiktok
    "x" -> profile.x
    "github" -> profile.github
    else -> profile.customSocial
}

private fun setSocialValue(profile: ContactProfile, id: String, value: String): ContactProfile = when (id) {
    "linkedin" -> profile.copy(linkedIn = value)
    "facebook" -> profile.copy(facebook = value)
    "instagram" -> profile.copy(instagram = value)
    "youtube" -> profile.copy(youtube = value)
    "tiktok" -> profile.copy(tiktok = value)
    "x" -> profile.copy(x = value)
    "github" -> profile.copy(github = value)
    else -> profile.copy(customSocial = value)
}

private fun wizardTitle(id: String): String = when (id) {
    "photo" -> "Profilkép"
    "logo" -> "Céges logó"
    "contact" -> "Elérhetőségek"
    "social" -> "Közösségi profilok"
    "bio" -> "Bemutatkozás"
    else -> id
}

private fun wizardTodoHint(id: String): String = when (id) {
    "photo" -> "A névjegy most monogramot mutat."
    "logo" -> "A logó a profilkép sarkában jelenik meg."
    "contact" -> "Még nincs nyilvános elérhetőséged."
    "social" -> "LinkedIn, Instagram és a többi."
    "bio" -> "Pár mondat a nyilvános profilra."
    else -> ""
}

private fun wizardSlug(value: String): String = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
    .replace(Regex("[\\u0300-\\u036f]"), "")
    .replace(Regex("[^a-z0-9]+"), "-")
    .trim('-')
    .take(50)

private fun wizardUrls(p: ContactProfile): ContactProfile? {
    fun url(value: String): String? = value.trim().takeIf(String::isNotBlank)?.let {
        val result = if (it.contains("://")) it else "https://$it"
        result.takeIf { link -> link.startsWith("https://") }
    } ?: ""

    val links = listOf(
        p.website, p.linkedIn, p.facebook, p.instagram, p.youtube,
        p.tiktok, p.x, p.github, p.customSocial,
    ).map(::url)
    if (links.any { it == null }) return null

    return p.copy(
        website = links[0]!!,
        linkedIn = links[1]!!,
        facebook = links[2]!!,
        instagram = links[3]!!,
        youtube = links[4]!!,
        tiktok = links[5]!!,
        x = links[6]!!,
        github = links[7]!!,
        customSocial = links[8]!!,
    )
}
