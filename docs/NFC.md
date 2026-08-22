# NFC

## Vállalt működés

A küldő Xiaomi/Redmi/POCO telefon Android HCE-vel NFC Forum Type 4 Tagként viselkedik. A fogadó Android telefon az NDEF `text/vcard` rekordot olvassa. A fogadó VIZIT telepítése nem lehet követelmény.

A tényleges Kontaktok-import UI gyártó- és rendszerfüggő. A funkció csak fizikai készülékteszt után kaphat `SUPPORTED` állapotot.

## APDU-folyamat

1. NDEF Tag Application kiválasztása: `D2760000850101`.
2. Capability Container (`E103`) kiválasztása és olvasása.
3. NDEF file (`E104`) kiválasztása.
4. NLEN és az NDEF message részleges `READ BINARY` műveletekkel történő kiolvasása.
5. Csak az NDEF-fájl teljes lefedettsége után küldhető `NFC-adat kiolvasva` esemény.

Ez az esemény nem jelenti azt, hogy a felhasználó a fogadó készüléken megnyomta a végső Mentés gombot.

## Biztonsági lifecycle

- Payload csak explicit megosztási módban érhető el.
- Az aktív payload memóriabeli és rövid TTL-lel rendelkezik.
- Képernyőelhagyás, timeout, felhasználói leállítás vagy sikeres teljes olvasás deaktiválja.
- `android:requireDeviceUnlock="true"` kötelező marad.
- A vCard és a kép nem kerülhet diagnosztikai logba.

## Payload-budget

- A display kép nem kerül közvetlenül NFC-re.
- A kontakt-avatar külön, fokozatosan csökkentett JPEG.
- A teljes NDEF méretét kell mérni, nem csak a JPEG-et.
- Ha a kép nem fér el, kisebb dimenzió/minőség következik; végső fallbackként a vCard kép nélkül készül el.
- Túlméretes payload nem okozhat crasht.

A jelenlegi szoftveres védőkorlát 16 KiB. Ez nem végleges kompatibilitási ígéret; a készülékmátrix eredménye alapján lefelé módosítható.

Az Android HCE dokumentáció rövid APDU-folyamatot javasol, és körülbelül 1 KB-ot említ ésszerű felső becslésként gyors tranzakcióhoz. A képes vCard ennél nagyobb, ezért a tényleges elfogadható budgetet kizárólag készülékmátrix alapján lehet véglegesíteni.

## iPhone irány

Az iPhone Core NFC olvasási képessége nem jelent automatikus, app nélküli MIME-vCard importgaranciát. Háttérben a legstabilabb felismerhető tartalom a megfelelő HTTPS/universal-link NDEF rekord. Emiatt:

- Android-szerű automatikus NFC-vCard import: nem vállalt kompatibilitási szerződés;
- NFC HTTPS-link: külön tesztelendő fallback;
- QR HTTPS-link: kötelező stabil fallback;
- publikus profiloldalon rendszer által támogatott kontaktmentés: kötelező.

## Fizikai teszt előtt tiltott állítások

- „A kontakt sikeresen mentve.”
- „Minden Androidon működik.”
- „iPhone-on ugyanúgy működik.”
- „A profilkép minden fogadó Kontaktok alkalmazásába átkerül.”
