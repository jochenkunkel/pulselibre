# OpenCode – Hybride CI (LOCAL/CLOUD)

- Agent/Umgebung: OpenCode (CLI-Agent) auf dem Entwicklungsrechner `smurl` (Ubuntu 26.04, x86_64)
- Beginn: 2026-11-20 07:10 UTC
- Letzte Aktualisierung: 2026-11-20 09:48 UTC
- Status: abgeschlossen
- Rolle/Zuständigkeit: DevOps / GitHub Actions / Android-Build (hybride CI, Runner-Betrieb, lokale Pipeline)
- Aktueller Auftrag: PulsLibre-CI umschaltbar machen zwischen Self-hosted Runner (LOCAL) und GitHub-hosted Runner (CLOUD), gemeinsame Pipeline, lokale CI ohne GitHub, Runner-Installation vorbereiten, dokumentieren
- Jetzt in Bearbeitung: abgeschlossen – Runner-Trockenlauf für `--system` ohne root integriert (PR #293)
- Zeitnah geplant: keine exklusive Reservierung; offene Freigaben siehe #281
- Betroffene Bereiche/Dateien: `.github/workflows/android.yml`, `.github/scripts/gradle-retry.sh`, `tools/ci/*`, `tools/ci-local.sh`, `tools/runner/*`, `docs/ci/HYBRID_CI.md`, `docs/CI_TEST_STRATEGY.md`, `README.md`, `AGENTS.md` (Testabschnitt), `docs/README.md`, `docs/PROJECT_STATUS.md`, `gradle.properties`, `.gitignore`
- Branch/Worktree: Integration über PR #283, #292 und #293; separate Follow-up-Sitzung in `2026-11-20_0930_opencode_hybrid-ci-followup.md`
- Abstimmungsbedarf: keiner. `gradle-retry.sh` wurde nur additiv um `GRADLE_CMD` erweitert; der bestehende #156-Regressionstest bleibt unverändert grün. Keine Überschneidung mit ChatGPT (#156) oder Claude Code (#154).
- Agent-sync-Nachrichten: Worktree-Kollision wurde durch PR #293/#294 überholt; siehe [Status](../../agent-sync/2026-11-20_0945_opencode-an-claude_shared-worktree-collision-status.md). Fortschritt und offene Punkte stehen in Issue #281.
- Nachweise:
  - Lokale QUICK-CI grün (Exit 0): 47 Python-Tests, 736 JVM-Unit-Tests, Debug-APK, R8-Release, R8-Mapping, APK-Diagnose
  - Lokale FULL-CI grün (Exit 0, 517 s): zusätzlich Lint, Test-APK, 103 Instrumentationstests auf AVD `pulselibre-ci-api35`, Coverage-`coverage.ec` + Gate
  - 14 Regressionstests für die Runner-Entscheidungslogik (`tools/test_ci_target.py`)
  - 12 Workflow-Strukturtests (`tools/test_workflow_structure.py`): JSON-Array für `runs-on`, statische Job-Namen, SHA-Pins, `persist-credentials: false`, kein `write-all`
  - Lokale QUICK-CI nach der Nachhärtung erneut grün (Exit 0, 60 Python-Tests)
  - PR #292 (Workflow-Strukturtests): CLOUD+FULL grün ([44705657911](https://github.com/jochenkunkel/pulselibre/actions/runs/44705657911)), Squash-Merge `18afad3`; `main`-Push CLOUD+FULL grün ([44706755589](https://github.com/jochenkunkel/pulselibre/actions/runs/44706755589))
  - PR #293 (Runner-System-Dry-Run): CLOUD+FULL grün ([44708553642](https://github.com/jochenkunkel/pulselibre/actions/runs/44708553642)); `main`-Push CLOUD+FULL grün ([44709934316](https://github.com/jochenkunkel/pulselibre/actions/runs/44709934316))
  - PR #295 (Testmatrix-Nachtrag): CLOUD+FULL grün ([44711503539](https://github.com/jochenkunkel/pulselibre/actions/runs/44711503539)); späterer aktueller Main-FULL-Lauf auf Head `530cda2` grün ([44714496489](https://github.com/jochenkunkel/pulselibre/actions/runs/44714496489))
  - PR #283: CLOUD+FULL grün ([44699795823](https://github.com/jochenkunkel/pulselibre/actions/runs/44699795823)), CLOUD+QUICK grün ([44698243635](https://github.com/jochenkunkel/pulselibre/actions/runs/44698243635)), LOCAL+QUICK als CLOUD-Fallback grün ([44698255871](https://github.com/jochenkunkel/pulselibre/actions/runs/44698255871))
  - `main`-Push auf Head 6087f1c: CLOUD-Fallback + FULL grün ([44701721576](https://github.com/jochenkunkel/pulselibre/actions/runs/44701721576)); der erste Push-Lauf wurde durch einen neueren Push abgebrochen (Concurrency)
  - Runner-Installer im `--dry-run` (Benutzermodus) vollständig durchlaufen, ohne Registrierung
- Nächster Schritt: keine Aktion von OpenCode. Offene Freigaben in #281: Repository privat schalten, Runner registrieren, `PULSELIBRE_LOCAL_RUNNER_ENABLED=true`, echte LOCAL-Läufe, Signaturabgleich, Branch-Schutz.

## Sicherheitsgrenzen dieses Auftrags

- Repository-Sichtbarkeit wurde **nicht** geändert (öffentlich bleibt öffentlich).
- Kein Self-hosted Runner wurde registriert; LOCAL läuft bis zur Freigabe als dokumentierter CLOUD-Fallback.
- Signaturschlüssel und Secrets wurden nicht verändert oder neu erzeugt.
- Instrumentationstests liefen ausschließlich auf dem eigens erzeugten AVD `pulselibre-ci-api35`, nie auf dem angeschlossenen S25 (`ANDROID_SERIAL` explizit gesetzt).
