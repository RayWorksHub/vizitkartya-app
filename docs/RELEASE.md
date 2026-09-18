# Release

Fejlesztési út: `feature/*` → `develop` → teszt → `main`.

Tervezett tagek: `v0.2.0-alpha.1`, `v0.5.0-beta.1`, `v0.9.0-rc.1`, majd kizárólag Definition of Done teljesülésekor `v1.0.0`.

A `main` stabil ág. Signing kulcs és jelszó GitHub Secret / biztonságos release környezetben tárolandó, repositoryban soha.

Az iPhone teljes Android-szerű NFC kontaktimportjának hiánya nem release blocker, ha a QR és HTTPS profil fallback stabil és dokumentált. Súlyos támogatott Android NFC hiba release blocker.
