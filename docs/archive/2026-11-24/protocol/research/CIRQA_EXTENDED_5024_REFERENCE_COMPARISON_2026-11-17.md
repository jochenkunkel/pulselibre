# Erweiterte GFDI-5024-Antwort: Referenzvergleich (2026-11-17)

## Direkt geprüfte externe Implementierung

- [GarminHRM600 hrm600/bootstrap.py](https://github.com/mskerwiderski/GarminHRM600/blob/main/hrm600/bootstrap.py) enthält ein als **fenix-8-Watch-Capture** beschriebenes `CAPTURED_WATCH_DEVICE_INFORMATION_ACK`, nicht eine CIRQA-Bestätigung.
- [GarminHRM600 hrm600/gfdi.py](https://github.com/mskerwiderski/GarminHRM600/blob/main/hrm600/gfdi.py) definiert GFDI `[length:u16][type:u16][body][crc16:u16]`, `5000=RESPONSE` und `5024=DEVICE_INFORMATION`.
- Das erfasste Watch-Frame hat **52 Byte** Gesamtlänge (`0x0034`), Typ **5000** (`0x1388`), Body-Beginn **`A0 13 00`** (Antwort auf 5024, Status 0). Es besitzt **43 zusätzliche Body-Bytes** gegenüber PulsLibres 9-Byte-Minimalantwort. Die zusätzlichen Bytes enthalten unter anderem erkennbare fenix-8-Modellinformationen; ihr CIRQA-Format ist nicht nachgewiesen.
- Die Referenz enthält danach weitere *kompakte* Bootstrap-Frames, Feature-Capabilities-Request und Ready-Trigger. Das ist ein beobachteter Ablauf **zwischen HRM600 und fenix 8**, nicht als CIRQA-Handshake validiert. **5050 ist in der geprüften GFDI-Namensliste nicht aufgelöst.**

## Umsetzung in PulsLibre

`GarminDeviceInformationAckCandidate.extendedFrame(extension)` kann jetzt **offline** einen CRC-korrekten erweiterten 5000/5024-Statusframe mit explizit bereitgestellten Erweiterungsbytes bilden. Unit-Tests prüfen Header, Body, CRC und Größenbegrenzung. **Der BLE-Sendepfad bleibt absichtlich unverändert:** kein Nachspielen einer fremden Geräteidentität, keine unbekannten Capability-Werte und keine automatischen 5050-ACKs.

## Offene Frage

Was bedeuten die 43 zusätzlichen Bytes semantisch, und welche davon muss ein CIRQA-kompatibler Client selbst bereitstellen? Dafür sind Felddefinitionen oder ein CIRQA-kompatibler, funktionierender Client-Mitschnitt erforderlich. Das Kopieren der fenix-8-Bytes wäre kein seriöser Kompatibilitätsnachweis.
