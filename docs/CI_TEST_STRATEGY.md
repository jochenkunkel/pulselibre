# CI test strategy: fast development vs full release verification

All existing tests remain in the repository; no test was deleted.

## Development (push to main)

- Run **all JVM unit tests** with `testDebugUnitTest` and Python HCI validator regression tests.
- Compile the Android app with `assembleDebug`. A signed update APK artifact is uploaded only if the four stable signing secrets are available and the run succeeds; no unsigned APK upload is promised.
- Skip Android lint and the emulator instrumentation suite for fast feedback.
- Cancel older in-progress runs on the same branch when newer commits arrive, reducing wasted runner minutes. Cancellation means earlier runs may show `cancelled`, not failed.

## Full verification

Triggered automatically by tags beginning with `v`; also manually through Actions > Android CI > Run workflow with `full_tests=true` (the input currently defaults to **false**). Ordinary pull requests receive only the development checks. This reflects `.github/workflows/android.yml` as inspected on 2026-11-17.

- All JVM unit tests and APK build.
- Android lint and instrumentation APK build.
- Full Android emulator instrumentation tests (API 35).

## Release gate

Before distributing a release, trigger and verify a **successful full workflow** for the exact release commit (e.g. a `v*` tag). A green fast development run alone does **not** mean emulator tests passed. CI success also does not resolve the separate APK signing issue.

## Tradeoff

Expected speedup: skip emulator provisioning/test execution (~3–5 minutes historically), plus lint on each intermediate push. Actual savings should be measured after the first runs. Retain full tests before releases and whenever a regression is suspected.
