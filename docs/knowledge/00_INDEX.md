# Wissensbasis CIRQA/PulsLibre – Einstieg

> Stand 24.11.2026. Ziel: mit **wenigen Klicks** vom Thema zur belastbaren Aussage kommen. Diese Seite ist ein **kuratierter Zugang**, keine neue Wahrheitsquelle: Bei Widersprüchen gilt **Code/CI > aktueller Projektstatus > historische Dokumente**. Originale bleiben erhalten und verlinkt; private Rohdaten bleiben lokal und erscheinen nie hier.

## 1. Schnellnavigation – „Ich will …"

| Frage | Dokument |
|---|---|
| Wie spricht das Band BLE/GATT an? Welche Kommandos, Formate, Pagination, was ist bewiesen vs. Hypothese? | [**01_ble-protokoll.md**](01_ble-protokoll.md) |
| Welche **Felder** haben wir gefunden (BLE, FIT, Gerätemetadaten, Status/Import, abgeleitet)? Was ist nicht dekodiert? | [**02_feldkatalog.md**](02_feldkatalog.md) |
| Wie kommen **Rohbytes in die Datenbank** (Stages, Verdicts, Dedup, Schema v11, Backup v11)? | [**03_parser-datenpfad.md**](03_parser-datenpfad.md) |
| Was haben die **echten Gerätetests** ergeben (S10/S25, Zähler, Datei 39, Persistenz)? | [**04_geraete-evidenz.md**](04_geraete-evidenz.md) |
| Was ist **noch offen** (Abdeckungsnachweis, `failed=20`, Sentinel-Lücke, #522/#599)? | [**05_offene-fragen.md**](05_offene-fragen.md) |
| Was ist mit dem **Chat/Abstimmungsbestand**? | [**06_collaboration-daten.md**](06_collaboration-daten.md) – vollständig behalten, Leseführer + Freigabevorlage |
| Was darf **aufgeräumt** werden? | [**07_aufraumungs-vorschlag.md**](07_aufraumungs-vorschlag.md) – **nur Vorschlag**, nichts gelöscht |

## 2. Die drei Kernaussagen (auf einen Blick)

1. **Der Datenpfad ist im Code vollständig verdrahtet** und in Tests belegt: Notification → Archiv → ItemList → Pull → MLR → Originalarchiv → FIT-Decoder → SQLite (v11) → Audit → Backup (v11, Restore v1–v10). Siehe [`03_parser-datenpfad.md`](03_parser-datenpfad.md) §0.
2. **Die gelisteten Dateien wurden am Gerät übertragen** (24.11.: 34/34 übertragen/verifiziert/privat gespeichert, 32 dekodiert, 12 SQLite-Commits), aber **`coverage=device-history-unverified`**: `runDrained=false`, `failed=20`, letzte Transaction-Follow-up-Antwort leer, 39. Datei nicht belegt. Siehe [`04_geraete-evidenz.md`](04_geraete-evidenz.md) §3.
3. **Bei unbekannter Dateiidentität wird nie übersprungen**, sondern vollständig erneut heruntergeladen und verlustfrei archiviert; Duplikate sind erlaubt, stille Sprünge nicht. Siehe [`../DATA_ACQUISITION_PRIORITY.md`](../DATA_ACQUISITION_PRIORITY.md).

## 3. Kanonische Originale (nach wie vor maßgeblich)

| Thema | Primärquelle |
|---|---|
| Protokoll-Spezifikation | [`../protocol/CIRQA_PROTOCOL_SPEC.md`](../protocol/CIRQA_PROTOCOL_SPEC.md) |
| Forschungs-/Evidenzstand Historie | [`../protocol/research/CIRQA_HISTORICAL_RESEARCH_STATUS_2026-11-18.md`](../protocol/research/CIRQA_HISTORICAL_RESEARCH_STATUS_2026-11-18.md) |
| Historien-Sync-Design | [`../protocol/sync/HISTORICAL_SYNC.md`](../protocol/sync/HISTORICAL_SYNC.md) |
| Vollständige FIT-Feldmatrix | [`../protocol/research/CIRQA_FIT_FIELD_INVENTORY_2026-11-18.csv`](../protocol/research/CIRQA_FIT_FIELD_INVENTORY_2026-11-18.csv), [`CIRQA_FIT_INVENTORY_MATRICES_2026-11-18.md`](../protocol/research/CIRQA_FIT_INVENTORY_MATRICES_2026-11-18.md) |
| Datenmodell IST/SOLL + Migration | [`../protocol/data-model/IST_SOLL_DATENMODELL_2026-11-22.md`](../protocol/data-model/IST_SOLL_DATENMODELL_2026-11-22.md), [`P0_DATA_ACQUISITION_AUDIT_2026-11-24.md`](../protocol/data-model/P0_DATA_ACQUISITION_AUDIT_2026-11-24.md) |
| Persistenzvertrag | [`../data/COMPLETE_BAND_DATA_PERSISTENCE.md`](../data/COMPLETE_BAND_DATA_PERSISTENCE.md) |
| Testplan/Abbruchregeln S10 | [`../protocol/device-tests/CIRQA_P0_S10_BOUNDED_FULL_PULL_PLAN_2026-11-24.md`](../protocol/device-tests/CIRQA_P0_S10_BOUNDED_FULL_PULL_PLAN_2026-11-24.md) |
| Terminal-/Pipeline-Review | [`../protocol/device-tests/CIRQA_S10_TERMINAL_EVIDENCE_2026-11-23.md`](../protocol/device-tests/CIRQA_S10_TERMINAL_EVIDENCE_2026-11-23.md) |
| aktuellster Filter-Audit | [`../protocol/research/CIRQA_CONNECT_TRANSACTION_FILTER_AUDIT_2026-11-24.md`](../protocol/research/CIRQA_CONNECT_TRANSACTION_FILTER_AUDIT_2026-11-24.md) |
| Projektstatus / Qualitätsgate | [`../PROJECT_STATUS.md`](../PROJECT_STATUS.md), [`../QUALITY_GATE.md`](../QUALITY_GATE.md) |
| CI-Betrieb | [`../ci/HYBRID_CI.md`](../ci/HYBRID_CI.md) |
| Daten-/Gerätesicherheit | [`../collaboration/CURRENT_BAND_RAW_DATA_POLICY.md`](../collaboration/CURRENT_BAND_RAW_DATA_POLICY.md), [`../collaboration/DEVICE_LEASE.md`](../collaboration/DEVICE_LEASE.md) |

## 4. Pflege dieser Wissensbasis

- Neue Erkenntnisse **zuerst** in den kanonischen Originalen (§3) ergänzen, danach hier verdichten – nicht umgekehrt.
- Jede Aussage hier hat einen Quellverweis. Fakten, Hypothesen und offene Punkte bleiben getrennt (Konvention in [`01_ble-protokoll.md`](01_ble-protokoll.md) §1 und [`02_feldkatalog.md`](02_feldkatalog.md) Einleitung).
- Widersprüche werden **markiert**, nicht stillschweigend geglättet ([`01_ble-protokoll.md`](01_ble-protokoll.md) §8, [`02_feldkatalog.md`](02_feldkatalog.md) §12).
- Keine Geräteadressen, Tokens, UIDs, Gesundheitsrohwerte, Seriennummern oder privaten Dateiinhalte in diese Dateien schreiben.
- Löschen nur nach ausdrücklicher Freigabe; [`07_aufraumungs-vorschlag.md`](07_aufraumungs-vorschlag.md) Option A ist der aktuelle Stand. Collaboration-Daten bleiben vollständig erhalten.
