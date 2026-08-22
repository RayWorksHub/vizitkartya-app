# VIZIT v2 gap analysis

Audit dátuma: 2026-08-22

Auditált távoli commit: `6964b1f` (`origin/develop`)

Master követelmény: `docs/VIZIT_MASTER_SPEC_V1.md`

## Állapotjelölések

- `IMPLEMENTED`: a kód elkészült, de nem feltétlenül tesztelt.
- `TESTED`: automatizált teszt vagy CI ellenőrizte.
- `DEVICE TESTED`: fizikai készüléken ellenőrizve.
- `PARTIALLY SUPPORTED`: a követelménynek csak egy része készült el.
- `REQUIRES REFACTOR`: a meglévő megoldás production előtt javítandó.
- `BLOCKED BY EXTERNAL ACCESS`: külső hozzáférés, credential vagy eszköz hiányzik.

## Ellenőrzött alapállapot

Az `Android CI` 8. futása sikeresen teljesítette a unit test, lint, DEV/BETA debug build és PROD release compile lépéseket a `6964b1f` commiton.

| Terület | Állapot | Audit eredménye |
|---|---|---|
| Gradle, Kotlin, Compose, SDK | `TESTED` | API 29–36, Java 17, Kotlin 2.3.21, Compose és Material 3 buildel. |
| DEV/BETA/PROD flavor | `TESTED` | Külön application ID/név/config mezők létrejöttek; a végleges PROD package ID még nincs jóváhagyva. |
| CI | `TESTED` | Unit test, lint és mindhárom környezet buildje sikeres. |
| Projektstruktúra | `REQUIRES REFACTOR` | Még gyökérszintű `app/`; a master szerinti `apps/android`, `shared` és teljes docs struktúra nincs kész. |
| Hivatalos branding | `REQUIRES REFACTOR` | Az auditált commit ideiglenes, nem hivatalos launcher ikont használt; a jóváhagyott asset ebben a feature blokkban került be. |
| Auth | `PARTIALLY SUPPORTED` | Email/jelszó, session, reset, Google adapter és Edge Function shell kész; jogi checkbox, jogi dokumentumok, megerősített törlési UI és élő Supabase E2E teszt hiányzik. |
| Supabase kliens | `IMPLEMENTED` | Auth/PostgREST/Storage/Functions kliens és flavor config kész, de távoli projekten nincs ellenőrizve. |
| Supabase schema/RLS | `REQUIRES REFACTOR` | Alap migration kész, de a publikus RPC a core profilmezők mezőszintű láthatóságát megkerüli; slug- és Storage-szabályok szigorítandók. |
| Room | `IMPLEMENTED` | Entitások és DAO készültek, de a futó profilrepository még nem használja őket. |
| DataStore | `IMPLEMENTED` | Beállítási store elkészült, de nincs bekötve az alkalmazásállapotba. |
| Offline sync | `BLOCKED` | Repository/outbox/WorkManager/retry/konfliktuskezelés nincs kész. |
| Profilmodell | `REQUIRES REFACTOR` | A futó UI még egyetlen név/telefon/email modellt és SharedPreferences-t használ; többértékű mezők, sorrend és láthatóság nincs bekötve. |
| Profilkép | `PARTIALLY SUPPORTED` | Photo Picker és tömörítés működik; crop/pan/zoom UI, külön display/thumbnail/contact asset pipeline nincs kész. |
| Céges logó | `BLOCKED` | Külön `contain` alapú cégeslogó-kezelés még nincs. |
| NFC HCE/NDEF/APDU | `TESTED`, `REQUIRES REFACTOR` | Type 4 Tag és APDU unit tesztelt; a payload személyes adatot ír SharedPreferences-be, nincs session guard és preferred HCE service. |
| vCard | `TESTED`, `REQUIRES REFACTOR` | vCard 3.0 és alapmezők készültek; strukturált név, több mező és UTF-8 bájthelyes folding javítandó. |
| NFC kép/payload | `IMPLEMENTED` | Adaptív fotótömörítés és fallback létezik; a 24 KiB célérték fizikai teszt nélkül nem tekinthető támogatottnak. |
| NFC lifecycle/UX | `PARTIALLY SUPPORTED` | Start/stop/timeout/semleges read visszajelzés kész; fizikai lifecycle és OEM működés nincs igazolva. |
| Kontakt QR | `PARTIALLY SUPPORTED` | Offline vCard QR és UI kész; nem ez az alapértelmezett mód, nincs méretbudget és nincs round-trip teszt. |
| Profil QR | `PARTIALLY SUPPORTED` | Konfigurált HTTPS URL kész; slug validáció, publikus webprofil és domain még hiányzik. |
| QR UX | `PARTIALLY SUPPORTED` | Fullscreen, fényerő, megosztás és mentés kész; Kontakt QR prioritás és eszközteszt hiányzik. |
| Publikus profil/App Link | `PARTIALLY SUPPORTED` | App Link intent megvan; weboldal, `assetlinks.json` és biztonságos publikus endpoint E2E nincs. |
| Xiaomi/HyperOS UX | `PARTIALLY SUPPORTED` | Edge-to-edge és Material 3 alap kész; predictive back, OEM NFC/beállítás, battery és accessibility audit hiányzik. |
| Fizikai kompatibilitás | `BLOCKED BY EXTERNAL ACCESS` | Egyetlen NFC/QR sor sem `DEVICE TESTED`; Xiaomi/Redmi/POCO/Samsung/Pixel/iPhone tesztpark szükséges. |
| Crash/performance/security review | `BLOCKED` | Crash monitoring, ANR mérés és teljes release security review nincs kész. |

