# Übergabe: unabhängiges CI-/Schema-Review für vollständige FIT-Feldgenerationen
Von: OpenCode FIT · An: CI-Agent (unabhängiger Reviewer), Projektleitung · 2026-11-24 07:28 UTC
Branch: `opencode/p0-fit-field-persistence-20261124` · Head bei Nachricht: `3d0aaead`
Bezug: P0-Owner-Entscheidung #595 (`docs/DATA_ACQUISITION_PRIORITY.md`), FIT-Antwort zur Erfassungskarte #606, Nutzerauftrag P0 „alle Banddaten dauerhaft in SQLite“.

## Zuständigkeit
- OpenCode übernimmt in diesem Branch den FIT-Decoder-/Datenpfad, additive SQLite-v11-DDL/Migration, generation-scoped Rohfelder, Backup-v10/Restore und synthetische Tests. Kein separater aktiver DB-/Backup-Agent war im Watchdog ausgewiesen; ich habe die DDL/Backup-Verantwortung deshalb in diesem isolierten Branch konsolidiert.
- CI-Agent wird um **unabhängiges** Review des konkreten Branch-Heads gebeten: P0-Datenverlustprüfung, Schema-/Migration v10→v11, Generation-Digest/Replay/Aktivierung/Rollback, Backup-v10 sowie Restore v1–v9. Bitte konkrete Befunde datei-/testbezogen dokumentieren und bei erfolgreichem Review FULL/API-35 auf dem Branch-Head ausführen oder bestätigen.
- GUI-Agent bleibt Owner der Anzeige; keine UI-Datei geändert. `docs/DATA_ACQUISITION_PRIORITY.md` ist die maßgebliche Produktentscheidung; die neue `docs/data/COMPLETE_BAND_DATA_PERSISTENCE.md` beschreibt den FIT-Implementierungsvertrag.

## Änderungen
- Originaldatei-Archiv bleibt unverändert als bytegenaue, gehashte Quelle in SQLite `downloaded_files`.
- Decoder v2 erfasst Standard-, Developer- und komprimierte Timestamp-Feldbytes; jede Feldzeile erhält Quell-/Generationskoordinaten, BaseType/Größe/Endianness soweit vorhanden sowie Zeitstempelherkunft.
- Unveränderliche Generationen speichern Beobachtungen plus Feldbytes. Der Output-Digest prüft beides; Replay bleibt inaktiv und Aktivierung/rollback validiert die komplette Generation transaktional.
- SQLite 11, `fit_generation_fields`, Backup 10 mit Feld-JSONL/Base64 und Restore-Unterstützung v1–v9.
- Review-Kandidaten (SpO₂-Zusatzfelder, Stresszeit, Schlaf-Level, Monitoring-Zyklen/Aktivität) werden nicht wegen medizinischer Vertrauenswürdigkeit verworfen; semantische Projektionen bleiben getrennt.

## Nachweise bisher
- JVM: `:app:testDebugUnitTest` erfolgreich (1.011 Tests im letzten vollständigen Lauf).
- Emulator API 35: 171 Instrumentationstests, 0 Fehler; enthielt v10→v11-Migration, FIT-Generation-Replay/Rollback und Backup-v10-Roundtrip mit synthetischem Rohfeld.
- FULL-Aufruf ist nach einem vorherigen Allowlist-/erwartete-Backupversion-Testfehler erneut gelaufen; letzter Emulatorlauf ist grün. Nach Aufnahme des komprimierten Timestamp-Headers folgen kompletter FULL-Lauf und CI-Review auf diesem neuen Head.

## Bitte / Übergabe
- CI-Agent: unabhängiger Review und FULL/API-35 für den final gepushten Branch-Head; ausdrücklich auch prüfen, dass Restore v1–v9 alte Generation-Digests erhält und Backup v10 das Feldarchiv bytegenau roundtript.
- Projektleitung: keine Rückfrage offen; Änderung bleibt lokal und privacy-safe, ohne Hardware-/BLE-Test.
