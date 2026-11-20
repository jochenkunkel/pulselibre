# CIRQA private FIT copies: read-only re-reconciliation (2026-11-20)

## Scope and privacy

This report records a fresh host-local, read-only rerun against the previously
saved private V24/V25 and V26 source copies and their matching SQLite snapshots.
No CIRQA command, Bluetooth operation, app-data mutation, database write, or
source-file modification was performed. Source originals, hashes, filenames,
sample IDs, exact timestamps, values, and generated detailed matrices remain
outside Git in private local storage.

This verifies the archived copies and their matching snapshots only. It does
**not** prove the copies are currently present in the S25 app-private store or
that the CIRQA's entire history has been downloaded.

## Results

Both runs used Garmin FIT Python SDK profile 21.222.0, loaded as literal
profile data by `tools/fit_inventory.py` (the SDK module was not executed).

| Snapshot | Files inspected | FIT bytes | SQLite target | Reconciliation |
| --- | ---: | ---: | --- | --- |
| Earlier V24/V25 set | 31 FIT + 2 non-FIT | 126,089 FIT; 20,204 non-FIT | legacy `pulselibre_readings.db` | 294/294 deterministic decoder candidate keys present; 294 readings, no duplicate keys; SQLite integrity `ok` |
| V26 follow-up set | 40 FIT + 2 non-FIT | 198,719 FIT; 20,232 non-FIT | canonical `cirqa_measurements.db` | 294/294 deterministic decoder candidate keys present; 40 FIT sources, 294 observations and 294 provenance rows; no duplicate keys or foreign-key violations; SQLite integrity `ok` |

Each set's two non-FIT originals were structurally classified as one XML and
one schema-unknown protobuf-like file; neither was treated as FIT. The
profile-derived candidate counts match the stored sample IDs, but do not prove
semantic correctness of every decoded field or complete device-history
coverage.

## Tooling change

`tools/fit_inventory.py` previously supported read-only reconciliation only
against the legacy `readings` table. It now also recognizes the canonical
`observations` + `fit_observation_provenance` + `fit_sources` schema, reports
aggregate source/provenance and foreign-key counts, and compares the same
deterministic sample IDs. It fails closed for unknown schemas. A synthetic
canonical-schema regression test covers this path.

The installed S25 app reports build 1330 and is not debuggable; `adb run-as`
denies access. Therefore this rerun did not read current app-private files from
the phone. The existing local private copies remain the evidence source.

## Reproduction and verification

The two commands were run with private input/output paths (not included here):

```sh
python3 tools/fit_inventory.py --input-dir <private-V24/V25-copy> \
  --profile <private-Profile.py> --sqlite <private-pulselibre_readings.db> \
  --output <private-output>
python3 tools/fit_inventory.py --input-dir <private-V26-copy> \
  --profile <private-Profile.py> --sqlite <private-cirqa_measurements.db> \
  --output <private-output>
```

All 34 Python tool tests passed after the schema support was added. Local
`testDebugUnitTest`, `lintDebug`, and `assembleDebugAndroidTest` also passed.
Instrumentation tests and GitHub CI were not run in this work unit.
