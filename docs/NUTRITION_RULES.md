# Ernährung & Energie – quellenbasierte Roadmap

Stand 2026-11-15. Diese Regeln sind **Orientierung**, keine medizinische Ernährungstherapie.

## DGE – Protein
Gesunde, normalgewichtige Erwachsene von 19 bis unter 65 Jahren: 0,8 g Protein pro kg Körpergewicht pro Tag (empfohlene Zufuhr). Ab 65 Jahren: 1,0 g/kg/Tag (Schätzwert). Für Über-/Untergewicht, Schwangerschaft, Stillzeit, Nierenerkrankungen, Verletzungen oder besondere sportliche Belastung **nicht automatisch** anhand des aktuellen Gewichts hochrechnen.
Quelle: https://www.dge.de/wissenschaft/referenzwerte/protein/
Implementiert: `dgeProteinGramsPerDay` liefert bei nicht passender Zielgruppe `null`.

## DGE – Trinken
Allgemeine DGE-Empfehlung: rund 1,5 Liter Getränke täglich, vorzugsweise Wasser oder kalorienfreie Getränke. An heißen Tagen, beim Sport oder bei erhöhten Verlusten kann der Bedarf höher sein. **Kein individueller Algorithmus für Trinkmenge** aus Herzfrequenz oder Schritten ableitbar; bei medizinischer Flüssigkeitsbeschränkung gilt die individuelle ärztliche Vorgabe.
Quelle: https://www.dge.de/gesunde-ernaehrung/gut-essen-und-trinken/dge-ernaehrungskreis/getraenke/
Referenztabellen: https://www.dge.de/wissenschaft/referenzwerte/wasser

## Energie und Aktivität
- Energieaufnahme aus protokollierten Lebensmitteln ist eine Summe dokumentierter Nährwertangaben, nicht aus Wearable-Daten ableitbar.
- Grundumsatz (z. B. Mifflin–St Jeor) und Gesamtbedarf (PAL) sind **Schätzmodelle**. Erst nach Quellenprüfung und Validierung der Eingaben integrieren.
- Kalorienverbrauch aus Herzfrequenz allein ist nicht zuverlässig bestimmt; ohne validierte gerätespezifische Schätzung zeigt PulseLibre **„nicht verfügbar“**.
- Exakte Einheitenumrechnung: 1 kcal = 4,184 kJ. Implementiert und getestet.

## Vorgesehene Oberfläche
- „Ernährung & Energie“: geschätzter Energiebedarf mit Quellen-/Modellangabe, tatsächlich protokollierte Aufnahme, Differenz nur wenn beide verfügbar und vergleichbar.
- „Getränke“: manuell protokollierte Menge und allgemeiner DGE-Hinweis, keine automatische Diagnose.
- „Protein“: DGE-Referenz nur für passende Zielgruppen, Quelle und Einschränkungen anzeigen.
- Alle Daten lokal, keine Cloud. Noch **keine** reale Ernährungseingabe oder Datenspeicherung implementiert.
