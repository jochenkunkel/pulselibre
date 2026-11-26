# GUI-Agent → OpenCode (FIT): Reset löscht jetzt auch die Originale (PR #690)

Stand 26.11.2026 10:15 UTC.

## Owner-Meldung
Nach „Datenbank zurücksetzen“ zeigte der Verlauf weiter Live- und Band-Werte.

## Ursachen
1. `unified_measurements` wurde nicht geleert. `refreshLive` erkennt ein Ersetzen nur an `MAX(events.id) < cursor`. Da `events` AUTOINCREMENT ist und die Aufzeichnung nach dem Reset sofort wieder schreibt, griff das nie.
2. `verified_fit/` und die übrigen Original-Verzeichnisse blieben liegen. `CirqaFitBackfill` und `archiveExistingDownloads` importierten sie beim nächsten Start wieder.

## Änderung (Owner-Entscheidung: Originale mitlöschen)
- `CirqaDatabase.replaceAllStoredRows` ruft in derselben Transaktion `UnifiedMeasurements.clearDerived` auf. Das gilt für Reset **und** Restore; Restore hatte dasselbe Live-Problem.
- `DatabaseBackup.reset` löscht danach `verified_fit`, `verified_raw`, `downloaded_originals_pending` und `cirqa_fit_history`. Symlinks werden nicht verfolgt. Der Dialog sagt das ausdrücklich.

## Bitte um Prüfung
- Gibt es weitere Original- oder Staging-Verzeichnisse, die beim Start re-importiert werden?
- Ist `cirqa_fit_history` während eines laufenden History-Downloads in Benutzung? Ein Reset stoppt bisher nur die Aufzeichnung, keinen laufenden History-Sync.
- Beim **Restore** bleiben vorhandene Originale liegen und werden beim nächsten Start zusätzlich zum Backup importiert, idempotent per SHA. Ist das so gewollt?
