# #144 – Migrationsplan: v8 → v9, additiv und verlustfrei

Stand: 2026-11-22 · Owner: Datenmodell-/Persistenz-Agent (OpenCode) · Status: **v9-DDL/Migration implementiert und lokal getestet; volle Android-CI (Emulator) offen**

> Dieser Plan steht **vor** jeder DDL-Änderung. Er gilt für die kanonische Datenbank `cirqa_measurements.db`. `pulselibre_readings.db` bleibt aus Kompatibilitätsgründen erhalten; es wird keine dritte Datenbank eingeführt. #140 bleibt zurückgestellt, #496 gesperrt. Fachliche Grundlage: [IST-/SOLL-Datenmodell](IST_SOLL_DATENMODELL_2026-11-22.md).

## 0. Evidenzbaseline (lesend geprüft)

| Prüfung | Ergebnis |
|---|---|
| Vier lokale v8-Kopien unter `/tmp/opencode/s25-device-metadata-20261122/` | alle `user_version=8`, `quick_check=ok`, `foreign_key_check=[]` |
| `cirqa_measurements.db` Datei-SHA-256 | `5e84aba26d48f44b51ef9e55609c2a1fbb03682d39c43dda2cb0212050d9650c` |
| `observations` | 27.884 Zeilen, Digest `85ff5aa1b57a9573ca820936eba557cbbab0658bae1eff1ddd92d53d364b25c1` |
| `fit_generation_observations` | 27.884 Zeilen, Digest `7b963c42ae156d4f00f34ff0bd41e736982f2375b8ec93a6d0fed5c85d8ec026` |
| `fit_observation_provenance` | 27.884 Zeilen, Digest `3624fe87e4d0d4b83ac9760a929371a5f08244a83edf2127c90c43be0bf6abf3` |

Digestmethode nur für diesen Nachweis: deterministisch sortierte Zeilen, Python-`repr`-Serialisierung, SHA-256. Kein Produktionscode, keine Veröffentlichung von Individualwerten.

**Wichtiger IST-Befund:** alle vier lokalen v8-Kopien enthalten **keine** Review-Kandidatentabelle, obwohl PR #517 eine solche Tabelle für Schema v8 beschreibt. `user_version` allein ist daher **kein** hinreichender Schema-Zustandsbeweis. Die Migration muss strukturell prüfen (`sqlite_master` / `PRAGMA table_info`) und mit `IF NOT EXISTS` arbeiten. Der Code-IST erklärt den Unterschied: `CirqaDatabase.kt:448-501` ergänzt `fit_review_candidates` und `live_device_metadata` erst beim Öffnen bedingt; der Widerspruch ist damit geklärt, nicht stillschweigend übergangen.

## 1. Grundsätze

1. **Keine destruktive DDL.** Nur `CREATE TABLE IF NOT EXISTS`, `CREATE INDEX IF NOT EXISTS`, `INSERT ... ON CONFLICT DO NOTHING`, `UPDATE` ausschließlich in neuen Spalten/Tabellen. Kein `DROP`, kein `DELETE`, kein `ALTER TABLE ... RENAME`, kein `INSERT OR REPLACE` auf bestehende Tabellen.
2. **Originalbytes unverändert.** `verified_fit/<sha256>.fit`, `<sha256>.bin`, `fit_sources`, `fit_generation_observations`, `fit_observation_provenance`, `observations`, `events` werden nicht umgeschrieben.
3. **Atomicität.** Alle DDL und das seeding der Kataloge laufen in **einer** Transaktion. `user_version = 9` wird erst nach erfolgreichem Commit gesetzt (ebenfalls in dieser Transaktion). `PRAGMA foreign_keys = ON` muss vor Beginn der Transaktion aktiv sein.
4. **Rückwärtskompatibilität.** v9-Code liest v8-Daten ohne Neuschreibung. Ein Downgrade einer v9-Datei durch ältere App-Versionen wird als Versionsfehler behandelt, nicht durch destruktive Migration.
5. **Keine erfundenen Semantiken.** Katalogeinträge für `-100`/`-200`/`-300`, `HR 0`, `SpO₂ 255` werden erst nach fachlicher Bestätigung durch den FIT-Agenten ergänzt. Bis dahin nur `UNKNOWN` (und `LEGACY_PROFILE_VALID` für die dokumentierte Kopie bereits vorhandener Klassifikation).

## 2. Ablauf

