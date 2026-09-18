# Offline profil és szinkron

## Garanciák

- A profilmentés először és atomi Room-tranzakcióban történik.
- Felhőhiba vagy alkalmazásleállás nem törli a helyi változtatást.
- Egy felhasználó várakozó profilváltozásai egyetlen, mindig a legfrissebb snapshotot tartalmazó outbox sorba olvadnak.
- A lokális profilkép-preview nem kerül a JSON outboxba; a médiafeltöltés külön fejlesztési blokk.
- Azonos `operationId` újraküldése idempotens.
- Eltérő `baseServerVersion` konfliktust eredményez, nem csendes felülírást.

## Állapotok

| Állapot | Jelentés | Következő lépés |
|---|---|---|
| `LOCAL_ONLY` | DEV helyi profil vagy még nem szinkronizált cache | Nincs automatikus felhőírás |
| `PENDING` | Helyi mentés kész, outbox vár | WorkManager indítása |
| `SYNCING` | A worker lease alatt küldi a snapshotot | Siker, retry vagy conflict |
| `RETRY_SCHEDULED` | Átmeneti hálózati/session hiba | Exponenciális backoff vagy kézi retry |
| `CONFLICT` | A szerververzió közben megváltozott | Helyi és távoli változat megőrzése; feloldó UX szükséges |
| `SYNCED` | A Room cache a visszaigazolt szerver snapshottal egyezik | Későbbi szerverfrissítéskor pull |

## Retry és crash recovery

- WorkManager: `NetworkType.CONNECTED`, egyedi `vizit-profile-sync` munka, exponenciális backoff.
- Outbox: 30 másodpercről induló, legfeljebb 6 órás exponenciális késleltetés.
- A `SYNCING` sor 5 perces lease-t kap. Folyamatleállás után a lejárt lease újra feldolgozható.
- A worker nem naplóz nyers Supabase hibát vagy személyes adatot a felhasználói állapotba.

## Konfliktusstratégia

A kliens a legutóbb visszaigazolt `serverVersion` értékkel ír. A Supabase RPC zárolja a profilt, majd csak egyező verziónál alkalmazza az egész aggregátumot. Eltéréskor:

1. a helyi outbox változat változatlan marad;
2. a távoli snapshot külön konfliktusmezőben megmarad;
3. az automatikus retry leáll;
4. a Beállítások képernyő egyértelmű konfliktusállapotot mutat.

A mezőszintű feloldó UI a teljes többértékű profilmodell blokkjában készül el. Addig a rendszer biztonságosan blokkol, ezért nem történhet csendes adatvesztés.

## Külső ellenőrzés

A repository-szintű unit tesztek nem igényelnek credentialt. A Supabase migráció alkalmazása, az RLS/RPC integrációs teszt és a tényleges offline→online eszközteszt csak külön jóváhagyott DEV környezetben végezhető el.
