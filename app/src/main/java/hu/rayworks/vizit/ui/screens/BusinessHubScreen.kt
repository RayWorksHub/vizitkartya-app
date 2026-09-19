package hu.rayworks.vizit.ui.screens

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import hu.rayworks.vizit.ui.design.Vizit
import hu.rayworks.vizit.ui.design.components.VizitBrandHeader
import hu.rayworks.vizit.ui.design.components.VizitBrandHeaderStyle
import hu.rayworks.vizit.ui.design.components.VizitButton
import hu.rayworks.vizit.ui.design.components.VizitButtonStyle
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
    val videoTitle: String,
    val videoSource: String,
    val videoUrl: String,
    val modules: List<CourseModule>,
)

private data class CourseModule(
    val title: String,
    val summary: String,
    val resourceTitle: String,
    val resourceUrl: String,
)

private data class GuideLink(
    val title: String,
    val description: String,
    val url: String,
)

private data class ToolkitGuide(
    val id: String,
    val title: String,
    val description: String,
    val result: String,
    val icon: ImageVector,
    val steps: List<String>,
    val links: List<GuideLink>,
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
        "Magyar videó",
        "Kezdő",
        Icons.Outlined.SmartToy,
        "ChatGPT képzés – Promptolási technikák",
        "VOSZ",
        "https://www.youtube.com/watch?v=uPYOrXxTxJI",
        listOf(
            CourseModule(
                "Hol teremt értéket az AI?",
                "Gyűjts össze három ismétlődő szöveges feladatot, és válaszd ki azt, amelyiknél a legkisebb a hibakockázat.",
                "VOSZ AI-videósorozat",
                "https://www.youtube.com/results?search_query=VOSZ+ChatGPT+k%C3%A9pz%C3%A9s",
            ),
            CourseModule(
                "Jó prompt 5 lépésben",
                "Add meg a szerepet, a célt, a bemenetet, a korlátokat és a kívánt kimeneti formátumot.",
                "Promptolási videó megnyitása",
                "https://www.youtube.com/watch?v=uPYOrXxTxJI",
            ),
            CourseModule(
                "Ajánlat és e-mail gyorsítása",
                "Készíts ellenőrzött sablont ajánlatra, utánkövetésre és ügyfélválaszra; érzékeny adatot ne másolj be.",
                "Microsoft Copilot vállalkozásoknak",
                "https://www.microsoft.com/hu-hu/microsoft-365/business/copilot-for-microsoft-365",
            ),
            CourseModule(
                "Ellenőrzés és adatvédelem",
                "Minden AI-kimenetet ember ellenőrizzen, és legyen belső szabály arra, milyen adat kerülhet a rendszerbe.",
                "NAIH tájékoztatók",
                "https://www.naih.hu/",
            ),
        ),
    ),
    BusinessCourse(
        "m365",
        "Microsoft 365 kisvállalkozásoknak",
        "Digitális munka",
        "Teams, Outlook, OneDrive és SharePoint egyszerű, biztonságos rendszerben.",
        "Magyar videók",
        "Kezdő",
        Icons.Outlined.Cloud,
        "Microsoft 365 bevezetés és csoportok",
        "Sämling Üzleti Oktatási Központ",
        "https://www.youtube.com/watch?v=py9fGXyBZcE",
        listOf(
            CourseModule(
                "Fiókok és jogosultságok",
                "Minden munkatársnak külön fiók, szerepkör szerinti hozzáférés és bekapcsolt többtényezős védelem kell.",
                "Microsoft 365 Vállalati verzió",
                "https://www.microsoft.com/hu-hu/microsoft-365/business",
            ),
            CourseModule(
                "Közös fájlkezelés OneDrive-val",
                "Alakíts ki közös mappaszerkezetet, tulajdonost és visszaállítási rendet; ne e-mailben küldözgess fájlmásolatokat.",
                "OneDrive magyar súgó",
                "https://support.microsoft.com/hu-hu/onedrive",
            ),
            CourseModule(
                "Teams-együttműködés",
                "Hozz létre ügyfél- vagy projektcsatornákat, és rögzítsd, melyik információ hol található.",
                "Teams magyar súgó",
                "https://support.microsoft.com/hu-hu/teams",
            ),
            CourseModule(
                "Naptár és automatizmusok",
                "Használj közös naptárt, foglalási oldalt és egyetlen jóváhagyott automatizmust egy ismétlődő folyamathoz.",
                "Power Automate",
                "https://www.microsoft.com/hu-hu/power-platform/products/power-automate",
            ),
        ),
    ),
    BusinessCourse(
        "basics",
        "Vállalkozói alapismeretek",
        "Cégépítés",
        "Üzleti modell, célpiac, árazás és az első 90 nap terve.",
        "Magyar videó",
        "Kezdő",
        Icons.Outlined.Storefront,
        "Az egyéni vállalkozás és indítása",
        "Magyar nyelvű oktatóvideó",
        "https://www.youtube.com/watch?v=d2tyNSnyv1Q",
        listOf(
            CourseModule(
                "Üzleti modell egy oldalon",
                "Írd le egy mondatban a vevőt, a problémáját, az ajánlatodat, az értékesítési csatornát és a bevételi módot.",
                "VOSZ vállalkozói információk",
                "https://www.vosz.hu/hu",
            ),
            CourseModule(
                "Indítás és kötelező lépések",
                "Ellenőrizd a tevékenységet, adózást, képesítési feltételt, kamarai bejelentést és számlázást.",
                "NAV: egyéni vállalkozás indítása",
                "https://nav.gov.hu/Elethelyzetek-adozasa/vallalkozas/Egyeni-vallalkozas-inditasa",
            ),
            CourseModule(
                "Ideális ügyfél és árazás",
                "Határozz meg egy konkrét célcsoportot, eredményt, költségszintet és minimális vállalható árat.",
                "MKIK Mentorprogram",
                "https://vallalkozztudatosan.mkik.hu/",
            ),
            CourseModule(
                "90 napos akcióterv",
                "Bonts három havi célra, heti mérőszámokra és minden héten egy lezárandó ügyfélszerzési feladatra.",
                "Vállalkozz digitálisan",
                "https://vallalkozzdigitalisan.mkik.hu/",
            ),
        ),
    ),
    BusinessCourse(
        "security",
        "Kiberbiztonság emberi nyelven",
        "Biztonság",
        "Fiókvédelem, mentés, adathalászat és egy egyszerű incidens-terv.",
        "Magyar videó",
        "Kezdő",
        Icons.Outlined.Security,
        "KiberPajzs – digitális biztonság",
        "Pénziránytű / KiberPajzs",
        "https://www.youtube.com/watch?v=2LqpB_03Jt0",
        listOf(
            CourseModule(
                "Többlépcsős belépés",
                "Kapcsold be először az e-mail-, banki, közösségi és adminisztrációs fiókoknál.",
                "KiberPajzs biztonsági tippek",
                "https://kiberpajzs.hu/hasznos-tippeket-olvasnek",
            ),
            CourseModule(
                "Eszközök és mentés",
                "Automatikus frissítés, képernyőzár és legalább egy külön helyen tárolt, visszaállítással is tesztelt mentés kell.",
                "NKI tudásbázis",
                "https://nki.gov.hu/",
            ),
            CourseModule(
                "Adathalászat felismerése",
                "Sürgetésnél állj meg, külön csatornán ellenőrizd a feladót, és ne a levélből nyisd meg a belépési oldalt.",
                "KiberPajzs videó",
                "https://www.youtube.com/watch?v=2LqpB_03Jt0",
            ),
            CourseModule(
                "Incidens-terv",
                "Írd le, kit kell hívni, hogyan zárod a fiókokat, hol van a mentés, és hogyan értesíted az érintetteket.",
                "NKI incidensbejelentés",
                "https://nki.gov.hu/intezet/tartalom/incidens-bejelentes/",
            ),
        ),
    ),
    BusinessCourse(
        "marketing",
        "Online jelenlét és ügyfélszerzés",
        "Marketing",
        "Egyszerű pozicionálás, tartalomterv és mérhető kampányalapok.",
        "Magyar videók",
        "Középhaladó",
        Icons.Outlined.Campaign,
        "Hogyan kerülhetsz fel a Google Térképre?",
        "Jobbágy András",
        "https://www.youtube.com/watch?v=-4qATDuCWgU",
        listOf(
            CourseModule(
                "Pozicionálási mondat",
                "Ne szolgáltatást sorolj: mondd meg, kinek, milyen eredményt és mitől más módon adsz.",
                "VOSZ videók",
                "https://www.youtube.com/@vosz.",
            ),
            CourseModule(
                "Bizalmat építő Google Cégprofil",
                "Tölts ki minden adatot, adj képeket, szolgáltatásokat és rendszeresen válaszolj az értékelésekre.",
                "Google Cégprofil hozzáadása",
                "https://support.google.com/business/answer/2911778?hl=hu",
            ),
            CourseModule(
                "4 hetes tartalomterv",
                "Hetente mutass problémát, megoldást, bizonyítékot és konkrét következő lépést.",
                "Meta Business Suite",
                "https://business.facebook.com/",
            ),
            CourseModule(
                "Mérés és javítás",
                "Mérd a forrást, érdeklődést, ajánlatot és vásárlást; a követőszám önmagában nem üzleti eredmény.",
                "Google Analytics",
                "https://analytics.google.com/",
            ),
        ),
    ),
    BusinessCourse(
        "finance",
        "Pénzügyi tudatosság alapjai",
        "Pénzügy",
        "Cash-flow, költségek és a könyvelővel való hatékony együttműködés.",
        "Magyar videó",
        "Kezdő",
        Icons.Outlined.AccountBalance,
        "NAV Online Számlázó Program bemutatása",
        "Nemzeti Adó- és Vámhivatal",
        "https://www.youtube.com/watch?v=U5s2bXrgnHc",
        listOf(
            CourseModule(
                "Bevétel, költség és nyereség",
                "Külön kezeld a beérkezett pénzt, az áfát, a fizetendő adót, a költségeket és a tulajdonosi kivétet.",
                "NAV vállalkozói élethelyzetek",
                "https://nav.gov.hu/Elethelyzetek-adozasa/vallalkozas",
            ),
            CourseModule(
                "13 hetes cash-flow",
                "Hetente vezesd a várható be- és kifizetéseket, és jelöld a bizonytalan tételeket.",
                "Pénziránytű",
                "https://penziranytu.hu/",
            ),
            CourseModule(
                "Számlázás és adatszolgáltatás",
                "Ellenőrizd a NAV-kapcsolatot, a számlaadatokat és azt, hogy ki figyeli a hibás adatszolgáltatást.",
                "NAV Online Számla",
                "https://onlineszamla.nav.gov.hu/",
            ),
            CourseModule(
                "Havi zárás a könyvelővel",
                "Legyen fix határidő a bizonylatokra, kintlévőségekre, adókra és a következő három hónap pénzigényére.",
                "NAV ONYA",
                "https://onya.nav.gov.hu/",
            ),
        ),
    ),
)

