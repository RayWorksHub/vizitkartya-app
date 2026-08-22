# VIZIT Android v2

A VIZIT Xiaomi/HyperOS-first natív Android digitális névjegy- és kontaktmegosztó alkalmazás.

A fejlesztés elsődleges követelményforrása a [`docs/VIZIT_MASTER_SPEC_V1.md`](docs/VIZIT_MASTER_SPEC_V1.md). A jelenlegi kód alpha állapotú; nem production kiadás.

## Termékcél

- telefon → telefon → új kontakt NFC-vel;
- beolvasás → új kontakt Kontakt QR-rel;
- fogadóoldali VIZIT-telepítés nélkül;
- `.vcf` letöltés és fájlkezelő nélkül a támogatott fő folyamatokban;
- QR + HTTPS publikus profil stabil platformfüggetlen fallbackként.

## Jelenlegi állapot

- Kotlin, Jetpack Compose, Material 3, API 29–36;
- DEV/BETA/PROD build flavor;
- Supabase Auth/Storage/PostgREST/Functions kliens és Google Credential Manager adapter;
- Room source of truth, DataStore beállítások, atomi outbox és WorkManager profil-szinkron;
- vCard 3.0, NFC Forum Type 4 Tag / NDEF HCE;
- Kontakt QR és HTTPS profil QR alapfolyamat;
- teljes email/jelszó Auth UX, jogi gate, biztonságos callback és explicit, médiát is takarító fióktörlés;
- hivatalos VIZIT brand asset és adaptív launcher icon;
- GitHub Actions unit test, lint, DEV/BETA build és PROD release compile.

A pontos, bizonyítékhoz kötött modulstátuszt a [`docs/GAP_ANALYSIS.md`](docs/GAP_ANALYSIS.md) tartalmazza. A `DEVICE TESTED` és `SUPPORTED` NFC/QR státuszhoz valós készülékteszt kötelező.

## Fejlesztői ellenőrzés

1. Nyisd meg a projektet a legfrissebb stabil Android Studióban.
2. Telepítsd az Android 16 / API 36 SDK-t.
3. Indítsd a DEV buildet NFC/HCE-képes Android 10+ készüléken.

Parancssori ellenőrzés:

```bash
./gradlew testDevDebugUnitTest lintDevDebug assembleDevDebug assembleBetaDebug assembleProdRelease
```

## Környezeti konfiguráció

A Supabase, Google OAuth és publikus profil értékei Gradle propertyből érkeznek. Production secret vagy service-role kulcs nem kerülhet a mobil buildbe vagy Gitbe. Részletek: [`docs/SUPABASE.md`](docs/SUPABASE.md) és [`docs/AUTH.md`](docs/AUTH.md).

## Dokumentáció

- [`docs/VIZIT_MASTER_SPEC_V1.md`](docs/VIZIT_MASTER_SPEC_V1.md)
- [`docs/GAP_ANALYSIS.md`](docs/GAP_ANALYSIS.md)
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)
- [`docs/BRANDING.md`](docs/BRANDING.md)
- [`docs/AUTH.md`](docs/AUTH.md)
- [`docs/SUPABASE.md`](docs/SUPABASE.md)
- [`docs/OFFLINE_SYNC.md`](docs/OFFLINE_SYNC.md)
- [`docs/NFC.md`](docs/NFC.md)
- [`docs/QR.md`](docs/QR.md)
- [`docs/PLATFORM_COMPATIBILITY.md`](docs/PLATFORM_COMPATIBILITY.md)
- [`docs/TESTING.md`](docs/TESTING.md)
- [`docs/RELEASE.md`](docs/RELEASE.md)
- [`docs/BLOCKERS.md`](docs/BLOCKERS.md)
