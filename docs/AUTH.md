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
- fióktörlési klienshívás és szerveroldali `delete-account` Edge Function;
- DEV debug-only helyi profil fallback;
- Google Credential Manager adapter és Supabase ID-token bekötés.

## BLOCKED BY EXTERNAL ACCESS

A Google gomb csak akkor jelenik meg, ha a `VIZIT_GOOGLE_WEB_CLIENT_ID` property rendelkezésre áll. Hamis production credential nincs a repóban.

A Supabase Auth működéséhez az environment URL/publishable key, redirect URL-ek és a migration/function deploy szükséges. A fióktörlési Edge Function service role kulcsot kizárólag a Supabase szerveroldali environmentből olvas; mobilalkalmazásba nem kerül.

## Redirect scheme

- DEV: `vizit-dev://auth-callback`
- BETA: `vizit-beta://auth-callback`
- PROD: `vizit://auth-callback`

A kliens PKCE flow-t használ.
