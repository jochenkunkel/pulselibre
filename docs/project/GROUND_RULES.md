# Ground Rules – evidenzbasierte Orientierung (Entwurf)

Stand: 2026-11-15. Nur veröffentlichte Referenzen, keine erfundenen altersabhängigen Puls-, Schritt- oder SpO2-Sollwerte.

## Nutzerprofil (geplant)
Geburtsdatum/Alter, Körpergröße (cm), Gewicht (kg), optional Geschlecht für später **nur** quellenbegründete geschlechtsspezifische Regeln. Optional individuelle medizinisch vereinbarte Ziele; keine Diagnosen oder sensiblen medizinischen Daten als Pflichtfeld. Daten lokal speichern, ohne Cloud und ohne Internetberechtigung.

## WHO – Bewegung
Für Erwachsene 18–64 und ab 65 Jahren: 150–300 Minuten moderate oder 75–150 Minuten intensive aerobe Aktivität pro Woche bzw. gleichwertige Kombination; muskelkräftigende Aktivitäten mindestens zwei Tage/Woche. Ab 65 zusätzlich funktionelles Gleichgewichts- und Krafttraining mindestens drei Tage/Woche. Quelle: WHO 2020 https://www.who.int/europe/news-room/fact-sheets/item/physical-activity und https://www.ncbi.nlm.nih.gov/sites/books/NBK566046/

**Wichtig:** Schritte allein können die Aktivitätsintensität nicht zuverlässig belegen. Kein pauschales WHO-Ziel von 10.000 Schritten erfinden. Empfehlungen müssen bei Krankheit, Verletzung oder Rehabilitation individualisiert werden.

## WHO – BMI für Erwachsene
BMI = Gewicht in kg / (Körpergröße in m)^2.
WHO-Kategorien: unter 18,5 Untergewicht; 18,5 bis unter 25 Normalbereich; 25 bis unter 30 Übergewicht; ab 30 Adipositas. Quelle: https://apps.who.int/nutrition/landscape/help.aspx?helpid=420&menu=0
Nur für Erwachsene; BMI ist ein Screening-Indikator, keine Diagnose oder individuelle Idealgewichtsvorgabe. Nicht als pauschales Ziel für Kinder, Schwangere oder Sonderpopulationen verwenden.

## SpO2 – Diagramm, keine Diagnose
Die FDA nennt 95–100 % als übliche Werte bei vielen gesunden Personen; Höhe, Grunderkrankungen und Messunsicherheit beeinflussen die Einordnung. Quelle: https://www.fda.gov/consumers/consumer-updates/pulse-oximeter-basics
Handgelenk-Wearables sind nicht automatisch medizinisch validierte Pulsoximeter. Ohne gerätespezifische Validierung keine farbigen Alarm-/Diagnoseschwellen oder altersabhängigen Sollwerte. Zeige Trend, Zeitstempel, Messqualität soweit verfügbar und Herkunft der Daten; keine Daten erfinden. Diese Regeln betreffen Aussage und Darstellung: Rohwerte sind nach der P0-Entscheidung 24.11.2026 (`docs/protocol/DATA_ACQUISITION_PRIORITY.md`) auch ohne Validierung lokal zu erfassen und als ungeprüft zu markieren.
Bei Beschwerden oder auffälligen Messwerten ärztlichen Rat einholen; bei schweren akuten Symptomen medizinische Notfallhilfe.

## Technische Abnahmekriterien
1. Alle Regeln haben Quellen-URL, Stand, Zielgruppe, Einheit und Einschränkungen.
2. Fehlende Profileingaben oder Messdaten führen zu „nicht verfügbar“, niemals zu synthetischen echten Werten.
3. Mathematische Funktionen sind offline, deterministisch und unit-getestet.
4. Keine medizinische Diagnose, keine automatische Therapieempfehlung.
5. SpO2-Plot nur bei echten, validiert importierten oder klar markierten Demo-Daten.
