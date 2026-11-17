# CIRQA 5050 ACK: Auswertung des zweiten gezielten Gerätetests (2026-11-17)

## Beobachtete Evidenz

- GFDI-Registrierung bestätigt; dynamischer ML-Handle 5.
- Empfangene GFDI-Typen: 5024 (1), 5050 (1), 5000 (1).
- 5024-ACK GATT-bestätigt; 5050-Antwortfolge mit 15-Byte-Nullmaske GATT-bestätigt.
- **Nach** GATT-Bestätigung der 5050-Antwortfolge: genau ein weiteres CRC-validiertes GFDI-Frame, Typ 5000, Status `request=5050/status=ACK`.
- Danach innerhalb des 20-Sekunden-Fensters keine 5050-Wiederholung, kein 5011, keine Dateiliste, keine Protobuf-Nachricht.
- Das `5000/ACK` ist eine positive Statusantwort für Nachricht 5050, **kein Beleg** für freigeschaltete History-Synchronisation. Ein ACK sagt nicht, ob jede Capability oder der komplette Bootstrapschritt akzeptiert wurde.
- Die passive Handshake-State-Machine meldet `WAITING_5050_RESPONSE` und `5050-Antwort=NICHT_GESENDET`, weil sie keinen aktiven TX-Zustand verfolgt. Der separate aktive Testbericht hat Vorrang für TX-Metadaten.
- Der Diagnosehinweis enthält derzeit das literale `\\n`; das ist ein Darstellungsfehler, kein BLE-Fehler.

## Öffentliche Implementierung: Folgeschritt

In Amazfish `garmingfdimessage.cpp` ruft `parseConfiguration` zunächst `GfdiMessageGenerator::ackResponse(msgId)` auf und danach `GarminConfigurationMessage::parse(data)`. Das stützt das separate ACK-plus-Konfigurationsantwort-Modell.

Die Amazfish-Datei `garminsynchronizationmessage.cpp` behandelt eine **eingehende** GFDI-Synchronization-Nachricht (5037), quittiert sie und sendet unter bestimmten Bedingungen einen Filter. Das belegt **nicht**, dass PulsLibre nach 5050 ungefragt eine 5037-Nachricht oder einen Filter senden soll.

Quellen:
- https://github.com/piggz/harbour-amazfish/blob/master/daemon/src/services/garmin/garmingfdimessage.cpp
- https://github.com/piggz/harbour-amazfish/blob/master/daemon/src/services/garmin/garminsynchronizationmessage.cpp

## Entscheidung

Keine unbestätigte 18-Byte-Hostmaske und keine automatische 5037-/Filter-/FileSync-Nachricht einführen. Zunächst ausschließlich passiv die Post-5050-Zustandsübergänge und die Semantik des positiven 5000-Status weiter untersuchen. Die fehlende Dateiliste ist kein Parserfehlerbeweis, weil noch keine valide Historien-Synchronisation angefordert wurde.
