# Rohdaten → Datenbank: der vollständige Verarbeitungspfad

> Stand 24.11.2026, statisch aus Code + Instrumentationstests belegt. Quellpfade relativ zu `app/src/main/java/de/pulselibre/` bzw. `app/src/androidTest/java/de/pulselibre/`. **Code ist maßgeblich** gegenüber älterer Doku (z. B. `DECODER_VERSION="2"`, nicht "1"; FIT-Originale seit Backup v9 im Backup). Keine Geräte-/BLE-Aktion, keine Testausführung in dieser Auswertung.

## 0. Kette auf einen Blick

```
BLE onCharacteristicChanged
 → CirqaMlHandshakeProbe.receive()                [Roh-Notification-Archivierung, #600]
 → ItemList-Parser / Pagination / transaction follow-ups
 → Pull + MLR-Transfer + CirqaMlrFileTransferAssembler [RAW/ZLIB, exakte Größe]
 → completeSyncFile() → mlrValidation()            [Typ/CRC/Größe/Verdict; Archivierung ZUERST]
 → DownloadedFileArchive.store()                   [verlustfreie Original-BLOBs, content-addressed]
 → CirqaVerifiedFitStore / CirqaVerifiedRawFileStore [app-private Datei-Originale]
 → FitHistoricalObservations.decode()              [Records, Sentinels, Review-Kandidaten, Rohfelder]
 → CirqaFitBackfill.importDecodedVerifiedFile()    [Re-Verifikation vor Commit]
 → CirqaDatabase.commitFitFile() → FitObservationCommitter.commit()
      ↳ EINE Transaktion: fit_sources, observations, provenance, generation-Tabellen,
        review candidates, unmapped fields, device metadata, sync_cursors
 → ImportAuditRecorder                             [import_runs/errors/status_events, #601]
 → DatabaseBackup.write/prepareRestore/restore     [Format v11, Restore v1–v10]
```

Zwei SQLite-Stores: kanonisch `cirqa_measurements.db` (**Schema v11**), Legacy `pulselibre_readings.db` (v3, nur Kopier-/Rollbackquelle).

## 1. Empfang und Roharchiv

- `CirqaMlHandshakeProbe.kt`: `onCharacteristicChanged` (~Z. 834–842) → `receive()` (~1536–1695) → `archiveRawNotification()` (~1521–1534). Nur bei `syncRequestMode && syncAccepted`; Deckeln 256 Notifications / 8 MiB pro Lauf; Diagnosefehler werden absichtlich verschluckt.
- `CirqaRawNotificationArchive.kt`: `archive()` mit festem lokalen Marker `0x4e 0x54 0x46` ("NTF"), **keine** Geräteadresse/UID; `count()` zählt Referenzen mit `type_label='notification'`. Rohbytes liegen im `DownloadedFileArchive`, nicht in `events.payload_json`; seit Backup **v9** im Backup. Test: `CirqaRawNotificationArchiveTest.rawNotificationsAreStoredOnceAndCounted`.

## 2. Protokoll-Parsing (fail-closed)

| Zweck | Klasse |
|---|---|
| Kanonisches Protobuf-Parsing | `CirqaFileAccessItemListParser.kt` — `fields()` ~59–99 (MAX_FIELDS=512, Varint-/Längenprüfung, keine Teilpräfixe), `parsePage()` ~105–141 (`complete=true` nur bei vollständigem Input), `parsePullResult()` ~144–168 |
| Sync-Antwort | `CirqaSyncResponseParser.kt` — genau ein `core_service(13)` mit genau einem `sync_response(2)`; **kein** Statuswert inferiert |
| Form-Inspektion | `CirqaProtobufShapeInspector.kt` — nur `field:wire`, keine Werte/IDs |
| Request-Builder | `GarminFileListPagination.kt` — `smartRequest(cursorId, startPageId)`: Cursor **oder** Page, nie beides |
| Seitenstatus | `CirqaItemListProgress.kt` — `FAILED / SESSION_PAGE / TRANSACTION_PENDING / COMPLETE` |
| Follow-ups | `CirqaItemListContinuationPolicy.kt` — `CONNECT_OBSERVED_TRANSACTION_FOLLOWUPS=3` (aus S10-HCI belegt), danach `TransactionLimitReached` |
| Kontext | `CirqaMlHandshakeProbe.kt` — `handleFileAccessResponse()`, `appendSyncItemListPage()`, `beginSyncContinuation()`, `requestNextSyncItemListPage()` |
| Fingerprint (nur Diagnose, lokal) | `CirqaItemListFingerprint.kt` — sortierte Item-Digests, keine rohen UID-Bytes im Output |

