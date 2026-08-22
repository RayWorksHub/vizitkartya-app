# Supabase

## Konfiguráció

A kliens csak publishable/anon kompatibilis kulcsot kaphat. Gradle property nevek:

- `VIZIT_DEV_SUPABASE_URL`, `VIZIT_DEV_SUPABASE_KEY`
- `VIZIT_BETA_SUPABASE_URL`, `VIZIT_BETA_SUPABASE_KEY`
- `VIZIT_PROD_SUPABASE_URL`, `VIZIT_PROD_SUPABASE_KEY`
- opcionális `VIZIT_PROFILE_HOST`

Hiányzó URL/kulcs esetén az adott buildben a Supabase feature flag automatikusan false.

## Séma

A migration kezeli a profilt, több kontaktot, címet, linket, profil/QR/NFC beállításokat, share eventeket, storage bucketeket és RLS-t.

Anon felhasználó nem kap közvetlen SELECT jogot a privát profil táblákra. A publikus profil kizárólag a `get_public_profile(slug)` RPC-n keresztül adja vissza a publikált profil és `is_public=true` mezők engedélyezett részét.

## Következő bekötés

Room cache → repository → Supabase PostgREST. Sikertelen sync esetén `pendingSync` lokálisan megmarad, a UI pedig nem állíthatja sikeresnek a felhőszinkront.
