# QR

Két mód készül ugyanabból a lokális profilból.

## Kontakt QR

`IMPLEMENTED`, `TESTED`: ez az alapértelmezett és elsődleges QR-mód. Szabványos vCard 3.0 szöveg, legfeljebb 2400 UTF-8 bájtos gyakorlati payloaddal. Internet nélkül generálható. A generátor L hibajavítást és 4 modul quiet zone-t használ. Fizikai kamera/QR kompatibilitási teszt továbbra is szükséges Androidon és iPhone-on.

## VIZIT profil QR

`PARTIALLY SUPPORTED`: validált, normalizált HTTPS URL `https://<profile-host>/p/{slug}` generálása és az Android App Link intent elkészült. A publikus profiloldal és a domain `assetlinks.json` még szükséges a teljes end-to-end működéshez.

## Fényképes kontakt QR

A QR-kód maga tartalmazza a `BEGIN:VCARD` névjegyet és az erősen optimalizált
JPEG profilképet. Nem tartalmaz `https://` hivatkozást, mert azt a rendszerkamerák
weboldalként osztályozzák még a szerver válasza előtt. Ha a kép az optimalizálás
után sem fér el biztonságosan, az alkalmazás hagyományos Kontakt QR-t jelenít meg,
és nem nevezi fényképesnek.

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
