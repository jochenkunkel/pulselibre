# ENDE – #637 Backfill-Fehlerprovenienz offline geprüft
- Zeit (UTC): 2026-11-26 10:24
- Von: OpenCode FIT
- Status: abgeschlossen, teilweise – Diagnosegrenze belegt; historische Fehlerursache bleibt unbekannt
- Ergebnis: `CirqaFitBackfill` speichert den aggregierten Fehlerzähler als `failed + invalid`. Invalid outcomes werden nicht als `import_errors`-Zeilen abgelegt. Geworfene Fehler werden mit `source_sha256=null` erfasst; Datei-Aliase sind nicht in `import_errors` persistiert. Der Startup-Maintenance-Aufrufer verwirft `Result` und schluckt Top-Level-Fehler. Daher lassen sich die drei historischen Zähler weder konkreten Dateien noch Ursachen zuordnen; sie könnten invalid outcomes enthalten, das ist nicht bewiesen.
- Evidenzgrenze: Separat sind 25/25 CRC-valide FIT-Streams aus dem manuellen Run inhaltlich bereits im Vorher-Archiv vorhanden; der Run brachte keine neuen FIT-Bytes. Das erklärt nicht, ob schon persistierbare Beobachtungen durch Importfehler ausblieben. Keine Behauptung vollständiger Historie oder Garmin-Connect-Parität.
- Issue-Nachweis: [#637 Code-Audit](https://github.com/jochenkunkel/pulselibre/issues/637#issuecomment-7189212650)
- Tests: Keine Backfill-Tests ausgeführt (read-only Quelltextaudit); Replay-Regression separat 22/22 bestanden. Keine Code-/Schemaänderung.
- Sicherheit: Keine privaten DB-Inhalte/Hashes/Namen/Gesundheitsdaten veröffentlicht; kein Geräte-/BLE-Zugriff.
- Nächster Schritt: Bei einem zukünftigen kontrollierten Lauf per-file Audit mit begrenzten festen Fehlerklassen und privatem content hash erwägen. Für bereits vergangene drei Zähler gibt es keine rekonstruierbare Provenienz.
