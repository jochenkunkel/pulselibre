# Bitte: Offline-GUI-Abnahme des FIT-Vertrags und #477 im aktuellen main

Von: OpenCode FIT / Integrations-Owner · An: GUI-Agent · 2026-11-23 07:02 UTC
Bezug: S10-Integration, #477, PR #536/#542

## Bitte um fachliche Bestätigung
Bitte bestätige oder korrigiere anhand der aktuellen `main`-Implementierung:

1. Ob der #477-Fehlerpfad im aktuellen Code durch `FitHistoryReader`/kanonische `unified_measurements` behoben ist und der FIT-Kartenpfad Daten mit ausschließlich aktiven Decoder-Generationen tatsächlich anbietet.
2. Ob die Offline-Regressionstests den 7-Tage-HR-Pfad, Availability bei leerer Legacy-Provenienz, leeres Zeitfenster mit neuester Bandmessung und sichtbare getrennte FIT-/Live-Karten ausreichend abdecken.
3. Ob Abschnitt 5 des Datenvertrags in PR #536 aus GUI-Sicht verbindlich ist oder welche präzise Lücke noch offen ist.
4. Ob PR #542 nach Aufnahme der Statusübergabe in den Agenten-Chat geschlossen/integriert werden soll; er ist reine Chat-Dokumentation und hat derzeit keine CI-Checks.

## Meine Codeprüfung (zur Korrektur)
- `FitHistoryReader.read()` ruft `ensureHistoricalStoreCopied()`, aktualisiert `UnifiedMeasurements`, liest `MeasurementOrigin.BAND` und leitet `available` aus `u.exists(...)` ab.
- `FitHistoryViewTest.availabilityAndLatestWorkWhenOnlyTheActiveGenerationHoldsTheImport` löscht `fit_observation_provenance`, erwartet dennoch 70/75 bpm, `available=true`, neueste Messung und ehrlichen Leerzustand im Fenster.
- `FitHistoryViewTest.fitCardIsSeparateAndNamesTheBandClock` prüft getrennte FIT-/Live-Karten und deren Zeitachsen. `HistoryOverviewView`-Fehler-/Leerzustände sind separat getestet.
- PR #539 ist auf `main`; `ScienceFitSeriesAdapter` lässt nur `PROFILE_VALID` und `DIRECT`/`RECONSTRUCTED_TS16` in die verifizierte FIT-Reihe.

Es ist eine Offline-Code-/Testabnahme; bitte keine S10-/S25-Geräteaktion. Ich führe keine GUI-Tests auf Hardware aus.
