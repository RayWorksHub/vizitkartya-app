# VIZIT – a jóváhagyott UI-terv végrehajtási állapota

Állapot: FOLYAMATBAN. Ez nem teljesítési igazolás és nem 100%-os készültségi jelentés.

Fejlesztési ág: `feature/figma-ui-complete-20260921`.
Forrás: a felhasználó által jóváhagyott, 24 oldalas `VIZIT_Figma_teljes_UI_leltar_lekodolt_2026-09-21(1).pdf` és az ahhoz jóváhagyott teljes fejlesztési terv.
PDF SHA-256: `d7ad022e08f04a02c8c0ab369b68ba0ebcf70e1ce8534901cefa70eba1c22a68`.
A mobilos követelmények: 01–19, 40, 50 és 60. A webes 30–32 a PDF szerint külön projekt, nem az iOS kiadás része.

## Ténylegesen a fejlesztési ágba került változások

### 1. Komponensek és valódi események összekötése

Commit: `51c0e1a48493a8fcdba1ecf2cfa4a1e019680f7a`.
Fordítási futás: `35604750212` – sikeres iOS Release build, aláírás nélkül.

- A mentési Toast és a Visszavonás Snackbar tényleges mentéshez kapcsolódik; a visszavonás ellenőrzi az aktív fiókot és a közben megváltozott adatokat.
- A kurzuskereső és kategóriaszűrő ténylegesen szűri a meglévő kurzusokat és leckéket; az üres találati lista a keresés törléséhez vezet.
- A ProgressCard a helyben rögzített lecketeljesítésekből számol, nem mintaszámot mutat.
- A kamerához külön engedélykérő és megtagadott engedély állapot tartozik, rendszerbeállítási és képtáras alternatívával.
- A lejárt munkamenet az auth eseményeihez és a munkamenet-hibákhoz kapcsolódik.
- A Loading és Skeleton állapotok tényleges indításkor, hiányzó profil letöltésekor és QR-kép feldolgozásakor jelennek meg.
- Az automatikus szinkron kapcsolója vezérli a mentés utáni feltöltést és az automatikus újrapróbálkozást.
- A fióktörlés megerősítő komponense valódi törlési művelethez kapcsolódik, a megerősítő kifejezés ellenőrzésének megtartásával.
- A mezők, kapcsolók, sorok és visszajelzések hozzáférhetőségi és szövegtördelési módosításokat kaptak.

### 2. Navigáció, kártya és szerkesztés

Commit: `a320fa78409784c89384d6783bb308cede45960c`.
Fordítási futás: `35606054122` – sikeres iOS Release build, aláírás nélkül.

- A négy fő célhoz a PDF szerinti sík alsó navigáció készül, nem az operációs rendszer lebegő tabbarjára támaszkodik.
- A már megnyitott főképernyők állapota tabváltáskor megmarad; a rejtett képernyők nem interaktívak és nem szerepelnek a VoiceOver-fában.
- A céges logó és a szekciósorrend a profilszerkesztő piszkozatának része. Mentéskor véglegesülnek; a Mégse nem írja át a mentett megjelenést.
- A szekciók átrendezése megjelenik az élő előnézeten és a névjegy részletblokkjain. A sorrend mentése duplikált vagy hiányzó elemek ellen normalizált.
- A sorrendszerkesztő a nyilvános profil bekapcsolása nélkül is elérhető.
- A Névjegy képernyőről közvetlenül elérhető a kártya megjelenése és az adatláthatóság.
- A nagy hozzáférhetőségi szövegméretnél a kártya tartalma természetes magassággal jelenik meg.
- A rejtett céges adatokhoz a céges logó sem jelenik meg a kártyán.
- A Papír kártya másodlagos szövege sötét alkalmazástémában is sötét marad a világos kártyafelületen.
- A videólecke Újrapróbálás művelete újra létrehozza a lejátszót, nem csak egy címke.

## A teljes terv nyomon követése

A táblázat az érintett területeket követi; a megjelölt beépítés nem jelent futásidejű vagy pixelpontos vizuális igazolást.

