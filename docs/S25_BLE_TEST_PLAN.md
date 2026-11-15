# Samsung Galaxy S25 – BLE discovery test plan (2026-11-15)

## Goal
Test the actual Android APK on the user's Galaxy S25 rather than requiring an emulator. CI emulator remains a regression gate, not a substitute for phone + wearable validation.

## Safety rules
- Do **not** unpair, reset, re-bond or remove the wearable from Garmin Connect.
- Do **not** issue proprietary writes, subscribe to notifications, or modify BLE descriptors until the protocol and coexistence are verified.
- BLE discovery is **read-only service/characteristic enumeration**. No decoded pulse, oxygen, sleep or step values are claimed.
- The user must explicitly tap **Bluetooth-Geräte suchen**; the app requests Bluetooth permissions only at that point.
- Android 12+ uses BLUETOOTH_SCAN/CONNECT. No INTERNET permission, no location tracking and no cloud transport.
- Garmin Connect may retain a connection, and the wearable may not advertise or accept another GATT connection. **Do not disconnect Garmin Connect to force discovery.**

## Installation and phone verification
1. Wait for a **green CI run at the latest commit**; download its debug APK artifact from GitHub Actions. This is an unsigned-for-distribution development build, not a production release.
2. On the S25, install the APK only if comfortable allowing installation from the chosen source. Android may warn about development APKs. Do not disable system security protections globally.
3. Open PulseLibre and confirm dashboard labels show no invented health readings.
4. Tap **Bluetooth-Geräte suchen** and grant Nearby Devices permissions. Confirm the scan stops after 10 seconds, including after repeated scans.
5. If the wearable appears, tap its device entry. Observe connection/service information. Do not attempt re-pairing, PIN entry or resetting Garmin Connect.
6. Tap **Trennen**; confirm Garmin Connect continues working normally. Test Bluetooth-off and permission-revocation scenarios separately.
7. Test **Einstellungen · Datenexport**: choose a date range, save CSV, verify header and zero records if no decoded measurements are stored.
8. Report Android version, wearable exact model, whether it appears in the scan, service UUIDs and any error codes. Avoid sharing device MAC addresses or other identifiers.

## Automated checks
- JVM: BLE safety policy (read-property bit, no writes/subscriptions, bounded diagnostics), BLE state transitions, CSV export and date boundaries.
- Instrumentation: dashboard, export screen, SQLite transactions and persistence.
- Remaining device-only tests: coexistence with Garmin Connect, permission revocation, Bluetooth toggling, scan timeouts, real GATT discovery and connection cleanup.
- Remaining work: validated CIRQA protocol decoder, opt-in data collection, robust history sync, storage wiring and visualization.

## Known limitation
BLE GATT service discovery is not equivalent to a functional Garmin integration. This milestone is an **S25-ready diagnostic and discovery layer**, pending green CI and real phone validation.
