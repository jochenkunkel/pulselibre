# CIRQA 5024 – Feldanalyse der veröffentlichten fenix-8-Referenz

Quelle: https://github.com/mskerwiderski/GarminHRM600/blob/main/hrm600/bootstrap.py

Der dort dokumentierte 52-Byte-Frame ist ein beobachteter fenix-8-Watch-Response an einen HRM 600. Keine CIRQA-Validierung.

| Offset im Frame | Bytes | Beobachtung | Sicherheit |
|---|---:|---|---|
| 0–1 | 2 | GFDI-Gesamtlänge 52 | Hoch |
| 2–3 | 2 | GFDI-Typ 5000 (Response) | Hoch |
| 4–5 | 2 | Originaltyp 5024 | Hoch |
| 6 | 1 | Status 0 | Hoch |
| 7–18 | 12 | Binärwerte, Semantik unbekannt | Nur Abgrenzung |
| 19 | 1 | Länge 14 des folgenden Textfeldes | Beobachtet |
| 20–33 | 14 | Modelltext der fenix 8 | Hoch |
| 34 | 1 | Länge 5 des folgenden Textfeldes | Beobachtet |
| 35–39 | 5 | weiterer Modelltext | Hoch |
| 40 | 1 | Länge 8 eines dritten Textfeldes | Beobachtet |
| 41–48 | 8 | weiterer Modelltext | Hoch |
| 49 | 1 | Unbekanntes Schlussbyte | Nur Abgrenzung |
| 50–51 | 2 | CRC16 | Hoch |

**Wichtige Korrektur gegenüber der ersten Analyse:** Die 43 Erweiterungsbytes enthalten **drei** längenpräfixierte Textfelder (14, 5, 8 Byte), nicht nur zwei. Die erste Bytegruppe ist 12 Byte lang. Ein strukturierter Offline-Parser und Test sollten alle drei Textfelder prüfen. Keine kopierten Watch-Identitätsdaten an CIRQA senden.

Nächster Schritt: unabhängige Feldsemantik aus öffentlicher Referenz oder validiertem Client ableiten. Erst dann einen expliziten, begrenzten Gerätetest mit CIRQA-eigenen Angaben aktivieren.

## Verifikationsstand und Entscheidung

- Der Offline-Parser prüft CRC, Response-Typ, Originaltyp, Status, drei ASCII-Längenfelder und Grenzen. Negative Unit-Tests für ungültige Textlänge und CRC sind ergänzt.
- Die zwölf Binärbytes sind **nicht** unabhängig semantisch dokumentiert. Keine Interpretation als Device-ID, Capability-Maske oder Versionsnummer ohne Evidenz.
- Die Referenz zeigt eine vollständige fenix-8-Identität; das Nachspielen dieses Captures wäre keine CIRQA-native Implementierung.
- **Entscheidung:** Kein zusätzlicher aktiver BLE-Testmodus mit willkürlichen 5024-Erweiterungen. Erst ein überprüfbares Feldschema oder ein unabhängiger funktionierender CIRQA-Handshake rechtfertigt die Freigabe.
- Alternativer nächster Forschungsansatz: Auswertung des bereits vorhandenen CIRQA-5024-Frames und der drei 5050-Frames auf Frame-Typ, Sequenz/Counter und strukturierte Metadaten; keine weitere Samsung-HCI-Anforderung an den Tester.
