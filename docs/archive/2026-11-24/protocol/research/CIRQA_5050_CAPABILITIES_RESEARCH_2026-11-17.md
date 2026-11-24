# CIRQA 5050: Konfigurations-Capabilities, Quellenvergleich und Offline-Test (2026-11-17)

## Quellen und Befund

- [Amazfish ConfigurationMessage (C++)](https://github.com/piggz/harbour-amazfish/blob/master/daemon/src/services/garmin/garminconfigurationmessage.cpp): empfängt 5050, erzeugt Antwort-5050 mit `[u8 capabilityLength][capabilityBytes]`, schließt Pairing ab. Die Referenz verwendet eine 15-Byte-Capability-Bitmaske; das ist **kein CIRQA-Default**.
- [Amazfish GFDI-Dispatcher](https://github.com/piggz/harbour-amazfish/blob/master/daemon/src/services/garmin/garmingfdimessage.cpp): `parseConfiguration` sendet **zuerst generisches 5000-ACK** und **danach eine eigene 5050-Antwort**. Ein reiner 5000-ACK reicht daher zumindest für diese Referenz nicht als kompletter Austausch.
- [garmin-ble Nachrichtentypen](https://github.com/gwerneckp/garmin-ble/blob/main/garmin_ble/constants.py): `5050=CONFIGURATION`, `5011=FIT_DEFINITION`.

## CIRQA: datenschutzschonende Rohframe-Analyse

Der vom Nutzer bereitgestellte, CRC-gültige 5050-Body hat 19 Byte. Sein **erstes Byte ist 0x12 (=18)**; danach folgen **exakt 18 Byte**. Das ist ein strukturell exakter Treffer für die in Amazfish verwendete `[Länge][Capability-Bitmaske]`-Kodierung. Es ist **noch kein Beweis** für die Semantik einzelner Bits oder dafür, welche Capabilities CIRQA vom Telefon erwartet. Keine rohen Identifikatoren oder Hexframes werden in diesem Dokument gespeichert.

Die 5050-Frames werden nach 4997 und 5011 ms identisch wiederholt. 5011 erscheint 5010 ms nach dem dritten 5050; die öffentliche Bezeichnung `FIT_DEFINITION` allein belegt keinen Historien-Download.

## Implementierung

- `Cirqa5050OfflineCapabilities.inspect(frame)`: rein offline, verlangt gültige Länge/CRC und Nachrichtentyp 5050, ermittelt deklarierten und tatsächlichen Bitmasken-Längenwert sowie gesetzte Bits **ohne Rohdaten auszugeben**.
- `Cirqa5050OfflineCapabilitiesTest`: passender 19-Byte-Body, falsche Länge, defekter CRC, fremder Nachrichtentyp.
- `CirqaOfflineGfdiStatusCandidate`: separat existierender 5000-ACK-Kandidat, weiterhin **nicht** mit BLE verbunden.

## Sicherheitsentscheidung und Folgearbeit

**Keine automatische 5050-Antwort.** Amazfish nutzt 15 Capability-Bytes, CIRQA sendet 18. Ein blindes Kopieren der Amazfish-Capabilities oder ACK+5050-Sequenz könnte falsche Funktionen zusagen. Nächste Aufgabe: Referenz-Bitzuordnung und Host-Capability-Auswahl mit weiteren Implementierungen abgleichen, dann offline 5050-Response-Encoder samt Tests bauen, ohne BLE-Schreibpfad. Gerätetest erst bei einer nachvollziehbaren und begrenzten Antwortstrategie.
