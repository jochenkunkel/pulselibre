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

## Separater Folgeversuch nach Transport-ACK ohne Item-List-Antwort

Die ersten direkten Item-List-Anfragen erhielten einen CRC-geprüften
GFDI-5000-Transport-ACK für 5043 (`requestId=1`, `offset=0`,
`successFlag=0`, `status=0`), aber keine 5044-Item-List-Antwort. Im vierten
bestätigten Lauf wurde deshalb ein einzelner Connect-kompatibler `sync_request`
vor höchstens einer Item-List-Anfrage gesendet. Das Gerät kann den
sync_request als übertragen markieren. Ergebnis: `accepted=true`, eine
CRC-validierte Item-List-Seite mit 45 Einträgen, `pullSent=false`, MLR-Chunks
und -Bytes 0. Der Test stoppte vor UID-Verarbeitung, MLR-Registrierung,
Datei-Pulls, Pagination und Import. Eine unerwartete Pull-Antwort würde
ebenfalls vor MLR/Pull zum Stopp führen. 40 s, kein Retry, gemeinsames
persistentes Limit von 15 bestätigten Läufen; Fehlversuche verbrauchen einen
Lauf. Die UID-Wiederholbarkeit erfordert noch einen separaten no-pull
Vergleichslauf; dies ist kein allgemeiner Datei-Sync.
