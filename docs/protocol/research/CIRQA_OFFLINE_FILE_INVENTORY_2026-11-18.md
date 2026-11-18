# CIRQA offline file and FIT field inventory (18.11.2026)

## Scope and privacy

This is a read-only analysis of the 33 private files already received in the
V24/V25 transfer: 31 FIT files (126,089 bytes) and two non-FIT files (20,204
bytes). No device connection, BLE test, gate reset, file mutation, or raw-file
commit was part of this work. The set is the observed transfer, **not** a
proven-complete historical archive.

The committed matrices contain per-file aliases (the first 12 hex characters
of the content SHA-256), file sizes, observed FIT message/field structure,
counts, and month-level date coverage only. They contain no source bytes,
full content hashes, filenames/UUIDs, timestamp values, location coordinates,
or health-measurement values. Health-related *message types* are identified
because their presence is material to the inventory. Garmin Profile names and
units are reference mappings, not evidence of clinical interpretation or
device/app presentation.

## Results

| Evidence | Observation |
| --- | --- |
| FIT envelope and independent decode | 31/31 FIT files passed the existing exact-size, magic, header/file CRC and SHA-256 checks. Garmin FitCSVTool 21.222.0 decoded all 31 with integrity checking and no decode errors. |
| Definitions and fields | 510 FIT message definitions and 10,181 field-definition entries. The local parser emits 9,784 FIT file/message/field rows (9,786 CSV data rows including the two raw files). |
| Data records | 5,511 total FIT data records. |
| Developer fields | None observed in the 31 FIT files. |
| Profile mapping | Garmin FIT Profile 21.222.0. There are 69 observed global message numbers; 31 had no message name in that profile and remain explicitly unknown. Of the file/message/field rows, 2,496 have no profile field name; these are retained by number/type without guessed names. |
| Time coverage | For FIT records with a complete timestamp, observed coverage is November 2026 at month precision. Five files had no complete timestamp for their records. No individual timestamp is included. |
| XML raw file | 19,975 bytes; well-formed XML, `Device` root, 726 elements. This structure is consistent with the previously noted GDXml hypothesis; no schema-level content interpretation was attempted. Element counts and attribute names are included; text and attribute values are not. |
| Protobuf raw file | 229 bytes; protobuf wire framing validates. Schema and semantic meaning remain unknown; only field numbers, wire types, occurrence counts and length distributions are recorded. Recursively parsed length-delimited shapes are structural candidates, not asserted submessage semantics. |
| SQLite import | Read-only `integrity_check=ok`; 294 rows: 129 `heart_rate`, 129 `distance`, 36 `speed`; zero duplicate primary keys. The Kotlin decoder emits 761 supported-profile candidates; after timestamp/quality filters, all 294 importable candidate keys match SQLite exactly. |

The FIT inventory contains message families with health-sensitive semantics,
including monitoring, monitoring heart rate, stress, SpO₂, respiration rate,
sleep level and assessment, HRV, overnight skin temperature, and sleep
disruption. Their presence/counts are evidence of encoded message types only;
the inventory deliberately does not expose their measured values.

The FIT `monitoring_hr_data` message contains **467** heart-rate records with
`timestamp_16`; none of these records has a full FIT timestamp. These values
are not part of the time-based SQLite import and are not assigned reconstructed
times here.

## Gesundheits- und Importstatus

| Bereich | Tatsächlich beobachtete Dateievidenz | SQLite / Grenze |
| --- | --- | --- |
| Herzfrequenz | `record.heart_rate` (129 Records); `monitoring_hr_data` resting/current-day HR (3 Records); `monitoring.heart_rate` (467 Records, nur `timestamp_16`) | 129 `heart_rate` rows aus Record-Feldern; die Monitoring-Felder sind nicht im zeitbasierten Import. |
| Distanz / Geschwindigkeit | `record.distance` (129); `record.enhanced_speed` (36) | 129 `distance` + 36 `speed` Zeilen. |
| Trittfrequenz / Leistung | Keine entsprechenden Record-Felder in den 31 Dateien beobachtet. | Nicht importiert; ein Profilmapping allein wäre kein Dateinachweis. |
| Schritte / Aktivität | `monitoring.cycles` (41 Records), aktive Kalorien (41), moderate Aktivitätsminuten (3) und intensive Aktivitätsminuten (1). | Keine dieser Monitoring-Metriken in SQLite. `cycles` wird hier nicht ohne zusätzliche Profil-/Geräteevidenz als konkrete Schrittzahl ausgegeben. |
| Stress / Atemfrequenz | `stress_level` (576 Records); `respiration_rate` (576 Records). | Nicht in den 294 SQLite-Zeilen. Für Stress wurde kein vollständiger FIT-Zeitstempel pro Record beobachtet; Atemfrequenzrecords haben einen vollständigen FIT-Zeitstempel. |
| SpO₂ | `spo2_data` (576 Records) mit vollständigem FIT-Zeitstempel. | Nicht in SQLite. |
| Schlaf | `sleep_level` (27 Records), `sleep_assessment` (1 Nachricht mit Score-benannten Profilfeldern), Schlafunterbrechungsnachrichten (7 Records) sowie `nap_event` (8 Records). | Nicht in SQLite. Die einzelne Assessment-Nachricht hat keinen vollständigen Nachrichtentimestamp. Diese Feldnamen belegen weder Genauigkeit noch, ob ein Score vom Gerät oder von Connect erzeugt/berechnet wurde. |
| HRV | `hrv_status_summary` (1 Nachricht) und `hrv_value` (75 Records), Profilfelder mit Einheit `ms`. | Nicht in SQLite; keine klinische Interpretation vorgenommen. |
| Temperatur | `skin_temp_overnight` (1 Nachricht mit temperaturbezogenen Profilfeldern). | Nicht in SQLite; Quelle, Messmethode und klinische Bedeutung damit nicht validiert. |
| Ruhepuls | Profilfelder `resting_heart_rate` und `current_day_resting_heart_rate` in 3 `monitoring_hr_data`-Records. | Nicht in SQLite. |
| Body Battery | Kein entsprechend benanntes Feld im aktuellen Profilmapping beobachtet. | In unbekannten globalen Nachrichten/Feldern nicht ausschließbar. |
| Connect-Dateitypen | Separater Connect-Verkehrsnachweis nennt `MONITORING_B`, `SLEEP_DATA`, `BIOMETRIC` und `TRUE_UP`. | Das ordnet diese Connect-Dateitypen nicht einzelnen der 33 lokalen Dateien zu. Die rohen ItemList-UIDs/Transaction-IDs wurden nicht in die veröffentlichte Inventur übernommen. |

