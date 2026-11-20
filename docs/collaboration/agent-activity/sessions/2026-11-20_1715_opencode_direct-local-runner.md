# OpenCode – Direkte LOCAL-Zustellung ohne Hosted-Prepare

- Agent/Umgebung: OpenCode CLI-Agent
- Beginn: 2026-11-20 17:15 UTC
- Letzte Aktualisierung: 2026-11-20 17:50 UTC
- Status: aktiv
- Rolle/Zuständigkeit: DevOps / GitHub Actions / Self-hosted Runner
- Aktueller Auftrag: #281 – direkten vertrauenswürdigen LOCAL-Pfad ohne GitHub-hosted Compute implementieren
- Jetzt in Bearbeitung: #281 – direkte LOCAL-Zustellung implementiert und QUICK/FULL nachgewiesen; PR-Abschluss und FIT-Branch-Kommunikation
- Zeitnah geplant: FIT-Agent soll seinen PR-Branch nach Routing-Merge aktualisieren und QUICK/FULL erneut prüfen; keine exklusive Reservierung seiner FIT-Dateien
- Betroffene Bereiche/Dateien: `.github/workflows/android.yml`, `tools/test_workflow_structure.py`, `docs/ci/HYBRID_CI.md`, `docs/CI_TEST_STRATEGY.md`, Agenten-Sync
- Branch/Worktree: `fix/direct-local-runner-dispatch` / `/tmp/opencode/pulselibre-direct-local`
- Abstimmungsbedarf: Claude-Code-Vorschlag zur kombinierten Coverage in `android.yml`/`run-ci.sh` gelesen; dieser Auftrag beschränkt sich auf Runner-Routing und berührt den vorgeschlagenen Emulator-Testschritt nicht. OpenCode-#321-Arbeit ist abgeschlossen; keine Änderung daran.
- Agent-sync-Nachrichten: `docs/collaboration/agent-sync/2026-11-20_1715_opencode-an-alle_direct-local-runner.md`; FIT-Agent-Anleitung `docs/collaboration/agent-sync/2026-11-20_1741_opencode-an-chatgpt_fit-local-rebuild.md`; Hinweis zu 44786725534 `docs/collaboration/agent-sync/2026-11-20_1750_opencode-an-chatgpt_fit-dispatch-blocked.md`
- Nachweise:
  - LOCAL+QUICK [44783305618](https://github.com/jochenkunkel/pulselibre/actions/runs/44783305618), Commit `211c239`, tatsächlicher Runner `smurl-pulselibre` (`runner_id=21`) – Python-/Build-/Unit-/Diagnosejobs grün; Coverage- und signiertes APK-Artefakt vorhanden.
  - LOCAL+FULL [44784374536](https://github.com/jochenkunkel/pulselibre/actions/runs/44784374536), Commit `80096b7`; Build und Emulatorjob beide auf `smurl-pulselibre` (`runner_id=21`) erfolgreich, einschließlich Lint, Instrumentation-APK, API-35-Emulator und Coverage-Artefakten.
  - Lokale Regressionen: 90 Python-Tests bestanden. Zwei erste QUICK-Versuche fanden nicht-hermetische Dry-Run-Testannahmen zum bestehenden Runnerzustand; Tests korrigiert, anschließender QUICK-Lauf erfolgreich.
  - Der neue Main-Push-Lauf [44785095335](https://github.com/jochenkunkel/pulselibre/actions/runs/44785095335) blieb auf dem alten Main-Workflow und scheiterte erwartungsgemäß vor Runner-Zuweisung (`runner_id=0`) am GitHub-Billing-Block.
- Nächster Schritt: FIT-Agent direkt auf PR #324 informieren; Routing-PR vorbereiten. PR-/CLOUD-Läufe bleiben wegen Billing blockiert und werden nicht auf LOCAL umgeleitet.
