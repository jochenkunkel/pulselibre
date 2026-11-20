# OpenCode – Runner-Installer Regressionstests

- Agent/Umgebung: OpenCode CLI-Agent
- Beginn: 2026-11-20 13:57 UTC
- Letzte Aktualisierung: 2026-11-20 14:32 UTC
- Status: aktiv
- Rolle/Zuständigkeit: DevOps / GitHub Actions / Runner-Sicherheit und lokale CI
- Aktueller Auftrag: #281 – unabhängige Regressionstests für den Runner-Installer ergänzen; Sicherheitsfreigaben bleiben ausdrücklich unberührt
- Jetzt in Bearbeitung: #281 – Runner-Installer-Schutz automatisieren und die lokale Keystore-Bereitstellung konkret dokumentieren
- Zeitnah geplant: #281 – echte LOCAL-Läufe, Offline-Runner-Test, Signaturkontinuität und Branch-Schutz nur nach den jeweiligen Freigaben; keine exklusive Reservierung
- Betroffene Bereiche/Dateien: `tools/test_runner_installer.py`, `tools/runner/install-runner.sh`, `.github/workflows/android.yml`, `tools/ci/run-ci.sh`, `docs/ci/HYBRID_CI.md`, `docs/CI_TEST_STRATEGY.md`, diese Sitzung
- Branch/Worktree: `ci/runner-installer-tests` / `/home/smiler/04_src/pulslibe/hybrid-ci`
- Abstimmungsbedarf: keine bekannte Überschneidung: aktive Sitzungen prüfen, keine offene PR bearbeitet Runner-Tests; Agent-Sync für #281 geprüft. Keine Änderung an Runner-Registrierung oder Repository-Einstellungen.
- Agent-sync-Nachrichten: keine neue Nachricht erforderlich, da keine Überschneidung vorliegt.
- Nachweise: 4 isolierte Installer-Regressionstests grün; gesamte Python-Suite 70 Tests grün; gemeinsame Phase `bash tools/ci/run-ci.sh python-tests` mit 70 Tests erfolgreich; `bash -n tools/runner/install-runner.sh` und `git diff --check` sauber; PR #307 gemergt, Full-CI PR `44750380436` und Main `44751861736` grün; Issue #281 aktualisiert. Lokale Signing-Anleitung ergänzt; keine Schlüsselwerte eingesehen.
- Nächster Schritt: Signing-Anleitungsnachtrag mit vollständigem PR-/Main-CI prüfen lassen, danach Sitzung abschließen; Freigaben in #281 bleiben ausstehend.

## Sicherheitsgrenzen

- Repository ist öffentlich; niemals einen Runner registrieren oder `PULSELIBRE_LOCAL_RUNNER_ENABLED` aktivieren.
- Echte Installationsversuche dürfen nur mit Test-Stubs stattfinden; kein Token, Netzwerkzugriff, Systemdienst, User-Service oder Schreibzugriff auf `/opt`.
- Secrets, Keystores und Sichtbarkeit nicht lesen oder ändern.
