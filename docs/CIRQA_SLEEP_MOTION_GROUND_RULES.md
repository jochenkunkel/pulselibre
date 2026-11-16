# PulseLibre sleep and movement: scientific/engineering ground rules (v1)

## Evidence from our actual CIRQA implementation

`GarminRealtime.SUPPORTED_SERVICES` contains nine realtime services (6, 7, 8, 10, 12, 13, 19, 20, 21). **No raw accelerometer, gyroscope, orientation or activity-count service has been implemented or demonstrated on CIRQA.** Service 7 is cumulative steps, not movement intensity or actigraphy. Zero new steps cannot establish immobility, and a lack of readings cannot establish sleep.

The 2026-11-15/16 real-device JSONL records pulses, RR, respiration, SpO2, stress, Body Battery, steps and other values; it does **not** contain movement time series, native sample timestamps, sleep labels, accelerometer vectors or a validated sleep/wake ground truth.

## Clinical/scientific framing

Wrist actigraphy estimates sleep/wake from movement over short epochs. It can confuse quiet wakefulness with sleep and cannot reliably assign REM, N1, N2 or N3 stages from motion alone. The gold-standard sleep staging reference is polysomnography (EEG/EOG/EMG), not wrist stillness. Heart rate/HRV/respiration can be useful contextual features, but do not by themselves validate sleep stages or duration.

Ground rules:
1. Do not reward total stillness as an unconditional marker of good sleep; normal sleep includes movements, and prolonged immobility can be wakefulness.
2. Keep distinct: time in bed, user-reported sleep interval, estimated sleep duration, movement fragmentation, and autonomic overnight observations.
3. No sleep score 0–100 until the underlying estimators have been validated against an appropriate reference dataset and performance is reported (sensitivity, specificity, wake after sleep onset, error of total sleep time).
4. No REM/deep/light labels without validated sleep-stage classification and an appropriate reference.
5. No sleep-efficiency metric without independently estimated sleep/wake epochs and a time-in-bed interval.
6. Personal HR/HRV trends require multiple nights, quality filtering and gaps explicitly shown; do not infer RMSSD across BLE reconnection gaps.
7. Missing motion telemetry is **not** zero movement; label it `Bewegungsdaten nicht verfügbar`.
8. Avoid medical diagnoses from consumer SpO2 or breathing rate; show raw values and quality/coverage.

## Technical next step

Investigate whether CIRQA exposes an accelerometer/actigraphy stream through documented BLE services or historical Garmin FIT records. A step counter is not a substitute. Passive GATT discovery or read-only protocol inspection only; no CLOSE_ALL, pairing reset, persistent internet or device-side changes. If a movement stream is confirmed, record timestamped acceleration vector magnitude or activity counts in short epochs, quality/missingness, then validate sleep/wake inference against user-marked sleep intervals and, for medical-grade claims, PSG.

## Candidate dashboard v1

- `Nachtfenster`: user-entered bed and wake times, **not** asserted sleep duration
- `Bewegung`: unavailable until an actual motion sensor source is validated
- `Nächtlicher Puls`: range and time series with gaps
- `Atmung`: measured time series and missingness
- `RR`: raw values and quality; RMSSD only for contiguous artifact-filtered windows
- `SpO₂`: measured time series with device-quality caveat
- `Schlafscore`: unavailable (no validated sleep/wake ground truth)

Reference starting points: AASM clinical practice guideline on use of actigraphy (Smith et al., J Clin Sleep Med 2018, doi:10.5664/jcsm.7230); AASM scoring manual (PSG staging); de Zambotti et al., wearable sleep technology validation literature. These support the general methodology, **not** a CIRQA-specific validated algorithm.