### Schritt 0 – Vorflug (rein lesend, keine Geräteaktion)
- `PRAGMA integrity_check`, `PRAGMA foreign_key_check`, `PRAGMA user_version`, Tabellen-/Indexliste, Zeilenzahlen und die Baseline-Digests aus Abschnitt 0 erfassen.
- Datei-SHA-256 vor und nach der Migration vergleichen (für die Testkopie); in Produktion stattdessen Backup-Manifest prüfen.
- Kein Zugriff auf S25, keine BLE-/FileSync-Kommandos.

### Schritt 1 – Backup vor DDL (implementierte Entscheidung: Backup-Format bleibt v8)
- **Entscheidung:** `DatabaseBackup.BACKUP_VERSION` bleibt **8**. Es werden **keine** v9-JSONL-Einträge eingeführt, solange `observation_status_events` und `import_runs`/`import_errors` noch nicht aus dem Importpfad befüllt werden (FIT-Freigabe offen). Ein v9-Backup würde derzeit keine neuen, noch nicht semantisch gesicherten Daten transportieren.
- `quality_status_catalog`, `measurement_modes` und `observation_quality` sind **deterministisch rebuildbar**: `CirqaDatabase.replaceAllStoredRows(...)` ruft nach dem Replay der unveränderten v1–v8-Einträge `rebuildObservationQuality(db)` **innerhalb derselben Transaktion** auf. Kataloge werden beim Schema-Arbeiten aus dokumentierten Konstanten geseedet. Damit bleibt ein v8-Backup auf v9 verlustfrei, ohne das Backup-Format zu ändern.
- **Voraussetzung für Backup v9:** sobald Statusereignisse oder Importprotokolle tatsächlich geschrieben werden, müssen sie als eigene JSONL-Einträge mit Manifest-SHA/Record-Counts ergänzt und in `prepareRestore()` validiert werden. Bis dahin werden diese Tabellen nicht als Datenquelle des Backups behandelt.
- Original-FIT-Dateien bleiben außerhalb des SQLite-Backups; ihre Referenzintegrität wird separat über `fit_sources.source_sha256` geprüft.

### Schritt 2 – Additive DDL (eine Transaktion)
Reihenfolge:
1. `quality_status_catalog`, `measurement_modes` mit `status_code`/`mode_code` als PK und `source_of_semantics`.
2. Seed: `UNKNOWN` für beide Kataloge; `LEGACY_PROFILE_VALID` nur mit `source_of_semantics = 'MIGRATION_v9_LEGACY_COPY'`.
3. `observation_quality` mit FK auf `observations(device_id, metric, measured_at_ms, sample_id)` `ON DELETE CASCADE` und optionalem FK auf `fit_sources.source_sha256` `ON DELETE RESTRICT`; UNIQUE-Schlüssel entspricht dem Observations-PK.
4. `observation_status_events` mit `measured_at_ms NULL`-Erlaubnis und dem bidirektionalen CHECK `(measured_at_ms IS NOT NULL AND timestamp_origin IN ('DIRECT','RECONSTRUCTED_TS16','RECONSTRUCTED_COMPRESSED')) OR (measured_at_ms IS NULL AND timestamp_origin IN ('UNKNOWN','NOT_APPLICABLE'))`; `idempotency_key UNIQUE`. Damit kann eine gemessene Zeit nie ohne rekonstruierenden Ursprung stehen und eine unbekannte Zeit nie erfunden werden.
5. `import_runs`, `import_errors` mit FK `import_run_id`.
6. Indizes auf `(metric, measured_at_ms)`, `(source_sha256, measured_at_ms)`, `(status_code)`, `(import_run_id)`.
7. `PRAGMA user_version = 9`.

Alle Tabellen/Indizes mit `IF NOT EXISTS`; die exakten DDL-Entwürfe stehen im IST-/SOLL-Dokument Abschnitt 3.2 und werden beim Implementieren unverändert übernommen oder dort aktualisiert.

### Schritt 3 – Backfill (nur dokumentierte Kopie, keine Neubewertung)
- Für jede bestehende `fit_generation_observations`-Zeile wird **eine** `observation_quality`-Zeile mit `quality_status='LEGACY_PROFILE_VALID'`, `timestamp_origin`, `record_ordinal`, `global_message_number`, `field_number`, `decoder_version`/`profile_version` aus der vorhandenen Generation bzw. Provenienz eingefügt.
- `measurement_mode`, `sensor_context` und `confidence` bleiben `UNKNOWN` bzw. `NULL`; es wird nichts aus dem Wert abgeleitet.
- Join über `generation_id` zur Generierungstabelle (liefert `source_sha256`, `decoder_version`, `profile_version`, `completed_at_ms`) und zusätzlicher Join auf `observations(device_id, metric, measured_at_ms, sample_id)`. Der Observations-Join garantiert die FK-Integrität; `INSERT OR IGNORE` überspringt verwaiste Generierungszeilen, ohne Default-Werte zu erfinden. Auf der lokalen v8-Kopie trafen alle 27.884 Generierungszeilen eine Observationszeile (quality_count == observation_count).
- Es werden **keine** `observation_status_events` aus historischen Zahlenwerten erzeugt. Insbesondere bleiben alle 27.884 vorhandenen Werte in `observations` unverändert; die lokale Kopie enthält keine negativen Werte.

