# VIZIT Android v2

A VIZIT natív Android/Xiaomi alkalmazása. A fejlesztés a `develop` ágon történik; a jelenlegi verzió `0.2.0-alpha.1`, nem production kiadás.

## Jelenlegi állapot

- natív Kotlin és Jetpack Compose felület;
- külön telepíthető DEV/BETA/PROD flavor;
- helyben tárolt, szerkeszthető proof-of-concept kontaktprofil;
- strukturált vCard 3.0, UTF-8 folding és profilkép;
- NFC Forum Type 4 Tag / NDEF HCE küldés rövid élettartamú, memóriabeli payloaddal;
- teljes NDEF-kiolvasás érzékelése APDU byte-coverage alapján;
- dinamikusan méretezett külön NFC kontakt-avatar és 16 KiB payload-budget;
- Supabase SQL migration normalizált profilmodellel, RLS-sel, Storage policykkal és szűkített publikus RPC-vel;
- fail-closed, flavoronkénti backend konfiguráció credential hardcode nélkül;
- világos, sötét és Android dinamikus színtéma;
- JVM egységtesztek az NFC APDU-folyamathoz, vCardhoz és backend konfigurációhoz.

A Supabase kliens, Auth, Room cache, QR és publikus profiloldal még fejlesztés alatt áll. A pontos állapotot a [`docs/GAP_ANALYSIS.md`](docs/GAP_ANALYSIS.md) tartalmazza.

## NFC működés

A küldő telefon a szabványos NDEF alkalmazás-AID-t (`D2760000850101`) emulálja, és egy `text/vcard` rekordot szolgál ki. A cél az alkalmazás nélküli Android kontaktimport; ennek tényleges támogatása csak fizikai készülékteszttel igazolható.

Az Android gyártói NFC-kezelése eltérhet. Az automatikus Kontaktok-megnyitást és a profilkép átvitelét ezért valódi Xiaomi/HyperOS és célkészülékeken kell kompatibilitási mátrixban ellenőrizni.

## Fejlesztői indítás

1. Nyisd meg a projektet a legfrissebb stabil Android Studióban.
2. Telepítsd az Android 16 / API 36 SDK-t.
3. Indítsd az `app` konfigurációt egy NFC/HCE-képes Android 10+ készüléken.

Parancssori ellenőrzés a DEV flavoron:

```bash
./gradlew testDevDebugUnitTest lintDevDebug assembleDevDebug
```

## Környezeti konfiguráció

A Supabase és publikus profil URL-ek környezeti változóként vagy Gradle propertyként adhatók át; titkos és service-role kulcs tiltott. A kulcsok listája és a migration alkalmazása: [`docs/SUPABASE.md`](docs/SUPABASE.md).

## Dokumentáció

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)
- [`docs/NFC.md`](docs/NFC.md)
- [`docs/QR.md`](docs/QR.md)
- [`docs/SUPABASE.md`](docs/SUPABASE.md)
- [`docs/AUTH.md`](docs/AUTH.md)
- [`docs/PLATFORM_COMPATIBILITY.md`](docs/PLATFORM_COMPATIBILITY.md)
- [`docs/TESTING.md`](docs/TESTING.md)
- [`docs/RELEASE.md`](docs/RELEASE.md)
- [`docs/BLOCKERS.md`](docs/BLOCKERS.md)
