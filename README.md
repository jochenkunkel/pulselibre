# PulsLibre

**Neuer Chat oder Entwicklungsagent?** Zuerst [CHAT_START_HERE.md](docs/CHAT_START_HERE.md) lesen, danach den [zentralen Projektstatus](docs/PROJECT_STATUS.md) und die offenen GitHub-Issues prüfen.

**Offline-first Android-Projekt für Garmin CIRQA auf Samsung Galaxy S25.**

![PulsLibre-Logo](design/pulselibre-icon.svg)

**Projektstatus (18.11.2026):** Die CIRQA-Bootstrap-/Handshake-Kette bis zur Legacy-Directory-Abfrage wurde am Gerät nachgewiesen. Über BLE erscheint bislang lediglich ein 32-Byte-Platzhalterverzeichnis; ein vollständiger historischer FIT-Download ist **nicht** bestätigt. Ein erster begrenzter RFCOMM-Versuch erhielt keine Gegenverbindung. Die App enthält experimentelle, ausdrücklich gestartete Bluetooth-Testpfade und ist **keine produktionsreife Garmin-Connect-Alternative**. Details und Grenzen: [aktueller Forschungsstand](docs/protocol/research/CIRQA_HISTORICAL_RESEARCH_STATUS_2026-11-18.md).

**Für Codex in VSCodium und weitere Coding-Agenten:** [AGENTS.md](AGENTS.md) beschreibt die gemeinsamen Arbeitsregeln, Branch-/Review-Absprachen und Sicherheitsgrenzen. Für die aktuelle CIRQA-Evidenz gilt [PROJECT_STATUS.md](docs/PROJECT_STATUS.md); ältere Protokollnotizen sind historische Beobachtungen.

**Für neue Mitwirkende:** [Leitfaden zur Mitarbeit](docs/CONTRIBUTING_GUIDE.md) – Einstieg, Arbeitsbereiche, Qualitätsanforderungen und Sicherheitsregeln.

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
- CIRQA-Testlabor mit passivem Scan, Android-Verbindungsstatus, begrenzter read-only GATT-Dienstsuche, ausdrücklich bestätigter vorhandener Live-Messwertprobe und datensparsamem Testbericht. Ein erfolgreicher historischer CIRQA-Download ist nicht nachgewiesen; aktive Testpfade unterliegen gesonderten Freigaben und Einmal-Gates.
- Offline FIT-Envelope-/CRC-Prüfung und begrenzte Dekodierung ausgewählter Garmin-Profilfelder. SAF-Dateiauswahl, keine allgemeine Dateiberechtigung, keine Kopie oder Datenbankübernahme. Dies bestätigt weder CIRQA-Herkunft noch einen CIRQA-Dateidownload.

## Sicherheitsgrenzen

Keine INTERNET-Berechtigung, kein Cloud-Sync, keine Telemetrie, keine erfundenen Gesundheitswerte. Aktive GATT-Verbindungsversuche und ein experimenteller ML-Handshake sind implementiert und dürfen nur nach ausdrücklicher Nutzeraktion starten. Die separate reine GATT-Dienstsuche schreibt und abonniert nicht; dies darf nicht pauschal auf den ML-Probe-Pfad übertragen werden. Keine automatische Neu-Kopplung behauptet. Die Android-Systemanzeige „GATT verbunden“ ist **kein** Beweis für Datenzugriff durch PulseLibre.

## Build und Tests

Lokale Build-Anleitung: JDK 17, Android SDK 35, Gradle 8.13. Der GitHub-Actions-Workflow `.github/workflows/android.yml` verwendet dagegen ausdrücklich **JDK 21**. Beispiel:

```bash
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

Starte **Android CI** in GitHub unter **Actions → Android CI → Run workflow** oder per `gh workflow run android.yml --ref main`. Der manuelle Schnelllauf baut die App und führt die JVM-Unit-Tests aus. Aktiviere `full_tests` für Lint, die Instrumentation-APK und API-35-Emulatortests. Pushes, Pull Requests und Tags lösen keinen CI-Lauf aus. Die Android-CI lädt APKs als zeitlich begrenzte Artifacts hoch.

## Ausblick

Nächster Forschungsschwerpunkt ist die abgesicherte Klärung des Bluetooth-Classic-/RFCOMM-Transportwegs. Der erste begrenzte RFCOMM-Versuch ergab keine Gegenverbindung; weitere Geräteversuche benötigen eine eigene Freigabe. Für neue Mitwirkende sind [Dokumentationswegweiser](docs/README.md), [Entwicklungsregeln](AGENTS.md) und [aktueller CIRQA-Forschungsstand](docs/protocol/research/CIRQA_HISTORICAL_RESEARCH_STATUS_2026-11-18.md) die besten Einstiege.

**Aktuelle Aufgaben:** [GitHub-Issues](https://github.com/jochenkunkel/pulselibre/issues) · [Historischer Audit und Reparaturplan (geschlossener, nicht gemergter PR #1)](https://github.com/jochenkunkel/pulselibre/pull/1). Die Architektur-Review-Datei enthält historische Befunde und ist noch nicht vollständig aktualisiert.

[Projektposter](design/pulselibre-poster.svg)

## Lizenz und Drittanbieter-Provenienz

Eigenständige PulsLibre-Beiträge stehen unter der [Apache License 2.0](LICENSE). Diese Lizenz ändert keine Rechte an Fremdcode. Die vorläufige Prüfung der AGPLv3-Protokollreferenzen und die noch offenen Herkunftsfragen stehen in [LICENSE_AGPL_AUDIT_2026-11-16.md](docs/LICENSE_AGPL_AUDIT_2026-11-16.md). **Die Prüfung ist noch keine vollständige rechtliche Freigabe.**
