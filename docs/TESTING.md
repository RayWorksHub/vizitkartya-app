# Tesztelés

## Automatikus

PR/branch CI: DEV unit test, DEV lint, DEV debug build, BETA debug build, PROD release compile.

A profil-adatréteg JVM tesztjei ellenőrzik a normalizált Room/payload mappinget, a lokális kép kizárását a felhő-outboxból, a stabil rekordazonosítókat, az exponenciális retry limitjét, az alkalmazott syncet, a hálózati retryt, a sessionhiányt, a konfliktusblokkolást és a tiszta cache pullját.

Az Auth JVM tesztek ellenőrzik:

- email/jelszó/regisztráció/jelszócsere validációt;
- explicit jogi és fióktörlési megerősítést;
- biztonságos Supabase hibatérképezést;
- lejárt callbacket;
- scheme/host/path/port/userinfo és méret szerinti deeplink-validációt.

Kötelező bővítendő területek: Room migráció eszköz/instrumentation teszt, Supabase RPC/RLS integrációs teszt, teljes többértékű profil mapping, App Link és Compose kritikus flow-k.

## Supabase adatbázisteszt

A `supabase/tests/auth_profile_rls_e2e.sql` pgTAP suite tranzakcióban, rollbackkel ellenőrzi a user bootstrapot, jogi gatet, pontos verzióelfogadást, profil pull/push engedélyezést, idempotens retryt, verziókonfliktust, cross-user RLS-t, közvetlen írás tiltását és a törölt user JWT-jét kizáró aktív-user ellenőrzést.

Lokális futtatás Supabase CLI + Docker környezetben:

```bash
supabase start
supabase test db
```

## Távoli Supabase DEV E2E

A `scripts/supabase-auth-e2e.mjs` kizárólag ellenőrzött DEV projekten futtatható. Ideiglenes, megerősített Auth-usereket és két teszt médiaobjektumot hoz létre, majd ellenőrzi a login, jogi gate, profil sync, idempotencia, konfliktus, RLS, rekurzív storage cleanup, Auth-törlés és adatbázis-kaszkád teljes útját. A `finally` ág takarítja a tesztadatot.

Szükséges környezeti változók:

- `VIZIT_E2E_ENVIRONMENT=DEV`
- `VIZIT_E2E_CONFIRM_MUTATION=CREATE_AND_DELETE_TEMP_USERS`
- `VIZIT_DEV_SUPABASE_URL`
- `VIZIT_DEV_SUPABASE_PROJECT_REF`
- `VIZIT_DEV_SUPABASE_KEY`
- `VIZIT_DEV_SUPABASE_SERVICE_ROLE_KEY`
- `VIZIT_PRIVACY_POLICY_VERSION`
- `VIZIT_TERMS_VERSION`
- opcionális biztonsági összevetéshez `VIZIT_PROD_SUPABASE_URL`

Futtatás:

```bash
node scripts/supabase-auth-e2e.mjs
```

A futtatás DEV külső adatot hoz létre és töröl, ezért minden alkalommal külön műveleti engedélyhez kötött. Production projekten tilos.

## Fizikai

NFC release minősítéshez fizikai eszköz kell. Az eredmény nem `SUPPORTED`, amíg nincs valódi készülékteszt. A mátrixban külön rögzítendő készülékmodell, HyperOS/Android/iOS verzió, kontaktimport, fotó, több mező, böngésző/fájlletöltés és fallback.

Az offline profilhoz külön ellenőrizendő: repülő módú szerkesztés és újraindítás, több egymás utáni szerkesztés outbox-coalescingje, hálózat-visszatérés, process kill alatti sync lease, kijelentkezett állapot, valamint két eszköz verziókonfliktusa. Élő Supabase vagy fizikai eszköz használata külön jóváhagyást igényel.
