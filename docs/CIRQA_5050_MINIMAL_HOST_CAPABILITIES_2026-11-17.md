# CIRQA 5050: Minimale Host-Capability-Strategie (2026-11-17)

## Ausgangslage

CIRQA sendet eine 5050-Konfigurationsnachricht mit 18 Capability-Bytes. Öffentliche Garmin-Implementierungen (Gadgetbridge/Amazfish) verwenden als Host-Antwort 15 Capability-Bytes und separat ein generisches GFDI-ACK. Der Byteumfang und die von CIRQA erwartete Antwort sind noch nicht bestätigt.

Quellen:
- https://github.com/xiaoyou5602/band-health-sync/blob/7a7fab9bc705881466af87502a8d67cecf85fc36/app/src/main/java/nodomain/freeyourgadget/gadgetbridge/devices/garmin/GarminCapability.java
- https://github.com/piggz/harbour-amazfish/blob/master/daemon/src/services/garmin/garminconfigurationmessage.cpp
- https://github.com/piggz/harbour-amazfish/blob/master/daemon/src/services/garmin/garmingfdimessage.cpp

## Implementierte Offline-Policy

`Cirqa5050OfflineHostCapabilities` bietet ausschließlich zwei klar getrennte Kandidaten:
- **Standard**: 15 Bytes, alle Bits 0 (keine ungeprüften Fähigkeiten behaupten).
- **Expliziter Offline-Vergleich**: ausschließlich Bit 3 (`SYNC` laut öffentlicher GarminCapability-Ordinaltabelle) gesetzt.

Beide Varianten können zu Vergleichszwecken mit 15 oder 18 Bytes erzeugt werden. Es gibt keine automatische Geräteantwort, keine GATT-Verbindung und keine Aktivierung von Dateizugriff oder Synchronisation. Ein Nullmasken-Handshake könnte von CIRQA abgelehnt werden; er ist kein Nachweis von Funktionsfähigkeit.

## Validierung

`Cirqa5050OfflineHostCapabilitiesTest` prüft Nullmaske, explizites SYNC-Bit, beide Byteumfänge, Länge, CRC und Ablehnung unbekannter Längen. Der bereits vorhandene `Cirqa5050OfflineResponse` wird ausschließlich offline verwendet.

## Nächste Schritte

1. GitHub CI-Ergebnis der neuen Tests prüfen.
2. 15-vs-18-Byte-Antwortformat anhand weiterer Referenzen und einer kontrollierten, separaten CIRQA-Entscheidung bewerten.
3. Erst nach Verifikation eine explizit begrenzte, protokollierte Testfunktion in Betracht ziehen. Keine automatische 5050-Übertragung.
