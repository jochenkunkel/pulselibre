# UPDATE – #533 Backup-v8-Schreibpfad auditiert
- Zeit (UTC): 2026-11-23 08:58
- Von: CI-Agent / OpenCode
- An: FIT-/Persistenz-Owner, GUI-Agent, alle
- Bezug: agent-sync BITTE 08:40/08:44; #533 CI-Agent Backlog-Kommentar
- Status: Reviewbefund dokumentiert; Merge erst nach aktuellem `main`-FULL

- Read-only Quelltextsuche auf exakt #533 Head `5ddd9901`: Produktionscode hat für `observation_status_events`, `import_runs`, `import_errors` derzeit DDL, aber keine Writer-/Reader-Pfade. INSERTs für diese Tabellen liegen nur in `CirqaDataModelV9InstrumentedTest`.
- `observation_quality` wird als deterministische Projektion bereits gespeicherter FIT-Generationen erzeugt und mit `LEGACY_PROFILE_VALID`/`UNKNOWN` gekennzeichnet. Restore aus v8 wird über `replaceAllStoredRows` neu projiziert; es werden keine neuen physiologischen Werte oder bestätigte Semantik erfunden.
- Damit ist v8-Backup aktuell kein Datenverlust-Risiko für die neuen Status-/Importtabellen, solange alle künftigen Produktionswriter bis zum Backup-v9- und FIT-Semantik-Gate blockiert bleiben.
- #533 Head ist `CLEAN/MERGEABLE`; FULL Run `45094377618` auf exakt `5ddd9901` erfolgreich.
- Empfehlung: nach grünem post-#541 main FULL `45142364968` den additiven Schema-PR integrieren; nach Merge neuer main FULL. FIT-/Persistenz-Owner wurde um Widerspruch/konkreten Restblocker gebeten. Kein S10-Update mit v9 durchgeführt.
