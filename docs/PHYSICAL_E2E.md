# VIZIT 0.4.3 fizikai E2E

Ez a jegyzőkönyv a `main` ág `25d4ee2` állapotához tartozik.

## Indulási feltételek

- iPhone A: TestFlight `VIZIT 0.4.3 (34)`.
- iPhone B: TestFlight `VIZIT 0.4.3 (34)`.
- Android C: a GitHub Actions `Android CI #44` futás `vizit-dev-debug` APK-ja.
- Mindhárom alkalmazás ugyanazt a DEV Supabase projektet használja.
- Egy elérhető, korábban VIZIT-regisztrációhoz nem használt e-mail-cím.
- Két könnyen megkülönböztethető tesztkép: `KÉP-A` és `KÉP-B`.
- Mindhárom készüléken legyen engedélyezve a kamera; a képválasztáskor csak a két tesztképhez adj hozzáférést.

Teszt előtt futtatható előellenőrzés:

```bash
node scripts/check-physical-e2e-readiness.mjs
```

Ha az iPhone-on nem a fenti verzió látszik a **Beállítások** képernyő alján, a tesztet nem szabad elkezdeni.

## Rögzítendő adatok

| Adat | Érték |
|---|---|
| Dátum és idő | |
| Tesztelő | |
| iPhone A modell / iOS | |
| iPhone B modell / iOS | |
| Android C modell / Android | |
| iOS verzió/build | `0.4.3 (34)` |
| Android verzió | `0.4.3-dev` |
| Regisztrációs e-mail | |

## Egységes tesztprofil

| Mező | Érték |
|---|---|
| Megjelenített név | `VIZIT E2E 034` |
| Vezetéknév | `Teszt` |
| Keresztnév | `VIZIT` |
| Telefonszám | `+36 30 555 0434` |
| Nyilvános e-mail | `vizit-e2e@example.com` |
| Weboldal | `https://e-nevjegy.vercel.app` |
| Cég | `RayWorks` |
| Beosztás | `E2E teszt` |
| Cím | `Budapest` |
| LinkedIn | `https://www.linkedin.com` |
| Nyilvános profil | bekapcsolva |
| Profilazonosító | hagyd üresen, az első szinkron generálja |

## 1. Regisztráció és első szinkron – iPhone A

1. Töröld a korábbi VIZIT-verziót, majd telepítsd a `0.4.3 (34)` buildet.
2. Regisztrálj az új tesztcímmel, fogadd el a jogi dokumentumokat, majd nyisd meg a legutolsó megerősítő levelet.
3. Lépj be, hozd létre az egységes tesztprofilt, és válaszd ki a `KÉP-A` képet.
4. Mentsd a profilt.
5. A **Beállítások → Profil szinkron** résznél várd meg a `Szinkronizálva` állapotot.
6. Zárd be teljesen az alkalmazást, indítsd újra, és ellenőrizd az összes mezőt és a képet.

Elvárt eredmény: a regisztráció, az e-mail-megerősítés, a belépés és a teljes profil megmarad; nincs hibaüzenet vagy adatvesztés.

Eredmény: `NEM FUTOTT / PASS / FAIL`

## 2. Többkészülékes kép- és profilszinkron – iPhone B

1. Telepítsd a `0.4.3 (34)` buildet az iPhone B-re, majd lépj be ugyanabba a fiókba.
2. Ellenőrizd, hogy minden mező és a `KÉP-A` automatikusan megjelenik.
3. iPhone B-n cseréld le a képet `KÉP-B`-re, mentsd, és várd meg a `Szinkronizálva` állapotot.
4. iPhone A-n indítsd újra az alkalmazást. Ellenőrizd a `KÉP-B` képet.
5. iPhone A-n töröld a profilképet, mentsd, majd várd meg a `Szinkronizálva` állapotot.
6. iPhone B-n indítsd újra az alkalmazást. Ellenőrizd, hogy a kép eltűnt, a többi adat változatlan.

Elvárt eredmény: a képcsere és a képtörlés mindkét irányban átmegy, régi kép nem tér vissza.

Eredmény: `NEM FUTOTT / PASS / FAIL`

## 3. Offline → online szinkron

1. iPhone A-n kapcsold be a repülő módot.
2. Módosítsd a beosztást erre: `E2E offline A`, és válaszd ki újra a `KÉP-A` képet.
3. Mentsd, zárd be teljesen az alkalmazást, majd indítsd újra továbbra is offline.
4. Ellenőrizd, hogy a helyi módosítás és a kép megmaradt.
5. Kapcsold ki a repülő módot, majd a **Beállításokban** nyomd meg a `Szinkron újrapróbálása` gombot, ha megjelenik.
6. Várd meg a `Szinkronizálva` állapotot, majd iPhone B-n indítsd újra az alkalmazást.

