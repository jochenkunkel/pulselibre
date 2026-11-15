# PulseLibre

**Offline-first Android-Projekt für Garmin CIRQA auf Samsung Galaxy S25.**

![PulseLibre-Logo](design/pulselibre-icon.svg)

**Projektstatus:** Passive BLE-Diagnose auf dem S25 praktisch getestet; erste Android-Bluetooth-Berechtigungsabfrage auf dem S25 bestätigt. **Kein CIRQA-Protokollzugriff, keine Gesundheitsdatensynchronisierung und keine aktive PulseLibre-GATT-Verbindung.** Garmin Connect wird nicht verändert.

## Einstieg und Dokumentation

- [Architektur, Code-Review und Ablaufdiagramme](docs/ARCHITECTURE_AND_CODE_REVIEW.md)
- [Priorisierte Roadmap bis zu echten CIRQA-Messwerten](docs/ROADMAP_TO_CIRQA_DATA.md)
- [Android-Gerätekompatibilität, Updates und Play-Store-Vorbereitung](docs/ANDROID_RELEASE_READINESS.md)
- [Geräte, Einschränkungen und Sicherheitsregeln](docs/TARGET_DEVICES.md)
- [Garmin-Protokollrecherche](docs/GARMIN_PROTOCOL_RESEARCH.md)
- [S25-Testplan](docs/S25_BLE_TEST_PLAN.md)
- [Projektziele und Anforderungen](docs/REQUIREMENTS_AND_PRINCIPLES.md)
- [Qualitätsanforderungen](docs/QUALITY_GATE.md)
- [Design und Branding](docs/BRANDING.md)

## Was derzeit tatsächlich funktioniert

- Android-Laufzeitberechtigung für Bluetooth beim ersten Start; bei bereits erteilter Berechtigung kein erneuter Dialog.
- Benutzerinitiierter, auf 60 Sekunden begrenzter **passiver** BLE-Scan; Werbeinformationen und vorsichtige Herstellerhinweise.
- Nur lesende Android-Abfrage zu CIRQA-Kopplung und Android-GATT-Status; die verbundene App ist **unbekannt**.
- Export einer Diagnose **nur nach ausdrücklicher Teilen-Aktion**. Vorsicht: Der Text kann Gerätebezeichnungen enthalten; rohe BLE-Service-Daten werden im Standardbericht nicht mitgeteilt.
- Offline-Datenmodell, lokale SQLite-Ablage mit Transaktionen und CSV-Export für künftig **validierte** Messwerte. Noch keine echte CIRQA-Datenquelle angebunden.
- Reine Offline-Inspektion von GFDI-Headern und CRC-16-Prüfung mit synthetischen Testpaketen. **Kein FIT-Decoder.**

## Sicherheitsgrenzen

Keine INTERNET-Berechtigung, kein Cloud-Sync, keine Telemetrie, keine erfundenen Gesundheitswerte. Keine aktive CIRQA-Verbindung, kein Re-Pairing, keine GATT-Schreiboperation, keine Benachrichtigungs-Subscription ohne separate ausdrückliche Freigabe. Die Android-Systemanzeige „GATT verbunden“ ist **kein** Beweis für Datenzugriff durch PulseLibre.

## Build und Tests

JDK 17, Android SDK 35, Gradle 8.13. Beispiel:

```bash
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

Der GitHub-Workflow `.github/workflows/android.yml` führt anschließend API-35-Emulatortests aus. Eine erfolgreiche CI ist **kein** Ersatz für einen echten CIRQA-Test auf dem S25. Der Workflow lädt Debug-APKs als zeitlich begrenzte Artifacts hoch. Derzeit ist kein Gradle-Wrapper im Repository.

## Ausblick

Die nächsten Schritte sind die Behebung der im [Review](docs/ARCHITECTURE_AND_CODE_REVIEW.md) beschriebenen Risiken, ein belastbarer Offline-FIT-Parser und erst danach eine gesonderte Entscheidung über einen sicheren, vom Nutzer ausdrücklich freigegebenen CIRQA-Transportweg.

[Projektposter](design/pulselibre-poster.svg)
