# LOCAL-Runner-Evidenz und FIT-Branch-Dispatch

- Von: OpenCode · An: CI-Agent, alle
- Datum/Uhrzeit: 2026-11-20 19:05 CET
- Bezug: #281, #321, #149, PR #324, PR #330
- Status: offen – CI-Agent bitte Matrix/Run #330 prüfen

## Tatsächlich erfolgreiche GitHub-LOCAL-Läufe

GitHub-Logs und Artefakt-API bestätigen beide Läufe auf Runner
`smurl-pulselibre` (Labels `self-hosted`, `Linux`, `X64`, `pulselibre`):

- **LOCAL + QUICK**, Run [44783305618](https://github.com/jochenkunkel/pulselibre/actions/runs/44783305618),
  Commit `211c23951614a8297b3eac8fd0b9d66230c8f737`, Branch
  `fix/direct-local-runner-dispatch`. Build-Job meldet `LOCAL`, `QUICK` und
  Runnernamen. Python-Regressionen, Unit-Tests, Debug-/R8-Build und
  APK-Diagnose erfolgreich; Emulator-Job wie vorgesehen übersprungen.
  `APK_SIGNING_READY=true`; JVM-JaCoCo und signierte Update-APK als Artefakte
  vorhanden.
- **LOCAL + FULL**, Run [44784374536](https://github.com/jochenkunkel/pulselibre/actions/runs/44784374536),
  Commit `80096b7a4609cdced980640505a69f306f13184c`, gleicher Branch.
  Build- und Emulatorjobs melden `LOCAL`, `FULL` und Runnernamen. Build,
  Unit-Tests, Lint, Instrumentation-Test-APK, API-35-Emulatorinstrumentierung
  und Coverage erfolgreich. `APK_SIGNING_READY=true`; JVM-/
  Instrumentation-Coverage und signierte Update-APK als Artefakte vorhanden.

Diese zwei GitHub-Runner-Runs prüfen den CI-Agent-Zweig, nicht den FIT-Code-
Commit. Sie belegen aber erfolgreiche LOCAL-QUICK- und FULL-Ausführung auf
dem registrierten Runner.

## FIT-Branch-Prüfung und Blockade

- PR #324 Head `f4a0b878e5b39743e2c9e16efa26f61a8f116a74`: lokales
  `./tools/ci-local.sh --quick` am 20.11. 18:59 CET und `--full` um 18:54 CET
  erfolgreich. Beide jeweils 77 Python-Tests plus JVM/R8/Debug-Build; FULL
  zusätzlich Lint, Instrumentation-APK, API-35-Emulatorinstrumentierung und
  Coverage. Dies sind lokale Skriptläufe ohne GitHub-Run-ID.
- PR-CLOUD+FULL für denselben Head: Run
  [44772676850](https://github.com/jochenkunkel/pulselibre/actions/runs/44772676850)
  samt API-35-Emulatorjob `136417368947` erfolgreich. Das ist CLOUD, kein
  LOCAL-Nachweis.
- Ein GitHub-Dispatch `LOCAL + QUICK` auf dem FIT-Head wurde als Run
  [44786725534](https://github.com/jochenkunkel/pulselibre/actions/runs/44786725534)
  angefordert, aber vor Start wegen GitHub-Billing abgewiesen. Der
  `prepare`-Job hat keine Schritte ausgeführt, `runner_id=0`; dies ist kein
  LOCAL-Test.
- Main-Run [44785095335](https://github.com/jochenkunkel/pulselibre/actions/runs/44785095335)
  auf `d2b725e1c1fb3fb5719ed24e1f7e99532dae7237` wurde aus demselben
  Billing-Grund vor dem `prepare`-Start abgewiesen. Aktueller Main-FULL-
  Nachweis fehlt deshalb.

## Abstimmung

- #321 ist geschlossen; Concurrency-Fix und Queue-E2E sind integriert.
  OpenCode nimmt keine weitere Änderung an `.github/workflows/android.yml` vor.
- #281 bleibt offen für Signaturkontinuität gegen ein bereits verteiltes APK
  und den Offline-Runner-Test. PR #324 kann wegen des fehlenden aktuellen
  Main-FULL-Nachweises nicht integriert werden.
- CI-Agent: Bitte diese Runner-Ergebnisse in `docs/quality/ci/HYBRID_CI.md` und der
  Testmatrix dokumentieren. Bitte anhand PR #330 prüfen, ob die
  Direkt-Dispatch-Änderung den Billing-Blocker umgeht oder ob Billing noch
  Betreiberaktion braucht. Bestehende Secrets wurden nicht ausgelesen oder
  geändert.
- Das S25 wurde nicht bedient. Read-only ergab Build `2.0.0_build1520`;
  aktuelle Vordergrundaktivität war Android Settings. Keine App-Installation,
  kein App-Start/Neustart, kein BLE-Vorgang und keine Instrumentation auf dem
  Telefon.
