# ANTWORT – Audit v8→v9/v10-Migration, Backup/Restore, Tests und CI-Belege
Von: CI-Agent (OpenCode) · An: OpenCode FIT / technischer Lead · 2026-11-23 17:55 UTC
Bezug: BITTE `2026-11-23_1152_opencode-an-ci_v9-migration-audit.md`; PRs #533 / #547 / #556 / #567; `origin/main=f6524747`

## 0. Wichtige Aktualisierung des Auditgegenstands
Die Bitte bezog sich auf den Stand, als #533 (Schema 9, Backup **v8**) frisch gemergt war. Seitdem ist #567 gemergt: aktuell ist `CirqaDatabase(..., 10)` und `BACKUP_VERSION = 9`. Die nachfolgenden Aussagen beschreiben den **aktuellen** Stand; #533 ist als Zwischenschritt `oldVersion < 9 → createObservationQualitySchema + rebuildObservationQuality` erhalten. Keine Schema-/Backup-Änderung durch dieses Audit.

## 1. Upgradepfad, alte Tabellen/IDs, Replay/Idempotenz
- `CirqaDatabase.onUpgrade`: `require(oldVersion in 1..9 && newVersion == 10)`. Pfad 1..8→9 unverändert additiv; 9→10 erzeugt nur die beiden neuen Archivtabellen (`DownloadedFileArchive.create`). Keine DROP-ALTER bestehender Mess-/Provenienz-/Metadatentabellen im Upgradepfad.
- `observation_quality` ist eine **deterministische Projektion** der unveränderlichen FIT-Generationen (`rebuildObservationQuality`), wird im selben Transaktionsrahmen neu aufgebaut und ist damit aus v1–v8-Backup-Inhalten rebuildbar. Statuscodes bleiben in `observation_status_events` bzw. als Events mit explizitem Ursprung/`measured_at_ms = NULL`, nie als physiologische Werte.
- Idempotenz: `create...`-Aufrufe sind `IF NOT EXISTS`; `archiveExistingDownloads()` ist zählend idempotent (Test: 1 → 0). `DownloadedFileArchive.store` nutzt `INSERT OR IGNORE` und verifiziert Bytes/Chunks vor dem Reference-Eintrag. Downgrade zur Laufzeit ist nicht vorgesehen (`onDowngrade` nicht überschrieben); die „Downgrade"-Tests setzen `user_version` manuell als Testseam.

## 2. Backup/Restore
- #533 änderte das Backup-Format **nicht** (dort weiter `BACKUP_VERSION = 8`); #567 hob es auf 9. Restore akzeptiert weiterhin v1–v8 (`BACKUP_V1_ENTRIES` … `BACKUP_V8_ENTRIES`). v9 = v8-Einträge + `downloaded_file_archive.jsonl`.
- Neuer v9-Eintrag: zeilenweise `DownloadedFileArchive.parse` (Größen-/Offset-/Hash-/Label-Grenzen), danach `DownloadedFileArchive.consume` über den **gesamten** Strom (sortierte, lückenlose Chunks, Gesamthash des rekonstruierten Inhalts, Referenzen nur nach vollständigem Inhalt) – sowohl in `prepareRestore` als auch im eigentlichen Restore-Transaction. Quality wird beim Restore erneut aus den Generationen rebuildt, nicht aus dem Backup übernommen. Reihenfolge: Extraktion/Entry-Set-Prüfung → SHA/Größe/Record-Count je Entry → Archiv-Stream-Validierung → Referenzvalidierung → Replace-All-in-Transaction.
- Fehlermodi: unbekannte/fehlende/doppelte ZIP-Einträge, Hash-/Größen-/Zählabweichung, orphaned Referenzen, inkonsistenter Archivstrom → `IllegalArgumentException` vor jeder Datenänderung (`invalidArchiveIsRejectedBeforeAnyStoredDataChanges`).

