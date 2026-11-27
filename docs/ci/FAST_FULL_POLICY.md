# PulsLibre CI: QUICK / FULL / RELEASE (Owner 2026-11-25)

> **Rollenpräzisierung 26.11.2026:** Alle in dieser Richtlinie genannten FIT-Self-Reviews, FIT-Merges und FIT-Testentscheidungen werden **ab jetzt durch Claude Code / GUI-Agent als alleinigen ausführenden Agenten** wahrgenommen. Die QUICK/FULL/RELEASE-Gates, GitHub-Branch-Protection und Dependency-Checks ändern sich dadurch **nicht**. Kein weiterer Agentenreview nötig, soweit Repository-Regeln keinen Review technisch verlangen. Keine lokale FULL-Emulation als Pflicht.

## Trigger
- Pull Request nach main: QUICK (Python-Regressionstests, Debug/R8-Build, JVM-Unit-Tests, APK-/Signaturdiagnostik). Kein Emulator.
- Push nach main: QUICK.
- Jeden Tag um 02:17 UTC: FULL auf main, einschließlich Lint, Instrumentation und Android-API-35-Emulator.
- workflow_dispatch: FULL (Vorgabe) oder QUICK explizit wählbar; CLOUD/LOCAL weiterhin wählbar.
- Release: vor Veröffentlichung FULL **auf exakt dem freizugebenden Commit** erfolgreich; signiertes Update-APK nur bei geprüfter Signaturkontinuität. Ein gewöhnlicher QUICK-Push gilt nicht als Releasefreigabe.
- **Kein FULL-Merge-Gate** für irgendeinen Änderungstyp. FIT wählt bei Risiken gezielte Tests oder optional FULL. FULL bleibt vor Release verpflichtend.

## Merge und Schutz
- Für gewöhnliche PRs genügt ein erfolgreicher QUICK-Workflow samt weiteren erforderlichen Dependency-Checks und dokumentiertem Self-Review von FIT.
- Rote/fehlende/ausstehende verpflichtende Checks dürfen nicht übergangen werden. Branch-Protection-Required-Checks müssen mit den tatsächlich erzeugten Jobnamen abgeglichen werden. Falls ein bisher required Emulator-Job auf QUICK als SKIPPED erscheint, ist die Schutzregel **regulär** zu aktualisieren; bis dahin kein Bypass.
- Nächtliche FULL-Fehler sind zu triagieren, zu dokumentieren und mit hoher Priorität zu beheben; ein roter FULL-Lauf sperrt Releasefreigaben.
- QUICK ist keine vollständige Instrumentation-/Emulatorvalidierung. Keine falsche FULL-Aussage in Reports.

## Betrieb
- Kein Zwang zu lokalen FULL-/Emulatorläufen. GitHub Actions ist maßgeblich.
- Eine exakte »jeder zehnte Merge«-Zählung wird bewusst **nicht** implementiert: täglicher FULL-Lauf ist robuster und einfacher zu betreiben.
- Geräte-Leases, Backups, APK-Signaturkontinuität und Hardware-Sicherheitsgrenzen bleiben unverändert.

## Schlanker FIT-Ablauf
1. Kleine Änderung + gezielte lokale Unit-/Replay-Prüfung, bei reinen Docs optional ohne lokale Tests.
2. PR öffnen; QUICK und relevante Dependency-Checks automatisch prüfen.
3. FIT führt Self-Review durch und merged nach grünen erforderlichen Checks eigenständig, ohne zweiten Reviewer oder Owner-Nachfrage.
4. Nächtlicher FULL läuft asynchron; Fehler werden am nächsten Arbeitsschritt priorisiert. Kein automatischer Rollback ohne Evidenz.
5. Vor Release FULL gezielt anfordern; sonst optional. Keine lokalen Emulator-/FULL-Pflichten.

Diese Regeln ersetzen alle widersprechenden pauschalen FULL-/Zweitreview-Gates in älteren Arbeitsanweisungen; historische Testberichte werden nicht rückwirkend umgeschrieben.
