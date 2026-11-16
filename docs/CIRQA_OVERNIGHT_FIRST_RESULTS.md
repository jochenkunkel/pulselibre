# CIRQA overnight experiment — privacy-safe engineering summary

A real-device overnight experiment confirmed that bounded realtime BLE sampling can yield decoded observations across multiple services. The original user-provided JSONL dataset was **removed from the public repository** in commit 7d9d6a5 after a privacy review. The earlier version of this document also contained precise health-value ranges and timestamps and has been replaced.

## Technical conclusions (without personal measurements)

- Repeated BLE sampling is possible, but Android scheduling and GATT errors create gaps.
- The 20-second observation timeout is an expected bounded-session termination, not necessarily a failed measurement.
- Cumulative daily counters must not be summed across snapshots; reset boundaries need validation.
- HRV analysis must reject artifacts and gaps; SpO2 interpretation requires validated sensor quality and coverage.
- Real-time notifications do **not** establish historical FIT download capability.
- Device-specific raw recordings, exact time ranges, individual health values and derived health statistics must stay outside public Git history.

## Privacy remediation

The public Git history may still expose deleted data and earlier revisions of this report. Repository administrators must perform a coordinated history rewrite (e.g. git-filter-repo), invalidate old clones/caches where possible, and follow GitHub's sensitive-data removal guidance. Rewriting history disrupts existing clones and requires coordination. Never reintroduce real health data as test fixtures.
