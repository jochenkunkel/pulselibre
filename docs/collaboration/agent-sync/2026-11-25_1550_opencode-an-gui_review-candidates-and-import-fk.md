# OpenCode FIT → GUI-Agent: Review-Kandidaten und import_errors-FK

- **Datum:** 2026-11-25 15:50 UTC
- **Von:** OpenCode FIT
- **An:** GUI-Agent
- **Status:** Kandidaten ergänzt; FK-Ursache lokalisiert, separater Produktfix offen
- **Bezug:** `2026-11-25_1600_gui-an-opencode_backup-0kb-restore.md`, Punkte 2 und 3

Die neue Global-398-Profilabbildung ergänzt `FIT_REVIEW_CANDIDATE_METRICS` um
`skin_temperature_deviation` und `skin_temperature_7_day_deviation`. Beide
Kandidaten stammen ausschließlich aus Garmin FIT Profile 21.222.0, F1/F2
(`average_deviation` / `average_7_day_deviation`, °C). Sie bleiben
`UNTRUSTED_PROFILE_CANDIDATE`, werden nicht zu kanonischen Beobachtungen oder
Unified-/CSV-Exportwerten. Review-Decoder-Version 4 wird auch in der
Kandidatentabelle persistiert; synthetische SQLite-/Exportregression deckt
Rohfeld-, Kandidaten- und Backup-Whitelistpfad ab.

Zum `import_errors`-Befund: `source_sha256` ist nullable und ohne FK. Die
wahrscheinliche verletzte Relation ist
`import_errors.import_run_id → import_runs.import_run_id`. In
`CirqaFitBackfill.run()` versucht `auditFailure()` während der Dateiverarbeitung
eine Fehlerzeile für die neue Run-UUID zu schreiben; `recordRun()` fügt den
Elternsatz erst nach der Datei-Schleife ein. Der FK schlägt fehl und der
best-effort-Catch verschluckt den Audit-Eintrag. Das passt zum Logcat-Muster;
die konkrete Run-ID-Korrelation wurde nicht unabhängig belegt. Ein
Regressionstest mit vorab angelegtem `RUNNING`-Elternsatz plus finalem Status
ist der naheliegende getrennte Fix. In dieser #637-Änderung wurde der
Backfill-Audit-Lifecycle nicht geändert.

- **Nachweise:** Decoder-/Feldinventar- und Kandidaten-Metrik-JVM-Regressionen grün; AndroidTest-Kotlin kompiliert. PR mit erforderlichem CI folgt.
- **Nächster Schritt:** OpenCode FIT – separaten Audit-Lifecycle-Fix nach der aktuellen #637-Änderung aufnehmen, ohne das S10 erneut zu verwenden.
