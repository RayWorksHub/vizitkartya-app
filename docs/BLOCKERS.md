# Külső blokkolók

## BLOCKED BY EXTERNAL ACCESS

- Google Auth végleges developer/credential jóváhagyás: a Credential Manager + Supabase ID-token adapter kész; aktiváláshoz `VIZIT_GOOGLE_WEB_CLIENT_ID` és Supabase Google provider konfiguráció kell.
- Xiaomi developer hozzáférések egy része: store/gyártói teszt és esetleges Xiaomi szolgáltatások aktiválását érinti, az alap Android fejlesztést nem.
- Apple Developer hozzáférés: külön iOS kliens jelenleg nem cél; iOS-specifikus app/entitlement teszthez később kellhet.
- Egyértelműen elkülönített VIZIT DEV Supabase projekt vagy DEV database branch: a Dashboardban elérhető VIZIT projekt `main / Production`, ezért a migráció, Edge Function deploy és élő E2E szándékosan nem futott le rajta. A leállított másik projekt rendeltetése nincs DEV-ként igazolva.
- DEV Supabase publishable/service-role hozzáférés biztonságos futtatási környezetben: az automatizált távoli Auth/RLS/Storage/Delete E2E elkészült, de service-role secret nem kérhető chatben és nem kerülhet Gitbe.
- `vizit.hu` vagy végleges publikus profil domain DNS/hosting és `assetlinks.json`: Android App Link verifikációhoz szükséges.
- Végleges production application ID: store release előtt üzleti döntés és jóváhagyás szükséges.
- Production signing: keystore, SHA-256 fingerprint és CI secret nélkül release nem publikálható.
- Adatkezelési tájékoztató és ÁSZF végleges tartalma/URL-je: a kötelező regisztrációs elfogadás teljes E2E aktiválásához szükséges.
- Fizikai készülékpark: Xiaomi, Redmi, POCO, Samsung, Pixel és iPhone nélkül nincs `DEVICE TESTED` vagy `SUPPORTED` NFC/QR státusz.

Ezek egyike sem indok a lokális Android/NFC/QR fejlesztés leállítására.
