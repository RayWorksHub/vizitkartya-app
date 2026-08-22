# Külső blokkolók

## BLOCKED BY EXTERNAL ACCESS

- Google Auth végleges developer/credential jóváhagyás: Google Sign-In aktiválásához szükséges, a kódstruktúrát nem blokkolja.
- Xiaomi developer hozzáférések egy része: store/gyártói teszt és esetleges Xiaomi szolgáltatások aktiválását érinti, az alap Android fejlesztést nem.
- Apple Developer hozzáférés: külön iOS kliens jelenleg nem cél; iOS-specifikus app/entitlement teszthez később kellhet.
- Távoli Supabase projekt és environment credentialek: a migration és kliensoldali konfiguráció elkészült; tényleges backend deployhoz projekt-hozzáférés szükséges.
- `vizit.hu` vagy végleges publikus profil domain DNS/hosting és `assetlinks.json`: Android App Link verifikációhoz szükséges.

Ezek egyike sem indok a lokális Android/NFC/QR fejlesztés leállítására.
