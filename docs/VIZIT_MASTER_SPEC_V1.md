# VIZIT MASTER DEVELOPMENT SPEC v1.0

## 0. A dokumentum státusza

Ez a dokumentum a **VIZIT v2 fejlesztésének master specifikációja**.

A fejlesztés során ezt kell elsődleges követelményforrásként használni.

Ha a jelenlegi kód, README, korábbi proof-of-concept vagy bármely régebbi implementáció eltér ettől a dokumentumtól, akkor:

**ez a specifikáció az irányadó.**

A jelenlegi alkalmazás nem tekintendő kész terméknek.

A jelenlegi repositoryban található megoldások:

- megtarthatók;
- refaktorálhatók;
- továbbfejleszthetők;

ha megfelelnek ennek a specifikációnak.

Hibás vagy ideiglenes megoldást csak azért ne tarts meg, mert már létezik.

---

# 1. Termék

Terméknév:

**VIZIT**

Szlogen:

**EGY ÉRINTÉS. EGY KAPCSOLAT.**

A VIZIT egy digitális névjegy- és kontaktmegosztó rendszer.

Elsődleges termékélmény:

> A felhasználó néhány másodperc alatt át tudja adni saját kapcsolati adatait egy másik ember telefonjára NFC-vel vagy QR-kóddal.

A megosztási folyamat legyen:

- gyors;
- egyszerű;
- természetes;
- lehetőleg alkalmazástelepítés nélkül használható a fogadó oldalon;
- fájlkezelő nélküli;
- technikai ismeretek nélkül használható.

---

# 2. Jelenlegi fejlesztési fókusz

Első körben kizárólag a következő kliens fejlesztése prioritás:

## Xiaomi / HyperOS Android

Elsődlegesen:

- Xiaomi;
- Redmi;
- POCO;

készülékeken.

A kliens azonban szabványos Android technológiára épüljön, ezért ahol lehetséges, működjön:

- Samsung;
- Google Pixel;
- más Android gyártók

készülékein is.

---

# 3. Jelenleg NEM készül teljes kliensként

Most ne készüljön:

- teljes értékű iOS alkalmazás;
- iPad alkalmazás;
- desktop alkalmazás;
- teljes webes VIZIT alkalmazás;
- NFC plasztikkártya-kezelő rendszer;
- marketplace;
- felesleges közösségi hálózati funkció.

Később ezek hozzáadhatók.

A backend és az adatmodell ezért már most legyen platformfüggetlen.

---

# 4. iPhone kompatibilitás

Bár iOS kliens most nem készül, a megosztási rendszer legyen használható iPhone fogadó készülék felé is, amennyire az Apple platform lehetővé teszi.

Ne feltételezz Android HCE-vel azonos iPhone működést.

Vizsgálandó:

- Android → iPhone NFC NDEF felismerés;
- NFC HTTPS URL felismerés;
- QR Kontakt felismerés;
- QR profil-link;
- Safari publikus profil;
- rendszeroldali kontaktmentés.

Ha valamelyik NFC-folyamat iPhone-on nem megbízható:

**QR + HTTPS profil legyen a stabil fallback.**

---

# 5. Jelenlegi repository

Repository:

`RayWorksHub/vizitkartya-app`

A jelenlegi repository már tartalmaz proof-of-concept szinten:

- natív Kotlin Android projektet;
- Jetpack Compose UI-t;
- profil szerkesztést;
- lokális profilmentést;
- profilképet;
- vCard-generálást;
- NFC Host Card Emulation prototípust;
- NFC Forum Type 4 Tag logikát;
- NDEF payloadot;
- néhány unit tesztet.

Ezt először auditálni kell.

---

# 6. Kötelező első művelet: repository audit

Fejlesztés előtt készíts teljes gap analysist:

## Ellenőrizd

- jelenlegi projektstruktúra;
- Gradle;
- Kotlin;
- Compose;
- SDK verziók;
- dependency-k;
- manifest;
- NFC HCE implementáció;
- APDU implementáció;
- NDEF implementáció;
- vCard;
- profilmodell;
- képfeldolgozás;
- UI;
- navigáció;
- unit tesztek;
- GitHub Actions;
- security;
- buildelhetőség.

Minden meglévő funkció kapjon állapotot:

- `IMPLEMENTED`
- `TESTED`
- `DEVICE TESTED`
- `PARTIALLY SUPPORTED`
- `REQUIRES REFACTOR`
- `BLOCKED`

Az audit után haladj tovább automatikusan.

Ne várj külön engedélyre minden apró javításnál.

---

# 7. Projektarchitektúra

A projektet úgy szervezd át, hogy most Android készüljön, de később bővíthető legyen.

Célstruktúra:

```text
VIZIT/
│
├── apps/
│   └── android/
│
├── supabase/
│   ├── migrations/
│   ├── functions/
│   ├── seed/
│   └── config/
│
├── shared/
│   ├── contracts/
│   └── schemas/
│
├── docs/
│
└── README.md

```

Később:

```text
apps/
├── android/
├── ios/
└── web/

```

A mostani fejlesztés azonban az:

`apps/android`

kliensre koncentráljon.

---

# 8. Technológiai stack

## Android

- Kotlin
- Jetpack Compose
- Material 3
- Android Studio
- Gradle
- compileSdk 36
- targetSdk 36
- minSdk API 29, hacsak az audit nem indokol mást
- Coroutines
- Flow
- ViewModel
- repository architecture
- use-case/service réteg, ahol indokolt
- dependency injection, ahol valóban hasznos
- Room
- DataStore
- WorkManager
- Supabase Kotlin

---

# 9. Backend

Készíts teljesen új:

**VIZIT Supabase projektet.**

Ne használd production backendként a régi webes alkalmazás korábbi adatrétegét.

Használandó:

- PostgreSQL
- Supabase Auth
- Supabase Storage
- Row Level Security
- SQL migrations
- Edge Functions, ahol indokolt

---

# 10. Source of truth

