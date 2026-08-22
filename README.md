# VIZIT Android

A VIZIT natív Android/Xiaomi alkalmazása. Az első kiadás célja, hogy a felhasználó a saját telefonját NFC-forrásként használva adhassa át a névjegyét egy másik Android telefonnak.

## Jelenlegi állapot

- natív Kotlin és Jetpack Compose felület;
- helyben tárolt, szerkeszthető kontaktprofil;
- automatikusan négyzetesre vágott és tömörített profilkép;
- vCard 3.0 generálás profilképpel;
- NFC Forum Type 4 Tag / NDEF HCE prototípus;
- az NFC-adat csak aktív küldés közben érhető el;
- világos, sötét és Android dinamikus színtéma;
- JVM egységtesztek az NFC APDU-folyamathoz és a vCardhoz.

## NFC működés

A küldő telefon a szabványos NDEF alkalmazás-AID-t (`D2760000850101`) emulálja, és egy `text/vcard` rekordot szolgál ki. A fogadó készülékre nem kell telepíteni a VIZIT alkalmazást.

Az Android gyártói NFC-kezelése eltérhet. Az automatikus Kontaktok-megnyitást és a profilkép átvitelét ezért valódi Xiaomi/HyperOS és célkészülékeken kell kompatibilitási mátrixban ellenőrizni.

## Fejlesztői indítás

1. Nyisd meg a projektet a legfrissebb stabil Android Studióban.
2. Telepítsd az Android 16 / API 36 SDK-t.
3. Indítsd az `app` konfigurációt egy NFC/HCE-képes Android 10+ készüléken.

Parancssori ellenőrzés:

```bash
./gradlew testDebugUnitTest assembleDebug
```

## Következő fejlesztési blokkok

1. Xiaomi/HyperOS NFC készülékteszt és APDU naplózás.
2. QR-alapú átadás ugyanebből a profilból.
3. backend szinkron és Google-belépés.
4. béta/production build változatok és aláírás.
