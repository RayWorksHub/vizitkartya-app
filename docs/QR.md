# QR

Két mód készül ugyanabból a lokális profilból.

## Kontakt QR

`IMPLEMENTED`, `TESTED`: ez az alapértelmezett és elsődleges QR-mód. Szabványos vCard 3.0 szöveg, Base64 profilkép nélkül, legfeljebb 1800 UTF-8 bájtos gyakorlati payloaddal. Internet nélkül generálható. A generátor M hibajavítást és 4 modul quiet zone-t használ; a magyar szöveg determinisztikus round-trip unit tesztje a 9. Android CI-futásban sikeres. Fizikai kamera/QR kompatibilitási teszt továbbra is szükséges Androidon és iPhone-on, ezért még nem `DEVICE TESTED` vagy `SUPPORTED`.

## VIZIT profil QR

`PARTIALLY SUPPORTED`: validált, normalizált HTTPS URL `https://<profile-host>/p/{slug}` generálása és az Android App Link intent elkészült. A publikus profiloldal és a domain `assetlinks.json` még szükséges a teljes end-to-end működéshez.

## UX

Megvalósítva:

- nagy kontrasztú fekete-fehér QR és 4 modul quiet zone;
- elsődleges Kontakt QR / másodlagos VIZIT profil QR módváltás;
- teljes képernyős QR;
- teljes képernyőn ideiglenes maximális fényerő, kilépéskor visszaállítással;
- QR megosztása PNG-ként;
- mentés `Pictures/VIZIT` alá Android 10+ MediaStore használatával;
- profil-link másolása.

A QR-generátor determinisztikus azonos payload és konfiguráció mellett.
