package hu.rayworks.vizit.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.CallEnd
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.VideoCall
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import hu.rayworks.vizit.ui.design.Vizit
import hu.rayworks.vizit.ui.design.components.VizitBrandHeader
import hu.rayworks.vizit.ui.design.components.VizitBrandHeaderStyle
import hu.rayworks.vizit.ui.design.components.VizitButton
import hu.rayworks.vizit.ui.design.components.VizitDivider
import hu.rayworks.vizit.ui.design.components.VizitGroup
import hu.rayworks.vizit.ui.design.components.VizitRow
import hu.rayworks.vizit.ui.design.components.VizitSectionHeader

private enum class PortalPage { Home, Vosz, Education, Course, Help, Toolkit }

private data class PortalCard(
    val page: PortalPage,
    val title: String,
    val description: String,
    val eyebrow: String,
    val icon: ImageVector,
)

private data class BusinessResource(
    val title: String,
    val description: String,
    val url: String,
    val icon: ImageVector,
)

private data class BusinessCourse(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val duration: String,
    val level: String,
    val icon: ImageVector,
    val modules: List<String>,
)

private val portalCards = listOf(
    PortalCard(
        PortalPage.Vosz,
        "VOSZ",
        "Hírek, videók és vállalkozói szolgáltatások egy helyen.",
        "PARTNERI FORRÁSOK",
        Icons.Outlined.BusinessCenter,
    ),
    PortalCard(
        PortalPage.Education,
        "Vállalkozói Edukáció",
        "Gyakorlati kurzusok AI-ról, Microsoft 365-ről és cégépítésről.",
        "6 KURZUS",
        Icons.Outlined.School,
    ),
    PortalCard(
        PortalPage.Help,
        "Digitális segítség",
        "Próbáld ki, hogyan indul majd egy videós szakértői konzultáció.",
        "BEMUTATÓ MÓD",
        Icons.Outlined.VideoCall,
    ),
    PortalCard(
        PortalPage.Toolkit,
        "Vállalkozói eszköztár",
        "Gyors digitális állapotfelmérés és személyre szabott következő lépések.",
        "INTERAKTÍV",
        Icons.Outlined.Checklist,
    ),
)

private val businessResources = listOf(
    BusinessResource(
        "VOSZ videók",
        "Vállalkozói hírek, interjúk és gyakorlati videók.",
        "https://youtube.com/@vosz.?si=k2EmMlI8Q5ttlPZC",
        Icons.Outlined.PlayCircleOutline,
    ),
    BusinessResource(
        "VOSZ információk",
        "Érdekképviselet, tanácsadás, programok és aktuális hírek.",
        "https://www.vosz.hu/hu",
        Icons.Outlined.BusinessCenter,
    ),
    BusinessResource(
        "VOSZPort",
        "Digitális ügyintézési és tudásmegosztási felület.",
        "https://voszport.com/",
        Icons.Outlined.Language,
    ),
)

private val courses = listOf(
    BusinessCourse(
        "ai",
        "AI a mindennapi vállalkozásban",
        "Mesterséges intelligencia",
        "Használható promptok, automatizálási ötletek és felelős AI-használat.",
        "52 perc",
        "Kezdő",
        Icons.Outlined.SmartToy,
        listOf("Hol teremt értéket az AI?", "Jó prompt 5 lépésben", "Ajánlat és e-mail gyorsítása", "Adatvédelem és ellenőrzés"),
    ),
    BusinessCourse(
        "m365",
        "Microsoft 365 kisvállalkozásoknak",
        "Digitális munka",
        "Teams, Outlook, OneDrive és SharePoint egyszerű, biztonságos rendszerben.",
        "1 óra 18 perc",
        "Kezdő",
        Icons.Outlined.Cloud,
        listOf("Fiókok és jogosultságok", "Közös fájlkezelés", "Teams-együttműködés", "Naptár és automatizmusok"),
    ),
    BusinessCourse(
        "basics",
        "Vállalkozói alapismeretek",
        "Cégépítés",
        "Üzleti modell, célpiac, árazás és az első 90 nap terve.",
        "1 óra 05 perc",
        "Kezdő",
        Icons.Outlined.Storefront,
        listOf("Üzleti modell egy oldalon", "Ideális ügyfél", "Árképzési alapok", "90 napos akcióterv"),
    ),
    BusinessCourse(
        "security",
        "Kiberbiztonság emberi nyelven",
        "Biztonság",
        "Fiókvédelem, mentés, adathalászat és egy egyszerű incidens-terv.",
        "44 perc",
        "Kezdő",
        Icons.Outlined.Security,
        listOf("Többlépcsős belépés", "Biztonságos eszközök", "Adathalászat felismerése", "Mit tegyünk baj esetén?"),
    ),
    BusinessCourse(
        "marketing",
        "Online jelenlét és ügyfélszerzés",
        "Marketing",
        "Egyszerű pozicionálás, tartalomterv és mérhető kampányalapok.",
        "58 perc",
        "Középhaladó",
        Icons.Outlined.Campaign,
        listOf("Pozicionálási mondat", "Bizalmat építő profil", "4 hetes tartalomterv", "Mérés és javítás"),
    ),
    BusinessCourse(
        "finance",
        "Pénzügyi tudatosság alapjai",
        "Pénzügy",
        "Cash-flow, költségek és a könyvelővel való hatékony együttműködés.",
        "49 perc",
        "Kezdő",
        Icons.Outlined.AccountBalance,
        listOf("Bevétel nem egyenlő nyereség", "Cash-flow tábla", "Tartalék és tervezés", "Kérdések a könyvelőhöz"),
    ),
)

