# CIRQA: Geräteergebnisse der passiven 5050-Strukturanalyse (17.11.2026)

Quelle: vom Benutzer bereitgestellte Diagnoseausgaben zweier unmittelbar verglichener Gerätetests. Kein Rohpayload verfügbar; alle Aussagen beziehen sich auf die sichtbaren Metadaten.

## Test A: passive Historien-Diagnose

- GFDI-Registrierung bestätigt; dynamischer Handle 1.
- 5 Notifications (1 Registrierung, 4 GFDI); 4 gültige GFDI-Frames; 0 ungültige.
- Nachrichtenfolge: `5024, 5024, 5024, 5024`.
- Wiederholte DeviceInformation: 3; kein 5024-ACK versucht.
- 5050-Frames: 0; Dateiliste: 0; keine Dateiübertragung.

## Test B: einmaliger Bootstrap-ACK-Kandidat

- GFDI-Registrierung bestätigt; dynamischer Handle 2.
- 6 Notifications (1 Registrierung, 5 GFDI); 5 gültige GFDI-Frames; 0 ungültige.
- Nachrichtenfolge: `5024, 5050, 5050, 5050, 5011`.
- Minimaler 5024-ACK-Kandidat gesendet und GATT-Schreiben bestätigt; Anwendungs-ACK **nicht** nachgewiesen.
- 5050: 3 Frames, jeweils 19 Byte GFDI-Body, 2 identische Wiederholungen, 0 Payloadwechsel, Intervalle 4997 und 5013 ms.
- 5011: 1 Frame, 21 Byte GFDI-Body.
- 5050-Antwort: nicht gesendet; Dateiliste: 0; keine Dateiübertragung.

## Interpretation / Evidenzgrenzen

Die Änderung von wiederholten 5024 zu 5050 nach dem 5024-ACK-Kandidaten ist ein starkes Indiz für einen Protokollfortschritt. Ein erfolgreiches GATT-Schreiben beweist **keine** semantische Akzeptanz auf GFDI-Ebene.

Drei identische 5050-Nutzlasten in fast genau fünfsekündigem Abstand passen zu einer Wiederholungs-/Timeout-Hypothese. Weder 5050-Semantik noch ACK-Wireformat sind daraus bestimmbar. Die nachfolgende 5011-Nachricht ist ebenfalls nicht ausreichend erklärt.

## Nächster Schritt

- Die existierende explizite, begrenzte Rohframe-Erfassung kann nach separater Nutzerfreigabe genutzt werden, um den CRC-validierten 5050-Body offline auf Strukturmerkmale zu untersuchen. **Vor dem Teilen** auf möglicherweise enthaltene Gerätekennungen, Zeitstempel oder andere private Daten prüfen.
- Keine automatischen 5050-ACKs, keine geratenen Antwortbytes, keine FileSync-Schreibbefehle.
- CIRQA-spezifische Referenzen bzw. verifizierte Request/Response-Paare suchen und mit dem beobachteten 19-Byte-Body vergleichen.
