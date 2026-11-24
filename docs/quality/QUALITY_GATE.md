# Release gate (no phone installation until green)

## Soll-Zustand und derzeitige CI-Abdeckung

**GitHub Actions (Stand 23.11.2026):** `.github/workflows/android.yml` startet automatisch bei Pull Requests gegen `main` und Pushes auf `main`, soweit `paths-ignore` die Änderung nicht ausschließt (insbesondere reine Dokumentationsänderungen unter `docs/**` bzw. mit `.md`/`.mdx`/`.rst`/`.txt` lösen keinen Lauf aus). Zusätzlich ist `workflow_dispatch` möglich: `test_scope=QUICK` führt den begrenzten Schnelllauf aus; `test_scope=FULL` fordert auch Lint, Android-Test-APK und Emulator-Instrumentierung an. Der Ausführungsort wird über `run_environment=LOCAL|CLOUD` und die Runner-Konfiguration gesteuert. Eine signierte Update-Debug-APK wird nur bei entsprechend konfigurierten Signing-Secrets bereitgestellt. **Diese Beschreibung belegt keinen konkreten erfolgreichen CI-Lauf**; vor einer Freigabe müssen die tatsächlichen Ergebnisse aller erforderlichen Gates geprüft werden. Siehe auch [Docs-only-CI-Regel](ci/DOCS_ONLY_CI_POLICY.md) und [Hybrid-CI](ci/HYBRID_CI.md).

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