Tests: `CirqaFileAccessItemListParserTest` (truncated/oversized/field-count/next-transaction/uid-fail-closed), `CirqaItemListContinuationPolicyTest`, `CirqaItemListProgressTest`, `CirqaSyncResponseParserTest`.

## 3. Transfer, Frame- und Datei-Integrität

- `GarminFrameHeader.kt.inspect()` (~12–30): 6..65535 Byte, `length == packet.size`, Typ-Namen 5000/5002/5004/5011/5012/5024/5043/5044/5101; **keine** CRC hier.
- `GarminFrameValidator.kt`: `crc16Arc()` (~9–19, reflektiertes `0xA001`, init 0), `inspectVerified()` (~22–28) → `null` bei Längen-/CRC-Fehler.
- `GarminProtobufReassembler.kt.acceptMessage()` (~26–77): nur 5043/5044, `declaredTotal ≤ maxBytes`, Offset/Chunk-Konsistenz, duplikater erster Chunk ignoriert, Retransmission nur bei Byte-Gleichheit, Lücken → `rejected` + Reset.
- `CirqaMlHandshakeProbe.kt`: `sendNextSyncFile()` (~997–1043) mit `item.dataSize in 1..MAX_FILE_BYTES`; `shouldPullTargetedItem()` (~1121–1124): lokaler Hash beweist **keine** Remote-Freshness → Full-Pull-Regel; Configure-Antwort wird verworfen (~1574–1584).
- `CirqaMlrFileTransferAssembler.kt`: `acceptRaw()` (~112–119) exakt `expectedSize`; `acceptCompressed()` (~121–171) 2-Byte-Transportpräfix nur nach gültigem zlib-Header, Inflater, kein trailing, keine Dictionary.
- `CirqaSyncPipelineLedger.kt`: `Stage` `SELECTED…SUCCESSFUL/FAILED`, `record()` idempotent (~36–40), `failWriteIfNoResponse()` (~47–51).

Tests: `GarminFrameValidatorTest`, `GarminProtobufReassemblerTest`, `CirqaMlrFileTransferAssemblerTest`, `CirqaCompressedFitImportChainTest`.

## 4. Verdict und verlustfreie Archivierung (der Kern)

`CirqaMlHandshakeProbe.mlrValidation()` (~3165–3441), Reihenfolge exakt:

1. **Größe:** `data.size == syncExpectedSize == item.dataSize`, sonst `transfer=FAILED`.
2. **Formtrate:** `declaredFit` (Label `FIT_TYPE_*`) oder `.FIT`-Signatur → `FIT`, sonst `NON_FIT`/`UNKNOWN`.
3. **Original ZUERST archivieren** (~3209–3240): `CirqaDatabase.archiveDownloadedFile(data, item.referenceBytes, label, now)`. Auch unbekannte/status-only Bytes bleiben erhalten; Fehler → `privateStorage=FAILED`.
4. **FIT-Integrität:** `FitFileHeader.inspectVerified()`; zlib-Kandidat nur akzeptiert, wenn das Inflierte selbst FIT-CRC-bestätigt ist.
5. **Nicht-FIT:** `CirqaVerifiedRawFileStore.saveExact()`, optional `CirqaRawDeviceMetadataParser` → `raw_device_metadata`; Status `VERIFIED`.
6. **FIT deklariert, CRC ungültig:** Raw-Fallback, `integrity=FAILED`, `decode=NOT_REACHED`, **kein** Importerfolg.
7. **Gültiges FIT:** `CirqaVerifiedFitStore.saveVerified()` → `FitHistoricalObservations.decode()` → `importable = PROFILE_VALID && timestamp != null` → `CirqaFitBackfill.importDecodedVerifiedFile()`.

