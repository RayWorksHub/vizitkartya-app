package hu.rayworks.vizit.v10.ui.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import hu.rayworks.vizit.v10.data.AppState
import hu.rayworks.vizit.v10.data.Page
import hu.rayworks.vizit.v10.ui.LocalActions
import hu.rayworks.vizit.v10.ui.components.ExtMark
import hu.rayworks.vizit.v10.ui.components.FilterChip
import hu.rayworks.vizit.v10.ui.components.GroupCard
import hu.rayworks.vizit.v10.ui.components.GroupLabel
import hu.rayworks.vizit.v10.ui.components.IconCircleButton
import hu.rayworks.vizit.v10.ui.components.ListRow
import hu.rayworks.vizit.v10.ui.components.PageScaffold
import hu.rayworks.vizit.v10.ui.components.PrimaryButton
import hu.rayworks.vizit.v10.ui.icons.VIcons
import hu.rayworks.vizit.v10.ui.theme.B1
import hu.rayworks.vizit.v10.ui.theme.B2
import hu.rayworks.vizit.v10.ui.theme.B3
import hu.rayworks.vizit.v10.ui.theme.B4
import hu.rayworks.vizit.v10.ui.theme.BlockColor
import hu.rayworks.vizit.v10.ui.theme.V
import kotlinx.coroutines.delay

private fun courseIcon(id: String): ImageVector = when (id) {
    "ai" -> VIcons.bulb
    "m365" -> VIcons.briefcase
    "basics" -> VIcons.building
    "security" -> VIcons.shield
    "marketing" -> VIcons.chart
    else -> VIcons.book
}

private fun guideIcon(id: String): ImageVector = when (id) {
    "launch" -> VIcons.building
    "billing" -> VIcons.doc
    "office" -> VIcons.briefcase
    "security" -> VIcons.shield
    "sales" -> VIcons.chart
    else -> VIcons.users
}

@Composable
private fun IconTile(icon: ImageVector, color: BlockColor, size: Int = 44) {
    Box(
        Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size * 0.3f).dp))
            .background(color.soft),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = color.c, modifier = Modifier.size((size * 0.5f).dp))
    }
}

@Composable
private fun Eyebrow(text: String, color: Color = V.sub) {
    Text(text.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.06.em, color = color, maxLines = 1)
}

private fun AppState.lessonsDoneIn(courseId: String) = lessonsDone.count { it.key.startsWith("$courseId:") && it.value }

/* ================================================================== Portál kezdőlap */

