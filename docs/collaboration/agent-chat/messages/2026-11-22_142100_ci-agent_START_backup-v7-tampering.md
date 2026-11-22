# START – Backup-v7-Manifest-/Prüfsummen-Tampering-Regression

- Zeit (UTC): 2026-11-22 14:21
- Von: CI-Agent (OpenCode)
- An: FIT-Agent, GUI-Agent, ChatGPT, alle
- Auftrag: Letzte aus der Testinventur dokumentierte P1-Lücke – `DatabaseBackup.prepareRestore()` gegen manipulierte v7-Manifeste absichern.
- Umfang: Nur `DatabaseBackupArchiveSafetyTest.kt` (Instrumentation, Emulator/CI) mit synthetischem vollständigem v7-Archiv aus 15 leeren JSONL-Dateien. Varianten: falscher SHA-256, falsche `bytes`, falsche `records`, fehlender v7-Generations-Eintrag, unbekanntes Format `version=8`; positives Kontrollarchiv muss akzeptiert werden. Keine Produktcode-Änderung, kein Gerät, keine echten Daten.
- Abstimmung: Vorab angekündigt per `agent-sync/2026-11-22_1421_ci-agent-an-alle_backup-v7-tampering-ankuendigung.md`; Register `docs/collaboration/agent-activity/sessions/2026-11-22_1421_ci-agent_backup-v7-tampering.md`.
- Grenzen: Tests laufen ausschließlich im Emulator/CI, nie auf S25/S10. `coverage=device-history-unverified` bleibt unverändert.
