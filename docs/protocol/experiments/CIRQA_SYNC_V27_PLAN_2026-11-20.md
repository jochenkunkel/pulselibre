# CIRQA V27 bounded sync plan

## Authorization and purpose

On 2026-11-20 the project owner explicitly authorized one further bounded
BLE/sync run and local database writes for V27. The purpose is to see whether
the current S25/CIRQA listing changes relative to V26 and validate the current
verified-file → FIT decode → canonical SQLite import path. This is not a claim
that another sync can establish complete device-history coverage.

The previously consumed `cirqaSyncAttemptedV25` preference is left untouched.
V27 uses a distinct one-shot preference and result key; no old gate is cleared,
re-armed, or migrated.

The owner also explicitly authorized pausing and resuming the currently active
`CirqaContinuousRecordingService` for this single run, accepting the brief live
sampling gap. The service is not mutually excluded from historical sync, so it
must be stopped before V27 starts and restarted after the sync has fully
terminated.

## Preconditions

- Install only a CI-signed in-place update with the established signing
  certificate and a higher version code. Do not uninstall, clear app data, or
  use `run-as`/root.
- Confirm the app version, S25 USB connection, Bluetooth permissions, and
  exactly one bonded CIRQA target using read-only checks.
- Ensure the recorder and any existing BLE/GATT session are inactive. The app's
  shared BLE-session guard must report the session available. The active
  continuous recorder may be stopped only for this authorized V27 window and
  must be restarted afterwards.
- Inspect the existing aggregate-only diagnostic before and after. If the
  app/database backup or upgrade path is not verified, stop before device
  sync.
- The user starts the existing Test Laboratory sync action; the app consumes
  the V27 one-shot key before attempting to connect.

## Bounded operation and stop conditions

- Use only the existing Connect-style `sync_request` → FileAccess list → MLR
  pull path. One run, 120-second cap, at most 256 files and 64 MiB total, zero
  retry.
- Store only complete, exact-size-validated originals privately; FIT files
  additionally require valid header/file CRC and content-addressed SHA-256.
- Import only supported, timestamped, profile-valid observations using the
  existing idempotent transaction/provenance path.
- No archive, file-flag, delete, reset, unpair, `CLOSE_ALL`, or device-write
  command. Do not reset the V25 gate or repeat V27 after timeout/failure.
- Stop immediately on an unexpected protocol response, competing BLE session,
  user-visible pairing warning, transfer-bound violation, DB integrity/FK
  failure, or any attempt to send an unrecognized/destructive command.

The device may mark buffered data as transferred in response to the
`sync_request`; that known side effect is within this explicit V27 authorization.
It must be disclosed in the final result.

## Evidence handling

Record only redacted aggregates: list/file totals, transfer success/failure,
FIT validation totals, SQLite integrity/FK/row-count deltas, and import
idempotency. Keep original files, identifiers, hashes, exact timestamps, and
measurement values private. Do not claim complete history from a single
bounded list response.

## Current status

Plan only; no V27 device command has been sent yet. The installed S25 app is
non-debuggable (`adb run-as` is unavailable). A CI-signed update and full
post-merge CI must be checked before installation or device testing. Emulator
instrumentation remains in CI only; never run it on the S25.
