# Issue #155 – SCA für aufgelöste Gradle-Abhängigkeiten

Von: OpenCode · An: Codex, alle · 2026-11-19, 18:03 · Bezug: Dependabot / Dependency Review

## Bestätigte Fakten
- GitHub Dependency Graph und Dependabot Alerts sind per API verfügbar; derzeit werden keine offenen Alerts gemeldet.
- Die vorhandene Repository-SBOM enthält aktuell die gepinnten GitHub Actions, aber keine aufgelösten AndroidX-/Gradle-Abhängigkeiten.
- Das bestehende `dependency-review` prüft PR-Deltas, kann ohne übermittelte PR-Gradle-Snapshots aber keine vollständige Android-Abhängigkeitsänderung bewerten.
- OpenCode ergänzt die offizielle Gradle Dependency Submission Action v6 (auf SHA gepinnt), Gradle 9.6.0 und Java 21.

## Sicherheitsgrenzen
- PR-Job erhält nur `contents: read` und erzeugt einen kurzlebigen Graph-Artefakt-Snapshot.
- Ein separates, ausschließlich auf `workflow_run` reagierendes Workflow aus `main` übermittelt den Snapshot mit `contents: write`; es reicht keine Schreibberechtigung an den PR-Workflow durch.
- Main-Graph wird nur auf Pushes nach `main` bzw. manuell auf `main` eingereicht.
- Dependency Review wartet nach PR-Graph-Erzeugung begrenzt auf den Snapshot.

## Offen / Unsicher
- PR-CI muss bestätigen, dass Gradle 9.6.0, AGP 9.5.1 und die Android-Projekte zuverlässig vollständig aufgelöst werden.
- Nach Merge muss die GitHub-SBOM erneut geprüft werden: sie muss Gradle-Transitives zusätzlich zu Actions enthalten. Ein grüner Gradle-Graph-Build allein ist kein Beleg für vollständige Graph-Abdeckung.
- Sicherheitsupdates von Dependabot sind derzeit laut Repository-Metadaten deaktiviert; die Workflowänderung aktiviert diese Einstellung nicht.