| Terv | Jelenlegi állapot / még nyitott feladat |
|---|---|
| 01 Brand | Meglévő márkaassetek és komponensek megtartva; teljes képi megfelelés lezárása még szükséges. |
| 02 Foundations | Meglévő közös tokenek használatban; minden képernyő végső token-áttekintése még nyitott. |
| 03 Színek | Meglévő light/dark színek, állapottónusok és anyagok; Papír felület kontrasztja javítva. |
| 04 Tipográfia | Nagy címek a közös Inter stílusra kötve; gombok és sorok nagy szövegmérete javítva. |
| 05 Grid és spacing | Alsó navigáció és biztonságos tartalomterület átalakítva; teljes kijelzőméret-lefedettség nem igazolt. |
| 06 Ikonok | Meglévő funkcionális rendszerikonok megmaradtak; a Figma ikonrajzaival való végső összevetés nyitott. |
| 07 Komponensek | A korábban csak deklarált komponensekhez tényleges képernyőbeli felhasználások készültek az 1. csomagban. |
| 08 Komponensállapotok | Aktív, tiltott, betöltés-, hiba- és kiválasztott állapotok bekötése bővült; teljes állapotmátrix lezárása nyitott. |
| 09 Patterns | Toast, Snackbar, keresés, engedélykérés, megerősítés és inline visszajelzés valódi műveletekhez kötve. |
| 10 Navigáció | Sík 4 célú navigáció, főképernyőállapot-megőrzés és új testreszabási belépési pontok elkészültek. |
| 11 Screen Map | Fő képernyők és szerkesztők megvannak; a szinkronállapot részletes célképernyője még bővítendő. |
| 12 Authentication | Lejárt munkamenet bekötve; a meglévő belépés, regisztráció és reset megmaradt. Teljes auth flow lezárása még nyitott. |
| 13 Main App | Kezdőlapi skeleton, névjegyszekciók, valós kontaktműveletek és testreszabási útvonalak beépítve. |
| 14 Profile Customization | Élő előnézet, menthető logó- és sorrendpiszkozat, Mégse és sorrendfelhasználás beépítve. A teljes anyag/layout/sorrend kombinációs áttekintés nyitott. |
| 15 Sharing | Valódi kameraengedély- és képfeldolgozási állapot, mentési visszajelzések; a fotós QR készítése alatti állapot és a QR-hibák pontos elkülönítése még bővítendő. |
| 16 Settings | Automatikus szinkron valódi vezérlése és törlésmegerősítés beépítve. Részletes szinkronállapot, valódi időbélyegek és egymást követő fiók-sheetek kezelése még nyitott. |
| 17 Vállalkozói Portál | Valódi keresés, kategóriaszűrés, leckehaladás és videó-újrapróbálás beépítve. |
| 18 QR módok | A meglévő három mód, H hibajavítás, quiet zone és márkajel megmaradt; a túlméretes Kontakt QR pontos hiba-visszajelzése még nyitott. |
| 19 Saját domain | Meglévő mező és négy UI-állapot megtartva; szerveroldali domain-aktiválást ez a két csomag nem módosított. |
| 40 Empty/Loading/Error | Keresés, engedély, lejárt session, indítás, profilbetöltés és képbeolvasás állapota valódi kiváltó eseményhez kötve. További QR/szinkron állapotok még hátra vannak. |
| 50 Prototype Flows | Új keresési, engedélyezési, mentés/visszavonás és testreszabási útvonalak beépítve; az összes folyamat teljes lezárása még nyitott. |
| 60 Accessibility | A komponensek érintési céljai, feliratai, kiválasztott állapotai és nagy szövegmérete bővült. Teljes VoiceOver/Dynamic Type megfelelőséget nem állítunk. |

## Kiadási állapot

Ebben a fejlesztési menetben nem futott unit-, UI-, szimulátoros vagy fizikai készülékteszt. A fordítás a forrás buildelhetőségét ellenőrizte, nem a funkciók futásidejű helyességét.
Nem készült új aláírt IPA, és új TestFlight-feltöltés sem történt.
A teljes jóváhagyott terv végrehajtása továbbra is folyamatban van; a fenti két csomag nem a teljes fejlesztés lezárása.
