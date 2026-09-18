# NFC

## Android küldő

A VIZIT megtartja a meglévő `HostApduService` + NFC Forum Type 4 Tag + NDEF megoldást. A szabványos NDEF application AID: `D2760000850101`.

Elsődleges rekord Android fogadóhoz: egyetlen `text/vcard` NDEF rekord, vCard 3.0. A publikus HTTPS profil URL-je engedélyezett esetben a vCard `URL;TYPE=VIZIT` mezőjébe kerül. Külön URI rekord nem keverhető automatikusan a kontakt-payload mellé, mert a fogadó OEM kiválaszthatja a webes műveletet a kontaktimport helyett.

Az iPhone HTTPS NFC-fallback később külön, explicit átadási módként vizsgálandó; a stabil iPhone fallback jelenleg a QR + HTTPS profil.

## Biztonság

A payload csak explicit NFC megosztási módban hozzáférhető. A vCard és a profilkép process-local memóriában marad, SharedPreferences-be vagy fájlba nem kerül. Minden aktiválás külön session ID-t kap; egy régi timeout vagy APDU-kapcsolat nem deaktiválhat újabb átadást. `requireDeviceUnlock=true` marad. Kilépés, leállítás, timeout és megfelelő lifecycle esemény deaktiválja a payloadot.

Az aktív képernyő a VIZIT `HostApduService` komponenst foreground preferred HCE service-ként állítja be, majd kilépéskor visszaállítja a rendszerállapotot.

## Sikerállapot

A kapcsolat létrejötte önmagában nem siker. A service csak akkor jelez `NFC adat kiolvasva` állapotot, ha az NDEF fájl teljes tartományát kiolvasta a reader. Ez nem bizonyítja, hogy a fogadó felhasználó a kontaktot el is mentette.

## Fotó

A display kép és NFC kontaktkép külön cél. Az NFC avatar agresszíven csökkentendő, és a teljes NDEF 16 KiB gyakorlati budgetje alapján kell tovább tömöríteni vagy végső esetben elhagyni. A limit csak konzervatív kiindulás; a végleges értéket fizikai kompatibilitási teszt alapján kell rögzíteni.

## Eszközteszt

Unit teszt nem elég. Xiaomi→Xiaomi/Redmi/POCO/Samsung/Pixel fizikai teszt kötelező. Android→iPhone esetén a vCard MIME import és a HTTPS URI fallback külön mérendő.