private val toolkitGuides = listOf(
    ToolkitGuide(
        "launch",
        "Vállalkozás indítása",
        "Az ötlettől a jogszerű indulásig, kihagyott kötelező lépések nélkül.",
        "Működő vállalkozói státusz és rendezett alapadatok.",
        Icons.Outlined.Storefront,
        listOf(
            "Írd le a tevékenységet és ellenőrizd, kell-e képesítés vagy engedély.",
            "Könyvelővel válassz vállalkozási formát és adózást még a bejelentés előtt.",
            "Indítsd el az egyéni vállalkozást a NAV Vállalkozói Ügysegédjén vagy intézd a cégalapítást szakértővel.",
            "Jelentkezz be az illetékes gazdasági kamarához a NAV által jelzett határidőn belül.",
            "Állíts be számlázást, vállalkozói bankszámlát és iratmegőrzési rendet.",
        ),
        listOf(
            GuideLink("NAV indítási útmutató", "Hivatalos, naprakész lépések és feltételek.", "https://nav.gov.hu/Elethelyzetek-adozasa/vallalkozas/Egyeni-vallalkozas-inditasa"),
            GuideLink("NAV Vállalkozói Ügysegéd", "Az online bejelentés belépési pontja.", "https://ugyfelportal.nav.gov.hu/"),
            GuideLink("MKIK", "Kamarai információk és területi kamarák.", "https://mkik.hu/"),
        ),
    ),
    ToolkitGuide(
        "billing",
        "Számlázás és NAV-ügyintézés",
        "A számla kiállításától a bevallási feladatok követéséig.",
        "Ellenőrizhető számlázási folyamat és kevesebb adminisztrációs hiba.",
        Icons.Outlined.AccountBalance,
        listOf(
            "Regisztrálj a NAV Online Számla rendszerébe és rögzíts technikai felhasználót, ha a program kéri.",
            "Válassz NAV-kapcsolatos számlázót, majd állíts ki és ellenőrizz egy tesztszámlát.",
            "Rögzíts heti rutint a hibás adatszolgáltatások és a kintlévőségek ellenőrzésére.",
            "Egyeztess a könyvelővel dokumentumleadási határidőt és felelőst.",
        ),
        listOf(
            GuideLink("NAV Online Számla", "Regisztráció, számlák és adatszolgáltatási hibák.", "https://onlineszamla.nav.gov.hu/"),
            GuideLink("NAV ONYA", "Online nyomtatványok és bejelentések.", "https://onya.nav.gov.hu/"),
            GuideLink("e-Beszámoló", "Közzétett éves beszámolók hivatalos keresője.", "https://e-beszamolo.im.gov.hu/"),
        ),
    ),
    ToolkitGuide(
        "office",
        "Digitális iroda kialakítása",
        "Céges e-mail, közös dokumentumok és világos hozzáférések.",
        "Egy helyen megtalálható fájlok és átadható működés.",
        Icons.Outlined.Cloud,
        listOf(
            "Használj saját domaines céges e-mail-címet, ne közös jelszóval használt postafiókot.",
            "Hozz létre egységes mappaszerkezetet ügyfelekre, pénzügyre és belső működésre.",
            "Adj személyenként jogosultságot, és távozáskor azonnal vond vissza.",
            "Kapcsold be a verziókövetést, mentést és teszteld egy fájl visszaállítását.",
        ),
        listOf(
            GuideLink("Microsoft 365 vállalkozásoknak", "Outlook, Teams, OneDrive és irodai alkalmazások.", "https://www.microsoft.com/hu-hu/microsoft-365/business"),
            GuideLink("Google Workspace", "Céges Gmail, Drive, Meet és közös munka.", "https://workspace.google.com/intl/hu/"),
            GuideLink("Vállalkozz digitálisan", "MKIK digitális megoldások és segítség.", "https://vallalkozzdigitalisan.mkik.hu/"),
        ),
    ),
    ToolkitGuide(
        "security",
        "Biztonság és adatvédelem",
        "A leggyakoribb fiók-, adat- és csalási kockázatok kezelése.",
        "Védett fiókok, működő mentés és leírt incidensfolyamat.",
        Icons.Outlined.Security,
        listOf(
            "Kapcsold be a többtényezős azonosítást az e-mailen, bankon, közösségi és adminfiókokon.",
            "Használj jelszókezelőt, és szüntesd meg a közös vagy újrahasznált jelszavakat.",
            "Készíts automatikus mentést külön helyre, majd próbáld visszaállítani.",
            "Írd le, ki mit tesz csalás, elveszett eszköz vagy adatszivárgás esetén.",
            "Tarts naprakész adatkezelési tájékoztatót, és csak szükséges ügyféladatot gyűjts.",
        ),
        listOf(
            GuideLink("KiberPajzs", "Magyar csalásmegelőzési és digitális biztonsági tippek.", "https://kiberpajzs.hu/hasznos-tippeket-olvasnek"),
            GuideLink("Nemzeti Kibervédelmi Intézet", "Riasztások, tudásanyag és incidensbejelentés.", "https://nki.gov.hu/"),
            GuideLink("NAIH", "Hivatalos adatvédelmi tájékoztatók és ügyintézés.", "https://www.naih.hu/"),
        ),
    ),
    ToolkitGuide(
        "sales",
        "Online jelenlét és ügyfélszerzés",
        "Megtalálható profilok, egyértelmű ajánlat és mérhető érdeklődők.",
        "Friss online jelenlét és követhető ügyfélszerzési tölcsér.",
        Icons.Outlined.Campaign,
        listOf(
            "Fogalmazd meg egy mondatban, kinek milyen eredményt adsz.",
            "Igényeld és töltsd ki a Google Cégprofilt, ha helyi vagy személyes szolgáltatást végzel.",
            "Válassz legfeljebb két aktív közösségi csatornát, és legyen minden felületen egyértelmű kapcsolatfelvétel.",
            "Rögzítsd minden érdeklődő forrását és a következő lépését.",
            "Havonta értékeld az érdeklődő, ajánlat és vásárlás számát.",
        ),
        listOf(
            GuideLink("Google Cégprofil létrehozása", "Ingyenes megjelenés a Google Keresőben és Térképen.", "https://support.google.com/business/answer/2911778?hl=hu"),
            GuideLink("Meta Business Suite", "Facebook- és Instagram-oldalak kezelése.", "https://business.facebook.com/"),
            GuideLink("Google Analytics", "Webes forgalom és konverziók mérése.", "https://analytics.google.com/"),
        ),
    ),
    ToolkitGuide(
        "growth",
        "Mentor, digitalizáció és fejlődés",
        "Hiteles segítség, partnerkapcsolatok és fejlesztési programok keresése.",
        "Kiválasztott fejlesztési cél és konkrét jelentkezési következő lépés.",
        Icons.Outlined.RocketLaunch,
        listOf(
            "Válassz egy 90 napos fejlesztési célt: értékesítés, digitalizáció, export vagy működés.",
            "Készíts egyoldalas helyzetképet a számokról, problémáról és elvárt eredményről.",
            "Keress hozzá szakmai szervezetet, mentort vagy minősített digitális szolgáltatót.",
            "Jelölj ki felelőst, határidőt és egy mérőszámot.",
        ),
        listOf(
            GuideLink("VOSZ", "Érdekképviselet, tanácsadás, programok és vállalkozói hírek.", "https://www.vosz.hu/hu"),
            GuideLink("MKIK Mentorprogram", "Gyakorlati mentorálás növekedéshez és külpiachoz.", "https://vallalkozztudatosan.mkik.hu/"),
            GuideLink("Vállalkozz digitálisan", "Digitális fejlesztési tudás és megoldások.", "https://vallalkozzdigitalisan.mkik.hu/"),
        ),
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
                    "${course.duration}  ·  ${course.level}  ·  ${(course.modules.size + 1) / 2} modul  ·  ${course.modules.size} lecke",
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
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val preferences = remember { context.applicationContext.getSharedPreferences("vizit_education", 0) }
    var completed by remember(course.id) {
        mutableStateOf(preferences.getStringSet("completed_lessons", emptySet())?.toSet() ?: emptySet())
    }
    var selectedLessonIndex by rememberSaveable(course.id) { mutableStateOf(0) }
    val selectedLesson = course.modules[selectedLessonIndex]
    val learningModules = course.modules.chunked(2)
    val courseCompleted = completed.intersect(course.modules.indices.map { "${course.id}:$it" }.toSet()).size

    fun lessonKey(index: Int) = "${course.id}:$index"
    fun markSelectedComplete() {
        val next = completed + lessonKey(selectedLessonIndex)
        completed = next
        preferences.edit().putStringSet("completed_lessons", next).apply()
    }

    val selectedVideoUrl = if (youtubeVideoId(selectedLesson.resourceUrl) != null) {
        selectedLesson.resourceUrl
    } else {
        course.videoUrl
    }
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
                    supporting = "${learningModules.size} modul · ${course.modules.size} videólecke",
                    showChevron = false,
                )
            }
        }
        item {
            LinearProgressIndicator(
                progress = { courseCompleted.toFloat() / course.modules.size },
                modifier = Modifier.fillMaxWidth(),
                color = colors.primary,
                trackColor = colors.controlTrack,
            )
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(Vizit.radius.lg),
                border = BorderStroke(1.dp, colors.border),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(Vizit.space.md),
                    verticalArrangement = Arrangement.spacedBy(Vizit.space.sm),
                ) {
                    YouTubeLessonPlayer(selectedVideoUrl, selectedLessonIndex) { markSelectedComplete() }
                    Text("AKTUÁLIS LECKE", style = Vizit.type.overline, color = colors.primary)
                    Text(selectedLesson.title, style = Vizit.type.h3, color = colors.textPrimary)
                    Text(selectedLesson.summary, style = Vizit.type.bodySmall, color = colors.textSecondary)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Vizit.space.xs),
                    ) {
                        val isComplete = lessonKey(selectedLessonIndex) in completed
                        Icon(
                            if (isComplete) Icons.Outlined.CheckCircle else Icons.Outlined.PlayCircleOutline,
                            null,
                            tint = if (isComplete) colors.success else colors.textMuted,
                        )
                        Text(
                            if (isComplete) "Lecke teljesítve" else "A videó legalább 90%-ának lejátszása után lesz kész",
                            style = Vizit.type.caption,
                            color = if (isComplete) colors.success else colors.textMuted,
                        )
                    }
                    if (selectedLesson.resourceUrl != selectedVideoUrl) {
                        VizitButton(
                            selectedLesson.resourceTitle,
                            { runCatching { uriHandler.openUri(selectedLesson.resourceUrl) } },
                            modifier = Modifier.fillMaxWidth(),
                            style = VizitButtonStyle.Secondary,
                            icon = Icons.Outlined.Language,
                        )
                    }
                }
            }
        }
        item { VizitSectionHeader("Tananyag") }
        items(learningModules.indices.toList()) { moduleIndex ->
            val moduleLessons = learningModules[moduleIndex]
            val firstLessonIndex = moduleIndex * 2
            val moduleComplete = moduleLessons.indices.all { lessonKey(firstLessonIndex + it) in completed }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = BorderStroke(1.dp, colors.border),
            ) {
                Column(
                    Modifier.padding(Vizit.space.md),
                    verticalArrangement = Arrangement.spacedBy(Vizit.space.sm),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Vizit.space.sm),
                    ) {
                        Icon(
                            if (moduleComplete) Icons.Outlined.CheckCircle else Icons.Outlined.School,
                            null,
                            tint = if (moduleComplete) colors.success else colors.primary,
                        )
                        Column(Modifier.weight(1f)) {
                            Text("${moduleIndex + 1}. MODUL", style = Vizit.type.overline, color = colors.textMuted)
                            Text(
                                if (moduleIndex == 0) "Alapok és felkészülés" else "Gyakorlati alkalmazás",
                                style = Vizit.type.h3,
                                color = colors.textPrimary,
                            )
                        }
                        Text(
                            "${moduleLessons.indices.count { lessonKey(firstLessonIndex + it) in completed }}/${moduleLessons.size}",
                            style = Vizit.type.caption,
                            color = colors.textMuted,
                        )
                    }
                    moduleLessons.forEachIndexed { lessonIndex, lesson ->
                        if (lessonIndex > 0) VizitDivider()
                        val absoluteIndex = firstLessonIndex + lessonIndex
                        val isComplete = lessonKey(absoluteIndex) in completed
                        VizitRow(
                            label = lesson.title,
                            icon = if (isComplete) Icons.Outlined.CheckCircle else Icons.Outlined.PlayCircleOutline,
                            supporting = "${moduleIndex + 1}.${lessonIndex + 1} · Videólecke",
                            onClick = { selectedLessonIndex = absoluteIndex },
                        )
                    }
                }
            }
        }
        item {
            Text(
                "A kész állapotot az alkalmazás automatikusan rögzíti a videó legalább 90%-ának tényleges lejátszása után. Kézzel nem módosítható.",
                style = Vizit.type.bodySmall,
                color = colors.textMuted,
            )
        }
    }
}

