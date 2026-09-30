package hu.rayworks.vizit.v10.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.rayworks.vizit.v10.data.AppState
import hu.rayworks.vizit.v10.ui.icons.VIcons
import hu.rayworks.vizit.v10.ui.theme.V

/* ================================================================== oldalváz */

/**
 * Teljes képernyős oldal (Beállítások, Statisztikák, Portál…): vissza nyíl, cím, görgethető tartalom.
 * A tartalom 16 dp oldalmargóval, 14 dp térközzel áll, mint a főképernyő.
 */
@Composable
fun PageScaffold(
    title: String?,
    onBack: () -> Unit,
    scroll: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(V.bg)
            .noRippleClickable { }
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp)
                .padding(start = 6.dp, end = 10.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconCircleButton(VIcons.back, "Vissza", tint = V.ink, onClick = onBack)
            if (title != null) {
                Text(
                    title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 6.dp),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            actions()
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

/* ================================================================== visszajelzések */

enum class Tone { Info, Success, Warn, Error }

private fun toneColors(t: Tone): Pair<Color, Color> = when (t) {
    Tone.Info -> V.blueSoft to V.blue
    Tone.Success -> V.greenSoft to V.green
    Tone.Warn -> V.warnSoft to V.warn
    Tone.Error -> V.redSoft to V.red
}

private fun toneIcon(t: Tone): ImageVector = when (t) {
    Tone.Info -> VIcons.info
    Tone.Success -> VIcons.checkCircle
    Tone.Warn -> VIcons.warning
    Tone.Error -> VIcons.warning
}

/** Teljes szélességű üzenetsáv ikonnal, opcionális művelettel a jobb oldalon. */
@Composable
fun Banner(text: String, tone: Tone, modifier: Modifier = Modifier, icon: ImageVector? = null, action: String? = null, onAction: (() -> Unit)? = null) {
    val (bg, fg) = toneColors(tone)
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .then(if (onAction != null) Modifier.clickable(onClick = onAction) else Modifier)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon ?: toneIcon(tone), contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        Text(text, fontSize = 14.sp, color = V.ink, modifier = Modifier.weight(1f))
        if (action != null) Text(action, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = fg)
    }
}

/** Kis állapotjelző kapszula pöttyel (Szinkronizálva, Ütközés…). */
@Composable
fun StatusPill(text: String, tone: Tone) {
    val (bg, fg) = toneColors(tone)
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(fg)
        )
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = fg, maxLines = 1)
    }
}

/** Üres / hiba állapot: nagy ikon, cím, üzenet, opcionális gomb. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String? = null,
    tone: Tone = Tone.Info,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val (bg, fg) = toneColors(tone)
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(30.dp))
        }
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        if (message != null) {
            Text(message, fontSize = 14.sp, color = V.sub, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 320.dp))
        }
        if (action != null && onAction != null) {
            Spacer(Modifier.height(2.dp))
            SmallOutlineButton(action, onAction)
        }
    }
}

@Composable
fun Spinner(size: Dp = 28.dp, color: Color = V.blue) {
    CircularProgressIndicator(modifier = Modifier.size(size), color = color, strokeWidth = 3.dp)
}

/* ================================================================== vezérlők */

/** Material 3 szegmentált gomb: a kijelölt rész tónusos, pipával. */
@Composable
fun Segmented(options: List<String>, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(shape)
            .border(1.dp, V.outline, shape)
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            val bg by animateColorAsState(if (on) V.container else Color.Transparent, tween(200), label = "seg")
            if (i > 0) {
                Box(
                    Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(V.outline)
                )
            }
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(bg)
                    .clickable { onSelect(i) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (on) {
                    Icon(VIcons.check, contentDescription = null, tint = V.onContainer, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (on) V.onContainer else V.ink, maxLines = 1)
            }
        }
    }
}

/** Material 3 szűrő-csip (Mind, AI, Digitális munka…). */
@Composable
fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        Modifier
            .height(32.dp)
            .clip(shape)
            .then(if (selected) Modifier.background(V.container) else Modifier.border(1.dp, V.outline, shape))
            .clickable(onClick = onClick)
            .padding(start = if (selected) 8.dp else 12.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (selected) Icon(VIcons.check, contentDescription = null, tint = V.onContainer, modifier = Modifier.size(16.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (selected) V.onContainer else V.ink, maxLines = 1)
    }
}

