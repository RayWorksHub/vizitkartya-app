# Release

Fejlesztési út: `feature/*` → `develop` → teszt → `main`.

Tervezett tagek: `v0.2.0-alpha.1`, `v0.5.0-beta.1`, `v0.9.0-rc.1`, majd kizárólag Definition of Done teljesülésekor `v1.0.0`.

A `main` stabil ág. Signing kulcs és jelszó GitHub Secret / biztonságos release környezetben tárolandó, repositoryban soha.

Az iPhone teljes Android-szerű NFC kontaktimportjának hiánya nem release blocker, ha a QR és HTTPS profil fallback stabil és dokumentált. Súlyos támogatott Android NFC hiba release blocker.


## Platformonként független alkalmazásverzió

Az Android és az iOS verziózása egymástól független:

- Android: `config/android-version.properties`
- iOS: `config/ios-version.properties`

Android release esetén:

```bash
python3 scripts/sync-version.py --apply-android
python3 scripts/sync-version.py --check-android
```

iOS release esetén:

```bash
python3 scripts/sync-version.py --apply-ios
python3 scripts/sync-version.py --check-ios
```

Egyik parancs sem írja át vagy ellenőrzi a másik platform verzióját. Az Android
`versionCode` külön, monoton növekvő Play buildazonosító.

Az iOS `CURRENT_PROJECT_VERSION` külön buildszám. A TestFlight CI ezt a GitHub Actions futásszámából állítja elő, ezért nem kell megegyeznie az Android `versionCode` értékével.

Jelenlegi Android-verzió: **10.0.3** (`versionCode`: **10000008**).
Jelenlegi iOS-verzió: **10.0.1**.
