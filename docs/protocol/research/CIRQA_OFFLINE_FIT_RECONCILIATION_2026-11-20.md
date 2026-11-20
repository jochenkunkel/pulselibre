# Offline FIT inventory and reconciliation (2026-11-20)

This report records a host-side, read-only analysis of the already available
private V26 copy. It does not describe a new phone transfer or establish
complete CIRQA history. Inputs were not modified. No raw bytes, device
identifiers, content hashes, measurement values, or exact timestamps are
included here.

## Scope and integrity

The private analysis input contained 42 content-addressed objects:

- 40 FIT files (198,719 bytes total), all accepted by the structural FIT
  inventory, including declared-length and CRC checks.
- 2 non-FIT files (20,232 bytes total), classified separately. One had
  well-formed XML framing; the other had valid protobuf wire framing but
  unknown schema/semantics. Neither was passed to a FIT decoder.

This is a different, larger snapshot than the 31 FIT + 2 Raw V25 set reported
on 2026-11-18. The aggregate byte counts should not be used to imply a transfer
or historical-coverage comparison between those snapshots.

The structural inventory used Garmin FIT Profile 21.222.0 as a schema
reference, not as proof that every field is semantically understood. Across
the FIT files it observed 70 distinct global message IDs. The files include
activity/record, monitoring, device-information, sleep, HRV, stress, SpO2,
respiration, and skin-temperature message families, as well as profile
unmapped message IDs. Five FIT files had no complete month-level timestamp
coverage; the other 35 had observed data in November 2026. These are
structural observations, not clinical interpretation or device-history
completeness.

Per-file aliases, exact sizes, field rows, and any higher-resolution temporal
details are kept in the private analysis output and are intentionally not
checked into the repository.

## Read-only database reconciliation

The canonical database snapshot was opened read-only. The inventory reported:

| Check | Result |
| --- | ---: |
| SQLite integrity | `ok` |
| Observation rows | 294 |
| Heart-rate rows | 129 |
| Distance rows | 129 |
| Speed rows | 36 |
| FIT sources | 40 |
| Provenance rows | 294 |
| Foreign-key violations | 0 |
| Duplicate primary keys | 0 |
| Offline decoder candidate keys | 294 |
| Candidate keys present in SQLite | 294 |
| Missing candidate keys | 0 |

This demonstrates that the current importable deterministic keys found by the
offline inventory are represented in the current canonical database snapshot.
It does **not** establish that all semantically meaningful FIT fields are
decoded, that the 294 rows are a complete semantic representation of the FIT
files, or that the stored files cover the device's full history. In particular,
the existence of additional message families is not a reason to import their
fields without validating profile semantics, units, timestamps, and quality.

## FIT metadata evidence (field presence only)

The profile-based structural inventory observed `file_id` and `device_info`
messages. Identifier-bearing FIT fields exist in the source files; their values
were neither copied into this report nor used as public device identity.
Within `device_info`, the observed battery-voltage and battery-level fields
contained no valid encoded values in this snapshot (their observed elements
were invalid/sentinel or missing). This is **not** evidence that the band has
no battery state; it only means these FIT fields did not provide a usable
battery reading here. Other metadata fields, including software-version
information, were observed in FIT structure, but no firmware value is
published or treated as a live/current device reading.

| Attribute | FIT evidence in this snapshot | Meaning / boundary |
| --- | --- | --- |
| Device/file identity | `file_id` and `device_info` messages with identifier-bearing fields | Values remain private; FIT identity is not equated with the paired CIRQA identity. |
| Battery voltage / level | Fields occur in `device_info`, but no valid encoded values were observed | No battery percentage, charging state, or freshness can be inferred. |
| Software version | Software-version field structure and values occur in some FIT metadata records | Historical file metadata only; not proof of currently installed firmware. |
| Hardware version | Field is part of the profile; no usable value was observed in the inspected records | Unknown for this evidence set. |
| Device clock / timezone | Timestamp fields and device settings occur in FIT messages | Not normalized into a trustworthy current band clock or timezone in this analysis. |
| Raw XML / protobuf metadata | XML framing valid; protobuf framing valid but schema unknown | Structure alone does not establish a battery, firmware, name, or live-state value. |

## Interpretation and remaining work

- The two non-FIT objects remain outside the FIT decoder path. XML structure
  and protobuf framing were identified, but their private values and the
  protobuf schema were not published or treated as verified device metadata.
- The decoder-candidate reconciliation is a key-level check, not a complete
  per-file/per-field explanation of decoder exclusions. A redacted field
  coverage review should establish, for each supported candidate, the reason
  for exclusion (unsupported field, invalid/sentinel, timestamp, quality,
  deduplication, or import status) before claiming semantic completeness.
- Existing source/provenance relations and the offline `FitHistoricalObservations`
  path provide a base for a backfill. A call-site audit found that
  `CirqaFitBackfill.run()` has no app production caller; the
  `importVerifiedFile()` helper is currently called from the BLE transfer path
  in `CirqaMlHandshakeProbe`. Therefore, the standalone no-BLE backfill required
  for already-stored originals is **not yet exposed as a production app action**.
  This analysis did not trigger a production backfill or mutate the database.
- The in-app diagnostic remains aggregate-only; this host report uses a private
  file-level matrix that is not included in Git.

Accordingly, #150 and #149 remain open for complete per-file semantic coverage,
metadata evidence, and backfill/pipeline acceptance. #144 remains open for the
broader storage architecture and migration acceptance.
