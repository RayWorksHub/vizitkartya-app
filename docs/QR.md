# QR

Az iOS 8.0 két módot készít ugyanabból a lokális profilból. Mindkét módnál
kötelező a profilkép; nincs csendes visszaesés kép nélküli névjegyre.

## Fényképes kontakt QR

`IMPLEMENTED`, `TESTED`: ez az alapértelmezett és elsődleges iOS QR-mód.
Szabványos vCard 3.0 szöveg, legfeljebb 2200 UTF-8 bájtos gyakorlati
payloaddal. A profilképet több méret- és minőségi lépcsőben optimalizálja, de
soha nem hagyja el. Internet nélkül generálható. A generátor M hibajavítást,
4 modul quiet zone-t és középre helyezett, fehér alapon megjelenő VIZIT
V-logót használ.

## VIZIT profil QR

`PARTIALLY SUPPORTED`: validált, normalizált HTTPS URL `https://<profile-host>/p/{slug}` generálása és az Android App Link intent elkészült. A publikus profiloldal és a domain `assetlinks.json` még szükséges a teljes end-to-end működéshez.

Ha a fénykép az optimalizálás után sem fér el biztonságosan, az alkalmazás
nem készít kép nélküli QR-t: rövidítendő mezőket jelez. A kód nem tartalmaz
`https://` hivatkozást, ezért a rendszerkamera közvetlen névjegyként ismeri fel.

## UX

Megvalósítva:

- nagy kontrasztú fekete-fehér QR és 4 modul quiet zone;
- elsődleges fényképes kontakt QR / másodlagos VIZIT profil QR módváltás;
- teljes képernyős QR;
- teljes képernyőn ideiglenes maximális fényerő, kilépéskor visszaállítással;
- QR megosztása PNG-ként;
- mentés `Pictures/VIZIT` alá Android 10+ MediaStore használatával;
- profil-link másolása.

A QR-generátor determinisztikus azonos payload és konfiguráció mellett.
