# CIRQA `nextTransaction`-Folgeprobe – S25-Ergebnis (2026-11-21)

## Ergebnis

Der begrenzte Einmaltest am Galaxy S25 erhielt zunächst eine Item-List mit
28 Einträgen. Die Antwort hatte `session=false` und `nextTransaction=true`.
Die Probe stellte daraufhin genau eine separate Item-List-Anfrage mit dem
zurückgegebenen `transaction_id` (ohne `session_id`).

Die Folgeantwort war erfolgreich (`status=0`), enthielt **0 Einträge** und
meldete erneut `session=false`, `nextTransaction=true`. Die Probe stoppte nach
dieser einen Folgeantwort. Es wurden keine weiteren Transaktionen angefragt.

Damit ist am Gerät belegt, dass `nextTransaction=true` vom
`session_id`-Seiten-Cursor zu unterscheiden ist und als Kontext für eine
separate Transaktionsanfrage verwendet werden kann. **Nicht belegt** ist, dass
die Folgeanfrage in diesem Lauf eine weitere befüllte Liste oder abrufbare
Dateien liefert. Die Antwort mit null Einträgen ist keine Evidenz für
zusätzliche Historien-Dateien.

## Sicherheits- und Datenschutzumfang

- Ein explizit bestätigter Lauf; separates Einmal-Gate danach verbraucht
  (`1/1`).
- Ein `sync_request`, eine initiale Item-List, genau eine
  `transaction_id`-Folgeanfrage.
- `pullSent=false`; keine Datei-Pulls, MLR-Datei-Registrierung, Speicherung
  oder Import. MLR-Transfer: 0 Chunks / 0 Bytes.
- Keine UIDs, Transaktions-ID, Rohframes oder Gesundheitsdaten protokolliert.
- Der Nutzer hatte ausdrücklich bestätigt, dass ein mögliches
  geräteseitiges Markieren gepufferter Daten als übertragen akzeptabel ist.
- Kein Reset des bestehenden 15/15-Item-List-Gates.

## Evidenzgrenzen

Das ist ein einzelner Lauf mit dem damaligen Geräte-/Archivzustand. Er klärt
nicht, warum die Folgeantwort leer war, ob eine spätere Transaktion weitere
Einträge liefert oder ob `nextTransaction=true` immer so reagiert. Kein
automatisches Fortsetzen/Download daraus ableiten.

## Dritter, ausdrücklich freigegebener Lauf (Build 1710)

Nach dem separaten Gate-Anstieg von `2` auf `3` wurde am Galaxy S25 genau
ein weiterer Lauf manuell bestätigt. Vor Ausführung zeigte das persistente
Gate `2/3`; danach steht es auf `3/3`. Das signaturgleiche Main-CI-Update
Build 1710 wurde in-place über Build 1701 installiert.

Der Lauf erhielt eine initiale Item-List mit 35 Einträgen:
`session=false`, `nextTransaction=true`. Es folgte genau eine separate
Anfrage mit dem zurückgegebenen `transaction_id`. Die Folgeantwort meldete
`status=0`, 0 Einträge, erneut `session=false` und `nextTransaction=true`.
Danach stoppte die Probe.

Sicherheitsumfang: ein `sync_request`, zwei Item-List-Antwortseiten
(initial plus genau eine Folgeantwort), `pullSent=false`, 0 MLR-Chunks/Bytes,
0 Transfers, Verifikationen, private FIT-Speicherungen oder Importe.
Keine Retry-/Paging-Kette oder weitere Transaktion. Das bestehende
15/15-Item-List-Gate wurde nicht verändert. Der Sync-Request kann dennoch
gepufferte Gerätedaten geräteseitig als übertragen markieren; der Lauf ist
deshalb nicht als vollständig zustandsneutral zu verstehen.

Der dritte Versuch bestätigt erneut, dass `nextTransaction=true` eine
separate Folgeanfrage auslösen kann; er lieferte auch diesmal keine weiteren
Listeneinträge. Daraus folgt weder zusätzliche Historienabdeckung noch eine
Erlaubnis zum automatischen Fortsetzen. Kein vierter Versuch freigegeben.
