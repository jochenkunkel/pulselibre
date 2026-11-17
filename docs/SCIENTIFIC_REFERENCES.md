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
