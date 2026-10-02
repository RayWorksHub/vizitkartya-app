package hu.rayworks.vizit.v10.ui.pages

/*
 * A Vállalkozói Portál tartalma szó szerint az éles appból (BusinessHubScreen.kt).
 * Minden hivatkozás valódi, a telefon böngészőjében nyílik.
 */

data class HubLink(val title: String, val sub: String, val url: String)

data class Lesson(val num: String, val title: String, val summary: String, val resTitle: String, val resUrl: String)

data class Course(
    val id: String,
    val title: String,
    val category: String,
    val level: String,
    val language: String,
    val description: String,
    val videoSource: String,
    val videoUrl: String,
    val lessons: List<Lesson>,
)

data class Guide(val id: String, val title: String, val description: String, val result: String, val steps: List<String>, val links: List<HubLink>)

val VOSZ_LINKS = listOf(
    HubLink("VOSZ videók", "Vállalkozói hírek, interjúk és gyakorlati videók", "https://youtube.com/@vosz.?si=k2EmMlI8Q5ttlPZC"),
    HubLink("VOSZ információk", "Érdekképviselet, tanácsadás, programok, hírek", "https://www.vosz.hu/hu"),
    HubLink("VOSZPort", "Digitális ügyintézés és tudásmegosztás", "https://voszport.com/"),
)

const val WEEKLY_TIP = "Válassz ki egy ismétlődő feladatot, és dokumentáld, mielőtt automatizálod."

val COURSE_CATEGORIES = listOf("Mind", "AI", "Digitális munka", "Cégépítés", "Biztonság", "Marketing", "Pénzügy")

