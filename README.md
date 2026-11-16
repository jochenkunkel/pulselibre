# PulseLibre

**Neuer Chat oder Entwicklungsagent?** Zuerst [CHAT_START_HERE.md](docs/CHAT_START_HERE.md) lesen, danach den [zentralen Projektstatus](docs/PROJECT_STATUS.md) und die offenen GitHub-Issues prüfen.

**Offline-first Android-Projekt für Garmin CIRQA auf Samsung Galaxy S25.**

![PulseLibre-Logo](design/pulselibre-icon.svg)

**Projektstatus:** Passive BLE-Diagnose auf dem S25 praktisch getestet; erste Android-Bluetooth-Berechtigungsabfrage auf dem S25 bestätigt. **Es gibt experimentelle, nutzerinitiierte aktive GATT- und Garmin-ML-Probe-Pfade; eine verlässliche CIRQA-Gesundheitsdatensynchronisierung ist nicht nachgewiesen.** Garmin Connect wird nicht verändert.

## Einstieg und Dokumentation

**Für alle Chats und Entwicklungsagenten verbindlicher Einstieg:** [Zentraler Projektstatus und Übergabeprotokoll](docs/PROJECT_STATUS.md). Änderungen und neue Erkenntnisse aus anderen Chats müssen dort explizit dokumentiert werden; Chatverläufe synchronisieren sich nicht automatisch.

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
- Experimentelle, ausdrücklich vom Nutzer gestartete GATT-Dienstsuche und ML-Probe einschließlich Vordergrunddienst für wiederholte Versuche. Die Existenz dieses Codes belegt keine erfolgreich ausgelesenen CIRQA-Gesundheitswerte; mögliche Wechselwirkungen mit Garmin Connect müssen am Gerät geprüft werden.
- Offline-Datenmodell, lokale SQLite-Ablage mit Transaktionen und CSV-Export für künftig **validierte** Messwerte. Noch keine echte CIRQA-Datenquelle angebunden.
- Reine Offline-Inspektion von GFDI-Headern und CRC-16-Prüfung mit synthetischen Testpaketen. **Kein FIT-Decoder.**

## Sicherheitsgrenzen

Keine INTERNET-Berechtigung, kein Cloud-Sync, keine Telemetrie, keine erfundenen Gesundheitswerte. Aktive GATT-Verbindungsversuche und ein experimenteller ML-Handshake sind implementiert und dürfen nur nach ausdrücklicher Nutzeraktion starten. Die separate reine GATT-Dienstsuche schreibt und abonniert nicht; dies darf nicht pauschal auf den ML-Probe-Pfad übertragen werden. Keine automatische Neu-Kopplung behauptet. Die Android-Systemanzeige „GATT verbunden“ ist **kein** Beweis für Datenzugriff durch PulseLibre.

## Build und Tests

JDK 17, Android SDK 35, Gradle 8.13. Beispiel:

```bash
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

Der GitHub-Workflow `.github/workflows/android.yml` führt bei Pushes auf `main` und bei Pull Requests **Build und JVM-Unit-Tests** aus. **Vollständiges Lint, Instrumentation-APK und API-35-Emulatortests** laufen nur bei `v*`-Tags oder manuell mit `full_tests=true`. Ein grüner Beta-Build ist daher **kein** bestandenes Full-Quality-Gate. Der tatsächliche Erfolg muss für jeden Commit separat geprüft werden. Eine erfolgreiche CI ist **kein** Ersatz für einen echten CIRQA-Test auf dem S25. Der Workflow lädt Debug-APKs als zeitlich begrenzte Artifacts hoch. Derzeit ist kein Gradle-Wrapper im Repository.

## Ausblick

Die nächsten Schritte sind die Absicherung und Gerätevalidierung der bereits implementierten experimentellen GATT-/ML-Pfade, die Behebung der dokumentierten Risiken sowie ein belastbarer Offline-FIT-Parser. Vor produktiver CIRQA-Kommunikation sind Interoperabilität und ausdrückliche Nutzerfreigabe zu prüfen.

**Aktuelle Aufgaben:** [GitHub-Issues](https://github.com/jochenkunkel/pulselibre/issues) · [Audit und Reparaturplan (Draft-PR #1)](https://github.com/jochenkunkel/pulselibre/pull/1). Die Architektur-Review-Datei enthält historische Befunde und ist noch nicht vollständig aktualisiert.

[Projektposter](design/pulselibre-poster.svg)

## Lizenz und Drittanbieter-Provenienz

Eigenständige PulsLibre-Beiträge stehen unter der [Apache License 2.0](LICENSE). Diese Lizenz ändert keine Rechte an Fremdcode. Die vorläufige Prüfung der AGPLv3-Protokollreferenzen und die noch offenen Herkunftsfragen stehen in [LICENSE_AGPL_AUDIT_2026-11-16.md](docs/LICENSE_AGPL_AUDIT_2026-11-16.md). **Die Prüfung ist noch keine vollständige rechtliche Freigabe.**