Typisiertes Verdict: `CirqaSyncFileCompletion.kt` — `Status { TRANSFERRED, VERIFIED, DECODED, IMPORTED, NO_IMPORTABLE_DATA, UNSUPPORTED, FAILED }`; `successfulForSync` (~56–60) zählt nur `VERIFIED/IMPORTED/NO_IMPORTABLE_DATA`; `StageCounts` (~69–111). Offline-Backfill: `CirqaFitBackfill.kt` — `FileStatus`, `FailureStage { READ, DATABASE, SOURCE_CONFLICT }`, Laufstatus `COMPLETED/PARTIAL`.

**Wichtig für Zähler:** `failed=20` im P0-Lauf 24.11. sind 20 dekodierte FITs, die im Import/Commit-Pfad scheiterten – nicht 20 nicht übertragene Dateien. Der konkrete Exception-Typ ist nicht belegt.

Tests: `CirqaSyncFileCompletionTest`, `CirqaFitBackfillTest`.

## 5. Originale und Dedup

- `CirqaVerifiedFitStore.kt.saveVerified()` (~30–66): verlangt CRC-validen Header, SHA-256-Dateiname `<sha>.fit`, privates nicht-Symlink-Verzeichnis, `fsync` + atomare `ATOMIC_MOVE`, vorhandenes Ziel byteverglichen (`ALREADY_PRESENT`), Prozess-Lock. `CirqaVerifiedRawFileStore.saveExact()` analog mit `<sha>.bin`.
- `FitFileHeader.kt.inspectVerified()` (~37–41): 12/14-B-Hülle, `.FIT`, `dataSize+headerSize+2 == file.size`, CRC-16/ARC über Header und Gesamtdatei == 0 → **Integrität, keine Authentizität**.
- `DownloadedFileArchive.kt`: `downloaded_files` PK `content_sha256 = SHA256(bytes)` → identische Bytes `INSERT OR IGNORE`; `downloaded_file_references` PK `(content_sha256, reference_sha256)` → gleiche Bytes mit anderem Item-Reference = zusätzliche Referenz. **Gleich große, aber verschiedene Dateien werden getrennt gespeichert.** `store()` verifiziert nach Insert Länge und chunkweises `substr(payload,…)` (CursorWindow-Schutz). Migration `archiveExistingDownloads()` (~114–136) übernimmt vorhandene `verified_fit/*.fit`/`verified_raw/*.bin` mit Hash-Gleichheit.
- Tests: `DownloadedFileArchiveTest` (u. a. `unknownOriginalsKeepBothSameSizeVersionsAndRoundTripBeyondCursorWindow`), `CirqaVerifiedFitStoreTest`, `CirqaVerifiedRawFileStoreTest`, `FitFileHeaderTest`.

## 6. FIT-Decoder und Generationen

- `FitHistoricalObservations.kt`: `DECODER_VERSION="2"`, `PROFILE_VERSION="21.222.0"`, `MAX_DECODED_FIELDS=1_000_000`. `decode()` verarbeitet nur validierte Bytes; `0xffff_ffff`-Timestamp verworfen; `timestamp_16`/komprimierte Zeit nur nach vollständigem Timestamp; unbekannte Felder → `UnknownField`-Inventar; **exakte Rohfeldbytes** → `DecodedField`. Klassifikation: `UNSUPPORTED_ENCODING`/`INVALID_SENTINEL` → `ExcludedField` (nur im Ergebnis, kein Wert in SQLite); `Quality { PROFILE_VALID, MISSING_TIMESTAMP, OUTSIDE_DISPLAY_RANGE }`; HR nur 1.0..240.0 PROFILE_VALID.
- Stabile ID: `sample_id = sha256("<fileSha>:<recordOrdinal>:<globalMessageNumber>:<fieldNumber>")` (~700); zusätzlich `duplicateFingerprint` über `metric:timestamp:valueBits` – Duplikate werden **geflaggt, nicht verworfen**.
- Review-Kandidaten (SpO₂/Stress/Atmung/Schlaf/Monitoring) bleiben `UNTRUSTED_PROFILE_CANDIDATE`.
- `FitDecoderGeneration.kt`: `id(sourceSha, decoderVersion, profileVersion)`, `outputDigest(rows, fields)` hasht **`rawBytes` inklusive Länge**; `activateFitDecoderGeneration()` (CirqaDatabase ~1143–1228) prüft `outputCount`, `decoded_field_count` **und** neu berechneten Digest. Keine automatische Aktivierung; erste Generation aktiv, spätere nicht.
- Replay: `replayVerifiedFitSource()` (~1030–1056), `storeFitDecoderReplay()` (~1063–1141) – verlangt registrierte Quelle + aktive Generation.
- Tests: `FitHistoricalObservationsTest`, `FitDecoderGenerationTest`, `HistoricalObservationRepositoryTest`.

