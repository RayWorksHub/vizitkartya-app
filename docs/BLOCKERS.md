# Külső blokkolók

## BLOCKED BY EXTERNAL ACCESS

- Google Auth DEV eszközteszt: a Credential Manager + Supabase ID-token adapter és a külső provider-konfiguráció kész; stabil DEV APK-aláírás és fizikai első/visszatérő belépési próba kell.
- Xiaomi developer hozzáférések egy része: store/gyártói teszt és esetleges Xiaomi szolgáltatások aktiválását érinti, az alap Android fejlesztést nem.
- Apple Developer hozzáférés: külön iOS kliens jelenleg nem cél; iOS-specifikus app/entitlement teszthez később kellhet.
- A `delete-account` Edge Function DEV deployja: a Beta-core útvonalat nem blokkolja, de a teljes fióktörlési E2E csak deploy után futtatható.
- `vizit.hu` vagy végleges publikus profil domain DNS/hosting és `assetlinks.json`: Android App Link verifikációhoz szükséges.
- Végleges production application ID: store release előtt üzleti döntés és jóváhagyás szükséges.
- Production signing: keystore, SHA-256 fingerprint és CI secret nélkül release nem publikálható.
- Adatkezelési tájékoztató és ÁSZF végleges tartalma/URL-je: a kötelező regisztrációs elfogadás teljes E2E aktiválásához szükséges.
- Fizikai készülékpark: Xiaomi, Redmi, POCO, Samsung, Pixel és iPhone nélkül nincs `DEVICE TESTED` vagy `SUPPORTED` NFC/QR státusz.

Ezek egyike sem indok a lokális Android/NFC/QR fejlesztés leállítására.
