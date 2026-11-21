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
