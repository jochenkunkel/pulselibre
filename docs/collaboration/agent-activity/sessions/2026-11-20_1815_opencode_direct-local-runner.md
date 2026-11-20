# OpenCode – Direkte LOCAL-Zustellung ohne Hosted-Prepare

- Agent/Umgebung: OpenCode CLI-Agent
- Beginn: 2026-11-20 18:15 UTC
- Letzte Aktualisierung: 2026-11-20 18:15 UTC
- Status: aktiv
- Rolle/Zuständigkeit: DevOps / GitHub Actions / Self-hosted Runner
- Aktueller Auftrag: #281 – direkten vertrauenswürdigen LOCAL-Pfad ohne GitHub-hosted Compute implementieren
- Jetzt in Bearbeitung: #281 – `android.yml` Routing, Regressionstests und Betriebsdokumentation
- Zeitnah geplant: echte LOCAL+QUICK- und LOCAL+FULL-Dispatches nach Merge; Zustellung ist bis dahin unbestätigt
- Betroffene Bereiche/Dateien: `.github/workflows/android.yml`, `tools/test_workflow_structure.py`, `docs/ci/HYBRID_CI.md`, Agenten-Sync
- Branch/Worktree: `fix/direct-local-runner-dispatch` / `/tmp/opencode/pulselibre-direct-local`
- Abstimmungsbedarf: Claude-Code-Vorschlag zur kombinierten Coverage in `android.yml`/`run-ci.sh` gelesen; dieser Auftrag beschränkt sich auf Runner-Routing und berührt den vorgeschlagenen Emulator-Testschritt nicht. OpenCode-#321-Arbeit ist abgeschlossen; keine Änderung daran.
- Agent-sync-Nachrichten: `docs/collaboration/agent-sync/2026-11-20_1815_opencode-an-alle_direct-local-runner.md`
- Nachweise: noch keine für die Änderung
- Nächster Schritt: Workflow-Policytests ergänzen, lokale Python-/QUICK-Checks ausführen, Änderungen reviewen und PR zur Full-CI-Abnahme vorbereiten.
