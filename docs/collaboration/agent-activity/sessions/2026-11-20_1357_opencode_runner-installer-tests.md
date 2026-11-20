# OpenCode – Runner-Installer Regressionstests

- Agent/Umgebung: OpenCode CLI-Agent
- Beginn: 2026-11-20 13:57 UTC
- Letzte Aktualisierung: 2026-11-20 15:48 UTC
- Status: abgeschlossen
- Rolle/Zuständigkeit: DevOps / GitHub Actions / Runner-Sicherheit und lokale CI
- Aktueller Auftrag: #281 – Runner-Installer-Regressionen und lokale Signing-Diagnose absichern
- Jetzt in Bearbeitung: Sitzungsabschluss nach Merge von PR #316 und erfolgreicher Prüfung des aktuellen Main-FULL-Laufs
- Zeitnah geplant: Die verbleibenden freigegebenen #281-Punkte in einer separaten Arbeitseinheit umsetzen; echter Self-hosted-Betrieb bleibt bis zur erforderlichen Repository-/Runner-Konfiguration aus
- Betroffene Bereiche/Dateien: `tools/test_runner_installer.py`, `tools/runner/install-runner.sh`, `.github/workflows/android.yml`, `tools/ci/run-ci.sh`, `tools/test_signing_diagnostics.py`, `docs/ci/HYBRID_CI.md`, `docs/CI_TEST_STRATEGY.md`, diese Sitzung
- Branch/Worktree: `fix/local-signing-diagnostics` (gemergt in `main`) / `/home/smiler/04_src/pulslibe/hybrid-ci`
- Abstimmungsbedarf: keine bekannte Überschneidung: aktive Sitzungen prüfen, keine offene PR bearbeitet Runner-Tests; Agent-Sync für #281 geprüft. Keine Änderung an Runner-Registrierung oder Repository-Einstellungen.
- Agent-sync-Nachrichten: keine neue Nachricht erforderlich, da keine Überschneidung vorliegt.
- Nachweise: 4 isolierte Installer-Regressionstests grün; Python-Suite nach Signing-Regressionstest 71 Tests grün; QUICK-CI Exit 0 mit `APK_SIGNING_READY=true`; Debug-/Release-APK gültig signiert und Zertifikate stimmen überein; PR #316 in `main` gemergt. PR-CI (inkl. Emulator) grün: Run `44763065792`; aktueller Main-FULL-Lauf `44766893338` ebenfalls vollständig grün. Kein Schlüssel- oder Passwortwert wurde ausgegeben; APK nicht installiert oder verteilt.
- Nächster Schritt: Diese Sitzung abschließen. Die verbleibenden #281-Maßnahmen sind eine separate Arbeitseinheit; die Nutzerfreigaben liegen nun vor.

## Sicherheitsgrenzen

- Repository ist öffentlich; bis zur sicheren Runner-Konfiguration keine LOCAL-Runner-Jobs ausführen und keine Fork-PR auf Self-hosted ausführen.
- Echte Installationsversuche dürfen nur mit Test-Stubs stattfinden; kein Token, Netzwerkzugriff, Systemdienst, User-Service oder Schreibzugriff auf `/opt`.
- Secret-Werte niemals ausgeben oder protokollieren; Schlüsselmaterial nicht rotieren oder ersetzen. Sichtbarkeits- und Runner-Änderungen nur innerhalb des ausdrücklich freigegebenen #281-Umfangs.
