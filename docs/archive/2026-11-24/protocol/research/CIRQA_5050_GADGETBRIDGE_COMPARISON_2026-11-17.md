# CIRQA 5050: Vergleich mit Gadgetbridge und Amazfish (17.11.2026)

## Primärquellen

1. [Gadgetbridge-abgeleiteter ConfigurationMessage.java](https://github.com/xiaoyou5602/band-health-sync/blob/7a7fab9bc705881466af87502a8d67cecf85fc36/app/src/main/java/nodomain/freeyourgadget/gadgetbridge/service/devices/garmin/messages/ConfigurationMessage.java): `parseIncoming` liest **ein Byte Längenangabe**, anschließend genau so viele Capability-Bytes; `generateOutgoing` erzeugt eine eigene 5050-Antwort mit eigener Capability-Maske; der Status wird separat erzeugt.
2. [Gadgetbridge-abgeleiteter GarminCapability.java](https://github.com/xiaoyou5602/band-health-sync/blob/7a7fab9bc705881466af87502a8d67cecf85fc36/app/src/main/java/nodomain/freeyourgadget/gadgetbridge/devices/garmin/GarminCapability.java): Bits sind little-endian innerhalb jedes Bytes (`1 << bitPos`); Capability-Index entspricht dem Enum-Ordinal. 120 definierte Bitpositionen, davon etliche unbekannt. `OUR_CAPABILITIES` ist ein breit gesetzter Referenz-Host-Funktionsumfang, **nicht** für PulsLibre geeignet.
3. [Amazfish ConfigurationMessage.cpp](https://github.com/piggz/harbour-amazfish/blob/master/daemon/src/services/garmin/garminconfigurationmessage.cpp) und [GFDI-Dispatcher](https://github.com/piggz/harbour-amazfish/blob/master/daemon/src/services/garmin/garmingfdimessage.cpp): zuerst generisches 5000-ACK, danach 5050-Antwort mit 15-Byte-Maske.

## Evidenzabgleich mit CIRQA

- CRC-validierter 5050-Body: **19 Bytes**, beginnt mit **0x12** (=18), gefolgt von genau 18 Bytes. Dies entspricht der konkreten Referenz-Parserstruktur, nicht nur einem beliebigen Längenmuster.
- CIRQA liefert 18 Capability-Bytes (144 Bits), die genannten Referenz-Hosts erzeugen 15 Bytes (120 Bits). Daraus folgt **keine** Zulässigkeit, 15-Byte-Referenzantworten blind zu kopieren, alle 144 Bits zu setzen oder das Gerät selbst als Host-Capabilities zurückzuspiegeln.
- 5011 wird öffentlich als FIT_DEFINITION benannt; das bestätigt keinen Historienzugriff.

## Implementierter Status

- `Cirqa5050OfflineCapabilities` validiert ausschließlich CRC, GFDI-Typ, Längenstruktur und Bitanzahl.
- `Cirqa5050OfflineResponse` erzeugt offline eine 5050-Antwort nur mit **explizit** übergebenen Bytes; keine Standardmaske, keine GATT-Verbindung.
- `CirqaOfflineGfdiStatusCandidate` erzeugt offline ein generisches 5000-ACK; ebenfalls ohne GATT.
- Unit-Tests decken die Struktur und CRC-Fehler ab.

## Entscheidung für den nächsten kontrollierten Schritt

Eine minimalistische Host-Capability-Strategie muss anhand tatsächlich implementierter PulsLibre-Funktionen (insbesondere SYNC/FILE_SYNC) entworfen und getestet werden; nicht anhand der umfassenden Referenzmaske. Vor dem Senden ist zu klären, ob CIRQA eine 15- oder 18-Byte-Host-Maske erwartet und welche Folge-Nachrichten nach ACK+5050 zu erwarten sind. Bis dahin **kein** aktiver 5050-Handshake.

## Lizenz

Die externe Gadgetbridge-abgeleitete Quelle ist AGPL-lizenziert. PulsLibre übernimmt **keinen** externen Quellcode oder Enum in das Repository; dokumentiert werden nur Protokollfakten und Links.
