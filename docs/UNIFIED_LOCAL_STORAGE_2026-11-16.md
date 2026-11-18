# Lokale Speicherung: Ereignisse und historische FIT-Messwerte

Stand: 2026-11-18. Die frühere Bezeichnung „einheitlich“ beschreibt den
Realtime-/Dashboardpfad, aber nicht mehr alle aktiven Schreibpfade:

- `cirqa_measurements.db` / `CirqaDatabase` speichert Realtime-Ereignisse.
  `CirqaLocalLog.append()/reading()` und der Nacht-Dienst schreiben in `events`.
- `pulselibre_readings.db` / `LocalReadingStore` speichert FIT-Historienmesswerte,
  Sync-Cursor und seit Schema v2 FIT-Quell-/Feldprovenienz. `commitFitFile`
  schreibt pro Datei Messwerte, Quellenbezug und Cursor in einer Transaktion.
- Die beiden physischen SQLite-Dateien bleiben vorerst erhalten. Die additive
  v1→v2-Migration verändert keine bestehenden Messwert- oder Cursorzeilen.
  Eine spätere physische Konsolidierung ist im
  [Storage-ADR](protocol/data-model/CIRQA_STORAGE_ARCHITECTURE_ADR_2026-11-18.md)
  beschrieben und noch nicht umgesetzt.

Dashboard/Statistik lesen den Ereignisspeicher. CSV kann historische Messwerte
aus dem zweiten Store lesen bzw. idempotent als `legacy_record` in `events`
importieren. FIT-Importzeit und Gerätemesszeit bleiben getrennt. Es werden nur
beobachtete, validierte Werte gespeichert.

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

Die FIT-Originalbytes bleiben separat und werden nicht in Backup v2 kopiert;
Backup enthält SHA-/Dateimetadaten und Feldherkunft. Dashboard-Repositories
greifen noch nicht direkt auf den normalisierten FIT-Store zu. Eine physische
DB-Konsolidierung, historische Geräteabdeckung und ein CIRQA-Hardwaretest sind
nicht durch diesen Storage-Stand behauptet. Weitere Details und Tests: oben
verlinktes Storage-ADR und `CIRQA_SQLITE_STORAGE.md`.
