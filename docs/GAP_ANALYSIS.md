# VIZIT v2 gap analysis

Audit dátuma: 2026-08-22  
Auditált kiinduló commit: `cde0286` (`main`, korábbi `0.1.0` proof-of-concept)

## Állapotjelölések

- `IMPLEMENTED`: a kód elkészült, de nem feltétlenül eszköztesztelt.
- `TESTED`: automatizált teszt lefutott.
- `DEVICE TESTED`: fizikai eszközön ellenőrizve.
- `SUPPORTED`: a kompatibilitási mátrixban igazoltan támogatott.
- `PARTIALLY SUPPORTED`: csak a dokumentált részfolyamat működik.
- `FALLBACK REQUIRED`: az elsődleges folyamat helyett QR/HTTPS szükséges.
- `BLOCKED BY EXTERNAL ACCESS`: külső fiók, credential vagy eszköz hiányzik.

## Kiinduló állapot

| Terület | A `cde0286` commit tényleges állapota | Fő hiány | Prioritás |
|---|---|---|---|
| Android alap | Kotlin, Compose, API 29–36 | nincs rétegezett architektúra, DI, navigációs keret | P0 |
| Buildkörnyezet | egyetlen `hu.rayworks.vizit` alkalmazás | DEV/BETA/PROD nincs | P0 |
| CI | unit test + debug assemble | lint nincs külön kapuként, nincs flavor-mátrix | P0 |
| Profil | egyetlen név/telefon/email, `SharedPreferences` | többértékű modell, láthatóság, sorrend, cloud sync nincs | P0 |
| Kép | 512 px center crop, legfeljebb 60 KB JPEG | nincs crop UI, nincs külön NFC-avatar, payload túlcsordulhat | P0 |
| Céges logó | nincs | külön `contain` alapú kezelés hiányzik | P1 |
| NFC HCE | Type 4 Tag/NDEF proof-of-concept | timeout, olvasási visszaigazolás, payload-budget, eszközteszt nincs | P0 |
| vCard | 3.0, néhány alapmező | strukturált név, több mező, teljes byte-helyes folding hiányzik | P0 |
| QR | nincs | Kontakt QR és profil-URL QR teljes egészében hiányzik | P0 |
| Publikus profil | nincs | HTTPS oldal, RLS-alapú publikus olvasás, App Link hiányzik | P0 |
| Supabase | nincs | projekt, migration, RLS, Storage, kliensréteg hiányzik | P0 |
| Auth | nincs | teljes emailes Auth és Google adapter hiányzik | P0 |
| Offline/szinkron | csak lokális POC-adat | Room cache, outbox, konfliktuskezelés hiányzik | P0 |
| Analitika | nincs | privacy-conscious eseménymodell hiányzik | P1 |
| UI/UX | három egyszerű képernyő | teljes állapotmodell, accessibility és platformfallback hiányzik | P1 |
| Automatizált teszt | 2 JVM tesztosztály | repository/ViewModel/QR/UI/integration tesztek hiányoznak | P0 |
| Készülékteszt | nincs bizonyíték | teljes Xiaomi/Android/iPhone mátrix hiányzik | release blocker Androidon |

## Kritikus auditmegállapítások

1. A jelenlegi 60 KB-os JPEG Base64-kódolva körülbelül 80 KB lehet, miközben a Type 4 Tag implementáció 32 767 bájtos NDEF-kapacitást hirdet. A `startNfcShare()` emiatt kivétellel leállhat.
2. Az aktív NFC payload `SharedPreferences`-ben marad. Process crash vagy nem várt lifecycle esetén ez tovább élhet, tehát nem felel meg az egyszer használatos, rövid idejű PII-kezelésnek.
3. A képernyő az NFC-t aktívnak jelzi, de a kód nem bizonyítja, hogy a fogadó az NDEF-fájlt teljesen kiolvasta.
4. A HCE/APDU egységteszt nem bizonyítja, hogy egy adott gyártó Kontaktok alkalmazása közvetlen import UI-t nyit fájlletöltés nélkül.
5. Az Android küldő nem tudja bizonyítani, hogy a fogadó felhasználó végül elmentette a kontaktot. A legpontosabb sikerállapot: `NFC-adat kiolvasva`.
6. Az iPhone nem kezelhető Android HCE-vel azonos, garantált automatikus vCard-import célként. App nélküli, stabil szerződésként a HTTPS profil-URL és a QR használható.
7. A repository nem tartalmaz Supabase projektet, migrationt, RLS-t, Authot, Storage-ot vagy titokkezelési struktúrát.

## Elfogadott technikai irány

- A meglévő HCE komponensek megmaradnak, de szigorú APDU-validációt, egyszer használatos memóriabeli payloadot, timeoutot és teljes-kiolvasás eseményt kapnak.
- A display kép és a kontakt/NFC-avatar külön származtatott asset lesz.
- Android kontaktátadásnál MIME vCard marad az elsődleges payload.
- iPhone és ismeretlen fogadó esetén a stabil alapértelmezett fallback a rövid HTTPS profil-URL és annak QR-kódja.
- A Supabase alapmodell normalizált, több telefon/email/link kezelésére alkalmas, és minden publikus olvasást szűkített RPC-n keresztül végez.
- A jelenlegi POC csak akkor nevezhető támogatottnak, ha a fizikai kompatibilitási mátrix adott sora `SUPPORTED` állapotú.
