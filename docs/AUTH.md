# Auth

## IMPLEMENTED

- email + jelszó regisztráció;
- email-hitelesítési flow támogatása Supabase Auth deeplinkkel;
- bejelentkezés;
- session-visszaállítás `sessionStatus` alapján;
- kijelentkezés;
- jelszó-visszaállító e-mail;
- recovery deeplink és új jelszó mentése;
- magyar input- és hibaállapotok;
- regisztrációs jogi checkbox explicit hibaüzenettel;
- verziózott adatkezelési/ÁSZF metadata átadása a Supabase signupnak;
- fióktörlési klienshívás és szerveroldali `delete-account` Edge Function;
- DEV debug-only helyi profil fallback;
- Google Credential Manager adapter és Supabase ID-token bekötés.

## BLOCKED BY EXTERNAL ACCESS

A Google gomb jelenleg csak DEV debug buildben és csak akkor jelenik meg, ha a `VIZIT_GOOGLE_WEB_CLIENT_ID` property rendelkezésre áll. BETA/PROD buildben szándékosan tiltott, amíg az új OAuth-fiókok jogi elfogadási gate-je és szerveroldali naplózása el nem készül. Hamis production credential nincs a repóban.

A Supabase Auth működéséhez az environment URL/publishable key, redirect URL-ek és a migration/function deploy szükséges. A fióktörlési Edge Function service role kulcsot kizárólag a Supabase szerveroldali environmentből olvas; mobilalkalmazásba nem kerül.

A regisztráció csak akkor aktiválható, ha a következő négy Gradle property mind rendelkezésre áll, és mindkét dokumentum-URL érvényes HTTPS-cím:

- `VIZIT_PRIVACY_POLICY_URL`
- `VIZIT_PRIVACY_POLICY_VERSION`
- `VIZIT_TERMS_URL`
- `VIZIT_TERMS_VERSION`

A kliens a dokumentumverziókat Auth user metadata formájában küldi. A `bootstrap_vizit_user` trigger szerveridővel, csak olvasható `legal_acceptances` rekordot készít. A Google-belépés production aktiválása előtt külön ellenőrizni kell, hogy jogi elfogadás nélküli OAuth-user ne juthasson túl az onboarding gaten.

## Redirect scheme

- DEV: `vizit-dev://auth-callback`
- BETA: `vizit-beta://auth-callback`
- PROD: `vizit://auth-callback`

A kliens PKCE flow-t használ.
