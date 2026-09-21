# VIZIT iOS 8.7

Natív SwiftUI iPhone/iPad kliens iOS/iPadOS 16.0-tól. A fejlesztői build az aktív VIZIT Supabase-projekthez csatlakozik, az Androidon elérhető névjegyfolyamatokat az iOS platformbiztonsági korlátai között valósítja meg.

## Funkciók

- E-mail/jelszó regisztráció kötelező e-mail-megerősítéssel, belépés és PKCE jelszó-visszaállítás.
- Google OAuth feature-gate mögött elérhető, de a jelenlegi aktív DEV Supabase-projektben a provider ki van kapcsolva, ezért a build nem mutat hozzá működést ígérő gombot.
- A Supabase-munkamenet kSecAttrAccessibleWhenUnlockedThisDeviceOnly kulcstár-elemként tárolódik.
- Felhasználónként elkülönített, atomi és teljes fájlvédelemmel mentett helyi profil; a könyvtár ki van zárva az eszközmentésből.
- Helyi-first profilmentés és optimista konkurenciavezérlés az adatbázis updated_at értékével. Ütközéskor a helyi példány nem íródik felül.
- Nyilvános profil engedélyezése automatikus azonosítóval és opcionális, ellenőrzött saját domainnel.
- Egyetlen elsődleges QR: fényképet is tartalmazó vCard, amelyet az iPhone és az Android natív kontaktként tud megnyitni. A nyilvános profil külön másolható hivatkozás.
- Képes .vcf-megosztás és manuálisan jóváhagyott Kontaktokba mentés.
- LinkedIn-, Facebook-, Instagram-, TikTok- és YouTube-profilok ellenőrzött, szinkronizált és vCardba kerülő integrációja.
- Vállalkozói Portál VOSZ-forrásokkal, magyar videós edukációval, bemutató videóhívással, digitális állapotfelméréssel és konkrét szolgáltatási útmutatókkal.
- A kurzusok témákra, modulokra és videóleckékre tagolódnak; egy lecke csak legalább 90% tényleges lejátszás után lesz kész.
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

1. a rögzített Figma-források hashének és a teljes UI-bizonyítékjegyzéknek az ellenőrzése;
2. a szükséges konfiguráció, a Supabase-forrás és az élő Auth-policy ellenőrzése;
3. a hordozható Swift unit tesztek;
4. a rögzített iPhone-modellen futó natív integrációs és UI-tesztek, kihagyás nélkül;
5. minden névvel ellátott képernyőkép megléte az eredeti xcresultban, beleértve a három kártyaanyagot és a három elrendezést;
6. az iPad-indítási smoke teszt;
7. a fizikai iOS célra készülő Release build;
8. az ARM64/Mach-O platform, plist, privacy manifest, IPA ZIP és SHA-256 ellenőrzése.

Nincs continue-on-error; hibás teszt után nem készül IPA-artifact. A szimulátoros teszt nem bizonyítja a kamerát, fotóválasztást, AirDropot, OAuth rendszerböngészőt vagy a valódi iPhone-telepítést.

Az artifact aláíratlan IPA. Sideloadly/AltStore a saját Apple ID-val újra tudja aláírni, vagy Macen Xcode-ból telepíthető saját Team kiválasztásával. Ez nem App Store/TestFlight kiadás.

## TestFlight-kiadás

A commitüzenet és a korlátlan ideig érvényes repository-változó többé nem tud
TestFlight-feltöltést indítani. Feltöltés kizárólag az **iOS secure IPA** kézi
indításával, `upload_testflight` művelettel történhet. A forrás-, fizikai
készülékes és vizuális jóváhagyásnak ugyanarra a teljes commit SHA-ra kell
mutatnia; ezen felül a `testflight-production` GitHub Environment jóváhagyása és
az `UPLOAD VIZIT TO TESTFLIGHT` megerősítés is kötelező.

A release-only futás teljes, ideiglenes felhasználókat létrehozó és törlő DEV
Supabase E2E-t is futtat. Ehhez a `VIZIT_DEV_SUPABASE_SERVICE_ROLE_KEY` secret
szükséges; ez a kulcs nem kerül az alkalmazásba vagy az artifactokba.

A feltöltés után a workflow az App Store Connect API-ból visszaolvassa ugyanazt
a verziót és buildszámot, és csak `VALID` feldolgozási állapotnál sikeres. A
feltöltött buildhez nem rendel automatikusan tesztelőcsoportot. Meglévő build
állapota a read-only **TestFlight build status** workflow-val kérdezhető le.
A jelenlegi kiadás: **VIZIT 8.7.0**.

A Megosztás képernyő három külön, feliratozott módot ad. A **Kontakt** QR a
teljes névjegyet kép nélkül, offline importálható vCardként viszi át; a
**Fényképes** QR ugyanezt optimalizált profilképpel adja át; a **Profil** QR a
sikeresen szinkronizált HTTPS-profilcímet nyitja meg. A fényképes és profil mód
hibaállapota nem készít félrevezető kódot, a Kontakt mód azonban profilkép
nélkül is működik. Mindhárom kód H hibajavítással, négy modulnyi csendes zónával
és legfeljebb 14%-os, fix színű VIZIT-jellel készül.

## Kötelező fizikai készülékteszt

Kiadásra jelölés előtt valódi iPhone-on külön ellenőrizendő:

- telepítés és első indítás;
- e-mailes regisztráció/megerősítés/belépés;
- e-mailes belépés és jelszó-visszaállítás;
- offline mentés, újracsatlakozás és két eszköz közti szinkronütközés;
- kameraengedély, Kontakt/Fényképes/Profil QR visszaolvasás, Kontaktokba mentés;
- fotóválasztás, .vcf és AirDrop;
- nyilvános profil megnyitása;
- csak erre létrehozott tesztfiókkal a végleges fióktörlés.

Sikert csak a ténylegesen lefutott, bizonyítékkal rendelkező ellenőrzés után szabad rögzíteni.

A teljes kiadási kapu, a Figma-hivatkozások és a pontos kézi eljárás:
[`Release/README.md`](Release/README.md).
