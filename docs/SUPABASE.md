# Supabase

## Konfiguráció

A kliens csak publishable/anon kompatibilis kulcsot kaphat. Gradle property nevek:

- `VIZIT_DEV_SUPABASE_URL`, `VIZIT_DEV_SUPABASE_KEY`
- `VIZIT_BETA_SUPABASE_URL`, `VIZIT_BETA_SUPABASE_KEY`
- `VIZIT_PROD_SUPABASE_URL`, `VIZIT_PROD_SUPABASE_KEY`
- opcionális `VIZIT_PROFILE_HOST`
- `VIZIT_PRIVACY_POLICY_URL`, `VIZIT_PRIVACY_POLICY_VERSION`
- `VIZIT_TERMS_URL`, `VIZIT_TERMS_VERSION`

Hiányzó URL/kulcs esetén az adott buildben a Supabase feature flag automatikusan false.

## Séma és migrációs fegyelem

Az eredeti `20260822174500_vizit_v2_core.sql` migráció változatlan marad, mert távoli környezetben már alkalmazva lehet. A `20260822203000_vizit_v2_security_hardening.sql` előrefelé alkalmazható migráció adja hozzá a privacy/integrity javításokat és a verziózott jogi elfogadást. A `20260822220000_profile_snapshot_sync.sql` vezeti be az atomi, idempotens és verzióellenőrzött profil-snapshot szinkront. A séma kezeli a profilt, több kontaktot, címet, linket, profil/QR/NFC beállításokat, share eventeket, storage bucketeket és RLS-t.

Anon felhasználó nem kap közvetlen SELECT jogot a privát profil táblákra. A publikus profil kizárólag a `get_public_profile(slug)` RPC-n keresztül ad vissza adatot. A core profilmezőket a `profile_settings.field_visibility`, az ismétlődő kontakt/cím/link rekordokat a saját `is_public` mezőjük szűri. Az alapértelmezés minden új megosztható mezőnél privát.

A slug kisbetűs, URL-safe formátumhoz kötött és kis/nagybetűtől függetlenül egyedi. A security-definer függvények rögzített, üres `search_path` mellett futnak.

A hardening migráció privacy-safe átállásként a már létező kontakt/cím/link rekordok publikus flagjét is `false` értékre állítja. Publikálás után minden mezőt újra explicit engedélyezni kell.

## Offline-first profil RPC

- `get_my_profile_snapshot()`: csak a bejelentkezett tulajdonos teljes snapshotját adja vissza.
- `sync_profile_snapshot(operation_id, base_version, snapshot)`: egy adatbázis-tranzakcióban cseréli a profilaggregátumot.
- A `profile_sync_operations` azonos művelet ismételt hálózati elküldésekor ugyanazt az eredményt adja vissza.
- Eltérő szerververzió esetén `conflict` válasz és a szerver snapshotja érkezik; nincs last-write-wins felülírás.
- Az aggregátum közvetlen kliensoldali írási grantjai vissza vannak vonva; a tulajdonosi olvasás és a verzióellenőrzött RPC marad engedélyezett.

A migráció repositoryban elkészült, de távoli Supabase projektre nincs alkalmazva. A telepítés és az élő RLS/RPC E2E külső DEV hozzáférést igényel, ezért külön engedélyköteles.
