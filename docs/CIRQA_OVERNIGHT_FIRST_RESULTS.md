# CIRQA overnight run: first analysis (2026-11-16)

Source: `data/overnight/pulselibre-cirqa-overnight.jsonl` (user-uploaded, contains sensitive health data). This document deliberately records aggregate technical findings only.

- 5,140 valid JSONL records; 5,055 `reading`, 83 `attempt`, one `session_start`, one `session_end` (`user_stop`). Repository blob size: 587,555 bytes.
- Start 2026-11-15 22:03:57 UTC, end 2026-11-16 04:35:43 UTC: 6h 31m 46s.
- Counts by service: HR 6=758; steps 7=83; calories 8=101; intensity 10=80; RR 12=1,841; stress 13=489; SpO2 19=1,423; Body Battery 20=81; respiration 21=199.
- 80 attempt summaries ended by the configured 20-second timeout; 3 were GATT 133 failures. The timer is the expected bounded session termination, not automatically a failed measurement: valid readings were received.
- Intervals between recorded attempt completion timestamps: median ~140s; max ~1215s; 20 of 82 intervals >300s. These are NOT precise attempt-start intervals, because the original APK did not record `attempt_start`.
- Step counter observed 10,377 before device reset to 0 then 20, with goal changing from 8,830 to 8,990. Reset was observed before midnight UTC; day-boundary mapping to device timezone must be verified. **Never sum snapshots.** Dashboard currently filters step snapshots by phone local calendar date, which does not necessarily equal Garmin's reset boundary.
- HR 59–102 bpm; RR 580–1,113 ms; SpO2 87–100%; stress 0–67; Body Battery 5–41; respiration 8–27/min. These are unvalidated consumer-device readings, not diagnoses.
- All nine service IDs produced valid decoded readings. Historical FIT download, sleep stages and accurate native device sample timestamps remain unimplemented.

## Engineering decisions

1. **Keep raw observations** until retention, quality and aggregation rules are tested. ~0.59 MB/6.5h is small; avoid irreversible lossy aggregation at this stage.
2. **Add provenance and quality**: per-connection attempt start/end, decoder version, service, monotonic capture session ID, source timestamp if proven, explicit gaps and Bluetooth failures.
3. **Counter reset**: detect decreases and annotate reset events. Never aggregate by summing daily counter snapshots; day boundary must be derived/validated from device behavior rather than assuming UTC midnight.
4. **HRV**: compute RMSSD only from contiguous, ordered NN intervals after artifact filtering; never bridge BLE gaps.
5. **SpO2**: assess coverage, contiguous episodes, missingness and sensor quality before any interpretation; isolated values are not medical evidence.
6. **Schedule**: the old build did perform 83 cycles. The independent timer/heartbeat change in later source is a robustness enhancement, not proof of the old scheduler failing. Test new build separately, and ensure a foreground-service crash/termination is visible.
7. **Data handling**: avoid committing future sensitive raw health exports to a public repository. Prefer private encrypted transfer or redacted aggregate statistics.
