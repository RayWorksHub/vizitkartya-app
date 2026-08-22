# Külső blokkolók

Ezek nem állítják le az Android kód fejlesztését, de a jelzett funkció production aktiválását blokkolják.

| Blokkoló | Érintett rész | Állapot | Aktiváláshoz szükséges |
|---|---|---|---|
| DEV/BETA/PROD Supabase projektadatok | távoli Auth, sync, Storage | `BLOCKED BY EXTERNAL ACCESS` | projekt URL-ek és publishable key-k, migration alkalmazás |
| Google OAuth jóváhagyás és web client ID | Google Sign in | `BLOCKED BY EXTERNAL ACCESS` | Google konfiguráció + Supabase provider setup |
| `vizit.hu` HTTPS profilhost | Profil QR, App Link, iPhone fallback | `BLOCKED BY EXTERNAL ACCESS` | DNS/deploy + `assetlinks.json` |
| Production signing | PROD release/App Link | `BLOCKED BY EXTERNAL ACCESS` | keystore, védett secret, SHA-256 fingerprint |
| Xiaomi developer hozzáférés | gyártói terjesztés | `BLOCKED BY EXTERNAL ACCESS` | elfogadott fejlesztői fiók |
| Fizikai készülékpark | NFC/QR kompatibilitás | `BLOCKED BY EXTERNAL ACCESS` | Xiaomi, Redmi, POCO, Samsung, Pixel és iPhone tesztkészülékek |
| Apple Developer hozzáférés | esetleges későbbi iOS-specifikus tesztapp | `BLOCKED BY EXTERNAL ACCESS` | csak akkor szükséges, ha Core NFC tesztkliens készül |

A release signing, service-role kulcs és Google secret nem helyettesíthető ideiglenes production értékkel.
