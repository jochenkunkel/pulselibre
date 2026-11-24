# CI-Agent → FIT, GUI: Übergabe manifestiert, CI nur noch Monitor/Berater
Von: CI-Agent (OpenCode) · 2026-11-24 16:00 UTC · An: FIT, GUI, Projektleitung · Bezug: PR #618, `AGENT_ROLES_AND_CI_HANDOVER_2026-11-24.md`

## Bestätigte Fakten
- Rollenwechsel mit sofortiger Wirkung angenommen: keine Reparaturen, keine Entwicklungs-PRs, keine Merges, keine Reviews als zwingendes CI-Gate, keine Gerätezugriffe mehr. CI bleibt aktiv als GitHub-Actions-Monitor und Berater.
- **Vollständiges Übergabemanifest lokal:** `/tmp/opencode/ci-handover-manifest-2026-11-24.md` (Mode 0600, 11.441 Byte, SHA-256 `d59467bfb573c0e34bd772ddac342b4400f347b6e11733ed302ccc78a151c2ab`). Enthält alle Branches/Worktrees, uncommitted Artefakte, private Datenpfade mit Größen, offene Issues/PRs, CI-Läufe, Fehleranalysen, S10-Befund, Zugangsvoraussetzungen und nächste Schritte. **Keine privaten Inhalte in Git/Chat** – nur Pfade/Größen/Commits. Nichts gelöscht, bereinigt oder abgeschaltet.
- GitHub-Stand: `main` `e6462373` mit Post-Merge-FULL `45377106828` SUCCESS inkl. API-35. PR #616 (Backup v11) auf `2da40d4e` mit FULL `45375170569` + DR `45375170646` grün gemergt; #601 technisch erledigt, formale Abnahme offen.
- 12 Worktrees mit uncommitted Beständen inventarisiert; relevanter Code: `CirqaSyncFileCompletionTest.kt` (FIT/S10), vier Kotlin-Dateien in `opencode/issue506-garmin-fit-20261122`. Private Archive (S10-Snoops/Bugreports/SQLite/Backups) bleiben lokal, überwiegend 0700.

## Empfehlung / Bitte
1. **FIT:** Übernahme bestätigen, Hash/Byteanzahl der privaten Archive **privat** prüfen, fehlende Dateien benennen, Prioritätenliste anlegen. Erst danach darf aufgeräumt werden. Risiko: `/tmp/opencode/s25-device-metadata-20261122` ist 0775 → auf 0700/0600 setzen (von CI nicht verändert).
2. **FIT + GUI:** Prüfung und Abnahme von **PR #618** abstimmen. `gh pr checks 618` meldet „no checks reported" (docs-only). CI liefert nur Status/Beratung, kein Review-Gate und keinen Merge. Erforderliche unabhängige Prüfung bitte extern organisieren.
3. **GUI:** DashboardTest-Flake (`dashboardShowsApprovedOverviewSections`, Assertion „Schlaf") prüfen; Run `45370109779` war einmalig rot, Rerun und `45377106828` grün. #619 liegt bei GUI.
4. **FIT:** #522 bleibt NO MERGE bis Parser-Zweitreview/Zählerabgleich. #599 ist Optimierung, kein Full-Pull-Blocker.

## Unsicherheiten / offene Punkte
- FITs S10-Lauf (10:22–10:53 UTC, genau ein automatischer Trigger): 34/34 Dateien übertragen/verifiziert/privat gespeichert, 32 FIT-dekodiert, 12 SQLite-Commits (`noImportableData`), `imported=0`, `failed=20`, `runDrained=false`, `coverage=device-history-unverified`, `next-transaction-followup-empty`. **Keine** vollständige Gerätehistorie belegt; Datei 39/Pagination nicht einzeln verifiziert. CIs FINAL-GO (15:00) kreuzte FITs ENDE (10:30) – es gab **keinen** zweiten Trigger und keinen weiteren Gerätezugriff. Quelle: FIT Agent-Chat im Worktree `cirqa-s10-root-cause-p0` (uncommitted).
- Keine Datei war wegen fehlender Rechte nicht inventarisierbar; Inhalte wurden aus Datenschutzgründen nicht geöffnet – Integrität muss FIT selbst bestätigen.

## Nicht anfassen
- Alle lokalen Worktrees/Archive bis zur FIT-Abnahme; #601/#522/#618 nicht durch CI schließen oder mergen; keine Geräteaktionen ohne neue Entscheidung.
