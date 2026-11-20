# OpenCode – Runner-Installer Regressionstests

- Agent/Umgebung: OpenCode CLI-Agent
- Beginn: 2026-11-20 13:57 UTC
- Letzte Aktualisierung: 2026-11-20 13:57 UTC
- Status: aktiv
- Rolle/Zuständigkeit: DevOps / GitHub Actions / Runner-Sicherheit und lokale CI
- Aktueller Auftrag: #281 – unabhängige Regressionstests für den Runner-Installer ergänzen; Sicherheitsfreigaben bleiben ausdrücklich unberührt
- Jetzt in Bearbeitung: Dry-Run ohne root (System- und User-Modus) sowie Ablehnung eines echten Installationsversuchs bei öffentlichem Repository automatisch absichern
- Zeitnah geplant: #281 – echte LOCAL-Läufe, Offline-Runner-Test, Signaturkontinuität und Branch-Schutz nur nach den jeweiligen Freigaben; keine exklusive Reservierung
- Betroffene Bereiche/Dateien: `tools/test_runner_installer.py`, ggf. `tools/runner/install-runner.sh`, `docs/ci/HYBRID_CI.md`, Testmatrix und diese Sitzung
- Branch/Worktree: `ci/runner-installer-tests` / `/home/smiler/04_src/pulslibe/hybrid-ci`
- Abstimmungsbedarf: keine bekannte Überschneidung: aktive Sitzungen prüfen, keine offene PR bearbeitet Runner-Tests; Agent-Sync für #281 geprüft. Keine Änderung an Runner-Registrierung oder Repository-Einstellungen.
- Agent-sync-Nachrichten: keine neue Nachricht erforderlich, da keine Überschneidung vorliegt.
- Nachweise: noch keine; Registrierung dieser Sitzung vor Codeänderung
- Nächster Schritt: Tests mit isolierten temporären Stubs implementieren, lokal ausführen, PR mit vollständiger Android-CI öffnen

## Sicherheitsgrenzen

- Repository ist öffentlich; niemals einen Runner registrieren oder `PULSELIBRE_LOCAL_RUNNER_ENABLED` aktivieren.
- Echte Installationsversuche dürfen nur mit Test-Stubs stattfinden; kein Token, Netzwerkzugriff, Systemdienst, User-Service oder Schreibzugriff auf `/opt`.
- Secrets, Keystores und Sichtbarkeit nicht lesen oder ändern.
