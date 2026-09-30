package hu.rayworks.vizit.v10.ui.sheets

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.rayworks.vizit.R
import hu.rayworks.vizit.v10.data.AppState
import hu.rayworks.vizit.v10.data.CRM_URL
import hu.rayworks.vizit.v10.data.DesignLayout
import hu.rayworks.vizit.v10.data.EDITOR_URL
import hu.rayworks.vizit.v10.data.NfcDevice
import hu.rayworks.vizit.v10.data.Page
import hu.rayworks.vizit.v10.data.Pic
import hu.rayworks.vizit.v10.data.Profile
import hu.rayworks.vizit.v10.data.SOCIALS
import hu.rayworks.vizit.v10.data.SheetKind
import hu.rayworks.vizit.v10.data.SyncStatus
import hu.rayworks.vizit.v10.data.VIS_FIELDS
import hu.rayworks.vizit.v10.data.AuthMode
import hu.rayworks.vizit.v10.data.Gate
import hu.rayworks.vizit.v10.ui.LocalActions
import hu.rayworks.vizit.v10.ui.components.ExtMark
import hu.rayworks.vizit.v10.ui.components.FilterChip
import hu.rayworks.vizit.v10.ui.components.GroupCard
import hu.rayworks.vizit.v10.ui.components.GroupLabel
import hu.rayworks.vizit.v10.ui.components.IconCircleButton
import hu.rayworks.vizit.v10.ui.components.ListRow
import hu.rayworks.vizit.v10.ui.components.OutlinedField
import hu.rayworks.vizit.v10.ui.components.PicImage
import hu.rayworks.vizit.v10.ui.components.ProfileAvatar
import hu.rayworks.vizit.v10.ui.components.ProfileLabel
import hu.rayworks.vizit.v10.ui.components.Segmented
import hu.rayworks.vizit.v10.ui.components.SheetGrab
import hu.rayworks.vizit.v10.ui.components.SheetOverlay
import hu.rayworks.vizit.v10.ui.components.StatusPill
import hu.rayworks.vizit.v10.ui.components.Tone
import hu.rayworks.vizit.v10.ui.components.VSwitch
import hu.rayworks.vizit.v10.ui.components.dashedBorder
import hu.rayworks.vizit.v10.ui.icons.VIcons
import hu.rayworks.vizit.v10.ui.theme.V
import hu.rayworks.vizit.v10.ui.wizard.ImageResult
import hu.rayworks.vizit.v10.ui.wizard.loadImage
import kotlinx.coroutines.launch

const val NOT_IN_PROTOTYPE = "Ez a képernyő marad a mostani"

/** NFC-megosztás kapcsoló sor (B: menü, A: Továbbiak › Megosztás). */
@Composable
fun NfcSwitchRow(app: AppState, withStatus: Boolean = false, divider: Boolean = false) {
    val status = when {
        !withStatus -> null
        app.nfcDevice == NfcDevice.Unsupported -> "A telefonban nincs NFC"
        app.nfcDevice == NfcDevice.SystemOff -> "Kapcsold be a telefon beállításaiban"
        else -> "Készen áll a közvetlen átadásra"
    }
    ListRow(
        "NFC-megosztás",
        icon = VIcons.nfc,
        sub = status,
        divider = divider,
        enabled = app.nfcDevice != NfcDevice.Unsupported,
        trailing = { VSwitch(app.nfcOn, if (app.nfcDevice != NfcDevice.Unsupported) { on -> app.setNfc(on) } else null) },
    ) { app.setNfc(!app.nfcOn) }
}

