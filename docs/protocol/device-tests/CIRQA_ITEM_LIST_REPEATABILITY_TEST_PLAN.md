# CIRQA Item-List-Wiederholbarkeit – Lab-Testplan

## Zweck

Zwei direkt aufeinanderfolgende FileAccess-Item-List-Anfragen innerhalb derselben
BLE-Sitzung vergleichen. Das prüft, ob die gelesenen Item-UIDs und deren
Metadaten bei einer wiederholten Anfrage stabil erscheinen. Es prüft **nicht**
die Stabilität über getrennte Sitzungen, die Vollständigkeit des Gerätearchivs
oder die Semantik der UIDs bei Retention/Dateiersetzung.

## Ablauf in PulsLibre Testlabor

1. S25 mit dem CIRQA verbunden lassen; sicherstellen, dass kein anderer BLE-
   Test oder Recorder aktiv ist.
2. Im Testlabor **„Item-List zweimal vergleichen (kein Pull)“** wählen.
3. Bestätigungsdialog und Grenzen prüfen; nur bei eindeutigem CIRQA-Ziel starten.
4. Der Test führt frisches Bootstrap/MLR-Setup aus und sendet exakt zwei
   read-only `FileAccess item_list_request`-Anfragen. 40 Sekunden Gesamtbudget,
   kein Retry.
5. Den abschließenden Status **„Item-List-Vergleichsstatus“** sichern.

## Sicherheits- und Datenschutzgrenzen

- Dieser Test sendet **keinen** `sync_request`, `pull_item`, Datei-Pull,
  Archiv-/Flag-/Lösch-/Reset- oder `CLOSE_ALL`-Befehl.
- Es werden keine Datei-Bytes angefordert oder gespeichert.
- UID-Bytes verbleiben nur flüchtig im Arbeitsspeicher. Verglichen werden
  interne SHA-256-Schlüssel; Status und Bericht enthalten ausschließlich
  Counts, keine UIDs, Hashes, Gerätedaten oder Datei-/Messwerte.
- Ein GFDI-5000-Frame zu 5043 wird als Transport-ACK getrennt von einer
  FileAccess-5044-Antwort ausgewiesen. Ein ACK belegt allein keine Annahme der
  Item-List-Anfrage durch die Geräteanwendung.
- Das persistente S25-Live-Test-Gate erlaubt insgesamt höchstens 15 ausdrücklich
  bestätigte Läufe (direkte Item-List- und no-pull-Sync-Diagnose zusammen) und
  wird vor jeder Verbindung atomar fortgeschrieben. Fehlversuche verbrauchen
  ebenfalls einen Versuch; es gibt keinen automatischen Retry oder Gate-Reset.

## Interpretation

- `UID+Metadaten unverändert`: gleicher UID-Schlüssel mit identischem
  `data_type`, `data_size` und `flags` in beiden Antworten.
- `Metadaten geändert`: gleicher UID-Schlüssel, aber mindestens eines der
  Metadatenfelder unterscheidet sich.
- `neu` / `fehlend`: UID nur in der zweiten / nur in der ersten Antwort.
- Doppelte UIDs werden separat gezählt und machen die Liste mehrdeutig.
- Eine vorhandene `session_id` wird nur vermerkt; der Test stoppt vor
  Pagination, damit er genau zwei Anfragen sendet.

## Bisherige S25-Ergebnisse

Drei separat bestätigte direkte Läufe erhielten jeweils ein CRC-validiertes
GFDI-5000-Transport-ACK zu 5043, aber keine FileAccess-5044-Item-List-Antwort.
Daher gab es keine Listen zum Vergleichen und keine Aussage zur UID-Stabilität.
Der vierte Lauf verwendete den getrennten Sync-Request-Pfad und erhielt eine
Item-List mit 45 Einträgen (eine Seite; keine session_id, nextTransactionId
vorhanden). Der Lauf stoppte vor MLR/Pull/Import. UID-Wiederholbarkeit bleibt
ungeprüft; auch ein erfolgreicher Listenlauf beweist keine vollständige
historische Abdeckung.

Der fünfte Lauf verwendete den ausdrücklich bestätigten Zwei-Listen-Modus:
beide Antworten enthielten 23 Einträge; 23 UID-/Metadatenpaare waren
unverändert, 0 Metadaten geändert, 0 neu/fehlend und 0 doppelte UIDs. Es gab
keine MLR-Chunks/-Bytes, Datei-Pulls oder Importe. Das belegt
Same-session-Wiederholbarkeit nur für diese beiden Antworten. Da der vierte
Lauf 45 Einträge lieferte, bleibt die Abweichung zwischen Listenläufen zu
untersuchen; UID-Stabilität über Sitzungen und Archivvollständigkeit sind
weiterhin unbewiesen.

Der sechste Lauf wurde nach einer zwischenzeitlichen Garmin-Connect-Synchroni-
sation erneut als Zwei-Listen-No-Pull-Test ausgeführt. Ergebnis wieder 23/23,
alle UID-/Metadatenpaare unverändert, 0 geändert/neu/fehlend, keine doppelten
UIDs; keine MLR-Chunks/-Bytes, Datei-Pulls oder Importe. Das passt zu der
Vermutung, dass die zwischenzeitliche Garmin-Connect-Synchronisation den
vorherigen 45-vs-23-Unterschied beeinflusst haben könnte, beweist aber keinen
Kausalzusammenhang. Gemeinsamer Zähler: 6/15.

