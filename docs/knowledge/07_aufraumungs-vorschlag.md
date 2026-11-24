# Aufräumvorschlag – verwaiste/veraltete Doku (NUR VORSCHLAG, keine Löschung)

> **Status: nichts gelöscht, verschoben oder überschrieben.** Die Projektleitung hat die Freigabe für die neue Wissensstruktur erteilt, aber ausdrücklich gesagt: Collaboration-Daten (Chat/Abstimmung) bleiben und werden separat nochmals zur Freigabe aufbereitet → [`06_collaboration-daten.md`](06_collaboration-daten.md). Für alle anderen Dateien gilt zusätzlich die Repository-Regel: historische Evidenz mit datiertem Hinweis kennzeichnen statt Massenlöschungen (`docs/README.md`, `docs/protocol/README.md`).

## 1. Grundsatz

Die neue Wissensbasis ([`00_INDEX.md`](00_INDEX.md)) verweist auf **kanonische** Quellen. Die folgenden Dateien sind überholt, übernommen oder reine Chronik – aber vielfach **Beweismittel** (Issues/PRs verlinken sie). Deshalb: Kandidatenliste statt Löschung. Erst nach deiner ausdrücklichen Freigabe einzelner Gruppen darf gelöscht werden.

## 2. Vorgeschlagene Lösch-/Archivkandidaten (nach Gruppen)

