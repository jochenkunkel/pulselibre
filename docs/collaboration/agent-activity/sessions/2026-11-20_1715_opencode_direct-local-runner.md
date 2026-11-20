# OpenCode – Direkte LOCAL-Zustellung ohne Hosted-Prepare

- Agent/Umgebung: OpenCode CLI-Agent
- Beginn: 2026-11-20 17:15 UTC
- Letzte Aktualisierung: 2026-11-20 17:33 UTC
- Status: aktiv
- Rolle/Zuständigkeit: DevOps / GitHub Actions / Self-hosted Runner
- Aktueller Auftrag: #281 – direkten vertrauenswürdigen LOCAL-Pfad ohne GitHub-hosted Compute implementieren
- Jetzt in Bearbeitung: #281 – `android.yml` Routing, Regressionstests und Betriebsdokumentation
- Zeitnah geplant: echte LOCAL+QUICK- und LOCAL+FULL-Dispatches nach Merge; Zustellung ist bis dahin unbestätigt
- Betroffene Bereiche/Dateien: `.github/workflows/android.yml`, `tools/test_workflow_structure.py`, `docs/ci/HYBRID_CI.md`, Agenten-Sync
- Branch/Worktree: `fix/direct-local-runner-dispatch` / `/tmp/opencode/pulselibre-direct-local`
- Abstimmungsbedarf: Claude-Code-Vorschlag zur kombinierten Coverage in `android.yml`/`run-ci.sh` gelesen; dieser Auftrag beschränkt sich auf Runner-Routing und berührt den vorgeschlagenen Emulator-Testschritt nicht. OpenCode-#321-Arbeit ist abgeschlossen; keine Änderung daran.
- Agent-sync-Nachrichten: `docs/collaboration/agent-sync/2026-11-20_1715_opencode-an-alle_direct-local-runner.md`
- Nachweise: LOCAL+QUICK [44783305618](https://github.com/jochenkunkel/pulselibre/actions/runs/44783305618), Commit `211c239`, tatsächlicher Runner `smurl-pulselibre` (`runner_id=21`) – Python-/Build-/Unit-/Diagnosejobs grün; zwei Artefakte vorhanden. Frühere Versuche 44782461165 und 44783019997 deckten umgebungsabhängige Installer-Dry-Run-Testannahmen auf; diese sind korrigiert.
- Nächster Schritt: LOCAL+FULL auf dem neuesten Branch-Head ausführen; danach PR-Abnahme und Sicherheits-/Billing-Grenzen dokumentieren.
