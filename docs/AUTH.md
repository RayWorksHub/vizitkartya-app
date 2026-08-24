# Auth

## Implementált kliensfolyamok

- email/jelszó regisztráció és külön „megerősítő e-mail elküldve” állapot;
- email-hitelesítési callback és session-visszaállítás;
- bejelentkezés és kijelentkezés;
- privacy-safe elfelejtettjelszó-visszajelzés;
- recovery callback, új jelszó és jelszóismétlés;
- konfigurációvezérelt Google Credential Manager + Supabase ID-token belépés;
- minden input-, loading-, siker- és hibaállapot magyar visszajelzéssel;
- jelszó láthatóságkapcsoló és loading alatti dupla művelet tiltása;
- DEV debug-only helyi profil fallback.

A callback feldolgozó csak az adott flavor pontos scheme-jét, az `auth-callback` hostot és üres vagy `/` pathot fogadja el. Eltérő host, port, userinfo, túlméretes vagy hibás URI nem jut el a Supabase sessionkezelőhöz. A szolgáltatói hibaleírás nyers tartalma nem kerül a képernyőre.

## Jogi elfogadás

A regisztrációs checkbox csak akkor aktiválható, ha az alábbi négy Gradle property rendelkezésre áll, és mindkét dokumentum-URL érvényes HTTPS-cím:

- `VIZIT_PRIVACY_POLICY_URL`
- `VIZIT_PRIVACY_POLICY_VERSION`
- `VIZIT_TERMS_URL`
- `VIZIT_TERMS_VERSION`

A kliens a verziókat signup metadata formájában küldi, a trigger pedig szerveridővel naplózza az elfogadást. OAuth- és korábbi fióknál az app az aktuális verziókat a `has_legal_acceptance` RPC-vel ellenőrzi. Hiány esetén kötelező jogi képernyő jelenik meg, és az `accept_legal_documents` RPC rögzíti az elfogadást. Profil pull/push csak legalább egy naplózott elfogadás után érhető el.

Az aktuális, ellenőrzött verzió csak az adott user ID-hoz kerül lokális DataStore cache-be. Ez offline sessionnél megakadályozza, hogy másik fiók korábbi elfogadása feloldja a profilt.

## Fióktörlés

A Beállítások képernyő külön felhőfiók-kártyát ad kijelentkezéssel és végleges törléssel. Törlés előtt a felhasználónak be kell írnia a `TÖRLÉS` kifejezést. Sikeres szerveroldali törlés után az alkalmazás tranzakcióban eltávolítja a Room-profilt, a kapcsolódó rekordokat, a sync outboxot és a lokális auth cache-t.

A `delete-account` Edge Function:

- a hívó JWT-jéből azonosítja a felhasználót;
- rekurzívan és lapozva törli a privát és publikus profilmédiát;
- storage hibánál nem folytatja csendben az Auth-törlést;
- törli az Auth usert, ami kaszkádolja a profilrekordokat;
- az Auth-törlés után ismét ellenőrzi a bucketeket;
- nem naplóz tokent, e-mailt vagy user ID-t.

Az aktív-user storage policy miatt egy törölt user még le nem járt JWT-je sem tölthet fel új fájlt.

## Külső konfiguráció

A Google gomb a DEV flavorban akkor jelenik meg, ha a jogi dokumentumok és a `VIZIT_DEV_GOOGLE_WEB_CLIENT_ID` is konfigurált. A BETA/PROD flavorban ez a DEV credential nem aktiválja a Google-belépést; Client Secret nem kerül az appba.

A CI-ben készülő DEV APK tartós aláírói SHA-1 fingerprintje: `42:97:D6:00:58:C2:7A:BB:5E:6F:87:BB:70:1A:73:47:99:BD:87:E1`. A `hu.rayworks.vizit.dev` Android OAuth kliensnek ezt a fingerprintet kell használnia.

A Supabase Auth működéséhez flavoronként URL/publishable key, engedélyezett redirect URL, migráció és Edge Function deploy szükséges. A service-role kulcs kizárólag a Supabase Edge Function környezetében vagy külön engedélyezett DEV E2E folyamatban használható; mobilbuildbe és Gitbe nem kerülhet.

## Redirect scheme

- DEV: `vizit-dev://auth-callback`
- BETA: `vizit-beta://auth-callback`
- PROD: `vizit://auth-callback`

A kliens PKCE flow-t használ.
