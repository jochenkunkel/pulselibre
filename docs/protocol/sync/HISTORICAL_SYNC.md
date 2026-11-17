# Historical BLE sync – offline-first design

## Why
The wearable is not continuously connected. Measurements made while out of Bluetooth range should appear under their original timestamps once a supported historical-data transfer becomes available. Do not imply that an unsupported Garmin protocol can already supply history.

## Contract
- Distinguish measurement timestamp from receive/import timestamp; show last successful sync in UI.
- Store local measurements persistently and transactionally; enforce a stable uniqueness key per device, metric and device-provided sample identifier (or documented timestamp/sample sequence fallback).
- Request history only when a **verified** device protocol supports it. Determine retention limits and whether historical HR, steps, sleep and SpO2 are available independently.
- Idempotent replay: retransmission and repeated reconnects must not create duplicate records.
- Import and sync checkpoint must commit **in the same database transaction**. Never advance cursor for a partial, failed or unpersisted batch.
- On disconnect retain stored data; on reconnect query the gap with overlap, validate packets, deduplicate, and continue.
- Device clock/time-zone drift, daylight-saving transitions, invalid samples, metric-specific sampling intervals and device resets require explicit handling.
- Reconnection must be bounded and battery-aware; Android background BLE restrictions and permissions must be considered. Do not promise guaranteed background sync.
- Never reset/unpair the band or interfere with existing Garmin Connect pairing.

## Implementation state
`HistorySync.kt` is a **pure in-memory merge/checkpoint model**, not a production database or Garmin sync implementation. Its tests cover chronology, replay, sample identity, invalid readings and cursor monotonicity. Production work requires a Room database, transactional DAO, a device-protocol adapter, integration tests and real-device verification with consent.

## Acceptance tests planned
1. Band out of range for 24h, then reconnect and backfill original timestamps.
2. Same batch delivered twice -> one record per unique sample.
3. Connection loss midway -> no skipped records or prematurely advanced cursor.
4. Process death during transaction -> rollback; safe retry.
5. DST/time-zone changes and clock drift do not silently rewrite measurement times.
6. Missing history on device -> show data gap, never synthesize values.
