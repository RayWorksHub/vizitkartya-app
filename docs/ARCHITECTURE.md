# VIZIT v2 architektúra

## Cél

A GitHub repository az egyetlen source of truth. A natív Android kliens offline-first módon működik: a felhasználói művelet először lokálisan jelenik meg, majd a repository réteg szinkronizál Supabase felé.

## Rétegek

- `ui`: Jetpack Compose képernyők és VIZIT Design System.
- `ViewModel`: UI state, lifecycle és felhasználói műveletek.
- `data/local`: Room cache és DataStore alkalmazásbeállítások.
- `data/remote`: Supabase kliens és későbbi remote data source-ok.
- `nfc`: vCard, NDEF, Type 4 Tag és HCE.
- `qr`: kontakt-QR és HTTPS profil-QR.
- `supabase/migrations`: verziózott PostgreSQL/RLS/Storage séma.

A jelenlegi `ContactProfileRepository` SharedPreferences tároló átmeneti kompatibilitási réteg. Production source of truthként ki lesz vezetve, amint a Room + Supabase repository bekötése elkészül.

## Environment

- DEV: `hu.rayworks.vizit.dev`, `VIZIT Dev`.
- BETA: `hu.rayworks.vizit.beta`, `VIZIT Beta`.
- PROD: `hu.rayworks.vizit`, `VIZIT`.

A Supabase URL és publishable key Gradle propertyből érkezik. Service role key mobilkliensben tilos.
