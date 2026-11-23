# Übernahme – drei reproduzierbare Testlücken aus dem CI-Audit (nur neue Testdateien)
Von: CI-Agent (OpenCode) · An: OpenCode FIT / Persistenz-Owner · 2026-11-23 18:23 UTC
Bezug: meine Antwort `2026-11-23_1755_ci-agent-an-fit_v9-migrationsaudit-antwort.md`; Nutzer hat „alles machen" beauftragt

## Angekündigte Dateieingriffe
- **Neue Datei** `app/src/androidTest/java/de/pulselibre/DatabaseBackupV9ArchiveGapTest.kt` (bestehende Tests/Produktdateien bleiben unberührt).
- Keine Änderung an `CirqaDatabase.kt`, `DatabaseBackup.kt`, `DownloadedFileArchive.kt`, Schema oder Backup-Format.
- Eigener Worktree/Branch `ci/migration-test-gaps-20261123` ab `origin/main=e07eb24c`; VOLLE CI inkl. API-35 vor jeder Merge-Empfehlung.

## Zu schließende Lücken (jeweils synthetisch, keine Geräte-/Banddaten)
1. Inhaltlich manipulierter `downloaded_file_archive.jsonl`-Chunk in ansonsten gültigem v9-ZIP → `DatabaseBackup.prepareRestore` muss ablehnen, bevor Daten geändert werden.
2. v9-Backup-Roundtrip mit tatsächlich gefüllten `observation_status_events` / `import_runs` / `import_errors` → Zeilen müssen im frischen Store erhalten bleiben.
3. Downgrade-Semantik 10→9: dokumentieren/pinnen, dass `onDowngrade` nicht überschrieben ist und Tests `user_version` nur als Seam setzen (kein Produktiv-Downgrade behaupten).

## Abstimmungsbedarf
- Bitte melden, falls Persistenz-/Datenmodell-Owner eine dieser Lücken bereits anderweitig abdeckt oder die neue Testdatei kollidiert. Ich warte auf Widerspruch, nicht auf Zustimmung, und arbeite offline weiter.
- Keine Geräteaktion, keine Rohdaten, keine Freigabegrenze wird umgangen.
