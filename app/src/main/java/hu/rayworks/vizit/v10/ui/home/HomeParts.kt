package hu.rayworks.vizit.v10.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import hu.rayworks.vizit.v10.data.AppState
import hu.rayworks.vizit.v10.data.NfcDevice
import hu.rayworks.vizit.v10.data.Page
import hu.rayworks.vizit.v10.data.Profile
import hu.rayworks.vizit.v10.data.SheetKind
import hu.rayworks.vizit.v10.data.SyncStatus
import hu.rayworks.vizit.v10.ui.components.Banner
import hu.rayworks.vizit.v10.ui.components.Tone
import hu.rayworks.vizit.v10.ui.components.ProfileAvatar
import hu.rayworks.vizit.v10.ui.components.ProfileLabel
import hu.rayworks.vizit.v10.ui.components.ProfileTag
import hu.rayworks.vizit.v10.ui.components.QrCode
import hu.rayworks.vizit.v10.ui.components.TonalSmallButton
import hu.rayworks.vizit.v10.ui.components.dashedBorder
import hu.rayworks.vizit.v10.ui.components.noRippleClickable
import hu.rayworks.vizit.v10.ui.icons.VIcons
import hu.rayworks.vizit.v10.ui.theme.V
import kotlin.math.abs

/* ------------------------------------------------------------------ fejléc */

/** .nav: VIZIT felirat (vagy nagy cím) balra, gomb jobbra. A VIZIT-et hosszan nyomva A ⇄ B elrendezés. */
@Composable
fun HomeHeader(
    app: AppState,
    modifier: Modifier = Modifier,
    title: String? = null,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (title == null) {
            Text(
                "VIZIT",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.18.em,
                lineHeight = 22.sp,
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(onLongPress = {
                        if (!app.productionMode) app.sheet = SheetKind.Prototype
                    })
                },
            )
        } else {
            Text(title, fontSize = 24.sp, fontWeight = FontWeight.Medium, lineHeight = 26.4.sp)
        }
        Box(Modifier.weight(1f))
        trailing()
    }
}

/** .me-btn: 44 dp-es profilkép fehér (2 dp) és szürke (1,5 dp) gyűrűvel. */
@Composable
fun MeButton(app: AppState, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .drawBehind {
                val r = size.minDimension / 2f
                drawCircle(V.line, radius = r + 3.5.dp.toPx())
                drawCircle(V.surface, radius = r + 2.dp.toPx())
            }
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        ProfileAvatar(app.focus, 44.dp, V.blueSoft, V.blue)
    }
}

/* ------------------------------------------------------------------ NFC sáv */

private val EaseOut = CubicBezierEasing(0f, 0f, 0.58f, 1f)

/** .nfc-ic: kék kör, két táguló gyűrűvel (ping: 2,2 s, ease-out, a második 1,1 s-mal később). */
@Composable
private fun NfcIcon(on: Boolean) {
    Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
        if (on) {
            val t = rememberInfiniteTransition(label = "nfcPing")
            val p1 by t.animateFloat(
                0f, 1f,
                infiniteRepeatable(tween(2200, easing = EaseOut), RepeatMode.Restart),
                label = "ring1",
            )
            val p2 by t.animateFloat(
                0f, 1f,
                infiniteRepeatable(
                    tween(2200, easing = EaseOut), RepeatMode.Restart,
                    initialStartOffset = StartOffset(1100, StartOffsetType.FastForward),
                ),
                label = "ring2",
            )
            listOf({ p1 }, { p2 }).forEach { p ->
                Box(
                    Modifier
                        .size(46.dp)
                        .graphicsLayer {
                            val v = p()
                            val s = 1f + 0.75f * v
                            scaleX = s
                            scaleY = s
                            alpha = 0.7f * (1f - v)
                        }
                        .border(2.dp, V.blue, CircleShape)
                )
            }
        }
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(if (on) V.blueFill else V.chip),
            contentAlignment = Alignment.Center,
        ) {
            Icon(VIcons.nfc, contentDescription = null, tint = if (on) V.onBlueFill else V.sub, modifier = Modifier.size(22.dp))
        }
    }
}

/**
 * Androidos NFC sáv. Kész: lüktet, koppintásra indul az NFC-küldés (60 mp).
 * Kikapcsolva (appban vagy a telefonon): bekapcsoló gomb. NFC nélküli telefonon nem jelenik meg.
 */