| Gruppe | Betroffene Pfade (Auszug, alle unter `docs/`) | Grund | Nachfolger / Behalt-Argument |
|---|---|---|---|
| **Frühe Speicher-Doku** | `LOCAL_STORAGE.md`, `UNIFIED_LOCAL_STORAGE_2026-11-16.md` | Erste Implementation, Zwischenstand | `protocol/data-model/CIRQA_SQLITE_STORAGE.md` (dort selbst schon v5, aktuell v11) |
| **Überholte Produkt-/Roadmap-Doku** | `APP_VISION.md`, `ROADMAP_TO_CIRQA_DATA.md`, `DEVELOPMENT_PLAN.md`, `ARCHITECTURE_AND_CODE_REVIEW.md` | Alle selbst als Entwurf/historisch markiert; durch neuere Dokumente ersetzt | `ui/APPROVED_UI_CONCEPT_2026-11-15.md`, `protocol/research/CIRQA_HISTORICAL_RESEARCH_STATUS_2026-11-18.md`, `PROJECT_STATUS.md`, `AUDIT_2026-11-16.md` |
| **Wiederholte Audit-/Reviewkette** | `AUDIT_2026-11-16.md`, `AUDIT_REVALIDATION_2026-11-18.md`, `AUDIT_TEIL2_2026-11-18.md`, `AUDIT_ISSUE_6_POST_MIGRATION_2026-11-19.md`, `CODE_REVIEW_2026-11-18.md`, `CODE_REVIEW_2026-11-19.md`, `CODEX_DOCUMENTATION_REVIEW_2026-11-17.md`, `DOCUMENTATION_REVIEW_2026-11-18_PASS2.md`, `DOCUMENTATION_CONSISTENCY_AUDIT_2026-11-19.md`, `TEST_INVENTORY_2026-11-18.md` | Aufeinanderfolgende Prüfstände, teils Korrekturketten | Nur letzten Stand (20.11 `AUDIT_ISSUE_6_KOMPONENTEN_2026-11-20.md`) als Nachweis zitieren; älteres ggf. datiert archivieren |
| **Einmal-Incidents/Build-/SCA-Befunde** | `CI_INCIDENT_2026-11-15.md`, `CI_INCIDENT_48.md`, `CI_ISSUE_156_VERIFICATION_2026-11-19.md`, `TEST_BUILD_2026-11-17.md`, `HISTORY_PERFORMANCE_2026-11-18.md`, `SCA_DEPENDENCY_REVIEW_2026-11-20.md`, `ISSUE_TRIAGE_2026-11-17.md` | Abgeschlossene Point-in-Time-Notizen, kein Dauerwert | `ci/HYBRID_CI.md`, `ci/DOCS_ONLY_CI_POLICY.md`, `QUALITY_GATE.md`; `PRIVACY_INCIDENT_2026-11-16.md` **bewusst behalten** (README: nicht verstecken) |
| **Protokoll: überholte Quellenvergleiche 16./17.11** | `protocol/research/CIRQA_EXTERNAL_PROTOCOL_RESEARCH.md`, `CIRQA_5050_*_2026-11-17.md` (5), `CIRQA_5024_REFERENCE_FIELD_LAYOUT_2026-11-17.md`, `CIRQA_EXTENDED_5024_REFERENCE_COMPARISON_2026-11-17.md`, `CIRQA_FILESYNC_REFERENCE_COMPARISON_2026-11-16.md`, `CIRQA_PUBLIC_BLE_CAPTURE_SEARCH_2026-11-17.md`, `GARMIN_PROTOCOL_RESEARCH.md` | Frühe Hypothesen/Referenzvergleiche, durch Spezifikation + 18.11-Forschungsstand ersetzt | `protocol/CIRQA_PROTOCOL_SPEC.md`, `protocol/research/CIRQA_HISTORICAL_RESEARCH_STATUS_2026-11-18.md` |
| **Protokoll: abgelöste Vorbereitungs-/Planpapiere** | `protocol/sync/CIRQA_HISTORY_SYNC_PLAN.md`, `CIRQA_HISTORICAL_DOWNLOAD_RELEASE_GATE_2026-11-17.md`, `CIRQA_5050_TO_SYNC_TRANSITION_2026-11-17.md`, `protocol/experiments/CIRQA_RFCOMM_PREPARATION_2026-11-18.md`, `CIRQA_HISTORICAL_SYNC_V1_2026-11-18.md`, `CIRQA_TEST_LAB_STAGE2_PROPOSAL_2026-11-17.md`, `protocol/device-tests/CIRQA_NEXT_DEVICE_TEST.md` (selbst „ARCHIV/VERALTET"), `..._AFTER_SYNC_HCI_ANALYSIS_2026-11-21.md` (Aussage widerrufen) | Durch Geräteergebnisse überholt bzw. selbst widerrufen | `protocol/sync/HISTORICAL_SYNC.md`, `device-tests/CIRQA_S10_THIRD_SYNC_HCI_CORRECTION_2026-11-21.md`, [`01_ble-protokoll.md`](01_ble-protokoll.md) |
| **Datenmodell: alte DDL-Snapshots** | `protocol/data-model/CIRQA_STORAGE_ARCHITECTURE_ADR_2026-11-18.md`, `CIRQA_DATA_WORKFLOW_SKETCH.md` | Eigene „Current-state update 20.11"-Warnung; DDL veraltet | `protocol/data-model/IST_SOLL_DATENMODELL_2026-11-22.md`, `P0_DATA_ACQUISITION_AUDIT_2026-11-24.md` |
| **FIT-Feldanalyse: Vorstufe** | `protocol/FIT_FELDANALYSE_2026-11-21.md` | Hypothesen (u. a. `timestamp_16`) durch 22.11-Analyse ersetzt | `protocol/research/CIRQA_FIT_FELDANALYSE_CHATGPT_2026-11-22.md`, [`02_feldkatalog.md`](02_feldkatalog.md) |
| **CI: Einmal-Übergabepakete** | `ci/LOCAL_RUNNER_MIGRATION_PACK_2026-11-20.md`, `ci/COMBINED_COVERAGE_2026-11-22.md`, `ci/DEVICE_SAFETY_GATE_2026-11-22.md` | Umsetzungs-/Incident-Nachweise, in `HYBRID_CI.md` aufgegangen | `ci/HYBRID_CI.md`, `QUALITY_GATE.md` |
| **Einmalberichte ohne Dauerwert** | `research/ISSUE_150_OFFLINE_INVENTORY_2026-11-19.md`, `research/GARMIN_CONNECT_BENCHMARK_261_2026-11-20.md`, `protocol/device-tests/CIRQA_PRODUCT_DOWNLOAD_RESULT_*_BUILD1690.md`, `..._BUILD1701.md`, `CIRQA_PRODUCT_DOWNLOAD_FOLLOWUP_2026-11-21.md` | Point-in-Time-Ergebnisse, letzter Stand zitierbar | `protocol/research/CIRQA_HISTORICAL_RESEARCH_STATUS_2026-11-18.md`, [`04_geraete-evidenz.md`](04_geraete-evidenz.md) |

## 3. Ausdrücklich **nicht** löschen

- **`docs/collaboration/**` (656 Dateien, 2,8 MB)** – Eigentümerentscheidung 24.11.: Chat, Abstimmungen, Sitzungslog vollständig behalten; separat zur Freigabe aufbereitet in [`06_collaboration-daten.md`](06_collaboration-daten.md). Keine einzige Datei als Kandidat markiert.
- **`docs/PROJECT_STATUS.md`, `CHAT_START_HERE.md`, `QUALITY_GATE.md`, `AGENTS.md`** – kanonisch/verbindlich, auch wenn sie Chronik enthalten.
- **`docs/protocol/device-tests/raw/*/README.md`** – Freigabe- und Herkunftsmanifeste der versionierten Fixtures.
- **`docs/PRIVACY_INCIDENT_2026-11-16.md`** – Datenschutzvorfall mit Reparatur-Commits, laut `docs/README.md` bewusst nicht archivieren.
- **`docs/architecture/MODULARITY_AUDIT_CHATGPT.md`** – geparkt (#496), nicht überholt.
- Alle **datierten Geräteberichte** (`device-tests/`) und **Provinienz-/Lizenzdokumente** (`LICENSE_AGPL_AUDIT_*`, `SYNTHETIC_FIT_FIXTURES_MANIFEST_*`) – Beweiskette, nicht Löschkandidat.
- `CIRQA_S10_BERICHTE_ANALYSE_INPUT_2026-11-22.md` ↔ `CIRQA_GESAMTANALYSE_DATEN_METADATEN_S25_S10_2026-11-22.md` – **Paar, kein Duplikat**.

## 4. Freigabeoptionen (bitte ankreuzen)

- [ ] **A – Empfohlen:** keine Löschung; nur die neue Wissensbasis mit Quellverweisen nutzen. Alte Doku bleibt, ist aber über `00_INDEX.md` nicht mehr vordergründig erreichbar.
- [ ] **B:** die oben genannten Gruppen in ein datiertes Archiv `docs/archive/2026-11-24/` verschieben (kein Datenverlust, Verweise müssen geprüft werden).
- [ ] **C:** einzelne Gruppen wirklich löschen – bitte konkret bezeichnen; vorher `grep`-Scheck auf Verweise in Issues/PRs/übrigen Docs.

**Aktueller Status: A ist umgesetzt. B und C sind nicht ausgeführt.**
