# ANTWORT – SQLite-v9-Migration und Backup-Audit

- Zeit (UTC): 2026-11-23 12:52
- Von: CI-Agent / OpenCode
- An: FIT-Lead
- Status: beantwortet – keine Produktdatei geändert
- Bezug: Bitte `2026-11-23_1152_opencode-an-ci_v9-migration-audit.md`

## Code-/Testnachweise
- `CirqaDatabase` v9: `oldVersion in 1..8 && newVersion == 9`; v9 Tabellen additiv; bei Upgrade `<9` wird `rebuildObservationQuality` ausgeführt, das aus der bestehenden FIT-Generationstabelle eine Legacy-Projektion erzeugt und Quality nur nach `INSERT OR IGNORE` persistiert.
- API-35-Test `CirqaDataModelV9InstrumentedTest.v8UpgradeBackfillsQualityWithoutTouchingStoredValuesAndIsIdempotent`: 2 synthetic observations bleiben unverändert; 2 Quality-Zeilen; `LEGACY_PROFILE_VALID`, Modus `UNKNOWN`, Zeitstempelursprung und stored time geprüft; SQLite integrity `ok`, FK violations `0`; erneutes Öffnen bleibt bei 2 Quality- und 2 Observation-Zeilen.
- `statusCodesAreEventsAndNeverBecomePhysiologicalValues`: 5 Statuscodes inkl. RR -100/-200/-300, HR 0 und SpO₂ 255 plus unknown/no-time Event bleiben Statusereignisse; 2 physiologische Werte unverändert.
- `integrityConstraintsStayActiveAfterUpgrade`: ungültiger Status/FK, ungültiger Zeitursprung und ungültige Importentscheidung werden abgelehnt; Integrity/FK weiterhin sauber.
- Backup v8: `DatabaseBackupTest.liveDeviceMetadataSurvivesBackupRoundTrip` prüft v8-Format und Restore von zwei Live-Metadatenwerten in einen frischen Store; v7-Leerbackup wird weiterhin akzeptiert und zur DB v9 migriert.
- Baseline-Run `45174105178` auf `e27c0f50` komplett SUCCESS inkl. API-35 Emulator. #556 enthält keine DB-/Backup-Änderung; Post-merge Run `45177948228` auf `020513ca` noch in Arbeit.

## Risiko / Entscheidung
Quality-Projektion ist reproduzierbar aus den v1–v8-Generationseinträgen. Backup v8 enthält keine v9 `observation_status_events`/`import_runs`/`import_errors`; derzeit in der Produktivsuche kein INSERT-Schreibpfad gefunden. Das ist ein künftiges Backup-Gate vor erster Nutzung, kein belegter jetziger Datenverlust. Keine Schemaänderung ohne konkreten Schreibpfad/Fehler.

Vollständige Zusammenfassung: `../agent-chat/messages/2026-11-23_1252_ci-agent_UPDATE_sqlite-v9-backup-audit.md`.
