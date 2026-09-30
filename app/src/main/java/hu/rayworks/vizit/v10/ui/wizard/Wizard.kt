package hu.rayworks.vizit.v10.ui.wizard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.rayworks.vizit.v10.data.AppState
import hu.rayworks.vizit.v10.ui.components.IconCircleButton
import hu.rayworks.vizit.v10.ui.components.PrimaryButton
import hu.rayworks.vizit.v10.ui.components.noRippleClickable
import hu.rayworks.vizit.v10.ui.icons.VIcons
import hu.rayworks.vizit.v10.ui.theme.SheetEasing
import hu.rayworks.vizit.v10.ui.theme.V
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** A felület műveletei, amelyekhez a varázsló állapotán kívül fókusz és képválasztó is kell. */
class WizardController(
    val app: AppState,
    val wiz: WizardState,
    private val focusManager: FocusManager,
    private val isTyping: () -> Boolean,
    val pickImage: (String) -> Unit,
) {
    /** A blokk gombja: „Tovább/Kihagyom” → next(), „Publikálás/Mentés” → publish(). */
    fun action(b: WBlock) {
        if (b.id == "done") publish() else wiz.next(b.id, typing = isTyping())
    }

    fun publish() {
        val p = wiz.buildProfile() ?: return
        focusManager.clearFocus(force = true)
        app.addPublished(p, wiz.isPublic)
    }

    fun close(force: Boolean) = app.closeWizard(force)

    /** Enter / „Tovább” a billentyűzeten: következő mező a blokkban, az utolsón a blokk gombja. */
    fun imeNext(b: WBlock, key: String) {
        val keys = wiz.fieldKeys(b.id)
        val i = keys.indexOf(key)
        if (i >= 0 && i < keys.size - 1) {
            wiz.focusRequest = keys[i + 1]
            return
        }
        if (wiz.status(b.id) == BlockStatus.Active && wiz.valid(b.id)) action(b) else focusManager.clearFocus()
    }

    /** A jobb felső gomb billentyűzet közben: a blokk gombja, vagy „Kész” = billentyűzet le. */
    fun go() {
        val a = wiz.activeBlock
        if (a != null && wiz.focusedBlock == a.id) {
            if (wiz.valid(a.id)) action(a)
        } else {
            focusManager.clearFocus()
        }
    }
}

/**
 * Az új profil varázsló. Alulról csúszik fel (0,36 s), a kezdőképernyő balra csúszik ki (0,38 s).
 * Amíg a kezdőképernyő látszik, az állapotsor háttere sötétkék.
 */
@Composable
fun WizardHost(app: AppState) {
    val wiz = app.wizard
    AnimatedVisibility(
        visible = app.wizardOpen && wiz != null,
        enter = slideInVertically(tween(360, easing = SheetEasing)) { (it * 1.05f).roundToInt() },
        exit = slideOutVertically(tween(360, easing = SheetEasing)) { (it * 1.05f).roundToInt() },
    ) {
        if (wiz != null) key(wiz) { WizardScreen(app, wiz) }
    }
    if (app.wizardOpen && wiz?.introShown == true) {
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(V.h1)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WizardScreen(app: AppState, wiz: WizardState) {
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val kb = WindowInsets.ime.getBottom(density) > 0
    val kbNow by rememberUpdatedState(kb)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pickKind by remember { mutableStateOf("photo") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val kind = pickKind
            scope.launch {
                when (val r = loadImage(context, uri, if (kind == "photo") 640 else 320)) {
                    is ImageResult.Ok -> wiz.setPic(kind, r.pic)
                    is ImageResult.Err -> wiz.errors[kind] = r.message
                }
            }
        }
    }
    val c = remember(wiz) {
        WizardController(
            app = app,
            wiz = wiz,
            focusManager = focusManager,
            isTyping = { kbNow && wiz.focusedKey != null },
            pickImage = { kind ->
                pickKind = kind
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
        )
    }

    // e-mail ellenőrzés gépelés közben 0,7 s késleltetéssel
    LaunchedEffect(wiz.email) {
        if (wiz.emailOk(wiz.email)) {
            wiz.errors.remove("email")
        } else {
            delay(700)
            wiz.errors["email"] = EMAIL_ERROR
        }
    }
    // bezáráskor a billentyűzet azonnal lemegy
    LaunchedEffect(app.wizardOpen) {
        if (!app.wizardOpen) focusManager.clearFocus(force = true)
    }

    val flow = wiz.flow
    val pager = rememberPagerState(initialPage = 0) { wiz.flow.size }
    LaunchedEffect(wiz.scrollRequest) {
        val r = wiz.scrollRequest ?: return@LaunchedEffect
        val i = wiz.fidx(r.blockId)
        if (i >= 0) {
            if (r.instant) pager.scrollToPage(i) else pager.animateScrollToPage(i)
        }
    }
    val k = pager.currentPage.coerceIn(0, flow.lastIndex)

    Box(
        Modifier
            .fillMaxSize()
            .noRippleClickable { }
            .statusBarsPadding()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(V.bg)
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
        ) {
            TopBar(c, kb, k)
            Progress(c, kb, k)
            HorizontalPager(
                state = pager,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = if (kb) 2.dp else 6.dp, bottom = if (kb) 12.dp else 18.dp),
                pageSpacing = 12.dp,
                key = { wiz.flow.getOrNull(it)?.id ?: it },
            ) { i ->
                val b = wiz.flow.getOrNull(i)
                if (b != null) BlockPage(c, b, i, kb)
            }
        }
        AnimatedVisibility(
            visible = wiz.introShown,
            enter = slideInHorizontally(tween(380, easing = SheetEasing)) { -it },
            exit = slideOutHorizontally(tween(380, easing = SheetEasing)) { -it },
        ) {
            WizardIntro(c)
        }
        if (wiz.confirmOpen) ConfirmBox(c)
    }
}

