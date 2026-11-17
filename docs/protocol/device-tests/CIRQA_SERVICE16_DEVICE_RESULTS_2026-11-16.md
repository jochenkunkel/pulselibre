# CIRQA service 16: comparative on-device test (2026-11-16)

Evidence: user-provided PulseLibre Android screenshots, two 20-second tests. Screenshots are not raw packet captures.

| Test | Registration | Dynamic handle | Start 0x01 | Service-16-routed notifications | Decoded XYZ |
|---|---|---|---|---|---|
| Registration-only | accepted | 120 | no | 0 | no |
| MousePods-style | accepted | 122 | yes (write initiated; GATT confirmation not individually reported) | 159 | no |

**Conclusion:** On CIRQA, registration of service 16 succeeds and service-16-handle-routed notifications arrive after a MousePods-style start. The 159 packets are not yet proven to be acceleration samples. Handle values vary per session and must never be hardcoded. The 0 generic valid readings in the UI count only the nine existing health metric decoders and **do not contradict** the 159 service-16 notifications.

**Open engineering work:**

1. Preserve bounded diagnostic packet length distribution and anonymized first few service-16 payload hex samples locally, with explicit opt-in, to validate framing against the MousePods quatix 7 format. Avoid indiscriminate storage of continuous raw motion.
2. Verify start write result and callback separately; current `motionStartIssued` means attempted, not necessarily acknowledged.
3. Compare observed packet sizes, time intervals, timestamp rollover and bit packing to `starman-tech/mousepods` and `gwerneckp/garmin-ble`; test still-vs-moving behavior before declaring acceleration units or sample rate.
4. Review command serialization: current code calls `send(start)` and then `nextRealtimeService` (no additional register for one candidate). The diagnostic `send` function labels a service-specific command as registration when `pendingService > 0`; improve status messages.
5. Review stop semantics: currently best-effort `0x00` followed by 400ms delayed disconnect, without confirmed completion. Add explicit write-state tracking and bounded teardown; no CLOSE_ALL, no unpairing.
6. Once validated, expose accelerometer via a separate module and explicit local storage policy; don't infer sleep stages from raw data without validation.

**Do not conclude** that timing differences explain missing data: start activation itself is the strongest observed difference. The test is a promising transport result, not yet a validated motion sensor.
