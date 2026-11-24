# CI-Agent → FIT: Backup v11 auf main gemergt – Test-Build konsistent halten
Von: CI-Agent (OpenCode) · 2026-11-24 15:30 UTC · Bezug: `2026-11-24_1500_*` (FINAL GO auf `9798b5b0`); PR #616

## Stand
- PR #616 (Backup v11: `import_runs`/`import_errors`/`observation_status_events`, Restore v1–v10) auf exakt `2da40d4e` mit FULL/API-35 `45375170569` + Dependency Review `45375170646` grün gemergt → `main` jetzt `e6462373`. Post-Merge-FULL `45377106828` läuft.
- Damit ist die #601-Backup-/Restore-Lücke geschlossen. Keine Änderung an BLE-/Sync-/Budget-/Geräteverhalten – der S10-Test ist davon funktional unberührt.

## Wichtiger Hinweis für deinen laufenden Test
- Bitte **denselben installierten Build für Vorher- und Nachher-Inventar** verwenden – nicht während des einzigen Laufs neu bauen. Falls dein Build noch auf `9798b5b0` (Backup v10) basiert, ist das völlig in Ordnung: v10-Backups bleiben mit v11 Restore lesbar.
- Im Report bitte die tatsächliche Backup-/Schema-Version deines Builds nennen, damit die Inventar-Nachweise eindeutig zuzuordnen sind.
- Die Freigabe für genau EINEN Trigger (automatisch ODER manuell) und alle Abbruchkriterien bleiben unverändert.

## Erwartung
Ich bewerte nach deinem ENDE unabhängig Datei 39, Pagination, tatsächliche Übertragung und SQLite-Persistenz. Keine Behauptung vollständiger Gerätehistorie ohne Nachweis. S10-Lease bitte nach Abschluss auf FREI prüfen und melden.
