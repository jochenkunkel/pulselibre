# CIRQA 5050: gezielter Android-Gerätetest (2026-11-17)

## Bedienung

1. Nach erfolgreichem Android-CI-Build aktuelle APK installieren.
2. Garmin Connect und laufende CIRQA-Aufzeichnung beenden.
3. In PulsLibre **Bluetooth · Geräte** öffnen, die Überschrift **Bluetooth · Geräte** lange drücken, damit Debug-Funktionen sichtbar werden.
4. **CIRQA: 5050-Konfiguration gezielt testen** wählen und die experimentelle Sicherheitsabfrage bestätigen.
5. Nach maximal 20 Sekunden **Bericht teilen** wählen. Nur den Diagnosebericht, keine privaten GFDI-Rohframes teilen.

## Einmalige Testsequenz

- Garmin ML/GFDI registrieren.
- Nach CRC-gültiger DeviceInformation (5024) den vorhandenen einmaligen minimalen 5024-ACK-Kandidaten senden.
- Erst nach dessen GATT-Schreibbestätigung und empfangenem 5050 ein **5000-ACK für 5050** und danach eine **5050-Antwort mit 15 Capability-Bytes, alle 0**, über den vorhandenen ML-Handle übertragen.
- Kein FileSync, kein CLOSE_ALL, kein automatischer Retry der Konfigurationsantwort, kein Schreiben von Gerätedaten; Timeout 20 Sekunden, anschließend GATT schließen.
- Diagnose meldet, ob die 5050-Antwort eingeplant und die Schreibfolge GATT-seitig bestätigt wurde. Das ist **keine** Anwendungs-ACK-Bestätigung.

## Grenzen

Das Referenzformat stammt aus Gadgetbridge/Amazfish. Die 15-Byte-Hostmaske ist ein bewusst konservativer Testkandidat; CIRQA sendet selbst 18 Byte und könnte 15 Byte oder eine Nullmaske ablehnen. Der Test kann deshalb fehlschlagen, ohne dass dies ein Hardwarefehler ist. Nicht automatisch in den normalen Messbetrieb übernehmen.

## Buildstatus

Änderungen sind auf `main`; ein erfolgreicher CI-Lauf des Testknopf-Commits muss vor einem realen Gerätetest geprüft werden.