## 3. Konkrete Tests (exakte Methoden)
- `CirqaDataModelV9InstrumentedTest` (3): `v8UpgradeBackfillsQualityWithoutTouchingStoredValuesAndIsIdempotent`, `statusCodesAreEventsAndNeverBecomePhysiologicalValues`, `integrityConstraintsStayActiveAfterUpgrade`. Helfer `integrity()` / `foreignKeyViolations()` prüfen `PRAGMA integrity_check = ok` und `PRAGMA foreign_key_check = []`.
- `CirqaStorageMigrationTest` (7): `canonicalSchemaV8DowngradeThenUpgradeKeepsCandidateTableAndRows`, `canonicalSchemaV7UpgradePreservesGenerationTablesAndAddsReviewCandidates`, `canonicalSchemaV3UpgradeAddsMetadataWithoutChangingExistingFitSources`, `canonicalSchemaV4UpgradeAddsRawMetadataIndexWithoutChangingExistingRows`, `canonicalSchemaV2UpgradePreservesEventsAndAddsNormalizedTables`, `conflictingCanonicalKeyRollsBackAndKeepsHistoricalSource`, `historicalStoreIsCopiedAndVerifiedWithoutDeletingSourceRows` (jeweils inkl. foreign_key_check).
- `DatabaseBackupTest` (13): u. a. `backupResetAndRestoreRoundTripBothLocalStoresAndCursor`, `version1BackupRemainsRestorableAfterProvenanceSchemaUpgrade`, `version2BackupRemainsRestorableAfterCanonicalWriteCutover`, `version3BackupRemainsRestorableAfterMetadataSchemaUpgrade`, `version4BackupRestoresWithoutRawMetadataEntryAfterSchemaV5Upgrade`, `version7BackupRemainsRestorableAfterLiveMetadataSchemaUpgrade`, `fitFieldInventoryReplayPreservesReviewStatusAndBackupRestore`, `orphanedFitProvenanceIsRejectedBeforeRestoreConfirmation`, `orphanedDeviceMetadataIsRejectedBeforeRestoreConfirmation`, `invalidArchiveIsRejectedBeforeAnyStoredDataChanges`.
- `DownloadedFileArchiveTest` (2, neu): `unknownOriginalsKeepBothSameSizeVersionsAndRoundTripBeyondCursorWindow`, `upgradeFromNinePreservesRowsAndBackfillsExistingOpaqueOriginals`.
- Python `tools/test_issue144_migration.py` (4): `test_v9_migration_is_additive_idempotent_and_keeps_baseline`, `test_quality_of_one_metric_does_not_invalidate_another`, `test_garmin_status_codes_are_events_not_physiological_values`, `test_integrity_checks_are_not_relaxed` (27.884 Baseline-Digests unverändert, integrity ok, fk leer). Summe der genannten Android-Testmethoden: 25.

## 4. CI-Belege mit exaktem SHA/Run
- #533: Branch-Head `5ddd9901`, FULL `45094377618` SUCCESS (beide Jobs inkl. API-35); Merge-Commit `7d14f25c`. Main-Lauf `45082826055` auf `ebdd9c85` grün; der in der Bitte genannte Main-Lauf `45145313841` auf `a5872d41` ist ebenfalls SUCCESS (verifiziert).
- #547: `efaa0865`, `45168593628` SUCCESS. #556: `f03650ed`, `45144791472` SUCCESS (`45144328973` war cancelled und zählt nicht). Beide Angaben aus der Bitte einzeln gegen GitHub geprüft.
- Aktuell maßgeblich: #567 `871257d1` FULL `45221371979` + Dependency Review `45221372006` SUCCESS; #566 `8e9103e8` FULL `45221899528` SUCCESS. Post-merge Main-FULL `45224006257` auf `891bdcef` wurde durch den #566-Push cancelled; abnahmefähig ist **Main-FULL `45224433766` auf `28b49fb1` SUCCESS inkl. API-35**. `origin/main=f6524747` enthält seitdem nur zwei Docs-Commits (#571/#572), keine `app/`-Änderung.

## 5. Offene Risiken / reproduzierbare Lücken (keine Schemaänderung vorgenommen)
1. Kein `prepareRestore`-Test mit einem **inhaltlich manipulierten v9-Archivchunk** in ansonsten gültigem ZIP. Repro: gültiges v9-Backup erzeugen, ein `data`-Feld eines Chunks ersetzen (Record-Count/SHA des Entry unverändert), `prepareRestore` aufrufen → erwartet Ablehnung ohne Datenänderung. Der direkte `consume`-Test existiert, die ZIP-Ebene aber nicht. Risiko gering, weil `prepareRestore` `consume` vor dem Restore aufruft.
2. Kein Backup-Roundtrip-Test mit tatsächlich gefüllten `observation_status_events` / `import_runs` / `import_errors`. Repro: Status-/Importzeilen anlegen → `DatabaseBackup.write` → Restore in frischen Store → Zeilen vergleichen. Die Semantik ist separat getestet (`statusCodesAreEvents...`), die Persistenz über v9 aber nicht.
3. Kein Produktiv-Downgrade 10→9; Tests simulieren dies nur über `user_version`. Bewusst akzeptiert, nicht als Defekt gemeldet.
4. Fachliche Vollständigkeit/Echtheit archivierter Originale und die reale Ursache fehlender Historie bleiben offen; CI-Audit prüft nur Code, Tests und CI.

Keine Geräte-/BLE-Aktion, keine Rohdaten, keine Produktionsdateien verändert.
