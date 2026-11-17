# Wissenschaftliche Quellen und Grenzen

## Schrittfrequenz als Aktivitätsanzeige

Tudor-Locke, C., Ducharme, S. W., Aguiar, E. J. et al. (2020). “Walking cadence
(steps/min) and intensity in 41 to 60-year-old adults: the CADENCE-adults study.”
*International Journal of Behavioral Nutrition and Physical Activity*, 17, 137.
https://doi.org/10.1186/s12966-020-01045-z

Die Studie ermittelte in fünfminütigen Laufband-Bouts bei Erwachsenen von 41–60 Jahren
heuristische Schrittfrequenzwerte, die mit absoluter Geh-Intensität verbunden waren:
100 Schritte/min als Anhaltspunkt für 3 MET und 130 Schritte/min für 6 MET. Das ist
keine Bestätigung, dass eine CIRQA-Schrittzählung jede Aktivität korrekt erkennt, und
100 Schritte/min ist keine universelle Sportgrenze. PulsLibre verwendet den vom Nutzer
gewählten Wert `>100 Schritte/min` nur als **experimentelle Modus-Anzeige** im Dashboard.
Die Ruhegrenze `<10 Schritte/min`, das Fünf-Minuten-Fenster, 30 Sekunden Mindestspanne,
die Drei-Minuten-Gap-/Frischegrenze und die Namen „Ruhe/Bewegung/Sport“ stammen nicht
aus dieser Studie. Sie sind konfigurierbare Produktheuristiken und nicht klinisch validiert.

Implementierung: [`StepActivity.kt`](../app/src/main/java/de/pulselibre/StepActivity.kt).
Die Rate ist Differenz kumulativer Zählerstände geteilt durch verstrichene Zeit; unklare,
veraltete, zurückgesetzte oder lückenhafte Daten ergeben „unbekannt“. Ein unveränderter
Zähler bedeutet ausschließlich, dass im erfassten Fenster keine Schritte beobachtet wurden;
er beweist weder Sitzen noch Schlaf. CIRQA liefert hier empfangene kumulative Schritte und
keine validierte Rohbewegungs-/Beschleunigungsmessung.

## Produktbeispiel: anhaltende Bewegung

Garmin beschreibt Move IQ als Erkennung kontinuierlicher Bewegung; Ereignisse werden bei
mindestens zehn Minuten kontinuierlicher Bewegung markiert:
https://support.garmin.com/en-ZA/?faq=zgFpy8MShkArqAxCug5wC6

Das ist ein Beispiel für zeitliche Glättung in einem kommerziellen Produkt, kein offenes
Algorithmus- oder CIRQA-Verfahren. PulsLibre nutzt ein kürzeres, einstellbares Fünf-Minuten-
Fenster, damit die Dashboard-Anzeige auf den vorhandenen CIRQA-Snapshots zeitnah reagieren
kann. Es zeichnet keine Trainingsaktivität auf und behauptet keine Geräteerkennung.

## Allgemeine Referenzbereiche in Dashboard-Infokarten

Diese Quellen geben kontextgebundene Orientierungsbereiche an. Sie definieren keine
individuellen Diagnosen oder universellen grün/gelb/orange/rot-Grenzen:

- American Heart Association. *Target Heart Rates Chart*. Für die meisten Erwachsenen
  werden 60–100 bpm als Ruhepuls genannt; Training, Medikamente, Stress und andere
  Faktoren beeinflussen den Wert. Quelle: https://www.heart.org/en/healthy-living/exercise-and-physical-activity/fitness-basics/target-heart-rates
- Oxford University Hospitals NHS Foundation Trust. *How to use your pulse oximeter*.
  95–100 % wird als allgemeine Orientierung für durchschnittliche Erwachsene genannt;
  die Hinweise beziehen sich auf ein Finger-Pulsoximeter und ruhige Messung. Das ist
  kein Genauigkeitsnachweis und keine Validierung des CIRQA-Handgelenksensors.
  Quelle: https://www.ouh.nhs.uk/redirecthandler/?id=82415Poximeter&type=patientleaflet