@Composable
fun BusinessHubScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var pageName by rememberSaveable { mutableStateOf(PortalPage.Home.name) }
    var selectedCourseId by rememberSaveable { mutableStateOf(courses.first().id) }
    val page = PortalPage.entries.firstOrNull { it.name == pageName } ?: PortalPage.Home
    val navigate: (PortalPage) -> Unit = { pageName = it.name }

    BackHandler {
        if (page == PortalPage.Home) onBack()
        else navigate(if (page == PortalPage.Course) PortalPage.Education else PortalPage.Home)
    }

    when (page) {
        PortalPage.Home -> PortalHome(onBack, navigate, modifier)
        PortalPage.Vosz -> VoszCenter({ navigate(PortalPage.Home) }, modifier)
        PortalPage.Education -> EducationCatalog(
            onBack = { navigate(PortalPage.Home) },
            onCourse = { selectedCourseId = it; navigate(PortalPage.Course) },
            modifier = modifier,
        )
        PortalPage.Course -> CourseDetail(
            course = courses.first { it.id == selectedCourseId },
            onBack = { navigate(PortalPage.Education) },
            modifier = modifier,
        )
        PortalPage.Help -> DigitalHelp({ navigate(PortalPage.Home) }, modifier)
        PortalPage.Toolkit -> BusinessToolkit({ navigate(PortalPage.Home) }, modifier)
    }
}

@Composable
private fun PortalHome(onBack: () -> Unit, navigate: (PortalPage) -> Unit, modifier: Modifier) {
    val colors = Vizit.colors
    LazyColumn(
        modifier = modifier.fillMaxSize().background(colors.canvas)
            .windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = Vizit.space.md),
        verticalArrangement = Arrangement.spacedBy(Vizit.space.md),
    ) {
        item { VizitBrandHeader(style = VizitBrandHeaderStyle.Compact, onBack = onBack) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Vizit.space.xs)) {
                Text("Vállalkozói Portál", style = Vizit.type.h1, color = colors.textPrimary)
                Text(
                    "Tanulás, hiteles források és digitális segítség a vállalkozásod következő lépéséhez.",
                    style = Vizit.type.body,
                    color = colors.textSecondary,
                )
            }
        }
        items(portalCards.chunked(2)) { rowCards ->
            Row(horizontalArrangement = Arrangement.spacedBy(Vizit.space.sm)) {
                rowCards.forEach { card ->
                    PortalFeatureCard(card, { navigate(card.page) }, Modifier.weight(1f))
                }
                if (rowCards.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        item {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(colors.primarySubtle, RoundedCornerShape(Vizit.radius.lg))
                    .padding(Vizit.space.md),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Vizit.space.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Lightbulb, null, tint = colors.primary)
                    Column {
                        Text("Heti fejlődési tipp", style = Vizit.type.label, color = colors.textPrimary)
                        Text(
                            "Válassz ki egy ismétlődő feladatot, és dokumentáld, mielőtt automatizálod.",
                            style = Vizit.type.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(Vizit.space.xxl)) }
    }
}

@Composable
private fun PortalFeatureCard(card: PortalCard, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Vizit.colors
    Card(
        onClick = onClick,
        modifier = modifier.height(214.dp),
        shape = RoundedCornerShape(Vizit.radius.lg),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.border),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(Vizit.space.md),
            verticalArrangement = Arrangement.spacedBy(Vizit.space.sm),
        ) {
            Box(
                modifier = Modifier.size(44.dp)
                    .background(colors.primarySubtle, RoundedCornerShape(Vizit.radius.md)),
                contentAlignment = Alignment.Center,
            ) { Icon(card.icon, null, tint = colors.primary) }
            Text(card.eyebrow, style = Vizit.type.overline, color = colors.primary)
            Text(
                card.title,
                style = Vizit.type.h3,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                card.description,
                style = Vizit.type.bodySmall,
                color = colors.textSecondary,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            Icon(Icons.Outlined.ArrowForward, null, tint = colors.primary, modifier = Modifier.align(Alignment.End))
        }
    }
}

