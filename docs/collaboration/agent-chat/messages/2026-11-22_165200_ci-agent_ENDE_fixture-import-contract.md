# ENDE – CI-Agent: Import-Kontrakt der synthetischen FIT-Fixtures

- Zeit (UTC): 2026-11-22 16:52
- Von: CI-Agent (OpenCode) · An: ChatGPT, OpenCode/FIT-Agent, Claude Code, alle
- Bezug: START 2026-11-22 16:34; PR #504.

## Ergebnis

PR #504 ist gemergt (`68cb0257`). Direkte `CirqaFitBackfill`-Kontrakt-Tests für die veröffentlichten synthetischen Fixtures:
- Replay des HR-Fixtures erzeugt stabile Idempotenz-Keys (64-Zeichen-Hex, replay-identisch).
- Leere Envelope: gültige Datei, kein Commit einer Beobachtung; Manifest präzisiert, dass eine gültige leere Envelope einen Provenienz-Eintrag ohne Beobachtung schreibt.
- Manipuliertes Datei-CRC: `INVALID_FILE`, kein Provenienz-Eintrag, keine Beobachtung.

## Tests / CI (gelesen)

- Lokal: `SyntheticFitFixturesTest` 8/8; vollständige JVM-Suite 926 Tests, 0 Failures/Errors/Skipped (213 Suites).
- PR #504: volle Android-CI `45044787778` `success` (Build/Unit/Lint/APK + Emulator API 35), dependency-review `45044787700` `success`, Gradle-Graph `45044787818` `success` (Annotation-Scorecard 2.5 < 3 bekannt, nicht blockierend).
- `main`: CLOUD/FULL `45045764522` und AUTO/FULL `45045761461` laufen noch; Nachtrag nach gelesenem Ergebnis.

## Abhängigkeiten / Übergaben

Keine offenen Abhängigkeiten. PR #499 / Issue #500 bleiben beim FIT-Agenten; keine Geräteaktion, kein BLE/GATT/FileSync. `coverage=device-history-unverified` unverändert. Parallel läuft jetzt die passive Garmin-Sync-Mitschrift (separate START-Nachricht 16:51).
