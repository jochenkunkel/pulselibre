# Historical BLE sync – offline-first design

## Why
The wearable is not continuously connected. Measurements made while out of Bluetooth range should appear under their original timestamps through the now verified CIRQA historical transfer. The V24/V25 device runs prove successful transfer of the listed files, **not** complete coverage of the device's entire historical archive.

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

## Implementation state (verified V24/V25, 2026-11-18)
- Verified read-only device sequence: Connect-style `sync_request` (5043/5044), Item-List, PullItem, transfer-handle configuration and MLR data pipe (0x2018).
- 33/33 listed files transferred; 31 FIT (126,089 B) + 2 non-FIT raw files (20,204 B) privately stored. All FIT files independently pass size, header, CRC and SHA-256 checks.
- The same list is continued **only if `session_id` is present**; `next_transaction_id` is not a page cursor in this observed exchange. V25 ended after one page with 33 items.
- SQLite has 294 imported observations (`heart_rate=129`, `distance=129`, `speed=36`); `integrity_check=ok`, no duplicate primary keys, and V25 replay was idempotent.
- The pure `HistorySync.kt` merge/checkpoint helper is **not** the entire production sync implementation. Do not describe the proven file-store/SQLite import pipeline as absent; do not infer that all intended transactional checkpoint, reconnect, background, retention or history-completeness guarantees are proven.
- The 33 listed files **do not prove** exhaustive history. The two raw files are not yet semantically decoded. The current FIT parser supports a restricted set of Record/Monitoring fields; other fields and sleep-related content require the [offline field inventory](https://github.com/jochenkunkel/pulselibre/issues/141).
- No new device run is authorized by this document. V24/V25 one-shot gates consumed; no reset, archive, flag, delete or CLOSE_ALL commands.

Primary evidence: [V24](../device-tests/CIRQA_SYNC_V24_DEVICE_RESULT_2026-11-18.md), [V25](../device-tests/CIRQA_SYNC_V25_DEVICE_RESULT_2026-11-18.md), [historical data scope](../../HISTORICAL_DATA_SCOPE.md).

## Acceptance tests planned
1. Band out of range for 24h, then reconnect and backfill original timestamps.
2. Same batch delivered twice -> one record per unique sample.
3. Connection loss midway -> no skipped records or prematurely advanced cursor.
4. Process death during transaction -> rollback; safe retry.
5. DST/time-zone changes and clock drift do not silently rewrite measurement times.
6. Missing history on device -> show data gap, never synthesize values.
