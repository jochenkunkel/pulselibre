# Samsung Galaxy S25 – BLE discovery test plan (2026-11-15)

## Goal
Test the actual Android APK on the user's Galaxy S25 rather than requiring an emulator. CI emulator remains a regression gate, not a substitute for phone + wearable validation.

## Safety rules
- Do **not** unpair, reset, re-bond or remove the wearable from Garmin Connect.
- Do **not** issue proprietary writes, subscribe to notifications, or modify BLE descriptors until the protocol and coexistence are verified.
- Current BLE discovery is **passive advertisement scanning only**; the UI does not initiate GATT connections. No decoded pulse, oxygen, sleep or step values are claimed.
- On first app launch, Android asks for missing Bluetooth/Nearby Devices permissions once. Later launches do not automatically repeat the prompt; tapping **Bluetooth-Geräte suchen** can request missing permissions again. No scan starts merely from granting permission.
- Android 12+ uses BLUETOOTH_SCAN/CONNECT. No INTERNET permission, no location tracking and no cloud transport.
- Garmin Connect may retain a connection, and the wearable may not advertise or accept another GATT connection. **Do not disconnect Garmin Connect to force discovery.**

## Installation and phone verification
1. Wait for a **green CI run at the latest commit**; download its debug APK artifact from GitHub Actions. This is an unsigned-for-distribution development build, not a production release.
2. On the S25, install the APK only if comfortable allowing installation from the chosen source. Android may warn about development APKs. Do not disable system security protections globally.
3. Open PulseLibre and confirm dashboard labels show no invented health readings.
4. On first launch, grant Nearby Devices permissions in the Android system prompt (if not already granted). Then tap **Bluetooth-Geräte suchen**. Confirm the scan stops after 60 seconds, including after repeated scans. Observe live RSSI, manufacturer IDs, service UUIDs and service-data fields.
5. Tap **Gekoppelte Geräte anzeigen** to inspect Android bond names; this is not proof of advertising or connection. Do not tap an unknown device to connect, re-pair, enter a PIN, or reset Garmin Connect.
6. Use **Diagnose teilen** only after reviewing the generated text. It may contain Bluetooth device names, manufacturer data, service data and identifiers; redact sensitive values before sharing. Confirm Garmin Connect still works normally. Test Bluetooth-off and permission-revocation scenarios separately.
7. Test **Einstellungen · Datenexport**: choose a date range, save CSV, verify header and zero records if no decoded measurements are stored.
8. Report Android version, wearable exact model, whether it appears in the scan, service UUIDs and any error codes. Avoid sharing device MAC addresses or other identifiers.

## Automated checks
- JVM: BLE safety policy (read-property bit, no writes/subscriptions, bounded diagnostics), BLE state transitions, CSV export and date boundaries.
- Instrumentation: dashboard, export screen, SQLite transactions and persistence.
- Remaining device-only tests: coexistence with Garmin Connect, permission revocation, Bluetooth toggling, 60-second scan timeout, live updates, bonded-device listing and safe diagnostic sharing. Real GATT discovery is not part of the current passive test.
- Remaining work: validated CIRQA protocol decoder, opt-in data collection, robust history sync, storage wiring and visualization.

## Known limitation
BLE GATT service discovery is not equivalent to a functional Garmin integration. This milestone is an **S25-ready diagnostic and discovery layer**, pending green CI and real phone validation.

## CI evidence (2026-11-15)
- [GitHub Actions run #83](https://github.com/jochenkunkel/pulselibre/actions/runs/43716786021), commit `2722197f`: build and emulator jobs completed successfully; debug APK artifact `pulselibre-debug-apk` was reported present and not expired.
- This verifies the CI checks only, **not** BLE visibility or Garmin CIRQA compatibility on a real S25. The user's earlier phone scan did not positively identify the band; the revised 60-second workflow still awaits hardware feedback.
- The manifest uses `BLUETOOTH_SCAN` with `neverForLocation`, which may filter certain advertisements. No change to this permission flag is authorized solely to increase discovery.

## Permission startup regression (2026-11-15)
- First-launch prompt decision is isolated in `BluetoothPermissionPromptPolicy` with four JVM test cases (missing, already granted, previously prompted, explicit retry unaffected).
- Android OS permission dialog and Samsung-specific behavior still require a real S25 test. Tests are committed, not assumed green until the new head workflow completes.
