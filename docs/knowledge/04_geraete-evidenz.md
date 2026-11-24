# Geräte-Evidenz: S10/S25 – was tatsächlich gemessen wurde

> Stand 24.11.2026. Nur belegte Fakten. **`coverage=device-history-unverified` gilt in allen Berichten** – `runDrained` heißt nur „Queue dieses Laufs abgearbeitet", nicht „gesamte Gerätehistorie". Keine vollständige Geräteabdeckung behaupten. Quellen sind relativ zu `docs/knowledge/` angegeben.

## 1. Rahmenbedingungen (unverändert gültig)

- **Ziel:** alle technisch zugänglichen Banddaten verlustfrei lokal/archive/SQLite sichern; unbekannte Dateiidentität verhindert nur das *Überspringen*, nie den Full-Pull. → [`../DATA_ACQUISITION_PRIORITY.md`](../protocol/DATA_ACQUISITION_PRIORITY.md), [`../protocol/data-model/P0_DATA_ACQUISITION_AUDIT_2026-11-24.md`](../protocol/data-model/P0_DATA_ACQUISITION_AUDIT_2026-11-24.md)
- **Testplan:** genau **ein** Trigger (automatisch ODER manuell). `MainActivity.onResume()` kann Auto-Sync starten – dieser zählt als der eine Lauf; kein zusätzlicher Button-Klick. Keine neuen BLE-Kommandos, kein SetFlags/Archive/Delete/Reset/CLOSE_ALL. Budget **120 s / 64 Seiten / 256 Dateien / 8 MiB pro Datei / 64 MiB pro Lauf**; Grenze → unvollständig, nicht erweitern. → [`../protocol/device-tests/CIRQA_P0_S10_BOUNDED_FULL_PULL_PLAN_2026-11-24.md`](../protocol/device-tests/CIRQA_P0_S10_BOUNDED_FULL_PULL_PLAN_2026-11-24.md)
- **Lease:** vor jedem Zugriff `tools/s25_lease.py status --device s10`, gesamte Sitzung in einem gehaltenen Wrapper, danach wieder `FREI`. Kein `run-as`/Root, keine SharedPreferences-Manipulation; ohne sicheren Vorher-Export → stoppen.
- **Abbruch:** negative Protokollantwort, ungültige Frames, unerwarteter Gerätezustand, Grenzerreichung oder unklarer Sync-Status → kontrollierter Stopp, kein Retry, kein vierter Follow-up.

## 2. Chronologie (kompakt)

