# VIZIT — teljes UI-architektúra és felületi koncepció

**Verzió:** 1.0  
**Kelt:** 2026. szeptember 18.  
**Státusz:** önálló tervezési javaslat; nem implementációs utasítás és nem megfelelőségi tanúsítvány.  
**Nyelv:** magyar; angol lokalizációra előkészített felületi rendszer.  
**Hatókör:** Android, iOS/iPadOS és a névjegyet fogadó publikus webes felület; elkülönített bővítési koncepciók.  
**Tárolási hely:** `design-concepts/vizit-ui-architecture/README.md`  
**Koncepcióág:** `concept/vizit-ui-architecture-2026-09-18`

> **FÜGGETLEN KONCEPCIÓ.** Ez a dokumentum kizárólag emberi olvasásra és későbbi, külön jóváhagyott tervezési munkára szolgál. Nem írja felül az alkalmazás aktív specifikációját. Nem indít fejlesztést, nem jogosít fel átépítésre, és nem ad engedélyt adatbázis-, infrastruktúra-, hitelesítési vagy alkalmazáskód-módosításra. A benne szereplő „legyen”, „szükséges” és „tilos” megfogalmazások a javasolt felület követelményei, nem azonnal végrehajtandó agentutasítások.

---

## Tartalomjegyzék

