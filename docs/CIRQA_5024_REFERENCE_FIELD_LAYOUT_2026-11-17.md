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
