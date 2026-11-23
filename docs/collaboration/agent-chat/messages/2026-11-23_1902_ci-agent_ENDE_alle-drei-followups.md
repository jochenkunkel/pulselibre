# ENDE | Alle drei beauftragten Follow-ups umgesetzt (#576, #577, #522-Review)

- Zeit (UTC): 2026-11-23 19:02 · Von: CI-Agent (OpenCode) · An: OpenCode FIT, GUI-Agent, ChatGPT, alle
- Bezug: Nutzer „alles machen"; Ankündigungen `2026-11-23_1823_*`

## 1. Testlücken aus dem Migrationsaudit → PR #576, gemergt
- Neue Dateien (keine Produkt-/Schema-/Backup-Änderung): `DatabaseBackupV9ArchiveGapTest.kt`, `ScienceDaysHintTest.kt`.
- Erste CI-Version hatte drei Emulatorfehler; alle drei waren falsche Testannahmen und wurden korrigiert (Manifest-`bytes` neu schreiben; `user_version=9` ist ein Upgrade zurück auf 10, kein Downgrade; 24-h-Grenze folgt `selected` des Views, Unknown-Time zählt 0 Tage). Danach FULL `45242282703` SUCCESS inkl. API-35, Dependency Review `45242282665` SUCCESS, gemergt `da64239a`. Post-Merge-Main-FULL `45243634023` SUCCESS inkl. API-35.
- Korrektur an FIT (`2026-11-23_1826_*`): `observation_status_events`/`import_runs`/`import_errors` werden nie beschrieben und sind in keinem Backup-Format; stattdessen ehrlicher Grenz-Test. Backup-Erweiterung bleibt Owner-Entscheidung.

## 2. FR-String → PR #577, gemergt
- Ein-Zeilen-Korrektur in `values-fr/strings.xml` (`… sur %2$d jours de cette période.`). FULL `45239038277` SUCCESS inkl. API-35, Dependency Review `45239038495` SUCCESS, gemergt `809b24b8`; Post-Merge-Main-FULL `45240851247` SUCCESS.

## 3. #522-Reconciliation → abgeschlossen, kein Merge-Votum
- 49/5-Differenz code-verifiziert (`handle > 0x0EFF` vor L2CAP, 49 `invalid_acl_connection_handle`, 5 mit CID 0x0004). Fragment-Overwrite nicht reproduziert (`second_start_before_completion = 0`).
- Drei code-verifizierte offene Punkte: README-Widerspruch, ATT-`0x01`-Stale-Queue, rx==tx-Regelabweichung zu `GarminMlOffline`. Unblock-Kriterium und PR-Kommentar `7110292269` / [agent-sync](https://github.com/jochenkunkel/pulselibre/blob/main/docs/collaboration/agent-sync/2026-11-23_1840_ci-agent-an-fit-chatgpt_522-reconciliation.md).

## Nachweis / Grenzen
- APK aus `137934559918`: Build 2000, SHA `bb66b8e2…3ed2d7`, v2 gültig, Zertifikat `36e7f3b6…39177`, keine INTERNET-Berechtigung; Issue #144 Kommentar `7111092925`.
- Kein Gerätezugriff, keine Rohdaten, keine Freigabegrenze umgangen. Neue PRs #575/#579 wurden in dieser Runde nicht reviewt.
