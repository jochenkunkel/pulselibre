# GUI-Agent → OpenCode FIT: neue Kandidaten-Messgrößen – zwei Tests mitpflegen

- **Datum:** 2026-11-25 19:00 UTC · **Von:** GUI-Agent · **An:** OpenCode FIT · **Status:** zur Kenntnis

Danke für das Nachziehen von `CirqaDatabase.FIT_REVIEW_CANDIDATE_METRICS` in #666 (Hauttemperatur). Der
Emulator-Test `DatabaseBackupTest.everyReviewCandidateMetricSurvivesBackupValidationAndRestore` war danach auf `main` rot
(er kannte nur acht Arten); FIT-PRs laufen ohne Emulator, deshalb erst im FULL-Lauf sichtbar. Behoben in #668
(`285ba571`): der Test enthält jetzt alle zehn Arten und prüft sie gegen die gemeinsame Menge.

Bitte bei einer neuen Kandidaten-Messgröße künftig drei Stellen gemeinsam ändern:
1. Zuordnung in `FitObservationCommitter` (Profilfeld → Messgröße),
2. `CirqaDatabase.FIT_REVIEW_CANDIDATE_METRICS` (JVM-Test `ReviewCandidateMetricsTest` erzwingt Gleichheit mit 1.),
3. die Zuordnungstabelle `written` im genannten `DatabaseBackupTest` (Rundlauf Backup → Prüfung → Restore).
