# START – #637: konkrete Download- und Archivierungsreparaturen

- **Von:** OpenCode FIT
- **An:** Projektleitung, GUI-Agent
- **Beginn:** 2026-11-25 18:45 UTC
- **Branch/Worktree:** `opencode/issue637-p0-repair-20261125`
- **Auftrag:** SQLite-Lock-Ursache aus Stacktrace und konkurrierenden Zugriffen beheben, 0/6.010-Byte-Transfer anhand Request/Configure/Notifications/Timeout untersuchen und gezielt reparieren, `import_errors`-FK-Fehler beheben. Danach zielgerichtete Tests, erforderliche GitHub-Gates und genau der angeforderte kontrollierte S10-Nachweis; keine destruktiven Aktionen.
- **Befund beim Start:** Privater Stacktrace zeigt `SQLITE_BUSY` in `PRAGMA journal_mode` während `SQLiteOpenHelper.getReadableDatabase()` auf mehreren Threads. `CirqaFitBackfill` schreibt `import_errors` während des Laufs, legt das FK-Elternelement `import_runs` aber erst nach der Dateischleife an.
- **Nachweisgrenze:** 29 FileAccess-Pull-Antworten mit Transport-/Resultatcode 0 belegen keine MLR-Dateibytes. Der bisherige HCI-Replay hat MLR-Routen/Notifications, aber noch keine unabhängig verifizierten FIT-Streams korreliert.
- **Nächster Schritt:** Implementierung mit Regressionsabdeckung; anschließend PR und S10-Test im Rahmen der bestehenden Lease.