## 7. SQLite-Commit (Persistenzkern)

Kette: `mlrValidation()` → `CirqaFitBackfill.importDecodedVerifiedFile()` (~258–274, Detail ~302–386) → `CirqaDatabase.commitFitFile()` (~975–995) → `FitObservationCommitter.commit()` (~7–356).

`importDecodedVerifiedFileDetailed()` verifiziert erneut: Datei existiert, keine Symlinks, `file.length == verifiedBytes.size`, Dateiname == SHA256, `readBytes().contentEquals`, FIT-Header CRC, alle Ergebnisse gleicher `sourceSha256`; nur timestamped `PROFILE_VALID` werden importiert.

`FitObservationCommitter.commit()` – **eine Transaktion** (~94–353):
1. `fit_sources` INSERT OR IGNORE + Byte-/Pfadgleichheits-`check()`;
2. `fit_review_candidates` + bitgenauer Wertvergleich;
3. `fit_unmapped_fields` Zähler-Update;
4. `fit_device_metadata` INSERT OR IGNORE + Whitelist-Check;
5. `observations` + `fit_observation_provenance` INSERT OR IGNORE, bei vorhandener `sample_id` bitgleicher Wert else Abbruch;
6. Generation-Tabellen `fit_decoder_generations`, `fit_generation_observations`, `fit_generation_fields`, `fit_active_generations` (INSERT OR IGNORE + check);
7. `sync_cursors` mit `MAX(last_committed_ms, excluded…)`;
8. `setTransactionSuccessful()`, `endTransaction()` in `finally`, `TrendCache.invalidate()`.

Rückgabewert = Anzahl **rekonzilierter Kandidaten**, nicht Zahl neuer Zeilen. Legacy-Kopie: `CirqaDatabase.ensureHistoricalStoreCopied()` (~776–961) mit bitgenauer Zeilenprüfung und `checkCountsMatch()`.

Tests: `CirqaFitBackfillInstrumentedTest`, `CirqaStorageMigrationTest` (u. a. `version10SchemaUpgradesTo11WithImmutableRawFieldTable`).

## 8. Schema v11 – Tabellen (kanonische DB)

