# PulsLibre CI: QUICK / FULL / RELEASE (Owner 2026-11-25)

## Trigger
- Pull Request nach main: QUICK (Python-Regressionstests, Debug/R8-Build, JVM-Unit-Tests, APK-/Signaturdiagnostik). Kein Emulator.
- Push nach main: QUICK.
- Jeden Tag um 02:17 UTC: FULL auf main, einschließlich Lint, Instrumentation und Android-API-35-Emulator.
- workflow_dispatch: FULL (Vorgabe) oder QUICK explizit wählbar; CLOUD/LOCAL weiterhin wählbar.
- Release: vor Veröffentlichung FULL **auf exakt dem freizugebenden Commit** erfolgreich; signiertes Update-APK nur bei geprüfter Signaturkontinuität. Ein gewöhnlicher QUICK-Push gilt nicht als Releasefreigabe.
- Risikoänderungen (Datenbankschema/Migrationen, Backup/Restore, BLE-Transport-/Protokoll, kritische Importintegrität): FIT muss vor Merge manuell FULL am PR-Head ausführen und dokumentieren.

## Merge und Schutz
- Für gewöhnliche PRs genügt ein erfolgreicher QUICK-Workflow samt weiteren erforderlichen Dependency-Checks und dokumentiertem Self-Review von FIT.
- Rote/fehlende/ausstehende verpflichtende Checks dürfen nicht übergangen werden. Branch-Protection-Required-Checks müssen mit den tatsächlich erzeugten Jobnamen abgeglichen werden. Falls ein bisher required Emulator-Job auf QUICK als SKIPPED erscheint, ist die Schutzregel **regulär** zu aktualisieren; bis dahin kein Bypass.
- Nächtliche FULL-Fehler sind zu triagieren, zu dokumentieren und mit hoher Priorität zu beheben; ein roter FULL-Lauf sperrt Releasefreigaben.
- QUICK ist keine vollständige Instrumentation-/Emulatorvalidierung. Keine falsche FULL-Aussage in Reports.

## Betrieb
- Kein Zwang zu lokalen FULL-/Emulatorläufen. GitHub Actions ist maßgeblich.
- Eine exakte »jeder zehnte Merge«-Zählung wird bewusst **nicht** implementiert: täglicher FULL-Lauf ist robuster und einfacher zu betreiben.
- Geräte-Leases, Backups, APK-Signaturkontinuität und Hardware-Sicherheitsgrenzen bleiben unverändert.
