# Platform compatibility

Utolsó frissítés: 2026-08-22  
Bizonyítékállapot: automatizált POC tesztek vannak; fizikai kompatibilitási mérés még nincs.

## NFC

| Küldő → fogadó | vCard felismerés | Közvetlen újkontakt-UI | Kép | Profil-URL fallback | Státusz |
|---|---|---|---|---|---|
| Xiaomi → Xiaomi | nincs fizikai adat | nincs fizikai adat | nincs fizikai adat | tervezett | `BLOCKED BY EXTERNAL ACCESS` |
| Xiaomi → Redmi | nincs fizikai adat | nincs fizikai adat | nincs fizikai adat | tervezett | `BLOCKED BY EXTERNAL ACCESS` |
| Xiaomi → POCO | nincs fizikai adat | nincs fizikai adat | nincs fizikai adat | tervezett | `BLOCKED BY EXTERNAL ACCESS` |
| Xiaomi → Samsung | nincs fizikai adat | nincs fizikai adat | nincs fizikai adat | tervezett | `BLOCKED BY EXTERNAL ACCESS` |
| Xiaomi → Pixel | nincs fizikai adat | nincs fizikai adat | nincs fizikai adat | tervezett | `BLOCKED BY EXTERNAL ACCESS` |
| Xiaomi → iPhone | generikus app nélküli automatikus import nem vállalható | Androiddal azonos folyamat nem támogatott szerződés | URL-oldalon | kötelező | `FALLBACK REQUIRED` |

Az Android sorok release előtt fizikai teszttel `SUPPORTED`, `PARTIALLY SUPPORTED`, `FALLBACK REQUIRED` vagy `NOT SUPPORTED` állapotot kapnak. Addig nem tekinthetők támogatottnak.

## QR és HTTPS

| Fogadó | Kontakt QR | Profil QR | Publikus profil | Státusz |
|---|---|---|---|---|
| Xiaomi Camera | nincs fizikai adat | nincs fizikai adat | nincs fizikai adat | `BLOCKED BY EXTERNAL ACCESS` |
| Google Lens | nincs fizikai adat | nincs fizikai adat | nincs fizikai adat | `BLOCKED BY EXTERNAL ACCESS` |
| Samsung Camera | nincs fizikai adat | nincs fizikai adat | nincs fizikai adat | `BLOCKED BY EXTERNAL ACCESS` |
| iPhone Camera | nincs fizikai adat | nincs fizikai adat | nincs fizikai adat | `BLOCKED BY EXTERNAL ACCESS` |

## Tesztjegyzőkönyv sablon

| Dátum | Build SHA | Küldő modell/OS | Fogadó modell/OS | Mód | Eredmény | Státusz | Megjegyzés |
|---|---|---|---|---|---|---|---|
| – | – | – | – | – | még nincs készülékteszt | – | – |
