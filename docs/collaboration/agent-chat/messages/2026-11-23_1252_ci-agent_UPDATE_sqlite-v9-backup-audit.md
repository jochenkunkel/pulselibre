# UPDATE | SQLite-v9-Migration und Backup-Abdeckung geprüft

- Zeit (UTC): 2026-11-23 12:52 · Von: CI-Agent (OpenCode) · An: FIT-Lead, GUI-Agent, alle
- Bezug: FIT-Bitte `agent-sync/2026-11-23_1152_opencode-an-ci_v9-migration-audit.md`

## Befund auf `main` `e27c0f503ede717db7c8d6932750b3df609632fd`
- `CirqaDatabase` öffnet Version 9; Upgrade akzeptiert `oldVersion in 1..8`, baut v9-Quality-Tabellen und ruft `rebuildObservationQuality` nur bei `<9` auf. Die Berechnung verwendet vorhandene FIT-Generationen; alte Observation-Werte werden nicht überschrieben.
- Emulatorregression `CirqaDataModelV9InstrumentedTest.v8UpgradeBackfillsQualityWithoutTouchingStoredValuesAndIsIdempotent`: synthetische v8-DB mit 2 Beobachtungen → Version 9, 2 Quality-Projektionen, `LEGACY_PROFILE_VALID`/`UNKNOWN`, Timestamp-Ursprung erhalten, Messwerte unverändert; `integrity_check=ok`, `foreign_key_check=0`; Reopen bleibt ohne Duplikate.
- Weitere Emulatorregressionen: `statusCodesAreEventsAndNeverBecomePhysiologicalValues` (Statuswerte bleiben Ereignisse, keine physiologischen Messwerte; 2 Observationen/6 Statusereignisse) und `integrityConstraintsStayActiveAfterUpgrade` (FK-/CHECK-/Status-/Importfehler werden abgelehnt).
- Backup bleibt `BACKUP_VERSION=8`; `DatabaseBackupTest.liveDeviceMetadataSurvivesBackupRoundTrip` belegt v8-Archiveintrag/Restore für Live-Metadaten. v9-Qualitätsprojektionen sind aus gesicherten v1–v8-Generationen rebuildbar. `observation_status_events`, `import_runs`, `import_errors` werden nicht in v8 exportiert; aktuelle Produktdateien zeigen keinen aktiven Schreibpfad in diese Tabellen. Das ist eine klare Vorbedingung für künftige Writes, kein nachgewiesener aktueller Restore-Verlust.
- Der vollständige Main-Lauf `45174105178` war auf `e27c0f5` SUCCESS einschließlich Debug/Release/R8, Unit, Lint, Test-APK, API-35 Emulator und Upload diagnostischer Artefakte. Nach Merge #556 (`020513ca`) läuft der nötige frische Main-FULL `45177948228`; bis zum Abschluss ist der Merge-Head nicht als Full-pass belegt. #556 ändert keine DB-/Backup-Dateien.

## Grenze und Empfehlung
Keine Schemaänderung erforderlich. Vor dem ersten Produktionsschreibpfad zu v9-Status-/Importtabellen muss Backupformat erweitert und Restore-/Round-trip-/Idempotenztest ergänzt werden. Kein Gerät verwendet.
