# CIRQA `nextTransaction`-Folgeprobe – S25-Ergebnis (2026-11-21)

## Erster Lauf

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

## Ein zusätzlich freigegebener Lauf

Nach ausdrücklicher Nutzerfreigabe wurde das separate Gate von `1` auf genau
`2` Gesamtversuche erhöht; der vorhandene Zähler wurde nicht zurückgesetzt. Das
gemeinsame Item-List-Gate (`15/15`) blieb unverändert. PR #378 wurde nach
Full-CI und Dependency Review gemergt (`b4e0ba201c8938256f432802e3700d7633685114`);
die Full-CI auf diesem Main-Commit war erfolgreich (Run `44920440011`).

Die signierte Main-APK hatte dieselbe Zertifikat-SHA-256 wie die installierte
Version; ein datenbewahrendes Update erhöhte die App-Version von `1667` auf
`1680`. Der Nutzer bestätigte die exklusive S25-Nutzung für diesen einzelnen
Zusatzversuch.

Der zweite Lauf erhielt zunächst **35 Einträge** mit `session=false` und
`nextTransaction=true`. Genau eine separate Item-List-Anfrage mit der
zurückgegebenen `transaction_id` folgte (ohne `session_id`). Ihre Antwort war
`status=0`, enthielt **0 Einträge** und meldete erneut `session=false` sowie
`nextTransaction=true`. Die Probe stoppte direkt danach. Es wurden keine
weiteren Transaktionen angefragt.

Auch der zweite Lauf belegt daher keine zusätzliche befüllte Liste oder
abrufbaren Dateien. Die Null-Einträge-Antwort ist keine Evidenz für weitere
Historien-Dateien.

## Sicherheits- und Datenschutzumfang

- Zwei einzeln bestätigte Läufe insgesamt; das separate Gate ist nach dem
  Zusatzlauf verbraucht (`2/2`). Der Gate-Zähler wurde nicht zurückgesetzt.
- Pro Lauf: ein `sync_request`, eine initiale Item-List und höchstens eine
  `transaction_id`-Folgeanfrage. Im zweiten Lauf genau eine Folgeanfrage.
- Zweiter Lauf: `pullSent=false`; keine Datei-Pulls, MLR-Datei-Registrierung,
  Speicherung oder Import. MLR-Transfer: 0 Chunks / 0 Bytes.
- Keine UIDs, Transaktions-ID, Rohframes oder Gesundheitsdaten protokolliert.
- Der Dialog warnte, dass der `sync_request` gepufferte Banddaten geräteseitig
  als übertragen markieren kann; der Nutzer hatte diesen möglichen Effekt
  ausdrücklich akzeptiert.
- Kein Reset des bestehenden 15/15-Item-List-Gates.

## Evidenzgrenzen

Es sind zwei einzelne Läufe mit dem jeweiligen damaligen Geräte-/Archivzustand.
Sie klären nicht, warum die Folgeantworten leer waren, ob eine spätere
Transaktion weitere Einträge liefert oder ob `nextTransaction=true` immer so
reagiert. Kein automatisches Fortsetzen/Download daraus ableiten. Keine
weiteren Geräteversuche sind durch diese Freigabe umfasst.
