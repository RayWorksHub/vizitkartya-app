# Supabase

## Környezeti konfiguráció

Az alábbi értékek Gradle propertyként vagy környezeti változóként adhatók át:

- `VIZIT_DEV_SUPABASE_URL`
- `VIZIT_DEV_SUPABASE_PUBLISHABLE_KEY`
- `VIZIT_BETA_SUPABASE_URL`
- `VIZIT_BETA_SUPABASE_PUBLISHABLE_KEY`
- `VIZIT_PROD_SUPABASE_URL`
- `VIZIT_PROD_SUPABASE_PUBLISHABLE_KEY`

Google Auth feature flagek:

- `VIZIT_DEV_GOOGLE_AUTH_ENABLED`
- `VIZIT_BETA_GOOGLE_AUTH_ENABLED`
- `VIZIT_PROD_GOOGLE_AUTH_ENABLED`

Service-role key mobil buildben és GitHub Actions kliens-build lépésben sem használható.

## Adatmodell

A migrationök a `supabase/migrations` alatt találhatók. A többértékű profiladatok külön táblákban vannak; a publikus olvasás nem közvetlen tábla-SELECT, hanem szűkített `get_public_profile(slug)` RPC.

## Storage

- `profile-assets`: privát eredeti/display képek és logók;
- `public-profile-assets`: kizárólag publikálásra szánt, származtatott képek.

A publikus bucketbe csak a felhasználó saját UUID mappájába lehet írni. A mobil kliens nem hozhat létre bucketet.

## RLS alapelv

- `authenticated`: csak saját `owner_id` sorok olvasása és módosítása;
- `anon`: nincs közvetlen profil-tábla olvasás;
- publikus adat: csak a whitelistes RPC eredménye;
- analitika: kliens csak minimális eseményt írhat, más felhasználó eseményét nem olvashatja.

## Aktiválás

1. DEV, BETA és PROD Supabase projektek létrehozása.
2. Migrationök alkalmazása Supabase CLI-val.
3. Publishable key és URL beállítása a megfelelő környezetben.
4. Auth redirect URL-ek és email sablonok beállítása.
5. Storage policy-k és RPC smoke test.
6. Csak ezután kapcsolható be a távoli adapter; üres konfiguráció nem használhat hamis production credentialt.
