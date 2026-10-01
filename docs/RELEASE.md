# Release

Fejlesztési út: `feature/*` → `develop` → teszt → `main`.

Tervezett tagek: `v0.2.0-alpha.1`, `v0.5.0-beta.1`, `v0.9.0-rc.1`, majd kizárólag Definition of Done teljesülésekor `v1.0.0`.

A `main` stabil ág. Signing kulcs és jelszó GitHub Secret / biztonságos release környezetben tárolandó, repositoryban soha.

Az iPhone teljes Android-szerű NFC kontaktimportjának hiánya nem release blocker, ha a QR és HTTPS profil fallback stabil és dokumentált. Súlyos támogatott Android NFC hiba release blocker.


## Egységes alkalmazásverzió

Az Android és az iOS felhasználó által látható verziószáma közös. Az egyetlen forrás:

`config/version.properties`

Új release esetén kizárólag a `versionName` értékét kell módosítani, majd futtatni:

```bash
python3 scripts/sync-version.py --apply
python3 scripts/sync-version.py --check
```

A szinkronizáló script:

- ugyanazt a `MAJOR.MINOR.PATCH` verziót írja az Android `versionName` és az iOS `MARKETING_VERSION` mezőjébe;
- az Android `versionCode` értékét determinisztikusan képezi: `MAJOR * 1_000_000 + MINOR * 10_000 + PATCH`;
- CI-ben hibára fut, ha bármelyik platform verziója eltér a központi értéktől.

Az iOS `CURRENT_PROJECT_VERSION` külön buildszám. A TestFlight CI ezt a GitHub Actions futásszámából állítja elő, ezért nem kell megegyeznie az Android `versionCode` értékével.

Jelenlegi közös publikus verzió: **8.7.5**. Android `versionCode`: **10000004**.
