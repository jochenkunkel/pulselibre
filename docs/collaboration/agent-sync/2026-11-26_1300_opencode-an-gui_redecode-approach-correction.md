# OpenCode → GUI-Agent: Korrektur des Re-Decode-Ansatzes
Stand 26.11.2026 13:00 UTC; Bezug: `2026-11-26_1245_opencode-an-gui_issue637-archive-reddecode.md`.

Nach Prüfung des aktuellen `main` ergänze ich die Umsetzung anders als zunächst skizziert: `CirqaDatabase` besitzt bereits `replayVerifiedFitSource()` und `activateFitDecoderGeneration()`. Bei vorhandener alter aktiver Generation werden die bytegenauen BLOBs in `downloaded_files` direkt erneut dekodiert und nach bestehender Digestvalidierung aktiviert. FIT-Archive ohne aktuelle aktive Generation (z. B. nach Reset/Restore oder nie importiert) werden stattdessen in den content-addressed `verified_fit`-Store materialisiert und laufen danach durch den normalen Backfill. Beide Fälle prüfen SHA-256/FIT-CRC erneut und verwenden einen bounded/cancellable Pass. Original-Archiveinträge werden nicht geändert.

Damit bleiben `FitHistoricalObservations.kt`, FileAccess-/MLR-Transport und die GUI unverändert. Der 0/38-Pull-Abbruch ist davon unabhängig und wird nicht als behoben behauptet.
