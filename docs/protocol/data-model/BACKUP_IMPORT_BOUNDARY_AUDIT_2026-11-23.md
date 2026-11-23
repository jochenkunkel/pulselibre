# Audit: Backup-/Import-Grenze für Status-/Import-Tabellen (2026-11-23)

Autor: CI-Agent (OpenCode). Read-only; keine Schema-, Backup- oder Produktänderung.

## Befund (BEWIESEN)
- `observation_status_events`, `import_runs`, `import_errors` werden in `CirqaDatabase` angelegt, aber im Produktcode **nie beschrieben** (grep über `app/src/main/java`).
- `DatabaseBackup.BACKUP_VERSION = 9`; `BACKUP_V9_ENTRIES = BACKUP_V8_ENTRIES + downloaded_file_archive.jsonl`. Es gibt **keine** Einträge für die drei Tabellen in `write`, `prepareRestore` oder `replaceAllStoredRows`.
- `observation_quality` ist eine deterministische Projektion der mitgesicherten FIT-Generationen und wird beim Restore rebuildt – daher kein Verlust.
- Ein Test (`DatabaseBackupV9ArchiveGapTest.statusAndImportTablesStayOutsideBackupUntilOwnerDecision`, PR #576) pinnt die Grenze explizit; kein Test behauptet einen Roundtrip für die drei Tabellen.
- Restore-Code verspricht keine Wiederherstellung dieser Tabellen; in `docs/user` wurde keine entsprechende Zusage gefunden (PLAUSIBEL, nicht vollständig exhaustiv geprüft).

## Dokumentationslage
- Zutreffend: `docs/collaboration/agent-chat/messages/2026-11-23_121000_*` benennt die Grenze korrekt („kein Produktions-INSERT-Pfad"; vor produktiver Nutzung Format-/Restore-Erweiterung nötig). `docs/protocol/data-model/MIGRATIONSPLAN_2026-11-22.md:37` hält die Entscheidung fest, ist aber in der Versionsangabe überholt.
- **Überholt / irreführend:** `docs/PROJECT_STATUS.md:15` sagt weiter „user_version 9" und „Backup-Format bleibt v8". Aktuell sind Schema 10 und Backup 9; die Grenze für Status-/Importdaten besteht unverändert, wird dort aber nicht als aktuelle Entscheidung benannt.
- Die fehlende Audit-Historie ist als Architekturentscheidung in Chat/Migrationsplan dokumentiert, aber **nicht** in einem kanonischen ADR mit Auswirkungen auf Migration/Restore/Datenschutz.

## Separater Vorschlag (nicht implementiert, keine Freigabe)
1. **Dokumentation:** `PROJECT_STATUS.md` und `MIGRATIONSPLAN` auf Schema 10 / Backup 9 aktualisieren und einen klaren Satz ergänzen: „Status-/Import-Tabellen sind write-inert und in keinem Backup-Format enthalten; eine Formaterweiterung ist vor dem ersten Produktions-INSERT nötig."
2. **Migration:** Bei Einführung von Backup 10 additive JSONL-Einträge; v1–v9 weiter restorbar; fehlende Einträge werden als leere Tabellen behandelt. Kein Downgrade.
3. **Restore:** Vor dem `replaceAllStoredRows`-Transaction die neuen Einträge validieren (wie beim Archiv), FK-Reihenfolge `import_runs` → `import_errors`, `observation_status_events` → `fit_sources`/Kataloge beachten; `integrity_check`/`foreign_key_check` innerhalb derselben Transaktion.
4. **Datenschutz:** `redacted_message` in `import_errors` darf keine Geräteadressen, Tokens, UIDs oder Gesundheitswerte enthalten; Einträge müssen vor Veröffentlichung eines Backups redigiert bleiben.
5. **Tests:** Roundtrip mit gefüllten Status-/Importzeilen, Tampering-Ablehnung, v9-Restore ohne neue Einträge, Idempotenz.

Keine eigenmächtige Formaterweiterung; Umsetzung nur nach FIT-/Persistenz-Owner-Freigabe.
