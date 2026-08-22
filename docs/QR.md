# QR

Két mód készül ugyanabból a lokális profilból.

## Kontakt QR

`IMPLEMENTED`: szabványos vCard 3.0 szöveg, Base64 profilkép nélkül. Internet nélkül generálható. Fizikai kamera/QR kompatibilitási teszt szükséges Androidon és iPhone-on, ezért még nem `SUPPORTED`.

## VIZIT profil QR

`PARTIALLY SUPPORTED`: stabil HTTPS URL `https://<profile-host>/p/{slug}` generálása és az Android App Link intent elkészült. A publikus profiloldal és a domain `assetlinks.json` még szükséges a teljes end-to-end működéshez.

## UX

Megvalósítva:

- nagy kontrasztú fekete-fehér QR és 4 modul quiet zone;
- VIZIT profil / Kontakt mód váltás;
- teljes képernyős QR;
- teljes képernyőn ideiglenes maximális fényerő, kilépéskor visszaállítással;
- QR megosztása PNG-ként;
- mentés `Pictures/VIZIT` alá Android 10+ MediaStore használatával;
- profil-link másolása.

A QR-generátor determinisztikus azonos payload és konfiguráció mellett.
