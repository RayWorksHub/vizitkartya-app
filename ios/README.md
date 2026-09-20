# VIZIT iOS 6.1

Natív SwiftUI iPhone/iPad kliens iOS/iPadOS 16.0-tól. A fejlesztői build az aktív VIZIT Supabase-projekthez csatlakozik, az Androidon elérhető névjegyfolyamatokat az iOS platformbiztonsági korlátai között valósítja meg.

## Funkciók

- E-mail/jelszó regisztráció kötelező e-mail-megerősítéssel, belépés és PKCE jelszó-visszaállítás.
- Google OAuth feature-gate mögött elérhető, de a jelenlegi aktív DEV Supabase-projektben a provider ki van kapcsolva, ezért a build nem mutat hozzá működést ígérő gombot.
- A Supabase-munkamenet kSecAttrAccessibleWhenUnlockedThisDeviceOnly kulcstár-elemként tárolódik.
- Felhasználónként elkülönített, atomi és teljes fájlvédelemmel mentett helyi profil; a könyvtár ki van zárva az eszközmentésből.
- Helyi-first profilmentés és optimista konkurenciavezérlés az adatbázis updated_at értékével. Ütközéskor a helyi példány nem íródik felül.
- Nyilvános profil engedélyezése, Kontakt QR, HTTPS-profil QR, képes .vcf-megosztás és manuálisan jóváhagyott Kontaktokba mentés.
- Kamerás QR-beolvasás; HTTPS-link csak felhasználói megerősítés után nyílik meg.
- Szerveroldali fióktörlés JWT-ellenőrzéssel, avatar-takarítással és az Auth-felhasználóhoz kapcsolt sorok kaszkádos törlésével.

Az iPhone nem tud Android HCE-szerű NFC-névjegyként működni. Ezt a kliens nem emulálja és nem jelzi támogatottként; iOS-en QR, AirDrop és HTTPS-profil használható.

## Konfiguráció

Az Xcode buildbe a következő build settingeknek kell bekerülniük; értéküket ne commitold:

- VIZIT_SUPABASE_URL
- VIZIT_SUPABASE_KEY (kliensoldali publishable/anon kulcs, service-role kulcs soha)
- VIZIT_AUTH_SCHEME (hu.rayworks.vizit.ios.dev.auth)
- VIZIT_GOOGLE_SIGN_IN_ENABLED (csak ténylegesen konfigurált provider esetén YES)
- VIZIT_PRIVACY_POLICY_URL, VIZIT_PRIVACY_POLICY_VERSION
- VIZIT_TERMS_URL, VIZIT_TERMS_VERSION
- VIZIT_PUBLIC_PROFILE_BASE_URL

A Supabase Swift SDK pontosan a 2.55.2 verzióra van rögzítve. Az OAuth és helyreállítási redirect: hu.rayworks.vizit.ios.dev.auth://auth-callback.

## Teszt és IPA

A GitHub **iOS secure IPA** workflow csak akkor tölti fel a VIZIT-iOS-DEV-unsigned-IPA artifactot, ha minden előfeltétel sikeres:

1. a szükséges konfiguráció és az élő Supabase Auth-policy ellenőrzése;
2. a hordozható Swift unit tesztek;
3. az iPhone-szimulátoros natív integrációs és UI-tesztek;
4. az iPad-indítási smoke teszt;
5. a fizikai iOS célra készülő Release build;
6. az ARM64/Mach-O platform, plist, privacy manifest, IPA ZIP és SHA-256 ellenőrzése.

Nincs continue-on-error; hibás teszt után nem készül IPA-artifact. A szimulátoros teszt nem bizonyítja a kamerát, fotóválasztást, AirDropot, OAuth rendszerböngészőt vagy a valódi iPhone-telepítést.

Az artifact aláíratlan IPA. Sideloadly/AltStore a saját Apple ID-val újra tudja aláírni, vagy Macen Xcode-ból telepíthető saját Team kiválasztásával. Ez nem App Store/TestFlight kiadás.

## TestFlight-kiadás

A `VIZIT_PHYSICAL_DEVICE_RELEASE_APPROVED=true` repository-változó és a
`[testflight]` kiadási commit együtt engedélyezi az aláírt App Store-archívum
ellenőrzését és TestFlight-feltöltését. A jelenlegi kiadás: **VIZIT 7.3.0**.

## Kötelező fizikai készülékteszt

Kiadásra jelölés előtt valódi iPhone-on külön ellenőrizendő:

- telepítés és első indítás;
- e-mailes regisztráció/megerősítés/belépés;
- Google OAuth és jelszó-visszaállítás;
- offline mentés, újracsatlakozás és két eszköz közti szinkronütközés;
- kameraengedély, Kontakt QR visszaolvasás, Kontaktokba mentés;
- fotóválasztás, .vcf és AirDrop;
- nyilvános profil megnyitása;
- csak erre létrehozott tesztfiókkal a végleges fióktörlés.

Sikert csak a ténylegesen lefutott, bizonyítékkal rendelkező ellenőrzés után szabad rögzíteni.
