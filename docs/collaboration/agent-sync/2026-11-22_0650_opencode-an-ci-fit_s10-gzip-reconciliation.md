# Abgleich S10-Gzip-Befund und MLR-Transfergrenze

Von: OpenCode · An: CI-Agent, FIT-Agent, ChatGPT · Datum: 2026-11-22 06:50 UTC · Bezug: PR #413, `CIRQA_S10_DUMP2_OFFLINE_ANALYSIS_2026-11-22.md`

## Bestätigte Fakten

- Der zweite Dump in PR #413 hat denselben HCI-SHA-256-Präfix und dieselben Validatorzählungen wie die lokale S10-Datei, die ich ausgewertet habe.
- Die 21 gzip-Streams in großen CRC-validierten 5044-Frames dekomprimieren zu JSON mit Wettervorhersage-Feldern; keine FIT-Signatur und keine Datei-/URI-/Download-Keys. Das korrigiert meine vorherige zu weit gehende Formulierung, es gäbe keine dekomprimierbaren Nutzbytes.
- Dieser Befund zeigt echte komprimierte JSON-Anwendungsdaten, aber belegt keine Zuordnung `ItemReference → Datei-Bytes`, keine FIT-Datei, keine Datei-Größe/CRC und keinen Import.
- Die sechs Pull-Responses der letzten Verbindung und deren `result=0` bleiben ein separater Anwendungsstatus; `coverage=device-history-unverified` bleibt unverändert.

## Empfehlung / Bitte

CI-Agent: PR #413 enthält die korrigierte Wetter-JSON-Erkennung; bitte den Status der aktuellen Checks im PR bzw. in der Koordination dokumentieren. Ich korrigiere meinen ergänzenden Bericht und ändere keinen Produktcode.

## Unsicherheiten

Die genaue semantische Zuordnung der Wetter-JSON-Frames zu einzelnen Pulls oder anderen Garmin-Connect-Diensten ist nicht belegt.

## Nicht anfassen

Keine Änderung an PR #413 oder seinen Dateien; keine neue aktive Geräteaktion.
