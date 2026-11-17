# CIRQA 5024 ACK – Quellcode-Audit (2026-11-17)

## Nachvollzogener Sendepfad (main)

1. `MainActivity` startet den explizit bestätigten `startBootstrapAckTest`.
2. `CirqaMlHandshakeProbe.receive` akzeptiert erst die GFDI-Registrierung, routet danach Notifications über den zugewiesenen ML-Handle und wartet auf ein CRC-validiertes DeviceInformation-Frame, das `GarminFileListProbe` zählt.
3. `GarminDeviceInformationAckCandidate.frame()` erzeugt **einen klassischen, nicht-kompakten GFDI-5000-Frame** mit 3-Byte-Body: `A0 13 00` (little-endian 5024 = 0x13A0; Status 0). Frame-Länge 9 Byte einschließlich 4-Byte-Header und 2-Byte-CRC16/ARC. Die beiden CRC-Bytes werden dynamisch berechnet.
4. `GarminFileListRequest.cobs` kodiert den GFDI-Frame für den ML-Datenkanal. Die kodierten Bytes werden in Stücke von höchstens 19 Byte geteilt, jedes Stück mit dem registrierten ML-Handle präfixiert, und sequenziell über GATT TX geschrieben.
5. `onCharacteristicWrite` bestätigt die GATT-Schreiboperation, **nicht** die Interpretation als GFDI-ACK. Das Gerät liefert danach drei identische 5050-Nachrichten im Abstand von 4968/5039 ms und schließlich 5011.

## Was dieser Code beweist – und was nicht

- Der ACK-Kandidat adressiert 5024 im Status-Response-Body. Er enthält **keine** Geräteidentitäts- oder Feature-Capabilities-Felder.
- Ein akzeptierter GATT-Schreibvorgang ist kein Anwendungs-ACK.
- Weder ein vollständiger 5024-Antwortbody noch ein 5050-ACK-Format ist für CIRQA verifiziert.
- 5050 ist in der bisher abgeglichenen GarminHRM600-Referenz nicht benannt. Es wäre unzulässig, ohne Evidenz das kompakte Protobuf-ACK-Schema auf 5050 zu übertragen.

## Priorisierte nächste Schritte

1. Offline-Vergleich der vollständigen DeviceInformation-Response-Struktur mit einem *nachweislich funktionierenden* Garmin-Client (oder einer öffentlich überprüfbaren Spezifikation). Keine watch-identitätsgebundenen Felder übernehmen.
2. Explizite TX-Metadaten (Frame-Typ, kodierte Länge, Chunk-Anzahl, Write-Status) im Bericht dokumentieren, ohne Payloads oder Gerätekennungen zu veröffentlichen.
3. Erst danach gezielte, einzeln bestätigte Geräteexperimente; **keine** automatisch gesendeten 5050-ACKs und keine FileSync-Dateikommandos.

## Verweise

- `app/src/main/java/de/pulselibre/GarminDeviceInformationAckCandidate.kt`
- `app/src/main/java/de/pulselibre/CirqaMlHandshakeProbe.kt`
- `app/src/main/java/de/pulselibre/GarminCompactAck.kt`
- `docs/protocol/device-tests/CIRQA_BOOTSTRAP_DEVICE_TEST_2026-11-17.md`