## Kód

GitHub.

## Felhasználói cloud adatok

Supabase.

## Offline állapot

Android lokális cache.

## Brand

A specifikációhoz csatolt hivatalos VIZIT logófájl.

---

# 11. Git workflow

Hozd létre:

`develop`

ágat.

Napi fejlesztés:

`feature/*`

→ `develop`

→ teszt

→ `main`

A `main` csak ellenőrzött állapotot tartalmazzon.

---

# 12. Verziózás

Semantic Versioning.

Példák:

```text
0.2.0-alpha.1
0.4.0-alpha.2
0.5.0-beta.1
0.9.0-rc.1
1.0.0

```

Git tagekkel együtt.

---

# 13. Build environmentek

Három elkülönített környezet szükséges.

## DEV

App:

`VIZIT Dev`

Külön application ID suffix.

Development backend.

Részletes diagnosztika.

---

## BETA

App:

`VIZIT Beta`

Külön application ID.

Staging/testing környezet.

Fizikai eszközteszt.

---

## PROD

App:

`VIZIT`

Production backend.

Production signing.

Debug funkciók nélkül.

---

# 14. Application ID

A jelenlegi:

`hu.rayworks.vizit`

azonosítót ne tekintsd automatikusan véglegesnek.

Mielőtt publikus Google Play vagy Xiaomi GetApps release készül:

**a production application ID-t véglegesen rögzíteni kell.**

Ezt külön blocker/state formájában dokumentáld, ha még nincs jóváhagyva.

---

# 15. Hivatalos VIZIT logó

A jelen specifikációhoz csatolt kép:

**a VIZIT hivatalos alkalmazás- és márkalogója.**

A grafika tartalmazza:

- stilizált V jelet;
- NFC hullámokat;
- VIZIT feliratot;
- „EGY ÉRINTÉS. EGY KAPCSOLAT.” szlogent.

Ez a brand source of truth.

---

# 16. A VIZIT logó használata

Használd megfelelő változatban:

- launcher icon;
- splash screen;
- login;
- registration;
- onboarding;
- About;
- VIZIT fejléc;
- QR sharing;
- publikus profil;
- Play Store;
- Xiaomi GetApps;
- notificationhoz külön megfelelő monochrome asset.

---

# 17. App icon

Launcher iconként ne a teljes feliratos logót zsugorítsd össze.

Használd:

**V + NFC hullámok**

brand markot.

Készíts:

- adaptive foreground;
- adaptive background;
- round icon;
- monochrome icon;
- themed icon kompatibilitást.

A hivatalos logót ne tervezz át önkényesen.

---

# 18. VIZIT brand vs felhasználói céges logó

Két teljesen külön fogalom.

## VIZIT brand logo

Az alkalmazás logója.

Nem felhasználói adat.

Nem cserélhető.

Nem kerül profiladatként Supabase-be.

---

## Company Logo

A felhasználó opcionális saját:

- céges;
- vállalkozási;
- szervezeti

logója.

Ez profiladat.

Soha nem helyettesítheti az alkalmazás VIZIT logóját.

Kódban is különítsd el.

Használj például:

`VizitBrandMark`

`VizitBrandLogo`

`VizitAppIcon`

és:

`CompanyLogo`

`ProfileCompanyLogo`

neveket.

---

# 19. VIZIT Design System

Készíts saját Compose design systemet.

A brand színvilágát a hivatalos VIZIT grafikából vezesd le.

Legyen konzisztens:

- primary;
- secondary;
- accent;
- surface;
- background;
- success;
- warning;
- error.

A VIZIT vizuális identitását ne írja felül kontrollálatlanul az Android Dynamic Color.

Dynamic Color csak akkor használható, ha nem rontja a brand következetességét.

---

# 20. Splash és app indulás

Kötelező app start flow:

```text
Splash
↓
App inicializálás
↓
Session ellenőrzés
↓
┌────────────────┬────────────────┐
│ nincs session  │ van session    │
↓                ↓
Auth             Kezdőlap

```

DEV buildben megengedett debug profil.

PROD buildben nem.

---

# 21. Auth nyitóképernyő

Nem szabad közvetlenül a fő alkalmazásba beesni, ha nincs session.

Legyen:

- VIZIT brand;
- Belépés;
- Regisztráció;
- Google-belépés, ha engedélyezett;
- elfelejtett jelszó.

---

# 22. Külső hozzáférések állapota

Jelenleg várakozhat:

- Google Auth konfiguráció;
- Xiaomi Developer approval;
- később esetleg Apple Developer.

Ez a teljes fejlesztést NEM állíthatja meg.

Használj:

- feature flaget;
- konfigurációt;
- dokumentált blockert.

---

# 23. Auth

Működjön:

- email/password regisztráció;
- email verification;
- login;
- logout;
- session restore;
- forgot password;
- password reset;
- account delete.

---

# 24. Google Sign-In

Készítsd elő teljesen.

Android oldalon modern Credential Manager-kompatibilis architektúrával.

Amíg nincs használható OAuth credential:

`BLOCKED BY EXTERNAL ACCESS`

de a teljes többi rendszer menjen tovább.

---

# 25. Auth validáció

Ne ismétlődjenek meg a régi webes hibák.

Minden hibánál legyen visszajelzés.

Példák:

- hibás email;
- gyenge jelszó;
- üres mező;
- eltérő jelszavak;
- már létező account;
- hibás login;
- nincs hálózat;
- lejárt link;
- szerverhiba.

Soha ne legyen:

> megnyomtam és semmi nem történt.

---

# 26. Jogi checkbox

Regisztrációnál legyen kötelező:

- adatkezelési tájékoztató elfogadása;
- szükség esetén ÁSZF elfogadása.

Ha nincs elfogadva:

jól látható hibaüzenet.

A dokumentumok legyenek megnyithatók.

---

# 27. Supabase adatmodell

Ne egy óriási `profiles` táblába zsúfolj mindent.

