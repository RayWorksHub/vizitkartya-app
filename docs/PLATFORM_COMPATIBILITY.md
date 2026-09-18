# Platform compatibility

Állapotjelölések: `IMPLEMENTED`, `TESTED`, `DEVICE TESTED`, `SUPPORTED`, `PARTIALLY SUPPORTED`, `FALLBACK REQUIRED`, `NOT SUPPORTED`, `BLOCKED BY EXTERNAL ACCESS`.

| Irány | Funkció | Jelenlegi állapot | Megjegyzés |
| --- | --- | --- | --- |
| Xiaomi → Android | Type 4 Tag / NDEF olvasás | IMPLEMENTED | Fizikai mátrix még szükséges. |
| Xiaomi → Android | `text/vcard` kontaktimport | PARTIALLY SUPPORTED | OEM kontaktkezelés eltérhet; készülékteszt nélkül nem minősíthető `SUPPORTED`-nak. |
| Xiaomi → Android | Profilkép vCardban | PARTIALLY SUPPORTED | Adaptív NFC-képoptimalizálás elkészült; fizikai validáció szükséges. |
| Xiaomi → iPhone | Automatikus Android-szerű vCard import | FALLBACK REQUIRED | Nem tekintjük garantált rendszerfolyamatnak. |
| Xiaomi → iPhone | NDEF HTTPS URI felismerés | FALLBACK REQUIRED | A kettős vCard + URI payload elkészült; fizikai Android-HCE → iPhone teszt szükséges. |
| Bármely → iPhone | Kontakt QR | IMPLEMENTED | QR-generálás elkészült; iPhone Camera fizikai teszt szükséges. |
| Bármely → iPhone | VIZIT profil QR | PARTIALLY SUPPORTED | QR-generálás kész; publikus HTTPS profiloldal még szükséges. |
| Bármely → Android | VIZIT profil QR / App Link | PARTIALLY SUPPORTED | QR és App Link intent elkészült; domain `assetlinks.json` és end-to-end teszt szükséges. |

## iPhone technikai döntés

Az iPhone XS és újabb modelleken az Apple dokumentált background tag reading NDEF URI rekordot keres, és HTTPS linknél app nélkül Safarit nyit. Emiatt az Android HCE payloadban a vCard mellett HTTPS URI rekord van. Arbitrary `text/vcard` MIME rekord automatikus háttérfeldolgozására nem építünk termékígéretet.

## Következő fizikai mátrix

Xiaomi → Xiaomi, Redmi, POCO, Samsung, Pixel, majd Xiaomi → iPhone. Minden tesztnél: NFC felismerés, MIME/URI, Kontaktok megnyitása, mezők, fotó, magyar karakter, több telefon/email, fájlletöltés/böngésző, VIZIT-telepítési igény és fallback.