1. [Elkülönítés és dokumentumhatár](#01)
2. [Kiinduló források és értelmezési szabályok](#02)
3. [Hivatalos szabványok és irányelvek](#03)
4. [Termékélmény és tervezési alapelvek](#04)
5. [Felhasználói szerepek és feladatok](#05)
6. [Információs architektúra és navigáció](#06)
7. [Teljes képernyőjegyzék](#07)
8. [Indítás, hitelesítés és első használat](#08)
9. [Névjegy-adatmodell a felület szemszögéből](#09)
10. [Névjegyszerkesztés és médiakezelés](#10)
11. [Megosztási központ és adat-előnézet](#11)
12. [Kontakt QR és profil QR](#12)
13. [NFC-folyamat és képességalapú működés](#13)
14. [Fogadás, QR-beolvasás és rendszeroldali kontaktmentés](#14)
15. [Publikus webes névjegy](#15)
16. [Offline állapot, szinkron és ütközéskezelés](#16)
17. [Beállítások, adatvédelem és fiókkezelés](#17)
18. [Vizuális irány és márkahasználat](#18)
19. [Színrendszer és ellenőrzött kontrasztok](#19)
20. [Tipográfia, térközök és felületi geometria](#20)
21. [Adaptív elrendezések és platformeltérések](#21)
22. [Komponensarchitektúra](#22)
23. [Interakció, fókusz, mozgás és visszajelzés](#23)
24. [Engedélyek és biztonsági felületek](#24)
25. [Mikroszövegek és lokalizáció](#25)
26. [Akadálymentességi követelmény–ellenőrzés mátrix](#26)
27. [Minőségbiztosítás és elfogadási forgatókönyvek](#27)
28. [Figma-tervezési és prototípus-átadási szerkezet](#28)
29. [Bővítési helyek, jelenlegi hatókörön kívül](#29)
30. [Döntési napló és nyitott bizonyítási kérdések](#30)
31. [Figma Make számára átadható tervezési brief](#31)
32. [Forrásjegyzék](#32)

---

<a id="01"></a>
## 1. Elkülönítés és dokumentumhatár

### 1.1. Technikai elkülönítés

Ez a koncepció egyetlen Markdown-fájl. Nem tartozik hozzá futtatható kód, csomag, konfiguráció, design-token export, generátor vagy szinkronfolyamat.

A dokumentum elhelyezése nem jelentheti az alábbiakat:

- import az alkalmazásba, Gradle-be, Xcode-ba, webes buildbe vagy dokumentációgenerátorba;
- Figma-, Code Connect-, CI-, webhook- vagy agentkapcsolat létrehozása;
- a gyökér-README, az aktív master specifikáció vagy agentkonfiguráció átírása;
- más fejlesztési ág beolvasztása, automatikus PR, auto-merge vagy telepítés;
- külső szolgáltatás bekötése, adatbázis-migráció vagy új backend létrehozása.

A koncepcióág a `main` ellenőrzött `b8a4e6d359268237e8195b56a4196f5055c4f45c` állapotából indul. A termékismereti hivatkozások egy ettől eltérő, külön megjelölt fejlesztési pillanatképre mutatnak. **A hivatkozás nem kódátvétel és nem függőség.**

Az ellenőrzött Android workflow a `main` és `develop` ágakra történő pushra, valamint az ezeket célzó PR-ekre indul. A külön koncepcióág és a PR mellőzése nem illeszkedik ezekhez az eseményszűrőkhöz. A workflow változatlan marad. [R4]

A fájl ugyanabban a repóban olvasható, ezért nem állítható, hogy egy ember vagy egy külön beállított külső indexelő soha nem találhatja meg. Az elkülönítés azt jelenti, hogy **ez a munka semmilyen automatikus felhasználási vagy fejlesztési kapcsolatot nem létesít**. A koncepció későbbi alkalmazása külön döntés.

### 1.2. Mit tartalmaz és mit nem?

Tartalmazza a teljes alapélmény felületi felosztását, a képernyők felelősségét, a navigációt, a fő állapotátmeneteket, a vizuális rendszert, a komponenseket, a hozzáférhetőséget és a tesztelhető elfogadási feltételeket.

Nem tanúsítja, hogy mindez már megvalósult. Nem bizonyít NFC-interoperabilitást, sikeres e-mail-kézbesítést, élő OAuth-konfigurációt vagy App Store-megfelelést. Nem helyettesít működő alkalmazáson végzett auditot.

<a id="02"></a>
## 2. Kiinduló források és értelmezési szabályok

### 2.1. Forráspillanatkép

A termékkövetelmények és a márka áttekintett forrása a `RayWorksHub/vizitkartya-app` repó `91243c6a26f9add6a6ce9e578528f777b9b1e210` commitja, a vizsgálatkor a `feature/ios-secure-beta` ág feje:

| Forrás | Mire használjuk? | Mire nem bizonyíték? |
|---|---|---|
| `docs/BRANDING.md` | Hivatalos színek, logóforrás, márkajel és céges logó elkülönítése | Nem teljes hozzáférhetőségi audit |
| `ios/README.md` | Az iOS-kliens dokumentált iránya és eltérő platformfolyamatai | Nem fizikai iPhone-tesztjegyzőkönyv |
| `README.md` | A fejlesztési állapot kontextusa | Nem production minősítés |

Ezek rögzített pillanatképre mutató olvasási hivatkozások; későbbi változásukat semmi nem követi automatikusan. [R1–R3, R5]

### 2.2. Eltérő történeti hatókörök

A master specifikáció eredeti prioritása Xiaomi/HyperOS Android, teljes iOS-kliens nélkül. A vizsgált későbbi ág ugyanakkor már külön iOS-kliensdokumentációt tartalmaz. Ezt nem olvasztjuk össze hallgatólagosan egy állítólag egységes, már megvalósult termékké. [R1, R3]

**E dokumentum tervezési döntése:** az alapfolyamatok teljes Android- és iOS-felületi koncepciót, valamint alkalmazástelepítést nem igénylő publikus webes fogadóoldalt kapnak. Az iOS-változat nem Android-másolat. A teljes webes szerkesztőportál, több névjegy, fizikai NFC-kártyák és Wallet bővítési helyek, nem automatikusan vállalt kiadási funkciók.

### 2.3. Követelmények eredete

| Jelölés | Jelentés |
|---|---|
| **K** | A vizsgált projektforrásban szereplő követelmény |
| **SZ** | Hivatkozott szabvány normatív követelménye a saját alkalmazási körében |
| **I** | Hivatalos platformirányelv vagy tájékoztató alkalmazási útmutató |
| **T** | A jelen dokumentumban javasolt VIZIT-tervezési döntés |
| **B** | Opcionális, később külön jóváhagyandó bővítés |

A képernyőfelosztás, méretválasztás, komponenselnevezés, szövegezés és folyamat-részletezés alapértelmezetten **T**, kivéve ahol más eredetet jelölünk. A szabványok nem írják elő a VIZIT négy menüpontját vagy márkaszíneit.

<a id="03"></a>
## 3. Hivatalos szabványok és irányelvek

### 3.1. Alkalmazott referenciák

| Referencia | Típusa | Alkalmazás a koncepcióban |
|---|---|---|
| ISO 9241-210:2019 | Nemzetközi szabvány | Emberközpontú tervezési folyamat, használati környezet és felhasználói értékelés. A katalógus szerint 2025-ben megerősítve. [S01] |
| ISO 9241-110:2020 | Nemzetközi szabvány | Interakciós alapelvek szerinti felületi felülvizsgálat. Nem márkaarculati recept. [S02] |
| WCAG 2.2, A és AA | W3C Recommendation | A publikus webes felület hozzáférhetőségi célja. A teljes megfelelés minden alkalmazandó A/AA követelmény vizsgálatát jelenti. [S03] |
| WCAG2ICT, 2025. december 11-i Group Note | Tájékoztató W3C-útmutató | A WCAG elveinek értelmezése nem webes szoftverre; önmagában nem új normatív szabvány. [S04] |
| EN 301 549 | Európai ICT-hozzáférhetőségi szabvány | Kiegészítő európai követelményrendszer. A verzió és a jogi hivatkozási státusz nem azonos kérdés. [S05] |
| Android/Compose/Material 3 hivatalos dokumentáció | Platformirányelvek | Natív vezérlők, érintési célok, adaptív ablakméretek, rendszerintegráció. [S06–S08] |
| Apple Human Interface Guidelines és fejlesztői dokumentáció | Platformirányelvek | iOS/iPadOS-navigáció, érintési célok, Dynamic Type, natív rendszerfelületek. [S09–S12] |
| WAI-ARIA Authoring Practices | Tájékoztató webes mintagyűjtemény | Szükséges egyedi webes párbeszédek fókusz- és billentyűzetkezelése. [S13] |
| NIST SP 800-63B-4 | Hitelesítési referencia | Jelszó-UX tervezési támpont; nem általános magyar jogszabály és nem automatikus backendváltoztatás. [S14] |
| IETF RFC 2426 és RFC 6350 | Formátumspecifikációk | A névjegyadatok interoperabilitásának kontextusa, nem UI-elrendezési előírás. [S15] |
| DENSO WAVE QR-útmutató | Elsődleges technikai útmutató | A QR-kód olvashatósága és a négy modul széles környező üres sáv. [S16] |

Az ISO-hivatkozásokhoz az elérhető hivatalos katalógusadatokat és összefoglalókat használtuk. A fizetős teljes szabványszövegek tételes auditja nem történt meg; ezért itt nincs kitalált ISO-klauzulaszám vagy ISO-tanúsítási állítás.

### 3.2. EN 301 549: verzió és jogi hatály

Az Európai Bizottság AccessibleEU központjának 2026. szeptember 7-i közlése szerint az **EN 301 549 V4.1.1 2026 szeptemberében megjelent**, és WCAG 2.2-re frissít. Ugyanez a közlés elkülöníti a közzétételt a hivatalos uniós hivatkozástól. Ezért a jelen terv nem állítja, hogy a megjelenés önmagában új, minden VIZIT-felületre automatikusan alkalmazandó jogi kötelezettséget keletkeztet. [S05]

A konkrét szolgáltatás jogi besorolása, a rá alkalmazandó követelmények és az aktuális harmonizált hivatkozás kiadás előtti külön ellenőrzési feladat. A teljes EN-szabvány klauzulánkénti megfelelőségi mátrixa nem része ennek a felületi koncepciónak.

### 3.3. Három eltérő minimum

**Android:** a hivatalos hozzáférhetőségi útmutató legalább 48 × 48 dp érintési felületet javasol. **Apple:** az általános UI-tervezési útmutató legalább 44 × 44 pt célt ír le. **WCAG 2.2 AA, 2.5.8:** 24 × 24 CSS px minimumot, illetve meghatározott kivételeket alkalmaz. Ezeket nem szabad egyetlen „hivatalos 48 pixeles szabálynak” nevezni. [S06, S09, A01]

A VIZIT saját webes tervezési célja ennél szigorúbb: a fő érinthető vezérlők legalább 48 × 48 CSS px területet kapnak. Ez **T**, nem a WCAG AA szó szerinti minimuma.

<a id="04"></a>
## 4. Termékélmény és tervezési alapelvek

### 4.1. Termékígéret

**VIZIT — EGY ÉRINTÉS. EGY KAPCSOLAT.**

A tulajdonos gyorsan megmutatja vagy átadja az általa kiválasztott kapcsolati adatokat; a fogadó megérti, kitől kapta őket, és saját döntésével elmentheti a névjegyet. A fogadóoldali VIZIT-telepítés és regisztráció nem lehet az alapfolyamat feltétele. [K; R1, 1., 34–37., 52–68. fejezet]

A szlogen nem műszaki garancia arra, hogy minden készülékpár NFC-vel működik. Az alkalmazás az adott környezetben elérhető, igazolt módszereket kínálja.

### 4.2. Döntési sorrend

1. **Adatbiztonság és érthetőség:** a felhasználó tudja, mely adatokat teszi elérhetővé.
2. **Feladat elvégzése:** a saját névjegy megmutatása és szerkesztése rövid úton elérhető.
3. **Hozzáférhetőség és platformhelyes működés:** nagy szöveg, képernyőolvasó és rendszerengedélyek nem utólagos kiegészítések.
4. **Vizuális karakter:** határozott VIZIT-márka, nem általános adminpanel és nem látványelemekkel túlterhelt landingoldal.

### 4.3. Konkrét termékszabályok

- A kezdőlapon egyetlen nagy névjegy-előnézet és világosan megkülönböztetett átadási műveletek szerepelnek.
- A profilkép, céges logó és VIZIT-márkajel három külön szerep.
- A megosztás helyi előnézetet használ; a publikus weboldal eltérő, még nem szinkronizált állapotára külön figyelmeztet.
- A „kontakt mentve” kifejezés csak ténylegesen igazolt mentési eseményhez tartozhat.
- A hiba következő lépést ad: javítás, újrapróbálás, másik mód vagy biztonságos kilépés.
- A tökéletlen profil nem marketinges százalékmutatót kap, hanem pontos hiányjelzést: „Adj meg egy megosztható telefonszámot vagy e-mail-címet.”
- Nem készül mesterséges aktivitási feed, követőrendszer, ranglista vagy indokolatlan értesítési központ.

<a id="05"></a>
## 5. Felhasználói szerepek és feladatok

| Szerep | Cél | Belépés szükséges? | Elsődleges felület |
|---|---|---|---|
| Névjegytulajdonos | Saját adatok szerkesztése, megosztása, online láthatóság szabályozása | A fiókhoz kötött funkciókhoz igen | Natív alkalmazás |
| Alkalmi fogadó | Megkapni, értelmezni, elmenteni a kapcsolati adatokat | Nem | Rendszerkamera, rendszer Kontaktok, publikus web |
| VIZIT-et már használó fogadó | QR beolvasása és ellenőrzött kontaktátadás a rendszernek | A nyilvános névjegy megtekintéséhez ne kelljen | Alkalmazáson belüli fogadófelület |
| Tulajdonos másik eszközön | Visszakapni a profilt, észrevenni az ütköző módosításokat | Igen | Bejelentkezés, profil, szinkronrészletek |

A termék alapegysége egy felhasználó saját névjegye, nem egy CRM-ben kezelt címjegyzék. Egy beolvasott idegen névjegy nem válik automatikusan a VIZIT felhőadatbázisának új rekordjává.

**Tervezési sikerfeltétel:** első használatkor a felhasználó meg tudja mondani, mely adatok kerülnek át; visszatérő használatkor a Kontakt QR legfeljebb két, a főképernyőről indított tudatos művelettel elérhető. Ez termékcél, nem mért jelenlegi eredmény.

<a id="06"></a>
## 6. Információs architektúra és navigáció

### 6.1. Négy állandó fő célterület

| Menüpont | Felelősség | Nem ide tartozik |
|---|---|---|
| **Kezdőlap** | Saját névjegy, gyors átadás, lényeges állapot | Minden beállítás és összes szerkesztőmező |
| **Névjegyem** | Tartalom, megjelenés, mezősorrend, megoszthatóság | Fiókjelszó és rendszerengedélyek |
| **Átadás** | Kontakt QR, profil QR, NFC és rendszermegosztás | Automatikus NFC-indítás pusztán a fül megérintésére |
| **Beállítások** | Fiók, adatvédelem, megjelenés, segítség, diagnosztika | A névjegy tényleges adatainak második szerkesztője |

Ez megtartja a master javasolt négy célterületét. [K; R1, 69–70. fejezet] Az Átadás kiemelése nem jelenti a másik három menüpont elrejtését.

### 6.2. Navigációs fa

```text
Alkalmazás
├── Indítás és munkamenet-helyreállítás
├── Hitelesítés
│   ├── Belépés / Regisztráció
│   ├── E-mail megerősítése
│   ├── Jelszó-helyreállítás
│   └── Elérhető külső belépési szolgáltató
├── Első névjegy létrehozása
├── Kezdőlap
│   ├── Névjegy-előnézet
│   ├── Gyors Kontakt QR / NFC
│   └── Releváns profil- vagy szinkronüzenet
├── Névjegyem
│   ├── Személyes és szakmai adatok
│   ├── Elérhetőségek / Linkek
│   ├── Profilkép / Céges logó
│   ├── Megosztható mezők / Sorrend
│   ├── Publikus profil / Webcím
│   └── Címzett nézete
├── Átadás
│   ├── Megosztási előnézet
│   ├── Kontakt QR / Profil QR / Teljes képernyő
│   ├── NFC előellenőrzés / Aktív átadás / Eredmény
│   ├── Megosztás másképp
│   └── QR beolvasása
└── Beállítások
    ├── Fiók és biztonság
    ├── Adatvédelem
    ├── Megjelenés / Nyelv
    ├── Szinkron és készülékállapot
    ├── Segítség / Kompatibilitás
    └── Névjegy / Jogi dokumentumok / Fióktörlés

Külön, hitelesítés nélkül elérhető belépési pont:
Publikus profil-link → webes vagy alkalmazáson belüli fogadófelület
```

### 6.3. Navigációs szerződés

Minden fő fül saját visszalépési állapotot és görgetési pozíciót őriz. A tab célterület, nem azonnal végrehajtott művelet; ez az Apple tabbar-irányelvével is összhangban áll. [I; S10]

A QR-beolvasás az Átadás oldalon egy külön művelet, nem ötödik fő menüpont. A belépés után a félbehagyott, biztonságosan tárolható célnavigáció folytatható, de NFC, kamerafelvétel vagy destruktív művelet nem indul újra automatikusan.

A szerkesztőből visszalépés szabálya: módosítás nélkül azonnali vissza; nem mentett változásnál „Mentés / Módosítások elvetése / Maradok”. A már helyileg elmentett, csak felhőre váró változás nem azonos a nem mentett piszkozattal.

Hitelesítési és visszaállítási link mindig központi ellenőrzésen át érkezik. Hibás, lejárt vagy már felhasznált link nem hozhat létre látszólag sikeres munkamenetet.

<a id="07"></a>
## 7. Teljes képernyőjegyzék

### 7.1. Olvasási szabály

Az azonosítók tervezési hivatkozások, nem implementált route-nevek. Egy azonosító lehet teljes képernyő, összetartozó sheet vagy natív rendszernek átadott lépés. Az alábbi felosztás nem ír elő ennyi külön forrásfájlt.

**Platform:** A = Android; I = iOS/iPadOS; W = publikus web. Ahol a képesség nem igazolt, ott csak magyarázó vagy alternatívát kínáló állapot jelenhet meg.

A teljes állapotkészlet a 7.8. fejezetben szerepel; a táblázat képernyőnként a különösen fontos eltérést emeli ki.

### 7.2. Rendszer és hitelesítés

| ID | Felület és cél | Fő művelet / kilépés | Lényeges állapot | Platform |
|---|---|---|---|---|
| SYS-01 | Indítás és helyi munkamenet betöltése | Automatikus továbbirányítás | Inicializálás; biztonságos hiba; nincs mesterséges várakozás | A/I |
| SYS-02 | Indítási helyreállítás | Újrapróbálás | Helyi adat nem olvasható; nem mutat idegen fiókadatot | A/I |
| SYS-03 | Szolgáltatáskimaradás / szükséges frissítés | Újrapróbálás vagy igazolt frissítési cél | Offline helyi funkciók elérhetősége külön | A/I/W |
| SYS-04 | Ellenőrzött mélylink-feldolgozás | Folytatás a tényleges célra | Hibás cél, lejárt token, eltérő felhasználó | A/I/W |
| AUTH-01 | Belépési kezdőfelület | Belépés; Regisztráció | Külső provider csak valódi elérhetőség esetén | A/I |
| AUTH-02 | E-mailes belépés | Belépés | Hibás adat, offline, túl sok próbálkozás | A/I |
| AUTH-03 | Regisztráció | Fiók létrehozása | Mezőhibák, jogi tájékoztatás, küldés folyamatban | A/I |
| AUTH-04 | E-mail megerősítésére várás | Újraküldés; Cím javítása | Lejárat, újraküldési korlát, visszatérés ellenőrzése | A/I |
| AUTH-05 | Elfelejtett jelszó | Helyreállítás kérése | Semleges kézbesítési tájékoztatás | A/I |
| AUTH-06 | Új jelszó beállítása | Jelszó mentése | Érvényes helyreállítási állapot; lejárt link | A/I |
| AUTH-07 | Külső belépés rendszerfelülete | Szolgáltatói bejelentkezés | Megszakítás nem hiba; callbackhiba külön | A/I |
| AUTH-08 | Munkamenet lejárt | Újbóli belépés | Megmaradó helyi módosítások látható jelzése | A/I |
| AUTH-09 | Jogi dokumentum olvasása | Vissza az eredeti lépésre | Betöltési hiba nem „elfogadva” | A/I/W |

### 7.3. Első használat és saját névjegy

| ID | Felület és cél | Fő művelet / kilépés | Lényeges állapot | Platform |
|---|---|---|---|---|
| ONB-01 | Név és első elérhetőség | Folytatás | Legalább egy megosztható telefon vagy e-mail szükséges | A/I |
| ONB-02 | Opcionális kép és szakmai adatok | Folytatás / Kihagyás | Kihagyható, nincs engedélykényszer | A/I |
| ONB-03 | Első névjegy ellenőrzése | Névjegy mentése | Megosztható adatok és online publikálás külön | A/I |
| HOME-01 | Kezdőlap | Kontakt QR / NFC / Szerkesztés | Hiányos profil, offline, szinkronhiba | A/I |
| PRO-01 | Névjegyem áttekintése | Szerkesztési csoport választása | Helyi és közzétett változat jelzése | A/I |
| PRO-02 | Személyes és szakmai adatok | Mentés | Név, cég, munkakör, bemutatkozás | A/I |
| PRO-03 | Elérhetőségek listája | Új elérhetőség | Üres lista, elsődleges mező, privát jelölés | A/I |
| PRO-04 | Egy telefon/e-mail/cím/webhely szerkesztése | Mentés | Típusfüggő mezők és hibák | A/I |
| PRO-05 | Közösségi és egyedi linkek | Hozzáadás / Mentés | Hibás vagy nem támogatott URL | A/I |
| PRO-06 | Profilkép szerkesztése | Kép alkalmazása | Kiválasztás, vágás, visszaállítás, eltávolítás | A/I |
| PRO-07 | Céges logó szerkesztése | Logó alkalmazása | Teljes grafika, belső térköz, világos/sötét előnézet | A/I |
| PRO-08 | Megosztható mezők kezelése | Beállítás mentése | Az utolsó használható elérhetőség elrejtése | A/I |
| PRO-09 | Mezők és linkek sorrendje | Sorrend mentése | Húzás mellett Feljebb/Lejjebb művelet | A/I |
| PRO-10 | Publikus profil és webcím | Közzététel / Kikapcsolás | Szerver által megerősített állapot szükséges | A/I |
| PRO-11 | Címzett nézete | Vissza / Átadás | Csatornánként eltérő adat-előnézet | A/I |
| PRO-12 | Publikus webcím módosítása | Változtatás megerősítése | Régi QR/link következménye; foglalt slug | A/I |

### 7.4. Átadás és NFC

| ID | Felület és cél | Fő művelet / kilépés | Lényeges állapot | Platform |
|---|---|---|---|---|
| SHR-01 | Átadási központ | Kontakt QR / NFC / Egyéb mód | Képességalapú módszerválasztás | A/I |
| SHR-02 | Csatornaspecifikus előnézet | Átadás folytatása | Kihagyott kép, privát mezők kizárva | A/I |
| SHR-03 | Kontakt QR | Teljes képernyő | Offline használható; túl sűrű payload | A/I |
| SHR-04 | VIZIT profil QR | Teljes képernyő / Link másolása | Publikus profil kikapcsolva vagy nincs visszaigazolt link | A/I |
| SHR-05 | Teljes képernyős QR | Bezárás | Fényerő-visszaállítás; nagy betűméret | A/I |
| SHR-06 | Megosztás másképp | Link / Szöveg / QR-kép / Külön fájlexport | Csak ténylegesen előállítható tartalom | A/I |
| SHR-07 | Megosztási művelet állapota | Új megosztás / Kész | Rendszernek átadás nem címzetti mentés | A/I |
| NFC-01 | NFC előellenőrzés | NFC indítása / Beállítások / QR | Hardver, rendszerállapot, HCE, kompatibilitás | A; I-n magyarázat |
| NFC-02 | Aktív NFC-átadás | Leállítás | Olvasható adatok, lejárat, kapcsolat megszakadt | A, igazolt képességgel |
| NFC-03 | NFC műszaki eredmény | Újra / Kontakt QR | Teljes adatkiolvasás vs. részleges olvasás | A |
| NFC-04 | NFC-segítség és kompatibilitás | QR / Rendszerbeállítások | Ismeretlen eszközpár nem „támogatott” | A/I |

### 7.5. Fogadás és szinkron

| ID | Felület és cél | Fő művelet / kilépés | Lényeges állapot | Platform |
|---|---|---|---|---|
| REC-01 | QR-beolvasás | Beolvasás / Bezárás | Kameraengedély, sötét kép, nem olvasható kód | A/I |
| REC-02 | Beolvasott névjegy vagy URL előnézete | Kontakt megnyitása / Webcím megnyitása | Külső cím és forrás látható | A/I |
| REC-03 | Ismeretlen vagy veszélyes tartalom | Új beolvasás / Bezárás | Nem futtat kódot, nem navigál automatikusan | A/I |
| REC-04 | Rendszer Kontaktok szerkesztője | Rendszer Mentés / Mégse | OS által kezelt; megbízható visszajelzés hiánya kezelve | A/I |
| SYN-01 | Szinkronrészletek | Újrapróbálás | Helyi, felhő-, média- és publikációs állapot | A/I |
| SYN-02 | Két változat ütközése | Kiválasztott változat mentése | Nincs automatikus adatvesztés | A/I |
| SYN-03 | Médiafeltöltési problémák | Újrapróbálás / Csere / Eltávolítás | Kép nélkül is használható profil | A/I |

### 7.6. Beállítások

| ID | Felület és cél | Fő művelet / kilépés | Lényeges állapot | Platform |
|---|---|---|---|---|
| SET-01 | Beállítások kezdőoldala | Beállításcsoport választása | Környezetjelzés DEV/BETA esetén | A/I |
| SET-02 | Megjelenés | Rendszer / Világos / Sötét | Rendszeres hozzáférhetőségi beállításokat tiszteletben tart | A/I |
| SET-03 | Nyelv | Elérhető fordítás választása | Nem ajánl fel hiányos nyelvi csomagot | A/I |
| SET-04 | Fiók és biztonság | Jelszó / Kijelentkezés / Törlés | Providerfüggő elérhetőség | A/I |
| SET-05 | Érzékeny művelet újrahitelesítése | Azonosítás megerősítése | Megszakítás biztonságos visszatérés | A/I |
| SET-06 | Jelszómódosítás | Új jelszó mentése | Nem azonos az elfelejtettjelszó-folyamattal | A/I |
| SET-07 | Adatvédelem | Láthatóság / Jogi dokumentumok / Opcionális hozzájárulások | Nincs előre kijelölt opcionális engedély | A/I |
| SET-08 | Fióktörlés | Végleges törlés | Online, újrahitelesített; szervereredmény szükséges | A/I |
| SET-09 | Segítség | Problématípus / Kapcsolat | Saját tartalom olvasható, nincs kitalált támogatási cím | A/I/W |
| SET-10 | Készülék és NFC-állapot | Új ellenőrzés / Beállítások | Nem állít recipientoldali támogatottságot | A/I |
| SET-11 | Az alkalmazásról | Verzió / Licencek / Jogi dokumentumok | Productionban nincs titok vagy fejlesztői adat | A/I |
| SET-12 | Kijelentkezési megerősítés | Kijelentkezés / Maradok | Nem szinkronizált adatok következménye | A/I |

### 7.7. Publikus webes névjegy

| ID | Felület és cél | Fő művelet / kilépés | Lényeges állapot | Platform |
|---|---|---|---|---|
| PUB-01 | Névjegy megtekintése | Kontakt mentése / Kapcsolatfelvétel | Csak közzétett mezők | W; natív fogadónézet |
| PUB-02 | Kontaktmentési átadás | Rendszerfolyamat / Dokumentált alternatíva | Böngészőfüggő import; nincs garantált automatikus mentés | W |
| PUB-03 | Nem elérhető névjegy | Újrapróbálás / Segítség | Nem különbözteti meg fölöslegesen a privát és törölt profilt | W |
| PUB-04 | Webes hálózati vagy szolgáltatáshiba | Újrapróbálás | Nem ad ki korábbi privát állapotból adatot | W |

### 7.8. Kötelező állapotfedettség

Minden adatot vagy műveletet kezelő képernyőhöz dokumentálandó: normál; betöltés; üres; beviteli hiba; műveleti hiba; offline; folyamatban; siker; megszakított; hozzáférés elutasítva; munkamenet lejárt; helyi mentésre/felhőre vár; ütközés. A nem értelmezhető állapotot „nem alkalmazandó” indoklással kell jelölni, nem mesterséges képernyővel kitölteni.

A globális komponensvariánsok nem helyettesítik a képernyő üzleti állapotait: egy piros gomb nem teljes hibafolyamat.

<a id="08"></a>
## 8. Indítás, hitelesítés és első használat

### 8.1. Indítási sorrend

```text
Indítás
  → biztonságos helyi állapot helyreállítása
  → ismert munkamenet állapota
      → nincs: AUTH-01
      → visszavont / biztosan érvénytelen: AUTH-08
      → helyben ismert, de hálózat nem ellenőrizhető: korlátozott offline állapot
      → használható: szükséges jogi és profilállapot ellenőrzése
          → nincs névjegy: ONB-01
          → van névjegy: HOME-01 vagy ellenőrzött korábbi cél
```

Hálózati hiba önmagában nem bizonyítja, hogy a felhasználót kijelentkeztették. Az offline mód nem engedélyez felhőírást lejárt hitelesítéssel, és nem lép át másik felhasználó helyi adatterébe.

A splash csak rendszerindítási felület. Nem szerepel benne beégetett reklámidő, kötelező animáció vagy bejelentkezést imitáló progress.

### 8.2. Regisztráció és megerősítés

AUTH-03 mezői: e-mail-cím, jelszó, jelszó megerősítése, szükséges feltételek/tájékoztatás. A névjegyhez tartozó személyes adatokat az onboarding kezeli, nem kérjük őket több lépésen át újra.

A gomb megnyomásakor a felület először azonosítja a javítandó mezőket, majd egyetlen kérést indít. Az aktív küldés látható és a duplaküldés tiltott. Sikeres kérelem után AUTH-04 jelenik meg, nem a főoldal.

Az e-mailes állapotok különböznek: „A megerősítő levél küldését kértük”, „A küldés nem sikerült”, „Az e-mail-cím megerősítve”. A kliens API-válasza nem bizonyítja a levél postaládába érkezését.

Újraküldés csak a szerver által engedett időpontban. A visszaszámlálás nem kitalált 60 másodperc, hanem tényleges korlátból származó információ. A „Cím javítása” megőrzi a már biztonságosan megőrizhető adatokat, de jelszót nem tesz naplóba vagy képernyőközi diagnosztikába.

A felhasználó visszatérésekor az alkalmazás ellenőrzi a megerősítést. Másik eszközön megnyitott link esetén is legyen „Már megerősítettem” ellenőrzési művelet. Lejárt linkhez újraküldés tartozik, nem zsákutca.

### 8.3. Jelszó-UX

A NIST referenciája egyfaktoros jelszavas hitelesítésnél legalább 15 karaktert, legalább 64 karakteres megengedett maximumot, valamint a szükségtelen összetételi és időszakos jelszócsere-kényszer kerülését írja le. Ebből a VIZIT számára javasolt, külön egyeztetendő célpolitika vezethető le; **a felület nem írhat elő a tényleges szervertől eltérő szabályt**. [I/T; S14]

A jelszókezelő, automatikus kitöltés, beillesztés és a „Jelszó megjelenítése” elérhető. A megerősítő mező sem tilthatja a beillesztést. Az egykori, más követelmény alapján létrehozott fiók belépését nem utasíthatja el pusztán az új regisztrációs minimumot alkalmazó kliensvalidáció. [A02; T]

A hibás belépés üzenete ne árulja el szükségtelenül, hogy egy idegen e-mail-címhez létezik-e fiók. Jelszó-visszaállításkor semleges szöveg: „Amennyiben ehhez a címhez tartozik fiók, elküldjük a helyreállítási teendőket.”

### 8.4. Külső belépés

Google- vagy más belépési gomb csak a kiadási környezetben ténylegesen konfigurált, végigtesztelt szolgáltatóhoz jelenjen meg. Konfigurációhiány esetén nincs működést ígérő ál-gomb.

Az iOS App Review Guidelines 4.8 a hatálya alá tartozó külső belépéseknél megfelelő adatvédelmi jellemzőjű egyenértékű alternatívát követel meg, kivételekkel. A Sign in with Apple lehetséges tervezési opció; nem állítjuk, hogy minden Google-belépésnél minden körülmény között ugyanaz a kötelezettség. Kiadás előtt a konkrét alkalmazásra kell ellenőrizni. [I; S12]

A szolgáltatói hitelesítési rendszerfelület nem VIZIT-stílusban újrarajzolt jelszóbekérő. Megszakítása visszatérés a belépési oldalra, nem piros általános hiba.

### 8.5. Jogi mezők: tudatos eltérés a régi megfogalmazástól

A master kötelező adatkezelési elfogadást említ. A jelen koncepció **tervezési felülvizsgálatként** különválasztja a feltételek elfogadását, a tájékoztató megismerésének visszajelzését és az esetleges opcionális adatkezelési hozzájárulást. Nem nevezi ezeket automatikusan ugyanannak. [R1, 26. fejezet; T]

Javasolt szövegek: „Elfogadom a felhasználási feltételeket”; „Megismertem az adatkezelési tájékoztatót”; külön, alapból kikapcsolt opcionális kapcsoló például termékértesítésekhez, amennyiben ilyen funkció egyáltalán készül. A tényleges jogalapot és végleges szöveget jogi ellenőrzésnek kell meghatároznia. Ez a dokumentum nem vezet be új hozzájárulás-kezelést.

### 8.6. Első névjegy

ONB-01: megjelenített név és legalább egy telefon vagy e-mail. ONB-02: kép, cég, munkakör, mind kihagyható. ONB-03: az átadható adatok ellenőrzése, majd helyi mentés és szinkronállapot.

Az online profil alapértelmezett javasolt állapota kikapcsolt. A kész névjegy ettől még használható Kontakt QR-rel. A regisztrációs e-mail nem válik automatikusan megosztható névjegyadattá: erről a felhasználó kifejezetten dönt.

<a id="09"></a>
## 9. Névjegy-adatmodell a felület szemszögéből

### 9.1. Tartalmi csoportok

| Csoport | Mezők | Szerkesztési viselkedés |
|---|---|---|
| Azonosító adatok | Megjelenített név, vezetéknév, keresztnév | A megjelenített név kötelező az átadáshoz; nem kizárólag nyugati névszerkezetre épül |
| Szakmai adatok | Cég/szervezet, munkakör, bemutatkozás | Opcionális; üres adat nem hoz létre üres blokkot |
| Telefonok | Érték, típus, elsődlegesség, sorrend | Több elem; nemzetközi formátum; a formázás és a tárolási reprezentáció külön kérdés |
| E-mail-címek | Érték, típus, elsődlegesség, sorrend | Több elem; a névjegy e-mailje nem feltétlenül a belépési cím |
| Címek | Ország, irányítószám, település, címmezők | Opcionális; országfüggő címformátumot nem kényszerít magyar mintára |
| Webhelyek | Címke, URL | Emberileg olvasható domain; biztonságos megnyitás |
| Közösségi linkek | Platform, URL, sorrend | LinkedIn, Instagram, Facebook, TikTok, YouTube és egyéb link |
| Média | Profilkép és céges logó | Külön szerkesztő és külön megoszthatóság |
| Publikus profil | Be/ki, visszaigazolt webcím, publikált változat | A beállítás nem azonos a helyi névjegy létezésével |

A mezők lefedettsége a master profilkövetelményeit követi. [K; R1, 28–33. fejezet] Új adatbázisséma vagy mezőmigráció itt nem készül.

### 9.2. Láthatósági modell

A felület két adatmező-szintű állapotot használ:

**Megosztható:** bekerülhet a névjegy megosztási csatornáiba; a publikus weboldalra csak akkor, ha az egész publikus profil is engedélyezett.  
**Csak nekem:** nem kerülhet NFC-be, QR-be, névjegyfájlba, megosztott szövegbe, publikus válaszba vagy linkelőnézeti metaadatba.

A „Megosztható” szó használata a történeti „public/private” jelölés felhasználóbarát megfogalmazása, nem a hozzáférési modell önkényes bővítése.

A minimum megosztható profil: név és legalább egy **megengedett** telefon vagy e-mail. Egy kizárólag privát telefonszám nem teljesíti ezt. A profil menthető hiányosan, de az adatot igénylő átadás nem indítható félrevezetően.

A csatorna képessége további szűkítést okozhat, soha bővítést. Például a Kontakt QR képet nem tartalmaz, de ettől a kép nélküli előnézetben sem jelenhet meg egy privát telefonszám.

### 9.3. Validációs szerződés

A felület adatvesztés nélkül mutatja a szerver által elutasított mezőt. A címke, segédszöveg és hiba együtt látható; a placeholder nem helyettesíti a címkét.

A név nem ASCII-ra korlátozott; a magyar ékezetek és más írásrendszerek megengedettek. A telefonszámellenőrzés nem követel kizárólag magyar számot. Az e-mail-ellenőrzés nem tilt tiltólistával érvényes hosszabb domaineket. A jelszót nem normalizáljuk és nem csonkoljuk csendben.

Konkrét maximális mezőhossz, elemszám, feltöltési méret és képformátum a későbbi implementáció igazolt szerződéséből érkezzen. Amíg ez nincs összevetve, a prototípus határértékei **tesztadatok**, nem hivatalos API-korlátok. A hosszak ellenőrzésekor külön kezelendő a karakter, Unicode-kódpont és átviteli bájtméret fogalma.

A „Mentés” befejezése után mindig derüljön ki, hogy helyi mentés vagy szerver által is visszaigazolt állapot történt.

<a id="10"></a>
## 10. Névjegyszerkesztés és médiakezelés

### 10.1. Szerkesztőfelépítés

A Névjegyem oldal csoportokra bontott, nem egy végtelen mezőlista. A fejlécben a cím és az előnézet elérése; alatta a releváns mentési állapot; majd az adatok, média, megoszthatóság és publikus profil csoportjai.

Egy új elérhetőség hozzáadása ugyanazt a PRO-04 szerkesztőt nyitja, mint a módosítás. A típus neve a címben és az inputcímkében is szerepel. Törlésnél visszavonható helyi művelet vagy megerősítés szükséges a tényleges következménynek megfelelően; a fióktörlés nem azonos kezelést kap egy még nem mentett üres mező eltávolításával.

### 10.2. Kép és logó

| Művelet | Profilkép | Céges logó |
|---|---|---|
| Választás | Rendszer képválasztó | Rendszer képválasztó |
| Alapmegjelenítés | Négyzetes forrásból kör vagy lekerekített portrékeret | Teljes kép, `contain` szemlélet |
| Átalakítás | Nagyítás, mozgatás, vágás, visszaállítás | Méretezés és belső térköz; automatikus vágás nélkül |
| Ellenőrzés | Kisméretű és nagy névjegy-előnézet | Világos és sötét környezetben is |
| Eltávolítás | Névkezdőbetűs semleges helyettesítő | A logóblokk megszűnik; nem kerül helyére VIZIT-logó |
| Feltöltés | Helyi előnézet azonnal; felhőállapot külön | Ugyanez, külön médiastátusszal |

Az eredeti logógrafika nem torzítható. A szerkesztő ne nevezze „késznek” a felhős képet csak azért, mert a helyi előnézet látható. [K; R1, R2]

A vágási mozdulatok mellett legyen gombos nagyítás/kicsinyítés, középre igazítás és visszaállítás. A lényeges művelet ne legyen kizárólag többujjas gesztussal vagy húzással elérhető. [T; A03]

### 10.3. Sorrend és előnézet

A mezősorrend húzással és „Feljebb / Lejjebb” művelettel is változtatható. A mozgatás után a képernyőolvasó a friss pozíciót közli, nem csak egy animáció látható.

A „Címzett nézete” nem az adatbeviteli űrlap lekicsinyítve. Csak a ténylegesen átadható tartalom, a választott csatorna és az esetleges korlát jelenik meg. A preview fölött egyértelmű címke: „Előnézet — Kontakt QR”, „Előnézet — NFC” vagy „Közzétett webes névjegy”.

<a id="11"></a>
## 11. Megosztási központ és adat-előnézet

### 11.1. Átadás oldal

A lap tetején a név és kis portré azonosítja, melyik névjegy lesz megosztva. Ezután az elsődleges műveletek: **Kontakt QR** és — megfelelő Android-képesség esetén — **NFC**. Másodlagos művelet a profil QR és a „Megosztás másképp”.

Az iOS-változatban nem kerül az elsődleges sávba nem működő NFC-indítógomb. A helyét nem kell üresen megtartani. A platformközi konzisztencia azonos fogalmakat jelent, nem azonos számú működésképtelen vezérlőt.

### 11.2. Kötelező adat-előnézet

A felhasználó minden csatornán ellenőrizheti a most átadandó mezőket. Első használatkor és megosztási tartalmat érintő változtatás után az előnézet a folyamat része. Ismételt használatkor a QR-oldal maga is lehet a rövidített, kibővíthető előnézet; nem kell minden alkalommal három megerősítést kérni.

Az előnézet tartalmi szerződése:

```text
Megosztott névjegy: [megjelenített név]
Módszer: [Kontakt QR / NFC / Profil-link / Szöveg / Export]
Átadott mezők: [a pontos, szűrt lista]
Kép: [átadva / ebben a módban nincs / tömörített / elhagyva]
Webes változat: [aktuális / korábbi publikált változat / nem publikus]
```

Nagyobb módosítás, például kép elhagyása vagy egy túl hosszú mező kihagyása, nem történhet csendben. A felhasználó kifejezetten választhat másik módot.

### 11.3. Adatváltozás aktív megosztás közben

Egy aktív megosztás egyetlen, rögzített előnézeti változathoz tartozik. A háttérszinkron nem cserélheti ki csendben egy olvasás közben a payloadot. Ha a felhasználó a saját felületén visszavon egy mezőt, a futó megosztás leáll, majd új előnézetből indítható.

A már átadott QR-képből, vCardból vagy a címzett által elmentett kontaktból nem vonhatók vissza utólag az adatok. Az online közzététel visszavonása ettől külön funkció, és ezt a privacy-magyarázat kimondja.

<a id="12"></a>
## 12. Kontakt QR és profil QR

### 12.1. Két külön tartalom, két külön ígéret

| Tulajdonság | Kontakt QR | VIZIT profil QR |
|---|---|---|
| Tartalom | Kiválasztott névjegyadatok | HTTPS-profilcím |
| Internet a küldőnél | Mentett helyi adatokból nem szükséges | Meglévő, visszaigazolt címből megjeleníthető offline |
| Internet a fogadónál | Az adatok kiolvasásához nem szükséges | A webes profil betöltéséhez szükséges |
| Profilkép | Nem kerül nagy képadat a QR-ba | A weboldalon megjelenhet |
| Frissülés | A korábban exportált QR tartalma rögzített | Az online cél tartalma változhat |
| Privát online profil | Továbbra is használható a megosztható mezőkkel | Inaktív, ha a publikus cél nem elérhető |

A Kontakt QR elsődlegessége projektkövetelmény; a felismerés konkrét kamera- és kontaktalkalmazás-függő, ezért tesztelendő. [K; R1, 52–63. fejezet]

### 12.2. QR vizuális szerződés

A QR sötét, lehetőleg fekete modulokból áll fehér felületen, mindkét témában. A kódot négy modul széles üres sáv veszi körül; ez **négy modul, nem négy pixel**. Nem vágjuk le, nem fedjük le logóval, és nem használunk gradiensmodulokat az alapváltozatban. [I/T; S16]

Javasolt normál kódterület 256 logikai egység, rugalmas növeléssel. Ez tervezési kiindulás, nem általános olvashatósági garancia. A tényleges méret a modulok számából, az üres sávból és a rendelkezésre álló helyből adódik. Ha a tartalom túl sűrű, a felület nagyobb megjelenítést, kevesebb kiválasztott adatot vagy profil-linket ajánl; a mezők csendes levágása tilos.

A QR alatt rövid címke és a szöveges adatok megnyitása szerepel. Képernyőolvasó számára nem hasznos a teljes kódolt karaktersor felolvasása: helyette a kódtípus, a név és az adatok megtekintése/másolása ad egyenértékű hozzáférést.

### 12.3. Teljes képernyő és fényerő

A kód dominál, de a bezárás és a módszer neve megmarad. Nincs folyamatos pulzálás. A fényerő emelése platform által támogatott módon történhet; az eredeti értéket kilépéskor, háttérbe kerüléskor és megszakításkor is vissza kell állítani. A képesség hiánya nem tiltja a QR megjelenítését.

QR megjelenítése nem kamera-használat, tehát nem indokol kameraengedélyt. [K; R1, 97. fejezet]

### 12.4. Sikeresség és export

„QR megjelenítve” nem „QR beolvasva”. Az app egy egyszerű, offline Kontakt QR kijelzéséből nem tudhatja, hogy ki olvasta be és elmentette-e.

QR-kép exportálása előtt: „A képen lévő adatok később is kiolvashatók.” Profil-link megosztásakor: „A link megnyitásához internetkapcsolat szükséges.” Két eltérő szöveget használunk, nem általános „sikeresen átadva” üzenetet.

<a id="13"></a>
## 13. NFC-folyamat és képességalapú működés

### 13.1. Miért szükséges külön képességvizsgálat?

Az Android HCE hivatalos platformképesség, de az elérhetősége és működése rendszer- és eszközfeltételektől függ. Ebből nem következik, hogy minden fogadó automatikusan kontaktként importálja a VIZIT-adatot. Az iOS Core NFC és az Android HCE nem ugyanaz a képesség. [I; S17, S18]

A vizsgált iOS-dokumentáció nem támogatottként kezeli az Android-szerű névjegyemulációt. A jelen iOS-koncepció sem ígéri ezt. Ez nem általános állítás minden jelenlegi vagy jövőbeli, speciális Apple-jogosultságról; a **VIZIT igazolt képességére** vonatkozó szabály. [R3]

### 13.2. NFC-előellenőrzés

| Vizsgálat | Megjelenítés | Következő lépés |
|---|---|---|
| Nincs NFC-hardver | „Ezen a készüléken az NFC-átadás nem érhető el.” | Kontakt QR |
| NFC kikapcsolva | „Kapcsold be az NFC-t a rendszerbeállításokban.” | Beállítások megnyitása; visszatéréskor új ellenőrzés |
| HCE/alkalmazásoldali képesség hiányzik | „Ez az átadási mód itt nem használható.” | QR; részletes segítség |
| Csak a fogadó kompatibilitása ismeretlen | „A fogadó készülék támogatása még nem igazolt.” | Dokumentált próba vagy QR, kész működés ígérete nélkül |
| Profil nem megosztható | Pontos hiányzó adat | Szerkesztés |
| Payload nem készíthető megbízhatóan | „Ezekkel az adatokkal az NFC-átadás nem indítható.” | Adatok áttekintése; Kontakt QR / profil-link |
| Minden helyi feltétel megfelelő | Átadandó adatok és rövid útmutató | Külön „NFC indítása” gomb |

A rendszer fizetési alapértelmezését nem állítjuk át automatikusan. Egy bankkártyafelület megjelenése nem VIZIT-sikerjelzés; ilyenkor az átadás leállítható, és a QR-alternatíva közvetlenül elérhető.

### 13.3. Állapotgép

```text
inaktív
  → előellenőrzés
  → készen áll az indításra
  → felhasználó: NFC indítása
  → aktív, olvasható névjegyváltozat
      → teljes payload igazoltan kiolvasva → műszaki átadás megtörtént
      → részleges olvasás / kapcsolat megszakadt → nem igazolt átadás
      → Leállítás / navigáció / zárolás / kijelentkezés → inaktív
      → időkorlát lejárt → inaktív, újraindítás lehetőségével
```

Az aktív felületen a név, átadott tartalom rövid előnézete, eszközérintési instrukció és nagy „Leállítás” művelet látható. A rendszer részleteit nem APDU-üzenetekben magyarázza a felhasználónak.

### 13.4. Időkorlát és akadálymentesség

Javasolt alap aktív időablak 120 másodperc, indítás előtt 2/5/10/20 perces választási lehetőséggel. Ez termékbiztonsági és használhatósági döntés, nem NFC-szabványból vett idő. Az előre állítható tartomány legalább tízszeres, így nem egy rejtett, néhány másodperces gesztus a hozzáférhetőség feltétele. A zárolás és kifejezett leállítás minden beállítás mellett megszünteti az olvashatóságot. [T; A04]

A lejárat előtt megjelenő figyelmeztetés nem takarja el a Stop gombot; hosszabbítás és újraindítás egyszerű művelet. Nem alkalmazunk automatikusan „biztonsági kivételt” minden időkorlátra hozzáférhetőségi vizsgálat helyett.

### 13.5. Pontos eredményjelzés

| Bizonyíték | Megengedett szöveg | Nem megengedett állítás |
|---|---|---|
| A megosztás aktiválódott | „NFC-átadás aktív.” | „A névjegy átadva.” |
| Teljes payload kiolvasása igazolt | „A névjegyadatokat a másik készülék kiolvasta.” | „Kontakt mentve.” |
| Csak részleges olvasás történt | „Az átadás megszakadt. Próbáld újra, vagy használj QR-kódot.” | „Sikeres átadás.” |
| A fogadó rendszerében tényleges mentés külön igazolt | „Kontakt mentve” kizárólag annak a visszajelzésnek a körében | Általános mentésszám a puszta NFC-eseményből |

A vCard 3.0 a projekt dokumentált kompatibilitási választása. Az RFC 6350 a 4.0 formátum alapdokumentuma, amely felváltotta az RFC 2426-ot; nem nevezzük a 3.0-t a legújabb szabványnak, és UI-tervezés címén nem változtatunk formátumot. [R1, 39. fejezet; S15]

<a id="14"></a>
## 14. Fogadás, QR-beolvasás és rendszeroldali kontaktmentés

A felhasználó saját QR-olvasót használhat, de az alapnévjegy-fogadás nem függhet ettől. A VIZIT-be épített olvasó kényelmi funkció, nem a névjegyhez való hozzáférés kapuja.

### 14.1. Olvasófolyamat

REC-01 megnyitásakor csak szükség esetén jelenik meg a kameraengedély. A kamera leáll, ha a felület háttérbe kerül. Egy felismerés után az olvasás szünetel, ezért ugyanaz a kód nem nyit több egymásra rakódó modalt.

Kontaktadat esetén REC-02 megmutatja a nevet, elérhetőségeket és a hiányzó/értelmezhetetlen adatot. HTTPS-cím esetén a domain és a teljes cél megtekintése után külön „Webcím megnyitása” művelet következik. Ismeretlen sémából vagy kódolt parancsból nem lesz automatikusan végrehajtás.

Nincs olyan szöveg, hogy „Ellenőrzött személy”, csak azért, mert a QR szintaktikailag olvasható vagy VIZIT-profilt nyit.

### 14.2. Rendszer Kontaktok

A névjegy rendszeroldali hozzáadását a platform saját szerkesztője kezelje, ahol az adott megoldás ezt támogatja. A végső Mentés a címzett döntése. A VIZIT nem rajzol a rendszer nevében hamis jogosultsági vagy mentési ablakot.

Ha az alkalmazás nem kap megbízható mentési visszajelzést, a saját szöveg: „A névjegyet megnyitottuk a Kontaktokban. A mentést ott fejezheted be.” Megszakítás után nincs sikerkártya.

A duplikációkezelést lehetőség szerint a rendszer Kontaktok végzi. Ha a VIZIT-nek ehhez nincs hozzáférése, nem állítja, hogy ellenőrizte a teljes telefonos címjegyzéket, és nem kér széles címjegyzékjogot pusztán e kényelmi ellenőrzésért.

<a id="15"></a>
## 15. Publikus webes névjegy

### 15.1. Tartalmi hierarchia

PUB-01 sorrendje: visszafogott VIZIT-azonosítás; profilkép és teljes név; munkakör/cég és külön céges logó; elsődleges kontaktmentési művelet; megengedett kapcsolatfelvételi műveletek; további linkek és bemutatkozás; jogi és segítségelérés.

Nem szükséges regisztráció, alkalmazásreklámot eltakaró popup vagy teljes képernyős „Töltsd le az appot” közbeiktatás. Natív app megnyitása másodlagos lehetőség, nem kényszer.

A hosszú nevet tördeljük. A telefonszámok és e-mail-címek másolhatók; nem csak ikonokból kell kitalálni, melyik művelet indul. A telefon, e-mail és cím gombjai saját, ellenőrzött csatornájukat nyitják. A cím megadása önmagában nem indokol helymeghatározási engedélyt.

### 15.2. Kontaktmentés a weben

A tervezési cél a lehető legközvetlenebb rendszeroldali névjegymentés. A böngésző és operációs rendszer viselkedését azonban a UI-terv nem tudja felülírni. A `.vcf` átadás egyes környezetekben importot, másutt letöltést eredményezhet.

Az igazoltan támogatott készülékfolyamat kapja az elsődleges „Kontakt mentése” műveletet. A fájlletöltést és kézi importot igénylő változat külön, egyértelműen megnevezett alternatíva, nem a közvetlen importtal egyenértékű siker. A weboldal nem ígér észrevétlen címjegyzékbe írást. [K; R1, 36–37., 63–66. fejezet; T]

### 15.3. Publikáció és adatvédelem

A hálózati válaszban, képek hozzáférésében és a megosztási metaadatokban is csak közzétehető adat szerepelhet; CSS-sel elrejteni egy privát telefonszámot nem elegendő. [K; R1, 29., 66., 84., 132–134. fejezet]

A kikapcsolt, törölt vagy ismeretlen profil semleges unavailable felületet kap. A publikus névjegyet nem tároljuk automatikusan korlátlanul használható offline másolatként a fogadó webes felületén. A visszavonás nem tudja törölni a címzett korábbi képernyőképét vagy elmentett kontaktját; ezt nem is ígérjük.

A keresőindexelés és a közösségi linkelőnézet külön tervezési döntés. Javasolt alap: nincs keresőindexelés, és minimális linkelőnézeti tartalom. A `noindex` nem hozzáférésvédelem, ezért nem helyettesíti a publikus állapot valódi ellenőrzését.

### 15.4. Webes elrendezés

Egyoszlopos, legfeljebb 720 CSS px tartalomszélességű névjegy, 320 CSS px szélességen is használható tördeléssel. A hosszú tartalom függőlegesen görgethető; nincs beégetett, egyetlen kijelzőmagasságra zsugorító elrendezés. Nagy képernyőn nyugodt környezetet kap, nem nyúlik a névjegy az egész monitoron keresztül.

Sticky műveletsáv csak akkor használható, ha nem takarja el a fókuszált elemet vagy annak hibáját. Billentyűzetes használathoz tartalomra ugró hivatkozás és értelmes címsorstruktúra tartozik. [T; A05, A06]

<a id="16"></a>
## 16. Offline állapot, szinkron és ütközéskezelés

### 16.1. Négy külön állapottengely

| Tengely | Állapotok | Miért külön? |
|---|---|---|
| Szerkesztés | Nem módosított / Piszkozat / Helyileg mentett | A helyi mentés nem azonos a felhőszinkronnal |
| Felhő | Szinkronban / Várakozik / Szinkronizál / Hiba / Ütközés | A profil nem veszhet el pusztán hálózati hiba miatt |
| Média | Helyi / Feltöltésre vár / Feltöltött / Hibás | Szöveg és kép eltérő időben készülhet el |
| Publikáció | Kikapcsolt / Bekapcsolásra vár / Publikus / Kikapcsolásra vár / Hiba | A helyi kapcsoló nem bizonyít szerveroldali hozzáférésváltozást |

A képernyő ezeket nem négy állandó technikai jelvényként jeleníti meg. Normál esetben csendes „Szinkronban” állapot vagy semleges státusz; probléma esetén egy rövid, feladathoz kötött üzenet és SYN-01 részletek.

### 16.2. Fontos átmenetek

```text
Szerkesztés → Mentés
  → helyi mentés sikeres → az adat helyben használható
      → offline: szinkronra vár
      → online: szinkron indul
          → visszaigazolt → felhőben is mentve
          → elutasított → a helyi változat megmarad, javítható
          → ütközik → SYN-02

Publikus profil kikapcsolása offline
  → helyben a megosztási linket nem kínáljuk újra
  → figyelmeztetés: a korábbi online oldal még elérhető lehet
  → hálózat után visszavonási kérés
  → csak szerver-visszaigazolás után „Publikus profil kikapcsolva”
```

Adatelrejtés vagy publikáció-visszavonás esetén a figyelmeztetés tartós, nem öt másodperces eltűnő toast. Ez a VIZIT egyik legfontosabb privacy-állapota.

### 16.3. Ütközésképernyő

SYN-02 két olvasható változatot mutat: „Ezen a készüléken” és „A fiókban”. A módosított mezők neve, értéke és ismert időpontja szerepel; privát adatok nem mennek külső diagnosztikai szolgáltatásba.

Biztonságos lehetőségek: az egyik változat megtartása, mezőnkénti áttekintés, döntés elhalasztása. Ha mezőszintű összevonás műszakilag nem támogatott, a felület nem imitálja. Az elutasított helyi adatot nem töröljük csak azért, mert új felhőváltozat érkezett.

A láthatóság ütközésekor a konzervatívabb, nem megosztó helyi viselkedés az alap addig, amíg nincs döntés. A korábbi online publikáció esetleges fennmaradását ettől függetlenül jelezni kell.

### 16.4. Munkamenet és kijelentkezés

Hitelesítési hiba és hálózathiány eltérő állapot. Újbóli belépés után a saját, megfelelő felhasználóhoz kötött változások folytathatók.

Kifejezett kijelentkezéskor az NFC leáll, a hitelesítési állapot megszűnik és a felhasználó privát helyi adatai nem maradnak másik fióknak elérhetők. Előtte a SET-12 pontosan jelzi, ha még nem szinkronizált változás elveszne. A rendszer nem állítja „biztonságosan mentve a felhőben”, ha csak a kijelentkezési párbeszédben ígéret hangzott el.

<a id="17"></a>
## 17. Beállítások, adatvédelem és fiókkezelés

A megjelenési beállítás: rendszer követése, világos vagy sötét. Az alkalmazás saját kapcsolója nem kapcsolhatja ki a rendszer nagybetűs, csökkentett mozgású vagy képernyőolvasós használatát.

A fiókoldal a belépési identitást mutatja, de nem keveri a névjegy e-mail-listájával. Egy providerrel létrehozott fióknál a jelszóváltoztatás elérhetőségét a tényleges hitelesítési mód határozza meg.

### 17.1. Fióktörlési folyamat

SET-08 felsorolja a következményeket: fiók, saját profil, média és megosztási célok megszűnése; korábban mások által elmentett kontaktok nem törölhetők visszamenőleg. Szükség esetén SET-05 újrahitelesítés következik.

A végleges művelet külön destruktív gombot kap. Előtte biztonságos kilépés, utána valós folyamatjelzés. Offline állapotban a végleges törlés nem „sikeres”. Ismeretlen kimenetű hálózati megszakadáskor az eredményt ellenőrizni kell, nem vakon újabb törlést indítani vagy késznek jelölni.

Fióklétrehozást kínáló appnál az Apple irányelve alkalmazáson belüli fióktörlési lehetőséget is megkövetel. [I; S12] A szerveroldali törlési folyamat tényleges implementálása nem ennek a dokumentumnak a feladata.

### 17.2. Súgó és diagnosztika

A súgó a konkrét feladatra válaszol: NFC nem indul; másik telefon bankkártyát mutat; QR nem olvasható; nem érkezik e-mail; szinkronra vár; nem elérhető publikus profil.

A felhasználói diagnosztika appverziót, környezetet, operációs rendszert, helyi NFC- és hálózati állapotot mutathat. Nem mutathat tokent, teljes APDU-tartalmat, teljes névjegyet, titkos kulcsot vagy jelszót. Támogatásnak elküldés előtt az adatcsomag tartalma ellenőrizhető és a küldés kifejezett művelet.

Adatexport, eszköz-munkamenetlista és fejlett értesítési beállítás csak akkor jelenik meg aktív funkcióként, ha mögötte tényleges szolgáltatás áll. Bővítési helyük a 29. fejezetben szerepel.

<a id="18"></a>
## 18. Vizuális irány és márkahasználat

### 18.1. Vizuális karakter

A VIZIT felülete professzionális, személyes és nyugodt. A legfontosabb vizuális tárgy a névjegy: valódi tartalommal kitöltött név, portré, szakmai szerep és elérhetőségek. A szín a cselekvéseket és az állapotot segíti, nem mindent egyforma kék kártyává alakít.

A fő felület nem teljes sötét neonháló, nem állandó glow-animáció, nem átlátszó üvegrétegek halmaza. A helyes hierarchia üres térből, tipográfiából, csoportosításból és néhány következetes hangsúlyból épül.

### 18.2. Hivatalos márka

A megőrzött színek: Navy `#061B46`, Blue `#055EEC`, Cyan `#13D1FC`, Ice `#EAF8FF`. A hivatalos logó forrása `brand/vizit-logo-master.png`; a vizsgált brandingdokumentumban közölt SHA-256: `31850228434482fd56321dc2bfd4450025e3bb4650367331fc079a5c311efee6`. A hash a forrásdokumentum adata, nem itt újonnan elvégzett képfájl-hitelesítés. [K; R2]

A logót nem tervezzük át. A teljes feliratos változat auth/onboarding/About felületen, a kompakt V + NFC márkajel kisebb helyeken jelenhet meg. A sötét feliratú teljes logó kontrollált fehér felületet kap világos és sötét témában is. [K; R2]

Tervezési javaslat: a márkajel körül legalább a jel magasságának egynegyedével megegyező védőtér. Ez nem a meglévő arculati dokumentumból kiolvasott hivatalos érték, hanem ellenőrzendő kiegészítés.

A céges logó soha nem veszi át az alkalmazásazonosító helyét. A céges grafika nem színeződik automatikusan át a VIZIT témájához.

### 18.3. Fő képernyők szerkezeti vázlata

A következő vázlatok hierarchiát jelölnek, nem végleges grafikai képet vagy minden kijelzőre rögzített koordinátát.

```text
KEZDŐLAP
[VIZIT márkajel]                              [állapot]

A névjegyed
┌─────────────────────────────────────────────────┐
│ [portré]   Kiss Anna                            │
│            Tervező · Példa Stúdió               │
│            [céges logó, teljes grafika]         │
│ anna@example.com                               │
│ [Címzett nézete]                                │
└─────────────────────────────────────────────────┘
[ Kontakt QR ]                  [ NFC, ha elérhető ]
Névjegy szerkesztése
[Csak szükség esetén: szinkron-/hiányüzenet]

Kezdőlap       Névjegyem       Átadás       Beállítások
```

```text
ÁTADÁS
[Azonosító mininévjegy]

Kontakt QR                  NFC
Közvetlen névjegyadatok      Támogatott Android-környezetben

Profil QR                   Megosztás másképp
Online névjegy              Link, szöveg, QR-kép, külön export

[Mit adok át?]               [QR beolvasása]
```

```text
NÉVJEGY SZERKESZTÉSE
[Vissza]       Elérhetőségek                 [Mentés]

Telefonok
[Munka]  [érték]           [Megosztható]       [Továbbiak]
+ Telefonszám hozzáadása

E-mail-címek
[Munka]  anna@example.com  [Megosztható]       [Továbbiak]
+ E-mail-cím hozzáadása

A megosztható adatok kerülhetnek a névjegyedbe.
[Helyileg mentve / szinkronállapot, amikor releváns]
```

A mintaadatok fiktívek, a prototípusban nem személyes vagy éles ügyféladatok szerepelnek.

<a id="19"></a>
## 19. Színrendszer és ellenőrzött kontrasztok

### 19.1. Szemantikus tokenek

A tokennevek az alkalmazott szerepet nevezik meg. A táblázat dokumentáció, nem automatikusan importálható tokenkonfiguráció.

| Token | Világos | Sötét | Szerep |
|---|---|---|---|
| `color.bg.canvas` | `#F6F8FC` | `#0B1220` | Oldalháttér |
| `color.bg.surface` | `#FFFFFF` | `#121E32` | Kártya, űrlap, sheet |
| `color.bg.raised` | `#FFFFFF` | `#1B2941` | Kiemelt réteg |
| `color.text.primary` | `#061B46` | `#F1F5F9` | Fő olvasható tartalom |
| `color.text.secondary` | `#475569` | `#CBD5E1` | Segédszöveg és másodlagos adat |
| `color.action.primary` | `#055EEC` | `#8EB7FF` | Elsődleges művelet háttér és releváns link |
| `color.action.onPrimary` | `#FFFFFF` | `#061B46` | Elsődleges gomb felirata |
| `color.accent.decorative` | `#13D1FC` | `#13D1FC` | Nem szöveges márkahangsúly; kontrasztvizsgálattal |
| `color.brand.ice` | `#EAF8FF` | `#EAF8FF` | Kontrollált márkafelület; nem általános sötét kártyaszín |
| `color.border.control` | `#64748B` | `#94A3B8` | Felismeréshez szükséges vezérlőhatár |
| `color.border.decorative` | `#DBE3EF` | `#334155` | Nem információhordozó elválasztó |
| `color.status.success` | `#15803D` | `#86EFAC` | Siker szöveggel és ikonnal |
| `color.status.warning` | `#B45309` | `#FCD34D` | Figyelmeztetés szöveggel és ikonnal |
| `color.status.error` | `#B91C1C` | `#FCA5A5` | Hiba szöveggel és ikonnal |
| `color.qr.foreground` | `#000000` | `#000000` | QR-modul |
| `color.qr.background` | `#FFFFFF` | `#FFFFFF` | QR-terület és üres sáv |

Az eredeti négy márkaszín **K**, a további szemantikus paletta **T**. Az Android Dynamic Color továbbra sem írhatja felül automatikusan a márkát. [R2]

### 19.2. Kiszámolt ellenőrző párok

Az alábbi értékek sRGB relatív luminancia alapján, átlátszatlan, homogén felületekre számolt arányok. A megjelenített érték kerekített; a minősítés a kerekítés előtti eredmény alapján történt. A valós komponensnél az összes állapotot, opacitást és tényleges hátteret külön ellenőrizni kell. [Módszer: A07]

| Előtér / háttér | Arány | Következtetés |
|---|---:|---|
| Navy `#061B46` / fehér | 16,76 : 1 | Normál szöveghez megfelelő |
| Blue `#055EEC` / fehér | 5,50 : 1 | Normál szöveghez és fehér feliratú gombhoz megfelelő |
| Cyan `#13D1FC` / fehér | 1,82 : 1 | Nem megfelelő normál szövegnek vagy önálló lényeges ikonjelzésnek |
| Navy / Cyan | 9,22 : 1 | Kontrollált színpárként olvasható |
| `#475569` / fehér | 7,58 : 1 | Másodlagos szöveghez megfelelő |
| `#F1F5F9` / `#0B1220` | 17,09 : 1 | Sötét főszöveg megfelelő |
| `#CBD5E1` / `#121E32` | 11,25 : 1 | Sötét másodlagos szöveg megfelelő |
| `#8EB7FF` / `#121E32` | 8,24 : 1 | Sötét témájú link megfelelő |
| Navy / `#8EB7FF` | 8,27 : 1 | Sötét témájú elsődleges gomb megfelelő |
| `#15803D` / fehér | 5,02 : 1 | Siker szöveges megjelölésére megfelelő |
| `#B45309` / fehér | 5,02 : 1 | Figyelmeztető szöveghez megfelelő |
| `#B91C1C` / fehér | 6,47 : 1 | Hibaüzenethez megfelelő |
| `#64748B` / `#F6F8FC` | 4,48 : 1 | Normál szöveghez nem elég; ezért nem másodlagos szövegtoken |

A normál szöveg WCAG AA kontrasztminimuma 4,5:1; nagy szövegre meghatározott feltételekkel 3:1. [SZ; A07] A VIZIT a fő szövegstílusoknál nem a nagybetűs kivételre támaszkodik.

A deaktivált vezérlő, díszítővonal és logó speciális szabványi kezelése nem felhatalmazás a lényeges tájékoztatás olvashatatlanná tételére. A letiltás okát normál olvasható szövegben kell közölni.

<a id="20"></a>
## 20. Tipográfia, térközök és felületi geometria

### 20.1. Betűrendszer

Androidon a platformhoz illeszkedő natív szövegstílus, iOS-en rendszerbetű és Dynamic Type, weben rendszerbetűs alap javasolt. A logó feliratát nem használjuk teljes alkalmazásbetűként. Nem szükséges új, külső fontszolgáltatást bekötni.

| Szerep | Android kiindulás | iOS kiindulás | Web kiindulás |
|---|---|---|---|
| Fő cím | 28 sp, 34 sp sormagasság | Natív title-stílus, skálázható | 1,75 rem / 1,25 |
| Szekciócím | 20 sp / 28 sp | Natív title3/headline szerep | 1,25 rem / 1,4 |
| Név a fő névjegyen | 24 sp / 32 sp | Natív title2 szerep | 1,5 rem / 1,3 |
| Törzsszöveg | 16 sp / 24 sp | Natív body, alapméretben jellemzően 17 pt | 1 rem / 1,5 |
| Mező- és gombcímke | 14–16 sp, médium | Natív body/headline a szerep szerint | 0,875–1 rem / 1,4 |
| Segédszöveg | 14 sp / 20 sp | Natív subheadline/footnote szerep | 0,875 rem / 1,45 |

Az értékek **tervezési kiindulások**, nem a teljes natív típusskála lemásolása. iOS-en az accessibility mérettartományokig alkalmazkodó elrendezés kell, nem csupán a betű fix értékének növelése. [I; S11]

A fontos telefonszámot, e-mailt, hibát, gombfeliratot és nevet nem zsugorítjuk automatikusan apró betűre. A címek több sorba törhetnek. A gomb és sor magassága a nagyobb szöveggel nőhet.

### 20.2. Térközskála

Dokumentált alapskála: 4, 8, 12, 16, 20, 24, 32, 48 és 64 logikai egység. Az adott platform dp/pt/CSS px használata nem állít fizikai azonosságot.

| Szerep | Javasolt érték |
|---|---:|
| Ikon–felirat távolság | 8 |
| Címke–mező távolság | 8 |
| Összetartozó mezők között | 12–16 |
| Kártya belső térköze | 16–20 |
| Mobil oldalmargó | 16; tágabb nézetben 24 |
| Szekciók között | 24–32 |
| Két elsődleges művelet között | 12 |
| Űrlap kívánt maximális tartalomszélessége | 640 |

### 20.3. Geometria

Mezők és elsődleges gombok kiinduló magassága 52 logikai egység, nagy szövegnél tartalomfüggően növelve. Önálló ikonok 20–24 egység körüliek, de a találati terület platformminimum feletti. Adatsor minimálisan 56 egység, kétsoros tartalommal nagyobb.

Javasolt sugárskála: mező/gomb 12, kisebb kártya 16, fő névjegykártya 24; teljes kör csak portréhoz és valóban kör alakú vezérlőhöz. Nem minden elem pill alakú.

Az árnyék szerepe réteghatár, nem dísz. Alap kártya lehet árnyék nélkül, kontrasztos felületváltással; lebegő sheet/modális réteg natív vagy visszafogott árnyékkal. Az olvashatóság nem támaszkodhat kizárólag halvány árnyékra.

<a id="21"></a>
## 21. Adaptív elrendezések és platformeltérések

### 21.1. Android ablakméret-osztályok

Az aktuálisan ellenőrzött Android-dokumentáció öt szélességi osztályt ad meg: compact 600 dp alatt; medium 600–839 dp; expanded 840–1199 dp; large 1200–1599 dp; extra-large 1600 dp-től. A rendelkezésre álló ablak számít, nem a készülék marketingneve. [I; S07]

A VIZIT elrendezési döntése:

| Elérhető szélesség | Elrendezés |
|---|---|
| Compact | Egy oszlop; alsó natív navigáció; teljes szélességű szerkesztő |
| Medium | Korlátozott tartalomszélesség; a magasság és környezet szerint alsó navigáció vagy rail |
| Expanded | Oldalsó navigáció; ahol értelmes, szerkesztő + élő előnézet két panelen |
| Large / Extra-large | Ugyanaz a tartalmi rendszer, maximált szélességgel; nem keletkezik új funkció csak a nagy ablak miatt |

Alacsony fekvő nézetben a QR és műveletek helye magasságfüggően is változik. Nyitott billentyűzetnél a fókuszált mező és hiba látható marad. Hajtható eszközön az érdemi tartalom nem kerül hajtás/takarás alá.

### 21.2. iOS és iPadOS

iPhone-on natív tabbar és navigációs verem; iPaden adaptív sidebar/split szemlélet. A rendszer saját biztonságos területeit, modális megjelenítését és visszagesztusát nem helyettesítjük Androidból átemelt fix méretekkel.

A Dynamic Type nagyobb méretein a többoszlopos mezőcsoport egy oszlopra vált; a kis ikonműveletek feliratos listaműveletté válhatnak. A rendszermegosztó, fotóválasztó, engedélykérés és Kontaktok-szerkesztő az operációs rendszer saját felülete marad. [I/T; S10–S11]

A rendszer aktuális vizuális effektjeit nem másoljuk le kézzel minden korábbi OS-verzióra. A natív vezérlő megjelenhet platformverzió szerint, a VIZIT tartalmi felülete pedig megőrzi saját márkáját.

### 21.3. Web

Saját CSS-töréspontok: 600 és 840 CSS px mint tervezési kiindulás, 320 CSS px működési ellenőrzéssel. Ezek nem az Android dp-osztályok fizikai megfeleltetései.

A publikus névjegy minden szélességen egy jól követhető olvasási sorrendű tartalom. Desktopon legfeljebb a kapcsolatfelvételi műveletek rendezhetők több oszlopba; a sorrend billentyűzettel és képernyőolvasóval nem cserélődhet fel értelmetlenül.

### 21.4. Folyamatok platformtérképe

| Folyamat | Android | iOS/iPadOS | Publikus web |
|---|---|---|---|
| Saját névjegy kezelése | Natív alkalmazás | Natív alkalmazás | Nem része az alap fogadóoldalnak |
| Kontakt QR | Helyi generálás | Helyi generálás | Fogadás kamera/OS útján |
| NFC-küldés | Képesség és teszt szerinti HCE | A jelen VIZIT-koncepcióban nem támogatottként kezelt | Nem ígérünk böngészős telefonemulációt |
| Linkmegosztás | Rendszermegosztó | Rendszermegosztó, elérhető AirDrop-céllal | Másolás / támogatott rendszermegosztás |
| Kontaktmentés | Rendszerfolyamat | Rendszerfolyamat | Böngésző/OS szerint, tesztelt alternatívával |
| Offline szerkesztés | Saját helyi adatok | Saját helyi adatok | Nem része az alap publikus profilnak |

<a id="22"></a>
## 22. Komponensarchitektúra

### 22.1. Rétegek

```text
Alapok
  szín, tipográfia, térköz, forma, ikon, mozgás
       ↓
Alapvezérlők
  gomb, beviteli mező, kapcsoló, választó, jelvény, modal
       ↓
VIZIT-tartalmi komponensek
  névjegy, elérhetőségsor, láthatóság, QR-panel, NFC-állapot
       ↓
Feladatminták
  ellenőrzött űrlap, megosztási előnézet, szinkronütközés
       ↓
Képernyősablonok
  auth, lista–részlet, szerkesztő, átadás, publikus névjegy
       ↓
Konkrét képernyők és folyamatok
```

Ez felületi felelősségi modell, nem új alkalmazásarchitektúra vagy kötelező mappaszerkezet. A meglévő működő kód átépítését nem rendeli el.

### 22.2. Komponenskatalógus

| Komponens | Tartalmi szerződés | Szükséges állapot/variáns |
|---|---|---|
| `AppShell` | Cím, fő navigáció, biztonságos terület, tartalom | Compact/adaptív; világos/sötét |
| `TopBar` | Vissza, cím, legfeljebb néhány releváns művelet | Normál, szerkesztő, hosszú cím |
| `PrimaryAction` | Egyértelmű felirat és eredmény | Normál, nyomott, fókusz, folyamatban, tiltott |
| `SecondaryAction` | Másodlagos művelet | Ugyanez; nem versenyez az elsődlegessel |
| `DestructiveAction` | Visszafordíthatatlan vagy adatvesztő művelet | Megerősítéshez kötött, folyamatban |
| `IconAction` | Látható vagy akadálymentes név | Fókusz, nyomott; megfelelő találati terület |
| `LabeledInput` | Címke, érték, példa, segítség, hiba | Üres, kitöltött, fókusz, hibás, csak olvasható |
| `PasswordInput` | Jelszó és megjelenítési művelet | Beillesztés/kitöltés támogatott; nincs titoklog |
| `ChoiceControl` | Egyértelműen leírt választás | Kijelölt, nem kijelölt, tiltott, hiba |
| `VisibilityControl` | Megosztható / Csak nekem és magyarázat | Függő online változás külön megjeleníthető |
| `ContactRow` | Típus, érték, láthatóság, művelet | Hosszú érték, elsődleges, privát, hibás |
| `ReorderRow` | Tartalom és sorrend | Húzás + gombos mozgatás; új pozíció közlése |
| `ProfileAvatar` | Portré vagy névkezdőbetű | Kép, helyettesítő, feltöltés, hibás kép |
| `CompanyLogo` | Teljes grafika és saját keret | Transzparens, hosszú, álló, fekvő, hiányzó |
| `VizitBrandMark` | Hivatalos márkajel | Jóváhagyott teljes/kompakt változat |
| `BusinessCard` | Név, szerep, cég, kép, kiválasztott adatok | Saját/előnézet/publikus; hiányos/hosszú adat |
| `ShareMethodCard` | Módszer neve, szerepe, tényleges elérhetősége | Elérhető, előfeltételes, nem támogatott |
| `QrPanel` | Kódkép, quiet zone, kódtípus, alternatív tartalom | Kontakt/profil; normál/teljes képernyő |
| `NfcSessionPanel` | Állapot, adatváltozat, idő, leállítás | Készen áll, aktív, részleges, kiolvasott, lejárt |
| `SyncStatus` | Felhasználó számára releváns mentési helyzet | Vár, szinkronizál, hiba, ütközés |
| `InlineMessage` | Rövid ok és következő lépés | Info, siker, figyelmeztetés, hiba |
| `EmptyState` | Miért üres és hogyan tölthető fel | Első használat, nincs adat, nincs jogosultság |
| `ErrorState` | Érthető probléma és valódi javítási út | Javítható, újrapróbálható, nem támogatott |
| `ConfirmDialog` | Következmény és konkrét döntés | Normál/destruktív; nincs semmitmondó OK |
| `ConflictCompare` | Helyi/felhős értékek és döntés | Teljes vagy mezőnkénti, ha valóban támogatott |
| `SystemHandoff` | Átadott tartalom és rendszerhatár | Elindult, megszakított, igazolt visszatérés |

### 22.3. Szemantika és variánsok

Egy komponensben a vizuális állapot és a hozzáférhetőségi név/érték együtt változik. A kijelölt tabot nem csak kék szín jelöli; a láthatósági kapcsoló felolvasható állapotot ad. Egy ismeretlen hálózati állapotot nem „disabled” szürke mezőként magyarázunk.

A variánsokat szerep, méret, állapot és platform szerint szervezzük, de nem gyártunk minden elméleti kombinációból külön komponenst. Például a QR hibája a panel állapota, nem száz külön QR-komponens.

Weben az alap interaktív szemantika natív HTML-vezérlő. ARIA nem helyettesíti a hiányzó működést. Androidon és iOS-en a natív accessibility-modell megfelelő szerepeit használjuk, nem webes attribútumokat másolunk a tervbe.

<a id="23"></a>
## 23. Interakció, fókusz, mozgás és visszajelzés

### 23.1. Fókusz

Webes modal megnyitásakor értelmes kezdőfókusz, a modalon belüli billentyűzetkezelés, háttérinterakció tiltása és bezáráskor visszaadott fókusz szükséges. Hosszú vagy destruktív tartalomnál nem feltétlenül a veszélyes gomb az első fókusz. [I; S13]

A fókuszjel javasolt kialakítása legalább 2 egység széles gyűrű, megfelelő kontrasztú elválasztással. Ez saját vizuális szabály, nem a WCAG 2.4.13 AAA követelmény automatikus teljesítésének állítása. A fő ellenőrzés: felismerhető, és a komponens nem kerül teljesen takarásba. [A06; T]

### 23.2. Visszajelzési csatornák

| Esemény | Felület |
|---|---|
| Mezőhiba | A mezőnél maradó szöveg és szükség esetén hibaösszegzés |
| Rövid, nem kritikus siker | Rövid állapotjelzés; képernyőolvasónak közölhető |
| Adatvesztés, privacy- vagy publikációs probléma | Tartós inline/banner állapot, elérhető részletekkel |
| Hálózati művelet | Gombon vagy érintett tartalomnál progress; nincs teljes appot feleslegesen blokkoló overlay |
| Irreverzibilis döntés | Külön megerősítési folyamat |

A státuszváltozás akkor is közölhető, ha a fókusz nem kerül át rá. Nem minden szinkronpróbálkozás hangzik el újra és újra. [I/SZ; A08]

### 23.3. Mozgás és haptika

Javasolt mozgási tartomány: rövid állapotváltás 100–150 ms; lap-/sheetátmenet 180–250 ms. Az értékek nem hivatalos platformminimumok. A mozgás a kapcsolatot magyarázza, nem díszítő „lebegés”.

Csökkentett mozgásnál nincs nagy zoom, parallax vagy folyamatos NFC-pulzus. Az állapot szöveggel és statikus grafikával is érthető. A QR soha nem mozog olvasás közben.

Haptika legfeljebb fontos indítás, igazolt műveleti eredmény vagy figyelmeztetés esetén; nem minden gombnál. Hang és rezgés nem az egyetlen visszajelzés. A rendszer és a felhasználó beállításai elsőbbséget élveznek.

<a id="24"></a>
## 24. Engedélyek és biztonsági felületek

### 24.1. Engedélymátrix

| Funkció | Kérés időpontja | Elutasítás utáni út |
|---|---|---|
| QR megjelenítése | Nincs kameraengedély-kérés | Változatlanul működik |
| QR beolvasása | A beolvasó megnyitásakor | Bezárás, saját QR használata; beállítások csak releváns esetben |
| Profilkép/logó kiválasztása | Rendszer képválasztó használatakor | Vissza a szerkesztőbe, korábbi kép megmarad |
| NFC használata | Helyi képességvizsgálat; szükség szerint rendszerbeállítás | Kontakt QR |
| Saját névjegy átadása | Nem indokol teljes címjegyzék-hozzáférést | Nincs ilyen kényszer |
| Új kontakt mentése | A tényleges rendszer-API által igényelt, minimális út | Mentés megszakítható; adatelőnézet megmarad |
| Értesítés, ha később készül | Konkrét hasznos funkció bekapcsolásakor | Az alap névjegy továbbra is használható |

A modern Android Photo Picker célja a kiválasztott médiához való hozzáférés, nem a teljes képtár indokolatlan bekérése. A pontos elérhetőség és visszaesési út OS-verziófüggő. [I; S08]

### 24.2. Biztonsági megjelenítési szabályok

A hitelesítési token, jelszó, privát API-kulcs és teljes névjegy-payload nem szerepel diagnosztikai panelen vagy prototípusadatban. A hibakód lehet másolható technikai azonosító, de a fő magyarázat magyar és feladatközpontú.

Külső URL megnyitása tudatos művelet. A névjegyfelületen megadott webcím nem futtathat `javascript:` vagy más aktív, nem engedélyezett sémát. A telefon/e-mail művelet saját típushoz kötött és ellenőrzött. A beolvasott tartalom nem utasítás az alkalmazás vagy egy AI számára.

Az operációs rendszer engedélyablakait nem rajzoljuk át megtévesztően. A saját magyarázó képernyő egyértelműen VIZIT-felület, a tényleges kérés rendszerfelület.

A megosztásra szánt QR-ról készíthető képernyőkép lehet szándékos funkció; nem terjesztünk ki rá általános screenshot-tiltást. Ettől a jelszó- és fiókbiztonsági felületek kezelése külön döntés marad.

<a id="25"></a>
## 25. Mikroszövegek és lokalizáció

### 25.1. Egységes fogalmak

A termék **névjegy**, a telefon címtárába mentett eredmény **kontakt**. A menü **Átadás**; a művelet megosztási mód szerint **Kontakt QR**, **Profil QR**, **NFC** vagy **Megosztás másképp**.

A fiók és névjegy elkülönül: fiókkal lépünk be, névjegyet szerkesztünk. Az „online”, „publikus”, „megosztható” és „szinkronban” nem egymás szinonimái.

### 25.2. Jóváhagyásra javasolt szövegkészlet

| Helyzet | Szöveg |
|---|---|
| Nincs használható elérhetőség | „A megosztáshoz adj meg legalább egy telefonszámot vagy e-mail-címet, és tedd megoszthatóvá.” |
| Helyi mentés | „A módosításokat ezen a készüléken elmentettük.” |
| Offline szinkron | „A felhőben még a korábbi változat lehet látható. A módosítások internetkapcsolatnál szinkronizálódnak.” |
| Publikáció visszavonásra vár | „A kikapcsolás még nem jutott el a szerverhez. A korábbi online névjegy egyelőre elérhető lehet.” |
| Kép nem része a Kontakt QR-nak | „Ez a QR a szöveges névjegyadataidat tartalmazza. A képes online névjegyhez használj profil QR-t.” |
| NFC aktív | „Az NFC-átadás aktív. Tartsd a telefonokat a készülékek NFC-érzékelőjénél egymáshoz közel.” |
| NFC nem igazolt | „Nem tudjuk megerősíteni, hogy minden adat átkerült. Próbáld újra, vagy válassz QR-kódot.” |
| Hiányzó iOS NFC-képesség | „Ebben a VIZIT-verzióban ezen a készüléken QR-rel vagy rendszermegosztással adhatod át a névjegyet.” |
| Publikus profil kikapcsolt | „A profil QR-hoz kapcsold be a publikus névjegyet. A Kontakt QR ettől még használható.” |
| Kameraengedély nincs | „A beolvasáshoz kamera-hozzáférés szükséges. A saját QR-kódod megjelenítéséhez nem.” |
| Megszakított rendszermentés | „A névjegymentést nem fejezted be.” — csak ha a megszakítás ténylegesen ismert |
| Ismeretlen mentési eredmény | „A mentést a Kontaktok alkalmazásban fejezheted be.” |
| Általános, javítható hiba | „Most nem sikerült elmenteni. A módosításaid megmaradtak. Próbáld újra.” — csak ha valóban megmaradtak |
| Törlés | „Fiók végleges törlése” — nem „Tovább” vagy „OK” |

Nem használunk sértő, hibáztató szöveget, hamis sikerjelzést vagy bizonytalan műveletre túlságosan határozott kijelentést.

### 25.3. Lokalizációs követelmények

A magyar feliratok külön erőforrásként kezelhetők a későbbi implementációban. Hosszabb fordításra legalább a mintafeliratok 30–40%-kal hosszabb változatával is készül tervteszt; ez munkamódszer, nem nyelvészeti állandó.

Dátum és idő a felhasználói környezethez igazodik. Az „utolsó mentés” ne csak „tegnap” legyen, amikor ütközésről kell dönteni: pontos időpont is megtekinthető. Teljes nemzetközi nevek, magyar hosszú ékezetek, emoji és jobbról balra írt nevek tesztadatként szerepeljenek.

<a id="26"></a>
## 26. Akadálymentességi követelmény–ellenőrzés mátrix

### 26.1. Alkalmazási kör

A webes cél WCAG 2.2 A/AA. A natív felületeknél WCAG2ICT szerinti értelmezés, valamint Android- és Apple-irányelvek együtt használhatók. A táblázat a VIZIT szempontjából kiemelt ellenőrzések leképezése, **nem a teljes A/AA szabvány helyettesítő listája**. Minden további alkalmazandó követelmény külön auditlandó. [S03–S04]

| Követelmény / referencia | VIZIT-alkalmazás | Ellenőrzés |
|---|---|---|
| 1.1.1 Nem szöveges tartalom | Portré és logó értelmes névvel; díszítőikon kihagyható; QR szöveges alternatívája | Képernyőolvasóval azonosítható név és megosztott adatok |
| 1.3.1 / 1.3.2 Információ és sorrend | Csoportcímek, mezőkapcsolatok, azonos vizuális és értelmes olvasási sorrend | Szemantikai fa és lineáris olvasás |
| 1.3.4 Tájolás | Nincs indokolatlan portrékényszer | Fekvő használat és billentyűzet |
| 1.4.1 Színhasználat | Láthatóság, hiba és szinkron szöveggel is megkülönböztethető | Szín nélküli állapotértelmezés |
| 1.4.3 Szövegkontraszt [A07] | A 19. fejezet párosításai | Normál szöveg legalább 4,5:1; minden állapotban mérve |
| 1.4.4 Szövegnagyítás [A09] | Weben 200%-os nagyításkor nincs tartalomvesztés | Valós böngészőteszt; nem csak a Figma zoomja |
| 1.4.10 Újratördelés [A05] | 320 CSS px szélességnél használható webes névjegy | Nem szükséges kétirányú görgetés a szöveg olvasásához |
| 1.4.11 Nem szöveges kontraszt [S03] | Lényeges ikon/mezőhatár és állapotkülönbség megfelelő kontraszttal | Alkalmazandó részeknél 3:1 ellenőrzés |
| 1.4.12 Szövegtérköz [A10] | Saját térköz-beállítás miatt nem vész el gomb és felirat | Sormagasság 1,5; bekezdésköz 2; betűköz 0,12; szóköz 0,16 betűméretarányos teszt |
| 2.1.1 / 2.1.2 Billentyűzet | A publikus web minden funkciója billentyűzettel elérhető; nincs csapda | Tab, Shift+Tab, Enter, Space és szükség szerinti Escape |
| 2.2.1 Időkorlát [A04] | NFC-időablak előre állítható; auth-időkorlát külön vizsgálva | A felhasználó nem néhány másodperces reakcióra kényszerül |
| 2.3.1 Villogás [S03] | Nincs villogó megosztási effekt | Mozgás és villogás vizsgálata |
| 2.4.3 / 2.4.7 Fókusz | Értelmes sorrend és látható jelzés | Billentyűzetes és natív accessibility-fókusz |
| 2.4.11 Fókusz nem takart [A06] | Sticky gomb, billentyűzet és modal nem takarja teljesen | Fókuszált vezérlő megjelenése minden elrendezésben |
| 2.5.3 Látható címke és név | A felolvasott név tartalmazza a látható gombfeliratot | Hangvezérléses kiválasztás |
| 2.5.7 Húzási alternatíva [A03] | Sorrendnél Feljebb/Lejjebb; képnél gombos beállítás | Húzás nélkül elvégezhető alapfeladat |
| 2.5.8 Célméret [A01] | Webes AA-minimum, a VIZIT fő céljai ennél nagyobbak | Vezérlőméret és egymáshoz közeli célok |
| 3.2.3 / 3.2.4 Következetesség | Azonos elnevezések, navigáció és műveleti szerepek | Képernyők közötti összevetés |
| 3.2.6 Következetes segítség [S03] | Ismétlődő súgóelérés azonos viszonylagos helyen | Webes oldalkészlet ellenőrzése |
| 3.3.1–3.3.3 Hibakezelés [A11] | Mezőnél konkrét hiba és értelmes javítási út | Üres, hibás és szerver által elutasított adat |
| 3.3.4 Hibamegelőzés [S03] | Végleges törlés következménye, ellenőrzése, megerősítése | Téves aktiválásból nincs azonnali adatvesztés |
| 3.3.7 Ismételt adatbevitel [S03] | A már megadott onboardingadat nem kérendő újra indokolatlanul | Teljes regisztráció–onboarding végigjárása |
| 3.3.8 Hozzáférhető hitelesítés [A02] | Jelszókezelő és beillesztés; nincs kizárólag memóriafeladat | Jelszókitöltés és teljes kód beillesztése |
| 4.1.2 Név, szerep, érték | Kapcsolók, tabok, mezők megfelelő szemantikával | Accessibility tree és olvasóteszt |
| 4.1.3 Állapotüzenetek [A08] | Mentés/szinkron/hiba fókuszmozgatás nélkül is érzékelhető | Élő bejelentés, ismétlési zaj nélkül |
| Natív nagy szöveg [S06, S11] | Android font scaling; iOS accessibility Dynamic Type | Hosszú név, mezőhiba, CTA és QR együttes használata |
| Csökkentett mozgás [T] | Statikus NFC-állapot és csökkentett átmenetek | Rendszerbeállítás mellett a feladat változatlanul elvégezhető |

### 26.2. Nem alkalmazandó és később újranyitandó területek

Az alapkonceptus nem tartalmaz időzített videót, hangos oktatóanyagot vagy élő kommunikációt. Ezekhez a média-hozzáférhetőségi követelményeket nem „teljesítettnek”, hanem a konkrét tartalom hiányában vizsgálandó/nem alkalmazandó állapotúnak kell jelölni. Ilyen tartalom hozzáadásakor a megfelelő felirat-, átirat- és vezérlési követelmények újranyílnak.

A Figma-prototípus nem bizonyítja a programozott szemantikát, billentyűzetkezelést, TalkBack/VoiceOver-viselkedést vagy a szerveroldali adatvédelmet. Ezekhez működő implementáció kell.

<a id="27"></a>
## 27. Minőségbiztosítás és elfogadási forgatókönyvek

### 27.1. Három külön minőségi állapot

**TERV KIDOLGOZVA:** minden képernyő és fontos állapot leírt, a döntések és hivatkozások követhetők.  
**PROTOTÍPUS ELLENŐRIZVE:** a kattintható tervben az útvonalak, hosszú tartalmak, témák és elrendezések vizsgálhatók.  
**IMPLEMENTÁCIÓ IGAZOLVA:** valós builden, megfelelő eszközzel és dokumentált eredménnyel ellenőrizték.

Ezek nem cserélhetők fel. Jelen dokumentum nem emeli a terméket a harmadik állapotba.

### 27.2. Elfogadási forgatókönyvek

| Teszt | Kiindulás és művelet | Elvárt eredmény |
|---|---|---|
| UI-01 | Új felhasználó regisztrál | Nem kerül megerősítés nélkül kész fiókként a főoldalra |
| UI-02 | Lejárt e-mail-linket nyit | Érthető állapot és újraküldési út; nincs üres oldal |
| UI-03 | Jelszókezelővel és beillesztéssel lép be | A teljes folyamat használható tiltott paste nélkül |
| UI-04 | Külső belépést megszakít | Visszatér a belépési oldalra, adatszivárgás és hamis hiba nélkül |
| UI-05 | Még nincs működő Google-konfiguráció | Nincs aktív, működést ígérő provider-gomb |
| UI-06 | Első névjegyen csak privát telefon szerepel | Menthető piszkozat, de kontaktátadás nem indul teljes értékűként |
| UI-07 | Hosszú ékezetes és nem latin név | Nincs indokolatlan levágás vagy adatvesztés |
| UI-08 | Széles transzparens céges logót választ | A teljes grafika látszik; nem portréként vágódik |
| UI-09 | Profilkép feltöltése hibás | Helyi előnézet és hiba külön; más adatok használhatók |
| UI-10 | Privát mezőt ad meg | Nem szerepel semmilyen kimenetben vagy publikus metaadatban |
| UI-11 | Offline módosítja a saját adatait | Helyileg megmarad, szinkronra váró állapot látszik |
| UI-12 | Offline kikapcsolja a publikus profilt | Nem állít azonnali szerveroldali visszavonást |
| UI-13 | Két eszköz ütköző adatot ment | Nincs csendes felülírás; döntési út elérhető |
| UI-14 | Kontakt QR-t mutat internet nélkül | A helyi adatokból előáll; nem kér kamerát |
| UI-15 | Publikus profil nélkül profil QR-t kér | Érthető előfeltétel; nem generál működést ígérő rossz linket |
| UI-16 | Nagy adatú QR-t készít | Nincs csendes mezővesztés; olvashatósági alternatíva |
| UI-17 | Teljes képernyős QR után kilép vagy háttérbe vált | Korábbi fényerő visszaáll |
| UI-18 | NFC nincs bekapcsolva | Rendszerbeállítás vagy Kontakt QR; nem hamis engedélyablak |
| UI-19 | NFC aktív, majd zárolás/navigáció történik | Olvashatóság megszűnik, nem folytatódik titokban |
| UI-20 | NFC csak részben olvasható ki | Nincs sikeres kontaktmentést állító üzenet |
| UI-21 | NFC teljesen kiolvasott | Műszaki eredmény, nem bizonyítatlan kontaktmentés |
| UI-22 | Fogadó kamera nem ismeri fel a kontakt QR-t | Másik ténylegesen elérhető csatorna, tisztázott következménnyel |
| UI-23 | Beolvasott QR veszélyes vagy ismeretlen sémát tartalmaz | Nincs automatikus megnyitás vagy kódfuttatás |
| UI-24 | Rendszer Kontaktokban Mégse | Nincs hamis „Kontakt mentve” állapot |
| UI-25 | Webes profil 320 CSS px és 200% szövegnagyítás | Olvasható tartalom és hozzáférhető műveletek |
| UI-26 | Billentyűzettel végigjárja a webes modalt | Értelmes fókusz, nincs csapda, fókusz visszatér |
| UI-27 | Maximális támogatott natív betűméret | A lényeges adat és művelet nem tűnik el |
| UI-28 | TalkBack/VoiceOver használata | Név, érték, kiválasztás, hiba és státusz érthető |
| UI-29 | Csökkentett mozgás | Nincs kötelező pulzáló/zoomoló út a megosztáshoz |
| UI-30 | Törlést offline kezdeményez | Nem kap végleges törlési sikert |
| UI-31 | Kijelentkezik függő szinkronnal | Következmények láthatók; idegen fiók nem fér hozzá a helyi adathoz |
| UI-32 | DEV/BETA és PROD környezet összevetése | Tesztkörnyezet azonosítható; productionban nincs titok vagy debugadat |

### 27.3. Eszköz- és környezetmátrix

A készüléklista tesztterv, nem támogatottsági állítás. Androidon Xiaomi/Redmi/POCO, Samsung és Pixel családokból tényleges modellek; külön Android- és HyperOS-verziók; gesztusos és eltérő navigációs környezetek. NFC-nél küldő és fogadó modellje egyaránt rögzítendő. [K; R1, 48–50., 103–105. fejezet]

iOS-en támogatott iPhone-méretek, iPad multitasking, rendszerkamera, Safari, rendszer Kontaktok és rendszermegosztás. A szimulátor nem bizonyít NFC-t, kamerás QR-interoperabilitást vagy fizikai átadást. [R3]

Weben tényleges Android Chrome és iOS Safari, valamint asztali billentyűzetes böngészőteszt. A verziókat a teszt napján kell rögzíteni, nem „legújabb” szóval helyettesíteni.

**Egy bizonyítékrekord:** tesztazonosító; build/commit; küldő és fogadó eszköz; OS és kamera/Contacts/böngésző verzió; dátum; előfeltétel; lépések; megfigyelt eredmény; hibajegy vagy személyes adatot nem tartalmazó bizonyíték.

### 27.4. Használhatósági értékelés

Külön feladatteszt: első névjegy létrehozása, adat elrejtése, QR átadás, nem működő NFC helyett alternatíva, online publikáció visszavonása. Mérendő a sikeres befejezés, téves adatmegosztás, segítségigény és a felhasználó eredményértelmezése.

Javasolt belső cél: a kritikus tesztforgatókönyvekben nulla észrevétlen privátadat-megosztás; minden zsákutcánál legalább egy értelmes kilépési/javítási út. A dokumentum nem talál ki kutatási mintát vagy már elért sikerarányt. Az ISO-hivatkozás nem pótolja a felhasználói kipróbálást. [T; S01]

<a id="28"></a>
## 28. Figma-tervezési és prototípus-átadási szerkezet

### 28.1. Javasolt Figma-oldalak

| Oldal | Tartalom |
|---|---|
| `00 · Olvasd el` | Hatókör, jelölések, forráspillanatkép, nincs implementációs felhatalmazás |
| `01 · Alapok` | Márka, színek, tipográfia, térköz, geometria, mozgás |
| `02 · Komponensek` | Alapvezérlők és VIZIT-tartalmi komponensek |
| `03 · Minták és állapotok` | Űrlap, hiba, offline, konfliktus, rendszernek átadás |
| `04 · Android` | A képernyőjegyzék Android-megvalósítási terve |
| `05 · iOS és iPadOS` | Natív adaptáció és eltérő rendszerfolyamatok |
| `06 · Publikus web` | Névjegy, unavailable, mentési út és adaptív állapotok |
| `07 · Folyamatok` | Regisztráció, szerkesztés, QR, NFC, fogadás, törlés prototípusa |
| `08 · Tesztesetek` | Hosszú adatok, nagy szöveg, sötét mód, hibák, ütközések |
| `09 · Átadási megjegyzések` | Döntések, rendszerhatárok, ellenőrzési feltételek |
| `10 · Külön bővítések` | Kizárólag B státuszú jövőbeli koncepciók |

Ez csak a későbbi tervfájl javasolt szerkezete. E dokumentum elkészítése nem hoz létre Figma-fájlt, nem kapcsol könyvtárat a repóhoz és nem publikál design systemet.

### 28.2. Szerkeszthetőség

A későbbi Figma-tervben a valóban összetartozó sorok és oszlopok Auto Layoutot kapnak; a tartalom nem kizárólag rögzített képernyőkoordinátákból áll. A névjegy neve, gombszöveg, állapot és hosszú hiba valódi szöveg, nem a képernyőbe lapított kép.

A világos/sötét szemantikus módok egyértelműek. A platformeltérések nem rejtett override-okból állnak. A komponens neve és leírása megmutatja a szerepét, az állapotát és a tartalmi korlátját.

Egy képernyő neve: `PRO-04 / Android / Telefon szerkesztése / Hibás adat`. A méretvariáns a konkrét tesztkeretet jelöli, nem azt, hogy kizárólag ilyen telefonon működhet.

### 28.3. Prototípus-folyamatok

Legalább a következő teljes utak legyenek kattinthatók:

1. Regisztráció → megerősítés → első névjegy → Kontakt QR.
2. Belépés → elérhetőség hozzáadása → elrejtése → címzetti előnézet.
3. Kép/logó szerkesztése → feltöltési hiba → helyi mentés → újrapróbálás.
4. NFC indítás → teljes/hiányos olvasás → leállítás vagy QR-alternatíva.
5. Profil publikálása → profil QR → publikus web → rendszeroldali kontaktátadás.
6. Offline módosítás → online ütközés → döntés → visszaigazolt állapot.
7. QR-beolvasás → előnézet → rendszer Kontaktok → megszakítás/siker bizonyíték szerint.
8. Fióktörlés → újrahitelesítés → következmények → visszaigazolt eredmény.

Az OS-felületeket megjelölt rendszerhatár reprezentálja, nem saját termékkomponensként tett ígéret. Minden siker, hiba és küldés fiktív prototípusállapot; a terv nem csatlakozik valódi fiókhoz, kontaktlistához, Supabase-hez vagy e-mail-küldéshez.

### 28.4. Átadási minimum

A későbbi tervezési átadás akkor teljes, ha a képernyőjegyzék minden alkalmazandó sora lefedett; az összes core út végigjárható; van világos/sötét és nagy szöveges példa; a rendszerhatárok megjelöltek; a privacy- és offlineállapotok nem hiányoznak; a komponensek nem lapított képek; a bizonyítékot igénylő képességek nincsenek kész funkcióként megjelölve.

<a id="29"></a>
## 29. Bővítési helyek, jelenlegi hatókörön kívül

A következő elemek **B** státuszúak. Nem jelennek meg működő főmenüként, üres upsell-kártyaként vagy kötelező fejlesztési feladatként az alapélményben.

| Bővítés | Javasolt felületi hely és folyamat | Kötelező külön döntés/bizonyítás |
|---|---|---|
| Több saját névjegy | Névjegyem fejlécében választó → Létrehozás/Másolat/Archiválás; megosztás előtt aktív névjegy neve | Jogosultság, adatmodell, csatornánkénti azonosítás; rossz profil véletlen megosztásának megelőzése |
| Fizikai NFC-kártya | Átadás alatti Eszközök → Kártya társítása → Tartalom előnézete → Írás → Visszaolvasás | Írható tag, tulajdonosi hozzárendelés, mobil képesség és tényleges teszt |
| Kártya elvesztése | Eszközrészletek → Linkcél letiltása vagy leválasztása → Következmények | Egy már ráírt statikus kontaktadat nem törölhető távolról; a linktiltás nem a fizikai tag átírása |
| Wallet-névjegy | Átadás → Hozzáadás a támogatott Wallethez → Rendszerfolyamat | Konkrét Wallet-formátum, jogosultság és kiadási feltétel; QR-pass nem NFC-telefonemuláció |
| Teljes webes tulajdonosi portál | Ugyanazon fogalmak szerkesztője, sidebar + előnézet | Külön hitelesítés, session- és adatvédelmi felülvizsgálat; nem bővíti automatikusan a publikus oldalt |
| Adatexport | Beállítások → Saját adatok → Export kérése → Állapot → Biztonságos átvétel | Formátum, adatkör, jogosultság, lejárat és tényleges backendtámogatás |
| Eszköz-munkamenetek | Fiók → Bejelentkezett eszközök → Egy munkamenet visszavonása | Valós szerveroldali lista és visszavonhatóság |
| Statisztika | Névjegyem alatti külön nézet, csak igazolt eseményekkel | QR-kijelzés nem beolvasás; NFC-olvasás nem mentés; nincs indokolatlan címzettazonosítás |
| Értesítések | Beállítások → Hasznos értesítéstípusok, kontextuális engedély | Valódi eseményforrás és a felhasználó által szabályozott kézbesítés |

Ezek tervezési elhelyezések, nem a platformok jelenleg elérhető összes API-jának ellenőrzött implementációs tervei. Különösen a Wallet és NFC-kártya bővítés nem aktiválható pusztán egy gomb megrajzolásával.

<a id="30"></a>
## 30. Döntési napló és nyitott bizonyítási kérdések

### 30.1. A koncepcióban rögzített döntések

| Döntés | Indok |
|---|---|
| Egyetlen független Markdown, külön ág | Nem kapcsolódik az alkalmazás futásához vagy aktív fejlesztéséhez |
| Meglévő VIZIT-márka megtartása | Az app újratervezése nem önkényes márkacsere |
| Négy fő célterület | Követi a termékfeladatokat és a master javaslatát |
| Kontakt QR elsődleges QR-mód | A projekt közvetlen kontaktátadási célját őrzi |
| NFC képesség és eszközteszt alapján | Elkerüli a korábbi, univerzális működést ígérő UX-et |
| Teljes iOS-core koncepció, natív eltérésekkel | A későbbi iOS-forrás jelenlétét figyelembe veszi, a régi Android-fókuszt nem tagadja |
| Publikus profil alapból nem automatikus | Tudatos adatközzététel |
| Helyi mentés, szinkron és publikáció külön állapot | Nem téveszti meg a felhasználót online adatainak állapotáról |
| Két témára külön szemantikus színek | A brandkék és cián nem minden háttéren használható szövegként |
| Rendszerfelületek natívak | Nem ígér saját UI-val nem létező platformképességet |
| Bővítések külön jelölve | Nem növeli hallgatólagosan a release-hatókört |

### 30.2. A tényleges megvalósítás előtt bizonyítandó

A végleges API-mezőkorlátok; a kiadási környezet tényleges auth-providerjei és jelszópolitikája; a publikus profil végleges domainje; a névjegykimenetek valós tartalma; az NFC-eszközpárok; a kamera QR-formátumfelismerése; a médiahozzáférés és publikáció-visszavonás működése; a rendszerkontakt-mentés visszajelzésének megbízhatósága; a jogi szövegek és alkalmazandó követelmények.

E kérdések hiánya nem indok egy hiányos felületi tervre: mindegyikhez szerepel előfeltételes, hibás vagy alternatív állapot. Ugyanakkor a terv nem tölti ki őket kitalált működési garanciával.

### 30.3. Nem megengedett következtetések

A dokumentum megléte nem jelenti, hogy a VIZIT teljesen WCAG-/EN-/ISO-megfelelő, productionkész, minden telefonon NFC-kompatibilis, App Store-jóváhagyott vagy automatikusan újratervezendő. A formális megfelelőség és a tényleges működés külön bizonyítást igényel.

<a id="31"></a>
## 31. Figma Make számára átadható tervezési brief

Az alábbi blokk későbbi, kézi használatra készült. **Nem indít el és nem konfigurál semmilyen Figma- vagy fejlesztési folyamatot.** A teljes dokumentum a részletes háttér, a brief nem helyettesíti a képernyőjegyzéket.

```text
Készíts szerkeszthető, fiktív adatokkal működő VIZIT UI-koncepciót
és kattintható felületi prototípust. Ez tervezési feladat, nem éles
alkalmazásfejlesztés. Ne kapcsolj backendet, hitelesítést, adatbázist,
e-mail-küldést, GitHub-szinkront vagy valós kontaktlistát.

A termék digitális névjegy- és kontaktmegosztó app.
Fő navigáció: Kezdőlap, Névjegyem, Átadás, Beállítások.
Fő feladat: saját névjegy szerkesztése és tudatos átadása Kontakt QR-rel,
igazolt Android-környezetben NFC-vel, illetve publikus profil-linkkel.
A fogadónak ne kelljen VIZIT-fiók vagy alkalmazástelepítés.

Tartsd meg a hivatalos VIZIT-logót és a Navy #061B46, Blue #055EEC,
Cyan #13D1FC, Ice #EAF8FF márkapalettát. A cián fehér háttéren
ne legyen normál szöveg. A teljes sötét feliratú logó kontrollált
fehér felületet kapjon. A VIZIT-logó, portré és céges logó külön fogalom.
A céges logót ne vágd le.

Kövesd a dokumentum képernyőazonosítóit, állapotait és tokenjavaslatait.
Készíts Android-, natív jellegű iOS/iPadOS- és mobil webes fogadóváltozatot.
Használj újrahasznosítható komponenseket, rugalmas elrendezést,
világos és sötét módot, nagy szöveggel is működő elrendezéseket.

Dolgozd ki a teljes regisztráció/megerősítés/visszaállítás folyamatot,
a profil- és képszerkesztést, mező-láthatóságot, offline/szinkronhibát,
ütközést, publikus profil állapotait, QR/NFC megosztást, fogadást,
engedélyelutasítást és fióktörlést. Ne csak sikeres demóoldalakat készíts.

A Kontakt QR és profil QR külön mód. A profil QR internetes oldalt nyit.
iOS-en ne mutass működőként Android HCE-szerű NFC-névjegyemulációt.
NFC aktiválás, teljes adatkiolvasás és címzetti kontaktmentés külön állapot.
Az offline publikáció-visszavonás ne jelenjen meg szerveroldalon késznek.

A rendszer fotóválasztója, engedélykérése, belépési ablaka,
megosztója és Kontaktok-szerkesztője jelölt rendszerhatár.
A prototípusban minden adat és művelet szimulált, ez legyen egyértelmű.
Ne állíts formális WCAG/ISO/EN megfelelőséget egy vizuális terv alapján.

A 29. fejezet bővítéseit ne építsd be automatikusan az alapfelületbe.
Ne adj hozzá közösségi hálót, CRM-et, csomagvásárlást vagy statisztikai
sikermutatókat külön döntés nélkül. A stílus legyen professzionális,
személyes és nyugodt, állandó glow, lebegés és túlzott dekoráció nélkül.
```

<a id="32"></a>
## 32. Forrásjegyzék

**Ellenőrzés időpontja:** 2026. szeptember 18. A hivatkozások olvasási források, nem függőségek. A platformoldalak később változhatnak; új implementációs munka előtt újraellenőrizendők.

### Projektforrások — rögzített commitok

- **[R2]** [VIZIT branding](https://github.com/RayWorksHub/vizitkartya-app/blob/91243c6a26f9add6a6ce9e578528f777b9b1e210/docs/BRANDING.md).
- **[R3]** [VIZIT iOS DEV — README](https://github.com/RayWorksHub/vizitkartya-app/blob/91243c6a26f9add6a6ce9e578528f777b9b1e210/ios/README.md).
- **[R4]** [A tárolási ág alapjául szolgáló Android workflow](https://github.com/RayWorksHub/vizitkartya-app/blob/b8a4e6d359268237e8195b56a4196f5055c4f45c/.github/workflows/android.yml).
- **[R5]** [VIZIT Android v2 — README](https://github.com/RayWorksHub/vizitkartya-app/blob/91243c6a26f9add6a6ce9e578528f777b9b1e210/README.md).

### Hivatalos szabványok és platformforrások

- **[S01]** ISO: [ISO 9241-210:2019 — Human-centred design for interactive systems](https://www.iso.org/standard/77520.html). Katalógus és összefoglaló; nem teljes klauzulaaudit.
- **[S02]** ISO: [ISO 9241-110:2020 — Interaction principles](https://www.iso.org/standard/75258.html). Katalógus és összefoglaló.
- **[S03]** W3C: [Web Content Accessibility Guidelines 2.2](https://www.w3.org/TR/WCAG22/). Normatív webes referencia.
- **[S04]** W3C: [Guidance on Applying WCAG 2 to Non-Web ICT — 2025-12-11 Group Note](https://www.w3.org/TR/2025/NOTE-wcag2ict-22-20251211/). Tájékoztató, nem önálló normatív követelményrendszer.
- **[S05]** European Commission, AccessibleEU: [The European accessibility standard EN 301 549 has been updated — 2026-09-07](https://accessible-eu-centre.ec.europa.eu/content-corner/news/european-accessibility-standard-en-301-549-has-been-updated-2026-09-07_en). A V4.1.1 közzétételének és a jogi hivatkozástól való elkülönítésének forrása; nem teljes szabványszöveg-audit.
- **[S06]** Android Developers: [Make apps more accessible](https://developer.android.com/guide/topics/ui/accessibility/apps).
- **[S07]** Android Developers: [Use window size classes](https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes).
- **[S08]** Android Developers: [Photo picker](https://developer.android.com/training/data-storage/shared/photo-picker).
- **[S09]** Apple Developer: [UI Design Dos and Don’ts](https://developer.apple.com/design/tips/).
- **[S10]** Apple Human Interface Guidelines: [Tab bars](https://developer.apple.com/design/human-interface-guidelines/tab-bars), az elérhető [hivatalos lokalizált leírással](https://developer.apple.com/cn/design/human-interface-guidelines/tab-bars).
- **[S11]** Apple: [DynamicTypeSize](https://developer.apple.com/documentation/SwiftUI/DynamicTypeSize), valamint [hivatalos tipográfiai útmutató](https://developer.apple.com/jp/design/human-interface-guidelines/typography).
- **[S12]** Apple: [App Review Guidelines](https://developer.apple.com/app-store/review/guidelines/), különösen 4.8 és 5.1.1(v). Kiadási szabályzat, nem általános UI-szabvány.
- **[S13]** W3C WAI: [ARIA APG — Dialog (Modal) Pattern](https://www.w3.org/WAI/ARIA/apg/patterns/dialog-modal/).
- **[S14]** NIST: [SP 800-63B-4 — Authenticators](https://pages.nist.gov/800-63-4/sp800-63b/authenticators/), Passwords fejezet.
- **[S15]** RFC Editor: [RFC 6350 — vCard Format Specification és előzményei](https://www.rfc-editor.org/info/rfc6350/). Az RFC 2426 leváltását a hivatalos rekord jelzi; a projekt 3.0-s választása külön projektkövetelmény.
- **[S16]** DENSO WAVE: [QR Code — Points to consider when creating a code](https://www.qrcode.com/en/howto/code.html).
- **[S17]** Android Developers: [Host-based card emulation overview](https://developer.android.com/develop/connectivity/nfc/hce).
- **[S18]** Apple Developer: [Core NFC](https://developer.apple.com/documentation/CoreNFC).

### Célzott WCAG-magyarázó források

Az alábbi Understanding-oldalak hivatalos **magyarázó útmutatók**; a követelmények normatív szövege az S03 alatt található.

- **[A01]** [2.5.8 Target Size (Minimum)](https://www.w3.org/WAI/WCAG22/Understanding/target-size-minimum.html).
- **[A02]** [3.3.8 Accessible Authentication (Minimum)](https://www.w3.org/WAI/WCAG22/Understanding/accessible-authentication-minimum.html).
- **[A03]** [2.5.7 Dragging Movements](https://www.w3.org/WAI/WCAG22/Understanding/dragging-movements.html).
- **[A04]** [2.2.1 Timing Adjustable](https://www.w3.org/WAI/WCAG22/Understanding/timing-adjustable.html).
- **[A05]** [1.4.10 Reflow](https://www.w3.org/WAI/WCAG22/Understanding/reflow.html).
- **[A06]** [2.4.11 Focus Not Obscured (Minimum)](https://www.w3.org/WAI/WCAG22/Understanding/focus-not-obscured-minimum.html).
- **[A07]** [1.4.3 Contrast (Minimum)](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html).
- **[A08]** [4.1.3 Status Messages](https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html).
- **[A09]** [1.4.4 Resize Text](https://www.w3.org/WAI/WCAG22/Understanding/resize-text.html).
- **[A10]** [1.4.12 Text Spacing](https://www.w3.org/WAI/WCAG22/Understanding/text-spacing.html).
- **[A11]** [3.3.1 Error Identification](https://www.w3.org/WAI/WCAG22/Understanding/error-identification.html).

---

**Dokumentumzárás:** a VIZIT teljes alapfelületi koncepciója és a bővítések helye ebben az egy dokumentumban található. A tárolás nem jelent automatikus implementációt, importot, szinkront, függőséget vagy fejlesztési engedélyt.
