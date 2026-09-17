# VIZIT iOS DEV 0.1

Natív SwiftUI iPhone/iPad fejlesztői előnézet, iOS/iPadOS 16.0-tól. A `develop` ág Android-kódjára épülő külön iOS könyvtár; az Android-appot és a backendet nem módosítja.

**Ez helyi funkciótesztre készült, nem teljes Android-port és nem kiadott TestFlight-verzió.**

## Kipróbálható funkciók

- Magyar, világos/sötét megjelenésű névjegyszerkesztés és profilkép-választás.
- Ellenőrzött, atomi helyi mentés; újraindítás után visszatöltés. Hibás/újabb mentést nem írunk felül.
- Kontakt QR fénykép nélkül, UTF-8/vCard 3.0 kódolással, méretkorláttal és csendes szegéllyel.
- Kamerás QR-beolvasás, kézi jóváhagyásos Kontaktokba mentés; HTTPS-link csak külön megerősítésre nyílik meg.
- Külön, tudatosan választott képes `.vcf` rendszermegosztás, például AirDroppal. Ez nem a fájlletöltés nélküli Kontakt QR folyamat.
- Helyi adatok törlése megerősítéssel. Sem regisztrációt, sem fizetős szolgáltatást nem kér az előnézet.

**Még nincs bekötve:** a meglévő fiókos belépés/regisztráció, Androiddal közös profil-szinkron, publikus profilkiadás, telefon–telefon NFC-küldés, NFC-kártyaírás és Wallet. Az UI ezeket nem mutatja működő funkciónak. Nincs kliensbe égetett API-kulcs vagy éles adatbázis-módosítás.

## Indítás Macen

Nyisd meg az `ios/VIZIT.xcodeproj` fájlt Xcode-ban. Válaszd a **VIZIT** sémát, majd egy iPhone vagy iPad szimulátort, és nyomd meg a Run gombot. Nincs CocoaPods-, XcodeGen- vagy külső Swift-csomag-függőség.

Saját iPhone/iPad esetén Xcode → Settings → Accounts alatt jelentkezz be az Apple-fiókodba. A VIZIT target **Signing & Capabilities → Team** mezőjében válaszd a csapatodat, maradjon automatikus az aláírás; szükség esetén válassz egyedi bundle identifier-t. Csatlakoztasd a készüléket, engedélyezd a Developer Mode-ot, válaszd ki futtatási célként, majd Run. Personal Teammel az aláírás időben korlátozott, rendszeres újratelepítés szükséges.

A szimulátoros `.app` **nem telepíthető iPhone-ra**. A CI fizikai készülékre is fordít, de aláírás nélkül; ezt nem nevezzük telepíthető kiadásnak.

## TestFlight

Ebben a változtatásban nem történik TestFlight-feltöltés. Ehhez Apple Developer Program tagság, App Store Connect alkalmazásrekord és csapathoz tartozó aláírás kell. A kiadási konfiguráció, végleges appikon és adatvédelmi/kiadási ellenőrzés külön feladat. Apple-jelszót, `.p12`, `.p8` vagy provisioning fájlt ne tölts fel a repóba és ne másolj beszélgetésbe.

## Ellenőrzés

```sh
swift test --package-path ios
xcodebuild -project ios/VIZIT.xcodeproj -scheme VIZIT \
  -destination 'platform=iOS Simulator,name=AZ_ELERHETO_SIMULATOR_NEVE' \
  CODE_SIGNING_ALLOWED=NO test
```

A GitHub **iOS native preview** workflow dinamikusan választ a telepített szimulátorok közül. A 23 hordozható teszt a validálást, vCard escape/fold szabályokat, QR méretkorlátot és a mentés sérülésvédelmét ellenőrzi. Három Apple-integrációs teszt vizsgálja a Contacts vCard-importot, a Vision QR-visszaolvasást és a natív kontaktmezőket. Három UI-teszt ellenőrzi a mentés/visszatöltés/QR folyamatot, a hibás mentést és a szerkesztés megszakítását. Külön iPad-indítási ellenőrzés és aláíratlan fizikai készülékes fordítás is szerepel benne.

A workflow eredménye a futás állapotából ellenőrizhető; a workflow puszta megléte nem bizonyít sikeres iOS-fordítást. Kamerát, fotóválasztást, AirDropot és két külön telefon közötti QR-felismerést valódi készüléken is tesztelni kell. Az elsődleges névjegy-QR nem tartalmaz fényképet.

Apple: [saját készülékes tesztelés és Personal Team](https://developer.apple.com/help/account/basics/about-your-developer-account), [bétatesztelés és kiadás](https://developer.apple.com/documentation/xcode/distributing-your-app-for-beta-testing-and-releases).