private fun youtubeVideoId(url: String): String? {
    val short = Regex("youtu\\.be/([A-Za-z0-9_-]{6,})").find(url)?.groupValues?.getOrNull(1)
    if (short != null) return short
    return Regex("[?&]v=([A-Za-z0-9_-]{6,})").find(url)?.groupValues?.getOrNull(1)
        ?: Regex("youtube\\.com/embed/([A-Za-z0-9_-]{6,})").find(url)?.groupValues?.getOrNull(1)
}

private class LessonCompletionBridge(private val onCompleted: () -> Unit) {
    @JavascriptInterface
    fun completed() {
        Handler(Looper.getMainLooper()).post { onCompleted() }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YouTubeLessonPlayer(url: String, lessonIndex: Int, onCompleted: () -> Unit) {
    val videoId = youtubeVideoId(url) ?: return
    key(videoId, lessonIndex) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = true
                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()
                    addJavascriptInterface(LessonCompletionBridge(onCompleted), "VizitLesson")
                    loadDataWithBaseURL(
                        "https://www.youtube-nocookie.com",
                        youtubePlayerHtml(videoId),
                        "text/html",
                        "UTF-8",
                        null,
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
        )
    }
}

private fun youtubePlayerHtml(videoId: String): String = """
    <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1">
    <style>html,body,#player{margin:0;width:100%;height:100%;background:#000;overflow:hidden}</style></head>
    <body><div id="player"></div><script src="https://www.youtube.com/iframe_api"></script><script>
    var player, watched=0, tick=0;
    function onYouTubeIframeAPIReady(){ player=new YT.Player('player',{videoId:'$videoId',playerVars:{playsinline:1,rel:0},events:{onStateChange:onState}}); }
    function onState(e){
      if(e.data===YT.PlayerState.PLAYING && !tick){tick=setInterval(function(){watched+=1;},1000);}
      if(e.data!==YT.PlayerState.PLAYING && tick){clearInterval(tick);tick=0;}
      if(e.data===YT.PlayerState.ENDED){var d=player.getDuration();if(d>0 && watched/d>=0.9){VizitLesson.completed();}}
    }
    </script></body></html>
""".trimIndent()

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
            "Ez a 6.1 verzió interaktív bemutatója: nem kapcsol valódi tanácsadóhoz és nem továbbít hangot vagy videót.",
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
    val uriHandler = LocalUriHandler.current
    var completedGuideIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val score = completedGuideIds.size * 100 / toolkitGuides.size
    LazyColumn(
        modifier = modifier.fillMaxSize().background(colors.canvas)
            .windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = Vizit.space.md),
        verticalArrangement = Arrangement.spacedBy(Vizit.space.md),
    ) {
        item {
            PortalHeader(
                "Vállalkozói eszköztár",
                "Válassz célt, hajtsd végre a lépéseket, majd nyisd meg közvetlenül a szükséges hivatalos szolgáltatást.",
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
                        Text("Megvalósítási állapot", style = Vizit.type.h3, color = colors.textPrimary)
                        Text(
                            "${completedGuideIds.size}/${toolkitGuides.size} útmutató teljesítve",
                            style = Vizit.type.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
        }
        items(toolkitGuides, key = { it.id }) { guide ->
            val completed = guide.id in completedGuideIds
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = BorderStroke(1.dp, colors.border),
                shape = RoundedCornerShape(Vizit.radius.lg),
            ) {
                Column(
                    Modifier.padding(Vizit.space.md),
                    verticalArrangement = Arrangement.spacedBy(Vizit.space.md),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Vizit.space.sm),
                    ) {
                        Box(
                            Modifier.size(44.dp).background(colors.primarySubtle, RoundedCornerShape(Vizit.radius.md)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(guide.icon, null, tint = colors.primary) }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Vizit.space.xxs)) {
                            Text(guide.title, style = Vizit.type.h3, color = colors.textPrimary)
                            Text(guide.description, style = Vizit.type.bodySmall, color = colors.textSecondary)
                        }
                        if (completed) Icon(Icons.Outlined.CheckCircle, "Teljesítve", tint = colors.success)
                    }
                    Box(
                        Modifier.fillMaxWidth().background(colors.primarySubtle, RoundedCornerShape(Vizit.radius.md))
                            .padding(Vizit.space.sm),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(Vizit.space.xxs)) {
                            Text("ELÉRENDŐ EREDMÉNY", style = Vizit.type.overline, color = colors.primary)
                            Text(guide.result, style = Vizit.type.bodySmall, color = colors.textPrimary)
                        }
                    }
                    Text("Lépések", style = Vizit.type.label, color = colors.textPrimary)
                    guide.steps.forEachIndexed { index, step ->
                        Row(horizontalArrangement = Arrangement.spacedBy(Vizit.space.sm)) {
                            Box(
                                Modifier.size(24.dp).background(colors.controlTrack, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) { Text("${index + 1}", style = Vizit.type.caption, color = colors.textSecondary) }
                            Text(step, style = Vizit.type.bodySmall, color = colors.textSecondary, modifier = Modifier.weight(1f))
                        }
                    }
                    Text("Közvetlen linkek", style = Vizit.type.label, color = colors.textPrimary)
                    VizitGroup {
                        guide.links.forEachIndexed { index, link ->
                            if (index > 0) VizitDivider()
                            VizitRow(
                                label = link.title,
                                icon = Icons.Outlined.Language,
                                supporting = link.description,
                                onClick = { runCatching { uriHandler.openUri(link.url) } },
                            )
                        }
                    }
                    VizitButton(
                        if (completed) "Teljesítve" else "Útmutató teljesítve",
                        {
                            completedGuideIds = if (completed) {
                                completedGuideIds - guide.id
                            } else {
                                (completedGuideIds + guide.id).distinct()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        style = if (completed) VizitButtonStyle.Tertiary else VizitButtonStyle.Secondary,
                        icon = Icons.Outlined.CheckCircle,
                    )
                }
            }
        }
        item {
            VizitGroup {
                VizitRow(
                    label = if (score < 50) "Következő lépés: válassz egy útmutatót" else "Következő lépés: mérd az eredményt",
                    icon = Icons.Outlined.RocketLaunch,
                    supporting = if (score < 50) {
                        "Ne mindent egyszerre: kezdd azzal, amelyik most a legtöbb hibát vagy elveszett időt okozza."
                    } else {
                        "Harminc nap múlva ellenőrizd, csökkent-e a hiba, az átfutási idő vagy a költség."
                    },
                    showChevron = false,
                )
            }
        }
        item {
            Text(
                "A linkek hivatalos vagy széles körben használt külső szolgáltatásokhoz vezetnek. Az adózási és jogi döntést egyeztesd könyvelővel vagy jogi szakértővel.",
                style = Vizit.type.bodySmall,
                color = colors.textMuted,
            )
        }
        item { Spacer(Modifier.height(Vizit.space.xxl)) }
    }
}