val COURSES = listOf(
    Course(
        "ai", "AI a mindennapi vállalkozásban", "Mesterséges intelligencia", "Kezdő", "Magyar videó",
        "Használható promptok, automatizálási ötletek és felelős AI-használat. Megtanulod, mely feladatokat érdemes AI-ra bízni, és melyeket soha.",
        "VOSZ", "https://www.youtube.com/watch?v=uPYOrXxTxJI",
        listOf(
            Lesson("1.1", "Hol teremt értéket az AI?", "Gyűjts össze három ismétlődő szöveges feladatot, és válaszd ki azt, amelyiknél a legkisebb a hibakockázat.", "VOSZ AI-videósorozat", "https://www.youtube.com/results?search_query=VOSZ+ChatGPT+k%C3%A9pz%C3%A9s"),
            Lesson("1.2", "Jó prompt 5 lépésben", "Add meg a szerepet, a célt, a bemenetet, a korlátokat és a kívánt kimeneti formátumot.", "Promptolási videó megnyitása", "https://www.youtube.com/watch?v=uPYOrXxTxJI"),
            Lesson("2.1", "Ajánlat és e-mail gyorsítása", "Készíts ellenőrzött sablont ajánlatra, utánkövetésre és ügyfélválaszra; érzékeny adatot ne másolj be.", "Microsoft Copilot vállalkozásoknak", "https://www.microsoft.com/hu-hu/microsoft-365/business/copilot-for-microsoft-365"),
            Lesson("2.2", "Ellenőrzés és adatvédelem", "Minden AI-kimenetet ember ellenőrizzen, és legyen belső szabály arra, milyen adat kerülhet a rendszerbe.", "NAIH tájékoztatók", "https://naih.hu"),
        ),
    ),
    Course(
        "m365", "Microsoft 365 kisvállalkozásoknak", "Digitális munka", "Kezdő", "Magyar videók",
        "Teams, Outlook, OneDrive és SharePoint egyetlen, biztonságos rendszerben. Fiókok, jogosultságok, közös fájlkezelés és automatizmusok.",
        "Sämling Üzleti Oktatási Központ", "https://www.youtube.com/watch?v=py9fGXyBZcE",
        listOf(
            Lesson("1.1", "Fiókok és jogosultságok", "Minden munkatársnak külön fiók, szerepkör szerinti hozzáférés és bekapcsolt többtényezős védelem kell.", "Microsoft 365 Vállalati verzió", "https://www.microsoft.com/hu-hu/microsoft-365/business"),
            Lesson("1.2", "Közös fájlkezelés OneDrive-val", "Alakíts ki közös mappaszerkezetet, tulajdonost és visszaállítási rendet; ne e-mailben küldözgess fájlmásolatokat.", "OneDrive magyar súgó", "https://support.microsoft.com/hu-hu/onedrive"),
            Lesson("2.1", "Teams-együttműködés", "Hozz létre ügyfél- vagy projektcsatornákat, és rögzítsd, melyik információ hol található.", "Teams magyar súgó", "https://support.microsoft.com/hu-hu/teams"),
            Lesson("2.2", "Naptár és automatizmusok", "Használj közös naptárt, foglalási oldalt és egyetlen jóváhagyott automatizmust egy ismétlődő folyamathoz.", "Power Automate", "https://www.microsoft.com/hu-hu/power-platform/products/power-automate"),
        ),
    ),
    Course(
        "basics", "Vállalkozói alapismeretek", "Cégépítés", "Kezdő", "Magyar videó",
        "Üzleti modell, célpiac, árazás és az első 90 nap terve. Az indítás kötelező lépéseitől a heti mérőszámokig.",
        "Magyar nyelvű oktatóvideó", "https://www.youtube.com/watch?v=d2tyNSnyv1Q",
        listOf(
            Lesson("1.1", "Üzleti modell egy oldalon", "Írd le egy mondatban a vevőt, a problémáját, az ajánlatodat, az értékesítési csatornát és a bevételi módot.", "VOSZ vállalkozói információk", "https://www.vosz.hu/hu"),
            Lesson("1.2", "Indítás és kötelező lépések", "Ellenőrizd a tevékenységet, adózást, képesítési feltételt, kamarai bejelentést és számlázást.", "NAV: egyéni vállalkozás indítása", "https://nav.gov.hu/Elethelyzetek-adozasa/vallalkozas/Egyeni-vallalkozas-inditasa"),
            Lesson("2.1", "Ideális ügyfél és árazás", "Határozz meg egy konkrét célcsoportot, eredményt, költségszintet és minimális vállalható árat.", "MKIK Mentorprogram", "https://vallalkozztudatosan.mkik.hu"),
            Lesson("2.2", "90 napos akcióterv", "Bonts három havi célra, heti mérőszámokra és minden héten egy lezárandó ügyfélszerzési feladatra.", "Vállalkozz digitálisan", "https://vallalkozzdigitalisan.mkik.hu"),
        ),
    ),
    Course(
        "security", "Kiberbiztonság emberi nyelven", "Biztonság", "Kezdő", "Magyar videó",
        "Fiókvédelem, mentés, adathalászat és egy egyszerű incidens-terv. A legnagyobb védelem a legkisebb munkáért.",
        "Pénziránytű / KiberPajzs", "https://www.youtube.com/watch?v=2LqpB_03Jt0",
        listOf(
            Lesson("1.1", "Többlépcsős belépés", "Kapcsold be először az e-mail-, banki, közösségi és adminisztrációs fiókoknál.", "KiberPajzs biztonsági tippek", "https://kiberpajzs.hu/hasznos-tippeket-olvasnek"),
            Lesson("1.2", "Eszközök és mentés", "Automatikus frissítés, képernyőzár és legalább egy külön helyen tárolt, visszaállítással is tesztelt mentés kell.", "NKI tudásbázis", "https://nki.gov.hu"),
            Lesson("2.1", "Adathalászat felismerése", "Sürgetésnél állj meg, külön csatornán ellenőrizd a feladót, és ne a levélből nyisd meg a belépési oldalt.", "KiberPajzs videó", "https://www.youtube.com/watch?v=2LqpB_03Jt0"),
            Lesson("2.2", "Incidens-terv", "Írd le, kit kell hívni, hogyan zárod a fiókokat, hol van a mentés, és hogyan értesíted az érintetteket.", "NKI incidensbejelentés", "https://nki.gov.hu/intezet/tartalom/incidens-bejelentes/"),
        ),
    ),
    Course(
        "marketing", "Online jelenlét és ügyfélszerzés", "Marketing", "Középhaladó", "Magyar videók",
        "Pozicionálás, Google Cégprofil, négyhetes tartalomterv és mérés. Ügyfélszerzés követhető lépésekben, nem követőszámban.",
        "Jobbágy András", "https://www.youtube.com/watch?v=-4qATDuCWgU",
        listOf(
            Lesson("1.1", "Pozicionálási mondat", "Ne szolgáltatást sorolj: mondd meg, kinek, milyen eredményt és mitől más módon adsz.", "VOSZ videók", "https://youtube.com/@vosz."),
            Lesson("1.2", "Bizalmat építő Google Cégprofil", "Tölts ki minden adatot, adj képeket, szolgáltatásokat és rendszeresen válaszolj az értékelésekre.", "Google Cégprofil hozzáadása", "https://support.google.com/business/answer/2911778?hl=hu"),
            Lesson("2.1", "4 hetes tartalomterv", "Hetente mutass problémát, megoldást, bizonyítékot és konkrét következő lépést.", "Meta Business Suite", "https://business.facebook.com"),
            Lesson("2.2", "Mérés és javítás", "Mérd a forrást, érdeklődést, ajánlatot és vásárlást; a követőszám önmagában nem üzleti eredmény.", "Google Analytics", "https://analytics.google.com"),
        ),
    ),
    Course(
        "finance", "Pénzügyi tudatosság alapjai", "Pénzügy", "Kezdő", "Magyar videó",
        "Cash-flow, költségek, számlázás és együttműködés a könyvelővel. Hogy a bevétel és a nyereség ne csússzon össze.",
        "Nemzeti Adó- és Vámhivatal", "https://www.youtube.com/watch?v=U5s2bXrgnHc",
        listOf(
            Lesson("1.1", "Bevétel, költség és nyereség", "Külön kezeld a beérkezett pénzt, az áfát, a fizetendő adót, a költségeket és a tulajdonosi kivétet.", "NAV vállalkozói élethelyzetek", "https://nav.gov.hu/Elethelyzetek-adozasa/vallalkozas"),
            Lesson("1.2", "13 hetes cash-flow", "Hetente vezesd a várható be- és kifizetéseket, és jelöld a bizonytalan tételeket.", "Pénziránytű", "https://penziranytu.hu"),
            Lesson("2.1", "Számlázás és adatszolgáltatás", "Ellenőrizd a NAV-kapcsolatot, a számlaadatokat és azt, hogy ki figyeli a hibás adatszolgáltatást.", "NAV Online Számla", "https://onlineszamla.nav.gov.hu"),
            Lesson("2.2", "Havi zárás a könyvelővel", "Legyen fix határidő a bizonylatokra, kintlévőségekre, adókra és a következő három hónap pénzigényére.", "NAV ONYA", "https://onya.nav.gov.hu"),
        ),
    ),
)