Der siebte Lauf folgte auf den Hinweis, dass möglicherweise vor dem vierten
Lauf der Aktionsknopf des Armbands gedrückt worden war; Garmin Connect habe
daraufhin eine Meldung angezeigt und die Funktion deaktiviert. Der neue
Zwei-Listen-No-Pull-Vergleich ergab erneut 23/23 identische UID-/Metadatenpaare,
0 geändert/neu/fehlend, keine doppelten UIDs und keine MLR-Chunks/-Bytes,
Datei-Pulls oder Importe. Ein Einfluss der Knopfaktion ist plausibel, aber
unverifiziert. Gemeinsamer Zähler: 7/15.

Der achte Versuch endete vor dem `sync_request`: Das Gerät lehnte die
GFDI-Registrierung ab; es wurden keine Item-Lists empfangen. Der Versuch wurde
trotzdem verbraucht. Beim anschließenden Knopftest startete die CIRQA die
eingestellte „Aktivität mit Zeitangabe“. Die Aktivität wurde kurz in Garmin
Connect gespeichert. Der neunte PulseLibre-Lauf erhielt danach zwei Listen
mit je 24 Einträgen; alle 24 UID-/Metadatenpaare waren identisch, 0 geändert,
neu, fehlend oder doppelt. MLR-Chunks/-Bytes, Datei-Pulls und Importe: 0.
Der Anstieg von 23 auf 24 passt zur gespeicherten Aktivität, ist ohne
Dateiabruf/UID-Zuordnung aber kein Beweis. Gemeinsamer Zähler: 9/15.

Der zehnte Lauf wiederholte den Zwei-Listen-No-Pull-Test: erneut 24 Einträge
je Liste und 24 unveränderte UID-/Metadatenpaare; 0 geändert, neu, fehlend
oder doppelt. Es gab keine MLR-Chunks/-Bytes, Datei-Pulls oder Importe.
Gemeinsamer Zähler: 10/15.

Für Lauf elf drückte der Nutzer unmittelbar davor erneut den Bandknopf. Beide
Listen enthielten weiterhin 24 Einträge und dieselben 24 UID-/Metadatenpaare;
es gab 0 geändert, neu, fehlend oder doppelt. MLR-Chunks/-Bytes, Datei-Pulls
und Importe: 0. In diesem Lauf wurde kein weiterer Listeneintrag sichtbar;
das belegt nicht, ob das Band intern eine Aktion gespeichert hat.
Gemeinsamer Zähler: 11/15.

Im Anschluss wurde die während des vorigen Tests laufende Garmin-Connect-
Aktivität (4:10) gestoppt und gespeichert; Connect zeigte sie danach in
„Aktivitäten von heute“. Der zwölfte Zwei-Listen-No-Pull-Lauf lieferte
26 Einträge je Liste und 26 unveränderte UID-/Metadatenpaare, 0 geändert,
neu, fehlend oder doppelt. Gegenüber dem vorherigen 24er-Bestand sind zwei
Einträge hinzugekommen. Der zeitliche Zusammenhang mit dem Speichern ist
auffällig, aber ohne UID-Zuordnung oder Dateiabruf ist nicht belegt, dass
beide Einträge dieser Aktivität zuzuordnen sind. MLR-Chunks/-Bytes, Pulls und
Importe: 0. Gemeinsamer Zähler: 12/15.

## Separater Folgeversuch nach Transport-ACK ohne Item-List-Antwort

Die ersten direkten Item-List-Anfragen erhielten einen CRC-geprüften
GFDI-5000-Transport-ACK für 5043 (`requestId=1`, `offset=0`,
`successFlag=0`, `status=0`), aber keine 5044-Item-List-Antwort. Im vierten
bestätigten Lauf wurde deshalb ein einzelner Connect-kompatibler `sync_request`
vor einer Item-List-Anfrage gesendet. Das Gerät kann den `sync_request`
geräteseitig als übertragen markieren. Ergebnis: eine CRC-validierte
Item-List-Seite mit 45 Einträgen, `pullSent=false`, MLR-Chunks/-Bytes 0. Der
Lauf stoppte vor MLR/Pull/Import. 40 s, kein Retry, gemeinsames persistentes
Limit von 15 bestätigten Läufen; Fehlversuche verbrauchen einen Lauf.

Der separat bestätigte no-pull-Modus fordert nach einem
`sync_request` genau zwei Item-Lists in derselben BLE-Sitzung an und vergleicht
UID-Schlüssel sowie Metadaten nur flüchtig im Speicher. Angezeigt/gespeichert
werden ausschließlich Counts. Der Modus stoppt nach der zweiten Antwort vor
MLR-Registrierung, Datei-Pull, Pagination und Import; das mögliche
geräteseitige Markieren durch `sync_request` wird vorher offengelegt. Der
fünfte und sechste Lauf ergaben jeweils 23/23 identische Einträge pro Sitzung.
Der Vergleich testet nur Same-session-Wiederholbarkeit, nicht UID-Stabilität
über Sitzungen oder Archivvollständigkeit.
