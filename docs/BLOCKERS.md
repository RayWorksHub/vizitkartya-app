# Külső blokkolók

## BLOCKED BY EXTERNAL ACCESS

- Google Auth DEV eszközteszt: a Credential Manager + Supabase ID-token adapter és a külső provider-konfiguráció kész; stabil DEV APK-aláírás és fizikai első/visszatérő belépési próba kell.
- Xiaomi developer hozzáférések egy része: store/gyártói teszt és esetleges Xiaomi szolgáltatások aktiválását érinti, az alap Android fejlesztést nem.
- TestFlight feldolgozás és telepítés: a `0.4.3 (34)` aláírt build feltöltése sikeres; a fizikai teszt csak akkor indulhat, amikor ez a build megjelent a tesztelőknél.
- A `delete-account` Edge Function DEV deployja: a Beta-core útvonalat nem blokkolja, de a teljes fióktörlési E2E csak deploy után futtatható.
- `vizit.hu` vagy végleges publikus profil domain DNS/hosting és `assetlinks.json`: Android App Link verifikációhoz szükséges.
- Végleges production application ID: store release előtt üzleti döntés és jóváhagyás szükséges.
- Production signing: keystore, SHA-256 fingerprint és CI secret nélkül release nem publikálható.
- Adatkezelési tájékoztató és ÁSZF végleges tartalma/URL-je: a kötelező regisztrációs elfogadás teljes E2E aktiválásához szükséges.
- Fizikai készülékpark: legalább két iPhone és egy Android nélkül nem zárható le az iPhone–iPhone, iPhone–Android, többkészülékes szinkron és valódi kontaktmentés.

Ezek egyike sem indok a lokális Android/NFC/QR fejlesztés leállítására.
