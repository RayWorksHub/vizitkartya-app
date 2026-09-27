package hu.rayworks.vizit.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hu.rayworks.vizit.data.remote.NodeBackendApi
import hu.rayworks.vizit.data.remote.SupabaseProvider
import hu.rayworks.vizit.ui.design.Vizit
import hu.rayworks.vizit.ui.design.components.VizitBanner
import hu.rayworks.vizit.ui.design.components.VizitButton
import hu.rayworks.vizit.ui.design.components.VizitGroup
import hu.rayworks.vizit.ui.design.components.VizitRow
import hu.rayworks.vizit.ui.design.components.VizitSectionHeader
import hu.rayworks.vizit.ui.design.components.VizitTone
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private data class ActivityDay(val day: String, val views: Int, val clicks: Int)
private data class TopAction(val label: String, val count: Int)
private data class AnalyticsSummary(
    val views: Int,
    val saves: Int,
    val clicks: Int,
    val days: List<ActivityDay>,
    val actions: List<TopAction>,
) {
    companion object {
        fun read(json: JsonObject): AnalyticsSummary {
            fun JsonObject.number(key: String) = get(key)?.jsonPrimitive?.intOrNull ?: 0
            return AnalyticsSummary(
                views = json.number("totalViews"),
                saves = json.number("totalSaves"),
                clicks = json.number("totalClicks"),
                days = json["last30Days"]?.jsonArray?.map { item ->
                    val day = item.jsonObject
                    ActivityDay(day["day"]?.jsonPrimitive?.content.orEmpty(), day.number("views"), day.number("clicks"))
                }.orEmpty(),
                actions = json["topActions"]?.jsonArray?.map { item ->
                    val action = item.jsonObject
                    TopAction(action["label"]?.jsonPrimitive?.content.orEmpty(), action.number("count"))
                }.orEmpty(),
            )
        }
    }
}

@Composable
fun AnalyticsScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    var reload by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var summary by remember { mutableStateOf<AnalyticsSummary?>(null) }
    LaunchedEffect(reload) {
        loading = true
        error = null
        try {
            val payload = NodeBackendApi(SupabaseProvider.getOrNull()).request("GET", "/api/analytics/summary")
            summary = AnalyticsSummary.read(payload)
        } catch (failure: Exception) {
            error = failure.localizedMessage ?: "A statisztika most nem tölthető be."
        } finally {
            loading = false
        }
    }

    val colors = Vizit.colors
    Column(
        modifier = Modifier.fillMaxSize().background(colors.canvas).verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.statusBars).padding(Vizit.space.md),
        verticalArrangement = Arrangement.spacedBy(Vizit.space.md),
    ) {
        VizitRow(label = "Vissza a kezdőlapra", icon = Icons.AutoMirrored.Outlined.ArrowBack, onClick = onBack)
        Text("Statisztikák", style = Vizit.type.title, color = colors.textPrimary)
        Text("Profilod elmúlt 30 napi megtekintései és kapcsolatfelvételei.",
            style = Vizit.type.body, color = colors.textMuted)
        if (loading) Text("Statisztika betöltése…", style = Vizit.type.body, color = colors.textMuted)
        if (error != null) VizitBanner(text = error!!, tone = VizitTone.Error)
        val data = summary
        if (data != null && !loading) {
            VizitSectionHeader("Összesítés")
            VizitGroup {
                VizitRow(label = "Profilmegtekintés", value = data.views.toString())
                VizitRow(label = "Kapcsolatmentés", value = data.saves.toString())
                VizitRow(label = "Hivatkozáskattintás", value = data.clicks.toString())
            }
            VizitSectionHeader("Napi aktivitás")
            if (data.days.isEmpty()) {
                Text("Még nincs mérhető esemény az elmúlt 30 napban.",
                    style = Vizit.type.body, color = colors.textMuted)
            } else {
                val max = data.days.maxOf { it.views + it.clicks }.coerceAtLeast(1)
                data.days.forEach { day ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${day.day}  ·  ${day.views} megtekintés  ·  ${day.clicks} kattintás",
                            style = Vizit.type.caption, color = colors.textPrimary)
                        Row(modifier = Modifier.fillMaxWidth().height(7.dp)) {
                            Box(Modifier.fillMaxWidth(day.views.toFloat() / max).height(7.dp).background(colors.primary))
                            Box(Modifier.fillMaxWidth(day.clicks.toFloat() / max.coerceAtLeast(max - day.views))
                                .height(7.dp).background(colors.textMuted))
                        }
                    }
                }
            }
            if (data.actions.isNotEmpty()) {
                VizitSectionHeader("Leggyakoribb műveletek")
                VizitGroup {
                    data.actions.forEach { action ->
                        VizitRow(label = action.label, value = action.count.toString())
                    }
                }
            }
        }
        Spacer(Modifier.height(Vizit.space.sm))
        VizitButton(text = "Frissítés", icon = Icons.Outlined.Refresh, onClick = { reload++ },
            modifier = Modifier.fillMaxWidth())
    }
}
