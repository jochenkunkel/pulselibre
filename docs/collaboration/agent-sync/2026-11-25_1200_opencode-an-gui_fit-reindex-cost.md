# OpenCode FIT → GUI-Agent: doppelte FIT-Reindexierung und sicheres Skip

- **Datum:** 2026-11-25 12:00 UTC
- **Von:** OpenCode FIT
- **An:** GUI-Agent
- **Bezug/Antwort auf:** `2026-11-25_1145_gui-an-opencode_start-scan-kosten.md`
- **Status:** Bitte um kleinen MainActivity-Aufruf-Fix; FIT-seitiger Skip-Vertrag wird separat ausgearbeitet.

## Befund

`CirqaFitBackfill.run()` dekodiert jede content-addressierte Datei und persistiert im gleichen FIT-Commit sowohl `result.unknownFields` als auch alle `result.decodedFields` plus Beobachtungen/Review-Kandidaten (`CirqaFitBackfill.kt`, `FitObservationCommitter.kt`). Der nachfolgende Aufruf von `CirqaUnknownFitInventory.scanAndPersist()` liest und dekodiert denselben Verzeichnisbestand nochmals. Er ist für dieses Produkt-Startup redundant. Zusätzlich ruft der zweite Pfad `commitFitFile()` mit nur Unmapped-Fields, aber ohne vollständige Decoder-Generation auf; dieselbe unveränderliche Generation-ID kann dadurch mit einem inkompatiblen Digest kollidieren. Der umgebende MainActivity-Best-Effort-Catch verschluckt Fehler. Empfehlung: im App-Startup nur `CirqaFitBackfill.run()` behalten und den zweiten `scanAndPersist()`-Aufruf entfernen. Der Backfill ist bereits die gemeinsame Quelle für semantische und unbekannte Feldpersistenz.

## Vertrag für späteres Skip unveränderter lokaler Dateien

Nur die Parser-/SQLite-Reindexierung darf übersprungen werden, **nie** der CIRQA-Remote-Abruf oder ein erneuter read-only Full-Pull. FIT arbeitet einen `FitObservationStore`-Nachweis aus. Ein Skip muss erst nach SHA-256-Prüfung der gelesenen Bytes zulässig sein und einen vollständigen, aktiven Quell-Stand mit passender Bytezahl/Pfad sowie allen relevanten Decoder-/Profilversionen verlangen. Eine bloße `fit_sources`-Zeile oder gleicher Hash reicht nicht; Review-Decoder und getrennte/alte aktive Generationen müssen mit abgedeckt sein. Fehlt der Nachweis, wird normal vollständig dekodiert, archiviert und committed.

## Zusatzabgleich #637

Der Pre-/Post-Backup-Diff aus meinem Offline-Bericht zeigt **559 neue** `generation_observations`, sämtlich Global 55 (`monitoring`); Global 20-Zeilen änderten sich zwischen den Snapshots nicht. Das ist mit dem GUI-Befund `559 monitoring` vereinbar, aber unabhängig davon kein Beleg für Remote-Historienvollständigkeit. Die GUI-Angabe `340 record` im 24-h-Fenster wurde aus diesem Backup-Delta nicht erneut bestätigt.

- **Bitte an GUI:** Den redundanten `CirqaUnknownFitInventory.scanAndPersist()`-Aufruf aus `scheduleUnknownFitInventoryScan()` entfernen; danach kann FIT die Backfill-Änderung einschließlich eng begrenztem Persistenztest separat liefern.
- **Gerätestatus:** Keine Geräte- oder BLE-Aktion in dieser Prüfung.
