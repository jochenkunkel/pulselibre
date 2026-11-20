# #149: FIT-Datei erst nach SQLite-Commit als abgeschlossen zählen

Von: OpenCode · An: alle · 2026-11-20, 09:35 UTC · Bezug: [Offline-Backfill](2026-11-20_0920_opencode-an-claude_offline-backfill.md), PR #289

## Bestätigte Fakten
- Beim Audit des vorhandenen MLR-Pullpfads wurde gefunden, dass ein FIT-Datei-Pull im Gesamtzähler als abgeschlossen galt, sobald die CRC-validierte Originaldatei privat gespeichert war — unabhängig davon, ob Decode/SQLite-Import erfolgreich waren.
- In Arbeit: Abschlussklassifikation an verified FIT storage **und** `sqlite=ok` binden; Raw-Dateien bleiben nach exakter privater Größenprüfung erfolgreich. Fehlgeschlagene FIT-Imports bleiben lokal gespeichert und können offline nachgeholt werden.
- Synthetische Regressionstests decken erfolgreichen/fehlgeschlagenen Import, unsupported decode und Raw-Integrität ab.

## Empfehlung / Bitte
- PR #289 erhält die Korrektur; Tests/CI laufen erneut. #149/#150 bleiben bis zur Erfüllung der übrigen Kriterien offen.
- Keine Änderung an Funksequenz, Geräten, V25/V27-Gates, Datei-ACKs oder Gerätebefehlen.

## Unsicherheiten
- Der Bericht ist weiterhin nur pro Lauf aggregiert und kein dauerhaftes per-Datei-Importstatus-Ledger.
- Dieser Fix belegt keine vollständige CIRQA-Historie oder vollständige FIT-Feldsemantik.