### Schritt 4 – Importpfad (nach FIT-Freigabe)
- Schreiben in `observation_quality` bzw. `observation_status_events` erfolgt in derselben Transaktion wie der zugehörige Observations-Commit.
- Statuscodes werden ausschließlich als `raw_code`/`raw_value_text` + `status_code` in `observation_status_events` abgelegt; `observations.value` erhält nur physikalisch gültige Zahlen.
- Ungültiger Wert einer Messgröße setzt keine andere Messgröße auf ungültig; Qualität ist immer an `sample_id` + Metrik gebunden.
- Timestamp-Origin aus Decoder übernehmen; bei `UNKNOWN` bleibt `measured_at_ms = NULL`. Unbekannte FIT-Nachrichten bekommen keinen erfundenen Zeitstempel.
- Idempotenz: `sample_id`-PK und `idempotency_key` UNIQUE; Re-Import erzeugt keine Duplikate und keine Überschreibung. Es gibt keinen Wechsel aktiver Generationen ohne vollständig committete Generation.
- Fehler: **Code-IST bestätigt** `require(...)` → `IllegalArgumentException` und `check(...)`/`error(...)` → `IllegalStateException` in `FitObservationCommitter.kt:22-71`, `:99-102`, `:140-151`, `:217-227`, `:272-288`, `:390-394`, `:427-438`, `:450`. Diese umgingen bisher den dateiweisen Handler in `CirqaFitBackfill.kt:100-113` und brachen die gesamte `run()`-Schleife ab. Im Branch werden beide Typen nun als `FailureStage.SOURCE_CONFLICT` pro Datei erfasst; die `check(...)`-Prüfungen und der Transaktions-Rollback (`endTransaction()` im `finally`) bleiben aktiv. Später werden diese Fälle zusätzlich in `import_runs`/`import_errors` pro Datei protokolliert und der Lauf als `PARTIAL` abgeschlossen. **Integritätsprüfungen und CHECK-Constraints bleiben aktiv**; ein Fehler führt nicht zum Unterdrücken, sondern zum expliziten `FILE_SKIPPED`. Ein echter Datenbank-/I/O-Fehler darf den Lauf abbrechen (`RUN_ABORTED`).

### Schritt 5 – Nachweise nach der Migration (auf Testkopie)
1. `integrity_check = ok`, `foreign_key_check = []`, `user_version = 9`.
2. Zeilenzahlen und Digests aus Abschnitt 0 für `observations`, `fit_generation_observations`, `fit_observation_provenance` **identisch**.
3. Alle vorhandenen `observations`-Zeilen haben genau eine `observation_quality`-Zeile; keine neue Zeile in `observations` oder in FIT-Buchhaltungstabellen.
4. Keine negativen RR-Werte, kein `HR = 0`, kein `SpO₂ = 255` in `observations.value` (Regex-/Bereichsprüfung je Metrik, keine Löschung).
5. Re-Import derselben FIT-Quelle: 0 neue Observations-, Qualitäts- und Statuszeilen; Digests unverändert; `import_runs.status` korrekt.
6. Backup/Restore: v9-Backup in leere Datenbank replayen → alle Zeilen und Kataloge vorhanden; anschließend erneut v9-Migration auf der restaurierten Datei → Idempotenz.
7. Negativtests: v8-Kopie **mit** und **ohne** Review-Kandidatentabelle; abgebrochene Transaktion; teilweise vorhandene Datei; doppelter Import; Metadatenkonflikt; konkurrierende Importgenerationen (nur lesend simuliert).

### Schritt 6 – Rollback
- **Vor Commit:** Transaktion verwerfen; die copied Datenbankdatei bleibt identisch (SHA-256-Vergleich).
- **Nach Commit:** kein automatischer Down-Code. Restore der v8-Backupdatei und Re-Import nach Freigabe. Rollback wird auf einer Testkopie geübt; Produktionsdaten werden nicht gelöscht.

## 3. Test- und Evidenzstufen (strikt getrennt)

