# P0-QA-Checkliste #597–#601 (CI-Agent)

Gilt für jeden eintreffenden PR: Head festfrieren → Diff/Provenienz prüfen → synthetische Offline-Tests → FULL/API-35 + Dependency Review auf exaktem Head → Review-Kommentar mit BEWIESEN/PLAUSIBEL/NOCH UNGEKLÄRT → Merge nur nach Freigabe. Kein Gerät, keine BLE-/Timeout-/Budgetänderung, keine Rohdaten in Git.

## #597 – Sentinel-/Encoding-Rohwerte
- [ ] Kandidat enthält `raw_code`/`raw_value_text`, Quellhash, `record_ordinal`, `global_message_number`, `field_number`, Ausschlussgrund.
- [ ] Keine Promotion zu `observations`/`PROFILE_VALID`; kein Trigger für Medizin-/UI-Werte.
- [ ] Bestehende Kandidaten-UNIQUE-Kollisionen geprüft; keine Überschreibung alter Zeilen.
- [ ] Synthetische FIT-Fixture mit Sentinel und unsupported encoding → SQLite + Backup-Roundtrip.

## #598 – Versionierte Kandidaten + Backup v10
- [ ] Freigabe-Auflagen `agent-sync/2026-11-24_1000_*` eingehalten (generation-scope Tabelle, aktive Projektion atomar, Decoder-Version bei Semantikänderung, Digest v2, Replay in gleicher Transaktion).
- [ ] Migration v10→v11 transaktional, `onOpen`-Fallback, legacy-attached store; kein Downgrade.
- [ ] Backup v10: additive JSONL-Einträge, Manifest-SHA/Record-Counts, `prepareRestore`-Validierung, v1–v9-Restore unverändert.
- [ ] Tests: Fresh-DB, v10→v11, v9/v10-Restore, Aktivierung/Rollback, Duplikate über Generationen, Tampering, integrity_check/FK.

## #599 – Sicherer Resume
- [ ] Keine naive UID-Skip-Logik; Skip nur bei belastbar nachgewiesener Geräte-/Dateiidentität und Unverändertheit (Owner-Regel 6).
- [ ] Keine Änderung an BLE-Wire, 120-s-Limit, Datei-/Byte-/Seitenbudgets ohne gesonderte Freigabe.
- [ ] Zähler `runDrained` und tatsächliche Geräteabdeckung getrennt; `coverage=device-history-unverified` bleibt.
- [ ] Tests: Timeout mitten im Pull, Resume nach Neustart, Duplicate-Skip nur verifiziert, FULL/API-35.

## #600 – Rohe Notification-/GATT-Bytes
- [ ] Private 0700/0600-Ablage außerhalb Git; keine Geräteadressen/Tokens in Doku/Logs.
- [ ] SQLite-Inventar mit Provenienz/Hash/Referenz; Backup-Konsistenz geklärt.
- [ ] Kein Build-/Release-Artefakt mit Rohdaten; Datenschutzprüfung zählend dokumentiert.
- [ ] Offline-Fixture → Archiv + Inventar + Restore.

## #601 – Status-/Importtabellen
- [x] Baustein `ImportAuditRecorder` gemergt (`a0d15e33`, PR #607): `import_runs`/`import_errors`, idempotent + redigiert, FULL/API-35 grün.
- [ ] INSERT-Pfade aus FIT-Import für `observation_status_events`; Anbindung des Recorders an `CirqaFitBackfill`.
- [ ] FK-konsistent, datensparsam (keine Rohwerte/Adressen in Freitext), keine Promotion von Kandidaten.
- [ ] Backup/Restore-Erweiterung mit v1–v9-Kompatibilität; integrity_check/FK.

## PR #602 (GUI) – abgeschlossen
- [x] #604 durch PR #605 (`cd8c374f`) geschlossen: `isUnknown`, neutraler String DE/EN/ES/FR, Regression; FULL `45343846319` grün.
- [x] PR #608 (`544b8d7a`) i18n der Kandidaten-Metriken gemergt: FULL `45347850112` inkl. API-35, DR `45347850088` SUCCESS; unbekannte Keys bleiben sichtbar.

## Laufender Stand
#597, #598, #599, #600 weiterhin ohne Implementierungs-PR (FIT). #601 Teilbaustein gemergt (`a0d15e33`), Rest offen (Anbindung + Status-Events + Backup).
