# OpenCode – Main-CI-Concurrency und FULL-Laufstabilität

- Agent/Umgebung: OpenCode CLI-Agent
- Beginn: 2026-11-20 16:00 UTC
- Letzte Aktualisierung: 2026-11-20 16:32 UTC
- Status: abgeschlossen
- Rolle/Zuständigkeit: DevOps / GitHub Actions / Runner- und Android-CI-Stabilität
- Aktueller Auftrag: Issue #321 – laufende FULL-Validierungen auf `main` gegen Abbruch durch schnelle Folge-Pushes absichern
- Jetzt in Bearbeitung: abgeschlossen – Issue #321; aktive Main-FULL-Läufe bleiben aktiv, neuester Pending-Lauf wird nachgezogen, überholter Pending-Lauf ersetzt
- Zeitnah geplant: #281 – Betriebsarten/Runner-Bereitschaft und tatsächliche Testmatrix prüfen; echte Self-hosted-Ausführung erst nach Sicherheits-/Systemprüfung
- Betroffene Bereiche/Dateien: `.github/workflows/android.yml`, `tools/test_workflow_structure.py`, neuer Offline-Concurrency-Policy-Test, `docs/ci/HYBRID_CI.md`, `docs/CI_TEST_STRATEGY.md`, Issue #321, diese Sitzung und Agent-Sync
- Branch/Worktree: `ci/main-full-concurrency-protection` / `/home/smiler/04_src/pulslibe/hybrid-ci`
- Abstimmungsbedarf: Claude-Codes offene Agent-Sync-Meldung zu Main-CI-Abbrüchen wurde gelesen; Übernahme von #321 angekündigt. PR #319 ändert nur die vorherige Sitzung, keine Überschneidung der Workflow-Dateien festgestellt.
- Agent-sync-Nachrichten: [Übernahme/Concurrency-Untersuchung](../../agent-sync/2026-11-20_1600_opencode-an-alle_main-ci-concurrency.md)
- Nachweise: Runs `44764576965` (`29868df`) und `44766111559` (`ec61224`) hatten erfolgreiche Build-/Lint-Jobs, deren Emulator-Jobs nach jeweiligem Folge-Push als `cancelled` endeten; Main-FULL `44766893338` auf `3681e22` einschließlich Emulator vollständig erfolgreich. Repository-Sichtbarkeit read-only als public bestätigt; 0 Runner; beide LOCAL-Schalter false. Main `aac5a86` Run `44768314086` bei letzter Prüfung noch in progress. Local FULL-Preflight bestätigt JDK 21, Gradle 9.6.0, Android SDK/build-tools 35.0.0 und beschreibbares KVM. Gesamte Python-Suite: 77 Tests grün; Concurrency-Policy- und Workflow-Strukturtests: 16 grün.
- Ergebnis: PR #323 gemergt; PR-FULL [44770360612](https://github.com/jochenkunkel/pulselibre/actions/runs/44770360612) und Post-Merge-Main-FULL [44771545401](https://github.com/jochenkunkel/pulselibre/actions/runs/44771545401), [44771640120](https://github.com/jochenkunkel/pulselibre/actions/runs/44771640120) jeweils einschließlich Emulator grün. E2E-Queue-Test: `44771545401` blieb trotz Folge-Push aktiv und beendete Emulator erfolgreich; älterer Pending-Run `44771568005` wurde ersetzt; neuester Run `44771640120` startete danach und wurde grün. Issue #321 abgeschlossen.

## Sicherheits- und Testgrenzen

- Keine Repository-Sichtbarkeit, Secrets, Runner-Registrierung, Systemdienste oder Berechtigungen ändern.
- Keine PR-/Fork-Inhalte auf einem Self-hosted Runner ausführen.
- LOCAL-QUICK/FULL werden nicht als wirklich getestet ausgeben, solange kein Runner registriert ist.
- QUICK ersetzt keinen erforderlichen FULL-Lauf; Integration nur nach grünem PR-FULL und anschließendem Main-FULL samt Emulator.
