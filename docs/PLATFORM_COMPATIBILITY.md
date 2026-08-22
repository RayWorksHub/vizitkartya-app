# Platform compatibility

Állapotjelölések: `IMPLEMENTED`, `TESTED`, `DEVICE TESTED`, `SUPPORTED`, `PARTIALLY SUPPORTED`, `FALLBACK REQUIRED`, `NOT SUPPORTED`, `BLOCKED BY EXTERNAL ACCESS`.

| Irány | Funkció | Jelenlegi állapot | Megjegyzés |
| --- | --- | --- | --- |
| Xiaomi → Android | Type 4 Tag / NDEF olvasás | IMPLEMENTED | Fizikai mátrix még szükséges. |
| Xiaomi → Android | `text/vcard` kontaktimport | PARTIALLY SUPPORTED | OEM kontaktkezelés eltérhet; készülékteszt nélkül nem minősíthető `SUPPORTED`-nak. |
| Xiaomi → Android | Profilkép vCardban | PARTIALLY SUPPORTED | Payload-optimalizálás és fizikai validáció szükséges. |
| Xiaomi → iPhone | Automatikus Android-szerű vCard import | FALLBACK REQUIRED | Nem tekintjük garantált rendszerfolyamatnak. |
| Xiaomi → iPhone | NDEF HTTPS URI felismerés | FALLBACK REQUIRED | Apple dokumentáció alapján ez a megfelelő interoperabilitási irány; fizikai Android-HCE → iPhone teszt szükséges. |
| Bármely → iPhone | VIZIT profil QR | FALLBACK REQUIRED | A stabil Safari/HTTPS fallback implementációja a QR fázis része. |
| Bármely → Android | VIZIT profil QR / App Link | PARTIALLY SUPPORTED | App Link intent elkészült; QR UI és domain `assetlinks.json` még szükséges. |

## iPhone technikai döntés

Az iPhone XS és újabb modelleken az Apple dokumentált background tag reading NDEF URI rekordot keres, és HTTPS linknél app nélkül Safarit nyit. Emiatt az Android HCE payloadban a vCard mellett HTTPS URI rekord lesz. Arbitrary `text/vcard` MIME rekord automatikus háttérfeldolgozására nem építünk termékígéretet.

## Következő fizikai mátrix

Xiaomi → Xiaomi, Redmi, POCO, Samsung, Pixel, majd Xiaomi → iPhone. Minden tesztnél: NFC felismerés, MIME/URI, Kontaktok megnyitása, mezők, fotó, magyar karakter, több telefon/email, fájlletöltés/böngésző, VIZIT-telepítési igény és fallback.
