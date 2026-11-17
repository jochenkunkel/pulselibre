# CIRQA 5050: passiver Strukturtest

## Ziel

Vorhandene CIRQA-Messung: 5024 einmal, anschließend 5050 dreimal mit identischer Nutzlast und Abständen 4968/5039 ms, danach 5011. Die Daten stützen eine Retry-Hypothese, belegen aber keine bestimmte 5050-Semantik oder eine akzeptierte Anwendungs-ACK.

## Umsetzung

- `Cirqa5050StructureObserver` verarbeitet ausschließlich CRC-geprüfte GFDI-Frames mit Typ 5050.
- Ermittelt Gesamtzahl, identische Wiederholungen, Payloadwechsel, Body-Längen und gemeinsame Präfixlängen zwischen unterschiedlichen 5050-Bodies.
- Nur aggregierte Strukturmetadaten im Diagnosebericht; keine Nutzdaten oder Modellidentitäten in diesem Abschnitt.
- In `GarminFileListProbe` eingebunden, damit die bestehende Historien-Diagnose die Auswertung automatisch enthält.
- Negative Tests für CRC und fremden Nachrichtentyp, Positivtest für Retry/Wechsel.
- Kein neuer Sendepfad, kein ACK auf 5050, kein FileSync, keine Flags.

## Testablauf nach erfolgreichem CI

1. Aktuelle APK installieren.
2. In der App die bestehende CIRQA Historien-Diagnose bzw. den expliziten Bootstrap-ACK-Test starten.
3. Nach dem 20-Sekunden-Fenster nur den Abschnitt `5050-Struktur (passiv)` sowie den Handshake-Zustand vergleichen.

Der passive Test ist diagnostisch, **kein** Beweis, dass ein 5050-ACK korrekt wäre. Für einen aktiven 5050-Test ist zuerst eine verifizierte CIRQA-spezifische Antwortstruktur nötig.
