# VIZIT v2 architektúra

## Cél

A GitHub repository az egyetlen source of truth. A natív Android kliens offline-first módon működik: a felhasználói művelet először lokálisan jelenik meg, majd a repository réteg szinkronizál Supabase felé.

## Rétegek

- `ui`: Jetpack Compose képernyők és VIZIT Design System.
- `ViewModel`: UI state, lifecycle és felhasználói műveletek.
- `data/local`: Room cache és DataStore alkalmazásbeállítások.
- `data/remote`: Supabase kliens és verzióellenőrzött profil-snapshot adapter.
- `data/sync`: outbox modellek, konfliktuskezelés, retry policy és WorkManager orchestration.
- `nfc`: vCard, NDEF, Type 4 Tag és HCE.
- `qr`: kontakt-QR és HTTPS profil-QR.
- `supabase/migrations`: verziózott PostgreSQL/RLS/Storage séma.

## Profil adatút

1. A UI-validáció után a `ContactProfileRepository` először Room-tranzakcióban menti a profilt.
2. Ugyanebben a tranzakcióban felhasználónként egy összevont outbox snapshot készül.
3. A UI kizárólag a Room `Flow` állapotát figyeli; hálózati válaszra nem vár a helyi mentéshez.
4. A WorkManager hálózati constraint mellett futtatja a `ProfileSyncEngine`-t.
5. A Supabase RPC idempotens műveletazonosítót és `baseServerVersion` értéket ellenőriz.
6. Verzióütközéskor a helyi adat és a távoli snapshot is megmarad, az automatikus felülírás leáll.

A korábbi `vizit_profile` SharedPreferences tartalom egyszer, sikeres Room-mentés után migrálódik, majd törlődik. Production profiladatot a SharedPreferences már nem tárol.

A DataStore futásidőben kezeli az automatikus szinkron beállítását, az aktív profilazonosítót és a legacy migráció jelzőjét. Strukturált profiladat nem kerül DataStore-ba.

## Environment

- DEV: `hu.rayworks.vizit.dev`, `VIZIT Dev`.
- BETA: `hu.rayworks.vizit.beta`, `VIZIT Beta`.
- PROD: `hu.rayworks.vizit`, `VIZIT`.

A Supabase URL és publishable key Gradle propertyből érkezik. Service role key mobilkliensben tilos.