Damit ist das Vorhandensein FIT-kodierter Schlaf-/HRV-/Biometrie-Nachrichten
belegt, nicht aber eine schlafmedizinische Interpretation, eine Garmin-Connect-
Darstellung oder ein aktueller App-Import dieser Daten.

## Artifacts

- [`CIRQA_FIT_FILE_INVENTORY_2026-11-18.json`](CIRQA_FIT_FILE_INVENTORY_2026-11-18.json):
  per-file aliases, sizes, message counts, monthly coverage, non-FIT structural
  summaries and aggregate SQLite comparison.
- [`CIRQA_FIT_FIELD_INVENTORY_2026-11-18.csv`](CIRQA_FIT_FIELD_INVENTORY_2026-11-18.csv):
  one row per observed FIT file/message/field plus one row per non-FIT file.
  Includes observed base type and encoded size separately from Profile scale,
  offset and unit; definition, record, valid/sentinel/missing and timestamp
  coverage counts; and persisted SQLite key-match counts.
- [`CIRQA_FIT_INVENTORY_MATRICES_2026-11-18.md`](CIRQA_FIT_INVENTORY_MATRICES_2026-11-18.md):
  readable per-file and per-message/field Markdown matrices, plus the import
  coverage summary.
- [`CIRQA_FIT_BASE_TYPES.md`](CIRQA_FIT_BASE_TYPES.md):
  explanation of FIT base-type IDs, widths, byte-order flags, sentinel values,
  arrays, and how to read the inventory counts.

`record_count` counts data records in which the field is present. Value counts
are counts of encoded scalar elements, not decoded values. The invalid/missing
count combines FIT invalid sentinels with records of that message where the
field is absent. Timestamp coverage has separate fields; `first_timestamp`
and `last_timestamp` contain `YYYY-MM` only.

The `currently_imported` / `imported_value_count` columns mean matching
idempotency keys were found in the existing SQLite copy for candidates emitted
by the current decoder mapping and eligible for import after timestamp/quality
filters. The decoder also reports 467 monitoring-heart-rate candidates without
an eligible full FIT timestamp; they are excluded from import. These counts do
not mean a field is fully represented or presented by the application. No
values are copied from SQLite.

## Reproduction

Run from the repository root with the existing private archive extracted
outside Git and a read-only SQLite copy. Obtain Garmin's FIT Python SDK
`Profile.py` at tag `21.222.0` from:
<https://raw.githubusercontent.com/garmin/fit-python-sdk/21.222.0/garmin_fit_sdk/profile.py>.
The profile is loaded using Python's literal AST parser; its code is not
executed. The script has no third-party Python dependencies.

```sh
python3 tools/fit_inventory.py \
  --input-dir /path/to/private/files \
  --profile /path/to/profile.py \
  --sqlite /path/to/private-readings.db \
  --output /tmp/cirqa-inventory
python3 -m unittest discover -s tools -p 'test_*.py' -v
```

The input directory must contain `verified_fit/` and `verified_raw/`.
The generated output directory should remain outside Git; the committed
aggregate artifacts are the reviewed, privacy-filtered outputs.

## Evidence boundaries and follow-up

- The profile describes standardized FIT fields; it does not establish the
  device's clinical semantics or prove what Garmin Connect displays.
- No unknown global message, field, or protobuf meaning is inferred.
- The two non-FIT file contents were not decoded as health data.
- The 33-file transfer does not establish complete historical coverage.
1. First, independently validate bounded `timestamp_16` reconstruction against
   a trusted reference before considering those 467 monitoring-heart-rate
   records for timestamped import.
2. Then assess profile-valid health fields not currently imported (sleep,
   HRV, SpO₂, respiration, stress and skin temperature) with separate
   provenance/quality criteria. Neither item is part of this inventory.
