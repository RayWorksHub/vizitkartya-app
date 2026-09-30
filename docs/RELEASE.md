# Release

Fejlesztési út: `feature/*` → `develop` → teszt → `main`.

Tervezett tagek: `v0.2.0-alpha.1`, `v0.5.0-beta.1`, `v0.9.0-rc.1`, majd kizárólag Definition of Done teljesülésekor `v1.0.0`.

A `main` stabil ág. Signing kulcs és jelszó GitHub Secret / biztonságos release környezetben tárolandó, repositoryban soha.

Az iPhone teljes Android-szerű NFC kontaktimportjának hiánya nem release blocker, ha a QR és HTTPS profil fallback stabil és dokumentált. Súlyos támogatott Android NFC hiba release blocker.


## Platformonként független alkalmazásverzió

Az Android és az iOS verziója egymástól független. Az Android forrása:

`config/android-version.properties`

Az Android release ellenőrzése és alkalmazása:

```bash
python3 scripts/sync-android-version.py --apply
python3 scripts/sync-android-version.py --check
```

Az iOS forrása és parancsai:

```bash
python3 scripts/sync-ios-version.py --apply
python3 scripts/sync-ios-version.py --check
```

Az Android fájl a `versionName` mellett a Play Console-ban soha újra nem használható
`versionCode` értéket is külön tárolja. Egyik platform szinkronizáló scriptje sem írja át a
másik platform projektjét, és a platform CI útvonalszűrői sem figyelik a másik verziófájlját.

Az iOS `CURRENT_PROJECT_VERSION` külön buildszám. A TestFlight CI ezt a GitHub Actions futásszámából állítja elő, ezért nem kell megegyeznie az Android `versionCode` értékével.

Jelenlegi Android verzió: **10.0.1** (`versionCode`: **10000001**). Jelenlegi iOS verzió: **9.0.0**.

## 9.0.0 – több profil egy fiókban

- A profilválasztó csak akkor enged új profilt létrehozni, amikor az éles `multi_profile` funkciókapcsoló aktív.
- A kiválasztott mobilprofil egyben a fiók alapértelmezett profilja, ezért a 8.x kliensek ugyanazt az aktív profilt látják.
- Profilváltás és profiltörlés csak tiszta, ütközésmentes szinkronállapotban indulhat.
- A mobilalkalmazás az aktív profilt tartja offline gyorsítótárban; másik profilra váltáshoz hálózati kapcsolat szükséges.
