# BMI-Einordnung (Issue #118) und Alter aus Geburtsdatum (Issue #117)

**Formel:** BMI = Gewicht (kg) / Größe (m)². Plausible Eingaben: Gewicht 15–400 kg, Größe 50–250 cm; sonst Platzhalter statt Wert.

**Einordnung (nur Erwachsene ab 20 Jahren, WHO, geschlechtsunabhängig)**

| BMI (ungerundet) | Kategorie | Farbe |
|---|---|---|
| < 18,5 | Untergewicht | gelb |
| 18,5 – < 25,0 | Normalgewicht | grün |
| 25,0 – < 30,0 | Präadipositas | gelb |
| 30,0 – < 35,0 | Adipositas Grad I | rot |
| 35,0 – < 40,0 | Adipositas Grad II | rot |
| ≥ 40,0 | Adipositas Grad III | rot |

Die Grenzen werden auf den **ungerundeten** Wert angewandt; angezeigt wird eine Nachkommastelle. Die Kategorie steht immer als **Text** (Farbe nie einziges Signal) und im Screenreader-Text.

**Quelle:** WHO, *Obesity: preventing and managing the global epidemic*, WHO Technical Report Series 894 (2000); WHO-Europa-BMI-Klassifikation für Erwachsene. Stand der Implementierung 2026-11-18; Wortlaut der Quelle vor einer medizinischen Nutzung gegen das Original prüfen.

**Nicht umgesetzt / bewusst nicht behauptet:**
- Unter 20 Jahren: **keine** Bewertung („Für diese Altersgruppe keine BMI-Bewertung verfügbar“); WHO-BMI-für-Alter-Referenzen (Kinder/Jugendliche, mit Alter und Geschlecht) sind nicht implementiert.
- Ältere Erwachsene: keine Sondergrenzen; Hinweis auf Grenzen des BMI im Info-Dialog.
- Geschlecht wird für die Einordnung nicht verwendet. Der BMI ist ein Screening-Indikator, keine Diagnose (Muskelmasse, Körperbau, Schwangerschaft).
- Ohne Geburtsdatum (nur altes gespeichertes Alter) wird das gespeicherte Alter für die Einordnung verwendet, wenn es ≥ 20 ist; ohne jedes Alter wird der Wert angezeigt, aber nicht eingeordnet.

**Alter (#117):** Geburtsdatum lokal als ISO-Datum (`birth_date`); Alter wird für das lokale Datum berechnet (vor dem Geburtstag eins weniger; 29. Februar: ab 1. März im Nichtschaltjahr). Alte Einträge `age_years`/`birth_year` bleiben unverändert erhalten, werden aber nur als Rückfall genutzt; ein Geburtsdatum wird **nicht** daraus erfunden.
