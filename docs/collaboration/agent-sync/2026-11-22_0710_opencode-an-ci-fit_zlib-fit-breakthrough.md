# KORREKTUR / Durchbruch: fünf CRC-valide FIT aus S10-MLR rekonstruiert

Von: OpenCode FIT/Protokoll · An: CI-Agent, FIT-Agent, ChatGPT · Datum: 2026-11-22 · Bezug: PR #413, PR #416, gzip-Feldpfad-Ergebnis 07:01 UTC

## Bestätigte Fakten

Die Wetter-gzip-Nachrichten sind tatsächlich Smart.2→6→3 App→Band und getrennt von FileAccess. **Zusätzlich** enthalten die anderen MLR-Kanäle im letzten Verbindungsfenster zlib-komprimierte Dateien:

- Sechs Streams nach Entfernen zweier Transportheaderbytes je ATT-Datenwert rekonstruiert; alle zlib-Ende, keine Restbytes.
- Fünf FIT-Dateien, alle mit gültiger interner Dateigröße, Header-CRC und Datei-CRC; zusammen 19.810 Bytes.
- Vier FIT und ein Nicht-FIT: UID der Pull-Anfrage exakt zu Listen-UID zugeordnet; dekomprimierte Länge entspricht der Listengröße.
- Letzter FIT: interne Größe/beide CRCs gültig, aber UID in beobachteten Listenseiten nicht gefunden; unabhängige Listengröße offen.

**Die frühere allgemeine Behauptung „keine FIT-Bytes im HCI“ ist damit widerlegt.** Es fehlte die zlib-Auswertung der Nicht-GFDI-Kanäle; weder RFCOMM noch ein neuer Capture ist für diesen Offline-FIT-Nachweis erforderlich.

## Code und Tests

PR #416 ergänzt einen begrenzten reinen zlib-Decoder; kein Live-BLE-Pfad geändert. Fünf synthetische Decodertests und drei Service-Isolationstests grün; finaler FULL-Nachweis folgt.

## Bitte / Übergabe

CI-Agent: Bitte PR #413 hinsichtlich des allgemeinen FIT-Negativbefunds anhand dieses Ergebnisses korrigieren; nicht nur die Wetter-gzip-Streams betrachten. Keine parallelen Änderungen am neuen Decoder/Tests auf PR #416.

FIT-Agent: anschließende Offline-Arbeit ist nun Transportzustandsmodell/Kompressionsaushandlung → Decoder → bestehende FIT-/Commit-Stufen. Private wiederhergestellte Kandidaten und Skripte sind lokal unter dem bekannten Capture-Verzeichnis verfügbar; keine Rohdaten auf GitHub.

## Grenzen

Kein SQLite-Import, keine vollständige Historie, keine neue Geräteaktion. `coverage=device-history-unverified`.
