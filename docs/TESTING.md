# Tesztelés

## Automatizált kapuk

Minden PR-nél:

```bash
./gradlew testDevDebugUnitTest
./gradlew lintDevDebug
./gradlew assembleDevDebug assembleBetaDebug assembleProdDebug
```

Kötelező további tesztterületek: vCard escaping/folding, magyar karakterek, többértékű mezők, adaptív képbudget, NDEF, hibás és részleges APDU, repository/cache/outbox, Auth állapotok, QR determinisztikusság, App Link intent és Compose kritikus folyamatok.

## Fizikai NFC-menet

Minden párosításnál rögzítendő:

- küldő és fogadó pontos modell;
- HyperOS/Android/iOS verzió és patch szint;
- VIZIT build SHA és environment;
- NFC felismerés és megnyitott alkalmazás;
- közvetlen újkontakt-UI vagy fájl/böngésző fallback;
- összes szöveges mező, több telefon/email;
- magyar karakter, hosszú név és profilkép;
- offline küldés;
- ismételt olvasás, timeout és képernyőelhagyás;
- végső kompatibilitási státusz.

Fizikai mérés nélkül egy sor sem kaphat `SUPPORTED` minősítést.

## QR-menet

Xiaomi Camera, Google Lens, Samsung Camera és iPhone Camera szükséges. Külön kell mérni a Kontakt QR felismerését és a profil-URL megnyitását normál/gyenge fényben, kis/nagy kijelzőn.