/** Kék szöveges gomb (Elfelejtett jelszó, Vissza a bejelentkezéshez…). */
@Composable
fun TextLink(text: String, modifier: Modifier = Modifier, color: Color = V.blue, onClick: () -> Unit) {
    Text(
        text,
        color = color,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

/** Külső hivatkozás jele a sor végén. */
@Composable
fun ExtMark() {
    Icon(VIcons.ext, contentDescription = null, tint = V.sub, modifier = Modifier.size(18.dp))
}

/**
 * Androidos körvonalas beviteli mező: a címke a keret tetején ül, fókuszban 2 dp kék keret és kék címke.
 * Jelszónál szem ikon, hibánál piros keret és alatta a hibaüzenet.
 */
@Composable
fun OutlinedField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    placeholder: String = "",
    prefix: String? = null,
    password: Boolean = false,
    singleLine: Boolean = true,
    error: String? = null,
    supporting: String? = null,
    enabled: Boolean = true,
    labelBg: Color = V.bg,
    onValueChange: (String) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    var reveal by remember { mutableStateOf(false) }
    val bad = error != null
    val borderColor = when {
        bad -> V.red
        focused -> V.blue
        else -> V.outline
    }
    Column(modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.5f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.fillMaxWidth()) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = singleLine,
                minLines = if (singleLine) 1 else 3,
                textStyle = TextStyle(fontSize = 15.sp, color = V.ink),
                cursorBrush = SolidColor(V.blue),
                visualTransformation = if (password && !reveal) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboardType, imeAction = imeAction),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused },
                decorationBox = { inner ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = if (singleLine) 54.dp else 96.dp)
                            .border(if (focused || bad) 2.dp else 1.dp, borderColor, RoundedCornerShape(6.dp))
                            .padding(start = 14.dp, end = if (password) 4.dp else 14.dp),
                        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
                    ) {
                        if (prefix != null) Text(prefix, fontSize = 14.sp, color = V.sub, maxLines = 1)
                        Box(
                            Modifier
                                .weight(1f)
                                .padding(vertical = 14.dp)
                        ) {
                            if (value.isEmpty() && placeholder.isNotEmpty()) {
                                Text(placeholder, fontSize = 15.sp, color = V.placeholder, maxLines = 1)
                            }
                            inner()
                        }
                        if (password) {
                            IconCircleButton(
                                if (reveal) VIcons.eyeOff else VIcons.eye,
                                if (reveal) "Jelszó elrejtése" else "Jelszó megjelenítése",
                                size = 44.dp,
                                iconSize = 20.dp,
                                tint = V.sub,
                            ) { reveal = !reveal }
                        }
                    }
                },
            )
            Text(
                label,
                fontSize = 12.sp,
                color = when {
                    bad -> V.red
                    focused -> V.blue
                    else -> V.label
                },
                modifier = Modifier
                    .offset(x = 10.dp, y = (-9).dp)
                    .background(labelBg)
                    .padding(horizontal = 4.dp),
            )
        }
        if (error != null) {
            Text(error, fontSize = 12.5.sp, color = V.red, modifier = Modifier.padding(start = 14.dp))
        } else if (supporting != null) {
            Text(supporting, fontSize = 12.5.sp, color = V.sub, modifier = Modifier.padding(start = 14.dp))
        }
    }
}

/* ================================================================== párbeszédablak */

/**
 * Material 3 megerősítő ablak (Profil törlése?, Fiók törlése, Felhőváltozat…). A TÖRLÉS-típusú
 * megerősítésnél a gomb csak a pontos szó beírása után él.
 */
@Composable
fun DialogHost(app: AppState) {
    val spec = app.dialog
    var last by remember { mutableStateOf(spec) }
    if (spec != null) last = spec
    AnimatedVisibility(visible = spec != null, enter = fadeIn(tween(180)), exit = fadeOut(tween(180))) {
        Box(
            Modifier
                .fillMaxSize()
                .background(V.scrim)
                .noRippleClickable { if (last?.requireText == null) app.dialog = null },
            contentAlignment = Alignment.Center,
        ) {
            val d = last
            if (d != null) {
                var typed by remember(d) { mutableStateOf("") }
                var busy by remember(d) { mutableStateOf(false) }
                var err by remember(d) { mutableStateOf<String?>(null) }
                LaunchedEffect(busy) {
                    if (busy) {
                        kotlinx.coroutines.delay(900)
                        app.dialog = null
                        d.onConfirm()
                    }
                }
                run {
                    Column(
                        Modifier
                            .animateEnterExit(enter = scaleIn(tween(180), initialScale = 0.92f), exit = scaleOut(tween(120), targetScale = 0.96f))
                            .padding(horizontal = 28.dp)
                            .widthIn(max = 420.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .background(V.surface)
                            .noRippleClickable { }
                            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        if (d.icon != null) {
                            Icon(d.icon, contentDescription = null, tint = if (d.danger) V.red else V.blue, modifier = Modifier.size(26.dp).align(Alignment.CenterHorizontally))
                        }
                        Text(d.title, fontSize = 22.sp, fontWeight = FontWeight.Normal, lineHeight = 28.sp)
                        if (d.body != null) Text(d.body, fontSize = 14.sp, color = V.sub, lineHeight = 20.sp)
                        if (d.requireText != null) {
                            OutlinedField(
                                label = "Megerősítés",
                                value = typed,
                                supporting = "Írd be: ${d.requireText}",
                                error = err,
                                enabled = !busy,
                                imeAction = ImeAction.Done,
                                labelBg = V.surface,
                            ) {
                                typed = it
                                err = null
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (d.dismiss != null) {
                                TextLink(d.dismiss, color = if (busy) V.sub else V.blue) { if (!busy) app.dialog = null }
                            }
                            if (busy) {
                                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Spinner(18.dp, V.red)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Törlés folyamatban…", fontSize = 14.sp, color = V.red)
                                }
                            } else {
                                TextLink(d.confirm, color = if (d.danger) V.red else V.blue) {
                                    val need = d.requireText
                                    if (need != null) {
                                        if (typed.trim().uppercase() != need) {
                                            err = "A törlés megerősítéséhez írd be: $need"
                                        } else {
                                            busy = true
                                        }
                                    } else {
                                        app.dialog = null
                                        d.onConfirm()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
