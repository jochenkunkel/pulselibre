# S25 offline FIT and SQLite reconciliation (18.11.2026)

## Scope and privacy

This is a read-only check of the existing private S25 app files and database
snapshots. No CIRQA synchronization, Bluetooth operation, database mutation, or
source-data publication was performed. Device identifiers, filenames, hashes,
timestamps, coordinates, and measured values are intentionally omitted here.
The private inputs remain under `/tmp/opencode/issue-141-private/` and
`/tmp/opencode/issue-150-private/`; neither directory is part of this repository.

## Verification

- The S25 app-private store and its existing local copy contain the same 33
  content hashes: 31 FIT files and 2 non-FIT files, 146,293 bytes total.
- Read-only snapshots of both app databases pass SQLite `integrity_check`;
  `foreign_key_check` reports no violations.
- `cirqa_measurements.db` contains 117,526 events. This is a separate event
  stream and is not treated as equivalent to the historical measurement store.
- `pulselibre_readings.db` contains 294 historical readings: 129 heart-rate,
  129 distance, and 36 speed observations, with no duplicate primary keys.
- Re-running `tools/fit_inventory.py` on the private originals using Garmin's
  official FIT Python SDK 21.222.0 profile reports 31 structurally valid FIT
  files, 126,089 FIT bytes, and 20,204 bytes across the two non-FIT files.
- The Kotlin decoder emitted 761 profile-mapped observation candidates. After
  timestamp/quality import filters, its 294 importable keys are all present in
  the read-only SQLite snapshot. The remaining 467 monitoring-heart-rate
  candidates lack eligible full FIT timestamps and are excluded. This confirms
  complete key-level reconciliation for importable current output, not semantic
  correctness of every value, coverage of unsupported fields, or completeness
  of the device archive.
- The non-FIT files are structurally classified as one XML file and one
  unrecognized protobuf/schema-like binary. Their contents are not published;
  no device metadata meaning is inferred from the unknown binary.
- The XML has root element `Device` and includes schema fields for device-level
  `Model`, `SoftwareVersion`, `MassStorageMode`, `DeviceExtension`, and 51
  file-entry groups (`FileName`, `Path`, `Location`, `DataType`, `Identifier`,
  `Specification`, `TransferDirection`, and related version/name fields).
  Only field names and counts are evidence in this report: actual text values,
  identifiers, and paths remain private and were not inferred or reproduced.
- The second raw file contains one top-level protobuf wire-type-2 field with a
  226-byte payload. Without its schema, it remains opaque; neither its field
  semantics nor any alleged battery, firmware, or sensor information is claimed.

## Privacy-limited device-metadata coverage

The accompanying structural inventory was rechecked against FIT Profile
21.222.0. The figures below count valid encoded elements, not distinct devices
or proof that a value describes CIRQA itself. FIT `device_info` records can
describe multiple attached sensors. Actual values, record timestamps, serials,
and filenames remain private.

| Candidate | Observed structural coverage | Source / freshness | Interpretation |
| --- | ---: | --- | --- |
| FIT device-info timestamp | 100/100 records valid, across 26 files | Timestamp field in the record; exact times withheld | Per-record time evidence exists; not a current/live clock reading |
| Device type | 60/98 values valid, across 24 files | FIT `device_info`; capture time represented by its timestamp | Profile-coded type only; not independently identified as the band |
| Manufacturer / product | 36/100 / 40/100 values valid, across 26 files | FIT `device_info`; record timestamp | Numeric profile identifiers; values withheld and not presented as a user-facing name |
| Software version | 68/100 values valid, across 26 files | FIT `device_info`; record timestamp | Encoded version values exist; no claim that they are the latest firmware |
| Serial number | 40/100 values structurally valid, across 26 files | FIT `device_info`; record timestamp | Sensitive identifier deliberately neither copied nor persisted by this inventory |
| Battery voltage / status / level | 0/98 valid values for each, across 24 files | FIT `device_info`; records have timestamps | No usable battery evidence in this sample |
| Device name / firmware text | Schema fields only, no value evidence published | XML `Device` schema; snapshot time private | Not established from the privacy-filtered material |
| Sync status / band clock | No validated current-state value | XML has file-transfer schema fields; FIT has historical timestamps | Transfer capability/schema is not evidence of current sync state or clock accuracy |
| Unknown protobuf metadata | One length-delimited field, 226-byte payload | Raw file; snapshot time private | Schema unknown; opaque and not decoded |

The XML inventory establishes a `Device` root, 726 elements, 51 file-entry
groups, and schema fields including `Model`, `SoftwareVersion`,
`MassStorageMode`, and `TransferDirection`. A hardened offline parser now
extracts only the software-version text under `Model`, a boolean
`MassStorageMode` value, and the count of `TransferDirection` elements. Tests
cover valid and empty fields, malformed XML, DTD/entity rejection, and opaque
binary input. On the available private pair, one file was XML with a populated
software-version field, empty mass-storage text, and 51 transfer-direction
elements; the other was rejected as this XML format. The Kotlin parser was also
run directly against these two private originals and produced the same
classification and field-presence/count results. Values were not printed or
copied into repository documentation.

These remain snapshot-derived structural observations, not current device
state. Raw metadata is deliberately not persisted in the application database
or backup yet: the existing raw-file storage has no privacy-reviewed metadata
record contract, and raw identifiers/paths must not enter persistence. Adding
such persistence needs an explicit whitelist schema, retention/linkage and
backup/restore review. Existing BLE/GFDI evidence is limited to protocol
structure and statuses documented in the dated experiment reports; no
read-only live metadata query is established. No additional device request was
made.

## FIT backfill runtime validation (19.11.2026)

The current Kotlin decoder was run against the same 31 private FIT originals;
all 31 passed its verified-file checks. It emitted 761 profile-mapped
observation candidates, of which 294 had importable timestamp/quality and were
committed to an isolated disposable schema-v4 database. The other 467 heart-rate
candidates have no eligible full FIT timestamp and were not imported. The
disposable database contained 31 FIT sources, 294 observations, 294 provenance
rows, and 100 whitelisted `device_info` rows; `foreign_key_check` was clean.
Repeating the backfill left those row counts unchanged. No existing app database
or source original was modified, and no measurement values or identifiers were
printed or retained in the repository.

The 100 metadata records had valid FIT record timestamps. Valid encoded
elements included 60 device-type, 36 manufacturer, 40 product, and 68 software
version values. Battery voltage, status, and level each had zero valid values.
These remain FIT profile codes/fields, not verified CIRQA identity, latest
firmware, or current battery state.

## Evidence boundary and remaining work

The official profile source was fetched from Garmin's `garmin/fit-python-sdk`
release commit `7698bb8` and parsed by the inventory tool as literal data (not
executed). The report files generated from private inputs are also kept private.
The checked-in inventories from issue #141 remain the public, privacy-filtered
structural evidence.

The S25 database checks establish the observed stored data only. They do not
prove that 33 transferred files exhaust the CIRQA archive. Supported-key
reconciliation does not establish semantic value parity for every FIT field;
unsupported and unknown fields remain inventory-only. Battery metadata is not
available from these FIT `device_info` records. Schema v4 now provides
privacy-filtered timestamped storage for a FIT `device_info` whitelist. The
verified originals were replayed into a disposable database to validate
extraction and idempotency; the user's existing database was not modified.
Opaque protobuf metadata remains undecoded and unstored; XML extraction is
limited to the three whitelisted fields above, which are not persisted.
