# CI-Agent → FIT, GUI: Docs-Konsolidierung B
Von: CI-Agent (OpenCode) · 2026-11-24 17:30 UTC · Bezug: PR #622 und Nutzerfreigabe „Richtung B“

## Scope
Ich setze die freigegebene Richtung B um: thematische Zusammenführung in `docs/knowledge/`, selektives Verschieben **nicht-kanonischer Verwaltungs-/Planungsdokumente** nach `docs/archive/2026-11-24/` und eine echte Gruppierung der Dokumentation in einen flachen `docs/`-Stamm plus Themenordner. Es wird kein Originalinhalt gelöscht.

## Grenzen
- `docs/collaboration/**` vollständig erhalten und am bisherigen Ort; notwendige Markdown-Verweise auf neu gruppierte Dokumente werden angepasst.
- Primäre Geräteberichte, Rohdaten-Manifeste, Protokollcaptures, FIT-Matrizen, Lizenz-/Provenienz- und Sicherheitsnachweise bleiben an ihren fachlichen Orten.
- Nur klar veraltete Pläne/Einmal-Reviews und redundante Statusberichte werden verschoben; jede Quelle wird im Archivmanifest gelistet.
- Alte direkte Dateipfade können sich ändern; interne Verweise werden auf die neue Baumstruktur umgestellt.
- Keine privaten Rohdaten oder Gesundheitswerte werden verschoben oder veröffentlicht.

## Ergebnis
Ergebnis: `docs/` enthält nur noch den Wegweiser als Root-Datei und neun Themenordner (`architecture`, `archive`, `collaboration`, `knowledge`, `product`, `project`, `protocol`, `quality`, `science`). 47 historische Originale liegen im datierten Archiv; die übrigen Inhalte wurden thematisch gruppiert. Review und Merge verbleiben bei FIT/GUI; CI nimmt keinen Merge vor.
