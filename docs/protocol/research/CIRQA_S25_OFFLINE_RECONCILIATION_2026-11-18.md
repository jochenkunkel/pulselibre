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
- The decoder-derived candidate key set has 294 keys, all 294 present in the
  read-only SQLite snapshot. This confirms complete key-level reconciliation
  for the supported current decoder output, not correctness of every decoded
  value, coverage of unsupported fields, or completeness of the device archive.
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
`MassStorageMode`, and `TransferDirection`. This is schema/structure evidence
only: the private values were not promoted into repository documentation or
application storage. Existing BLE/GFDI evidence is limited to protocol
structure and statuses documented in the dated experiment reports; no
read-only live metadata query is established. No additional device request was
made.

## Evidence boundary and remaining work

The official profile source was fetched from Garmin's `garmin/fit-python-sdk`
release commit `7698bb8` and parsed by the inventory tool as literal data (not
executed). The report files generated from private inputs are also kept private.
The checked-in inventories from issue #141 remain the public, privacy-filtered
structural evidence.

The S25 database checks establish the observed stored data only. They do not
prove that 33 transferred files exhaust the CIRQA archive. Supported-key
reconciliation does not establish semantic value parity for every FIT field;
unsupported and unknown fields remain inventory-only. Battery metadata is
not available from these FIT `device_info` records. No production metadata
repository currently stores these candidates; implementing a privacy-filtered,
timestamped metadata store and fixture-backed offline extraction remains open.
