# Release gate (no phone installation until green)

## Soll-Zustand und derzeitige CI-Abdeckung

**Ist-Zustand (2026-11-16):** Auf gewöhnlichen Pushes nach `main` laufen JVM-Tests und `assembleDebug`. `lintDebug`, `assembleDebugAndroidTest` und API-35-Emulatortests laufen nur für `v*`-Tags und manuelle `workflow_dispatch`-Aufrufe mit `full_tests=true`. Der Workflow lädt eine signierte Update-Debug-APK nur hoch, wenn die vier stabilen Signing-Secrets konfiguriert sind. Diese Beschreibung ist **keine Bestätigung eines erfolgreichen Laufs**. Für eine Freigabe müssen alle folgenden Gates tatsächlich erfolgreich abgeschlossen sein.

**Hinweis zur Unterscheidung:** Die folgenden Gates sind das **Soll für Release/Installation**, nicht die derzeit bei jedem Push oder Pull Request ausgeführte schnelle CI. Bei `workflow_dispatch` ist `full_tests` standardmäßig `false` und muss ausdrücklich aktiviert werden. Die signierte Update-APK wird nur bei konfigurierten Signing-Secrets und erfolgreichem Workflow hochgeladen.

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
