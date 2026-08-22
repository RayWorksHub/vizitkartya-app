# QR

## Profil QR

Alapértelmezett üzleti QR:

`https://vizit.hu/p/{slug}`

- rövid, stabil HTTPS URL;
- Androidon App Link nyithatja a telepített VIZIT-et;
- alkalmazás nélkül a minimális publikus profiloldal nyílik;
- iPhone-on Safari fallback működik;
- csak publikált profilhoz generálható.

## Kontakt QR

- vCard 3.0 UTF-8 payload;
- nincs beágyazott profilkép;
- csak a megosztásra engedélyezett mezők;
- szigorú méretkorlát a gyors beolvasás érdekében;
- offline is előállítható a lokális profilból.

A stock kamerák vCard-felismerése nem egységes. Ezért a Kontakt QR készülékenként tesztelendő, és a Profil QR mindig elérhető marad.

## Renderelési követelmények

- fekete modulok fehér alapon;
- legalább 4 modul quiet zone;
- dekoráció nem fedheti a modulokat;
- teljes képernyős megjelenítés;
- a fényerő csak a képernyő életciklusára emelhető, kilépéskor visszaállítandó;
- ugyanazon payload és beállítás ugyanazt a QR-mátrixot adja.

## App Link

Az Android intent filter a `https://vizit.hu/p/...` útvonalat kezeli. A domainen a production aláíró tanúsítvány SHA-256 fingerprintjével ellátott `/.well-known/assetlinks.json` szükséges. DEV és BETA külön package/fingerprint sorokat igényel, ha azokhoz is automatikus verifikáció kell.
