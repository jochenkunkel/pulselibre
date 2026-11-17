# PulseLibre dashboard v0.1 — evidence and mathematical ground rules

**2026-11-15.** First-pass read-only Android dashboard. Source: `CirqaLocalLog` private `cirqa_overnight.jsonl`. `CirqaDashboardReader` reads complete JSONL records only; malformed lines ignored. **No synthetic health data.** Home page preserves the original six placeholder cards and adds a separately identified CIRQA section; manual refresh avoids interfering with the running foreground recorder.

## Metric contract

| Value | Source | Mathematics / unit | Restrictions |
|---|---|---|---|
| Heart rate | Garmin realtime service 6 | direct u8 bpm | Valid 1–240 per decoder; not a diagnosis |
| Resting HR | service 6 | direct u8 bpm | 0/absent = unknown; no self-computed resting pulse |
| Steps | service 7 | unsigned 32-bit cumulative counter | **Last observation of current local calendar day**, not sum of observations; device day rollover not yet verified |
| Step goal | service 7 | unsigned 32-bit target | device-provided, not a WHO threshold |
| SpO₂ | service 19 | direct integer percentage | 255 unavailable, 0 invalid; no interpretation of isolated values |
| RR interval | service 12 | direct milliseconds | **Not RMSSD or SDNN**; HRV statistics require time-ordered RR intervals and artifact filtering |
| Stress | service 13 | proprietary 0–100 device scale | not a clinical stress score |
| Body Battery | service 20 | proprietary 0–100 device scale | not a validated clinical energy measure |
| Respiration | service 21 | direct breaths/minute | no inference of respiratory disease |
| Calories | service 8 | device-provided total and active integers | exact unit/scale still unverified on CIRQA, so dashboard **does not label as kcal** |
| Intensity | service 10 | moderate/vigorous device-provided minutes | Garmin accumulation window and weekly applicability unverified; do not compare directly with WHO weekly guideline |
| Sleep | not yet implemented FIT history | none | no sleep duration/stage estimates from sporadic realtime readings |

## Ground rules

1. **Provenance:** Only values decoded from registered Garmin services and written as `type=reading` may appear. No demo data or interpolation.
2. **Timestamp:** The JSONL timestamp is the **phone reception time in UTC**, not proven sensor acquisition time. Display local time and indicate if older than 15 minutes.
3. **Units:** Use measured or source-provided units only. No conversion to unsupported metrics; specifically no RR→RMSSD, no calorie energy balance, no sleep staging.
4. **Daily counters:** Do not sum cumulative step snapshots. Dashboard shows only latest steps from **today in phone's local zone**. Future improvement: compare device timezone/day boundary and account for counter resets.
5. **Statistics:** Avoid trend lines, means, correlations and confidence intervals without time-series coverage, missing-data policy and sufficient sample count. No 24h claims from a sparse 2-minute reconnect probe.
6. **Health:** No medical diagnosis or normal/abnormal color coding from an isolated consumer wearable reading. WHO guidance is general population advice, not derived from the device; user-specific recommendations need additional context.
7. **Missing values:** Display `Keine Messdaten` when a service is unsupported, absent or invalid. Retain last reading for other services with its actual reception timestamp, not label it live.
8. **Data privacy:** No cloud; private app storage; exported JSONL contains sensitive health measurements and is user-directed.
9. **Evidence labels:** CIRQA confirmed HR/steps from device test #193; other services are open-source-derived and unverified on CIRQA. No historical GFDI/FIT sync exists yet.
10. **No device reset:** Never send CLOSE_ALL, daily reset or delete/archive flags for dashboard purposes.

## Reference and verification

- Project: `docs/protocol/CIRQA_PROTOCOL_SPEC.md`, `docs/protocol/device-tests/CIRQA_OVERNIGHT_COLLECTION.md`, `GarminRealtime.kt`, `CirqaLocalLog.kt`.
- WHO physical activity guidelines: https://www.who.int/publications/i/item/9789240015128 (adult weekly moderate 150–300 min or vigorous 75–150 min; not a per-device realtime score).
- Heart-rate variability measurement conventions: Task Force of ESC/NASPE, *Heart Rate Variability: Standards of Measurement, Physiological Interpretation, and Clinical Use*, Circulation 1996; RMSSD is sqrt(mean of squared differences between successive **normal-to-normal** RR intervals), not a single RR sample.
- Source-backed byte layouts: https://github.com/gwerneckp/garmin-ble/tree/main/garmin_ble/metrics and https://github.com/Freeyourgadget/Gadgetbridge
- Verification checklist: build unit tests; check actual CIRQA overnight log; verify partial line handling, 32-bit counters, day rollover, stale display, empty log, 10 MiB cap and background service behavior.

**Known limits:** Local JSONL reader scans up to the 10 MiB cap on each manual refresh. Future database should index by service/time and support retention, deduplication and explicit provenance. Do not aggregate until the first real overnight export has been inspected.