@Composable
private fun PortalHeader(title: String, subtitle: String, onBack: () -> Unit) {
    val colors = Vizit.colors
    Column(verticalArrangement = Arrangement.spacedBy(Vizit.space.sm)) {
        VizitBrandHeader(style = VizitBrandHeaderStyle.Compact, onBack = onBack)
        Text(title, style = Vizit.type.h2, color = colors.textPrimary)
        Text(subtitle, style = Vizit.type.body, color = colors.textSecondary)
    }
}

@Composable
private fun VoszCenter(onBack: () -> Unit, modifier: Modifier) {
    val colors = Vizit.colors
    val uriHandler = LocalUriHandler.current
    LazyColumn(
        modifier = modifier.fillMaxSize().background(colors.canvas)
            .windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = Vizit.space.md),
        verticalArrangement = Arrangement.spacedBy(Vizit.space.md),
    ) {
        item {
            PortalHeader(
                "VOSZ forrásközpont",
                "Hivatalos vállalkozói tartalmak és szolgáltatások.",
                onBack,
            )
        }
        item {
            VizitGroup {
                businessResources.forEachIndexed { index, resource ->
                    if (index > 0) VizitDivider()
                    VizitRow(
                        label = resource.title,
                        icon = resource.icon,
                        supporting = resource.description,
                        onClick = { runCatching { uriHandler.openUri(resource.url) } },
                    )
                }
            }
        }
        item {
            Text(
                "A hivatkozások külső oldalakra vezetnek; azok tartalmáért az adott szolgáltató felel.",
                style = Vizit.type.bodySmall,
                color = colors.textMuted,
            )
        }
    }
}

@Composable
private fun EducationCatalog(onBack: () -> Unit, onCourse: (String) -> Unit, modifier: Modifier) {
    val colors = Vizit.colors
    var selectedCategory by rememberSaveable { mutableStateOf("Mind") }
    val categories = listOf("Mind", "AI", "Digitális munka", "Cégépítés", "Biztonság")
    val filtered = courses.filter {
        selectedCategory == "Mind" ||
            (selectedCategory == "AI" && it.id == "ai") ||
            it.category == selectedCategory
    }
    LazyColumn(
        modifier = modifier.fillMaxSize().background(colors.canvas)
            .windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = Vizit.space.md),
        verticalArrangement = Arrangement.spacedBy(Vizit.space.md),
    ) {
        item {
            PortalHeader(
                "Vállalkozói Edukáció",
                "Rövid, gyakorlatias tananyagok, saját tempóban.",
                onBack,
            )
        }
        item {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Vizit.space.xs),
            ) {
                categories.forEach { category ->
                    AssistChip(
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        leadingIcon = if (selectedCategory == category) {
                            { Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp)) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
        items(filtered, key = { it.id }) { course -> CourseCard(course) { onCourse(course.id) } }
        item { Spacer(Modifier.height(Vizit.space.xxl)) }
    }
}

@Composable
private fun CourseCard(course: BusinessCourse, onClick: () -> Unit) {
    val colors = Vizit.colors
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(Vizit.radius.lg),
        border = BorderStroke(1.dp, colors.border),
    ) {
        Row(Modifier.padding(Vizit.space.md), horizontalArrangement = Arrangement.spacedBy(Vizit.space.md)) {
            Box(
                Modifier.size(64.dp).background(colors.primarySubtle, RoundedCornerShape(Vizit.radius.md)),
                contentAlignment = Alignment.Center,
            ) { Icon(course.icon, null, tint = colors.primary, modifier = Modifier.size(30.dp)) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Vizit.space.xxs)) {
                Text(course.category.uppercase(), style = Vizit.type.overline, color = colors.primary)
                Text(course.title, style = Vizit.type.h3, color = colors.textPrimary)
                Text(
                    course.description,
                    style = Vizit.type.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${course.duration}  ·  ${course.level}  ·  ${course.modules.size} lecke",
                    style = Vizit.type.caption,
                    color = colors.textMuted,
                )
            }
        }
    }
}

