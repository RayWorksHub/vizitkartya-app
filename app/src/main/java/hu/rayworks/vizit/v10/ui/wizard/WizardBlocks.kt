package hu.rayworks.vizit.v10.ui.wizard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import hu.rayworks.vizit.R
import hu.rayworks.vizit.v10.data.PRESETS
import hu.rayworks.vizit.v10.data.Pic
import hu.rayworks.vizit.v10.data.SOCIALS
import hu.rayworks.vizit.v10.ui.components.PrimaryButton
import hu.rayworks.vizit.v10.ui.components.SmallOutlineButton
import hu.rayworks.vizit.v10.ui.components.VSwitch
import hu.rayworks.vizit.v10.ui.components.cssLinearGradient
import hu.rayworks.vizit.v10.ui.components.dashedBorder
import hu.rayworks.vizit.v10.ui.components.noRippleClickable
import hu.rayworks.vizit.v10.ui.icons.VIcons
import hu.rayworks.vizit.v10.ui.theme.V

const val EMAIL_ERROR = "Ez nem tűnik érvényes e-mail-címnek."

/* ================================================================== blokk kártya */

/**
 * Egy blokk (.blk) a lapozóban: színes fejléc, alatta vagy a mezők (aktív / kész / újranyitott),
 * vagy az előnézet (még nem / kihagyva). Aktív blokk alján a gomb; billentyűzet közben tömör nézet.
 */
@Composable
fun BlockPage(c: WizardController, b: WBlock, n: Int, kb: Boolean) {
    val wiz = c.wiz
    val s = wiz.status(b.id)
    val shape = RoundedCornerShape(28.dp)
    val alpha by animateFloatAsState(if (s == BlockStatus.Todo) 0.7f else 1f, tween(250), label = "blkAlpha")
    val open = s == BlockStatus.Active || s == BlockStatus.Done || s == BlockStatus.Open
    Column(
        Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
            .clip(shape)
            .background(V.surface)
            .then(
                when (s) {
                    BlockStatus.Active -> Modifier.border(2.dp, b.color.c, shape)
                    BlockStatus.Todo -> Modifier.dashedBorder(1.dp, V.line, 28.dp)
                    else -> Modifier.border(1.dp, V.line, shape)
                }
            )
    ) {
        BlockHead(b, n, s, kb)
        if (open) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(if (kb) PaddingValues(horizontal = 14.dp, vertical = 12.dp) else PaddingValues(16.dp)),
                verticalArrangement = Arrangement.spacedBy(if (kb) 12.dp else 14.dp),
            ) {
                when (b.id) {
                    "personal" -> PersonalBody(c, b, kb)
                    "company" -> CompanyBody(c, b, kb)
                    "online" -> OnlineBody(c, b)
                    "done" -> DoneBody(c, b)
                }
            }
            if (s == BlockStatus.Active && !kb) BlockActions(c, b)
        } else {
            BlockTeaser(c, b, s, Modifier.weight(1f))
        }
    }
}

/** .blk-head: fehér ikon kör, „1. BLOKK”, cím, állapot jelvény, halvány nagy sorszám a sarokban. */
@Composable
private fun BlockHead(b: WBlock, n: Int, s: BlockStatus, kb: Boolean) {
    val todo = s == BlockStatus.Todo
    val grey = todo || s == BlockStatus.Skip
    val accent = if (todo) V.sub else b.color.c
    Box(
        Modifier
            .fillMaxWidth()
            .background(if (grey) V.chip else b.color.soft)
            .clipToBounds()
    ) {
        if (!kb) {
            Text(
                "0${n + 1}",
                fontSize = 76.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.05).em,
                color = accent.copy(alpha = 0.12f),
                style = TextStyle(
                    lineHeight = 76.sp,
                    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
                ),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-8).dp, y = 12.dp),
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(
                    if (kb) PaddingValues(horizontal = 14.dp, vertical = 9.dp)
                    else PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp)
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val icShape = if (kb) RoundedCornerShape(10.dp) else CircleShape
            Box(
                Modifier
                    .size(if (kb) 32.dp else 42.dp)
                    .shadow(1.dp, icShape)
                    .clip(icShape)
                    .background(V.surface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(b.icon(), contentDescription = null, tint = accent, modifier = Modifier.size(if (kb) 18.dp else 22.dp))
            }
            Column(Modifier.weight(1f)) {
                if (!kb) {
                    Text(
                        "${n + 1}. BLOKK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.04.em,
                        color = accent,
                        maxLines = 1,
                    )
                }
                Text(
                    b.title,
                    fontSize = if (kb) 16.sp else 20.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = if (kb) 19.sp else 24.sp,
                    modifier = Modifier.padding(top = if (kb) 0.dp else 2.dp),
                )
            }
            if (!kb) StatusBadge(s, b.color.c)
        }
    }
}

