# Dashboard data provenance and scientific limits

**Implementation audit: 2026-11-17.** This document describes the current `main` implementation. Earlier notes about reading `cirqa_overnight.jsonl` are obsolete: the dashboard now reads the canonical local SQLite database `cirqa_measurements.db`.

## Data path

- `CirqaDatabase` stores raw event JSON locally. The database has a unique SHA-256 key per complete event line; exact duplicate lines are ignored. It retains the original payload and indexed receive timestamp/service.
- `CirqaDashboardReader` reads events whose JSON has `type=reading`, `schema=1`, and a service in the registered realtime service set. Malformed records, unsupported services, invalid timestamps, and readings more than 60 seconds in the future are ignored.
- `received_at_utc` is the phone's reception timestamp. The sensor acquisition time is not established, so displayed recency is receive age, not sensor latency.
- For each service, the overview retains its latest accepted record. Steps are restricted to the current phone-local calendar date; snapshots are a cumulative device counter and are not summed.
- `CirqaHistoryReader` currently loads only heart-rate and SpO₂ observations in a rolling selected interval. Points are positioned by phone reception time. `aggregateHistoryPoints` computes bucket min/median/max from observations and omits empty buckets; it does not interpolate.
- The reader still loads all event lines into memory before selecting latest values. A future bounded SQL projection would scale better for long-running databases.

## Metric contract

| Displayed value | Source | Current handling | Evidence and limits |
|---|---|---|---|
| Heart rate | Service 6, `bpm` | Integer 1–240; latest received value | Realtime stream value; not a validated resting or clinical measurement |
| Resting heart rate | Service 6, `resting_bpm` | Integer 1–240; latest received value | Device-provided field; no independently derived resting phase |
| Steps | Service 7, `steps` | Unsigned 32-bit cumulative counter; latest sample today in phone-local time | No sum of snapshots; device reset/day-boundary behavior remains unverified |
| Step goal | Service 7, `goal` | Positive device-provided integer | Not a WHO or user-specific activity recommendation |
| SpO₂ | Service 19, `spo2_percent` | Integer 1–100; latest received value | Device-specific sensor validation and sampling quality are outstanding; no night analysis or diagnosis |
| RR interval | Service 12, `rr_ms` | Positive single interval in milliseconds | Not RMSSD/SDNN/HRV; no interval series or artifact filter |
| Stress | Service 13, `stress` | Device scale 0–100 | Proprietary scale; not a clinical stress measure |
| Body Battery | Service 20, `body_battery` | Device scale 0–100 | Proprietary device index; no clinical interpretation |
| Respiration | Service 21, `breaths_per_min` | Integer 1–100 | Device-provided realtime value; no diagnosis |
| Calories | Service 8, `total` / `active` | Nonnegative device integer | Unit and accumulation semantics are unverified; do not label as kcal |
| Intensity minutes | Service 10, `moderate` / `vigorous` | Nonnegative device integer, shown as minutes | Accumulation window and relation to guideline minutes are unverified |
| Sleep | No supported history source in the dashboard reader | Explicitly unavailable | No sleep duration, stages, or intervals inferred from inactivity |
| HRV | No validated metric | Not calculated | A single RR interval is insufficient |

The status “CIRQA realtime” does not by itself establish independent decoder or sensor validation. Refer to the current protocol/device evidence before treating any service as verified.

## Current quality handling and gaps

The overview rejects malformed/unsupported records and applies per-field plausibility ranges. Missing or invalid values become “Keine Messdaten” / “Nicht gemessen oder ungültig”. These are display strings, not a shared structured quality model.

The current reader does **not** expose a common quality object for valid-sample fraction, time coverage, stale age, source confidence, artifact flags, or sensor acquisition time. It also does not classify “insufficient data” separately from “not available” for every metric. History retains observation timestamps and bucket counts, but does not yet surface coverage or a quality gate in the model.

Phone-local date handling is used for the daily step card. The underlying events are UTC, but the dashboard does not yet provide a shared local-day/night partition model with explicit zone changes, duplicate policy beyond exact-line hashing, or daylight-saving regression coverage across all analyses. No daily sleep/activity pairing or 7/28/90-day fitness trend is available.

