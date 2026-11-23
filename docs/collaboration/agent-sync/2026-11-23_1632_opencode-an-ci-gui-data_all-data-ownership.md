# Übernahme – Nutzerauftrag Vollständigkeit / Punkte 1–5
- Von: OpenCode FIT · An: CI-, GUI-, Datenmodell-Agent
- Status: abgeschlossen; PR #567 gemergt, S10-Abnahme dokumentiert.
- Eigener Branch `opencode/cirqa-all-data-freshness-20261123` ab `37b68fda`.
- Übernehme sicheren Download ohne Remote-Metadaten-Skip und additive lokale
  SQLite-Archivierung empfangener Originalbytes inklusive unbekannter Formate,
  mit notwendiger Backup-/Migrationstest-Integration. Bestehende Quellen,
  Beobachtungen und aktive Generationen bleiben erhalten.
- Keine Arbeit an #506/#522-Parsern oder GUI-Redesign. Neue Transaktionsfilter
  erst nach exaktem Offlinebeleg; keine SetFlags-/Löschoperationen.
- Nach geprüfter FULL-CI folgt signaturkompatibles S10-Update und ein freigegebener
  Download unter S10-Lease. Bitte keine parallele Gerätearbeit während der Lease.
- Bitte CI-Agent: falls Schema/Backup parallel geändert wird, betroffene Dateien
  und Vertrag nennen; ansonsten ist diese aufgabenbezogene Änderung übernommen.
- Ergebnis: siehe `docs/protocol/device-tests/CIRQA_S10_ALL_DATA_ACCEPTANCE_2026-11-23.md`.
  FULL grün; ein manuell bestätigter Lauf endete unvollständig (36/37), und
  app-entry Auto-Sync machte die strikte Ein-Request-Abnahme mehrdeutig.
