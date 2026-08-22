# Release

## Ágmodell

`feature/...` → `develop` → ellenőrzött release PR → `main`

- napi fejlesztés: `develop` vagy rövid életű feature ág;
- `main`: csak zöld CI és lezárt regresszió;
- közvetlen napi commit a `main` ágra tilos.

## Verziók

- `v0.2.0-alpha.1`: stabil fejlesztési alap;
- `v0.5.0-beta.1`: készülékes béta;
- `v0.9.0-rc.1`: teljes regresszióra jelölt kiadás;
- `v1.0.0`: csak a Definition of Done teljesülése után.

## Signing

- Keystore és jelszó nem kerülhet Gitbe.
- DEV debug kulccsal épülhet.
- BETA/PROD signing GitHub Environment secretből vagy védett release környezetből érkezik.
- A production SHA-256 fingerprint szükséges az Android App Links `assetlinks.json` fájljához.

## Release előtti kötelező ellenőrzés

- CI zöld;
- nincs blocker crash;
- migration és rollback terv ellenőrzött;
- Android készülékmátrix kritikus sorai `SUPPORTED`;
- iPhone QR/HTTPS fallback `SUPPORTED`;
- privacy és account deletion tesztelt;
- verzió- és kompatibilitási dokumentáció friss.
