# PulsLibre: Vollständiger historischer Datenabruf (Projektziel)

## Verbindliches Ziel

PulsLibre soll **alle vom Garmin CIRQA über die verfügbare Geräteschnittstelle zugänglichen historischen Daten** herunterladen, nicht nur historische Herzfrequenzdaten.

Das umfasst grundsätzlich sämtliche vom Gerät angebotenen Dateien, Datensätze und Datentypen. Welche davon das konkrete CIRQA tatsächlich bereitstellt, muss durch Gerätebeobachtung und Protokolltests nachgewiesen werden. Keine Vorabfilterung auf Herzfrequenz, FIT oder einzelne Aktivitätstypen.

## Umsetzungskriterien

1. Alle verfügbaren Dateilisten und gegebenenfalls deren Seiten/Cursor vollständig und nachvollziehbar abrufen.
2. Alle vom Gerät zum Lesen freigegebenen historischen Dateien herunterladen; unbekannte Dateitypen nicht verwerfen.
3. Originalbytes verlustfrei erhalten, Integrität und Herkunft (ohne sensible Gerätekennungen in Diagnoselogs) prüfen.
4. Unterbrechungen, Wiederholungen und Dubletten sicher behandeln; Fortschritt und Vollständigkeit nachvollziehbar machen.
5. Dateninterpretation (Herzfrequenz, Bewegung, Aktivität und weitere Typen) **nachgelagert** durchführen, ohne den Rohdatenabruf einzuschränken.
6. Keine löschenden oder verändernden Gerätebefehle verwenden (insbesondere kein FileSetFlags, kein Reset und kein CLOSE_ALL ohne separate Verifikation und Freigabe).
7. Keine Behauptung vollständiger historischer Datenabdeckung, bevor Dateiliste, Paginierung und alle zugänglichen Dateien am CIRQA nachweislich geprüft wurden.

## Aktueller Stand (2026-11-16)

- BLE-Realtime und passive GFDI-Diagnostik vorhanden.
- FileSync-Dateilistenanfrage wurde vom CIRQA mit UNSUPPORTED abgelehnt.
- Passive Decoder, Offline-ACK-Encoder und erste Protobuf-Reassembly-Bausteine vorhanden.
- **Ein vollständiger historischer Datenabruf ist noch nicht nachgewiesen.**

## Priorisierung

Zuerst sichere Protokollinitialisierung und vollständige Verzeichnis-/Dateiübertragung; anschließend generische Rohdatenspeicherung, Wiederaufnahme und Integritätsprüfung; erst danach datentypspezifische Analyse.

Dieses Dokument korrigiert frühere Formulierungen, die das Projektziel fälschlich auf historische Herzfrequenzdaten verengt haben.