| Stufe | Was passiert | Was daraus **nicht** folgt |
|---|---|---|
| Simulierter Import | JVM-Test mit synthetischen FIT-/SQLite-Fixtures | kein Beleg für echte Decoder- oder Gerätedaten |
| Erfolgreicher Decoderlauf | FIT-Original wird dekodiert, Zählung/Profilausgabe liegt vor | kein Beleg, dass Zeilen in SQLite stehen |
| Tatsächlich persistierte Daten | eigene Test-DB, Zeilen in `observations`/`observation_quality`/`observation_status_events`, Digests und `PRAGMA`-Nachweise | kein Beleg für vollständige Gerätehistorie (`coverage=device-history-unverified`) |

Der abschließende SQLite-Importtest verwendet lokal vorhandene FIT-Originale und eine **separate** Test-DB; Simulation, Decodererfolg und gespeicherte Zeilen werden im #144-Abschlussbericht getrennt ausgewiesen. Instrumentationstests laufen nur im Emulator/GitHub-CI, nie auf dem S25.

## 4. Freigaben und Reihenfolge

- [OFFEN] FIT-Agent: Feldsemantik/Statuscodes, Messmodus/Konfidenz, Timestamp-Ursprung, Decoder-/Profilversionen. Ohne Antwort bleiben die Kataloge bei `UNKNOWN`; es werden keine Statuswerte erfunden.
- [OFFEN] GUI-Agent: Kenntnisnahme der additiven Lese-Schnittstelle; keine UI-Änderung ohne dokumentierte Felder.
- [ERLEDIGT] Code-IST: `IllegalStateException`/`IllegalArgumentException`-Kette bestätigt und dateiweise abgefangen; Legacy-Store ist Version 3. Regressionstests vorhanden (JVM-Zieltest `CirqaFitBackfillTest` grün, 22.11.2026). Voller CI-Lauf weiterhin offen.
- Vor Merge: volle Android-CI inkl. Emulator auf Branch-Head und anschließend auf `main`; Ergebnisse lesen, bevor gemergt wird.

## 5. Implementierungsstand und lokale Nachweise (22.11.2026)

**Implementiert:** `CirqaDatabase.kt` Version 9, `createObservationQualitySchema(...)`, `rebuildObservationQuality(...)`, `ensureObservationQualityAvailable(...)` in `onOpen`, Rebuild in `replaceAllStoredRows(...)`. Alle DDL nutzt `IF NOT EXISTS`; Kataloge per FK statt CHECK-Enum; CHECKs bleiben aktiv. `BACKUP_VERSION` bleibt 8 (siehe Schritt 1).

**Lokal geprüft (keine Hardware, kein BLE, kein Gerät):**

| Nachweis | Kommando | Ergebnis |
|---|---|---|
| Migrationsnachweis auf Kopie der echten v8-DB (Quelle unverändert) | `python3 tools/verify_issue144_migration.py /tmp/opencode/s25-device-metadata-20261122/cirqa_measurements.db` | 27.884 Baseline-Digests identisch, 27.884 Quality-Zeilen (`LEGACY_PROFILE_VALID`/`UNKNOWN`), 0 negative Werte, `integrity_check=ok`, `foreign_key_check=[]`, `user_version=9`, zweite Anwendung idempotent |
| Synthetische v9-/Statuscode-Tests | `python3 -m unittest discover -s tools -p 'test_issue144_migration.py' -v` | 4 Tests OK (Additivität, Idempotenz, Metrik-Trennung, Statuscodes als Events, CHECK/FK-Schutz) |
| Gesamte Python-Toolsuite | `python3 -m unittest discover -s tools -p 'test_*.py'` | 116 Tests OK |
| JVM + Debug-APK | `gradle :app:testDebugUnitTest :app:assembleDebug --offline` | 218 Suites / 957 Tests, 0 Fehler; APK gebaut |
| Android-Testquellen kompiliert | `gradle :app:compileDebugAndroidTestKotlin --offline` | BUILD SUCCESSFUL |

**Neu hinzugefügter Instrumentationstest:** `CirqaDataModelV9InstrumentedTest.kt` (3 Tests: echter v8→v9-Upgrade mit Quality-Backfill und Idempotenz, Statuscodes als Events ohne physiologische Werte, aktive CHECK/FK-Constraints). Nur kompiliert, **nicht** auf Emulator/Gerät ausgeführt — Ausführung erfolgt in der Android-CI.

**Nicht behauptet:** voller Android-CI-Lauf inkl. Emulator, S25-/Gerätetest, BLE-Transfer, vollständige Gerätehistorie (`coverage=device-history-unverified`), medizinische Validierung, Backup-Format v9.
