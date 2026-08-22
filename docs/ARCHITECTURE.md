# VIZIT v2 architektúra

## Alapelvek

- A GitHub repository az egyetlen source of truth.
- A Supabase a cloud source of truth; az Android kliens offline-first lokális cache-t tart.
- Az NFC és a Kontakt QR kizárólag lokális, utoljára sikeresen szinkronizált profilból is előállítható.
- A publikus profil platformfüggetlen HTTPS-erőforrás.
- Titkos vagy service-role credential nem kerülhet mobil buildbe.

## Célrétegek

| Réteg | Felelősség |
|---|---|
| `ui` | Compose képernyők, design system, navigáció, hozzáférhetőség |
| `presentation` | ViewModel, immutable UI state, egyszeri UI eventek |
| `domain` | profil-, Auth-, megosztási modellek és use case-ek |
| `data/local` | Room cache, outbox és DataStore beállítások |
| `data/remote` | Supabase Auth/PostgREST/Storage adapterek |
| `data/repository` | local-first olvasás, optimista mentés, szinkron és hibamodellezés |
| `nfc` | vCard, NDEF, Type 4 Tag APDU, HCE lifecycle és diagnosztika |
| `qr` | kontakt- és HTTPS-profil QR payloadok, determinisztikus renderelés |

Az első refaktor idején egyetlen Gradle `app` modul marad, de a csomaghatárok nem függhetnek visszafelé a UI-ra. Több modul csak akkor indokolt, ha a buildidő vagy a csapatméret ezt igazolja.

## Adatfolyam

1. A UI profilváltozást küld a ViewModelnek.
2. A repository azonnal tranzakcióban frissíti a Room cache-t és outbox-bejegyzést készít.
3. A UI Flow-ból az új lokális állapotot jeleníti meg.
4. A sync worker a módosítást Supabase-be küldi.
5. Siker esetén az outbox rekord lezárul; hiba esetén retry állapot és felhasználói visszajelzés készül.
6. NFC/QR mindig a lokális, konzisztens snapshotot olvassa, ezért offline is működik.

## Környezetek

| Flavor | Application ID | Név | Backend |
|---|---|---|---|
| `dev` | `hu.rayworks.vizit.dev` | VIZIT Dev | development Supabase |
| `beta` | `hu.rayworks.vizit.beta` | VIZIT Beta | staging Supabase |
| `prod` | `hu.rayworks.vizit` | VIZIT | production Supabase |

A konfiguráció Gradle propertyből vagy azonos nevű környezeti változóból érkezik. A kulcsneveket a `SUPABASE.md` sorolja fel.

## Megosztási szerződés

- Android elsődleges: `text/vcard` NDEF Type 4 Tag HCE-n.
- Platformfüggetlen: `https://vizit.hu/p/{slug}`.
- Kontakt QR: kép nélküli, méretkorlátos vCard.
- Profil QR: rövid HTTPS URL.
- iPhone: QR/HTTPS az elsődlegesen vállalható fallback; generikus NFC-vCard automatikus import nem ígérhető.
