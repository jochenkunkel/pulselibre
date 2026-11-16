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
