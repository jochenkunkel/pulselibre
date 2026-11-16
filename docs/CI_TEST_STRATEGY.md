# CI test strategy: fast development vs full release verification

All existing tests remain in the repository; no test was deleted.

## Development (push to main)

- Run **all JVM unit tests** with `testDebugUnitTest`.
- Compile the Android app and upload the debug APK with `assembleDebug`.
- Skip Android lint and the emulator instrumentation suite for fast feedback.
- Cancel older in-progress runs on the same branch when newer commits arrive, reducing wasted runner minutes. Cancellation means earlier runs may show `cancelled`, not failed.

## Full verification

Triggered automatically by pull requests and tags beginning with `v`; also manually through Actions > Android CI > Run workflow (enable `full_tests`, which defaults to true).

- All JVM unit tests and APK build.
- Android lint and instrumentation APK build.
- Full Android emulator instrumentation tests (API 35).

## Release gate

Before distributing a release, trigger and verify a **successful full workflow** for the exact release commit (e.g. a `v*` tag). A green fast development run alone does **not** mean emulator tests passed. CI success also does not resolve the separate APK signing issue.

## Tradeoff

Expected speedup: skip emulator provisioning/test execution (~3–5 minutes historically), plus lint on each intermediate push. Actual savings should be measured after the first runs. Retain full tests before releases and whenever a regression is suspected.
