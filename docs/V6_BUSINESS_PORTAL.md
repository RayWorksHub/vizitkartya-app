# VIZIT 7.0.0 — Vállalkozói Portál és saját domain

## Elkészült funkciók

- Az első profilmentés automatikusan, a megjelenített névből készít publikus azonosítót.
- Az adatbázis az azonosító-ütközést fiókhoz kötött utótaggal oldja fel.
- A profilhoz opcionális egyedi domain kérhető Androidon, iOS-en és a webes szerkesztőben.
- A QR és az NFC csak ellenőrzött egyedi domaint használ; addig a stabil VIZIT-cím marad aktív.
- A webalkalmazás az ellenőrzött domain gyökerén rendereli a hozzá tartozó publikus profilt.
- A kezdőlapról elérhető az új Vállalkozói Portál:
  - VOSZ forrásközpont;
  - hat kurzusból álló Vállalkozói Edukáció;
  - leckék és helyi haladásjelzés;
  - egyértelműen jelölt videóhívás-bemutató;
  - interaktív digitális állapotfelmérés és következő lépés.

## Egyedi domain aktiválása

1. A tulajdonos megadja a hostnevet, például `nevjegy.cegem.hu`.
2. A backend a domaint nem ellenőrzöttre állítja.
3. Az üzemeltető a domaint hozzáadja a Vercel projekthez, majd a szolgáltató biztonságos DNS-folyamatával ellenőrzi a tulajdonjogot.
4. A megbízható backend-folyamat `custom_domain_verified = true` állapotot állít be.
5. A következő profilszinkron után a mobil QR/NFC automatikusan a saját domaint használja.

A domain megváltoztatása mindig törli az ellenőrzött állapotot. Mobil- vagy publikus API-kliens nem jelölhet domaint saját maga ellenőrzöttnek.

## Elrendezés

A portál négy területe és az edukáció témái egymás mellett, vízszintesen
görgethető sorban állnak. Így mindegyik kártya elég nagy ahhoz, hogy egy
pillantásra olvasható legyen, és a sor nem tolja le a képernyőről a portál
többi részét. A sor a képernyő széléig fut, így a félig látszó következő
kártya jelzi, hogy tovább lehet húzni.

## Verzió

- Android: `7.0.0` (`7000000`)
- iOS: `7.0.0`
