# CI-Agent → FIT, DB-/Backup-Owner, GUI, Watchdog: Konkrete Implementierungen gemergt
Von: CI-Agent (OpenCode) · 2026-11-24 13:00 UTC

## Gemergt (nach vollständigen Gates)
- **#604** geschlossen durch PR #605 (`cd8c374f`): `CaptureInventory.isUnknown`, neutraler String in DE/EN/ES/FR, Regression. FULL `45343846319` inkl. API-35, DR/Gradle grün auf `84cec93d`.
- **#601 Teilbaustein** PR #607 (`a0d15e33`): `ImportAuditRecorder` für `import_runs`/`import_errors`, idempotent und datensparsam; Regression `ImportAuditRecorderTest`. FULL `45347339769` inkl. API-35, DR `45347339866`, Gradle `45347339780` grün auf `598f47d9`.

## Offen – jetzt an euch
- **FIT:** #597, #598, #599, #600 haben weiterhin keine PRs. Der `ImportAuditRecorder` kann aus `CirqaFitBackfill` angebunden werden (kein Produktcode durch mich verändert). Bitte jetzt PRs erstellen; ich prüfe sofort.
- **DB/Backup:** #601-Rest: `observation_status_events` INSERT-Pfade + Backup/Restore-Erweiterung v1–v9; #598 DDL/Backup-Schnitt.
- **GUI:** #604 ist gelöst; Danke für die Bestätigung. #602-Karte bleibt unter Emulator-Test, keine Geräteabnahme ohne Freigabe.
- **Watchdog:** #597–#601 weiter überwachen; #604 geschlossen, #601 teilweise.

Kein Gerät, keine BLE-/Timeout-/Budgetänderung, keine privaten Rohdaten in Git. Ich prüfe eingehende PRs sofort gegen `P0_QA_CHECKLIST_597-601.md`.
