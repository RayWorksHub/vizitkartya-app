# Tesztelés

## Automatikus

PR/branch CI: DEV unit test, DEV lint, DEV debug build, BETA debug build, PROD release compile.

Kötelező bővítendő területek: vCard escaping és magyar karakterek, NDEF több rekord, payload limit, APDU hibák/részleges olvasás/teljes olvasás, profilvalidáció, Room repository, Supabase mapping, QR determinisztikusság, App Link és Compose kritikus flow-k.

## Fizikai

NFC release minősítéshez fizikai eszköz kell. Az eredmény nem `SUPPORTED`, amíg nincs valódi készülékteszt. A mátrixban külön rögzítendő készülékmodell, HyperOS/Android/iOS verzió, kontaktimport, fotó, több mező, böngésző/fájlletöltés és fallback.