/** .wiz-bar2: bezárás · (típus · n/N ⌄ / aktuális blokk színes pöttyel) · billentyűzet közben a blokk gombja. */
@Composable
private fun TopBar(c: WizardController, kb: Boolean, k: Int) {
    val wiz = c.wiz
    val flow = wiz.flow
    val b = flow[k]
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = if (kb) 2.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            IconCircleButton(VIcons.close, "Bezárás", tint = V.sub) { c.close(false) }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
            if (!kb) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { wiz.introShown = true }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        (if (wiz.type == ProfileType.Private) "Magánszemély" else "Vállalkozói") + " · ${k + 1}/${flow.size}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = V.sub,
                    )
                    Icon(VIcons.chevDownBold, contentDescription = null, tint = V.sub, modifier = Modifier.size(13.dp))
                }
            }
            val dot by animateColorAsState(b.color.c, tween(200), label = "nowDot")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(
                    Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(dot)
                )
                Text(b.title, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            }
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            if (kb) GoButton(c)
        }
    }
}

@Composable
private fun GoButton(c: WizardController) {
    val wiz = c.wiz
    // ha az aktív blokk egyik mezőjében gépelünk: a blokk gombja; különben „Kész” (billentyűzet le)
    val a = wiz.activeBlock?.takeIf { wiz.focusedBlock == it.id }
    val label = a?.let { wiz.actionLabel(it) } ?: "Kész"
    val soft = a == null || wiz.isSkip(a)
    val enabled = a == null || wiz.valid(a.id)
    val bg = when {
        !enabled -> V.switchOff
        soft -> V.blueSoft
        else -> V.blueFill
    }
    val fg = when {
        !enabled -> V.sub
        soft -> V.blue
        else -> V.onBlueFill
    }
    Box(
        Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .clickable(enabled = enabled) { c.go() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = fg, maxLines = 1)
    }
}

/**
 * .wiz-prog2: blokkonként egy csík (kész/újranyitott = blokk színe, kihagyott = szürke, aktív = 45%-ig színes).
 * Az éppen látott blokk csíkja vastagabb; koppintásra odagörget.
 */
@Composable
private fun Progress(c: WizardController, kb: Boolean, k: Int) {
    val wiz = c.wiz
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = if (kb) 6.dp else 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        wiz.flow.forEachIndexed { i, b ->
            val s = wiz.status(b.id)
            val here = i == k
            val base by animateColorAsState(
                when (s) {
                    BlockStatus.Done, BlockStatus.Open -> b.color.c
                    BlockStatus.Skip -> V.chev
                    else -> V.switchOff
                }, tween(250), label = "prog"
            )
            Box(
                Modifier
                    .weight(1f)
                    .height(14.dp)
                    .noRippleClickable { wiz.jump(b.id) }
                    .padding(vertical = if (here) 3.dp else 5.dp)
                    .clip(RoundedCornerShape(if (here) 3.dp else 1.dp))
                    .drawBehind {
                        drawRect(base)
                        if (s == BlockStatus.Active) drawRect(b.color.c, size = Size(size.width * 0.45f, size.height))
                    }
            )
        }
    }
}

/** .wiz-confirm: „Kilépsz? Az adatok elvesznek.” Kilépés / Maradok. */
@Composable
private fun ConfirmBox(c: WizardController) {
    Box(
        Modifier
            .fillMaxSize()
            .background(V.scrim)
            .noRippleClickable { }
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 24.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(V.surface)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Kilépsz? Az adatok elvesznek.", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            PrimaryButton("Kilépés", danger = true) { c.close(true) }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { c.wiz.confirmOpen = false },
                contentAlignment = Alignment.Center,
            ) {
                Text("Maradok", color = V.blue, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
