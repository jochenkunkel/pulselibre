# CIRQA overnight local collection experiment (2026-11-15)

## Purpose and scope

This is an **opt-in, user-started Android connected-device foreground service** to collect genuine CIRQA *realtime* metrics overnight, with no internet permission and no Garmin Connect. It is **not** the historical GFDI/FIT download described in `CIRQA_PROTOCOL_SPEC.md`. It must not be represented as uninterrupted, guaranteed overnight capture.

The service connects at approximately **120-second intervals**. Each existing Garmin ML session lasts up to **20 seconds**, registers GFDI then available realtime metric services, decodes validated notifications and disconnects. Android battery policies, BLE failures, sensor availability and foreground-service restrictions can interrupt sampling. There is a persistent notification with **Sammlung beenden** action. The user can also stop the service in the Devices tab.

## Storage format

Private application file: `filesDir/cirqa_overnight.jsonl`, **JSON Lines UTF-8**, append-only, hard limit **10 MiB**; app stops collection if the limit is reached. Android `allowBackup=false`. No MAC, BLE address, raw packet, cloud, external storage permission or synthetic measurements. A record has:
- `schema`: 1
- `received_at_utc`: ISO-8601 UTC time of receipt on the phone
- `type`: `session_start`, `reading`, `attempt` or `session_end`
- For `reading`: numeric Garmin service code and only validated service-specific fields, e.g. `bpm`, `resting_bpm`, `steps`, `goal`, `spo2_percent`, `rr_ms`, `stress`, `body_battery`, `breaths_per_min`, `total`, `active`, `moderate`, `vigorous`.
- For `attempt`: number and bounded human-readable outcome, including service rejection / missing data.
- For `session_start`: interval and collection mode; `session_end`: stop reason and counts.

The timestamps are **phone reception times**, not guaranteed original sensor sampling timestamps. A step value is a cumulative *daily* counter; do not sum readings. No aggregation, deduplication or historical FIT merge is performed yet. A repeated test adds more lines; a new session does not delete existing lines.

## User workflow

1. Install successful new APK over existing app without uninstalling (to preserve local data).
2. Stop Garmin Connect; ensure CIRQA remains paired, Bluetooth on, and phone has sufficient battery.
3. Devices → **CIRQA: Nachtaufzeichnung starten (lokal)** → confirm.
4. Verify persistent notification and keep phone charging overnight; do not force-stop PulseLibre. Android may still stop the service; no reliability claim.
5. Next morning Devices → **CIRQA: Nachtaufzeichnung stoppen** → **CIRQA: Lokale Dateistatistik** → **CIRQA: Nachtprotokoll exportieren**. The user explicitly chooses destination for the exported `.jsonl` file, which contains sensitive health readings; share only if intended.
6. Analyze number of samples, interval gaps, service coverage, file size, day-boundary behavior and repeated daily counters. Decide later whether to aggregate and which retention period is appropriate.

## Security and limitations

- Android connectedDevice foreground service uses `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_CONNECTED_DEVICE`, with already existing Bluetooth permissions; no Internet permission.
- 10 MiB hard cap; bounded per-session timeout; no `CLOSE_ALL`, no reset, no bond changes.
- Does not yet store raw GFDI messages, download FIT files, calculate daily/weekly/monthly aggregates or provide production-grade background reconnection guarantees.
- Source code must pass CI and a real-device test before overnight use is recommended as reliable. Battery optimization, firmware differences and foreground-service behavior require verification.
