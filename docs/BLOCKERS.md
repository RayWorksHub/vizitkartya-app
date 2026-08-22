# Külső blokkolók

## BLOCKED BY EXTERNAL ACCESS

- Google Auth végleges developer/credential jóváhagyás: a Credential Manager + Supabase ID-token adapter kész; aktiváláshoz `VIZIT_GOOGLE_WEB_CLIENT_ID` és Supabase Google provider konfiguráció kell.
- Xiaomi developer hozzáférések egy része: store/gyártói teszt és esetleges Xiaomi szolgáltatások aktiválását érinti, az alap Android fejlesztést nem.
- Apple Developer hozzáférés: külön iOS kliens jelenleg nem cél; iOS-specifikus app/entitlement teszthez később kellhet.
- Távoli Supabase projekt és environment credentialek: migration, Auth kliens és `delete-account` Edge Function elkészült; tényleges deployhoz projekt-hozzáférés szükséges.
- `vizit.hu` vagy végleges publikus profil domain DNS/hosting és `assetlinks.json`: Android App Link verifikációhoz szükséges.

Ezek egyike sem indok a lokális Android/NFC/QR fejlesztés leállítására.