## Első javítási blokk ezen a feature ágon

Az alábbiak lokálisan implementálva és statikusan ellenőrizve vannak, de ezen az ágon még nem futott GitHub CI, ezért nem kapnak `TESTED` státuszt:

| Terület | Feature ág állapota |
|---|---|
| Hivatalos branding | A csatolt master asset byte-pontosan bekerült; official markos adaptive/round/themed ikon, kontrollált brandfelületek és kikapcsolt alapértelmezett Dynamic Color készült. |
| Supabase privacy | Külön forward-only hardening migráció javítja a publikus RPC mezőszintű szűrését, a privacy-safe defaultokat, slug-integritást, search pathot és jogi elfogadás naplózását. |
| NFC HCE | A payload process-local memóriába került; session guard, foreground preferred service, teljes NDEF-olvasás visszajelzés és 16 KiB konzervatív budget készült. |
| vCard/NDEF | Strukturált név, UTF-8 oktettalapú folding és egyetlen MIME vCard NDEF rekord készült. |
| Kontakt QR | Elsődleges alapmód, 1800 bájtos budget, M hibajavítás, 4 modul quiet zone, slug/HTTPS validáció és round-trip teszt készült. |
| Auth jogi gate | Kötelező checkbox, HTTPS dokumentumkonfiguráció, verziózott signup metadata és DEV-only Google adapter gate készült. |

A következő kötelező bizonyíték: unit test + lint + DEV/BETA/PROD build CI-n, majd valós NFC/QR készülékteszt.

## P0 javítási sorrend

1. az elkészült brand/privacy/NFC/vCard/QR/Auth blokk CI-validálása és javítása;
2. Room + Supabase local-first repository és outbox sync;
3. teljes Auth UX és Supabase DEV E2E;
4. többértékű profil, mezősorrend/láthatóság, kép- és cégeslogó pipeline;
5. publikus profil és App Link end-to-end;
6. fizikai Xiaomi/cross-OEM teszt és kompatibilitási mátrix.

## Külső blokkolók

- DEV/BETA/PROD Supabase projekt URL és publishable key;
- Google OAuth web client ID és Supabase provider konfiguráció;
- `vizit.hu` HTTPS profilhost és `assetlinks.json`;
- végleges production application ID;
- production signing keystore/secretek;
- Xiaomi Developer jóváhagyás;
- fizikai Xiaomi, Redmi, POCO, Samsung, Pixel és iPhone tesztkészülékek.

Ezek egyikét sem szabad hamis production credentialdel vagy nem ellenőrzött kompatibilitási állítással megkerülni.
