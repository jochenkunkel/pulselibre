# Schritt-Tagesverlauf als Kachelhintergrund (Issue #122)

**Eingabe:** vorhandene, lokal gespeicherte Ereignisse der Tagesschritte (Service 7, Feld `steps`, kumulativer Tageszähler) mit Empfangszeitstempel. Keine neue Bluetooth-Abfrage, keine erfundene Reihe.

**Aggregation (`StepIntervals`):**
1. Beobachtungen des lokalen Tages sortieren, Duplikate entfernen.
2. Zwischen zwei **aufeinanderfolgenden** Beobachtungen wird die Differenz als Schritte im Zeitraum dazwischen gewertet – nur wenn der Abstand ≤ 10 min ist (sonst ist unbekannt, *wann* die Schritte entstanden; Lücke).
3. Negative Differenzen (Zähler-Reset) werden übersprungen.
4. Die Differenz wird zeitproportional auf die überdeckten 15-Minuten-Intervalle verteilt (kein künstliches Zuschlagen zu einem Intervall).
5. Längere Pausen (10 min–12 h) mit Zuwachs werden gleichmäßig über die Pause verteilt, aber nur als **helle Umrissbalken („ungefähr, Zeitpunkt unbekannt“)** gezeichnet und zählen nie als gemessen. **Intervalle ohne Beobachtung bleiben `null` (Lücke) und werden nicht als 0 Schritte gezeichnet**; 0 Schritte gibt es nur bei bestätigt unverändertem Zähler.

**Intervalle pro Tag:** 96 (24 h), 92 (23 h Sommerzeit), 100 (25 h) – tatsächliche lokale Tagesdauer.

**Darstellung (`StepBarsView`):** schmale, abgerundete Säulen in Akzentfarbe, ≈ 16 % Deckkraft, hinter allen Texten der Schrittekachel, an der Kartenrundung abgeschnitten; Höhe = Schritte im Intervall relativ zum Tagesmaximum; keine Achsen/Zahlen. Ohne beobachtete Intervalle keine Balken und der Hinweis „Tagesverlauf noch nicht verfügbar“. Screenreader: „Aktivitätsverlauf in 15-Minuten-Intervallen, teilweise verfügbar (x von y Intervallen)“.

**Einschränkungen:** Zeitpunkt = Empfangszeit auf dem Telefon, nicht Messzeit des Geräts; die Summe der Balken kann kleiner als der Tageszähler sein (Lücken, Zeiten ohne Empfang). Tageswert und Zielfortschritt sind unverändert.