@Composable
private fun StatusBadge(s: BlockStatus, color: Color) {
    val (bg, fg, text) = when (s) {
        BlockStatus.Done -> Triple(V.surface, color, "Kész")
        BlockStatus.Skip -> Triple(V.surface, V.sub, "Kihagyva")
        BlockStatus.Active -> Triple(color, V.onAccent, "Most")
        else -> return
    }
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (s == BlockStatus.Done) Icon(VIcons.checkBold, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = fg)
    }
}

/** .blk-teaser: nagy ikon és a blokk tartalmának címkéi; kihagyott blokknál „Mégis kitöltöm”. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BlockTeaser(c: WizardController, b: WBlock, s: BlockStatus, modifier: Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Box(
            Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(V.chip),
            contentAlignment = Alignment.Center,
        ) {
            Icon(b.bigIcon(), contentDescription = null, tint = V.sub, modifier = Modifier.size(40.dp))
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            b.items.forEach { item ->
                Text(
                    item,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = V.sub,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(V.chip)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
        if (s == BlockStatus.Skip) SmallOutlineButton("Mégis kitöltöm") { c.wiz.reopen(b.id) }
    }
}

/** .blk-actions: vonal fölötte, „Tovább · következő blokk ⌄” / „Kihagyom” / „Publikálás”. */
@Composable
private fun BlockActions(c: WizardController, b: WBlock) {
    val wiz = c.wiz
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(V.line)
        )
        Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)) {
            if (b.id == "done") {
                PrimaryButton(wiz.actionLabel(b), enabled = wiz.valid(b.id)) { c.action(b) }
            } else {
                PrimaryButton(
                    wiz.actionLabel(b),
                    secondaryText = wiz.nextOf(b.id)?.title,
                    trailingIcon = VIcons.chevDown,
                    soft = wiz.isSkip(b),
                    enabled = wiz.valid(b.id),
                ) { c.action(b) }
            }
        }
    }
}

/* ================================================================== blokkok tartalma */

@Composable
private fun ColumnScope.PersonalBody(c: WizardController, b: WBlock, kb: Boolean) {
    val wiz = c.wiz
    if (!kb) Media(c, "photo")
    WField(c, b, "name", "Név", required = true, capitalization = KeyboardCapitalization.Words, error = wiz.errors["name"])
    WField(c, b, "phone", "Telefon", placeholder = "+36 30 123 4567", keyboardType = KeyboardType.Phone)
}

