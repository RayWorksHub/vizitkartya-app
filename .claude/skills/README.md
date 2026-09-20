# Design skillek

Ez a mappa külső, nyílt forrású Claude Code design skilleket tartalmaz, a
"The Claude Web Design Killer Setup" ajánlás alapján. A skillek egyszerű
Markdown utasításkészletek — nem futtatnak kódot, nem hívnak külső API-t,
nem igényelnek API kulcsot.

## Forrás és licenc

| Skill | Forrás | Commit | Licenc |
|---|---|---|---|
| `emil-design-eng`, `animate`, `animation-vocabulary`, `apple-design`, `find-animation-opportunities`, `improve-animations`, `mobile-native`, `pick-ui-library`, `prototype`, `review-animations`, `write-swift` | [emilkowalski/skills](https://github.com/emilkowalski/skills) | `85e8e23` | MIT — `LICENSE.emilkowalski-skills` |
| `design-taste-frontend`, `redesign-existing-projects`, `high-end-visual-design`, `minimalist-ui` | [leonxlnx/taste-skill](https://github.com/leonxlnx/taste-skill) | `5217fb4` | MIT — `LICENSE.leonxlnx-taste-skill` |

A taste skillek mappanevei a `SKILL.md` frontmatter `name` mezőjéhez lettek
igazítva (`taste-skill` → `design-taste-frontend`, `redesign-skill` →
`redesign-existing-projects`, `soft-skill` → `high-end-visual-design`,
`minimalist-skill` → `minimalist-ui`).

## Melyik mire jó

**Web (publikus VIZIT profil-oldal, landing):**
`design-taste-frontend` (a fő "taste" skill: anti-sablon layout, tipográfia,
színrendszer), `high-end-visual-design`, `minimalist-ui`,
`redesign-existing-projects` (meglévő oldal auditja és felhúzása),
`mobile-native` (webes felület mobilon natív érzet), `animate`,
`apple-design`, `pick-ui-library`.

**Natív (Android Compose, iOS):**
`emil-design-eng` (általános UI-polish elvek), `apple-design`,
`animation-vocabulary`, `review-animations`, `improve-animations`,
`find-animation-opportunities`, `write-swift` (az `ios/` modulhoz).

A `pick-ui-library`, `prototype` és `review-animations` skillek
`disable-model-invocation: true` beállítással jönnek — ezeket kézzel kell
meghívni (`/pick-ui-library` stb.), maguktól nem indulnak el.

## Ami szándékosan kimaradt

- **Impeccable** (`pbakaus/impeccable`) — nem sima skill: `npx impeccable
  install` letölt egy külső Rust binárist `~/.impeccable/bin/`-be, és hookot
  ír a `.claude/settings.local.json`-be, ami minden UI-fájl szerkesztésekor
  automatikusan lefut. Külön döntés kérdése, ezért nincs itt.
- `animate-expo`, `ask-sonner` — React Native/Expo, illetve a Sonner toast
  library; egyik sincs ebben a stackben.
- `brandkit`, `imagegen-frontend-web`, `imagegen-frontend-mobile`,
  `image-to-code` — képgeneráláshoz kötött skillek, képgeneráló eszköz
  nélkül nem használhatók.

## Vizuális ellenőrzés Playwright-tal

A Claude Code webes környezetében a Playwright és a Chromium előre telepítve
van (`PLAYWRIGHT_BROWSERS_PATH=/opt/pw-browsers`), külön telepítés nélkül
használható — **ne futtass `playwright install`-t**. Desktop + mobil
screenshot egy futó oldalról:

```bash
npx -y playwright@1.56.1 screenshot --viewport-size=1440,900 <url> desktop.png
npx -y playwright@1.56.1 screenshot --viewport-size=390,844  <url> mobile.png
```

Lokálisan `npm i -D @playwright/test && npx playwright install chromium`.

## Frissítés

```bash
git clone --depth 1 https://github.com/emilkowalski/skills /tmp/ek
cp -r /tmp/ek/skills/<skill> .claude/skills/<skill>
```
