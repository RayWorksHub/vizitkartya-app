# VIZIT v2 gap analysis

Audit dátuma: 2026-08-22

Auditált alap commit: `d7be71f` (`origin/develop`, PR #3 merge)

Master követelmény: `docs/VIZIT_MASTER_SPEC_V1.md`

## Állapotjelölések

- `IMPLEMENTED`: a kód elkészült, de nem feltétlenül tesztelt.
- `TESTED`: automatizált teszt vagy CI ellenőrizte.
- `DEVICE TESTED`: fizikai készüléken ellenőrizve.
- `PARTIALLY SUPPORTED`: a követelménynek csak egy része készült el.
- `REQUIRES REFACTOR`: a meglévő megoldás production előtt javítandó.
- `BLOCKED BY EXTERNAL ACCESS`: külső hozzáférés, credential vagy eszköz hiányzik.

## Ellenőrzött alapállapot

Az `Android CI` 10. futása sikeresen teljesítette a unit test, lint, DEV/BETA debug build és PROD release compile lépéseket a PR #2 merge előtti, azonos tartalmú feature headen. A `develop` alap ennek squash merge commitja.

| Terület | Állapot | Audit eredménye |
|---|---|---|
| Gradle, Kotlin, Compose, SDK | `TESTED` | API 29–36, Java 17, Kotlin 2.3.21, Compose és Material 3 buildel. |
| DEV/BETA/PROD flavor | `TESTED` | Külön application ID/név/config mezők létrejöttek; a végleges PROD package ID még nincs jóváhagyva. |
| CI | `TESTED` | Unit test, lint és mindhárom környezet buildje sikeres. |
| Projektstruktúra | `REQUIRES REFACTOR` | Még gyökérszintű `app/`; a master szerinti `apps/android`, `shared` és teljes docs struktúra nincs kész. |
| Hivatalos branding | `TESTED` | A jóváhagyott master asset, adaptive/round/themed launcher ikon és kontrollált brandfelületek buildelnek. |
| Auth | `IMPLEMENTED`, élő E2E blokkolt | Teljes email/jelszó UX, session/reset, validált callback, post-auth jogi gate, Google adapter, explicit fióktörlés és lokális purge kész; élő DEV bizonyíték hiányzik. |
| Supabase kliens | `IMPLEMENTED` | Auth/PostgREST/Storage/Functions kliens és flavor config kész, de távoli projekten nincs ellenőrizve. |
| Supabase schema/RLS | `IMPLEMENTED` | Forward-only privacy/auth/storage hardening, jogi RPC és verzióellenőrzött profil-sync kész; távoli DEV projekten még nincs alkalmazva vagy E2E tesztelve. |
| Room | `IMPLEMENTED` | A futó profilrepository Room source of truth; v1→v2 migráció és a korábbi SharedPreferences adat egyszeri átemelése elkészült. |
| DataStore | `IMPLEMENTED` | Az automatikus szinkron, az aktív profiltulajdonos és a legacy migráció állapota futásidőben be van kötve. |
| Offline sync | `IMPLEMENTED` | Atomi, összevont outbox, WorkManager hálózati constraint, exponenciális retry, sync lease, idempotens Supabase RPC és verziókonfliktus-blokkolás készült. Élő Supabase E2E még szükséges. |
| Profilmodell | `REQUIRES REFACTOR` | A Room séma normalizált, de a futó UI még egyetlen telefon/e-mail/cím/link mezőt mutat; többértékű szerkesztés, sorrend és láthatóság nincs bekötve. |
| Profilkép | `PARTIALLY SUPPORTED` | Photo Picker és tömörítés működik; crop/pan/zoom UI, külön display/thumbnail/contact asset pipeline nincs kész. |
| Céges logó | `BLOCKED` | Külön `contain` alapú cégeslogó-kezelés még nincs. |
| NFC HCE/NDEF/APDU | `TESTED` | Process-local payload, session guard, foreground preferred service és teljes NDEF-olvasás unit/build szinten ellenőrzött; fizikai teszt hiányzik. |
| vCard | `TESTED`, `PARTIALLY SUPPORTED` | Strukturált név és UTF-8 bájthelyes folding kész; a többértékű profil UI bekötése hiányzik. |
| NFC kép/payload | `IMPLEMENTED` | Adaptív fotótömörítés és fallback létezik; a 24 KiB célérték fizikai teszt nélkül nem tekinthető támogatottnak. |
| NFC lifecycle/UX | `PARTIALLY SUPPORTED` | Start/stop/timeout/semleges read visszajelzés kész; fizikai lifecycle és OEM működés nincs igazolva. |
| Kontakt QR | `TESTED`, `PARTIALLY SUPPORTED` | Elsődleges offline vCard QR, méretbudget és round-trip unit teszt kész; fizikai kamera-teszt hiányzik. |
| Profil QR | `PARTIALLY SUPPORTED` | Konfigurált HTTPS URL kész; slug validáció, publikus webprofil és domain még hiányzik. |
| QR UX | `PARTIALLY SUPPORTED` | Fullscreen, fényerő, megosztás és mentés kész; Kontakt QR prioritás és eszközteszt hiányzik. |
| Publikus profil/App Link | `PARTIALLY SUPPORTED` | App Link intent megvan; weboldal, `assetlinks.json` és biztonságos publikus endpoint E2E nincs. |
| Xiaomi/HyperOS UX | `PARTIALLY SUPPORTED` | Edge-to-edge és Material 3 alap kész; predictive back, OEM NFC/beállítás, battery és accessibility audit hiányzik. |
| Fizikai kompatibilitás | `BLOCKED BY EXTERNAL ACCESS` | Egyetlen NFC/QR sor sem `DEVICE TESTED`; Xiaomi/Redmi/POCO/Samsung/Pixel/iPhone tesztpark szükséges. |
| Crash/performance/security review | `BLOCKED` | Crash monitoring, ANR mérés és teljes release security review nincs kész. |

## Első, developba merge-elt javítási blokk

Az alábbiak implementálva és statikusan ellenőrizve vannak. A PR 9. Android CI-futása sikeresen teljesítette a unit test, lint, DEV/BETA debug build és PROD release compile lépéseket; az automatizálható részek ezért `TESTED` státuszúak. Fizikai készülékteszt még nem történt.

| Terület | Feature ág állapota |
|---|---|
| Hivatalos branding | `TESTED`: a csatolt master asset byte-pontosan bekerült; official markos adaptive/round/themed ikon, kontrollált brandfelületek és kikapcsolt alapértelmezett Dynamic Color készült. |
| Supabase privacy | `TESTED` buildszinten: külön forward-only hardening migráció javítja a publikus RPC mezőszintű szűrését, a privacy-safe defaultokat, slug-integritást, search pathot és jogi elfogadás naplózását. Távoli Supabase E2E még szükséges. |
| NFC HCE | `TESTED` unit/build szinten: a payload process-local memóriába került; session guard, foreground preferred service, teljes NDEF-olvasás visszajelzés és 16 KiB konzervatív budget készült. Fizikai NFC-teszt még szükséges. |
| vCard/NDEF | `TESTED`: strukturált név, UTF-8 oktettalapú folding és egyetlen MIME vCard NDEF rekord készült. |
| Kontakt QR | `TESTED` unit/build szinten: elsődleges alapmód, 1800 bájtos budget, M hibajavítás, 4 modul quiet zone, slug/HTTPS validáció és round-trip teszt készült. Fizikai kamera-teszt még szükséges. |
| Auth jogi gate | `TESTED` unit/build szinten: kötelező checkbox, HTTPS dokumentumkonfiguráció, verziózott signup metadata és DEV-only Google adapter gate készült. Távoli Auth E2E még szükséges. |

A következő kötelező bizonyíték: távoli Supabase/Auth E2E, majd valós NFC/QR készülékteszt.

## Második javítási blokk ezen a feature ágon

Az `Android CI` 16. futása sikeresen teljesítette a unit test, lint, DEV/BETA debug build és PROD release compile lépéseket. A Room-migráció, valamint a távoli Supabase RPC működése továbbra is külön integrációs/E2E bizonyítékot igényel.

| Terület | Feature ág állapota |
|---|---|
| Room runtime | `IMPLEMENTED`: a profil UI Room `Flow` source of truth-ból él; a v1→v2 adatbázis-migráció és a legacy SharedPreferences átemelése adatvesztés ellen védett. |
| DataStore runtime | `IMPLEMENTED`: automatikus sync, aktív profiltulajdonos és migrációjelző futásidőben bekötve. |
| Outbox/retry | `IMPLEMENTED`: a profilmentés és az összevont outbox atomi; network constraint, exponenciális backoff, process-kill lease és kézi retry készült. |
| Konfliktuskezelés | `IMPLEMENTED`: szerververzió-ütközésnél a helyi és távoli snapshot megmarad, az automatikus overwrite leáll és a UI visszajelez. |
| Supabase sync | `IMPLEMENTED`: idempotens, tranzakciós push/pull RPC és mobiladapter készült. Távoli migráció/E2E engedély és DEV hozzáférés nélkül nem futott. |
| Automatizált ellenőrzés | `TESTED`: mapper, PII-kizárás, stabil ID, retry, sessionhiány, push, pull és konfliktus JVM tesztek, továbbá lint és mindhárom környezet buildje sikeres. |

## P0 javítási sorrend

1. teljes Auth UX és Supabase DEV E2E;
2. többértékű profil, mezősorrend/láthatóság, kép- és cégeslogó pipeline;
3. publikus profil és App Link end-to-end;
4. fizikai Xiaomi/cross-OEM teszt és kompatibilitási mátrix.

## Harmadik javítási blokk ezen a feature ágon

| Terület | Feature ág állapota |
|---|---|
| Auth UX | `IMPLEMENTED`: külön regisztráció/email-verifikáció/elfelejtett jelszó/reset/jogi állapot, password visibility, loading lock és emberi hibák készültek. |
| Callback security | `TESTED` JVM szinten: pontos flavor scheme/host/path ellenőrzés, spoof/port/userinfo/méret tiltás és biztonságos lejárt-link visszajelzés készült. |
| Jogi gate | `IMPLEMENTED`: signup trigger mellett OAuth/régi user RPC-ellenőrzés és elfogadás, user+verzió kötött offline cache, valamint profil-RPC gate készült. |
| Fiókkezelés | `IMPLEMENTED`: beállításokbeli logout, `TÖRLÉS` megerősítés, lokális Room/outbox purge, rekurzív storage cleanup és Auth-kaszkád készült. |
| Storage security | `IMPLEMENTED`: törölt Auth-user még élő JWT-je nem írhat a user bucketjeibe; a törlőfunkció pre/post cleanupot végez. |
| Supabase tesztek | `IMPLEMENTED`, futtatás blokkolt: 24 állításos pgTAP RLS/RPC suite és őrzött távoli Auth/Profile/RLS/Storage/Delete E2E runner készült. |
| Élő DEV bizonyíték | `BLOCKED BY EXTERNAL ACCESS`: az elérhető VIZIT projekt `main / Production`; külön DEV projekt vagy branch nincs azonosítva, ezért távoli módosítás nem történt. |

A feature ág CI-státusza a PR létrehozása után kerül rögzítésre.

## Külső blokkolók

- DEV/BETA/PROD Supabase projekt URL és publishable key;
- Google OAuth web client ID és Supabase provider konfiguráció;
- `vizit.hu` HTTPS profilhost és `assetlinks.json`;
- végleges production application ID;
- production signing keystore/secretek;
- Xiaomi Developer jóváhagyás;
- fizikai Xiaomi, Redmi, POCO, Samsung, Pixel és iPhone tesztkészülékek.

Ezek egyikét sem szabad hamis production credentialdel vagy nem ellenőrzött kompatibilitási állítással megkerülni.