Logikai modell legalább:

```text
profiles
profile_phones
profile_emails
profile_addresses
profile_websites
profile_socials
profile_custom_links
profile_media
profile_field_preferences
profile_share_settings
share_events

```

Az Auth user ID legyen a tulajdonosi kapcsolat alapja.

---

# 28. Profil alapadatok

Támogatandó:

- keresztnév;
- vezetéknév;
- megjelenített név;
- titulussal való későbbi bővíthetőség;
- profilkép;
- céges logó;
- vállalat;
- munkakör;
- bemutatkozás;
- több telefon;
- több email;
- több cím;
- több weboldal;
- LinkedIn;
- Instagram;
- Facebook;
- TikTok;
- YouTube;
- egyéb linkek.

---

# 29. Mezőkezelés

A felhasználó tudja:

- hozzáadni;
- módosítani;
- törölni;
- átrendezni;
- publikussá tenni;
- priváttá tenni.

A rejtett mezők:

**semmilyen publikus payloadba ne kerüljenek bele.**

Ez vonatkozik:

- NFC;
- QR;
- publikus profil;
- share sheet

adatokra is.

---

# 30. Megosztási előnézet

NFC és QR küldés előtt legyen egyértelmű:

**mit fogok most átadni?**

A felhasználó lássa:

- név;
- kép;
- telefon;
- email;
- cég;
- további megosztott mezők.

Private mező ne jelenjen meg.

---

# 31. Profilkép

A profilkép és a céges logó külön adat.

Profilkép szerkesztő:

- képválasztás;
- zoom;
- pan;
- crop;
- preview;
- reset;
- remove.

---

# 32. Profilképek több változatban

Ne ugyanazt a fájlt használd minden célra.

Generálj:

## Original / display

Jó minőségű cloud változat.

## Thumbnail

UI listákhoz.

## NFC contact avatar

Erősen optimalizált változat.

---

# 33. Céges logó

A felhasználói céges logóhoz legyen:

- upload;
- preview;
- remove;
- reset;
- padding kezelése;
- világos háttér preview;
- sötét háttér preview;
- transzparens PNG támogatás;
- WebP/megfelelő formátum támogatás, ahol indokolt.

Megjelenítés:

**contain**

ne:

**cover**.

Nem vághatja le automatikusan a logó fontos részeit.

---

# 34. NFC – termék core feature

A VIZIT fő funkciója:

# TELEFON → TELEFON NFC KONTAKTÁTADÁS

Nem NFC plasztikkártya.

Küldő:

**Xiaomi + VIZIT**

Fogadó:

elsődlegesen másik Android telefon.

---

# 35. NFC elvárt UX

```text
VIZIT
↓
Átadás
↓
NFC
↓
NFC-megosztás aktiválva
↓
telefonok összeérintése
↓
fogadó felismeri a kontaktadatot
↓
ÚJ KONTAKT
↓
Mentés

```

---

# 36. Fogadóoldali követelmény

Fogadó Android telefonon:

NEM szükséges:

- VIZIT;
- account;
- regisztráció;
- `.vcf` fájl manuális letöltése;
- Downloads megnyitása;
- fájlkezelő;
- manuális importálás.

A cél:

**közvetlen rendszeroldali új kontakt folyamat.**

---

# 37. Fontos NFC korlát

Az alkalmazás nem kerülheti meg a fogadó operációs rendszer biztonsági szabályait.

Ha az Android megköveteli a végső:

**Mentés**

megerősítést:

az elfogadható.

Nem kell és nem szabad titokban a másik ember kontaktjai közé írni.

---

# 38. NFC implementáció

Auditáld és productionizáld a meglévőt:

- Host Card Emulation;
- `HostApduService`;
- NFC Forum Type 4;
- NDEF;
- APDU;
- MIME vCard.

Meglévő komponensek:

- `VizitHostApduService`
- `Type4TagApduProcessor`
- `NdefVCardEncoder`
- `VCardBuilder`

---

# 39. vCard

Használj lehetőleg interoperábilis:

**vCard 3.0**

formátumot az Android kompatibilitás érdekében, hacsak valódi eszközteszt nem bizonyítja más verzió előnyét.

---

# 40. vCard mezők

Támogatandó:

- strukturált név;
- teljes név;
- organisation;
- title;
- több telefon;
- telefon típusok;
- több email;
- email típusok;
- weboldal;
- cím;
- profil URL;
- kompatibilis social mezők;
- profilkép.

---

# 41. Magyar karakterek

Külön teszt:

- á;
- é;
- í;
- ó;
- ö;
- ő;
- ú;
- ü;
- ű.

UTF-8 kezelésnek hibátlannak kell lennie.

---

# 42. NFC kép

Profilképet lehetőség szerint vCardba ágyazva add át.

De a kép nem teheti megbízhatatlanná az NFC-t.

Dinamikusan csökkentsd:

- felbontást;
- JPEG qualityt;
- fájlméretet,

amíg a teljes payload stabilan átadható.

---

# 43. Payload hard limit

Soha ne csak egy konstans maximális méretet feltételezz.

Legyen:

- payload size calculation;
- overflow prevention;
- adaptív image compression;
- fallback.

Ha a kép semmilyen elfogadható minőségben nem fér bele:

a kontakt menjen át kép nélkül,

de ezt diagnosztikában rögzítsd.

---

# 44. NFC lifecycle

NFC adat csak explicit megosztáskor legyen olvasható.

Leáll:

- Stop gomb;
- képernyő elhagyása;
- timeout;
- megfelelő lifecycle esemény;
- logout.

---

# 45. NFC képernyő

Legyen vizuálisan egyértelmű:

**NFC átadás aktív**

Mutassa:

- VIZIT branding;
- NFC animáció;
- átadandó profil rövid előnézete;
- instrukció;
- Stop.

---

# 46. NFC sikeresség

Ne mondd azt:

**„Kontakt mentve”**

csak azért, mert NFC kapcsolat volt.

