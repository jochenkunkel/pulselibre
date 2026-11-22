# CI-Agent – Backup-v7-Tampering-Regression

- Agent/Umgebung: CI-Agent (OpenCode, Emulator-/CI-Tests)
- Beginn: 2026-11-22 14:21 UTC
- Letzte Aktualisierung: 2026-11-22 14:21 UTC
- Status: aktiv (Ankündigung erfolgt, Implementierung folgt)
- Rolle/Zuständigkeit: Tests und CI für Backup-/Restore-Sicherheit
- Aktueller Auftrag: Letzte dokumentierte P1-Lücke – fehlende Manifest-/Prüfsummen-Tampering-Regression für `DatabaseBackup.prepareRestore()` (v7).
- Jetzt in Bearbeitung: synthetischer v7-Test in `DatabaseBackupArchiveSafetyTest.kt`; kein Produktcode, kein Gerät.
- Zeitnah geplant: keine exklusive Reservierung; danach Inventur weiterer P1-Lücken.
- Betroffene Dateien: `app/src/androidTest/java/de/pulselibre/DatabaseBackupArchiveSafetyTest.kt`, `docs/collaboration/agent-sync/`, `docs/collaboration/agent-chat/messages/`, eigene Sitzungsdatei. Kein Produktcode, keine FIT-/GUI-Dateien.
- Branch/Worktree: `ci/backup-v7-tampering-20261122`, `/tmp/opencode/pulselibre-backup-v7-tampering-20261122`
- Abstimmungsbedarf: Produktzeile in `DatabaseBackup.kt` nicht geplant; Ankündigung `2026-11-22_1421_ci-agent-an-alle_backup-v7-tampering-ankuendigung.md`. Keine Geräteaktion ohne neue Freigabe.
- Nachweise: ausstehend (lokal kompilierbarer Test, voller CLOUD/FULL-CI mit Emulator API 35).
- Stop-Grenzen: keine BLE-/GATT-/FileSync-Kommandos, kein Reset/Entkoppeln/`CLOSE_ALL`, keine Löschung realer Daten, keine Instrumentationstests auf physischen Geräten, keine Rohdaten/Gesundheitswerte/Gerätekennungen in GitHub.
- Nächster Schritt: Test ergänzen, `git diff --check`, PR mit vollem CI; Merge erst nach gelesenem grünem Lauf und FULL-Lauf auf `main`.
