# Fehlerbehebung und häufige Fragen

Alle Hinweise beschreiben das tatsächliche Verhalten der aktuellen App. Wo etwas noch nicht geprüft ist, steht das dabei.

## Ich sehe „Keine Messdaten“
- Das heißt: für diese Kachel liegt **kein gültiger Wert** vor. Die App zeigt nie eine 0 oder einen geschätzten Wert an dessen Stelle.
- Prüfe unter **Gerät** den **letzten Empfang je Messgröße**. Steht dort „vor mehreren Stunden“, kam seitdem nichts vom Band an.

## Unter „Gerät“ steht „GATT verbunden (Aufzeichnung aus)“
- Das Telefon hat eine Verbindung zum Band, die **Aufzeichnung** ist aber nicht gestartet. Mit **Starten** im Bereich Aufzeichnung beginnt der Dienst, Werte zu speichern (Android zeigt dann eine dauerhafte Benachrichtigung mit Stopp-Taste).
- Optional: In den Einstellungen „Beim App-Start automatisch aufzeichnen“ einschalten. Das funktioniert nur, wenn Bluetooth an ist und die Berechtigungen schon erteilt sind; die App schaltet Bluetooth nie selbst ein.

## „Bluetooth-Berechtigung fehlt“
- Erlaube in den Android-App-Einstellungen **Geräte in der Nähe** (und Benachrichtigungen). Ohne sie kann die App keine Verbindung aufbauen.

## Der Verlauf oder „Tag im Überblick“ zeigt Lücken
- Lücken sind **gewollt sichtbar**: Zeiten ohne Messwerte werden schraffiert und mit „keine Daten“ beschriftet. Die App verbindet sie nicht und schätzt nichts.
- Typische Ursachen: Aufzeichnung war aus, Band außer Reichweite, Bluetooth aus. Fehlende Tage zählen in Mittelwerten nie als 0.

## Warum zeigt die Analyse keinen Trend?
- Für Trends braucht es genug Tage mit Daten (7 T mindestens 4, 28 T mindestens 14, 90 T mindestens 45). Bis dahin zeigt die App nur die Tageswerte und einen Hinweis, keinen Vergleich. Unter **Datenabdeckung** steht, wie viele Tage fehlen.

## Warum gibt es keinen Schlaf-Score, keine Schlafphasen, keine HRV?
- Diese Werte sind aus den bisher gelesenen Gerätedaten **nicht belegt**. PulsLibre schätzt sie nicht und ahmt keine proprietären Scores nach. Die Module stehen in **Analysen** als „Noch nicht verfügbar“ mit Begründung.

## Kalorien, Stress, Intensitätsminuten oder Body Battery wirken seltsam
- Diese Gerätewerte werden angezeigt, wenn das Band sie sendet; **Einheiten und Messbedingungen sind nicht geprüft**. Sie gelten nicht als gesicherte Messung.

## Eine Kachel ist verschwunden
- Vermutlich ausgeblendet: Ganz unten auf der Übersicht **Startseite anpassen** öffnen und die Kachel wieder einblenden, oder **Zurücksetzen** wählen. Ausgeblendete Kacheln werden weiter aufgezeichnet.

## Wo liegen meine Daten, kann ich sie sichern?
- Nur auf deinem Telefon. Unter **Daten** gibt es Export (CSV) und Sicherung der lokalen Datenbank. Eine Wiederherstellung verlangt eine ausdrückliche Bestätigung. Beim **Deinstallieren der App werden die lokalen Daten gelöscht**; vorher exportieren oder sichern.

## Verbraucht die Aufzeichnung viel Akku?
- Dauerhafte Aufzeichnung kostet Strom auf Telefon und Band. Eine belastbare Messung dazu steht noch aus (Ticket #235); bis dahin gibt es keine Zahlen.

## Weiter Hilfe
- Fehler melden oder nachlesen: [GitHub-Issues des Projekts](https://github.com/jochenkunkel/pulselibre/issues). Bitte **keine** privaten Gesundheitsdaten, Screenshots mit echten Werten oder Gerätekennungen anhängen.