/** A szinkron állapota kapszulában (menü fejléce). */
@Composable
fun SyncPill(app: AppState) {
    when {
        app.offline -> StatusPill("Offline", Tone.Info)
        app.sync == SyncStatus.Synced -> StatusPill("Szinkronizálva", Tone.Success)
        app.sync == SyncStatus.Pending -> StatusPill("Szinkronra vár", Tone.Info)
        app.sync == SyncStatus.Syncing -> StatusPill("Szinkronizálás…", Tone.Info)
        app.sync == SyncStatus.Retry -> StatusPill("Újrapróbálásra vár", Tone.Warn)
        app.sync == SyncStatus.Conflict -> StatusPill("Ütközés", Tone.Error)
        else -> StatusPill("Csak ezen a telefonon", Tone.Info)
    }
}

@Composable
private fun SheetColumn(content: @Composable () -> Unit) {
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) { content() }
}

/* ================================================================== Menü (B) */

/** Menü lap (B elrendezés, a profilképre koppintva): minden, ami nem fér a főképernyőre. */
@Composable
fun MenuSheet(app: AppState) {
    val actions = LocalActions.current
    SheetOverlay(visible = app.sheet == SheetKind.Menu, onDismiss = { app.sheet = null }) {
        SheetColumn {
            SheetGrab()
            val p = app.focus
            Row(
                Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProfileAvatar(p, 48.dp, V.blueSoft, V.blue)
                Column(Modifier.weight(1f)) {
                    Text(if (app.productionMode) app.accountName else p.name, fontSize = 17.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val email = if (app.productionMode) app.accountEmail else p.email
                    if (email.isNotBlank()) Text(email, fontSize = 13.sp, color = V.sub, maxLines = 1)
                }
                SyncPill(app)
            }
            GroupCard {
                ListRow("Profiljaid", icon = VIcons.stack, sub = "${app.profiles.size} profil") { app.sheet = SheetKind.Profiles }
            }
            GroupCard {
                ListRow("Profil szerkesztése", icon = VIcons.pencil, enabled = app.current != null) { app.openEdit(null) }
                ListRow("Adatok láthatósága", icon = VIcons.eye, value = "${p.visibleCount}/6", divider = true, enabled = app.current != null) {
                    app.openEdit("lathatosag")
                }
                ListRow("Kártya megjelenése", icon = VIcons.palette, value = p.preset.name, divider = true, enabled = app.current != null) {
                    app.push(Page.Appearance)
                }
            }
            GroupCard {
                ListRow("Névjegy beolvasása", icon = VIcons.scan) { app.push(Page.Scanner) }
                ListRow("Statisztikák", icon = VIcons.chart, sub = "Megtekintések, mentések és kattintások", divider = true) { app.push(Page.Analytics) }
                ListRow("Vállalkozói Portál", icon = VIcons.book, sub = "VOSZ, edukáció, digitális segítség", divider = true) { app.push(Page.Hub) }
            }
            GroupCard {
                ListRow("Online szerkesztő", icon = VIcons.globe, sub = "Színek, logó, közösségi linkek", trailing = { ExtMark() }) { actions.openUrl(EDITOR_URL) }
                ListRow("CRM", icon = VIcons.users, sub = "Partnerek, ügyletek, feladatok", divider = true, trailing = { ExtMark() }) { actions.openUrl(CRM_URL) }
            }
            if (app.nfcDevice != NfcDevice.Unsupported) GroupCard { NfcSwitchRow(app) }
            GroupCard {
                ListRow("Beállítások", icon = VIcons.sliders) { app.push(Page.Settings) }
                ListRow("Kijelentkezés", icon = VIcons.logout, danger = true, divider = true) { app.logout() }
            }
        }
    }
}

/* ================================================================== Profiljaid */

/** „Profiljaid” lap: minden profil egy sor, alul „Új profil” és az aktív profil törlése. */
@Composable
fun ProfilesSheet(app: AppState) {
    SheetOverlay(visible = app.sheet == SheetKind.Profiles, onDismiss = { app.sheet = null }) {
        SheetColumn {
            SheetGrab()
            Text(
                "Profiljaid",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
            )
            GroupCard {
                app.profiles.forEachIndexed { i, pf ->
                    val active = i == app.focusIndex
                    ListRow(
                        title = pf.label + (pf.tag?.let { " · " + it.lowercase() } ?: ""),
                        sub = pf.short,
                        divider = i > 0,
                        leading = {
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(V.chip),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(pf.color)
                                )
                            }
                        },
                        trailing = {
                            if (active) Icon(VIcons.check, contentDescription = "Aktív", tint = V.blue, modifier = Modifier.size(20.dp))
                            else Icon(VIcons.grip, contentDescription = null, tint = V.chev, modifier = Modifier.size(20.dp))
                        },
                    ) { app.pickFromSheet(i) }
                }
                ListRow("Új profil", icon = VIcons.plus, divider = true) { app.openWizard() }
            }
            GroupCard {
                ListRow("Aktív profil törlése", icon = VIcons.trash, danger = true, enabled = app.profiles.size > 1) { app.askDeleteActive() }
            }
        }
    }
}

