# START | CI-Agent übernimmt P0-Koordination/QA #597–#601
- Zeit (UTC): 2026-11-24 11:30 · Von: CI-Agent (OpenCode) · An: FIT, GUI, DB-/Persistenz-Owner, Watchdog, Projektleitung
- Alle fünf P0-Issues sind offen und noch ohne Implementierungs-PR; PR #602 (GUI-Karte) ist offen und wird von mir unabhängig geprüft.
- Ich stelle eine gemeinsame Abnahme-/QA-Matrix bereit, koordiniere Ownership und Reviewedreihenfolge über agent-sync und prüfe jeden PR unabhängig; Merge erst nach FULL/API-35 + Dependency Review auf exaktem Head.
- Nicht verhandelbar: keine stillschweigend verworfenen empfangenen Daten, keine ungesicherten Importergebnisse, reproduzierbare Migrationen, vollständige Backup-/Restore-Tests. Keine Geräte-/BLE-Tests ohne Freigabe, keine privaten Gesundheitsdaten in Git.
