# Auth

## Célállapot

- email+jelszó regisztráció és email-megerősítés;
- bejelentkezés, kijelentkezés, session-visszaállítás;
- elfelejtett jelszó és deeplinkes új jelszó;
- fióktörlés hitelesített Edge Functionön keresztül;
- Google Sign in külön, feature flagelt Credential Manager adapterrel.

## Állapotmodell

Minden művelet explicit `Idle`, `Loading`, `Success` vagy felhasználóbarát `Error` állapotot ad. Nyers Supabase/Ktor exception és stack trace nem kerül UI-ra.

## Google adapter

A Google ID tokent a Credential Manager adja, majd Supabase ugyanahhoz a felhasználói rendszerhez kapcsolja. A server client ID konfigurációból érkezik. Credential hiányában a gomb disabled/„hamarosan aktiválható”, és az emailes Auth ettől függetlenül működik.

Az Android és a webdomain közötti Digital Asset Links fájlhoz a DEV/BETA/PROD package-ek és signing fingerprintjeik külön bejegyzést igényelhetnek.

## DEV tesztprofil

Debug-only lokális profil kizárólag `devDebug` változatban engedélyezhető. A production source setből és release buildből kódszinten ki kell zárni; puszta UI-elrejtés nem elegendő.
