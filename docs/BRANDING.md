# VIZIT branding

## Source of truth

A hivatalos forrásfájl: `brand/vizit-logo-master.png`.

SHA-256:

```text
31850228434482fd56321dc2bfd4450025e3bb4650367331fc079a5c311efee6
```

## Assetek

| Asset | Használat |
|---|---|
| `vizit_logo_full.png` | Auth, onboarding, About és kiemelt brand felületek |
| `vizit_logo_mark.png` | Fejléc, NFC és kompakt alkalmazásfelületek |
| `ic_launcher_foreground_logo.png` | Adaptive és round launcher icon foreground |
| `ic_launcher_monochrome.xml` | A hivatalos márkajel alfaalakját használó themed/monochrome drawable |

Az adaptív ikon háttérszíne `VizitIce`, mert ezen a hivatalos márkajel sötét és világos gradiense egyaránt kontrasztos. A foreground a teljes feliratos logó helyett kizárólag a „V + NFC” márkajel; a themed icon ugyanennek a torzítatlan alfaalakját használja.

## Színek

- Navy: `#061B46`
- Blue: `#055EEC`
- Cyan: `#13D1FC`
- Ice: `#EAF8FF`

A Dynamic Color alapértelmezetten ki van kapcsolva, hogy a Xiaomi/HyperOS és más Android rendszerek ne írják felül a VIZIT márkaidentitását.

## VIZIT brand és céges logó

A VIZIT-logó nem felhasználói adat és nem cserélhető. A `CompanyLogo` külön profiladat; megjelenítése `ContentScale.Fit`/`contain`, hogy ne vágja le a grafika részeit.

## Világos és sötét felület

A forráslogó sötét szöveget tartalmaz, ezért a teljes lockup mindkét témában fehér Surface-en jelenik meg. A kompakt márkajel szintén kontrollált fehér felületet kap.