Ha az APDU olvasás alapján az NDEF teljesen kiolvasásra került:

mutasd:

**„NFC adat átadva”**

vagy:

**„NFC adat kiolvasva”**

Ezután:

- haptic feedback;
- animation;
- success state.

---

# 47. NFC kompatibilitási fallback

Ha az adott telefon nem kezel vCardot megfelelően:

ajánld fel:

- Kontakt QR;
- VIZIT profil QR;
- profil-link megosztása.

---

# 48. NFC fizikai teszt

Kötelező:

- Xiaomi → Xiaomi;
- Xiaomi → Redmi;
- Xiaomi → POCO;
- Xiaomi → Samsung;
- Xiaomi → Pixel.

Lehetőség szerint:

- több Android;
- több HyperOS verzió.

---

# 49. NFC tesztelendő

Minden készüléken:

- felismeri-e;
- milyen intent indul;
- kontaktimport nyílik-e;
- minden mező átkerül-e;
- kép átkerül-e;
- magyar karakterek;
- több telefon;
- több email;
- cím;
- cég;
- title;
- URL;
- nincs-e download;
- nincs-e fájlkezelő;
- nincs-e VIZIT telepítési kényszer.

---

# 50. NFC mezővesztés

Ha egy OEM Contacts alkalmazás bizonyos mezőt eldob:

ne jelöld 100%-ban támogatottnak.

Dokumentáld.

Kompatibilitási státusz:

- SUPPORTED
- PARTIALLY SUPPORTED
- FALLBACK REQUIRED
- NOT SUPPORTED

---

# 51. Android → iPhone NFC

Vizsgáld külön.

Nem release blocker, ha az Android-szerű direkt vCard import nem működik.

Ebben az esetben legyen:

- HTTPS NFC fallback;
- QR.

---

# 52. QR – szintén core feature

A QR ne későbbi extra legyen.

Az első release része.

---

# 53. Elsődleges QR: Kontakt QR

A fő QR-megosztási mód célja:

**közvetlen névjegyfelismerés.**

A másik telefon kamerája/QR-olvasója lehetőség szerint:

felismeri a kontaktadatokat

→ új kontakt létrehozását ajánlja.

---

# 54. Kontakt QR adat

Használj olyan szabványos formátumot, amelyet a cél Android és iPhone tesztek alapján a legtöbb kamera megfelelően felismer.

Tesztelendő:

- vCard QR;
- szükség esetén MECARD.

A döntést ne elméletből, hanem készülékteszt alapján véglegesítsd.

---

# 55. Kontakt QR és profilkép

Ne próbálj nagy Base64 képet QR-ba tenni.

A QR:

- gyorsan olvasható;
- kellően ritka;
- stabil

maradjon.

Profilképhez használható a másodlagos online profil.

---

# 56. Másodlagos QR: VIZIT profil

Második QR mód:

**VIZIT profil QR**

Tartalma:

HTTPS URL.

Példa:

`https://<PUBLIC_PROFILE_BASE_URL>/p/{slug}`

A domain ne legyen a kódban mindenhol hardcode-olva.

---

# 57. PUBLIC\_PROFILE\_BASE\_URL

Konfiguráció:

- DEV URL;
- BETA URL;
- PROD URL.

Legyen központi konfiguráció.

---

# 58. QR UX

Átadás képernyő:

```text
NFC

QR

```

QR-nél:

```text
Kontakt QR
VIZIT profil QR

```

Elsődleges vizuális prioritás:

**Kontakt QR.**

---

# 59. QR teljes képernyő

Legyen:

- nagy QR;
- nagy quiet zone;
- magas kontraszt;
- maximális olvashatóság;
- brightness boost;
- kilépéskor brightness restore.

---

# 60. QR műveletek

Lehessen:

- teljes képernyő;
- link másolása;
- QR megosztása képként;
- szükség esetén mentés képként.

---

# 61. QR offline

Kontakt QR:

**internet nélkül működjön.**

Ha a profil lokálisan cache-elve van.

Profil QR:

előállítható offline,

de a fogadóoldali weboldal internetet igényel.

---

# 62. QR tesztek

Legalább:

- Xiaomi Camera;
- Xiaomi Scanner, ha releváns;
- Google Lens;
- Samsung Camera;
- Pixel Camera;
- iPhone Camera.

Vizsgáld:

- kontaktfelismerés;
- sebesség;
- rosszabb fény;
- különböző kijelzőfényerő;
- távolság;
- elforgatás.

---

# 63. QR-nál tiltott fő UX

Ne az legyen a normál Xiaomi Android flow, hogy:

```text
Scan
↓
.vcf fájl letöltése
↓
Downloads
↓
megnyitás
↓
import

```

Ez nem elfogadható fő UX.

Ha egy konkrét telefon csak ezt tudja:

dokumentáld fallbackként,

de ne nevezd teljesen támogatottnak.

---

# 64. Publikus VIZIT profil

A profil QR és platform fallback miatt minimális weboldal szükséges.

Nem teljes VIZIT webapp.

---

# 65. Publikus profil tartalma

- VIZIT branding;
- profilkép;
- név;
- cég;
- beosztás;
- engedélyezett telefonok;
- engedélyezett emailek;
- engedélyezett linkek;
- social;
- cím;
- kapcsolatfelvételi műveletek;
- kontaktmentési lehetőség.

---

# 66. Publikus profil adatvédelem

Csak olyan mezőt olvashat anonim felhasználó:

amit a profil tulajdonosa publikussá tett.

RLS / biztonság ezt szerveroldalon is kényszerítse ki.

---

# 67. App Links

Androidon legyen App Link támogatás.

Ha később VIZIT telepítve van:

profil link

→ VIZIT app megfelelő profilképernyő.

Ha nincs:

→ weboldal.

---

# 68. Megosztás másképp

Maradjon általános:

**Megosztás másképp**

funkció.

Android Share Sheet.

Megosztható:

- VIZIT profil-link;
- kontaktinformáció;
- megfelelő rövid szöveges névjegy.

---

# 69. Kezdőlap

Mutassa:

- VIZIT brand;
- névjegy preview;
- NFC quick action;
- QR quick action;
- profil szerkesztése;
- profil állapot;
- szinkron állapot, ha releváns.

---

# 70. Fő navigáció

Javasolt:

- Kezdőlap
- Névjegyem
- Átadás
- Beállítások

Az Átadás legyen kiemelt.

---

# 71. Xiaomi/HyperOS UX

Kötelező:

- edge-to-edge;
- Material 3;
- megfelelő system bars;
- gesture navigation;
- predictive back;
- keyboard handling;
- haptics;
- state restoration;
- lifecycle;
- font scaling;
- accessibility;
- responsive layout.

---

# 72. Alkalmazásállapotok

Minden fontos felületnek legyen:

- loading;
- success;
- error;
- empty;
- offline;
- disabled;
- permission denied;
- session expired;
- retry;
- sync pending.

---

# 73. Dark és light

Teljes:

- light;
- dark

támogatás.

Minden VIZIT asset legyen jól olvasható mindkettőn.

---

# 74. Accessibility

Legalább:

- TalkBack content descriptions;
- megfelelő kontraszt;
- 48dp+ touch targets;
- font scaling;
- ne csak szín jelezzen állapotot;
- megfelelő fókuszsorrend.

---

# 75. Xiaomi-specifikus ellenőrzés

Teszteld:

- NFC default beállítás;
- HCE;
- NFC permission/system state;
- HyperOS battery management;
- background restrictions;
- Photo Picker;
- app lifecycle;
- Android App Links;
- notification permission;
- dark mode;
- gestures.

Gyártóspecifikus workaround külön osztályba/modulba kerüljön és dokumentáld.

---

# 76. Offline-first profil

Saját profil legyen használható internet nélkül.

NFC ne igényeljen internetet.

Kontakt QR ne igényeljen internetet.

---

# 77. Lokális adat

Használj Roomot olyan adatokhoz, ahol strukturált cache szükséges.

DataStore:

- preferenciák;
- UI beállítások;
- kis konfiguráció.

Ne használj production profiladatbázisként egyszerű SharedPreferences-t.

---

# 78. Szinkronizáció

Supabase cloud source of truth.

De:

lokális módosítás ne vesszen el offline esetén.

---

# 79. Sync queue

Szükség szerint WorkManagerrel:

- pending changes;
- retry;
- backoff;
- network constraint.

---

# 80. Konfliktuskezelés

Definiálj stratégiát:

- updated\_at / version;
- last-write-wins csak akkor, ha megfelelő;
- kritikus konfliktusnál biztonságos merge vagy user feedback.

Ne történjen csendes adatvesztés.

---

# 81. Média feltöltés

Profilkép/logó:

- lokális preview azonnal;
- background upload;
- progress;
- retry;
- server URL;
- cache.

---

# 82. Auth session biztonság

Tokeneket biztonságosan kezeld.

Secret nem kerülhet:

- repositoryba;
- logba;
- screenshot-friendly debug képernyőre PROD-ban.

---

# 83. Supabase RLS

Minden táblához explicit RLS policy.

Teszteld is.

Nem elég csak bekapcsolni az RLS-t.

---

# 84. Storage security

Profilkép és céges logó:

- megfelelő bucket;
- megfelelő ownership;
- publikus vagy signed access tudatosan eldöntve.

Ne legyen véletlenül minden privát asset világosan listázható.

---

# 85. Fióktörlés

Fióktörlés:

- explicit megerősítés;
- Auth user;
- profiladatok;
- média;
- szükséges kapcsolódó rekordok

jól definiált törlése vagy jogilag szükséges megőrzése.

---

# 86. Export

Az architektúra készüljön úgy, hogy később felhasználói adatexport hozzáadható legyen.

---

# 87. Push notification infrastruktúra

Ne kelljen minden értesítési feature-nek 1.0-ban elkészülnie.

De az architektúra készüljön elő:

- security notification;
- rendszerüzenet;
- app update;
- későbbi profilinterakció.

---

# 88. Notification UX

Csak hasznos értesítések.

Ne legyen spam.

Permissiont csak kontextusban kérj.

---

# 89. Analitika

Privacy-conscious események.

Példák:

```text
profile_view
nfc_share_started
nfc_payload_read
nfc_share_cancelled
nfc_fallback
qr_contact_opened
qr_profile_opened
qr_shared
public_profile_opened
contact_action

```

---

# 90. Felhasználói statisztika

Az adatmodell legyen alkalmas későbbi dashboardra.

Például:

- profilmegtekintés;
- NFC megosztás;
- QR megosztás;
- profil link kattintások.

Ne azonosíts feleslegesen fogadó személyeket.

---

# 91. Crash reporting

Beta előtt legyen production-compatible crash monitoring.

Követelmények:

- crash;
- non-fatal error;
- app version;
- OS;
- device model;
- breadcrumb/context privacy-safe módon.

Ne logolj személyes névjegyadatokat.

---

# 92. ANR és performance

Vizsgáld:

- startup;
- main thread blocking;
- képdekódolás;
- NFC payload építés;
- DB műveletek;
- hálózat.

Képmanipuláció ne blokkolja a UI threadet.

---

# 93. Strukturált logging

DEV-ben részletes.

PROD-ban minimális és biztonságos.

Logokban tilos:

- jelszó;
- token;
- teljes kontakt;
- személyes email/telefon indokolatlanul.

---

# 94. Security review

Release előtt:

- exported components;
- deep links;
- intent validation;
- WebView, ha van;
- storage;
- Supabase keys;
- backup;
- logs;
- screenshots érzékeny képernyőn, ha releváns.

---

# 95. Android backup

Tudatosan döntsd el:

mi menthető Android Backupba.

Auth secret/token ne kerüljön rosszul backupba.

---