## Rules for new analyses

1. Use only stored, decoded observations; never fabricate or interpolate readings.
2. Keep phone reception time distinct from an unknown sensor measurement time.
3. Preserve units and provenance. Do not infer kcal, HRV, sleep, diagnosis, VO₂max, or guideline intensity from unsupported fields.
4. Do not treat cumulative step snapshots as increments. Any activity estimate based on step deltas must be labeled as a heuristic and include its window and limitations.
5. Do not calculate a trend, correlation, night metric, or clinical threshold without explicit sampling, coverage, missingness, outlier, and validation rules plus tests.
6. Distinguish unavailable, invalid, insufficiently covered, and experimental results in the model and accessible UI.
7. Keep all personal measurements local and out of issues, logs, and test fixtures.

## Code and evidence references

- `app/src/main/java/de/pulselibre/CirqaDatabase.kt`
- `app/src/main/java/de/pulselibre/CirqaDashboardReader.kt`
- `app/src/main/java/de/pulselibre/CirqaHistoryReader.kt`
- `app/src/main/java/de/pulselibre/HistorySeries.kt`
- `app/src/androidTest/java/de/pulselibre/CirqaDashboardReaderTest.kt`
- `app/src/androidTest/java/de/pulselibre/CirqaHistoryReaderTest.kt`
- `docs/PROJECT_STATUS.md`
- `docs/ENGINEERING_SCIENTIFIC_STANDARDS.md`


---

## Geplante Informationsarchitektur: Hauptreiter „Analysen“ (Entscheidung 2026-11-19)

**Verbindliches Zielbild für die weitere Planung:** Wissenschaftliche Auswertungen werden künftig unter einem eigenständigen Hauptreiter **„Analysen“** gebündelt. Die **„Übersicht“** bleibt für aktuelle Messwerte und Gerätestatus; dort sind nur kurze Teaser mit Deep Link in die Analyse erlaubt. Die Umsetzung ist in [GitHub #227](https://github.com/jochenkunkel/pulselibre/issues/227) spezifiziert. **Dies ist eine Dokumentationsänderung, kein Nachweis einer bereits implementierten Navigation.**

**Geplante Modulgruppen:** (1) Herz & Kreislauf: Ruhepuls/Fitness #41, optionale HRV/Belastungserholung/SpO₂; (2) Schlaf & Erholung: #43 und #44, mögliche Lag-Analysen; (3) Bewegung & Entwicklung: #46, #47, Aktivitätssignatur; (4) Zusammenhänge & Muster: #45, Tagesrhythmik, Trendbrüche und explorative Muster; (5) Datenqualität & Methoden: Quellen, Abdeckung, persönliche Baseline, Datenlücken, Unsicherheit. Forschungs-Ideen sind nicht automatisch als verfügbare Funktionen anzusehen.

**Kartenvertrag:** Jede wissenschaftliche Karte besitzt eine verständliche Fragestellung, Einheit/Zeitraum, Quellennachweis, Stichprobengröße/Abdeckung, Qualitätszustand und einen Methoden-Drilldown. Keine künstlichen Fitnessscores, klinischen Aussagen oder erfundenen Messreihen. Fehlen verifizierte Daten, zeigt das Modul einen erklärten Leerzustand. 7/28/90 Tage nur dort anbieten, wo reale Daten und fachliche Eignung nachgewiesen sind. Alle bestehenden wissenschaftlichen Tile-Spezifikationen bleiben gültig; sie werden künftig als **Analysen-Modul** statt als überladene Startseiten-Kachel eingeordnet.

**Designreferenzen:** [Fitness-Icons](../../../design/icons/fitness-v1/README.md), [Research-Icons](../../../design/icons/research-v1/README.md), [Science-Mockups](../../../design/mockups/research-v1/). SVGs sind Entwürfe; Android-VectorDrawable-Konvertierung und Produktiv-UI sind separate Codex-Arbeit. Quellcode, Navigation und BLE bleiben durch diese Dokumentationsänderung unverändert.