@Composable
private fun ColumnScope.CompanyBody(c: WizardController, b: WBlock, kb: Boolean) {
    if (!kb) Media(c, "logo")
    WField(c, b, "company", "Cégnév", required = true, capitalization = KeyboardCapitalization.Words)
    WField(c, b, "role", "Beosztás")
    WField(c, b, "place", "Hely", capitalization = KeyboardCapitalization.Words)
    WField(c, b, "bio", "Bemutatkozás", placeholder = "Mivel foglalkozol?", multiline = true, counter = "${c.wiz.bio.length}/420")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.OnlineBody(c: WizardController, b: WBlock) {
    val wiz = c.wiz
    WField(
        c, b, "email", "E-mail",
        placeholder = "nev@ceg.hu",
        keyboardType = KeyboardType.Email,
        capitalization = KeyboardCapitalization.None,
        error = wiz.errors["email"],
        onBlur = {
            if (wiz.emailOk(wiz.email)) wiz.errors.remove("email") else wiz.errors["email"] = EMAIL_ERROR
        },
    )
    WField(c, b, "web", "Weboldal", placeholder = "ceg.hu", keyboardType = KeyboardType.Uri, capitalization = KeyboardCapitalization.None)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Közösségi profilok", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = V.sub)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SOCIALS.forEach { s ->
                val on = s.id in wiz.socialOrder
                val shape = RoundedCornerShape(8.dp)
                Row(
                    Modifier
                        .height(34.dp)
                        .clip(shape)
                        .then(if (on) Modifier.background(V.blueSoft) else Modifier.border(1.dp, V.outline, shape))
                        .clickable { wiz.toggleSocial(s.id) }
                        .padding(start = 10.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(if (on) VIcons.check else VIcons.plus, contentDescription = null, tint = if (on) V.blue else V.ink, modifier = Modifier.size(16.dp))
                    Text(s.label, fontSize = 14.sp, color = if (on) V.blue else V.ink, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
    wiz.socialOrder.forEach { id ->
        val s = SOCIALS.first { it.id == id }
        WField(c, b, "s-$id", s.label, placeholder = s.placeholder, keyboardType = KeyboardType.Uri, capitalization = KeyboardCapitalization.None)
    }
}

@Composable
private fun ColumnScope.DoneBody(c: WizardController, b: WBlock) {
    val wiz = c.wiz
    val st = wiz.slugState()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        WField(
            c, b, "slug", "Profil címe",
            prefix = "vizitkartyam.hu/",
            keyboardType = KeyboardType.Uri,
            capitalization = KeyboardCapitalization.None,
            invalid = !st.ok,
            imeAction = ImeAction.Done,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                st.msg,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (st.ok) V.green else V.red,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (st.ok) V.greenSoft else V.redSoft)
                    .padding(horizontal = 9.dp, vertical = 4.dp),
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Szín", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = V.sub)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PRESETS.take(4).forEach { pr ->
                val checked = wiz.preset == pr.id
                Box(
                    Modifier
                        .size(36.dp)
                        .drawBehind {
                            val r = size.minDimension / 2f
                            if (checked) {
                                drawCircle(V.blue, radius = r + 4.dp.toPx())
                                drawCircle(V.surface, radius = r + 2.dp.toPx())
                            }
                            drawCircle(cssLinearGradient(145f, size, 0f to pr.c1, 1f to pr.c3), radius = r)
                            drawCircle(Color.White.copy(alpha = 0.18f), radius = r - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
                        }
                        .clip(CircleShape)
                        .clickable { wiz.preset = pr.id }
                )
            }
            Text(
                PRESETS.first { it.id == wiz.preset }.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = V.sub,
                modifier = Modifier.padding(start = 2.dp),
            )
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .noRippleClickable { wiz.isPublic = !wiz.isPublic },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Nyilvános profil", fontSize = 15.sp, fontWeight = FontWeight.Medium)
        VSwitch(wiz.isPublic) { wiz.isPublic = it }
    }
}

/* ================================================================== mezők */

/**
 * .w-field: címke fölötte, körvonalas mező (fókuszban kék, hibánál piros, 2 dp), alatta hibaszöveg vagy számláló.
 * Az Enter („Tovább” a billentyűzeten) a blokk következő mezőjére ugrik, az utolsón megnyomja a blokk gombját.
 */
@Composable
private fun WField(
    c: WizardController,
    block: WBlock,
    key: String,
    label: String,
    required: Boolean = false,
    placeholder: String = "",
    prefix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    multiline: Boolean = false,
    error: String? = null,
    invalid: Boolean = false,
    counter: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    onBlur: (() -> Unit)? = null,
) {
    val wiz = c.wiz
    val value = wiz.value(key)
    val requester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }

    LaunchedEffect(wiz.focusRequest) {
        if (wiz.focusRequest == key) {
            withFrameNanos { }
            runCatching { requester.requestFocus() }
            wiz.focusRequest = null
        }
    }
    DisposableEffect(key) {
        onDispose {
            if (wiz.focusedKey == key) {
                wiz.focusedKey = null
                wiz.focusedBlock = null
            }
        }
    }

    val bad = error != null || invalid
    val borderW = if (bad || focused) 2.dp else 1.dp
    val borderC = when {
        bad -> V.red
        focused -> V.blue
        else -> V.outline
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(if (required) "$label *" else label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = V.sub)
        BasicTextField(
            value = value,
            onValueChange = { wiz.input(key, it) },
            singleLine = !multiline,
            minLines = if (multiline) 3 else 1,
            textStyle = TextStyle(fontSize = 16.sp, lineHeight = 21.6.sp, color = V.ink),
            cursorBrush = SolidColor(V.blue),
            keyboardOptions = KeyboardOptions(
                capitalization = capitalization,
                keyboardType = keyboardType,
                imeAction = if (multiline) ImeAction.Default else imeAction,
            ),
            keyboardActions = KeyboardActions(
                onNext = { c.imeNext(block, key) },
                onDone = { c.imeNext(block, key) },
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(requester)
                .onFocusChanged { st ->
                    if (st.isFocused) {
                        focused = true
                        wiz.focusedKey = key
                        wiz.focusedBlock = block.id
                    } else if (focused) {
                        focused = false
                        if (wiz.focusedKey == key) {
                            wiz.focusedKey = null
                            wiz.focusedBlock = null
                        }
                        onBlur?.invoke()
                    }
                },
            decorationBox = { inner ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = if (multiline) 112.dp else 48.dp)
                        .border(borderW, borderC, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = if (multiline) Alignment.Top else Alignment.CenterVertically,
                ) {
                    if (prefix != null) {
                        Text(prefix, fontSize = 14.sp, color = V.sub, maxLines = 1)
                        Spacer(Modifier.width(1.dp))
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .padding(vertical = 12.dp)
                    ) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(placeholder, fontSize = 16.sp, color = V.placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        inner()
                    }
                }
            },
        )
        if (error != null) Text(error, fontSize = 12.5.sp, color = V.red)
        if (counter != null) {
            Text(counter, fontSize = 12.5.sp, color = V.sub, modifier = Modifier.align(Alignment.End))
        }
    }
}

/** .w-media: kerek profilkép / szögletes logó helye, mellette „Mostani kép · Feltöltés” vagy „Csere · Törlés”. */
@Composable
private fun Media(c: WizardController, kind: String) {
    val wiz = c.wiz
    val photo = kind == "photo"
    val pic = if (photo) wiz.photo else wiz.logo
    val shape = if (photo) CircleShape else RoundedCornerShape(16.dp)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(shape)
                    .then(
                        if (pic == null) Modifier.background(V.emptyBg).dashedBorder(1.5.dp, V.chev, if (photo) 32.dp else 16.dp, dash = 4.dp, gap = 4.dp)
                        else Modifier.background(if (photo) V.blueSoft else Color.White)
                    )
                    .clickable { c.pickImage(kind) },
                contentAlignment = Alignment.Center,
            ) {
                val initials = wiz.initials()
                when {
                    pic != null -> PicImage(pic, if (photo) ContentScale.Crop else ContentScale.Fit)
                    photo && initials.isNotEmpty() -> Text(initials, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = V.sub)
                    photo -> Icon(VIcons.camera, contentDescription = null, tint = V.sub, modifier = Modifier.size(24.dp))
                    else -> Icon(VIcons.plus, contentDescription = null, tint = V.sub, modifier = Modifier.size(24.dp))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(if (photo) "Profilkép" else "Céges logó", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val links: List<Pair<String, () -> Unit>> = when {
                        pic != null -> listOf("Csere" to { c.pickImage(kind) }, "Törlés" to { wiz.setPic(kind, null) })
                        photo -> listOf("Mostani kép" to { wiz.setPic(kind, Pic.Res(R.drawable.avatar)) }, "Feltöltés" to { c.pickImage(kind) })
                        else -> listOf("Feltöltés" to { c.pickImage(kind) }, "Minta" to { wiz.setPic(kind, Pic.Res(R.drawable.sample_logo)) })
                    }
                    links.forEach { (t, onClick) ->
                        Text(
                            t,
                            color = V.blue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable(onClick = onClick)
                                .padding(vertical = 4.dp),
                        )
                    }
                }
            }
        }
        wiz.errors[kind]?.let { Text(it, fontSize = 12.5.sp, color = V.red) }
    }
}

@Composable
private fun PicImage(pic: Pic, scale: ContentScale) {
    when (pic) {
        is Pic.Res -> Image(painterResource(pic.id), contentDescription = null, contentScale = scale, modifier = Modifier.fillMaxSize())
        is Pic.Bmp -> Image(pic.bitmap, contentDescription = null, contentScale = scale, modifier = Modifier.fillMaxSize())
    }
}