@Composable
private fun CourseDetail(course: BusinessCourse, onBack: () -> Unit, modifier: Modifier) {
    val colors = Vizit.colors
    var completed by rememberSaveable(course.id) { mutableStateOf(emptyList<Int>()) }
    LazyColumn(
        modifier = modifier.fillMaxSize().background(colors.canvas)
            .windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = Vizit.space.md),
        verticalArrangement = Arrangement.spacedBy(Vizit.space.md),
    ) {
        item { PortalHeader(course.title, course.description, onBack) }
        item {
            VizitGroup {
                VizitRow(
                    label = "${course.duration} · ${course.level}",
                    icon = course.icon,
                    supporting = "${course.modules.size} rövid, egymásra épülő lecke",
                    showChevron = false,
                )
            }
        }
        item {
            LinearProgressIndicator(
                progress = { completed.size.toFloat() / course.modules.size },
                modifier = Modifier.fillMaxWidth(),
                color = colors.primary,
                trackColor = colors.controlTrack,
            )
        }
        item { VizitSectionHeader("Kurzus tartalma") }
        items(course.modules.indices.toList()) { index ->
            val isComplete = index in completed
            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    completed = if (isComplete) completed - index else (completed + index).distinct()
                },
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = BorderStroke(1.dp, colors.border),
            ) {
                Row(
                    Modifier.padding(Vizit.space.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Vizit.space.sm),
                ) {
                    Icon(
                        if (isComplete) Icons.Outlined.CheckCircle else Icons.Outlined.PlayArrow,
                        null,
                        tint = if (isComplete) colors.success else colors.primary,
                    )
                    Column(Modifier.weight(1f)) {
                        Text("${index + 1}. lecke", style = Vizit.type.caption, color = colors.textMuted)
                        Text(course.modules[index], style = Vizit.type.body, color = colors.textPrimary)
                    }
                    Text(
                        if (isComplete) "Kész" else "Megnyitás",
                        style = Vizit.type.caption,
                        color = if (isComplete) colors.success else colors.primary,
                    )
                }
            }
        }
        item {
            Text(
                "A 6.0 bétában a tanulási felület és a haladás kipróbálható; a teljes videótananyagok fokozatosan érkeznek.",
                style = Vizit.type.bodySmall,
                color = colors.textMuted,
            )
        }
    }
}

@Composable
private fun DigitalHelp(onBack: () -> Unit, modifier: Modifier) {
    val colors = Vizit.colors
    var inCall by rememberSaveable { mutableStateOf(false) }
    var microphone by rememberSaveable { mutableStateOf(true) }
    var camera by rememberSaveable { mutableStateOf(true) }
    Column(
        modifier = modifier.fillMaxSize().background(colors.canvas)
            .windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = Vizit.space.md),
        verticalArrangement = Arrangement.spacedBy(Vizit.space.md),
    ) {
        PortalHeader(
            "Digitális segítség",
            "Szakértői videókonzultáció élményének interaktív előnézete.",
            onBack,
        )
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(0.82f)
                .background(colors.ink, RoundedCornerShape(Vizit.radius.xl))
                .border(1.dp, colors.borderStrong, RoundedCornerShape(Vizit.radius.xl)),
        ) {
            Column(
                Modifier.fillMaxSize().padding(Vizit.space.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        if (inCall) "KAPCSOLÓDVA · DEMÓ" else "BEMUTATÓ MÓD",
                        style = Vizit.type.overline,
                        color = Color.White,
                    )
                    Text(
                        if (inCall) "00:24" else "ELŐNÉZET",
                        style = Vizit.type.caption,
                        color = Color.White.copy(alpha = .72f),
                    )
                }
                Box(
                    Modifier.size(116.dp).background(colors.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Person,
                        "Digitális tanácsadó",
                        tint = Color.White,
                        modifier = Modifier.size(58.dp),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (inCall) "VIZIT digitális tanácsadó" else "Próbahívás",
                        style = Vizit.type.h3,
                        color = Color.White,
                    )
                    Text(
                        if (inCall) "A kapcsolat bemutató módban fut" else "Ellenőrizd a kamerát és a mikrofont",
                        style = Vizit.type.bodySmall,
                        color = Color.White.copy(alpha = .72f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Vizit.space.md)) {
                    CallControl(if (microphone) Icons.Outlined.Mic else Icons.Outlined.MicOff, "Mikrofon") {
                        microphone = !microphone
                    }
                    CallControl(if (camera) Icons.Outlined.Videocam else Icons.Outlined.VideocamOff, "Kamera") {
                        camera = !camera
                    }
                    if (inCall) CallControl(Icons.Outlined.CallEnd, "Befejezés", colors.error) { inCall = false }
                }
            }
        }
        if (!inCall) {
            VizitButton(
                "Próbahívás indítása",
                { inCall = true },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Outlined.VideoCall,
            )
        }
        Text(
            "Ez a 6.0 verzió interaktív bemutatója: nem kapcsol valódi tanácsadóhoz és nem továbbít hangot vagy videót.",
            style = Vizit.type.bodySmall,
            color = colors.textMuted,
        )
    }
}

