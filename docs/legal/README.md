# VIZIT jogi dokumentumok

Ez a mappa a VIZIT regisztrációs felületén hivatkozott dokumentumok forrásszövegét tartalmazza.

## Dokumentumok

- `ADATKEZELESI_TAJEKOZTATO.md` – Adatkezelési tájékoztató
- `ASZF.md` – Általános Szerződési Feltételek és Felhasználási Feltételek

## Állapot

**1.0 – tervezet, 2026-09-19.**

A dokumentumok ezen az ágon kizárólag fejlesztési és felülvizsgálati célból készültek. Nem kerültek TestFlightba és nem történt PROD-publikálás.

## Élesítés előtt kötelezően lezárandó

1. Székhely kitöltése.
2. Adószám kitöltése.
3. Egyéni vállalkozói nyilvántartási szám kitöltése.
4. A tényleges PROD Supabase-régió, DPA és adattovábbítás ellenőrzése.
5. A nyilvános profil webes infrastruktúrájának végleges szolgáltatói listája.
6. A backup-, security log- és hibalog-megőrzési idők véglegesítése.
7. Fióktörlési folyamat és törlési SLA technikai ellenőrzése.
8. Fizetős funkciók esetén a vásárlási, számlázási és elállási szabályok kiegészítése.
9. Fogyasztóvédelmi és békéltető testületi adatok véglegesítése.
10. Jogi felülvizsgálat.

## Integrációs elv

A mobilalkalmazásban az elfogadáskor tárolt dokumentumverzió egyezzen az éppen megjelenített dokumentum verziójával.

Javasolt első éles verzióazonosítók:

- privacy: `2026-09-19-v1`
- terms: `2026-09-19-v1`

A jelen ág nem módosít TestFlight- vagy App Store-kiadást.
