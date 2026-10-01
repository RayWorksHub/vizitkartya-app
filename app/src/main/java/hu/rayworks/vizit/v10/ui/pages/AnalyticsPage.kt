package hu.rayworks.vizit.v10.ui.pages

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.rayworks.vizit.data.remote.NodeBackendApi
import hu.rayworks.vizit.data.remote.SupabaseProvider
import hu.rayworks.vizit.v10.data.AppState
import hu.rayworks.vizit.v10.data.Profile
import hu.rayworks.vizit.v10.ui.components.EmptyState
import hu.rayworks.vizit.v10.ui.components.GroupCard
import hu.rayworks.vizit.v10.ui.components.GroupLabel
import hu.rayworks.vizit.v10.ui.components.OutlineButton
import hu.rayworks.vizit.v10.ui.components.PageScaffold
import hu.rayworks.vizit.v10.ui.components.Pill
import hu.rayworks.vizit.v10.ui.components.ProfileLabel
import hu.rayworks.vizit.v10.ui.components.Spinner
import hu.rayworks.vizit.v10.ui.icons.VIcons
import hu.rayworks.vizit.v10.ui.theme.V
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.math.sin

private val MONTHS = listOf("jan.", "febr.", "márc.", "ápr.", "máj.", "jún.", "júl.", "aug.", "szept.", "okt.", "nov.", "dec.")
private fun LocalDate.hu() = MONTHS[monthValue - 1] + " " + dayOfMonth + "."

private class Stats(val days: List<LocalDate>, val views: List<Int>, val clicks: List<Int>, val saves: Int, val top: List<Pair<String, Int>>)

private fun liveStats(payload: JsonObject): Stats {
    fun JsonObject.number(key: String) = get(key)?.jsonPrimitive?.intOrNull ?: 0
    val end = LocalDate.now()
    val days = (29 downTo 0).map { end.minusDays(it.toLong()) }
    val activity = payload["last30Days"]?.jsonArray.orEmpty().mapNotNull { item ->
        val value = item.jsonObject
        runCatching { LocalDate.parse(value["day"]?.jsonPrimitive?.content.orEmpty()) }.getOrNull()?.let { day ->
            day to (value.number("views") to value.number("clicks"))
        }
    }.toMap()
    val top = payload["topActions"]?.jsonArray.orEmpty().map { item ->
        val value = item.jsonObject
        value["label"]?.jsonPrimitive?.content.orEmpty() to value.number("count")
    }.filter { it.first.isNotBlank() }
    return Stats(
        days = days,
        views = days.map { activity[it]?.first ?: 0 },
        clicks = days.map { activity[it]?.second ?: 0 },
        saves = payload.number("totalSaves"),
        top = top,
    )
}

/** Minta adatok az élő profilhoz (éles: GET /api/analytics/summary, 30 nap). */
private fun sampleStats(): Stats {
    val end = LocalDate.now()
    val days = (29 downTo 0).map { end.minusDays(it.toLong()) }
    val views = days.mapIndexed { i, d ->
        val weekend = d.dayOfWeek.value >= 6
        (7 + 3 * sin(i / 4.0) + (i * 13 % 5) - (if (weekend) 4 else 0) + i / 6).roundToInt().coerceAtLeast(1)
    }
    val clicks = views.mapIndexed { i, v -> (v / 3 + i % 3).coerceAtMost(v) }
    return Stats(days, views, clicks, 37, listOf("Névjegy mentése" to 37, "Hívás" to 21, "E-mail" to 16, "Weboldal" to 11))
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(V.surface)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.Medium)
        Text(label, fontSize = 13.sp, color = V.sub, maxLines = 1)
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(width = 14.dp, height = 3.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Text(label, fontSize = 13.sp, color = V.sub)
    }
}

/**
 * Napi aktivitás vonaldiagram: két sorozat (megtekintés, kattintás) közös tengelyen, 2 dp vonal,
 * halvány rácsvonalak, jelmagyarázat, érintésre/húzásra függőleges vonal és a nap értékei.
 */
