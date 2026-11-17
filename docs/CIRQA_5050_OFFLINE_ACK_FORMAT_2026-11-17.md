# CIRQA 5050: Offline-ACK-Formatvergleich (17.11.2026)

## Verifizierte Referenzdaten

- Öffentliche [garmin-ble-Typen](https://github.com/gwerneckp/garmin-ble/blob/main/garmin_ble/constants.py): `5050=CONFIGURATION`, `5011=FIT_DEFINITION`, `5000=RESPONSE`.
- Öffentliche [Open-Fit-GFDI-Analyse](https://github.com/mstraa/open-fit-project/blob/main/docs/GARMIN-GFDI-DESIGN.md): generischer Status-Body `[u16 original_message_id][u8 status]` mit ACK-Status 0; nur 5043/5044 als Protobuf beschrieben.
- CIRQA-Beobachtung: 5050-Body 19 Byte, dreimal bytegleich, ~5 s Retry; danach 5011-Body 21 Byte. Keine bestätigte Anwendungs-ACK-Akzeptanz.

## Implementierter rein lokaler Encoder

`CirqaOfflineGfdiStatusCandidate.ackFrame(5050)` erzeugt einen 9-Byte-GFDI-Frame: Länge=9, Typ=5000, ursprünglicher Typ=5050, Status=0, CRC-16/ARC. Keine GATT- oder BLE-Anbindung. Unit-Tests prüfen Länge, Felder, CRC, beschädigte Frames und Bytegleichheit des bestehenden minimalen 5024-Kandidaten.

**Wichtige Grenze:** Ein formal korrekt kodierter ACK ist noch kein gültiger CIRQA-Konfigurations-Handshake. Das 19-Byte-Konfigurationsformat ist bislang nicht entschlüsselt; weder seine Felder noch eine eventuell erforderliche zusätzliche Antwort sind belegt. Deshalb keine automatische 5050-Bestätigung und kein weiterer Gerätetest.

## Weiteres Vorgehen

- CI-Unit-Tests abwarten und etwaige Fehler beheben.
- Unabhängige konkrete 5050-Parser/Antwortbeispiele recherchieren; mögliche Versionen und erforderliche Statusfolge gegen CIRQA-Frames offline prüfen.
- Erst mit belastbarer Evidenz und expliziter Testentscheidung kontrolliert am Gerät erproben. Keine geratenen Antworten.
