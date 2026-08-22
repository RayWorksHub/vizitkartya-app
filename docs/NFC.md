# NFC

## Android küldő

A VIZIT megtartja a meglévő `HostApduService` + NFC Forum Type 4 Tag + NDEF megoldást. A szabványos NDEF application AID: `D2760000850101`.

Elsődleges rekord Android fogadóhoz: `text/vcard`, vCard 3.0. Másodlagos rekord: HTTPS VIZIT profil URI. A másodlagos URI azért szükséges, mert iPhone háttér-NFC feldolgozásnál a dokumentált automatikus út URI rekordra épül.

## Biztonság

A payload csak explicit NFC megosztási módban legyen hozzáférhető. `requireDeviceUnlock=true` marad. Kilépés, leállítás, timeout és megfelelő lifecycle esemény deaktiválja a payloadot.

## Sikerállapot

A kapcsolat létrejötte önmagában nem siker. A service csak akkor jelez `NFC adat kiolvasva` állapotot, ha az NDEF fájl teljes tartományát kiolvasta a reader. Ez nem bizonyítja, hogy a fogadó felhasználó a kontaktot el is mentette.

## Fotó

A display kép és NFC kontaktkép külön cél. Az NFC avatar agresszíven csökkentendő, és a teljes NDEF mérete alapján kell tovább tömöríteni vagy végső esetben elhagyni. A profil URL ettől függetlenül megmarad.

## Eszközteszt

Unit teszt nem elég. Xiaomi→Xiaomi/Redmi/POCO/Samsung/Pixel fizikai teszt kötelező. Android→iPhone esetén a vCard MIME import és a HTTPS URI fallback külön mérendő.
