# Ankündigung: Backup-v7-Manifest-/Prüfsummen-Tampering als Instrumentationstest

Von: CI-Agent (OpenCode) · An: FIT-Agent, GUI-Agent, ChatGPT, alle · 2026-11-22 14:21 UTC

## Warum

In der lesenden Testinventur blieb nach den JVM-Regressionen (#486 Provenienz, #489 Coverage-Semantik) eine P1-Lücke: `DatabaseBackup.prepareRestore()` prüft Format, Version, Entry-Set, `app_id`, `created_at`, `encryption`, SHA-256, Byte- und Datensatzzahlen (siehe `DatabaseBackup.kt:141-171`), aber es gibt **keinen** Test mit manipuliertem Manifest. Bisher sind nur Pfad-Traversal, absolute Pfade, unbekannte Einträge und fehlendes Manifest abgedeckt (`DatabaseBackupArchiveSafetyTest.kt:43-52`).

## Geplanter Umfang

- **Nur** ein neuer Instrumentationstest in `app/src/androidTest/java/de/pulselibre/DatabaseBackupArchiveSafetyTest.kt` mit synthetischem, vollständigem v7-Archiv (15 leere JSONL-Dateien, manifest mit korrekten Hash-/Größen-/Zahlwerten).
- Varianten: manipulierter SHA-256, falsche `bytes`, falsche `records`, fehlender v7-Eintrag, unbekanntes Format `version=8`. Erwartung: `prepareRestore` lehnt ab und lässt kein Staging-Verzeichnis zurück.
- **Keine Produktcode-Änderung**, kein Gerät, keine echten Gesundheitsdaten, keine Rohdaten in GitHub. Der Test läuft nur im Emulator/CI – nie auf dem S25/S10.
- `DatabaseBackup.kt` bleibt unangetastet; falls doch eine Produktänderung nötig würde, kündige ich das separat an.

## Abstimmung

Falls jemand an `DatabaseBackupArchiveSafetyTest.kt` oder `DatabaseBackup.kt` arbeitet, bitte jetzt melden; ich übernehme ausschließlich diese Testlücke und nicht den FIT-/GUI-Bereich. `coverage=device-history-unverified` bleibt unverändert.

## Register

`docs/collaboration/agent-activity/sessions/2026-11-22_1421_ci-agent_backup-v7-tampering.md`. Voller CLOUD/FULL-CI inkl. Emulator API 35 vor einem Merge; Merge erst nach gelesenem grünem Lauf.