# 96. Permission minimalizálás

Csak olyan permission kerüljön Manifestbe, ami ténylegesen kell.

Ne kérj:

- Contacts write;
- kamera;
- storage

engedélyt csak azért, mert „hátha kell”.

Photo Picker esetén használj modern rendszermegoldást.

---

# 97. QR kamera

A VIZIT saját QR megjelenítéséhez nem kell kameraengedély.

Ha később QR scanning kerül bele:

külön permission UX.

---

# 98. CI

GitHub Actions minden PR-re:

- compile;
- unit tests;
- lint;
- debug build.

Lehetőleg:

- static analysis;
- dependency check.

---

# 99. Main branch védelem

Hibás build ne kerüljön `main` ágra.

---

# 100. Unit tests

Legalább:

- vCard;
- escaping;
- UTF-8;
- több mező;
- image payload;
- NDEF;
- APDU;
- QR;
- profile visibility;
- repository;
- cache;
- sync;
- validation.

---

# 101. UI tests

Kritikus Compose flow:

- app start;
- login;
- registration;
- validation;
- profile edit;
- image selection;
- NFC start/stop;
- QR display;
- offline state.

---

# 102. Integration test

Supabase DEV környezettel:

- account;
- profile;
- update;
- media;
- delete;
- RLS.

---

# 103. Fizikai teszt kötelező

Emulátor nem elegendő:

- NFC;
- HyperOS;
- camera QR interoperability;
- HCE;
- battery management

ellenőrzéséhez.

---

# 104. Xiaomi device matrix

Minden tesztnél dokumentáld:

- modell;
- HyperOS verzió;
- Android verzió;
- VIZIT build;
- eredmény.

---

# 105. Platform compatibility dokumentum

`docs/PLATFORM_COMPATIBILITY.md`

Tartalmazza:

| KüldőFogadóNFCQR ContactQR ProfileKépStátusz |
| -------------------------------------------- |

---

# 106. Dokumentáció

Kötelező:

```text
ARCHITECTURE.md
AUTH.md
SUPABASE.md
NFC.md
QR.md
OFFLINE_SYNC.md
SECURITY.md
TESTING.md
PLATFORM_COMPATIBILITY.md
RELEASE.md
BLOCKERS.md
BRANDING.md

```

---

# 107. BLOCKERS.md

Minden külső várakozás:

pl.

```text
Google OAuth production approval
STATUS: BLOCKED BY EXTERNAL ACCESS

```

de mellette legyen:

mit lehet addig megcsinálni.

---

# 108. Branding dokumentáció

`BRANDING.md`

Tartalmazza:

- hivatalos logo source;
- brand mark;
- app icon;
- full logo;
- slogan;
- light/dark usage;
- company logo elkülönítés.

---

# 109. Google Play

Készüljön később:

- AAB;
- signing;
- privacy declarations;
- Data Safety;
- store listing;
- screenshots;
- icon;
- feature graphic;
- internal testing;
- closed/open testing, ahol szükséges.

---

# 110. Xiaomi GetApps

Mivel Xiaomi-first:

a Xiaomi Developer hozzáférés jóváhagyása után külön release csatorna.

Készítsd elő:

- megfelelő package;
- signing;
- APK/AAB formátum, amit az aktuális store kér;
- listing;
- icon;
- screenshots;
- privacy information;
- permissions declaration.

A developer approval hiánya:

`BLOCKED BY EXTERNAL ACCESS`

nem fejlesztési blocker.

---

# 111. Release signing

Production signing secret:

SOHA nem kerül Gitbe.

Használj GitHub secrets / megfelelő secure CI megoldást.

---

# 112. R8 / minification

Release előtt teszteld.

Ne legyen olyan, hogy debug működik, release pedig reflection/proguard miatt elromlik.

---

# 113. Beta

Beta buildet fizikai Xiaomi készüléken tesztelj.

App neve:

**VIZIT Beta**

---

# 114. Production

App:

**VIZIT**

Hivatalos VIZIT iconnal.

---

# 115. Google Auth későbbi aktiválása

Ha a jóváhagyás megérkezik:

ne kelljen architektúrát újraírni.

Csak:

- credential;
- config;
- redirect;
- feature flag

aktiválás legyen szükséges.

---

# 116. Xiaomi hozzáférés későbbi aktiválása

Ugyanez.

Store-specific kód ne szivárogjon bele indokolatlanul a domain logikába.

---

# 117. Publikus domain

A domain ne legyen szétszórva a kódban.

Konfiguráció:

`PUBLIC_PROFILE_BASE_URL`

---

# 118. Deep link routing

Készüljön központi router.

Ne képernyőnként manuális URL parsing.

---

# 119. Account onboarding

Első bejelentkezés után:

ha még nincs profil:

rövid onboarding.

Legalább:

1. név;
2. telefon/email;
3. profilkép opcionálisan;
4. névjegy preview.

Ne legyen indokolatlanul hosszú.

---

# 120. Profile completeness

Lehet profil teljességi állapot.

De ne legyen játékos erőltetés.

Cél:

jelezni, mi hiányzik ahhoz, hogy értelmes kontaktot lehessen átadni.

---

# 121. Minimum megosztható profil

NFC/QR kontakt csak akkor aktiválható, ha:

- név van;
- és legalább egy valódi kapcsolati adat.

Például:

telefon vagy email.

---

# 122. Validation

Email:

normális formátumellenőrzés.

Telefon:

ne legyen túl agresszív.

Nemzetközi számokat is kezeljen.

URL:

validálás.

---

# 123. Error messages

Magyar, emberi.

Ne:

`HTTP 422`.

Hanem:

`Ezzel az e-mail-címmel már létezik fiók.`

---

# 124. Retry

Hálózati hiba után legyen:

**Újrapróbálás**

ahol értelmes.

---

# 125. Destructive action

Fióktörlés, profilkép törlés, stb.:

megerősítés.

---

# 126. Loading

Ne legyen dupla kattintásból duplikált művelet.