| Gruppe | Tabellen |
|---|---|
| Ereignisse | `events` (`line_sha256 UNIQUE`, `payload_json`) |
| Messwerte | `observations` PK `(device_id, metric, measured_at_ms, sample_id)`, `sync_cursors`, `observation_quality`, `quality_status_catalog`, `measurement_modes` |
| FIT-Quellen/Provenienz | `fit_sources` (SHA-PK), `fit_observation_provenance`, `fit_device_metadata`, `raw_device_metadata` |
| Generationen | `fit_decoder_generations` (inkl. v11-Spalte `decoded_field_count`), `fit_generation_observations` (`timestamp_origin` DIRECT/RECONSTRUCTED_TS16/RECONSTRUCTED_COMPRESSED/UNKNOWN), `fit_generation_fields` (`raw_bytes BLOB`, `big_endian`, `base_type_code`), `fit_active_generations` |
| Inventar/Review | `fit_unmapped_fields`, `fit_review_candidates` (`UNTRUSTED_PROFILE_CANDIDATE`), `live_device_metadata` |
| Audit (#601) | `observation_status_events` (`idempotency_key UNIQUE`, `raw_code`, `timestamp_origin`), `import_runs` (`RUNNING/COMPLETED/PARTIAL/FAILED`), `import_errors` (`error_class`, `redacted_message`, `decision`) |
| Originale | `downloaded_files` (`content_sha256 PK`, `payload BLOB`), `downloaded_file_references` |
| Migration | `storage_migrations` |

`onUpgrade()` verlangt `oldVersion in 1..10 && newVersion == 11`; v10→v11 ergänzt `decoded_field_count` und `fit_generation_fields`. `onOpen()` (~696–707) ergänzt Tabellen/Spalten strukturell (`hasTable`/`hasColumn`), weil `user_version` allein kein Zustandsbeweis ist. Legacy `pulselibre_readings.db` v3 wird per `ATTACH … AS legacy` eingebunden.

## 9. Audit und Datenschutz

- `ImportAuditRecorder.kt`: `recordRun()` → `import_runs`; `recordFileError()` → `import_errors` mit `redacted_message = "per-file import failure; details in original archive"`, `decision FILE_SKIPPED/RUN_ABORTED`; `recordStatusEvent()` → `observation_status_events` mit `measured_at_ms=NULL`, `timestamp_origin=NOT_APPLICABLE`, `status_code=UNKNOWN`, `raw_code=NULL`. Produktaufrufer: `CirqaFitBackfill.kt` (~65–79, ~184–195), Injektion in `MainActivity.kt:131`.
- **Bekannte Lücke:** kein Produktionsschreiber befüllt `observation_status_events.raw_code` mit einem echten FIT-Sentinel; `ExcludedField`-Fälle bleiben nur im In-Memory-Ergebnis und als Rohbytes im Archiv. Rohcodes sind nicht als SQLite-Statuszeilen auffindbar.
- **Gespeichert:** beliebige empfangene Originalbytes, app-private `<sha>.fit/.bin`, rohe FIT-Feldbytes (`fit_generation_fields.raw_bytes`), nur `PROFILE_VALID`+timestamped Messwerte, Review-Kandidaten mit Rohwert, whitelisted Gerätemetadaten, Feldinventar (Signatur/Zähler), Zähler-/Fehlerklassen-Audit, gedeckelte Roh-Notifications.
- **Nicht gespeichert/redigiert:** Geräteadressen, Tokens, UID-Rohbytes (nur in-memory), Dateipfade/-namen, Seriennummern, rohe GATT-Protobuf-Payloads in `live_device_metadata`, erfundene Zeitstempel (`measured_at_ms=NULL` + `timestamp_origin=UNKNOWN/NOT_APPLICABLE`), Sentinel-Werte als physiologische Werte (HR 0, SpO₂ 255, RR −100/−200/−300), Werte/Hashes/IDs in Diagnose-Summaries.

## 10. Backup v11

`DatabaseBackup.kt`: `BACKUP_VERSION=11`, 128 MiB Archiv / 120 MiB Entry / 2 MiB Zeile / 1 M Records / `encryption="none"`. Entry-Sets kumulativ v1…v11; **v11 ergänzt `import_runs.jsonl`, `import_errors.jsonl`, `observation_status_events.jsonl`**; v9 hatte bereits `downloaded_file_archive.jsonl`, v10 `canonical_fit_generation_fields.jsonl`. `prepareRestore()` mappt 1–11, prüft exakte Entry-Sets, `app_id`, SHA/Größe/Records pro Entry, FKs und berechnet den Generations-Digest neu (inkl. Rohfelder). `restore()` ersetzt beide Stores in einer Transaktion, v11-Audit-Tabellen in einer zweiten. `extractArchive()` verbietet Pfadtraversen, unbekannte/doppelte Einträge. Tests: `DatabaseBackupTest`, `DatabaseBackupArchiveSafetyTest`, `DatabaseBackupV9ArchiveGapTest`.

## 11. Bewiesen vs. offen

**Bewiesen (Code + Tests):** Kette vollständig verdrahtet; Schema v11 + Upgrade 1..10→11; Backup v11/Restore v1–v10; Originale seit v9 im Backup; Idempotenz/Dedup auch bei gleicher Größe; ungültige FITs als Raw behalten aber nicht als Import; per-Datei-Fehler → `PARTIAL` mit Fortlauf; keine automatische Generationsaktivierung, Digest inkl. Rohfelder.

**Offen:** `failed=20` Exception-Typ (24.11.) und fehlendes Post-Run-Backup; leere letzte Transaction-Follow-up-Antwort; dedizierte Restore-Tests für v5/v6/v8/v10 im Checkout nicht gefunden (Codepfade vorhanden; Tests decken v1–v4, v7, v9, v11 ab); `raw_code`-Sentinel-Lücke; vollständige Gerätehistorie **nicht bewiesen** (`coverage=device-history-unverified`, `runDrained` = nur Queue dieses Laufs); semantische/klinische Validität aller FIT-Felder nicht bewiesen (Parser ist Allowlist, Unbekanntes wird inventarisiert).
