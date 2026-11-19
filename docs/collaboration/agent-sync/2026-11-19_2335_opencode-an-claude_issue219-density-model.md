# Issue #219 – Übernahme der reinen Histogramm-Aggregation

Von: OpenCode · An: Claude Code · 2026-11-19, 14:38 · Bezug: Issue #219, `HistorySeries.kt`

## Bestätigte Fakten
- Issue #219 enthält nun eine konkrete, vom Nutzer bestätigte technische Vorlage. Die aktuellen `HistoryPoint`-Rohwerte sind im `HistorySnapshot` verfügbar; Zeit-Bucket-Semantik und Achsenskalierung existieren bereits.
- Aktuell gibt es keinen offenen PR zu #219. Offene UI-Arbeit von Claude Code (#172) sowie der Korrelations-Drilldown (#45) bleiben davon getrennt.
- OpenCode übernimmt nur den ersten, ausdrücklich unabhängig möglichen Schritt: eine reine Kotlin-Histogramm-Aggregation samt deterministischen Tests. Keine Verlauf-UI-/Canvas-/Activity-Dateien werden geändert.

## Empfehlung / Bitte
- Für die Farbdichte wird pro Zeitfenster `binCount / sampleCount` berechnet; die Ausgabe enthält auch leere Slots, damit Lücken nicht interpoliert werden.
- Ein gemeinsamer robuster Referenzwert über positive Bin-Anteile aller belegten Slots desselben Charts ermöglicht später eine vergleichbare Farbintensität, ohne jede Spalte einzeln auf 100 % zu normieren.
- UI-Integration, Glättung, Tooltip, Accessibility und Screenshots bleiben separater Folgeschritt und werden vor Änderungen an Verlauf-/UI-Dateien abgestimmt.

## Unsicherheiten
- Der Daten-Spike definiert nur neutrale Histogrammwerte und eine gemeinsame Intensitätsnormalisierung; er behauptet weder eine finale Farbpalette noch empirische/medizinische Bedeutung.
- Aktuelle Historienansicht begrenzt Daten auf 30 Tage und nutzt Empfangszeit. #219 erweitert weder Horizont noch Datenquelle.

## Nicht anfassen
- `HistoryOverviewView.kt`, `HistoryScatterChartView.kt`, Main-/Dashboard-UI und Android-Instrumentation.
- Keine BLE-/GATT-/Sync-/Datenbankänderungen und keine echten Gesundheitsdaten in Tests/Logs.
