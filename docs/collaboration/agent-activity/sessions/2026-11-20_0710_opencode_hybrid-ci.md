# OpenCode – Hybride CI (LOCAL/CLOUD)

- Agent/Umgebung: OpenCode (CLI-Agent) auf dem Entwicklungsrechner `smurl` (Ubuntu 26.04, x86_64)
- Beginn: 2026-11-20 07:10 UTC
- Letzte Aktualisierung: 2026-11-20 08:05 UTC
- Status: aktiv
- Rolle/Zuständigkeit: DevOps / GitHub Actions / Android-Build (hybride CI, Runner-Betrieb, lokale Pipeline)
- Aktueller Auftrag: PulsLibre-CI umschaltbar machen zwischen Self-hosted Runner (LOCAL) und GitHub-hosted Runner (CLOUD), gemeinsame Pipeline, lokale CI ohne GitHub, Runner-Installation vorbereiten, dokumentieren
- Jetzt in Bearbeitung: Workflow-Umbau `.github/workflows/android.yml`, gemeinsame Pipeline `tools/ci/run-ci.sh`, lokale CI `tools/ci-local.sh`, Runner-Skripte `tools/runner/`, Dokumentation `docs/ci/HYBRID_CI.md`
- Zeitnah geplant: lokale QUICK-/FULL-Läufe abschließen, CLOUD-Lauf über PR prüfen, Testmatrix in `docs/CI_TEST_STRATEGY.md` mit echten Ergebnissen füllen (Issue #281)
- Betroffene Bereiche/Dateien: `.github/workflows/android.yml`, `.github/scripts/gradle-retry.sh`, `tools/ci/*`, `tools/ci-local.sh`, `tools/runner/*`, `docs/ci/HYBRID_CI.md`, `docs/CI_TEST_STRATEGY.md`, `README.md`, `AGENTS.md` (Testabschnitt), `docs/README.md`, `docs/PROJECT_STATUS.md`, `gradle.properties`, `.gitignore`
- Branch/Worktree: `feat/hybrid-ci` in `/home/smiler/04_src/pulslibe/hybrid-ci`
- Abstimmungsbedarf: keiner – kein anderer Agent bearbeitet derzeit die CI-Infrastruktur. `gradle-retry.sh` wird nur additiv um `GRADLE_CMD` erweitert (Issue #156 / ChatGPT bleibt kompatibel, Regressionstest unverändert grün).
- Agent-sync-Nachrichten: keine neue Nachricht nötig; dieser Eintrag plus PR/Issue #281 genügen. Bei Bedarf Nachricht an alle vor dem Merge.
- Nachweise: lokaler QUICK-Lauf erfolgreich (Exit 0; 47 Python-Tests, 736 JVM-Unit-Tests, Debug-APK, R8-Release, R8-Mapping, APK-Diagnose); lokaler FULL-Lauf erfolgreich (Exit 0, 517 s; zusätzlich Lint, Test-APK, 103 Instrumentationstests auf AVD `pulselibre-ci-api35`, Coverage-`coverage.ec` + Gate); 14 Regressionstests für die Runner-Entscheidungslogik (`tools/test_ci_target.py`); PR #283 mit CLOUD+FULL grün ([44696848985](https://github.com/jochenkunkel/pulselibre/actions/runs/44696848985)); CLOUD+QUICK grün ([44698243635](https://github.com/jochenkunkel/pulselibre/actions/runs/44698243635)); LOCAL+QUICK als dokumentierter CLOUD-Fallback grün ([44698255871](https://github.com/jochenkunkel/pulselibre/actions/runs/44698255871))
- Nächster Schritt: PR #283 nach grünem Lauf auf dem finalen Head mergen, Push-Lauf auf `main` prüfen, Testmatrix und Session aktualisieren

## Sicherheitsgrenzen dieses Auftrags

- Repository-Sichtbarkeit wird **nicht** geändert (öffentlich bleibt öffentlich).
- Kein Self-hosted Runner wird registriert, solange das Repository öffentlich ist.
- Signaturschlüssel und Secrets werden nicht verändert oder neu erzeugt.
- Die bestehende Cloud-CI bleibt funktionsfähig; LOCAL fällt ohne Freigabe sichtbar auf CLOUD zurück.
- Instrumentationstests laufen ausschließlich auf dem eigens erzeugten AVD `pulselibre-ci-api35`, nie auf dem angeschlossenen S25 (`ANDROID_SERIAL` wird explizit gesetzt).
