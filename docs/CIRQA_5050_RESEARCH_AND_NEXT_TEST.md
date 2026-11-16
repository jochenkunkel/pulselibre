# CIRQA 5050 / 5011 – Referenzabgleich und nächster Diagnoseversuch

Stand: 2026-11-16

## Beobachtung am CIRQA

Nach dem einmaligen minimalen GFDI-5024-ACK folgen reproduzierbar 5050 ×3 (Body jeweils 19 Bytes) und 5011 ×1 (Body 21 Bytes). Keine FileListResponse. Ob 5050 drei identische Wiederholungen oder drei unterschiedliche Nachrichten sind, war bislang unbekannt.

## Externe Referenzen

- [GarminHRM600](https://github.com/mskerwiderski/GarminHRM600): am HRM 600 real getesteter GFDI-/FileSync-V2-Client. Dokumentiert DeviceInformation-ACK, weitere Core-/FeatureCapabilities-Schritte und ACKs für chunked Protobuf. **Nicht 1:1 als CIRQA-Sequenz validiert.**
- [Gadgetbridge Garmin-Protokoll](https://gadgetbridge.org/internals/specifics/garmin-protocol/): GFDI-5000-Status und Sequenzkodierung, klassische Dateizugriffsvarianten; warnt indirekt vor Änderungen durch Set File Flags.
- [garmin-hr-web](https://github.com/fstanis/garmin-hr-web): zusätzliche GDI-Diagnoseansätze, aber GDI-Pfad laut Projekt noch nicht an echter Hardware getestet.
- [garmin-proto-lab](https://github.com/seofernando25/garmin-proto-lab): unabhängige Offline-Implementierung; Hardware-Akzeptanz noch ausstehend.

**Keine der hier überprüften Referenzen belegt eindeutig die Bedeutung von CIRQA-GFDI-5050 oder ein dafür korrektes ACK.** 5011 ist in GarminHRM600 als FIT_DEFINITION benannt, jedoch ist die konkrete CIRQA-Nachricht noch nicht dekodiert.

## Implementierter nächster Test

Die vorhandene Schaltfläche **CIRQA: Bootstrap-ACK-Test (experimentell)** protokolliert nun zusätzlich `Payload-Varianten` für 5050/5011. Dabei werden nur flüchtige Gleichheitsklassen pro Typ ausgegeben: `5050=[1, 1, 1]` bedeutet drei identische Bodies; `5050=[1, 2, 3]` drei unterschiedliche Bodies. Kein Payload, Hash, Seriennummer oder Messwert wird ausgegeben. Höchstens acht Varianten je Typ werden intern verglichen.

Der Test sendet weiterhin ausschließlich das einmalige minimale DeviceInformation-ACK. **Kein unbekanntes 5050-ACK wird gesendet**, solange das Format nicht bestätigt ist. Nach dem nächsten Gerätetest lässt sich die Retransmissions-Hypothese gezielter bewerten.

## Rohdaten-Test: Strukturresultat

GFDI-Handle 15; fünf CRC-geprüfte Frames. Nach dem einmaligen DeviceInformation-ACK wurden drei bytegenau identische GFDI-5050-Frames (je 19 Byte Body) und ein GFDI-5011-Frame (21 Byte Body) empfangen. Keine FileSync-Antwort. Die Identität stützt die Retransmission-Hypothese, belegt aber keine bestimmte ACK-Semantik. Vollständige Rohbytes werden aus Datenschutzgründen nicht in dieser öffentlichen Dokumentation geführt.

## Erweiterter Export – Stand 2026-11-16

Der opt-in Bootstrap-Rohdatenbericht erhält für jedes empfangene, CRC-geprüfte Frame relative Millisekunden, Richtung RX und den GFDI-Typ zusätzlich zu Hexbytes. Die bestehenden Limits (32 Frames / 8 KiB) bleiben erhalten. TX-Rohbytes und persistente Exportdateien sind **noch nicht implementiert**; das ist ein nächster Entwicklungsschritt. Vor einem neuen aktiven Protokollbefehl zuerst die identischen 5050-Wiederholungen und ihre Abstände prüfen. Keine unbestätigten ACKs senden.

## Zeitmessung: periodische 5050-Wiederholung (2026-11-16)

Neuer Opt-in-Rohdatenbericht: 5024 bei +1540 ms; 5050 bei +1599, +6597, +11604 ms; 5011 bei +16620 ms. Intervalle 5050→5050: **4998 ms** und **5007 ms**; letzter 5050→5011: **5016 ms**. Alle drei 5050-Frames sind bytegenau identisch und CRC-geprüft. Das spricht stark für einen 5-s-Retry/Timeout, belegt jedoch noch nicht, ob GFDI-5050 ein ACK auf Transport- oder Anwendungsebene benötigt. Keine FileListResponse und kein Historienzugriff. Rohpayloads bleiben aus öffentlicher Dokumentation ausgeschlossen. Nächster Schritt: Antwortsemantik anhand verifizierter Referenzimplementierungen oder eines funktionierenden Client-Mitschnitts bestimmen, bevor aktive Antworten gesendet werden.

## Referenzcode-Abgleich (2026-11-16)

Direkt geprüft: `mskerwiderski/GarminHRM600/hrm600/gfdi.py`, `hrm600/filesync.py`, `docs/gfdi-filetransfer.md`. Dort sind **5011 = FIT_DEFINITION**, **5000 = RESPONSE**, **5024 = DEVICE_INFORMATION** dokumentiert. **5050 ist dort nicht benannt**. Die 5-Sekunden-Retransmissionen im HRM600 betreffen bestätigungspflichtige **kompakte 0x2c-Protobuf-Nachrichten**, nicht nachgewiesenermaßen CIRQA-5050. Folglich ist eine blind übertragene 5050-Bestätigung fachlich nicht gerechtfertigt. Die kompakte GFDI-Sequenz ist 7 Bit (`(type >> 8) & 0x7f`), nicht 5 Bit; der PulsLibre-Decoder wurde entsprechend korrigiert und mit Regressionstest ergänzt. Nächster belastbarer Weg: funktionierenden Garmin-Connect-Mitschnitt (RX/TX) gewinnen und 5050-Antwort korrelieren; alternativ gezielt öffentliche Implementierungen auf 5050 durchsuchen. Keine ungeprüften TX-Pakete.
