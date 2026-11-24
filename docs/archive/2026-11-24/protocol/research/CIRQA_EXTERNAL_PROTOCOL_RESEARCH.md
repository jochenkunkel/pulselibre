# CIRQA: Externe Protokollhinweise und Bootstrap-Hypothese

Stand: 2026-11-16. Ziel: **alle zugänglichen historischen Daten**, keine Einschränkung auf Herzfrequenz.

## Beobachtung am CIRQA

Im vollständig abgelaufenen passiven Diagnosefenster: GFDI-Registrierung erfolgreich, Handle 11, vier gültige GFDI-5024-DeviceInformation-Frames, keine FileListResponse und keine aktive FileSync-Anfrage. Dies belegt **keinen** funktionierenden historischen Datenabruf.

## Externe Vergleichsimplementierungen

1. Sam Dumont, Reverse-engineering the Garmin HRM 600: https://dropbars.be/blog/reverse-engineering-garmin-hrm600-running-dynamics/ . Dort folgt auf wiederholte DeviceInformation-Frames ein vom Client gesendetes ACK, ein Core-Preamble/FeatureCapabilities-Handshake und weitere Schritte. Die anfängliche Annahme, Registrierung allein genüge, war falsch. **Übertragbarkeit auf CIRQA unbestätigt.**
2. mskerwiderski/GarminHRM600: https://github.com/mskerwiderski/GarminHRM600 . Beschreibt den Multi-Link/GFDI-Bootstrap und FileSync V2 einschließlich Chunk-ACKs, FileListResponse, FileRequest und Rohdateistream über Dienst 0x2018. Die klassische GFDI-Dateiübertragung wurde beim HRM 600 als UNSUPPORTED abgelehnt; das ist **nicht** gleichbedeutend mit unserem früheren CIRQA-Status für 0x8A39.
3. Garmin Support, USB File Access: https://support.garmin.com/de-DE/?faq=BAoGNOOLURAVh0AatVJiv5 . Der Garmin-Support nennt CIRQA ausdrücklich unter Geräten mit USB-Dateizugriff. Das ist ein unabhängiger möglicher Weg zur Rohdatensicherung; welche Dateien damit verfügbar sind, muss separat geprüft werden.
4. Garmin CIRQA Nutzerberichte zu Sync-Verzögerungen: https://www.reddit.com/r/GarminCIRQAUsers/comments/1wqi9ma/why_does_the_garmin_cirqa_take_so_long_to_sync/ . Nutzer beschreiben verzögerte Synchronisation in Garmin Connect. Das ist **kein** Beleg für einen GFDI-Fehler oder für den nötigen Bootstrap.

## Arbeitsannahme

Vier wiederholte 5024-Frames könnten bedeuten, dass CIRQA auf eine bislang fehlende Antwort des Clients wartet. **Nicht bestätigt**: exaktes ACK-Format, Sequenzzähler, Geräteidentität, erwartete Folgepakete und Unterstützung von FileSync V2 auf CIRQA.

## Sichere nächste Schritte

- Bestehenden GFDI-5024-Observer um passive Wiederholungsdiagnostik und zeitliche Sequenzanalyse ergänzen; keine Rohdaten/Seriennummern loggen.
- Öffentliche HRM600-ACK- und Bootstrap-Implementierung mit dem eigenen GFDI-Frame-Encoder abgleichen; zuerst Offline-Tests mit Byte-Fixtures.
- Erst nach verifizierten ACK-Bytes und sicherem Abbruchpfad einen **separaten, explizit aktivierten** CIRQA-ACK-Diagnosetest anbieten. Nicht automatisch im normalen Betrieb senden.
- USB-Dateizugriff als parallele Option prüfen, ohne ihn als vollständige Datenquelle zu behaupten.
- Keine spekulativen FileListRequests, FileSetFlags, CLOSE_ALL, Reset- oder Löschbefehle.

## Akzeptanzkriterium

Erst ein CIRQA-Gerätebericht mit beobachteten Folge-Nachrichtentypen nach dem bestätigten Bootstrap rechtfertigt den nächsten aktiven FileSync-Versuch. Ein grüner CI-Build allein reicht nicht.
