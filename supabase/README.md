# VIZIT Supabase backend

Ez a mappa a backend SQL source of truth-ja. A migrationöket sorrendben kell alkalmazni külön DEV, BETA és PROD projektre.

## Alkalmazás

```bash
supabase link --project-ref <PROJECT_REF>
supabase db push
```

A project ref, access token, adatbázis-jelszó és service-role key nem kerülhet a repositoryba.

## Első migration tartalma

- Supabase Auth felhasználóhoz kapcsolt `user_accounts`;
- profil és külön profilbeállítások;
- több telefon, email, cím és link;
- mező láthatóság és sorrend;
- QR- és NFC-beállítások;
- privacy-conscious megosztási/analitikai események;
- minden privát táblán RLS;
- anon hozzáférés csak a szűkített `get_public_profile(slug)` RPC-n;
- privát és publikus képbucket külön policyval.

## Biztonsági smoke test

Aktiválás előtt külön ellenőrizendő:

1. egy hitelesített felhasználó nem tud másik felhasználó profiljára SELECT/UPDATE/DELETE műveletet végezni;
2. anon szerepkör nem tud közvetlenül profiltáblát olvasni;
3. az RPC csak `is_public=true` profilt és kizárólag engedélyezett mezőt ad vissza;
4. privát Storage objektum másik UUID mappájából nem olvasható;
5. publikus bucketbe csak a saját UUID mappába lehet írni;
6. service-role kulcs nincs Android buildben.