/** Vállalkozói Portál: VOSZ, Edukáció, Digitális segítség, Eszköztár, heti tipp. */
@Composable
fun HubPage(app: AppState) {
    val totalDone = app.lessonsDone.count { it.value }
    PageScaffold("Vállalkozói Portál", onBack = { app.pop() }) {
        data class Entry(val page: Page, val icon: ImageVector, val color: BlockColor, val eyebrow: String, val title: String, val sub: String)
        listOf(
            Entry(Page.HubVosz, VIcons.users, B1, "Partneri források", "VOSZ", "Hírek, videók és tanácsadás"),
            Entry(Page.HubEdu, VIcons.school, B3, "6 kurzus · $totalDone/24 lecke kész", "Vállalkozói Edukáció", "Rövid videóleckék saját tempóban"),
            Entry(Page.HubHelp, VIcons.headset, B2, "Bemutató mód", "Digitális segítség", "Videós konzultáció szakértővel"),
            Entry(Page.HubToolkit, VIcons.wrench, B4, "6 útmutató · ${app.guidesDone.count { it.value }} kész", "Vállalkozói eszköztár", "Lépésről lépésre, hivatalos linkekkel"),
        ).forEach { e ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(V.surface)
                    .clickable { app.push(e.page) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                IconTile(e.icon, e.color, 52)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Eyebrow(e.eyebrow, e.color.c)
                    Text(e.title, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    Text(e.sub, fontSize = 13.sp, color = V.sub)
                }
                Icon(VIcons.chev, contentDescription = null, tint = V.chev, modifier = Modifier.size(20.dp))
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(V.blueSoft)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(VIcons.bulb, contentDescription = null, tint = V.blue, modifier = Modifier.size(22.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Heti tipp", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = V.blue)
                Text(WEEKLY_TIP, fontSize = 14.sp, color = V.ink)
            }
        }
    }
}

/* ================================================================== VOSZ */

@Composable
fun HubVoszPage(app: AppState) {
    val actions = LocalActions.current
    PageScaffold("VOSZ", onBack = { app.pop() }) {
        GroupCard {
            val icons = listOf(VIcons.playCircle, VIcons.info, VIcons.globe)
            VOSZ_LINKS.forEachIndexed { i, l ->
                ListRow(l.title, icon = icons[i], sub = l.sub, divider = i > 0, trailing = { ExtMark() }) { actions.openUrl(l.url) }
            }
        }
        Text("Külső oldalak; tartalmukért a szolgáltató felel.", fontSize = 12.5.sp, color = V.sub, modifier = Modifier.padding(start = 6.dp))
    }
}

/* ================================================================== Edukáció */

@Composable
fun HubEduPage(app: AppState) {
    var chip by remember { mutableStateOf("Mind") }
    PageScaffold("Vállalkozói Edukáció", onBack = { app.pop() }) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            COURSE_CATEGORIES.forEach { c -> FilterChip(c, chip == c) { chip = c } }
        }
        COURSES.filter { it.matches(chip) }.forEach { c ->
            val done = app.lessonsDoneIn(c.id)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(V.surface)
                    .clickable { app.push(Page.HubCourse(c.id)) }
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconTile(courseIcon(c.id), B3, 48)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Eyebrow(c.category)
                        Text(c.title, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                Text(c.description, fontSize = 13.5.sp, color = V.sub, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${c.level} · 4 lecke", fontSize = 12.5.sp, color = V.sub, modifier = Modifier.weight(1f))
                    if (done > 0) {
                        Box(Modifier.width(72.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(V.chip)) {
                            Box(Modifier.fillMaxWidth(done / 4f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(B3.c))
                        }
                        Text("$done/4", fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = V.ink)
                    }
                }
            }
        }
    }
}

/* ================================================================== Kurzus */

/**
 * Kurzus lejátszó (éles: beágyazott YouTube). A prototípusban a lejátszás gomb a YouTube-ot
 * nyitja meg, és a kiválasztott leckét késznek jelöli (élesben: ha a videó 90%-a lement).
 */
@Composable
fun HubCoursePage(app: AppState, id: String) {
    val actions = LocalActions.current
    val c = COURSES.firstOrNull { it.id == id } ?: COURSES.first()
    var tab by remember { mutableIntStateOf(0) }
    var current by remember { mutableStateOf(c.lessons.firstOrNull { app.lessonsDone["${c.id}:${it.num}"] != true }?.num ?: c.lessons.first().num) }
    val done = app.lessonsDoneIn(c.id)

    Column(
        Modifier
            .fillMaxSize()
            .background(V.bg)
            .statusBarsPadding()
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(V.player),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f))
                    .clickable {
                        app.lessonsDone["${c.id}:$current"] = true
                        actions.openUrl(c.videoUrl)
                    },
                contentAlignment = Alignment.Center,
            ) { Icon(VIcons.play, contentDescription = "Lejátszás", tint = Color.White, modifier = Modifier.size(34.dp)) }
            IconCircleButton(
                VIcons.chevDown, "Vissza a kurzusokhoz",
                Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                background = Color.Black.copy(alpha = 0.45f),
                tint = Color.White,
            ) { app.pop() }
            Text(
                "Lecke $current",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
            )
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(c.title, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            Text(c.videoSource, fontSize = 13.sp, color = V.sub)
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
            listOf("Leckék", "Anyagok", "A kurzusról").forEachIndexed { i, t ->
                val on = tab == i
                val line by animateColorAsState(if (on) V.blue else Color.Transparent, tween(200), label = "tab")
                Column(
                    Modifier
                        .weight(1f)
                        .clickable { tab = i }
                        .padding(top = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(t, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (on) V.blue else V.sub)
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.fillMaxWidth(0.7f).height(3.dp).clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)).background(line))
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(V.line))
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (tab) {
                0 -> listOf("1. modul · Alapok és felkészülés", "2. modul · Gyakorlati alkalmazás").forEachIndexed { m, header ->
                    val lessons = c.lessons.filter { it.num.startsWith("${m + 1}.") }
                    val md = lessons.count { app.lessonsDone["${c.id}:${it.num}"] == true }
                    Row(Modifier.padding(start = 6.dp, end = 6.dp, top = 4.dp)) {
                        Text(header, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = V.blue, modifier = Modifier.weight(1f))
                        Text("$md/2", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (md == 2) V.green else V.sub)
                    }
                    GroupCard {
                        lessons.forEachIndexed { i, l ->
                            val isDone = app.lessonsDone["${c.id}:${l.num}"] == true
                            val isCur = l.num == current
                            ListRow(
                                l.title,
                                sub = when {
                                    isCur -> "Videólecke · kiválasztva"
                                    isDone -> "Videólecke · elvégezve"
                                    else -> "Videólecke"
                                },
                                divider = i > 0,
                                leading = {
                                    Box(
                                        Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (isCur) V.blueSoft else V.chip),
                                        contentAlignment = Alignment.Center,
                                    ) { Text(l.num, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (isCur) V.blue else V.sub) }
                                },
                                trailing = {
                                    Icon(
                                        when {
                                            isDone -> VIcons.checkCircle
                                            isCur -> VIcons.play
                                            else -> VIcons.playCircle
                                        },
                                        contentDescription = null,
                                        tint = when {
                                            isDone -> V.green
                                            isCur -> V.blue
                                            else -> V.sub
                                        },
                                        modifier = Modifier.size(22.dp),
                                    )
                                },
                            ) { current = l.num }
                        }
                    }
                }
                1 -> GroupCard {
                    c.lessons.forEachIndexed { i, l ->
                        ListRow(l.resTitle, icon = VIcons.doc, sub = l.title, divider = i > 0, trailing = { ExtMark() }) { actions.openUrl(l.resUrl) }
                    }
                }
                else -> {
                    Text(c.description, fontSize = 14.sp, color = V.ink)
                    GroupCard {
                        ListRow("Szint", value = c.level)
                        ListRow("Terjedelem", value = "2 modul · 4 lecke", divider = true)
                        ListRow("Nyelv", value = c.language, divider = true)
                        ListRow("Forrás", value = c.videoSource, divider = true)
                        ListRow("Haladásod", value = "$done / 4 lecke", valueBold = true, divider = true)
                    }
                    Text("Egy lecke akkor kész, ha a videó legalább 90%-a lejátszódott.", fontSize = 12.5.sp, color = V.sub, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

/* ================================================================== Digitális segítség */

/** Videóhívás bemutató: próbahívás, mikrofon, kamera, befejezés. Valódi hívás nem indul. */
@Composable
fun HubHelpPage(app: AppState) {
    var inCall by remember { mutableStateOf(false) }
    var mic by remember { mutableStateOf(true) }
    var cam by remember { mutableStateOf(true) }
    var seconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(inCall) {
        seconds = 0
        while (inCall) {
            delay(1000)
            seconds++
        }
    }
    PageScaffold("Digitális segítség", onBack = { app.pop() }) {
        Column(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.86f)
                .clip(RoundedCornerShape(28.dp))
                .background(V.player)
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth()) {
                Eyebrow(if (inCall) "Kapcsolódva · demó" else "Bemutató mód", if (inCall) V.green else Color.White.copy(alpha = 0.7f))
                Spacer(Modifier.weight(1f))
                Text(
                    if (inCall) "%02d:%02d".format(seconds / 60, seconds % 60) else "ELŐNÉZET",
                    fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.06.em, color = Color.White.copy(alpha = 0.7f),
                )
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(116.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (cam || !inCall) VIcons.person3 else VIcons.videoOff, contentDescription = "Digitális tanácsadó", tint = Color.White, modifier = Modifier.size(56.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(if (inCall) "VIZIT digitális tanácsadó" else "Próbahívás", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text(
                if (inCall) "Bemutató kapcsolat" else "Ellenőrizd a kamerát és a mikrofont",
                color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp,
            )
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                CallButton(if (mic) VIcons.mic else VIcons.micOff, "Mikrofon", on = mic) { mic = !mic }
                CallButton(if (cam) VIcons.video else VIcons.videoOff, "Kamera", on = cam) { cam = !cam }
                if (inCall) CallButton(VIcons.callEnd, "Befejezés", on = true, danger = true) { inCall = false }
            }
        }
        if (!inCall) PrimaryButton("Próbahívás indítása", icon = VIcons.video) { inCall = true }
        Text("Bemutató: nem kapcsol valódi tanácsadóhoz, hangot és videót nem továbbít.", fontSize = 12.5.sp, color = V.sub, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun CallButton(icon: ImageVector, label: String, on: Boolean, danger: Boolean = false, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(
                    when {
                        danger -> Color(0xFFD13B3B)
                        on -> Color.White.copy(alpha = 0.16f)
                        else -> Color.White
                    }
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = if (!on && !danger) V.player else Color.White, modifier = Modifier.size(24.dp))
        }
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
    }
}

/* ================================================================== Eszköztár */

/** Hat útmutató lépésekkel és hivatalos linkekkel, megvalósítási állapot körrel. */
@Composable
fun HubToolkitPage(app: AppState) {
    val actions = LocalActions.current
    var open by remember { mutableStateOf<String?>(GUIDES.first().id) }
    val n = GUIDES.count { app.guidesDone[it.id] == true }
    val score = n * 100 / GUIDES.size
    PageScaffold("Vállalkozói eszköztár", onBack = { app.pop() }) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(B4.soft)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val track = V.surface
            val fill = B4.c
            Box(Modifier.size(68.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(68.dp)) {
                    val sw = 7.dp.toPx()
                    drawArc(track, -90f, 360f, false, style = Stroke(sw))
                    drawArc(fill, -90f, 360f * n / GUIDES.size, false, style = Stroke(sw, cap = StrokeCap.Round))
                }
                Text("$score%", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Megvalósítási állapot", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text("$n/6 útmutató teljesítve", fontSize = 13.sp, color = V.sub)
            }
        }

        GUIDES.forEach { g ->
            val isDone = app.guidesDone[g.id] == true
            val expanded = open == g.id
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(V.surface)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { open = if (expanded) null else g.id }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    IconTile(guideIcon(g.id), if (isDone) B4 else B1, 44)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(g.title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text(g.description, fontSize = 13.sp, color = V.sub, maxLines = if (expanded) 3 else 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (isDone) Icon(VIcons.checkCircle, contentDescription = "Teljesítve", tint = V.green, modifier = Modifier.size(22.dp))
                    else Icon(if (expanded) VIcons.chevDown else VIcons.chev, contentDescription = null, tint = V.chev, modifier = Modifier.size(20.dp))
                }
                AnimatedVisibility(expanded, enter = expandVertically(tween(250)) + fadeIn(tween(200)), exit = shrinkVertically(tween(220)) + fadeOut(tween(150))) {
                    Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(B4.soft)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Eyebrow("Elérendő eredmény", B4.c)
                            Text(g.result, fontSize = 14.sp)
                        }
                        Text("Lépések", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = V.blue)
                        g.steps.forEachIndexed { i, step ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(V.blueSoft),
                                    contentAlignment = Alignment.Center,
                                ) { Text("${i + 1}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = V.blue) }
                                Text(step, fontSize = 14.sp, modifier = Modifier.weight(1f).padding(top = 2.dp))
                            }
                        }
                        Text("Közvetlen linkek", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = V.blue)
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(V.bg)
                        ) {
                            g.links.forEachIndexed { i, l ->
                                ListRow(l.title, icon = VIcons.globe, sub = l.sub, divider = i > 0, trailing = { ExtMark() }) { actions.openUrl(l.url) }
                            }
                        }
                        PrimaryButton(if (isDone) "Teljesítve" else "Útmutató teljesítve", icon = if (isDone) VIcons.check else null, soft = isDone) {
                            app.guidesDone[g.id] = !isDone
                        }
                    }
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(V.blueSoft)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(VIcons.arrowRight, contentDescription = null, tint = V.blue, modifier = Modifier.size(22.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    if (score < 50) "Következő lépés: válassz egy útmutatót" else "Következő lépés: mérd az eredményt",
                    fontSize = 14.sp, fontWeight = FontWeight.Medium, color = V.blue,
                )
                Text(
                    if (score < 50) "Kezdd azzal, amelyik most a legtöbb időt viszi el." else "Harminc nap múlva nézd meg, csökkent-e a hiba, az átfutási idő vagy a költség.",
                    fontSize = 14.sp,
                )
            }
        }
        Text("Adózási és jogi döntésnél kérd könyvelő vagy jogász véleményét.", fontSize = 12.5.sp, color = V.sub, textAlign = TextAlign.Start, modifier = Modifier.padding(start = 6.dp))
    }
}