- MedlinePlus. *Rapid shallow breathing*. 12–20 Atemzüge/min für Erwachsene in Ruhe.
  Das ist kein passender Normvergleich für Messungen während Bewegung.
  Quelle: https://medlineplus.gov/ency/article/007198.htm
- World Health Organization. *Physical activity*. Erwachsene: 150–300 min moderat
  oder 75–150 min intensiv pro Woche. CIRQA-Intensitätswerte werden nicht mit diesem
  Wochenziel verglichen, solange ihr gespeicherter Bezugszeitraum unbestätigt ist.
  Quelle: https://www.who.int/initiatives/behealthy/physical-activity

Die Anzeige verwendet deshalb für Prozentwerte und andere begrenzte Zahlen neutrale
Positionsbalken. Ein gedämpfter Marker kann einen publizierten Kontextbereich zeigen;
die Messwertfarbe wird nicht nach „normal/abnormal“ geändert. Die grün-gelb-orange-rote
Tacho-Farbfläche bleibt eine feste Skalenillustration von 40–180 bpm, keine medizinische
Bewertung. Schrittbalken vergleichen ausschließlich mit dem vom CIRQA gelieferten
Tagesziel. Proprietäre Stress-/Body-Battery-Werte haben keine klinische Normalzone.


## SpO₂-Orientierung und nächtliche Auswertung (Issue #39)

Die Vorschlagsgrenzen 95–100 %, 90–94 % und <90 % sind eine vorläufige Produkt-Orientierung, keine durch diese Quellen validierte CIRQA-Grenze. NHS/Oxford University Hospitals beschreibt 95–100 % als typische Orientierung für viele Erwachsene in Ruhe mit einem Finger-Pulsoximeter: https://www.ouh.nhs.uk/redirecthandler/?id=82415Poximeter&type=patientleaflet. BTS (2017), *Guideline for oxygen use in adults in healthcare and emergency settings*, Thorax 72(Suppl 1):ii1–ii90, DOI 10.1136/thoraxjnl-2016-209729, behandelt klinisch verordnete Sauerstofftherapie-Zielbereiche in Versorgungskontexten; diese sind keine Referenzschwellen für gesunde Wearable-Nutzer: https://thorax.bmj.com/content/72/Suppl_1/ii1. NHS-England-COVID-Heimüberwachung betrifft einen akuten COVID-Kontext und ist nicht auf CIRQA-Wearable-Farben übertragbar: https://www.england.nhs.uk/coronavirus/documents/c0445-pulse-oximetry-to-detect-early-deterioration-of-patients-with-covid-19-in-primary-and-community-care-settings-12-january-2021/. FDA nennt Genauigkeitsgrenzen und Einflussfaktoren wie Bewegung, Durchblutung, Hauttemperatur und Pigmentierung: https://www.fda.gov/medical-devices/products-and-medical-procedures/pulse-oximeters.

Für CIRQA liegen dekodierte Service-19-Einzelwerte mit Telefon-Empfangszeit vor, aber kein Signalqualitätskennzeichen, keine bestätigte gerätespezifische Validierung und keine garantierte Samplingfrequenz. Deshalb ist die Farborientierung standardmäßig deaktiviert; die konfigurierbaren 95/90-Grenzen werden nur als Vorschau dargestellt. Es gibt keine Alarm- oder Diagnosefunktion.

ODI3/ODI4, Ereigniszahl und T90 werden nicht berechnet, bis Sampling, Nachtsegmentierung, Zeitbasis, Artefaktfilter, Mindestabdeckung und fachliche Ereignisdefinition validiert sind. Lücken werden nicht interpoliert. Das Dashboard zeigt den Status „nicht berechenbar“, ohne Schlafzeiten oder Desaturationsereignisse zu erfinden. AASM-Regeln hängen von Ausgabe und Kontext ab; maßgeblich ist die aktuelle Ausgabe des Scoring Manuals: https://aasm.org/clinical-resources/scoring-manual/.
