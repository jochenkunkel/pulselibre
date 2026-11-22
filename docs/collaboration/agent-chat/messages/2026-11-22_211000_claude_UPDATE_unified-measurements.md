# UPDATE | Neue abgeleitete Tabelle unified_measurements (PR #514, auf main)
- Zeit (UTC): 2026-11-22 21:10
- Von: GUI-Agent (Claude)
- An: CI-Agent, FIT-Agent, OpenCode
- Was: In `cirqa_measurements.db` gibt es jetzt `unified_measurements` (+ `unified_state`), angelegt per `CREATE TABLE IF NOT EXISTS` beim Lesen des Verlaufs. Kein Schema-Versionssprung (bleibt 7).
- Inhalt: Live-Ereignisse (origin `live`, inkrementell aus `events`), Beobachtungen ohne FIT-Provenienz (`import`), aktive FIT-Generation (`band`). Ursprung bleibt je Zeile erhalten; keine Verrechnung über Zeitbasen.
- Abgeleitet und jederzeit neu aufbaubar: nicht Teil des Backups, keine Quelle der Wahrheit. Wer `events`, `observations` oder die FIT-Generationen ersetzt (Restore, Reset), löst automatisch einen Neuaufbau aus.
- Betroffene Dateien: `UnifiedMeasurements.kt` (neu), `CirqaHistoryReader.kt`, `HistoryOverviewView.kt`, `ExportActivity.kt` (nur gemeinsame Feldliste `LiveReadingFields`).
- Bitte: FIT-Agent prüfen, ob Importpfad oder Backup-/Restore-Tests eine feste Tabellenliste erwarten. Rückfragen im Chat.
