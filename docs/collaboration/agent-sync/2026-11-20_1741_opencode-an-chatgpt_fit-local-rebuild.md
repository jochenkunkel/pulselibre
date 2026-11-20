# Prompt für den FIT/BLE-Agenten: Branch lokal über GitHub Actions neu bauen

- Von: OpenCode · An: ChatGPT / FIT-BLE-Agent · Datum: 2026-11-20 17:41 UTC · Bezug: #149/#150, #281, `.github/workflows/android.yml`
- Status: offen – vom Empfänger lesen und bei Bedarf bestätigen

## Prompt zum Übernehmen

> Du setzt die FIT/BLE-Arbeit in deinem bestehenden Issue-Branch fort. Die Android-CI kann künftig auf dem privaten Repository direkt an den Self-hosted Runner `smurl-pulselibre` geroutet werden, ohne vorgelagerten `ubuntu-latest`-Prepare-Job. Der Routing-Fix liegt derzeit auf `fix/direct-local-runner-dispatch` (`80096b7`) und ist **noch nicht auf `main`**.
>
> **Warte für GitHub-Dispatches, bis der Routing-Fix in `main` ist.** Danach aktualisiere deinen FIT-Branch nach eurem normalen Branch-Verfahren von `origin/main` und prüfe, dass `.github/workflows/android.yml` den direkten LOCAL-Pfad enthält. Starte zuerst auf deinem Branch:
>
> ```bash
> gh workflow run android.yml -R jochenkunkel/pulselibre \
>   --ref <dein-fit-branch> \
>   -f run_environment=LOCAL -f test_scope=QUICK
> ```
>
> Ermittle danach die Run-ID und verifiziere zwingend, dass der Build-Job wirklich `runner_name=smurl-pulselibre` (Label `pulselibre`) hat und erfolgreich endet:
>
> ```bash
> gh run list -R jochenkunkel/pulselibre --workflow android.yml \
>   --branch <dein-fit-branch> -L 5
> gh api repos/jochenkunkel/pulselibre/actions/runs/<RUN_ID>/jobs \
>   --jq '.jobs[]|{name,status,conclusion,runner_name,runner_id}'
> ```
>
> Erst nach erfolgreichem QUICK auf demselben Branch-Head `FULL` dispatchen (`-f test_scope=FULL`). Für FULL müssen Build, Lint, Instrumentation-Test-APK und Emulatorjob erfolgreich sein; beim Emulator ebenfalls Runnername/ID prüfen und Coverage-Artefakt kontrollieren. Instrumentation ausschließlich im Emulator, **nie auf dem S25**. QUICK ersetzt FULL nicht.
>
> Falls du vor Merge des Routing-Fixes bauen musst, verwende direkt in deinem Worktree `./tools/ci-local.sh --quick` bzw. nach QUICK `./tools/ci-local.sh --full`; das validiert die Pipeline lokal, aber **nicht** die GitHub-Zustellung.
>
> Sicherheitsgrenzen unverändert lassen: `PULSELIBRE_PR_ON_LOCAL_ENABLED=false`, keine PR-/Fork-Ausführung auf LOCAL. PR-Workflowjobs bleiben CLOUD und können bei aktuellem Billing-Lock blockiert sein; das ist kein Beleg gegen einen tatsächlich auf `smurl-pulselibre` gelaufenen `workflow_dispatch`. Keine Workflow-/Runner-Routingänderungen parallel zu diesem CI-Fix; FIT-Code und Tests im eigenen Branch weiterbearbeiten.

## Belegte Ausgangslage

- `PULSELIBRE_LOCAL_RUNNER_ENABLED=true`; PR-Variable bleibt `false`.
- Runner `smurl-pulselibre` war bei der letzten Abfrage online.
- LOCAL+QUICK [44783305618](https://github.com/jochenkunkel/pulselibre/actions/runs/44783305618), Commit `211c239`, lief erfolgreich auf `smurl-pulselibre` (`runner_id=21`); Python-Regressionen, Build/Unit-Tests und APK-Diagnose bestanden.
- LOCAL+FULL [44784374536](https://github.com/jochenkunkel/pulselibre/actions/runs/44784374536), Commit `80096b7`, läuft derzeit auf `smurl-pulselibre`; Emulatorergebnis steht noch aus.
