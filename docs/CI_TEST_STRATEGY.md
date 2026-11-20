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
- Preserve an active `main` FULL run when newer commits arrive. GitHub keeps at most one pending run per concurrency group and coalesces further pushes to the newest pending commit. Superseded pull-request runs may still be cancelled to avoid wasting minutes. Offline policy tests cover the intended active/pending behavior; Actions runs remain the end-to-end scheduler evidence.

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
| 2 | Lokale FULL-CI (`./tools/ci-local.sh --full`) | **bestanden** (2026-11-20, Exit 0, 411 s, `main` `c61bd65`): 91 Python-Regressionstests, Lint, Instrumentation-Test-APK, 111 Instrumentationstests (0 Fehler) auf AVD `pulselibre-ci-api35` (API 35), Coverage-Datei `coverage.ec` erzeugt und Gate bestanden |
| 3 | GitHub Actions LOCAL + QUICK | **bestanden** – [Run 44783305618](https://github.com/jochenkunkel/pulselibre/actions/runs/44783305618), Commit `211c239`, tatsächlicher Runner `smurl-pulselibre` (`runner_id=21`); Python-Regressionen, Build/Unit-Tests und APK-Diagnose grün; Coverage- und signiertes APK-Artefakt vorhanden |
| 4 | GitHub Actions LOCAL + FULL | **bestanden** – [Run 44784374536](https://github.com/jochenkunkel/pulselibre/actions/runs/44784374536), Commit `80096b7`; Build-Job und Emulatorjob erfolgreich auf `smurl-pulselibre` (`runner_id=21` für beide Jobs). Lint, Instrumentation-APK, API-35-Emulator und Instrumentation-Coverage erfolgreich; `pulselibre-instrumentation-coverage`, `pulselibre-jvm-jacoco` und signiertes APK-Artefakt vorhanden |
| 5 | GitHub Actions CLOUD + QUICK | **bestanden** – Lauf [44698243635](https://github.com/jochenkunkel/pulselibre/actions/runs/44698243635) (Dispatch, `feat/hybrid-ci`), Emulator-Job korrekt übersprungen |
| 6 | GitHub Actions CLOUD + FULL | **bestanden** – PR-Lauf [44763065792](https://github.com/jochenkunkel/pulselibre/actions/runs/44763065792) inkl. Emulator; älterer Nachweis [44696848985](https://github.com/jochenkunkel/pulselibre/actions/runs/44696848985) umfasste Build 5m04s, Emulator 4m08s sowie JVM-/Instrumentation-Coverage-Artefakte |
| 7 | APK-Signaturvergleich | teilweise: Debug-/Release-Zertifikat werden in `diagnostics` verglichen, sobald signiert wird. Abgleich mit einer bereits verteilten APK **offen** – Original-Keystore liegt lokal nicht vor |
| 8 | Testberichte und Artefakte | **bestanden** lokal: `build/ci-reports/`, `app/build/reports/`, `app/build/test-results/`, `app/build/outputs/code_coverage/`; GitHub-Artefakte siehe CLOUD-Läufe |
| 9 | Verhalten bei offline geschaltetem Runner | Entscheidungs-/Fallback-Policy **lokal getestet** (`tools/test_ci_target.py`); echter Offline-Queue-Test bleibt **offen**. Runner `smurl-pulselibre` ist jetzt registriert und online; bei freigegebenem LOCAL gibt es keinen stillen CLOUD-Fallback |
| 10 | Keine unerwarteten Cloud-Jobs | **bestanden** (Logiktests): Forks immer CLOUD, unbekannte Trigger CLOUD, PR ohne Freigabe CLOUD; LOCAL nur bei expliziter Freigabe |
| 11 | Workflow-Struktur (Regressionsschutz) | **bestanden**: `tools/test_workflow_structure.py` prüft direkte LOCAL-Auswahl ohne Hosted-Prepare, main-/Dispatch-Regeln, PR-/Fork-Schutz, JSON-Array + `fromJSON`, statische Job-Namen, SHA-Pins, `persist-credentials: false` und kein `write-all` |
| 12 | Runner-Installer-Trockenlauf | **bestanden** (`--user` und `--system --dry-run` ohne root, jeweils Exit 0): vollständiger Ablauf angezeigt, keine Registrierung, kein Schreibzugriff auf `/opt`; PR [44708553642](https://github.com/jochenkunkel/pulselibre/actions/runs/44708553642) und `main` [44709934316](https://github.com/jochenkunkel/pulselibre/actions/runs/44709934316) grün |
| 13 | Runner-Installer-Sicherheitsregressionen | **bestanden**: 4 isolierte Tests in `tools/test_runner_installer.py`; tokenloser System-/User-Dry-Run, öffentliche Repo-Installation und Dry-Run ohne expliziten Override vor Seiteneffekten abgelehnt; Python-Suite: 70 Tests grün; PR [44750380436](https://github.com/jochenkunkel/pulselibre/actions/runs/44750380436) und Main [44751861736](https://github.com/jochenkunkel/pulselibre/actions/runs/44751861736) CLOUD+FULL inkl. Emulator grün |
| 14 | Main-CI-Concurrency-Policy | **lokal bestanden**: 4 deterministische Policy-Tests plus Workflow-Strukturtest; laufende Push-/Dispatch-Validierung wird nicht abgebrochen; veraltete PR-Läufe dürfen weiter abgebrochen werden; neueste Pending-Revision bleibt erhalten. Reale Scheduler-Abnahme über PR-FULL und Main-FULL erforderlich |
| 15 | Direkte LOCAL-Workflow-Routen | **bestanden**: Struktur-/Policytests plus echte GitHub-Dispatches LOCAL+QUICK [44783305618](https://github.com/jochenkunkel/pulselibre/actions/runs/44783305618) und LOCAL+FULL [44784374536](https://github.com/jochenkunkel/pulselibre/actions/runs/44784374536) auf `smurl-pulselibre`. Beide Modi wurden tatsächlich lokal ausgeführt; der Billing-Block verhindert in diesem Konto weiterhin Hosted-CLOUD-Jobs |

Der FIT-Branch-Dispatch [44786725534](https://github.com/jochenkunkel/pulselibre/actions/runs/44786725534) am 2026-11-20 verwendete noch den Hosted-Prepare-Workflow aus seinem Branch und endete vor Runner-Zuweisung (`runner_id=0`) am Billing-Block. Er ist **kein** LOCAL-Buildnachweis; nach Integration des direkten Routing-Fixes den FIT-Branch aktualisieren und erneut testen.

### Push auf `main`

Der erste Push-Lauf nach dem Merge ([44701191634](https://github.com/jochenkunkel/pulselibre/actions/runs/44701191634)) zeigte `requested_environment=LOCAL`, `environment=CLOUD`, `scope=FULL` – also den dokumentierten Fallback – und wurde durch einen neueren Push eines anderen Agenten abgebrochen (Concurrency). Der Lauf auf dem damaligen Head [6087f1c](https://github.com/jochenkunkel/pulselibre/commit/6087f1c) ([44701721576](https://github.com/jochenkunkel/pulselibre/actions/runs/44701721576)) war mit `CI-Ziel bestimmen`, `Build, Unit-Tests, Lint, APK` und `Emulator-Instrumentationstests (API 35)` **grün**.

Die Nachhärtung aus PR #292 (Workflow-Strukturtests) lief ebenfalls vollständig grün: als PR [44705657911](https://github.com/jochenkunkel/pulselibre/actions/runs/44705657911) und als Push auf `main` [44706755589](https://github.com/jochenkunkel/pulselibre/actions/runs/44706755589), jeweils CLOUD + FULL inklusive Emulator.

Der Runner-Installer-Trockenlauf aus PR #293 lief in der PR [44708553642](https://github.com/jochenkunkel/pulselibre/actions/runs/44708553642) und nach dem Merge auf `main` [44709934316](https://github.com/jochenkunkel/pulselibre/actions/runs/44709934316) ebenfalls CLOUD + FULL **grün**, jeweils einschließlich Emulator.

Nach der Dokumentationsaktualisierung in PR #295 wurde der unmittelbar folgende Main-Lauf [44712911621](https://github.com/jochenkunkel/pulselibre/actions/runs/44712911621) durch einen neueren Main-Push abgebrochen (Concurrency). Der Lauf auf dem danach aktuellen Head `530cda2` ([44714496489](https://github.com/jochenkunkel/pulselibre/actions/runs/44714496489)) war vollständig **grün**, einschließlich Emulator.

Am 2026-11-20 wurden zwei weitere Main-FULL-Läufe durch Folge-Pushes während der Emulatorphase abgebrochen: [44764576965](https://github.com/jochenkunkel/pulselibre/actions/runs/44764576965) (`29868df`) und [44766111559](https://github.com/jochenkunkel/pulselibre/actions/runs/44766111559) (`ec61224`). In beiden Fällen war der Build-/Lint-Job erfolgreich; der Emulatorjob endete `cancelled`. Ursache war die gemeinsame Main-Concurrency-Gruppe mit `cancel-in-progress: true`. Der Lauf auf Head `3681e22` [44766893338](https://github.com/jochenkunkel/pulselibre/actions/runs/44766893338) war anschließend einschließlich Emulator grün. Issue #321 verfolgt die Korrektur.

Die Policy-Regressionstests in `tools/test_workflow_concurrency_policy.py` modellieren reproduzierbar einen aktiven Lauf, einen einzelnen Pending-Slot und die Ersetzung des Pending-Laufs durch die neueste Revision. Ein Strukturtest stellt sicher, dass der Workflow nur bei `pull_request` `cancel-in-progress` aktiviert. Der Offline-Test ist kein Ersatz für GitHub-Scheduler-Evidenz; PR- und Post-Merge-Main-FULL-Läufe bleiben die End-to-End-Abnahme.

Details und Befehle: [ci/HYBRID_CI.md](ci/HYBRID_CI.md). Die Tabelle wird nach
jedem tatsächlich ausgeführten Lauf mit Datum, Lauf-ID und Ergebnis aktualisiert;
nicht ausführbare Tests bleiben ausdrücklich offen.
