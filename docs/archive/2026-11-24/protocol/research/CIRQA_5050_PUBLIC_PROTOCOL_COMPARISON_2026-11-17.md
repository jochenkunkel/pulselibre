# CIRQA GFDI 5050/5011: Abgleich mit öffentlichen Implementierungen (2026-11-17)

## Externe Referenzen (keine CIRQA-Protokollspezifikation)

- [gwerneckp/garmin-ble: garmin_ble/constants.py](https://github.com/gwerneckp/garmin-ble/blob/main/garmin_ble/constants.py): `5024=DEVICE_INFORMATION`, `5050=CONFIGURATION`, `5011=FIT_DEFINITION`, `5000=RESPONSE`.
- [mstraa/open-fit-project: GARMIN-GFDI-DESIGN](https://github.com/mstraa/open-fit-project/blob/main/docs/GARMIN-GFDI-DESIGN.md): gleiche GFDI-Nachrichtentypen. Beschreibt generisches `5000 RESPONSE`-ACK mit `[u16 original_message_id][u8 status]` (ACK-Status 0) und `5050` als binäre Konfiguration, **nicht** als Protobuf. Die Dokumentation basiert auf anderen Garmin-Geräten und ist für CIRQA nicht verifiziert.

## Abgleich mit CIRQA-Messung

- Passive Diagnose ohne ACK: `5024 x4`.
- Einmaliger 5024-ACK-Kandidat: `5024, 5050 x3, 5011`.
- Alle drei 5050-Frames identisch, 19-Byte-Body; Abstand 4997 und 5011 ms. 5011 folgt nach weiteren 5010 ms; 21-Byte-Body.
- **Neue Arbeitshypothese:** 5050 ist eine Konfigurationsnachricht, die mangels gültiger Anwendungsantwort wiederholt wird. **Nicht bewiesen:** dass ein generisches 5000-ACK für CIRQA genügt, dass 5011 einen Abbruch signalisiert, oder dass FIT-Daten bereits übertragen werden.
- Insbesondere ist die öffentliche Bezeichnung `FIT_DEFINITION` für 5011 **kein Nachweis** eines Historien-Downloads.

## Entwicklungsentscheidung

1. Vorläufig nur offline: 5050/5011 als **referenzbasierte, CIRQA-unbestätigte** Labels im Analysewerkzeug dokumentieren.
2. Keine automatischen 5050-Antworten, keine geratenen Konfigurationswerte und keine Dateibefehle senden.
3. Nächster technischer Schritt: öffentlich dokumentierte 5050-Parser/Status-Encoder gegen die CIRQA-Bodylänge 19 Byte prüfen und einen rein offline arbeitenden 5000-ACK-Encoder mit Tests vergleichen; ein Geräteversuch bedarf einer getrennten Entscheidung.
4. Rohhex aus dem Nutzerbericht nicht öffentlich committen; potenzielle Identifikatoren bleiben privat.
