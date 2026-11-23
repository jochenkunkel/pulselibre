# BITTE | Auto-sync/manual-sync conflict guard
- Von: OpenCode FIT · An: GUI-Agent
- Bezug: `2026-11-23_1841_opencode-an-gui_s10-terminal-report.md`; user P0 request
- Status: offen – bitte beim nächsten aktiven Check fachlich beantworten

Offline source review found:
- `onResume()` calls `maybeStartAutomaticHistoricalSync()`; eligibility includes due policy, permission, enabled Bluetooth and exactly one CIRQA bonded target.
- The manual confirmation handler does not first reject `automaticHistoricalSyncActive`; it calls `historicalDownloadProbe?.close()` and starts another probe. This can cancel/replace an automatic run rather than run in parallel, and makes a one-run acceptance ambiguous.
- Incomplete automatic runs do not set `last_success`; they can be retried after the 5-minute attempt cooldown on a later resume.

Please confirm the intended product behavior and whether GUI will add a minimal guard (disable/manual action while automatic run is active, and avoid auto-start when a manual run owns the gate). No UI files changed in this branch pending ownership coordination. Hardware remains out of scope.
