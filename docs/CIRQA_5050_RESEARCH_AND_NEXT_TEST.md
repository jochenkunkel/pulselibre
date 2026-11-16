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