@Composable
fun NfcStrip(app: AppState, modifier: Modifier = Modifier) {
    if (app.nfcDevice == NfcDevice.Unsupported) return
    val shape = RoundedCornerShape(24.dp)
    val ready = app.nfcOn && app.nfcDevice == NfcDevice.Ready
    if (ready) {
        Row(
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(V.container)
                .clickable {
                    if (app.current == null) app.toast("Válassz profilt") else app.push(Page.NfcSend)
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NfcIcon(on = true)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("NFC kész", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = V.onContainer)
                val sub = app.current?.let { it.label + " profil" }
                if (sub != null) {
                    Text(sub, fontSize = 13.sp, color = V.onContainer.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Icon(VIcons.chev, contentDescription = "NFC-küldés indítása", tint = V.onContainer, modifier = Modifier.size(20.dp))
        }
    } else {
        Row(
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(V.surface)
                .border(1.dp, V.line, shape)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NfcIcon(on = false)
            Text(
                if (!app.nfcOn) "NFC kikapcsolva" else "Az NFC ki van kapcsolva a telefonon",
                fontSize = 16.sp, fontWeight = FontWeight.Medium, color = V.ink, modifier = Modifier.weight(1f),
            )
            TonalSmallButton("Bekapcsolás") {
                if (!app.nfcOn) app.setNfc(true) else {
                    app.nfcDevice = NfcDevice.Ready
                    app.toast("NFC bekapcsolva")
                }
            }
        }
    }
}

/** Offline mód és szinkronhiba jelzése a főképernyő tetején (csak ha van mit mondani). */
@Composable
fun StatusBanners(app: AppState, modifier: Modifier = Modifier) {
    when {
        app.offline -> Banner("Offline mód · a szinkron később folytatódik", Tone.Info, modifier, icon = VIcons.cloudOff)
        app.sync == SyncStatus.Conflict -> Banner("A szinkron ütközött", Tone.Error, modifier, action = "Megoldás") { app.push(Page.Settings) }
        app.sync == SyncStatus.Retry -> Banner("A szinkron újrapróbálásra vár", Tone.Warn, modifier, action = "Részletek") { app.push(Page.Settings) }
        else -> Unit
    }
}

/* ------------------------------------------------------------------ profil kártyák */

/** .pcard: címke (+ MINTA/ÚJ), QR (184 dp, koppintásra teljes képernyő), név és rövid link. */
@Composable
fun ProfileCard(pf: Profile, qr: String, onQr: (() -> Unit)?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(V.surface)
            .border(1.dp, V.line, shape)
            .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileLabel(pf.label, pf.color, Modifier.weight(1f, fill = false))
            if (pf.tag != null) {
                Box(Modifier.padding(start = 8.dp)) { ProfileTag(pf.tag, pf.fresh) }
            }
        }
        Box(
            Modifier
                .width(184.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(V.qrPlate)
                .then(if (onQr != null) Modifier.clickable(onClick = onQr) else Modifier)
                .padding(4.dp)
        ) {
            QrCode(qr, Modifier.fillMaxWidth())
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(pf.name, fontSize = 17.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(pf.short, fontSize = 13.sp, color = V.sub, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * .pcard.new: szaggatott keretes „Új profil” kártya. Pontosan akkora, mint a profil kártyák
 * (egy láthatatlan profil kártya adja a méretét, mint a CSS flex „stretch”).
 */
@Composable
fun NewProfileCard(template: Profile, onCreate: () -> Unit) {
    Box {
        ProfileCard(template, qr = "VIZIT", onQr = null, modifier = Modifier.alpha(0f).clearAndSetSemantics { })
        Column(
            Modifier
                .matchParentSize()
                .dashedBorder(2.dp, V.line, 28.dp)
                .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(V.blueSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(VIcons.plus, contentDescription = null, tint = V.blue, modifier = Modifier.size(30.dp))
            }
            Text("Új profil", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            TonalSmallButton("Létrehozás", onCreate)
        }
    }
}

/** .dots: az aktuális pötty 22 dp széles és sötét (0,2 s), az utolsó üres karika = „Új profil”. */
@Composable
fun Dots(count: Int, selected: Int, onPick: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count + 1) { i ->
            val cur = i == selected
            val plus = i == count
            val w by animateDpAsState(if (cur) 22.dp else 8.dp, tween(200), label = "dotW")
            val c by animateColorAsState(
                when {
                    cur -> V.ink
                    plus -> Color.Transparent
                    else -> V.dot
                }, tween(200), label = "dotC"
            )
            Box(
                Modifier
                    .height(20.dp)
                    .noRippleClickable { onPick(i) }
                    .padding(horizontal = 3.dp),
                contentAlignment = Alignment.Center,
            ) {
                val shape = RoundedCornerShape(4.dp)
                Box(
                    Modifier
                        .size(w, 8.dp)
                        .clip(shape)
                        .background(c)
                        .then(if (plus && !cur) Modifier.border(1.5.dp, V.dot, shape) else Modifier)
                )
            }
        }
    }
}

/**
 * .pstack: oldalra húzható profil kártyák + pöttyök. Egy húzás = egy kártya (scroll-snap-stop: always),
 * a kártyák között 12 dp, a szomszédok 16 dp-rel belógnak.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileStack(app: AppState, modifier: Modifier = Modifier) {
    val pager = rememberPagerState(initialPage = app.selected) { app.profiles.size + 1 }
    var swipeStart by remember(pager) { mutableStateOf<Pair<Long, Int>?>(null) }
    val minimumFlingVelocity = with(LocalDensity.current) { 400.dp.toPx() }
    val fling = remember(pager, minimumFlingVelocity) {
        object : TargetedFlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float,
                                                         onRemainingDistanceUpdated: (Float) -> Unit): Float {
                val gesture = swipeStart
                try {
                    val origin = gesture?.second ?: pager.settledPage
                    val position = pager.currentPage + pager.currentPageOffsetFraction
                    val displacement = position - origin
                    val direction = when {
                        displacement > 0.001f -> 1
                        displacement < -0.001f -> -1
                        initialVelocity > 0f -> 1
                        initialVelocity < 0f -> -1
                        else -> 0
                    }
                    val quickForward = abs(initialVelocity) >= minimumFlingVelocity &&
                        initialVelocity * direction > 0f
                    val target = (origin + if (abs(displacement) >= 0.5f || quickForward) direction else 0)
                        .coerceIn(0, pager.pageCount - 1)
                    val distance = (target - position) * (pager.layoutInfo.pageSize + pager.layoutInfo.pageSpacing)
                    if (hu.rayworks.vizit.BuildConfig.DEBUG) android.util.Log.d("VizitProfilePager",
                        "fling origin=$origin position=$position velocity=$initialVelocity target=$target")
                    // Drive the existing scroll scope to the exact card; a second native snap
                    // must not choose another neighbor after the approach animation.
                    var consumed = 0f
                    onRemainingDistanceUpdated(distance)
                    if (abs(distance) > 0.5f) {
                        animate(0f, distance, animationSpec = tween(240, easing = EaseOut)) { value, _ ->
                            consumed += scrollBy(value - consumed)
                            onRemainingDistanceUpdated(distance - consumed)
                        }
                    }
                    scrollBy(distance - consumed)
                    onRemainingDistanceUpdated(0f)
                    return 0f
                } finally {
                    if (swipeStart === gesture) swipeStart = null
                }
            }
        }
    }
    var handled by remember { mutableStateOf(app.goToRequest?.nonce) }
    var applyingNavigation by remember { mutableStateOf(false) }
    LaunchedEffect(pager) {
        snapshotFlow {
            Triple(pager.settledPage, pager.isScrollInProgress || applyingNavigation, app.goToRequest?.nonce != handled)
        }.collect { (page, scrolling, pendingNavigation) ->
            if (!scrolling && !pendingNavigation) app.selected = page
        }
    }
    val req = app.goToRequest
    LaunchedEffect(req) {
        if (req != null && req.nonce != handled) {
            applyingNavigation = true
            handled = req.nonce
            swipeStart = null
            try {
                val index = req.index.coerceIn(0, app.profiles.size)
                if (req.animate) pager.animateScrollToPage(index) else {
                    // Apply catalog changes and the requested selection in the same remeasure.
                    // An immediate scroll can measure the old "new-profile" key and retain it
                    // at the end of the newly loaded catalog while AppState still selects 0.
                    pager.requestScrollToPage(index)
                }
            } finally {
                applyingNavigation = false
            }
        }
    }
    Column(modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxWidth().testTag("profile-pager").pointerInput(pager) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    swipeStart = System.nanoTime() to pager.settledPage
                    if (hu.rayworks.vizit.BuildConfig.DEBUG) android.util.Log.d("VizitProfilePager",
                        "touch origin=${swipeStart?.second} current=${pager.currentPage}")
                    waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    // Consumed drag events can cancel this observer before the scroll job starts.
                    // The matching fling (or a later explicit navigation) clears the origin.
                }
            },
            key = { index -> app.profiles.getOrNull(index)?.id ?: "new-profile" },
            userScrollEnabled = !app.operationBusy,
            flingBehavior = fling,
            contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 6.dp, bottom = 20.dp),
            pageSpacing = 12.dp,
            verticalAlignment = Alignment.Top,
        ) { i ->
            val list = app.profiles
            if (i < list.size) {
                ProfileCard(list[i], qr = app.cardQr(list[i]), onQr = { app.openFullQr(i) })
            } else {
                NewProfileCard(list.firstOrNull() ?: app.focus, onCreate = { app.openWizard() })
            }
        }
        Dots(count = app.profiles.size, selected = app.selected, onPick = { app.goTo(it) })
    }
}

/* ------------------------------------------------------------------ elrendezés segéd */

/**
 * Oszlop-elrendezés állandó térközzel, ahol a megadott elem (a kártyák) a maradék hely közepére kerül:
 * a CSS `.solo .pstack { margin-block: auto }` megfelelője.
 */
class CenterItemArrangement(private val gap: Dp, private val centerIndex: Int) : Arrangement.Vertical {
    override val spacing: Dp get() = gap

    override fun Density.arrange(totalSize: Int, sizes: IntArray, outPositions: IntArray) {
        val g = gap.roundToPx()
        val used = sizes.sum() + g * (sizes.size - 1).coerceAtLeast(0)
        val free = (totalSize - used).coerceAtLeast(0)
        var y = 0
        sizes.forEachIndexed { i, s ->
            if (i == centerIndex) y += free / 2
            outPositions[i] = y
            y += s + g
            if (i == centerIndex) y += free - free / 2
        }
    }
}
