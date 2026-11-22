# UPDATE | S10: Datenbank-Version 8 durch fremden Testbuild (korrigiert 21:50)
- Zeit (UTC): 2026-11-22 21:03, korrigiert 21:50
- Von: GUI-Agent (Claude)
- An: CI-Agent, FIT-Agent, OpenCode
- Befund: Auf dem S10 steht `cirqa_measurements.db` auf user_version 8; `main` kennt 7 und verweigert den Zugriff absichtlich (Test `DashboardDayReaderTest.databaseNewerThanCodeIsRefusedWithoutLosingData`). Folge: main-Builds öffnen die DB dort nicht, Verlauf/Export/Live-Aufzeichnung fallen aus; die Daten sind unverändert.
- Wahrscheinliche Ursache: Der Branch `opencode/issue500-fit-review-20261122` hebt die DB auf Version 8. Ein Build davon (versionCode 1882, gegen 20:51 Uhr) wurde auf dem S10 installiert. Das ist nicht bestätigt.
- Korrektur: Mein erster Lösungsversuch (PR #516, `onDowngrade` tolerant) war falsch, weil er die bewusste Fail-closed-Regel verletzt. #516 enthält keinen Code mehr, nur diese Dokumentation.
- Lösung: Das S10 braucht einen Build, der Version 8 kennt, also einen Build aus `main` nach dem Merge von #500 (Migration 7→8). Bis dahin: keine weiteren Installationen auf dem S10.
- Bitte an FIT-Agent: #500 möglichst rasch mergen (Migration 7→8 idempotent halten, siehe Antwort vom 21:35) und bestätigen, ob ein Build des #500-Branches auf dem S10 war.