@Composable
private fun ActivityChart(s: Stats) {
    var sel by remember { mutableStateOf<Int?>(null) }
    val max = (s.views.maxOrNull() ?: 1).let { (((it + 4) / 5) * 5).coerceAtLeast(5) }
    val c1 = V.series1
    val c2 = V.series2
    val gridC = V.grid
    val surface = V.surface
    val subC = V.sub
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(V.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Napi aktivitás", fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendItem(c1, "Megtekintés")
                LegendItem(c2, "Kattintás")
            }
        }
        val i = sel ?: s.views.lastIndex
        Text(
            "${s.days[i].hu()} · ${s.views[i]} megtekintés · ${s.clicks[i]} kattintás",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
        Row(Modifier.fillMaxWidth().height(170.dp)) {
            Column(
                Modifier
                    .width(26.dp)
                    .fillMaxHeight()
                    .padding(bottom = 18.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                listOf(max, max / 2, 0).forEach { Text("$it", fontSize = 11.sp, color = V.sub, lineHeight = 11.sp) }
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .pointerInput(s) {
                            detectTapGestures { o -> sel = ((o.x / size.width) * (s.views.size - 1)).roundToInt().coerceIn(0, s.views.lastIndex) }
                        }
                        .pointerInput(s) {
                            detectHorizontalDragGestures { change, _ ->
                                sel = ((change.position.x / size.width) * (s.views.size - 1)).roundToInt().coerceIn(0, s.views.lastIndex)
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val pad = 6.dp.toPx()
                    fun x(k: Int) = k * w / (s.views.size - 1)
                    fun y(v: Int) = pad + (h - 2 * pad) * (1f - v.toFloat() / max)
                    listOf(0f, 0.5f, 1f).forEach { f ->
                        val gy = pad + (h - 2 * pad) * f
                        drawLine(gridC, Offset(0f, gy), Offset(w, gy), strokeWidth = 1.dp.toPx())
                    }
                    fun series(values: List<Int>, color: Color) {
                        val path = Path()
                        values.forEachIndexed { k, v -> if (k == 0) path.moveTo(x(k), y(v)) else path.lineTo(x(k), y(v)) }
                        drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                        val last = values.lastIndex
                        drawCircle(surface, radius = 6.dp.toPx(), center = Offset(x(last), y(values[last])))
                        drawCircle(color, radius = 4.dp.toPx(), center = Offset(x(last), y(values[last])))
                    }
                    series(s.views, c1)
                    series(s.clicks, c2)
                    val k = sel
                    if (k != null) {
                        drawLine(subC, Offset(x(k), 0f), Offset(x(k), h), strokeWidth = 1.dp.toPx())
                        listOf(s.views[k] to c1, s.clicks[k] to c2).forEach { (v, c) ->
                            drawCircle(surface, radius = 6.dp.toPx(), center = Offset(x(k), y(v)))
                            drawCircle(c, radius = 4.dp.toPx(), center = Offset(x(k), y(v)))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().height(18.dp).padding(top = 4.dp)) {
                    Text(s.days.first().hu(), fontSize = 11.sp, color = V.sub, modifier = Modifier.weight(1f))
                    Text(s.days[s.days.size / 2].hu(), fontSize = 11.sp, color = V.sub, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    Text(s.days.last().hu(), fontSize = 11.sp, color = V.sub, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** Leggyakoribb műveletek: vízszintes sávok egy színnel, érték a sáv végén. */
@Composable
private fun TopActions(top: List<Pair<String, Int>>) {
    val max = (top.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
    GroupCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            top.forEach { (label, n) ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row {
                        Text(label, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text("$n", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(V.chip)) {
                        Box(
                            Modifier
                                .fillMaxWidth(n.toFloat() / max)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(V.series1)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Statisztikák (éles AnalyticsScreen): az elmúlt 30 nap megtekintései, mentései, kattintásai,
 * napi aktivitás és a leggyakoribb műveletek. Minta profilnál üres állapot.
 */
@Composable
fun AnalyticsPage(app: AppState) {
    val p: Profile = app.focus
    var loading by remember { mutableStateOf(true) }
    var reloads by remember { mutableIntStateOf(0) }
    var stats by remember { mutableStateOf<Stats?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(reloads, p.id, app.productionMode) {
        loading = true
        error = null
        try {
            stats = if (!p.real) {
                null
            } else if (app.productionMode) {
                liveStats(NodeBackendApi(SupabaseProvider.getOrNull()).request("GET", "/api/analytics/summary?profileId=${p.id}"))
            } else {
                delay(650)
                sampleStats()
            }
        } catch (failure: Exception) {
            stats = null
            error = failure.localizedMessage ?: "A statisztika most nem tölthető be."
        } finally {
            loading = false
        }
    }
    PageScaffold("Statisztikák", onBack = { app.pop() }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileLabel(p.label, p.color)
            Pill("Elmúlt 30 nap", V.chip, V.sub)
        }
        when {
            loading -> Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) { Spinner() }
            error != null -> EmptyState(VIcons.warning, "A statisztika most nem tölthető be", error.orEmpty())
            !p.real -> EmptyState(VIcons.chart, "Még nincs mérhető esemény", "Az elmúlt 30 napban nem volt megtekintés.")
            stats != null -> {
                val data = requireNotNull(stats)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("${data.views.sum()}", "Megtekintés", Modifier.weight(1f))
                    StatTile("${data.saves}", "Mentés", Modifier.weight(1f))
                    StatTile("${data.clicks.sum()}", "Kattintás", Modifier.weight(1f))
                }
                ActivityChart(data)
                if (data.top.isNotEmpty()) {
                    GroupLabel("Leggyakoribb műveletek")
                    TopActions(data.top)
                }
            }
            else -> EmptyState(VIcons.chart, "Még nincs mérhető esemény", "Az elmúlt 30 napban nem volt megtekintés.")
        }
        OutlineButton("Frissítés", VIcons.sync, enabled = !loading) { reloads++ }
    }
}
