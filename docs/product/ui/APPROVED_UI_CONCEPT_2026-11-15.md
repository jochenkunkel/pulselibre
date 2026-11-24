# PulseLibre – freigegebenes UI-Konzept (15.11.2026)

## Entscheidung
Der Nutzer hat den **zweiten, erweiterten Vier-Screens-Entwurf** ausdrücklich positiv bewertet und als visuelle Zielrichtung bestätigt. Der frühere Entwurf mit fast ausschließlich Puls war unvollständig. Dieser Entwurf ist **Designreferenz, keine Darstellung bereits implementierter Funktionen**.

## Screens aus dem Entwurf
1. **Übersicht:** PulseLibre-Titel, Verbindungsstatus, zwei nebeneinander liegende Kacheln für Puls und Sauerstoff (SpO₂), eine breite Schlafkachel mit Dauer und farblicher Phasen-Zeitleiste, Abschnitt „Heute auf einen Blick“ mit Schritten, Aktivkalorien und Aktivzeit, kompakte breite Nacht-Insights-Kachel mit SpO₂-Abdeckung/Analyseverfügbarkeit, letzte Synchronisierung, Navigation. Nacht-Insights zeigen Kennzahlen nur bei nachgewiesener Datenqualität; keine interpolierten oder diagnostischen Schlaf-/Desaturationswerte.
2. **Verlauf:** Tag/Woche/Monat, Datumsnavigation, getrennte Puls- und SpO₂-Liniendiagramme, Schlafphasen-Zeitleiste. Diagramme nur mit tatsächlich erfassten Messungen; keine interpolierten Werte als Rohdaten ausgeben.
3. **Schlafdetails:** Dauer, Schlafphasen mit Dauer/Anteilen, Einschlaf- und Aufwachzeit sowie Zeit im Bett, **nur falls verlässlich vom Gerät bereitgestellt**.
4. **Datenexport:** von/bis einschließlich, Beschreibung, Auswahl der Messkategorien als **späteres Designziel**, Hinweis auf ausschließlich lokale Daten, CSV speichern. Aktuell implementiert ist ein vollständiger Export aller lokal vorhandenen Messgrößen im ausgewählten Zeitraum, **ohne Kategorie-Checkboxen**.

## Visuelle Sprache
- Der genehmigte Bildentwurf nutzt **dunkelblauen Kopfbereich**, weiße Flächen, abgerundete Karten, blaue Diagramme, rot/rosa Puls und violett-blaue Schlafphasen.
- Die ursprüngliche PulseLibre-Markenfarbe **#5127C9 (Violett)** bleibt als Leitfarbe für die spätere Implementierung vorgesehen. Die Bildgestaltung ist keine verbindliche Änderung der bestehenden Markenrichtlinie.
- Android-orientierte, gut lesbare Beschriftungen; zugängliche Kontraste und große Touch-Flächen.
- Informationshierarchie: Überblick zuerst, Details und Export über klare Navigation.

## Datenintegrität, Datenschutz und Sicherheitsregeln
- Alle Zahlen im Mockup (beispielsweise 72 bpm, 98 %, 7 h 42 min, 5.842 Schritte, 326 kcal) sind **rein illustrative Beispieldaten**; sie dürfen weder im realen Dashboard als Messung angezeigt noch als Testdaten in produktive Datensätze geschrieben werden.
- Bei fehlenden Daten: „Keine Messdaten“ oder „Noch nicht verfügbar“, mit letzter verlässlicher Messzeit, wenn bekannt.
- Aktivkalorien nur bei fachlich begründeter und transparenter Datenquelle/Berechnung, sonst ausblenden.
- SpO₂- und Schlafphasen-Anzeigen setzen entsprechende bestätigte Geräteinformationen voraus. Garmin-CIRQA-Protokoll ist nicht verifiziert; keine erfundene Gerätekompatibilität.
- Offline-first, kein INTERNET-Permission, keine Cloud, kein Eingriff in Garmin Connect und keine Neu-Kopplung.
- CSV exportiert nur lokal gespeicherte Originalmessungen; sensible Gesundheitsdaten beim Teilen schützen.

## Implementierungsstand und nächste Schritte
- Vorhanden: sechs Dashboard-Karten als Platzhalter, BLE-Erkundung, SQLite-Lokalspeicherung, Exportansicht mit Zeitraum und Android-Dateidialog, CSV-Serializer und automatisierte Tests.
- Noch offen: produktive Kachel-Anordnung entsprechend dem Bild, echte Diagramme, Navigation, Schlafdetails, Geräteprotokoll/Dekodierung, zuverlässige historische Synchronisierung und Messkategorie-Filter im Export.
- Vor Implementierung neue UI-Tests für fehlende Messwerte, Navigationswege, Anzeige nur valider Messungen und Diagramm-Zeitzonen ergänzen.

## Referenzbild
Der zweite Vier-Telefone-Entwurf wurde im Chat generiert und dort gezeigt. **Die generierte PNG-Datei ist nicht Bestandteil dieses Git-Commits**; diese Textspezifikation hält die freigegebene Designentscheidung reproduzierbar fest. Eine binäre Bilddatei kann nachgereicht werden.
