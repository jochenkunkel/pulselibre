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
- Das neue persistente Lab-Gate wird vor der Verbindung genau einmal verbraucht;
  es wird nicht zurückgesetzt.

## Interpretation

- `UID+Metadaten unverändert`: gleicher UID-Schlüssel mit identischem
  `data_type`, `data_size` und `flags` in beiden Antworten.
- `Metadaten geändert`: gleicher UID-Schlüssel, aber mindestens eines der
  Metadatenfelder unterscheidet sich.
- `neu` / `fehlend`: UID nur in der zweiten / nur in der ersten Antwort.
- Doppelte UIDs werden separat gezählt und machen die Liste mehrdeutig.
- Eine vorhandene `session_id` wird nur vermerkt; der Test stoppt vor
  Pagination, damit er genau zwei Anfragen sendet.

## Ausstehende Verifikation

Zum Zeitpunkt der Erstellung war kein S25 über ADB verbunden; kein App-Build
wurde installiert und kein Gerätekommando gesendet. Ein späterer, separat
bestätigter zweiter Lab-Lauf in einer neuen Verbindung wäre erforderlich, um
UID-Stabilität über Sitzungen hinweg zu untersuchen. Auch zwei erfolgreiche
Listenläufe beweisen keine vollständige historische Abdeckung.