/** Szűrőcsip → kurzus (az „AI” a mesterséges intelligencia kurzust jelenti). */
fun Course.matches(chip: String): Boolean = chip == "Mind" || (chip == "AI" && id == "ai") || category == chip

val GUIDES = listOf(
    Guide(
        "launch", "Vállalkozás indítása", "Az ötlettől a jogszerű indulásig, kihagyott kötelező lépések nélkül.",
        "Működő vállalkozói státusz és rendezett alapadatok.",
        listOf(
            "Írd le a tevékenységet és ellenőrizd, kell-e képesítés vagy engedély.",
            "Könyvelővel válassz vállalkozási formát és adózást még a bejelentés előtt.",
            "Indítsd el az egyéni vállalkozást a NAV Vállalkozói Ügysegédjén vagy intézd a cégalapítást szakértővel.",
            "Jelentkezz be az illetékes gazdasági kamarához a NAV által jelzett határidőn belül.",
            "Állíts be számlázást, vállalkozói bankszámlát és iratmegőrzési rendet.",
        ),
        listOf(
            HubLink("NAV indítási útmutató", "Hivatalos, naprakész lépések és feltételek", "https://nav.gov.hu/Elethelyzetek-adozasa/vallalkozas/Egyeni-vallalkozas-inditasa"),
            HubLink("NAV Vállalkozói Ügysegéd", "Az online bejelentés belépési pontja", "https://ugyfelportal.nav.gov.hu"),
            HubLink("MKIK", "Kamarai információk és területi kamarák", "https://mkik.hu"),
        ),
    ),
    Guide(
        "billing", "Számlázás és NAV-ügyintézés", "A számla kiállításától a bevallási feladatok követéséig.",
        "Ellenőrizhető számlázási folyamat és kevesebb adminisztrációs hiba.",
        listOf(
            "Regisztrálj a NAV Online Számla rendszerébe és rögzíts technikai felhasználót, ha a program kéri.",
            "Válassz NAV-kapcsolatos számlázót, majd állíts ki és ellenőrizz egy tesztszámlát.",
            "Rögzíts heti rutint a hibás adatszolgáltatások és a kintlévőségek ellenőrzésére.",
            "Egyeztess a könyvelővel dokumentumleadási határidőt és felelőst.",
        ),
        listOf(
            HubLink("NAV Online Számla", "Regisztráció, számlák és adatszolgáltatási hibák", "https://onlineszamla.nav.gov.hu"),
            HubLink("NAV ONYA", "Online nyomtatványok és bejelentések", "https://onya.nav.gov.hu"),
            HubLink("e-Beszámoló", "Közzétett éves beszámolók hivatalos keresője", "https://e-beszamolo.im.gov.hu"),
        ),
    ),
    Guide(
        "office", "Digitális iroda kialakítása", "Céges e-mail, közös dokumentumok és világos hozzáférések.",
        "Egy helyen megtalálható fájlok és átadható működés.",
        listOf(
            "Használj saját domaines céges e-mail-címet, ne közös jelszóval használt postafiókot.",
            "Hozz létre egységes mappaszerkezetet ügyfelekre, pénzügyre és belső működésre.",
            "Adj személyenként jogosultságot, és távozáskor azonnal vond vissza.",
            "Kapcsold be a verziókövetést, mentést és teszteld egy fájl visszaállítását.",
        ),
        listOf(
            HubLink("Microsoft 365 vállalkozásoknak", "Outlook, Teams, OneDrive és irodai alkalmazások", "https://www.microsoft.com/hu-hu/microsoft-365/business"),
            HubLink("Google Workspace", "Céges Gmail, Drive, Meet és közös munka", "https://workspace.google.com/intl/hu/"),
            HubLink("Vállalkozz digitálisan", "MKIK digitális megoldások és segítség", "https://vallalkozzdigitalisan.mkik.hu"),
        ),
    ),
    Guide(
        "security", "Biztonság és adatvédelem", "A leggyakoribb fiók-, adat- és csalási kockázatok kezelése.",
        "Védett fiókok, működő mentés és leírt incidensfolyamat.",
        listOf(
            "Kapcsold be a többtényezős azonosítást az e-mailen, bankon, közösségi és adminfiókokon.",
            "Használj jelszókezelőt, és szüntesd meg a közös vagy újrahasznált jelszavakat.",
            "Készíts automatikus mentést külön helyre, majd próbáld visszaállítani.",
            "Írd le, ki mit tesz csalás, elveszett eszköz vagy adatszivárgás esetén.",
            "Tarts naprakész adatkezelési tájékoztatót, és csak szükséges ügyféladatot gyűjts.",
        ),
        listOf(
            HubLink("KiberPajzs", "Csalásmegelőzési és digitális biztonsági tippek", "https://kiberpajzs.hu/hasznos-tippeket-olvasnek"),
            HubLink("Nemzeti Kibervédelmi Intézet", "Riasztások, tudásanyag és incidensbejelentés", "https://nki.gov.hu"),
            HubLink("NAIH", "Hivatalos adatvédelmi tájékoztatók és ügyintézés", "https://naih.hu"),
        ),
    ),
    Guide(
        "sales", "Online jelenlét és ügyfélszerzés", "Megtalálható profilok, egyértelmű ajánlat és mérhető érdeklődők.",
        "Friss online jelenlét és követhető ügyfélszerzési tölcsér.",
        listOf(
            "Fogalmazd meg egy mondatban, kinek milyen eredményt adsz.",
            "Igényeld és töltsd ki a Google Cégprofilt, ha helyi vagy személyes szolgáltatást végzel.",
            "Válassz legfeljebb két aktív közösségi csatornát, és legyen minden felületen egyértelmű kapcsolatfelvétel.",
            "Rögzítsd minden érdeklődő forrását és a következő lépését.",
            "Havonta értékeld az érdeklődő, ajánlat és vásárlás számát.",
        ),
        listOf(
            HubLink("Google Cégprofil létrehozása", "Megjelenés a Google Keresőben és Térképen", "https://support.google.com/business/answer/2911778?hl=hu"),
            HubLink("Meta Business Suite", "Facebook- és Instagram-oldalak kezelése", "https://business.facebook.com"),
            HubLink("Google Analytics", "Webes forgalom és konverziók mérése", "https://analytics.google.com"),
        ),
    ),
    Guide(
        "growth", "Mentor, digitalizáció és fejlődés", "Hiteles segítség, partnerkapcsolatok és fejlesztési programok keresése.",
        "Kiválasztott fejlesztési cél és konkrét jelentkezési következő lépés.",
        listOf(
            "Válassz egy 90 napos fejlesztési célt: értékesítés, digitalizáció, export vagy működés.",
            "Készíts egyoldalas helyzetképet a számokról, problémáról és elvárt eredményről.",
            "Keress hozzá szakmai szervezetet, mentort vagy minősített digitális szolgáltatót.",
            "Jelölj ki felelőst, határidőt és egy mérőszámot.",
        ),
        listOf(
            HubLink("VOSZ", "Érdekképviselet, tanácsadás, programok, hírek", "https://www.vosz.hu/hu"),
            HubLink("MKIK Mentorprogram", "Mentorálás növekedéshez és külpiachoz", "https://vallalkozztudatosan.mkik.hu"),
            HubLink("Vállalkozz digitálisan", "Digitális fejlesztési tudás és megoldások", "https://vallalkozzdigitalisan.mkik.hu"),
        ),
    ),
)
