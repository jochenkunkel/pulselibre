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
documented separately and no additional hardware query was made.
