# OpenCode – #144 Datenarchitektur/Persistenz

- Agent/Umgebung: OpenCode (vom Nutzer ausdrücklich als Datenmodell-/Persistenz-Agent beauftragt)
- Beginn: 2026-11-22 22:41 UTC
- Letzte Aktualisierung: 2026-11-22 22:55 UTC
- Status: aktiv (Phase A abgeschlossen, Phase B begonnen)
- Rolle/Zuständigkeit: alleiniger koordinierender Owner für SQLite-Schema, Migration, Datenintegrität, Backup/Restore und Persistenz-API. #506 und #150 nur fachliche Referenz; keine Übernahme der FIT-/BLE-Implementierung oder GUI-Arbeit.
- Aktueller Auftrag: #144 – IST-Inventur, SOLL-Schema, Migrationsplan, dann additive getestete Migration.
- Jetzt in Bearbeitung: #144 Phase B. Code-IST abgeschlossen; bestätigter Importabbruch durch `IllegalStateException`/`IllegalArgumentException` ist dateiweise abgefangen (`FailureStage.SOURCE_CONFLICT`), ohne `check(...)`-Integritätsprüfungen zu entfernen. Nächster Schritt: additive v9-DDL/Migration plus Qualitäts-/Statuspersistenz und SQLite-Importnachweis.
- Zeitnah geplant: #144 Phase B (additive Migration + Regressionstests), #150/#506 Abstimmung (keine exklusive Reservierung).
- Betroffene Bereiche/Dateien: `CirqaDatabase.kt`, `DatabaseBackup.kt`, Import-/Backfill-Klassen, JVM-/Instrumentationstests, `docs/protocol/data-model/**`, `docs/PROJECT_STATUS.md`, Koordinationsdateien unter `docs/collaboration/**`. Kein Gerätezugriff, keine BLE-Aktion.
- Branch/Worktree: `data/issue144-schema-und-migration`, auf `origin/main` `bf94f28a` rebased
- Abstimmungsbedarf: FIT-Agent (OpenCode #506/#500) – Feldsemantik/Statuscodes/Decoder-Generationen; GUI-Agent – Schnittstellenstabilität; ChatGPT – #506-Fachreferenz. Konkrete Nachricht: `../agent-sync/2026-11-22_2241_datenmodell-an-fit-gui-chatgpt_issue144-ownership.md`.
- Agent-sync-Nachrichten: oben genannte Ownership-Nachricht; START-Chat `../agent-chat/messages/2026-11-22_224100_opencode_START_issue144-datenarchitektur.md`.
- Nachweise: eigene lokale Prüfung `gradle :app:testDebugUnitTest :app:assembleDebug --offline` erfolgreich (218 Suites, 957 Tests, 0 Fehler; Debug-APK gebaut). Keine volle Android-CI, kein Emulator-, kein Gerätelauf. Ausgang `origin/main` `c3bc65f0`, zwischenzeitlich `bf94f28a` eingearbeitet.
- Nächster Schritt: FIT-Agent-Antwort zu Statuscodes einholen; additive v9-Migration, Backup-Erweiterung, Qualitäts-/Statuspersistenz und separater SQLite-Importtest; danach volle CI.