@Composable
private fun CallControl(icon: ImageVector, label: String, tint: Color = Color.White, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(52.dp).background(Color.White.copy(alpha = .14f), CircleShape),
        ) { Icon(icon, label, tint = tint) }
        Text(label, style = Vizit.type.caption, color = Color.White.copy(alpha = .8f))
    }
}

@Composable
private fun BusinessToolkit(onBack: () -> Unit, modifier: Modifier) {
    val colors = Vizit.colors
    val steps = remember {
        listOf(
            "Van professzionális céges e-mail-címem",
            "Minden fontos fiókon bekapcsoltam a kétlépcsős belépést",
            "Rendszeres biztonsági mentésem van",
            "Az ügyféladataimat egy helyen kezelem",
            "Mérem, honnan érkeznek az érdeklődők",
            "Van leírt heti és havi munkafolyamatom",
        )
    }
    var checked by rememberSaveable { mutableStateOf(emptyList<Int>()) }
    val score = checked.size * 100 / steps.size
    LazyColumn(
        modifier = modifier.fillMaxSize().background(colors.canvas)
            .windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = Vizit.space.md),
        verticalArrangement = Arrangement.spacedBy(Vizit.space.md),
    ) {
        item {
            PortalHeader(
                "Vállalkozói eszköztár",
                "Jelöld, ami már rendben van, és kapsz egy gyors következő lépést.",
                onBack,
            )
        }
        item {
            Box(
                Modifier.fillMaxWidth().background(colors.primarySubtle, RoundedCornerShape(Vizit.radius.lg))
                    .padding(Vizit.space.lg),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Vizit.space.md),
                ) {
                    Box(
                        Modifier.size(68.dp).background(colors.primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text("$score%", style = Vizit.type.h3, color = colors.onPrimary) }
                    Column {
                        Text("Digitális felkészültség", style = Vizit.type.h3, color = colors.textPrimary)
                        Text("${checked.size}/${steps.size} alap rendben", style = Vizit.type.bodySmall, color = colors.textSecondary)
                    }
                }
            }
        }
        items(steps.indices.toList()) { index ->
            val selected = index in checked
            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    checked = if (selected) checked - index else checked + index
                },
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = BorderStroke(1.dp, colors.border),
            ) {
                Row(
                    Modifier.padding(Vizit.space.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Vizit.space.sm),
                ) {
                    Icon(
                        if (selected) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        null,
                        tint = if (selected) colors.success else colors.textMuted,
                    )
                    Text(
                        steps[index],
                        style = Vizit.type.body,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        item {
            VizitGroup {
                VizitRow(
                    label = if (score < 50) "Következő lépés: biztos alapok" else "Következő lépés: automatizálás",
                    icon = Icons.Outlined.RocketLaunch,
                    supporting = if (score < 50) {
                        "Kezdd a fiókvédelemmel és a mentéssel, majd haladj tovább."
                    } else {
                        "Válassz egy ismétlődő folyamatot, és készíts hozzá egyszerű sablont."
                    },
                    showChevron = false,
                )
            }
        }
        item { Spacer(Modifier.height(Vizit.space.xxl)) }
    }
}