| Datum | Gerät / Build | Belegter Befund | Quelle |
|---|---|---|---|
| 18.11. | S25 V24 / V25 | 33/33 Dateien (146.293 B; 31 FIT + 2 Roh), `integrity_check=ok`, 294 Zeilen; V25 1.018 MLR-Chunks, eine Seite, `next_transaction_id` aber kein Follow-up. | [`CIRQA_SYNC_V24_DEVICE_RESULT_2026-11-18.md`](../protocol/device-tests/CIRQA_SYNC_V24_DEVICE_RESULT_2026-11-18.md), [`..._V25_...`](../protocol/device-tests/CIRQA_SYNC_V25_DEVICE_RESULT_2026-11-18.md) |
| 19.11. | S25 V26 | 42/42 Dateien (40 FIT + 2), 218.951 B, 1.520 Chunks, 0 Fehler; alle CRC/Hash ok; SQLite 294, `events` 117.746; idempotent. | [`..._V26_DEVICE_RESULT_2026-11-19.md`](../protocol/device-tests/CIRQA_SYNC_V26_DEVICE_RESULT_2026-11-19.md) |
| 21.11. | S25, 3 freigegebene Läufe | Pro Lauf **eine** `transaction_id`-Folgeantwort `status=0` mit **0 Einträgen`, erneut `nextTransaction=true`; `pullSent=false`; kein 4. Versuch. | [`CIRQA_NEXT_TRANSACTION_PROBE_RESULT_2026-11-21.md`](../protocol/device-tests/CIRQA_NEXT_TRANSACTION_PROBE_RESULT_2026-11-21.md) |
| 22.11. | S10 Build 1806 | Lauf 1 ohne Import; Lauf 2 **DB-Import nachgewiesen**: 27.736 Beobachtungen / 23 FIT-Quellen, überstand force-stop. | [`CIRQA_S10_BOUNDED_DB_IMPORT_EXECUTION_2026-11-22.md`](../protocol/device-tests/CIRQA_S10_BOUNDED_DB_IMPORT_EXECUTION_2026-11-22.md) |
| 22.11. | S10 Garmin-Connect passiv | 85.154 HCI-Records; 2 vollständige CRC-valide FIT-Dateien; offline 6 zlib-Kandidaten (5 FIT), `CirqaZlibFileDecoder` **nicht im Live-Pfad**. | [`..._GARMIN_FULL_SYNC_LOG_2026-11-22.md`](../protocol/device-tests/CIRQA_S10_GARMIN_FULL_SYNC_LOG_2026-11-22.md), [`..._MLR_TRANSFER_ANALYSIS_2026-11-22.md`](../protocol/device-tests/CIRQA_S10_MLR_TRANSFER_ANALYSIS_2026-11-22.md) |
| 22./23.11. | S10 Capture-Vergleich | 39 ML-Registrierungen, 39 Pulls, **eine** Item-List mit 39 Einträgen; **38** Transfers bytegleich rekonstruiert, 34 FIT CRC-valid; **39. Datei nicht vollständig belegt**. | [`CIRQA_CONNECT_S10_CAPTURE_COMPARISON_2026-11-23.md`](../protocol/research/CIRQA_CONNECT_S10_CAPTURE_COMPARISON_2026-11-23.md) |
| 23.11. | S10 Build 1989 | Ein bestätigter Sync, **unvollständig 36/37**; 38 Originale/642.709 B vor und nach, 0 neue Hashes, Referenzen 40→74; Ursache **120-s-Limit**. | [`CIRQA_S10_ALL_DATA_ACCEPTANCE_2026-11-23.md`](../protocol/device-tests/CIRQA_S10_ALL_DATA_ACCEPTANCE_2026-11-23.md), [`..._TERMINAL_EVIDENCE_2026-11-23.md`](../protocol/device-tests/CIRQA_S10_TERMINAL_EVIDENCE_2026-11-23.md) |
| 23.11. | S10 Build 1939 → 1958 | 1939: 34 Einträge, 14 selektiert, 3 leere Follow-ups. 1958: 37 Einträge, 16/16 transferiert/verifiziert/gespeichert, 12 dekodiert & committed, `failed=0`. | [`CIRQA_S10_BUILD1939_SYNC_POSTMORTEM_2026-11-23.md`](../protocol/device-tests/CIRQA_S10_BUILD1939_SYNC_POSTMORTEM_2026-11-23.md) |
| **24.11.** | **S10 Build 2062, P0-Lauf** | **Genau ein automatischer Trigger**, 34/34 übertragen/verifiziert/privat gespeichert; 32 dekodiert; 12 SQLite-Commits; Details unten. | [`CIRQA_P0_S10_BOUNDED_FULL_PULL_PLAN_2026-11-24.md`](../protocol/device-tests/CIRQA_P0_S10_BOUNDED_FULL_PULL_PLAN_2026-11-24.md); Chat `2026-11-24_103900_*`, `105300_*` |

## 3. Der P0-Lauf vom 24.11. – Endzähler

| Zähler | Wert |
|---|---:|
| Dateien übertragen / integritätsverifiziert / privat gespeichert | **34 / 34 / 34** |
| FIT dekodiert | **32** |
| SQLite-committed | **12** |
| `imported` | **0** |
| `noImportableData` | **12** |
| `unsupported` / `failed` | **0 / 20** |
| `runDrained` | **false** |
| `coverage` | **`device-history-unverified`** |
| `incompleteReason` | **`next-transaction-followup-empty`** (letzte Folgeantwort leer) |

Ablauf: 10:30 ENDE „teilweise" – Vorher-Backup v9 (ZIP ok, 50 Originale), Build 2062 signiert, `MainActivity` genau einmal geöffnet, kein manueller Sync, kein Post-Run-Backup. 10:39 Korrektur nach read-only Nachblick: der MainActivity-Start hatte **tatsächlich den Auto-Lauf ausgelöst**; S10 danach FREI. 10:53 Offline-Zuordnung: die 20 Fehler liegen im **FIT→SQLite-Import/Commit-Pfad**; konkreter Exception-Typ **nicht belegt**. Der leere Follow-up ist ein separater Listen-Fortsetzungsfehler.

## 4. Pagination, ``next_transaction_id``, „Datei 39"

- `session_id` = Cursor **derselben** Item-List-Seite; `next_transaction_id` = Kontext einer **separaten späteren Transaktion**, kein Seiten-Cursor. Produkt-Policy: maximal **3** wiederholte Transaction-Follow-ups, dann fail-closed.
- S25-Proben und S10 1939/1958/2062: jeweils leere Folgeantworten, `runDrained=false` → keine zusätzliche Historie belegt.
- **Datei 39:** 39 gelistete Einträge, 39 Pulls, 38 bytegleiche Transfers (34 FIT CRC-valid); der 39. Transfer ist **nicht vollständig belegt**. Älterer Terminalbefund: Ordinal 39 war queued/write-accepted, erhielt aber 0 der erwarteten 1.094 Bytes vor dem 120-s-Limit.
- Transaction-Filter-Audit 24.11.: 117 CRC-valide Frames; Host-Pulls **UID-only**, Item-List-Requests mit Feldern 4/5 bzw. 5/6; PulsLibre sendet im Follow-up nur Feld 2. **Hypothese**, kein Ursachenbeweis. → [`../protocol/research/CIRQA_CONNECT_TRANSACTION_FILTER_AUDIT_2026-11-24.md`](../protocol/research/CIRQA_CONNECT_TRANSACTION_FILTER_AUDIT_2026-11-24.md)

## 5. Bewiesen vs. nicht bewiesen

**Bewiesen:** vollständiger Multi-Datei-Abruf + MLR + private Speicherung + idempotenter Import (S25 V24–V26, S10); S10-DB-Import am 22.11.; die 120-s-Grenze als Ursache des 36/37-Laufs („36/37" = erfolgreiche Pipelines, **nicht** 36 von 37 Gerätedateien); bytegenaue FIT-Rekonstruktion für 38 Transfers; Persistenz mit generation-scoped Rohfeldern/Provenienz/Digest (PR #610), Notification-Archiv (PR #609); 24.11-Lauf-Zähler wie oben.

**Nicht bewiesen / ungeprüft:** dass die beobachteten Listen die gesamte Historie abdecken; dass das Gerät fehlende Intervalle überhaupt anbot; dass Datei 39 vollständig ist; dass das 120-s-Limit fehlende Messwerte *verursachte*; Raw-Device-List-Total, exakte Request-Anzahl, per-File-Stage-Totals, Cursor-Zustand beim Timeout; Exception-Typ der 20 Importfehler; Persistenz per Backup-Diff am 24.11. (**kein Post-Run-Backup**). Eine leere GUI-Anzeige ist **kein** Beweis für Datenverlust.

## 6. Persistenz-Evidenz (Auszug)

- S25 V24–V26: `integrity_check=ok`, 294 Zeilen, idempotent. **294 ist anderer Geräte-/Versuchsstand als 27.736 (S10) – nicht addierbar.**
- S10 22.11. Drei-Export-Vergleich: vor/nach Lauf 1 2.786 B, 0/0; nach Lauf 2 **4.043.919 B, 27.736 Beobachtungen, 23 FIT-Quellen**; 15 Manifest-Einträge ok; Provenienz 27.736, `unmapped_fields` 10.283, `device_metadata` 115.
- S10 23.11.: 38 Originale/642.709 B, 0 neue Hashes, Referenzen **40→74**, 28.668 kanonische Observationen, 109.960 unbekannte Feld-Vorkommen, Zeitraum 15.–23.11.
- CSV/DB-Differenz 157: **aufgelöst** – alle 27.736 Backup-Schlüssel in CSV (27.893) enthalten; 157 Zeilen = Live-Events nach dem Snapshot, kein Verlust. → [`../protocol/device-tests/CIRQA_S10_CSV_DB_157_DIFFERENZ_AUFGELOEST_2026-11-22.md`](../protocol/device-tests/CIRQA_S10_CSV_DB_157_DIFFERENZ_AUFGELOEST_2026-11-22.md)
- Status-/Import-Tabellen existieren, waren bis #601/PR #613 ohne INSERT-Pfade; Backup v11 sichert sie jetzt.

## 7. Offene Punkte

1. `failed=20`: Exception-Typ klären, synthetische `StageCounts`-Regression (kein Hardware-Repeat).
2. Leere letzte Follow-up-Antwort: sicherer Sync-Status-Pfad für einen künftigen Einzelversuch; kein S10-Zugriff ohne neue Entscheidung.
3. Kein Post-Run-Backup am 24.11. → kein Backup-Diff-Nachweis.
4. Datei 39: 38/39 bytegleich, 39. Transfer nicht belegt.
5. Beweiskriterien Abdeckung: (a) alle angebotenen Dateien bytegleich archiviert, (b) Replay-/Feldabdeckung aller decoding-relevanten Felder, (c) Backup/Restore konsistent.

## 8. Hypothesen – nicht als Fakten

| Hypothese | Grenze |
|---|---|
| Connect-Filterfelder (4/5 bzw. 5/6) vs. PulsLibre nur Feld 2 erklären abweichende Listen | plausibel, **unbewiesen**; keine Filter-/UUID-Werte geraten |
| Doppelter FIT-Decode (~50 s `fit-import`) ist Hauptsystemengpass | stärkster identifizierter Engpass, aber kein Ursachenbeweis; Fix PR #591 |
| Redundante Full-Pulls nach Freshness-Fix #567 verbrauchten 120-s-Budget | plausibel, keine per-File-Dauer belegt |
| 12.730 offline vs. 12.712 UI = gleitendes Fenster | plausibel, 18 Differenzwerte nicht als Verlust deuten |