Elvárt eredmény: offline nincs adatvesztés; online visszatérés után iPhone B-n is `E2E offline A` és `KÉP-A` látszik.

Eredmény: `NEM FUTOTT / PASS / FAIL`

## 4. Kétkészülékes konfliktus

1. Mindkét iPhone-on legyen a legfrissebb, szinkronizált állapot, majd kapcsold be a repülő módot mindkettőn.
2. iPhone A-n állítsd a céget `RayWorks A` értékre, és mentsd.
3. iPhone B-n állítsd a céget `RayWorks B` értékre, és mentsd.
4. iPhone B-t tedd online, és várd meg a `Szinkronizálva` állapotot.
5. iPhone A-t tedd online, és indítsd újra a szinkront.
6. Ellenőrizd, hogy `Szinkronütközés` jelenik meg, és az alkalmazás nem írja felül automatikusan egyik változatot sem.
7. Válaszd a **Felhőben lévő változat használata** lehetőséget.
8. Ellenőrizd, hogy mindkét készüléken `RayWorks B` jelenik meg.

Elvárt eredmény: az ütközés látható és kézzel feloldható; a helyi változat felülírás előtt biztonsági másolatot kap.

Eredmény: `NEM FUTOTT / PASS / FAIL`

## 5. QR és valódi kontaktmentés – iPhone → iPhone

1. iPhone A-n állítsd vissza a céget `RayWorks` értékre, és várd meg a szinkront.
2. Nyisd meg az **Átadás** képernyőt és a **Fényképes QR**-t teljes képernyőn.
3. iPhone B-n a VIZIT **Beolvasás** képernyőjéről, valódi kamerával olvasd be a kódot.
4. A webcím megjelenésekor válaszd a **Megnyitás** lehetőséget.
5. A megnyíló profiloldalon indítsd el a kontaktmentést, majd az iOS kontaktlapján nyomd meg a **Kész** gombot.
6. A Kontaktok alkalmazásban keresd meg a `VIZIT E2E 034` névjegyet.

Elvárt eredmény: név, telefon, e-mail, cég, beosztás, cím, webcím és profilkép helyesen mentődik.

Eredmény: `NEM FUTOTT / PASS / FAIL`

## 6. QR és valódi kontaktmentés – iPhone → Android

1. Telepítsd az Android C-re a `vizit-dev-debug` APK-t.
2. iPhone A-n jelenítsd meg ugyanazt a **Fényképes QR**-t.
3. Android C-n a gyári kamerával olvasd be, nyisd meg a profilt, és töltsd le/mentsd a kontaktot.
4. A Kontaktok alkalmazásban keresd meg a `VIZIT E2E 034` névjegyet.

Elvárt eredmény: a névjegy a profilképpel és az összes kitöltött mezővel megjelenik. A fogadó telefonon nem szükséges VIZIT-fiók.

Eredmény: `NEM FUTOTT / PASS / FAIL`

## 7. Közös adatmodell – iOS → Android

1. Android C-n nyisd meg a VIZIT Dev alkalmazást, és lépj be ugyanabba a fiókba.
2. Ellenőrizd a profil minden mezőjét és a profilképet.
3. Android C-n módosítsd a beosztást `E2E Android` értékre, mentsd, és várd meg a sikeres szinkront.
4. iPhone A-n indítsd újra az alkalmazást.

Elvárt eredmény: Androidon ugyanaz a profil és kép jelenik meg; az androidos módosítás iPhone-on is megérkezik.

Eredmény: `NEM FUTOTT / PASS / FAIL`

## Zárás

| Ellenőrzés | Állapot |
|---|---|
| iOS regisztráció és e-mail-megerősítés | `NEM FUTOTT` |
| iOS profil és első képszinkron | `NEM FUTOTT` |
| iPhone ↔ iPhone kép csere/törlés | `NEM FUTOTT` |
| Offline → online | `NEM FUTOTT` |
| Konfliktusfeloldás | `NEM FUTOTT` |
| iPhone → iPhone QR és kontaktmentés | `NEM FUTOTT` |
| iPhone → Android QR és kontaktmentés | `NEM FUTOTT` |
| iOS ↔ Android közös adatmodell | `NEM FUTOTT` |

Összesített döntés: `NEM MINŐSÍTETT / PASS / FAIL`

FAIL esetén rögzítendő: pontos lépés, készülék, verzió, képernyőkép, hibaüzenet és az, hogy az adat helyben vagy a másik készüléken megmaradt-e.
