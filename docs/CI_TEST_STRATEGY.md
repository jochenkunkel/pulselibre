# CI test strategy: fast development vs full release verification

All existing tests remain in the repository; no test was deleted.

Die hybride CI (LOCAL/CLOUD) ist in [ci/HYBRID_CI.md](ci/HYBRID_CI.md) beschrieben.
Beide Umgebungen führen dieselben Phasen aus (`tools/ci/run-ci.sh`), gesteuert
über den Eingabewert `test_scope`.

## Development (push to main)

- Run **all JVM unit tests** with `testDebugUnitTest` and Python HCI validator regression tests.
- Compile the Android app with `assembleDebug` and the R8 release variant with `assembleRelease`.
- A signed update APK artifact is uploaded only if the four stable signing secrets are available and the run succeeds; no unsigned APK upload is promised.
- Lint and the emulator instrumentation suite run because push-to-main uses `FULL`.
- Cancel older in-progress runs on the same branch and environment when newer commits arrive, reducing wasted runner minutes. Cancellation means earlier runs may show `cancelled`, not failed.

## Full verification

Triggered automatically by pushes and pull requests against `main`; also manually
through Actions > Android CI > Run workflow with **Testumfang = FULL**.

- All JVM unit tests and APK builds (debug + R8 release).
- Android lint and instrumentation APK build.
- Full Android emulator instrumentation tests (API 35).

## Quick verification

Manual runs with **Testumfang = QUICK** skip lint, the instrumentation APK and
the emulator. QUICK is a deliberate subset, not a way to pass a failing test:
every test that exists belongs to one of the two scopes.

## Release gate

Before distributing a release, trigger and verify a **successful full workflow**
for the exact release commit. A green QUICK run alone does **not** mean emulator
tests passed. CI success also does not resolve the separate APK signing question.

## Local runs

`./tools/ci-local.sh --quick` and `--full` execute the identical phases without
GitHub. Reports land in `build/ci-reports/` and `app/build/reports/`.

## Tradeoff

FULL costs an emulator boot (~1–3 minutes) and lint on every push. QUICK is
roughly the build plus unit tests. The emulator remains mandatory before merges
and releases.

## Testmatrix (Stand 2026-11-20)

| # | Test | Ergebnis |
| --- | --- | --- |
| 1 | Lokale QUICK-CI (`./tools/ci-local.sh --quick`) | **bestanden** (2026-11-20, Exit 0): 60 Python-Tests (inkl. 12 Workflow-Strukturtests), 736 JVM-Unit-Tests, Debug-APK, R8-Release-Build, R8-Mapping, APK-Diagnose; nach der Nachhärtung erneut grün |
| 2 | Lokale FULL-CI (`./tools/ci-local.sh --full`) | **bestanden** (2026-11-20, Exit 0, 517 s): zusätzlich Lint, Instrumentation-Test-APK, 103 Instrumentationstests auf AVD `pulselibre-ci-api35` (API 35), Coverage-Datei `coverage.ec` erzeugt und Gate bestanden |
| 3 | GitHub Actions LOCAL + QUICK | **teilweise** – Dispatch `LOCAL`+`QUICK` [44698255871](https://github.com/jochenkunkel/pulselibre/actions/runs/44698255871) lief als dokumentierter CLOUD-Fallback (`reason=… PULSELIBRE_LOCAL_RUNNER_ENABLED != true`). Echte Ausführung auf dem Self-hosted Runner **offen** (Runner nicht registriert) |
| 4 | GitHub Actions LOCAL + FULL | offen – Runner nicht registriert (Repository öffentlich, Sicherheitsregel) |
| 5 | GitHub Actions CLOUD + QUICK | **bestanden** – Lauf [44698243635](https://github.com/jochenkunkel/pulselibre/actions/runs/44698243635) (Dispatch, `feat/hybrid-ci`), Emulator-Job korrekt übersprungen |
| 6 | GitHub Actions CLOUD + FULL | **bestanden** – PR-Lauf [44696848985](https://github.com/jochenkunkel/pulselibre/actions/runs/44696848985): Build 5m04s, Emulator 4m08s, Artefakte `pulselibre-jvm-jacoco` und `pulselibre-instrumentation-coverage` |
| 7 | APK-Signaturvergleich | teilweise: Debug-/Release-Zertifikat werden in `diagnostics` verglichen, sobald signiert wird. Abgleich mit einer bereits verteilten APK **offen** – Original-Keystore liegt lokal nicht vor |
| 8 | Testberichte und Artefakte | **bestanden** lokal: `build/ci-reports/`, `app/build/reports/`, `app/build/test-results/`, `app/build/outputs/code_coverage/`; GitHub-Artefakte siehe CLOUD-Läufe |
| 9 | Verhalten bei offline geschaltetem Runner | Entscheidungslogik **getestet** (`tools/test_ci_target.py`, 14 Fälle): LOCAL ohne Freigabe → CLOUD mit Begründung; LOCAL mit Freigabe → Self-hosted, wartet bei Offline-Runner. Echter Offline-Test **offen** (kein registrierter Runner) |
| 10 | Keine unerwarteten Cloud-Jobs | **bestanden** (Logiktests): Forks immer CLOUD, unbekannte Trigger CLOUD, PR ohne Freigabe CLOUD; LOCAL nur bei expliziter Freigabe |
| 11 | Workflow-Struktur (Regressionsschutz) | **bestanden**: 12 Tests in `tools/test_workflow_structure.py` – `runs-on` nur über ein JSON-Array + `fromJSON`, statische Job-Namen, Actions auf Commit-SHA gepinnt, `persist-credentials: false`, kein `write-all` |

### Push auf `main`

Der erste Push-Lauf nach dem Merge ([44701191634](https://github.com/jochenkunkel/pulselibre/actions/runs/44701191634)) zeigte `requested_environment=LOCAL`, `environment=CLOUD`, `scope=FULL` – also den dokumentierten Fallback – und wurde durch einen neueren Push eines anderen Agenten abgebrochen (Concurrency, erwartetes Verhalten). Der Lauf auf dem aktuellen Head [6087f1c](https://github.com/jochenkunkel/pulselibre/commit/6087f1c) ([44701721576](https://github.com/jochenkunkel/pulselibre/actions/runs/44701721576)) war mit `CI-Ziel bestimmen`, `Build, Unit-Tests, Lint, APK` und `Emulator-Instrumentationstests (API 35)` **grün**.

Details und Befehle: [ci/HYBRID_CI.md](ci/HYBRID_CI.md). Die Tabelle wird nach
jedem tatsächlich ausgeführten Lauf mit Datum, Lauf-ID und Ergebnis aktualisiert;
nicht ausführbare Tests bleiben ausdrücklich offen.