Loading alatt gomb megfelelően disabled.

---

# 127. Haptic feedback

Használd tudatosan:

- NFC aktiválás;
- NFC read;
- siker;
- fontos action.

Ne vibráljon minden gomb.

---

# 128. NFC diagnosztika

Settingsben:

- NFC available;
- NFC enabled;
- HCE available;
- VIZIT HCE service;
- current share state.

DEV-ben lehet részletes APDU diagnostics.

PROD-ban egyszerű.

---

# 129. Xiaomi NFC Settings shortcut

Ha NFC ki van kapcsolva:

lehetőség szerint rendszerbeállítás megnyitása.

---

# 130. QR preview

A QR tartalma legyen felhasználó számára érthető.

Kontakt QR:

„A QR a következő adatokat tartalmazza…”

---

# 131. Profile slug

Publikus slug:

- egyedi;
- URL-safe;
- normalizált;
- később módosítható kontrolláltan.

Ne használj közvetlen Auth UUID-t publikus URLként.

---

# 132. Public profile privacy

Legyen kapcsoló:

**Publikus VIZIT profil**

Ki/be.

Ha ki:

profil-URL ne adjon ki adatot.

---

# 133. QR fallback private profile esetén

Ha publikus profil ki van kapcsolva:

Profile QR ne legyen aktív,

vagy egyértelműen kérje a bekapcsolását.

Kontakt QR ettől még működhet.

---

# 134. NFC profil URL privacy

Ha publikus profil disabled:

ne kerüljön automatikusan aktív publikus URL a vCardba.

---

# 135. Megosztási események

Ne tárolj fogadó személyről személyes adatot.

Elegendő lehet:

- idő;
- mód;
- kliens;
- anonim technikai eredmény.

---

# 136. Adatminimalizálás

Csak olyan adat legyen backendben, ami a szolgáltatáshoz szükséges.

---

# 137. Localization

Első nyelv:

**magyar.**

De minden user-facing string legyen erőforrásból.

Ne hardcode-olj UI szöveget Compose fájlokba hosszú távon.

Később angol könnyen hozzáadható legyen.

---

# 138. Dátum/idő

Ne feltételezz magyar timezone-t adatbázisban.

Backend timestamp:

UTC.

UI lokalizált.

---

# 139. Release notes

Minden beta/release verziónál legyen rövid CHANGELOG.

---

# 140. CHANGELOG

Készüljön:

`CHANGELOG.md`

---

# 141. README

A README ne roadmapként helyettesítse a tényleges specifikációt.

Tartalmazza:

- projekt;
- build;
- architektúra hivatkozások;
- jelenlegi status;
- setup.

---

# 142. Fejlesztési státusz

Minden nagy modul:

```text
IMPLEMENTED
TESTED
DEVICE TESTED
BLOCKED

```

---

# 143. Munkamódszer

Dolgozz folyamatosan.

Ne állj le minden döntésnél felhasználói jóváhagyást kérni.

Kérdés csak akkor szükséges, ha:

- üzleti döntés;
- visszafordíthatatlan döntés;
- secret;
- külső szerződés;
- production publikálás;
- fizetés;
- végleges package ID

szükséges.

---

# 144. Ne végezz engedély nélkül

Ne:

- publikálj production store-ba;
- törölj production adatbázist;
- írj át production DNS-t;
- változtass végleges application ID-t;
- használj fizetős szolgáltatást;
- generálj/cserélj production secretet

explicit jóváhagyás nélkül.

---

# 145. Commit stratégia

Jelentős funkcionális blokkonként commit.

Például:

```text
feat(auth): add Supabase email authentication
feat(profile): add multi-contact profile model
feat(nfc): improve Type 4 vCard sharing
feat(qr): add contact QR sharing

```

---

# 146. Ne commitolj félkész mockot kész funkcióként

Ha csak UI:

`UI IMPLEMENTED`

ne:

`FEATURE COMPLETE`.

---

# 147. Fejlesztési sorrend – 1. fázis

## Stabilizálás

- repository audit;
- gap analysis;
- project move/refactor;
- develop branch;
- CI;
- build flavors;
- brand asset integráció;
- app icon;
- splash.

---

# 148. 2. fázis

## Supabase alap

- project;
- migrations;
- RLS;
- storage;
- repository;
- local cache;
- sync.

---

# 149. 3. fázis

## Auth

- email;
- session;
- reset;
- registration validation;
- legal checkbox;
- Google integration shell.

---

# 150. 4. fázis

## Profil

- full profile model;
- multi phone/email;
- socials;
- image;
- company logo;
- visibility;
- ordering.

---

# 151. 5. fázis

## NFC

- HCE refactor;
- APDU;
- NDEF;
- vCard;
- image;
- lifecycle;
- diagnostics;
- physical testing.

---

# 152. 6. fázis

## QR

- Contact QR;
- QR format device test;
- Profile QR;
- brightness;
- share.

---

# 153. 7. fázis

## Public profile

- secure public endpoint;
- mobile web profile;
- App Links;
- contact save fallback.

---

# 154. 8. fázis

## Xiaomi UX polish

- gestures;
- HyperOS;
- battery;
- theme;
- animations;
- haptics;
- accessibility.

---

# 155. 9. fázis

## Reliability

- offline;
- sync;
- retry;
- crash reporting;
- performance;
- security.

---

# 156. 10. fázis

## Testing

- unit;
- integration;
- UI;
- physical Xiaomi;
- cross-OEM;
- iPhone fallback.

---

# 157. 11. fázis

## Beta

`VIZIT Beta`

Valódi teszt.

---

# 158. 12. fázis

## Release candidate

`v0.9.0-rc.1`

Teljes regresszió.

---

# 159. VIZIT 1.0 Definition of Done

A VIZIT 1.0 akkor kész, ha:

### App

- stabilan indul Xiaomi/HyperOS-on;
- hivatalos VIZIT brandinget használ;
- launcher icon helyes;
- splash helyes;
- nincs blocker crash.

