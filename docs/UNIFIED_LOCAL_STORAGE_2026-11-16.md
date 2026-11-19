# Lokale Speicherung: CIRQA-Ereignisse und FIT-Messwerte

Stand: 2026-11-18. Der Realtime-/Dashboardpfad und FIT-Historienpfad benutzen
jetzt dieselbe kanonische DB, während die alte Historien-DB als Übergangsquelle
erhalten bleibt:

- `cirqa_measurements.db` / `CirqaDatabase` (Schema v5) speichert
  Realtime-Ereignisse sowie normalisierte FIT-Beobachtungen, Cursor,
  Quellmetadaten, Feldprovenienz und ausgewählte FIT-`device_info`-Metadaten.
  V25-FIT-Dateien schreiben nach einem verifizierten Kopierschritt pro Datei
  Messwerte, Provenienz, whitelisted Gerätefeldwerte und Cursor atomar.
- `pulselibre_readings.db` / `LocalReadingStore` bleibt als historische
  Quell-/Rollback-DB erhalten. Schema v2 ergänzt die Quellen-/Feldprovenienz
  additiv, ohne vorhandene Messwert- oder Cursorzeilen zu verändern.
- Die beiden physischen SQLite-Dateien bleiben vorerst erhalten. Schema v3 der
  kanonischen DB erhält einen transaktionalen, wiederholbaren Kopierpfad; die
  FIT-Originale und die Historien-Quell-DB werden nicht gelöscht. Die physische
  Konsolidierung ist im
  [Storage-ADR](protocol/data-model/CIRQA_STORAGE_ARCHITECTURE_ADR_2026-11-18.md)
  beschrieben und noch nicht umgesetzt.

Dashboard/Statistik lesen weiterhin den Ereignisspeicher. CSV bezieht alte
`legacy_record`-Ereignisse, den normalisierten kanonischen Store und rohe CIRQA-
Ereignisse dedupliziert ein. FIT-Importzeit und Gerätemesszeit bleiben getrennt.
Es werden nur beobachtete, validierte Werte gespeichert.

## Migration und Rückwärtskompatibilität
**Nutzerentscheidung vom 2026-11-17 (im Bereinigungs-PR):** Die frühere
`cirqa_overnight.jsonl` wird nicht mehr benötigt. Der automatische Import entfällt;
beim erfolgreichen Öffnen der kanonischen SQLite-Datenbank wird ausschließlich diese
Datei im privaten App-Dateiverzeichnis bestmöglich gelöscht. Bei fehlgeschlagener
Löschung wird beim nächsten Öffnen erneut versucht. Bereits gespeicherte SQLite-Ereignisse
bleiben erhalten; nicht importierte Inhalte der alten Datei werden bewusst verworfen.
Der aktuelle Nacht-Recorder bleibt aktiv und schreibt weiterhin ausschließlich SQLite.
Nutzergesteuerte JSONL-Exporte sind davon unabhängig und bleiben verfügbar.

Die frühere `cirqa_overnight.jsonl` wird gemäß der ausdrücklichen
Nutzerentscheidung nicht mehr importiert; beim Öffnen wird nur diese alte Datei
best-effort entfernt. Gespeicherte SQLite-Ereignisse und die getrennte FIT-
Originalablage werden dadurch nicht gelöscht. Nutzergesteuerte JSONL-Exporte
bleiben verfügbar.

## Grenzen

Die FIT-/Raw-Originalbytes bleiben separat und werden nicht in Backup v5 kopiert;
Backup enthält SHA-/Dateimetadaten, Feldherkunft und privacy-gefilterte
FIT-`device_info`-Felder. Seriennummern und Geräteindizes werden ausgelassen.
Dashboard-Repositories
greifen noch nicht direkt auf den normalisierten FIT-Store zu. Eine physische
DB-Konsolidierung, historische Geräteabdeckung und ein CIRQA-Hardwaretest sind
nicht durch diesen Storage-Stand behauptet. Weitere Details und Tests: oben
verlinktes Storage-ADR und `CIRQA_SQLITE_STORAGE.md`.