/* ================================================================== Megosztás */

/** Megosztási lap: link, másolás, szöveg, névjegyfájl, QR – a kiválasztott profilhoz. */
@Composable
fun ShareSheet(app: AppState) {
    val actions = LocalActions.current
    SheetOverlay(visible = app.sheet == SheetKind.Share, onDismiss = { app.sheet = null }) {
        SheetColumn {
            SheetGrab()
            val p = app.focus
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProfileAvatar(p, 44.dp, V.blueSoft, V.blue)
                Column(Modifier.weight(1f)) {
                    Text(p.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(p.short, fontSize = 13.sp, color = V.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                ProfileLabel(p.label, p.color)
            }
            val link = app.profileQrAvailable(p)
            GroupCard {
                ListRow("Link megosztása", icon = VIcons.share, enabled = link) {
                    app.sheet = null
                    actions.shareLink(p)
                }
                ListRow("Link másolása", icon = VIcons.copy, divider = true, enabled = link) {
                    app.sheet = null
                    actions.copyLink(p)
                }
            }
            GroupCard {
                ListRow("Névjegy szövegként", icon = VIcons.textLines) {
                    app.sheet = null
                    actions.shareText(p)
                }
                ListRow("Névjegyfájl képpel", icon = VIcons.contact, sub = ".vcf profilképpel", divider = true, enabled = p.photo != null) {
                    app.sheet = null
                    actions.shareContact(p)
                }
                ListRow("QR-kód", icon = VIcons.qr, divider = true) { app.openFullQr(app.focusIndex) }
            }
        }
    }
}

/* ================================================================== Profil szerkesztése */

/** Kép sor a szerkesztőben (profilkép kerek, logó szögletes). */
@Composable
private fun PicRow(title: String, pic: Pic?, round: Boolean, initials: String, links: List<Pair<String, () -> Unit>>, onPick: () -> Unit) {
    val shape = if (round) CircleShape else RoundedCornerShape(14.dp)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            Modifier
                .size(56.dp)
                .clip(shape)
                .then(if (pic == null) Modifier.background(V.emptyBg).dashedBorder(1.5.dp, V.chev, if (round) 28.dp else 14.dp, 4.dp, 4.dp) else Modifier.background(V.blueSoft))
                .clickable(onClick = onPick),
            contentAlignment = Alignment.Center,
        ) {
            when {
                pic != null -> PicImage(pic, Modifier.fillMaxSize(), if (round) ContentScale.Crop else ContentScale.Fit)
                round && initials.isNotEmpty() -> Text(initials, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = V.sub)
                round -> Icon(VIcons.camera, contentDescription = null, tint = V.sub, modifier = Modifier.size(22.dp))
                else -> Icon(VIcons.plus, contentDescription = null, tint = V.sub, modifier = Modifier.size(22.dp))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                links.forEach { (t, onClick) ->
                    Text(
                        t, color = V.blue, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onClick)
                            .padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}

private fun visibilityOf(p: Profile, key: String): String = when {
    !p.hasValue(key) -> "Még nincs kitöltve"
    p.vis[key] == false -> "Senki"
    p.isPublic -> "Mindenki"
    else -> "Csak akivel megosztod"
}

/** Az éles mentés ellenőrzései, az első hibát adja vissza. */
private fun validate(p: Profile): String? = when {
    p.name.isBlank() -> "Add meg a nevedet a névjegyben."
    p.email.isNotBlank() && !p.email.contains('@') -> "Az e-mail-cím formátuma nem megfelelő."
    p.isPublic && p.slug.isNotEmpty() && !Regex("^[a-z0-9](?:[a-z0-9-]{1,48})[a-z0-9]$").matches(p.slug) ->
        "A profilazonosító 3–50 kisbetűből, számból vagy kötőjelből állhat."
    p.customDomain.isNotBlank() && !Regex("^[a-z0-9.-]{4,253}$").matches(p.customDomain.trim().lowercase()) || (p.customDomain.isNotBlank() && !p.customDomain.contains('.')) ->
        "Az egyedi domain csak teljes domainnév lehet (nevjegy.cegem.hu)."
    else -> null
}

/**
 * Profil szerkesztése (magas lap): képek, adatok, elérhetőségek, közösségi profilok, nyilvános
 * profil (profilcím, egyedi domain), „Mi látszik a profilon” és a kártya megjelenése.
 * „Adatok” / „Mi látszik” sorról nyitva az adott részhez görget.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditSheet(app: AppState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pickKind by remember { mutableStateOf("photo") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val kind = pickKind
            scope.launch {
                when (val r = loadImage(context, uri, if (kind == "photo") 640 else 320)) {
                    is ImageResult.Ok -> app.updateFocus { if (kind == "photo") it.copy(photo = r.pic) else it.copy(logo = r.pic) }
                    is ImageResult.Err -> app.toast(r.message)
                }
            }
        }
    }
    fun pick(kind: String) {
        pickKind = kind
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    SheetOverlay(visible = app.sheet == SheetKind.Edit, onDismiss = { app.sheet = null }, tall = true) {
        val pf = app.focus
        fun upd(change: (Profile) -> Profile) = app.updateFocus(change)
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconCircleButton(VIcons.close, "Bezárás", size = 40.dp) { app.sheet = null }
            Text("Profil szerkesztése", fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f).padding(start = 4.dp))
            Text(
                "Mentés",
                color = V.blue,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        val issue = validate(app.focus)
                        if (issue != null) {
                            app.toast(issue)
                        } else {
                            app.saveFocus()
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(V.line)
        )

        val scroll = rememberScrollState()
        val density = LocalDensity.current
        var adatokY by remember { mutableStateOf<Float?>(null) }
        var lathatosagY by remember { mutableStateOf<Float?>(null) }
        val target = when (app.editSection) {
            "adatok" -> adatokY
            "lathatosag" -> lathatosagY
            else -> null
        }
        var scrolled by remember { mutableStateOf(false) }
        LaunchedEffect(target) {
            if (target != null && !scrolled) {
                scrolled = true
                scroll.scrollTo((target + with(density) { 4.dp.toPx() }).toInt().coerceAtLeast(0))
            }
        }

        Column(
            Modifier
                .weight(1f)
                .imePadding()
                .verticalScroll(scroll)
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.fillMaxWidth().padding(bottom = 2.dp), contentAlignment = Alignment.Center) { ProfileLabel(pf.label, pf.color) }

            GroupCard {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    PicRow(
                        "Profilkép", pf.photo, round = true, initials = pf.initials,
                        links = if (pf.photo != null) listOf("Csere" to { pick("photo") }, "Törlés" to { upd { it.copy(photo = null) } })
                        else listOf("Mostani kép" to {
                            val currentPhoto = if (app.productionMode) app.profiles.firstOrNull { it.photo != null }?.photo else Pic.Res(R.drawable.avatar)
                            if (currentPhoto != null) upd { it.copy(photo = currentPhoto) } else pick("photo")
                        }, "Feltöltés" to { pick("photo") }),
                    ) { pick("photo") }
                    PicRow(
                        "Céges logó", pf.logo, round = false, initials = "",
                        links = if (pf.logo != null) listOf("Csere" to { pick("logo") }, "Törlés" to { upd { it.copy(logo = null) } })
                        else listOf("Feltöltés" to { pick("logo") }, "Minta" to { upd { it.copy(logo = Pic.Res(R.drawable.sample_logo)) } }),
                    ) { pick("logo") }
                }
            }

            GroupLabel("Adatok", Modifier.onGloballyPositioned { adatokY = it.positionInParent().y })
            Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedField("Név", pf.name) { v -> upd { it.copy(name = v) } }
                OutlinedField("Beosztás", pf.title) { v -> upd { it.copy(title = v) } }
                OutlinedField("Cég", pf.company) { v -> upd { it.copy(company = v) } }
                OutlinedField("Bemutatkozás", pf.bio, singleLine = false, supporting = "${pf.bio.length}/420", imeAction = ImeAction.Default) { v ->
                    upd { it.copy(bio = v.take(420)) }
                }
            }
            GroupLabel("Elérhetőségek")
            Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedField("Telefon", pf.phone, keyboardType = KeyboardType.Phone) { v -> upd { it.copy(phone = v) } }
                OutlinedField("E-mail", pf.email, keyboardType = KeyboardType.Email) { v -> upd { it.copy(email = v) } }
                OutlinedField("Weboldal", pf.web, keyboardType = KeyboardType.Uri) { v -> upd { it.copy(web = v) } }
                OutlinedField("Cím", pf.address) { v -> upd { it.copy(address = v) } }
            }

            GroupLabel("Közösségi profilok")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SOCIALS.forEach { s ->
                    val on = pf.socials.containsKey(s.id)
                    FilterChip(s.label, on) {
                        upd { if (on) it.copy(socials = it.socials - s.id) else it.copy(socials = it.socials + (s.id to "")) }
                    }
                }
            }
            if (pf.socials.isNotEmpty()) {
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SOCIALS.filter { pf.socials.containsKey(it.id) }.forEach { s ->
                        OutlinedField(s.label, pf.socials[s.id] ?: "", keyboardType = KeyboardType.Uri, placeholder = s.placeholder) { v ->
                            upd { it.copy(socials = it.socials + (s.id to v)) }
                        }
                    }
                }
            }

            GroupLabel("Nyilvános profil")
            GroupCard {
                ListRow("Publikus VIZIT profil", icon = VIcons.globe, trailing = { VSwitch(pf.isPublic) { v -> upd { it.copy(isPublic = v) } } }) {
                    upd { it.copy(isPublic = !it.isPublic) }
                }
            }
            if (pf.isPublic) {
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedField(
                        "Profilcím", pf.slug, keyboardType = KeyboardType.Uri, prefix = "vizitkartyam.hu/",
                        supporting = if (pf.slug.isEmpty()) "Első mentéskor a nevedből készül" else "Kisbetű, szám, kötőjel",
                    ) { v -> upd { it.copy(slug = v.trim().lowercase().replace(Regex("\\s+"), "-")) } }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedField("Egyedi domain", pf.customDomain, keyboardType = KeyboardType.Uri, placeholder = "nevjegy.cegem.hu", imeAction = ImeAction.Done) { v ->
                            upd { it.copy(customDomain = v, domainVerified = false) }
                        }
                        if (pf.customDomain.isNotBlank()) {
                            Row(Modifier.padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(if (pf.domainVerified) VIcons.checkCircle else VIcons.timer, contentDescription = null, tint = if (pf.domainVerified) V.green else V.warn, modifier = Modifier.size(16.dp))
                                Text(
                                    if (pf.domainVerified) "Ellenőrzött domain · a QR és az NFC ezt használja" else "Beállításra vár · addig a VIZIT-cím marad",
                                    fontSize = 12.5.sp, color = if (pf.domainVerified) V.green else V.warn,
                                )
                            }
                        }
                    }
                }
            }

            GroupLabel(
                "Mi látszik a profilon",
                Modifier.onGloballyPositioned { lathatosagY = it.positionInParent().y },
                trailing = "${pf.visibleCount}/6",
            )
            GroupCard {
                ListRow("Teljes név", value = "Kötelező")
                VIS_FIELDS.forEach { (key, label) ->
                    val filled = pf.hasValue(key)
                    val checked = pf.vis[key] != false
                    ListRow(
                        label,
                        sub = visibilityOf(pf, key),
                        divider = true,
                        enabled = filled,
                        trailing = { VSwitch(checked, if (filled) { v -> upd { it.copy(vis = it.vis + (key to v)) } } else null) },
                    ) { upd { it.copy(vis = it.vis + (key to !checked)) } }
                }
            }

            GroupLabel("Megjelenés")
            GroupCard {
                ListRow("Kártya megjelenése", icon = VIcons.palette, value = pf.preset.name) { app.push(Page.Appearance) }
            }
        }
    }
}

/* ================================================================== Prototípus */

/** Prototípus lap (hosszan nyomva a VIZIT feliratot): elrendezés, állapotok, belépési képernyők. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrototypeSheet(app: AppState) {
    SheetOverlay(visible = app.sheet == SheetKind.Prototype, onDismiss = { app.sheet = null }) {
        SheetColumn {
            SheetGrab()
            Text("Prototípus", fontSize = 18.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            GroupLabel("Elrendezés")
            Segmented(listOf("B · egy képernyő", "A · fülek"), if (app.layout == DesignLayout.B) 0 else 1) {
                app.switchLayout(if (it == 0) DesignLayout.B else DesignLayout.A)
            }
            GroupLabel("Szinkron")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SyncStatus.entries.forEach { s -> FilterChip(s.label, app.sync == s) { app.sync = s } }
            }
            GroupLabel("NFC")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NfcDevice.entries.forEach { d -> FilterChip(d.label, app.nfcDevice == d) { app.nfcDevice = d } }
            }
            GroupCard {
                ListRow("Offline mód", icon = VIcons.cloudOff, trailing = { VSwitch(app.offline) { app.offline = it } }) { app.offline = !app.offline }
                ListRow("Kameraengedély", icon = VIcons.camera, divider = true, trailing = { VSwitch(app.cameraAllowed) { app.cameraAllowed = it } }) {
                    app.cameraAllowed = !app.cameraAllowed
                }
                ListRow("Jogi elfogadás belépéskor", icon = VIcons.doc, divider = true, trailing = { VSwitch(app.legalRequired) { app.legalRequired = it } }) {
                    app.legalRequired = !app.legalRequired
                }
            }
            GroupLabel("Képernyők")
            GroupCard {
                val go = { g: Gate ->
                    app.sheet = null
                    app.authBanner = null
                    app.gate = g
                }
                ListRow("Bejelentkezés", icon = VIcons.lock) { go(Gate.Auth(AuthMode.Login)) }
                ListRow("Regisztráció", icon = VIcons.person, divider = true) { go(Gate.Auth(AuthMode.Register)) }
                ListRow("Új jelszó", icon = VIcons.lock, divider = true) { go(Gate.Auth(AuthMode.NewPassword)) }
                ListRow("Jogi elfogadás", icon = VIcons.doc, divider = true) { go(Gate.Auth(AuthMode.Legal)) }
                ListRow("Betöltés", icon = VIcons.sync, divider = true) { go(Gate.Loading) }
                ListRow("A névjegy nem tölthető be", icon = VIcons.warning, divider = true) { go(Gate.ProfileError) }
                ListRow("A munkamenet lejárt", icon = VIcons.timer, divider = true) { go(Gate.SessionExpired) }
                ListRow("Feltételek ellenőrzése sikertelen", icon = VIcons.shield, divider = true) { go(Gate.LegalFailed) }
            }
        }
    }
}
