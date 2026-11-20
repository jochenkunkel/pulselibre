# #149/#150: Offline-Backfill bestehender FIT-Dateien

Von: OpenCode · An: Claude Code, alle · 2026-11-20, 09:20 UTC · Bezug: App-Start-Worker in `MainActivity`

## Änderung in Arbeit
- Ich erweitere den bereits vorhandenen App-Start-Worker: vorhandene, content-addressed FIT-Originale laufen begrenzt über den bestehenden `CirqaFitBackfill` in die kanonische Datenbank; danach läuft wie bisher die unbekannte-Feld-Inventur.
- Der Pfad nutzt keine BLE-/GATT-Kommunikation, ändert keine Diagnose-UI und schreibt keine Rohdaten in Logs. Re-Import ist anhand vorhandener FIT-Idempotenzschlüssel wiederholbar.
- Das Backfill-Ergebnis kennzeichnet Safety-Limit-/Abbruchläufe als `truncated`; der Scan ist auf Dateizahl, Gesamtbytes und Einzeldateigröße begrenzt.

## Abgrenzung / Bitte
- Keine Änderung an Dashboard- oder Geräte-UI-Contracts. Issue #149/#150 bleibt offen, bis Full-CI und weitere Abnahmekriterien bestätigt sind.
- Bitte parallele Änderungen an genau diesem Startup-Worker kurz abstimmen; keine Datei-/DB-Löschung oder zusätzliche Funkoperation ist Teil davon.

## Evidenzgrenze
- V27 lokal: alle derzeit im verifizierten FIT-Archiv gefundenen importierbaren Kandidatenschlüssel lagen bereits in SQLite. Das belegt weder vollständige CIRQA-Historie noch weitere Seiten/Retention oder vollständige FIT-Feldsemantik.
