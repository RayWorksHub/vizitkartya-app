# Web–mobil funkcióátvilágítás (2026-09-27)

Források: a `vizitkartyam-web-frontend` `022437b` commitja, Android `c509c2f`, iOS 8.7 `2f7d4c3` és a `vizitkartyam_backend` `8577083` commitja. A státusz a forráskód vizsgálatán alapul; fizikai telefonos végponttól végpontig tesztet nem jelent.

| Webes funkció | Android a változtatás előtt | iOS a változtatás előtt | Mobilmegoldás |
| --- | --- | --- | --- |
| Névjegy, fotó, nyilvános profil, QR, közösségi linkek | Natív alapmezők, social mezők, QR, NFC | Natív alapmezők, social mezők, QR | Megmarad natívan. Az egyedi profilcím most szerkeszthető mindkét appban, Androidon a megjelenített cím a valós `www.vizitkartyam.hu/p/` hostot használja. |
| Online névjegy háttérszíne, logó, többféle közösségi link és azok sorrendje | Csak eszközön tárolt kártyanézet; a webes megjelenés külön | Csak eszközön tárolt kártyanézet; a webes megjelenés külön | Közvetlen, feliratozott hozzáférés a mobilra optimalizált webes `/dashboard/profile` szerkesztőhöz. Bejelentkezés a böngészőben szükséges lehet. A natív kártyaszínek továbbra sem a webes publikus megjelenési adatok. |
| Profilmegtekintés, névjegymentés, kattintás, napi aktivitás és leggyakoribb műveletek | Hiányzott | Hiányzott | Natív statisztikai képernyő mindkét appban; ugyanahhoz a felhasználóhoz kötött 30 napos események a bearer hitelesítésű `/api/analytics/summary` végpontból. |
| CRM: partnerek, érdeklődők, ügyletek, ajánlatok, feladatok, naptár, fájlok, import, riportok, csapatjogosultságok | Hiányzott | Hiányzott | Közvetlen belépési pont a mobilra optimalizált teljes webes CRM munkatérbe, böngészőben; az app nem másolja külön adatbázisba a CRM-et. A böngészőben bejelentkezés szükséges lehet. |
| Fiók adatainak JSON exportja | Hiányzott | Hiányzott | Natív rendszerfájl mentő; a már meglévő, hitelesített `/api/account` végpont tölti fel a tartalmat. |
| Vállalkozói Portál: VOSZ, edukáció, segítség, eszköztár | Natív katalógus és leckék | Natív katalógus és leckék | Már elérhető mindkét platformon; a webes és natív leckekövetés külön tárolódik, így az előrehaladás nem szinkronizált. |
| Weboldal Pilóta | Weben kikapcsolt, „hamarosan” funkció | Weben kikapcsolt, „hamarosan” funkció | A weboldalon sincs működő weboldalkészítés, ezért az appokba nincs meghirdetve működőként. |
| Fióktörlés és jelszókezelés | Már van | Már van | Megmarad a natív fiókfolyamat. |

## Határok és ellenőrzés

- A CRM és a speciális online profilmegjelenés a biztonságos webes munkafelületen nyílik meg, így a böngésző saját munkamenete eltérhet az alkalmazásétól. Mobil auth tokent nem teszünk URL-be és nem adjuk át másik folyamatnak.
- A statisztikai végpontnak ellenőrzött felhasználói token kell, tulajdonos szerint szűr, és a profilhoz tartozó eseményeket olvassa. 401-et ad bejelentkezés nélkül. Az API a központi backend CI-jében ellenőrzött; a mobilképernyők build és valós fiókos próbája külön szükséges.
- Az adatexport személyes adatot tartalmaz; a mentési helyet a felhasználó választja. A mobilapp nem csatolja automatikusan üzenethez vagy e-mailhez.
