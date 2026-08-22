# Tesztelés

## Automatikus

PR/branch CI: DEV unit test, DEV lint, DEV debug build, BETA debug build, PROD release compile.

A profil-adatréteg JVM tesztjei ellenőrzik a normalizált Room/payload mappinget, a lokális kép kizárását a felhő-outboxból, a stabil rekordazonosítókat, az exponenciális retry limitjét, az alkalmazott syncet, a hálózati retryt, a sessionhiányt, a konfliktusblokkolást és a tiszta cache pullját.

Kötelező bővítendő területek: Room migráció eszköz/instrumentation teszt, Supabase RPC/RLS integrációs teszt, teljes többértékű profil mapping, App Link és Compose kritikus flow-k.

## Fizikai

NFC release minősítéshez fizikai eszköz kell. Az eredmény nem `SUPPORTED`, amíg nincs valódi készülékteszt. A mátrixban külön rögzítendő készülékmodell, HyperOS/Android/iOS verzió, kontaktimport, fotó, több mező, böngésző/fájlletöltés és fallback.

Az offline profilhoz külön ellenőrizendő: repülő módú szerkesztés és újraindítás, több egymás utáni szerkesztés outbox-coalescingje, hálózat-visszatérés, process kill alatti sync lease, kijelentkezett állapot, valamint két eszköz verziókonfliktusa. Élő Supabase vagy fizikai eszköz használata külön jóváhagyást igényel.
