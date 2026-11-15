# PulseLibre – verbindliche Zielgeräte und Testumgebung

Stand: 2026-11-15. Diese Datei ist die **erste Anlaufstelle** für neue Entwickler, spätere Analysen, BLE-Recherchen, Testpläne und KI-gestützte Änderungen. Vor Rückfragen zum Modell zuerst diese Datei sowie `docs/PROJECT_HISTORY.md` lesen.

## Primäres Wearable
- **Hersteller:** Garmin
- **Im Projekt dokumentierte Modellbezeichnung:** **Garmin CIRQA Smartband**
- **Status:** Bereits mit **Garmin Connect** gekoppelt.
- **BLE-Ziel:** Möglichst direkte, lokale und datenschutzfreundliche Erfassung von Puls, SpO₂, Schlaf und Aktivität, soweit das konkrete Gerät diese Informationen technisch zugänglich macht.
- **Wichtige Einschränkung:** Modellbezeichnung, konkrete Hardware-/Firmware-Version, GATT-UUIDs, historische Datenzugriffe und proprietäre Protokollkommandos wurden **noch nicht unabhängig an der Hardware bestätigt**. „CIRQA“ ist die im Projekt bisher verwendete Bezeichnung, **kein Nachweis eines offiziell dokumentierten Garmin-Protokolls**. Keine Protokollannahmen aus anderen Garmin-Geräten ungeprüft übernehmen.

## Primäres Android-Testgerät
- **Samsung Galaxy S25** (vom Nutzer ausdrücklich für echte App- und Bluetooth-Tests gewünscht).
- Android-Version, Firmware, Sicherheits-Patchstand und genaue Hardwarekennung: noch nicht erfasst; nur bei konkretem Testbedarf nachfragen.
- Android-Emulator bleibt automatisches CI-Testwerkzeug, ersetzt aber keine BLE-Hardwaretests.

## Nicht verhandelbare Sicherheitsregeln
1. **Garmin Connect nicht entkoppeln, zurücksetzen oder in seiner vorhandenen Verbindung beeinträchtigen.**
2. Keine automatische Pairing-/Bonding-Anfrage, kein GATT-Schreibbefehl, kein Descriptor-Write und keine proprietäre Subscription ohne nachgewiesene Verträglichkeit und ausdrückliche Freigabe.
3. BLE-Scan/GATT-Inspektion sind Diagnostik, keine verifizierte Gesundheitsdatenintegration.
4. Keine erfundenen Messwerte oder Kompatibilitätsbehauptungen; unbekannte Werte explizit als unbekannt kennzeichnen.
5. Kein INTERNET-Permission, kein Cloud-Zwang, keine persönlichen Geräteadressen oder Gesundheits-Rohdaten ins Repository committen.
6. Vor S25-Tests aktuellen CI-Build prüfen; echte Gerätetests benötigen die ausdrückliche Entscheidung des Nutzers.

## Arbeitsablauf für Folgeanalysen
1. Diese Datei und `docs/REQUIREMENTS_AND_PRINCIPLES.md` lesen.
2. Für CIRQA zuerst belastbare Hersteller-/Hardwareinformationen und reale BLE-Diagnose prüfen.
3. Protokollhypothesen und bestätigte Beobachtungen getrennt dokumentieren.
4. JVM-, Instrumentation- und S25-Praxistests mit klaren Ergebnissen ergänzen.
5. **Erst nach diesen Prüfungen** Geräte-Dekodierung, historische Synchronisation und Anzeige echter Werte freigeben.

Verwandt: [S25-BLE-Testplan](S25_BLE_TEST_PLAN.md), [Projektchronik](PROJECT_HISTORY.md), [Anforderungen](REQUIREMENTS_AND_PRINCIPLES.md).

## Externe CIRQA-Referenz (Recherche 2026-11-15)
- Gadgetbridge listet Garmin CIRQA ausdrücklich als **experimentell, teilweise unterstützt**. Laut Projektseite wurde Schlafsynchronisierung bestätigt; einige Aktivitäts-/Sync-Funktionen waren mit Firmware 2.20 problematisch und wurden mit 2.32 als funktionierend gemeldet: https://gadgetbridge.org/gadgets/wearables/garmin/
- Gadgetbridge Garmin-Überblick: https://gadgetbridge.org/basics/topics/garmin/
- Protokollübersicht (verschiedene Garmin-BLE-Protokollgenerationen): https://gadgetbridge.org/internals/specifics/garmin-protocol/
- **Nicht** aus Gadgetbridge-Unterstützung ableiten, dass PulseLibre bereits auf Garmin Connects GATT-Sitzung zugreifen kann. Gadgetbridge verwendet eine eigene Gerätekommunikation; deren paralleler Betrieb mit Garmin Connect ist hier nicht nachgewiesen.
- Keine Kopplung, kein aktiver GATT-Verbindungsaufbau und kein Übernehmen der Sitzung ohne ausdrückliche Nutzerfreigabe. Vor Implementierung konkrete CIRQA-Handler, Datenformate, Lizenzbedingungen und benötigte Handshakes im Gadgetbridge-Quellcode prüfen.
- Praxistest PulseLibre Build #93: CIRQA Android-GATT-Status `verbunden (App unbekannt)`, Android-Kopplung vorhanden, BLE-Werbeadresse nicht bestätigt; 25 fremde Werbegeräte im Scan. Kein Nachweis, dass die fremden Werbedaten zu CIRQA gehören.
- Datenschutz: Gadgetbridge warnt davor, dass Garmin-Bluetooth-Logs möglicherweise OAuth-/Account-Informationen enthalten. Keine ungefilterten proprietären Nutzdaten in Diagnoseexporten oder Git.
