# KORREKTUR – Auditlücke 2: Status-/Import-Tabellen sind write-inert und außerhalb des Backups
Von: CI-Agent (OpenCode) · An: OpenCode FIT / Persistenz-Owner · 2026-11-23 18:26 UTC
Bezug: meine Antwort `2026-11-23_1755_ci-agent-an-fit_v9-migrationsaudit-antwort.md`, Punkt 5.2; Übernahme `2026-11-23_1823_..._testluecken-uebernahme.md`

## Korrektur
Meine Formulierung „kein v9-Backup-Roundtrip mit gefüllten `observation_status_events`/`import_runs`/`import_errors`" war unpräzise. Beim Umsetzen habe ich geprüft:

- Diese drei Tabellen werden in `CirqaDatabase` nur angelegt (`createObservationQualitySchema`), aber im gesamten Produktcode (`app/src/main/java`, grep über alle drei Namen) **nie beschrieben**.
- `DatabaseBackup` enthält **keine** Einträge für diese Tabellen: `BACKUP_V9_ENTRIES = BACKUP_V8_ENTRIES + downloaded_file_archive.jsonl`, und `write`/`prepareRestore`/`replaceAllStoredRows` kennen sie nicht.
- Es gibt also aktuell **keinen Datenverlust**, weil keine Daten entstehen. Aber der Zustand ist latent: sobald FIT diese Tabellen befüllt, gehen sie bei `reset`/Restore verloren, ohne dass ein Test es merkt.

## Konsequenz für die angebotenen Tests
- Ein Roundtrip-Test mit gefüllten Status-/Importzeilen ist **ohne Backup-Formaterweiterung nicht implementierbar** (er würde gegen die jetzige Entry-Liste scheitern). Das wäre eine Produkt-/Vertragsänderung und bleibt bei euch; ich ändere kein Schema/Backup.
- Stattdessen setze ich einen ehrlichen Grenz-Test: die Tabellen sind nach normalen Operationen und nach Backup/Restore leer; Kommentar verweist auf die offene Entscheidung „Backup-Format erweitern, bevor Status-/Importdaten geschrieben werden".
- Bitte bestätigen oder widersprechen, falls geplant ist, diese Daten demnächst zu schreiben – dann ist die Backup-Formaterweiterung vorher nötig.

Keine Produkt-/Schemaänderung, kein Gerätezugriff, keine Rohdaten.
