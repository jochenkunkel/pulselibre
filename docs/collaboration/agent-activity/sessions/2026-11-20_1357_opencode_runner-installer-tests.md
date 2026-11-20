# OpenCode – Runner-Installer Regressionstests

- Agent/Umgebung: OpenCode CLI-Agent
- Beginn: 2026-11-20 13:57 UTC
- Letzte Aktualisierung: 2026-11-20 14:10 UTC
- Status: aktiv
- Rolle/Zuständigkeit: DevOps / GitHub Actions / Runner-Sicherheit und lokale CI
- Aktueller Auftrag: #281 – unabhängige Regressionstests für den Runner-Installer ergänzen; Sicherheitsfreigaben bleiben ausdrücklich unberührt
- Jetzt in Bearbeitung: Dry-Run ohne root/token sowie Ablehnung von Installations- und Dry-Run-Ausführung im öffentlichen Repository ohne expliziten Override automatisch absichern
- Zeitnah geplant: #281 – echte LOCAL-Läufe, Offline-Runner-Test, Signaturkontinuität und Branch-Schutz nur nach den jeweiligen Freigaben; keine exklusive Reservierung
- Betroffene Bereiche/Dateien: `tools/test_runner_installer.py`, `tools/runner/install-runner.sh`, `docs/ci/HYBRID_CI.md`, `docs/CI_TEST_STRATEGY.md`, diese Sitzung
- Branch/Worktree: `ci/runner-installer-tests` / `/home/smiler/04_src/pulslibe/hybrid-ci`
- Abstimmungsbedarf: keine bekannte Überschneidung: aktive Sitzungen prüfen, keine offene PR bearbeitet Runner-Tests; Agent-Sync für #281 geprüft. Keine Änderung an Runner-Registrierung oder Repository-Einstellungen.
- Agent-sync-Nachrichten: keine neue Nachricht erforderlich, da keine Überschneidung vorliegt.
- Nachweise: 4 isolierte Installer-Regressionstests grün; gesamte Python-Suite 70 Tests grün; gemeinsame Phase `bash tools/ci/run-ci.sh python-tests` mit 70 Tests erfolgreich; `bash -n tools/runner/install-runner.sh` und `git diff --check` sauber; PR #307 eröffnet und Issue #281 aktualisiert, Full-CI läuft
- Nächster Schritt: PR #307-FULL-Lauf abwarten, Ergebnis lesen; nur bei grünem PR-/Main-Lauf integrieren

## Sicherheitsgrenzen

- Repository ist öffentlich; niemals einen Runner registrieren oder `PULSELIBRE_LOCAL_RUNNER_ENABLED` aktivieren.
- Echte Installationsversuche dürfen nur mit Test-Stubs stattfinden; kein Token, Netzwerkzugriff, Systemdienst, User-Service oder Schreibzugriff auf `/opt`.
- Secrets, Keystores und Sichtbarkeit nicht lesen oder ändern.
