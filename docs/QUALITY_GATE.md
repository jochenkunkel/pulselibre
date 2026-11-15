# Release gate (no phone installation until green)

## Mandatory CI
- Compile debug app and androidTest APK
- JVM tests and lint
- Android emulator instrumentation tests (launch + no INTERNET permission)
- Upload APK only after successful checks
- Manual inspection of CI results and APK metadata before recommending installation

## Hardware-independent fault scenarios to add
- Permission grant/deny/revoke; Bluetooth disabled mid-scan
- Scan callback error and empty results
- Duplicate advertisement, missing device name, varying RSSI
- Connection failure, disconnect, GATT discovery error
- Android lifecycle and background transitions
- Notification bytes malformed/empty/large, device protocol framing
- Data minimization, local persistence failures, schema migrations
- No unintended pairing/unpairing and no Garmin Connect data loss

## Limitations
An emulator does not emulate a Garmin CIRQA radio or proprietary protocol. Passing CI does not prove CIRQA compatibility. Do not describe the prototype as release-ready until actual CI results have been inspected and required simulated failure tests are implemented.
