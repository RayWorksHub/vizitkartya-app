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

Az eredeti `20260822174500_vizit_v2_core.sql` migráció változatlan marad, mert távoli környezetben már alkalmazva lehet. A további migrációk kizárólag előrefelé alkalmazhatók:

- `20260822203000_vizit_v2_security_hardening.sql`: privacy/integrity és verziózott jogi elfogadás;
- `20260822220000_profile_snapshot_sync.sql`: atomi, idempotens és verzióellenőrzött profil-snapshot sync;
- `20260822234500_auth_legal_hardening.sql`: Auth utáni jogi gate, jogi RPC-k, profil-RPC kapu és törölt Auth-usert kizáró storage policy.

A séma kezeli a profilt, több kontaktot, címet, linket, profil/QR/NFC beállításokat, share eventeket, storage bucketeket és RLS-t.

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

## Auth és jogi RPC

- `has_legal_acceptance(privacy_version, terms_version)`: csak a saját, pontos verziójú elfogadást ellenőrzi;
- `accept_legal_documents(privacy_version, terms_version)`: hitelesített userhez, szerveridővel rögzít;
- `get_my_profile_snapshot()` és `sync_profile_snapshot(...)`: jogi elfogadás nélkül `42501` hibával leáll;
- `is_active_vizit_user()`: a storage owner policy számára ellenőrzi, hogy a JWT subject még létezik-e az Authban.

## DEV telepítési kapu

Távoli migráció vagy E2E csak olyan projektben futhat, amelyet a Dashboard egyértelműen VIZIT DEV-ként azonosít. A jelenleg elérhető VIZIT Supabase projekt `main / Production` jelölésű, a másik projekt pedig leállított és nem azonosítható VIZIT DEV-ként; ezért egyikben sem történt módosítás.

A távoli HTTP E2E további védelmei:

- kötelező `VIZIT_E2E_ENVIRONMENT=DEV`;
- a megadott projekt-refnek pontosan egyeznie kell a Supabase hosttal;
- azonos DEV/PROD URL esetén azonnal leáll;
- külön `CREATE_AND_DELETE_TEMP_USERS` megerősítés nélkül nem indul;
- a service-role kulcsot nem írja ki és nem menti.
