# PulseLibre

**Neuer Chat oder Entwicklungsagent?** Zuerst [CHAT_START_HERE.md](docs/CHAT_START_HERE.md) lesen, danach den [zentralen Projektstatus](docs/PROJECT_STATUS.md) und die offenen GitHub-Issues prüfen.

**Offline-first Android-Projekt für Garmin CIRQA auf Samsung Galaxy S25.**

![PulseLibre-Logo](design/pulselibre-icon.svg)

**Projektstatus:** Passive BLE-Diagnose auf dem S25 praktisch getestet; erste Android-Bluetooth-Berechtigungsabfrage auf dem S25 bestätigt. **Es gibt experimentelle, nutzerinitiierte aktive GATT- und Garmin-ML-Probe-Pfade; eine verlässliche CIRQA-Gesundheitsdatensynchronisierung ist nicht nachgewiesen.** Garmin Connect wird nicht verändert.

**Für Codex in VSCodium und weitere Coding-Agenten:** [AGENTS.md](AGENTS.md) beschreibt die gemeinsamen Arbeitsregeln, Branch-/Review-Absprachen und Sicherheitsgrenzen. Für die aktuelle CIRQA-Evidenz gilt [PROJECT_STATUS.md](docs/PROJECT_STATUS.md); ältere Protokollnotizen sind historische Beobachtungen.

## Dokumentation nach Fachbereichen

**[Dokumentationswegweiser](docs/README.md)** · [UI-Konzept](docs/ui/README.md) · [CIRQA-Protokollentwicklung](docs/protocol/README.md) · [Wissenschaftliche Messwerte und Berechnungen](docs/science/README.md). Die früheren Markdown-Weiterleitungen wurden nach der Linkmigration entfernt; maßgeblich sind die kanonischen Pfade im Dokumentationswegweiser.

## Einstieg und Dokumentation

**Für alle Chats und Entwicklungsagenten verbindlicher Einstieg:** [Zentraler Projektstatus und Übergabeprotokoll](docs/PROJECT_STATUS.md). Änderungen und neue Erkenntnisse aus anderen Chats müssen dort explizit dokumentiert werden; Chatverläufe synchronisieren sich nicht automatisch.

- [Architektur, Code-Review und Ablaufdiagramme](docs/ARCHITECTURE_AND_CODE_REVIEW.md)
- [Priorisierte Roadmap bis zu echten CIRQA-Messwerten](docs/ROADMAP_TO_CIRQA_DATA.md)
- [Android-Gerätekompatibilität, Updates und Play-Store-Vorbereitung](docs/ANDROID_RELEASE_READINESS.md)
- [Geräte, Einschränkungen und Sicherheitsregeln](docs/TARGET_DEVICES.md)
- [Garmin-Protokollrecherche](docs/protocol/research/GARMIN_PROTOCOL_RESEARCH.md)
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
- Offline-Inspektion von GFDI-Headern und CRC-16-Prüfung sowie ein begrenzter Offline-FIT-Decoder (`FitHistoricalObservations`) für ausgewählte Standardfelder mit zugehörigen Unit-Tests. Dies belegt weder einen CIRQA-Historiendownload noch einen Geräteimport oder einen erfolgreichen CI-Lauf.
- CIRQA-Testlabor mit passivem Scan, Android-Verbindungsstatus, begrenzter read-only GATT-Dienstsuche, ausdrücklich bestätigter vorhandener Live-Messwertprobe und datensparsamem Testbericht. Historischer CIRQA-Download ist gesperrt.
- Offline FIT-Envelope-/CRC-Prüfung und begrenzte Dekodierung ausgewählter Garmin-Profilfelder. SAF-Dateiauswahl, keine allgemeine Dateiberechtigung, keine Kopie oder Datenbankübernahme. Dies bestätigt weder CIRQA-Herkunft noch einen CIRQA-Dateidownload.

## Sicherheitsgrenzen

Keine INTERNET-Berechtigung, kein Cloud-Sync, keine Telemetrie, keine erfundenen Gesundheitswerte. Aktive GATT-Verbindungsversuche und ein experimenteller ML-Handshake sind implementiert und dürfen nur nach ausdrücklicher Nutzeraktion starten. Die separate reine GATT-Dienstsuche schreibt und abonniert nicht; dies darf nicht pauschal auf den ML-Probe-Pfad übertragen werden. Keine automatische Neu-Kopplung behauptet. Die Android-Systemanzeige „GATT verbunden“ ist **kein** Beweis für Datenzugriff durch PulseLibre.

## Build und Tests

JDK 17, Android SDK 35, Gradle 8.13. Beispiel:

```bash
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

Der GitHub-Workflow `.github/workflows/android.yml` führt bei Pushes auf `main` und bei Pull Requests **Build und JVM-Unit-Tests** aus. **Vollständiges Lint, Instrumentation-APK und API-35-Emulatortests** laufen nur bei `v*`-Tags oder manuell mit `full_tests=true`. Ein grüner Beta-Build ist daher **kein** bestandenes Full-Quality-Gate. Der tatsächliche Erfolg muss für jeden Commit separat geprüft werden. Eine erfolgreiche CI ist **kein** Ersatz für einen echten CIRQA-Test auf dem S25. Der Workflow lädt Debug-APKs als zeitlich begrenzte Artifacts hoch. Derzeit ist kein Gradle-Wrapper im Repository.

## Ausblick

Die nächsten Schritte sind die Gerätevalidierung der bestehenden Stufe-1-Funktionen und die Auswertung zulässiger passiver CIRQA-Evidenz. Der Stufe-2-Vorschlag bleibt gesperrt, bis die CIRQA-spezifischen Protokoll- und Sicherheitsvoraussetzungen erfüllt und der konkrete Test ausdrücklich freigegeben sind.

**Aktuelle Aufgaben:** [GitHub-Issues](https://github.com/jochenkunkel/pulselibre/issues) · [Audit und Reparaturplan (Draft-PR #1)](https://github.com/jochenkunkel/pulselibre/pull/1). Die Architektur-Review-Datei enthält historische Befunde und ist noch nicht vollständig aktualisiert.

[Projektposter](design/pulselibre-poster.svg)

## Lizenz und Drittanbieter-Provenienz

Eigenständige PulsLibre-Beiträge stehen unter der [Apache License 2.0](LICENSE). Diese Lizenz ändert keine Rechte an Fremdcode. Die vorläufige Prüfung der AGPLv3-Protokollreferenzen und die noch offenen Herkunftsfragen stehen in [LICENSE_AGPL_AUDIT_2026-11-16.md](docs/LICENSE_AGPL_AUDIT_2026-11-16.md). **Die Prüfung ist noch keine vollständige rechtliche Freigabe.**
