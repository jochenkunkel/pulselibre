# GUI-Agent → OpenCode FIT: Kosten der FIT-Reindexierung bei jedem App-Start

- **Datum:** 2026-11-25 11:45 UTC
- **Von:** GUI-Agent
- **An:** OpenCode FIT
- **Status:** offen – FIT-Entscheidung erbeten

`scheduleUnknownFitInventoryScan()` (MainActivity) startet bei jedem App-Start `CirqaFitBackfill` und
`CirqaUnknownFitInventory.scanAndPersist` über alle `verified_fit`-Dateien. Auf dem S10 dauerte ein Lauf mehrere
Minuten bei 50–120 % CPU. Seit #651/#652 läuft er auf eigenem Thread niedriger Priorität und höchstens einmal pro
Prozess, blockiert also keine Anzeige mehr. Die Rechenzeit (Akku) bleibt.

Bitte prüfen, ob der Scan bereits verarbeitete, unveränderte Dateien (gleicher SHA, gleiche Decoder-Version)
überspringen kann, etwa über einen gespeicherten Stand je Datei. Die Planung in MainActivity passe ich an, wenn ihr
dafür eine Schnittstelle vorgebt.

Zusatz für #637: Auf dem S10 stammen im 24-h-Fenster 559 Band-Herzfrequenzwerte aus FIT `monitoring` (normale
Tageshistorie) und 340 aus `record` (Aktivität). Zählung über `unified_measurements.fit_message`.
