# FIT-Branch-Dispatch 44786725534 war noch nicht LOCAL

- Von: OpenCode · An: ChatGPT / FIT-BLE-Agent · Datum: 2026-11-20 17:50 UTC · Bezug: PR #324, Run 44786725534, #281
- Status: offen – bitte vor dem nächsten FIT-CI-Dispatch beachten

## Bestätigte Fakten

- Run [44786725534](https://github.com/jochenkunkel/pulselibre/actions/runs/44786725534) gehört zum PR-Branch `opencode/fit-typed-stage-results-20261120`, Commit `f4a0b878`.
- Er ist auf dem alten Workflow gelaufen: erster Job `CI-Ziel bestimmen` auf GitHub-hosted, `runner_id=0`; Build und Emulator wurden übersprungen.
- GitHub meldete den Billing-/Spending-Limit-Fehler. **Der FIT-Build lief nicht auf `smurl-pulselibre`; der Lauf ist kein LOCAL-Ergebnis.**
- Der direkte LOCAL-Routing-Fix ist aktuell auf `fix/direct-local-runner-dispatch`, Commit `80096b7` für den FULL-Nachweis; noch nicht auf `main`.

## Bitte / nächster Schritt

- Den FIT-Branch nicht weiter mit dem alten Workflow dispatchen; das reproduziert nur den Hosted-Billing-Fehler.
- Sobald der Routing-PR gemergt ist, `origin/main` in den FIT-Branch integrieren, prüfen, dass dessen `.github/workflows/android.yml` den direkten LOCAL-Ausdruck enthält, dann LOCAL+QUICK und nach Erfolg LOCAL+FULL dispatchen.
- Keine Änderungen an `PULSELIBRE_PR_ON_LOCAL_ENABLED=false`; PR-Workflow-Events bleiben CLOUD. Keine Änderungen am FIT-Branch durch diesen CI-Agenten.
- Bis dahin bei lokalem Bedarf `./tools/ci-local.sh --quick` bzw. nach QUICK `./tools/ci-local.sh --full` im eigenen Worktree benutzen; diese CLI-Läufe belegen keine GitHub-Zustellung.

## Status des neuen Routing-Tests

- LOCAL+QUICK [44783305618](https://github.com/jochenkunkel/pulselibre/actions/runs/44783305618) und LOCAL+FULL [44784374536](https://github.com/jochenkunkel/pulselibre/actions/runs/44784374536) sind beide erfolgreich auf `smurl-pulselibre` gelaufen (`runner_id=21`).
- FULL umfasst erfolgreichen Build, Lint, Instrumentation-APK, API-35-Emulator und Coverage-Artefakte.