### Auth

- registration;
- login;
- logout;
- verification;
- reset;
- session;
- account delete.

A Google login külső approval miatt opcionálisan lehet dokumentált blocker.

### Profile

- teljes profil;
- több telefon;
- több email;
- social;
- kép;
- céges logó;
- mezősorrend;
- visibility;
- offline cache.

### NFC

- offline működik;
- telefon → telefon;
- fogadó Androidon nincs VIZIT szükség;
- támogatott készüléken közvetlen kontaktimport flow;
- nincs `.vcf` download fő folyamatként;
- nincs Downloads;
- kép a támogatott készülékeken;
- több mező;
- magyar karakterek;
- fizikailag tesztelve.

### QR

- Contact QR működik;
- offline előállítható;
- gyorsan olvasható;
- Xiaomi kamerával tesztelt;
- más Androiddal tesztelt;
- iPhone-nal tesztelt;
- Profile QR működik.

### Backend

- Supabase;
- migrations;
- RLS;
- Storage;
- sync;
- security.

### Web fallback

- minimális publikus profil;
- mobilbarát;
- Android;
- iPhone Safari.

### Quality

- unit tests;
- integration tests;
- UI tests;
- CI green;
- crash monitoring;
- security review;
- release build teszt.

### Environments

- DEV;
- BETA;
- PROD.

### Documentation

naprakész.

---

# 160. Google Auth blocker szabály

Ha Google jóváhagyás még nincs:

a VIZIT fejlesztés:

**NEM ÁLLHAT MEG.**

Google:

`BLOCKED BY EXTERNAL ACCESS`

Minden más:

folytatódik.

---

# 161. Xiaomi Developer blocker szabály

Ugyanez.

GetApps publikálás várhat.

Android fejlesztés nem.

---

# 162. Legfontosabb termékelv

A VIZIT megosztási funkcióját ne a technológia szemszögéből tervezd.

A felhasználó gondolkodása:

> „Odaérintem a telefonomat, és a másik ember el tudja menteni a kontaktomat.”

és:

> „Megmutatom a QR-t, és a másik ember el tudja menteni a kontaktomat.”

Minden UX-döntés ezt szolgálja.

---

# 163. Tiltott kompromisszumok

Nem fogadható el kész megoldásként:

- NFC csak weboldalt nyit Androidon, miközben direkt kontakt működhetne;
- NFC `.vcf` fájlt letöltet fő megoldásként;
- QR `.vcf` letöltésre épül fő Xiaomi flowként;
- fogadóoldali VIZIT telepítés kötelező;
- private mező véletlenül megosztható;
- profilkép hibásan vágva;
- céges logó levágva;
- VIZIT logó összekeverve a céges logóval;
- gomb visszajelzés nélkül;
- mock backend production funkcióként;
- debug credential productionban.

---

# 164. Első végrehajtandó feladatlista

A specifikáció átvétele után:

1. ellenőrizd a teljes jelenlegi repositoryt;
2. készíts gap analysist;
3. dokumentáld a megőrzendő és javítandó kódot;
4. hozd létre a `develop` ágat;
5. szervezd át a projektet jövőálló struktúrára;
6. integráld a mellékelt hivatalos VIZIT brand assetet;
7. készíts helyes adaptive app icont;
8. hozd létre DEV/BETA/PROD build flavorokat;
9. stabilizáld a CI-t;
10. építsd fel az új Supabase adatbázist;
11. hozd létre az RLS policykat;
12. készítsd el az offline/cache réteget;
13. készítsd el az email/password Authot;
14. készítsd elő a Google Auth adaptert;
15. készítsd el a teljes profilmodellt;
16. készítsd el a kép- és cégeslogó-kezelést;
17. refaktoráld production szintre az NFC HCE kódot;
18. oldd meg az adaptív NFC képtömörítést;
19. készítsd el az NFC megosztási preview-t;
20. készítsd el a Contact QR-t;
21. valódi készülékeken döntsd el a legjobb Kontakt QR formátumot;
22. készítsd el a Profile QR-t;
23. készítsd el a minimális publikus profilt;
24. készítsd el az App Link infrastruktúrát;
25. készítsd el a Share Sheet megosztást;
26. teljesítsd a Xiaomi/HyperOS UX követelményeket;
27. készíts offline sync/retry rendszert;
28. készíts crash monitoringot;
29. bővítsd a teszteket;
30. készíts fizikai kompatibilitási mátrixot;
31. építs `VIZIT Beta` verziót;
32. teszteld valós Xiaomi készüléken;
33. javíts minden blocker hibát;
34. készíts release candidate-et;
35. csak a Definition of Done teljesülése után készíts `1.0.0` release-t.

---

# 165. Végső fejlesztői utasítás

Ne készíts új, eldobható párhuzamos prototípust.

A meglévő működő komponenseket használd fel, de a teljes rendszert alakítsd át a jelen master specifikációnak megfelelő production architektúrára.

A fejlesztés fő fókusza most:

**Xiaomi / HyperOS.**

A backend:

**új Supabase.**

A két legfontosabb átadási mód:

**NFC + QR.**

Az NFC elsődleges célja:

**telefon → telefon → új kontakt.**

A QR elsődleges célja:

**beolvasás → új kontakt.**

A publikus profil URL:

**platformfüggetlen fallback és másodlagos megosztási mód.**

A VIZIT hivatalos logója:

**a specifikációhoz mellékelt brand asset.**

A felhasználó céges logója:

**ettől teljesen külön profiladat.**

A Google Auth és Xiaomi Developer hozzáférések hiánya:

**nem állíthatja le a teljes fejlesztést.**

Haladj végig folyamatosan a specifikáción, buildelj, tesztelj, commitolj és dokumentálj minden jelentős blokk után.

A cél nem demo, hanem egy valóban kiadható:

# VIZIT 1.0 – Xiaomi/HyperOS

alkalmazás.