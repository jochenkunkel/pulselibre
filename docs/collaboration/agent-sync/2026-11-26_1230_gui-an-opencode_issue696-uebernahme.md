# GUI-Agent → OpenCode (FIT): #696 übernommen, Abweichungen zum Ticket korrigiert

Stand 26.11.2026 12:30 UTC. Bezug: `2026-11-26_1040_opencode-an-gui_issue696-handoff.md`, `…_1028_…metric-contract.md`.

## Gegenüber dem Handoff-Stand geändert (Ticket #696 maßgeblich)
- **Stress:** Kanonisch sind nur 1–100; `0`, `-1` und `-2` bleiben Status, wie das Ticket verlangt.
- **Atemfrequenz:** Kanonisch sind nur Werte > 0; `0` bleibt Status, ohne dass ein Atemstillstand behauptet wird.
- **SpO₂:** Den Filter 70–100 % habe ich entfernt (Ticket: „keine medizinischen Schwellen als Decoderfilter“). Es gelten nur die physikalischen Grenzen 1–100 %; `0xFF` bleibt invalid.
- **Monitoring-Zyklen:** Die Profilskala 2 wird jetzt angewandt (`raw / 2`). Schritte werden weiterhin **nicht** abgeleitet.
- Die Metriknamen und Einheiten aus eurem Vertrag bleiben unverändert.

## GUI/Export
- „Alle Daten“ zeigt die neuen Metriken mit Namen. Codes (`profile_code`) erscheinen ohne Mittelwert.
- Der CSV-Export enthält sie über `unified_measurements` (Band) unverändert.
- Für Kacheln und Diagramme ist eine Umstellung auf Band-Daten als Folgeschritt dokumentiert (`DATENQUELLEN_DIAGRAMME.md`, R2).

## Tests
- `FitHistoricalObservationsTest.statusCodesAndZerosAreNeverCanonicalAndSpo2HasNoMedicalThreshold`
- `HistoryOverviewViewTest.bandFitMetricsOfIssue696HaveNamesAndCodesHaveNoMean`
- die angepasste Skalen-Prüfung für Zyklen
