# CIRQA active-GATT entry-point audit (2026-11-20)

## Scope and evidence

Static audit of `origin/main` commit `2373aefb2be6a4e7c4f96adcb4c172c7c943ec5c` and the active-download candidate in PR #289. This is a code-path review, **not** a new Bluetooth session or a hardware interoperability result. No scan, connect, GATT operation, instrumentation test, or device setting change was made for this audit.

## Active transport entry points

`connectGatt()` occurs in two production Kotlin classes:

| Entry | Caller and guard | Transport behavior | Evidence limits |
| --- | --- | --- | --- |
| `CirqaGattProbe.start()` | `CirqaTestLaboratoryActivity.confirmGattDiscovery()`; explicit confirmation dialog | Temporary LE connection, service/characteristic discovery only, 15-second timeout; disconnects/closes. No characteristic read/write or notification subscription. | The GATT connection itself can affect Garmin Connect. No S25 coexistence test is claimed here. |
| `CirqaMlHandshakeProbe.startRealtime()` | Continuous recorder, test-laboratory probes, and the history-download candidate | Active Garmin ML/GFDI path; can register services, enable notifications and write protocol messages. It is **not** the read-only discovery path. | Method presence does not prove every command meaning or successful data access. Review each caller and gate. |

### User-facing callers

- **Continuous realtime recording:** the Device screen’s `Starten` action explains the active connection and retries, then requires an explicit confirmation before `ACTION_START`. The settings option “Beim App-Start automatisch aufzeichnen” defaults off. If the user turns it on, the app may start on a fresh launch only when Bluetooth is already on, permissions are already granted, and the service is not running. It never enables Bluetooth or requests permissions from the autostart path. The foreground service and test-laboratory activity are `android:exported="false"`.
- **Read-only GATT discovery:** hidden Testlabor entry is a deliberate long press. Discovery has its own confirmation, a 15-second limit, and metadata-only behavior.
- **Live ML probe:** Testlabor has a separate confirmation describing notification subscription and Garmin-service registrations. It is not implied by the read-only discovery confirmation.
- **Historical download (PR #289):** the visible Device action checks permissions and presents a separate confirmation explaining the request and possible device-side “transferred” marking. Only the positive action calls `startHistoricalDownload(userConfirmed = true)`. The UI says it sends no delete/archive/SetFlags/reset command and that one page does not prove full history.
- **Stopping a running recorder:** the Stop button and ongoing notification request `ACTION_STOP`; database restore/reset flows separately ask before stopping the recorder. No start action is attached to a database operation.

## Static findings

1. The visible recorder start is confirmation-gated; app-start autostart is a separate, default-off opt-in with additional runtime guards.
2. GATT service discovery and the ML probe are separate paths with materially different capabilities. Documentation should not label all GATT activity “read-only.”
3. The historical download and realtime recorder share `CirqaMlHandshakeProbe`, so review of future changes must preserve caller-specific intent/gates; a new public method on that class is not itself authorization to invoke it.
4. Static inspection cannot prove that Android/Garmin Connect coexistence is safe, that an active ML command has the intended firmware effect, or that the band returns complete history.

## Verification status / remaining work

- Existing offline unit tests cover the default-off/autostart decision gates. The full JVM suite `:app:testDebugUnitTest` passed locally on the audited `origin/main` commit. This does not replace a full CI result.
- No new GATT command was issued. No Android instrumentation test was run on the S25.
- Still required for issue #2: full CI on the current PR head and a separately approved, bounded S25 interoperability check of an existing path. Do not claim Garmin Connect coexistence or production-safe live ML behavior until that is measured.
- Historical coverage remains `device-history-unverified`; a drained single-page run is not a full-history proof.
