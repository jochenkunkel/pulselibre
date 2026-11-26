# PulsLibre

**Geplante Wissenschaftsnavigation:** eigener Hauptreiter [„Analysen“ (#227)](https://github.com/jochenkunkel/pulselibre/issues/227), [Sitemap und Modulgruppen](docs/science/ANALYSEN_NAVIGATION_CONCEPT.md); **noch nicht als App-Reiter implementiert**.

**Anwenderinnen und Anwender:** [Hilfe, Benutzerhandbuch und Fehlerbehebung](docs/product/user/README.md).

**Neuer Chat oder Entwicklungsagent?** Zuerst [CHAT_START_HERE.md](docs/project/CHAT_START_HERE.md) lesen, danach den [zentralen Projektstatus](docs/project/PROJECT_STATUS.md) und die offenen GitHub-Issues prüfen.

**Offline-first Android-Projekt für Garmin CIRQA auf Samsung Galaxy S25.**

![PulsLibre-Logo](design/pulselibre-icon.svg)

![PulsLibre – Deine Gesundheitsdaten. Deine Kontrolle. Unsere Philosophie und Vision](design/pulselibre-vision-poster-2026.svg)

*Unsere Vision: Selbstbestimmung, Offline-first, Transparenz, Wissenschaft und Sicherheit. [Poster als SVG ansehen](design/pulselibre-vision-poster-2026.svg). Der begrenzte historische Mehrdateiabruf ist durch V24/V25 nachgewiesen; die vollständige historische Abdeckung bleibt offen.*

**Projektstatus (18.11.2026):** Die CIRQA-Synchronisation und der begrenzte Mehrdateiabruf sind am Gerät nachgewiesen: V24/V25 übertrugen jeweils 33 gelistete Dateien (31 FIT, 2 Nicht-FIT). Die 31 FIT-Dateien wurden auf Länge, Header, CRC und SHA-256 geprüft; der SQLite-Import enthält 294 Metrikzeilen und wurde bei V25 ohne Duplikate wiederholt. **Nicht nachgewiesen** sind die vollständige historische Abdeckung, eine vollständige Feldinventur und die Validierung weiterer Gesundheitsmetriken. Frühere Legacy-Directory- und RFCOMM-Fehlversuche sind historische Zwischenstände. Die App ist **keine produktionsreife Garmin-Connect-Alternative**. Details und Grenzen: [aktueller Forschungsstand](docs/protocol/research/CIRQA_HISTORICAL_RESEARCH_STATUS_2026-11-18.md).

**Für die Haupt-IDE VSCodium/Codium (Claude Code oder Codex als alternativ genutzter API-Zugang) und weitere Mitwirkende:** [AGENTS.md](AGENTS.md) beschreibt die gemeinsamen Arbeitsregeln, Branch-/Review-Absprachen und Sicherheitsgrenzen. Für die aktuelle CIRQA-Evidenz gilt [PROJECT_STATUS.md](docs/project/PROJECT_STATUS.md); ältere Protokollnotizen sind historische Beobachtungen.

**Für neue Mitwirkende:** [Leitfaden zur Mitarbeit](docs/project/CONTRIBUTING_GUIDE.md) – Einstieg, Arbeitsbereiche, Qualitätsanforderungen und Sicherheitsregeln.

**Technische Abläufe als Bilder:** [Vier illustrierte Diagramme zu BLE, Transportforschung, FIT und Architektur](docs/architecture/diagrams/README.md).

## PulsLibre auf einen Blick

![PulsLibre – Deine Daten. Deine Kontrolle. Bluetooth, FIT-Prüfung, lokale SQLite-Daten und transparente Analysen](design/pulselibre-workflow-poster-2026.svg)

*Der dargestellte Ablauf ist eine schematische Projektillustration, kein Screenshot und kein Nachweis einer vollständig funktionierenden Gerätehistorie. Der begrenzte CIRQA-Dateiabruf und die lokale FIT-Verarbeitung sind dokumentiert; vollständige historische Abdeckung und sämtliche Messgrößen bleiben in Prüfung. [Poster als SVG öffnen](design/pulselibre-workflow-poster-2026.svg) · [Evidenz und Projektstatus](docs/project/PROJECT_STATUS.md).*

## Dokumentation nach Fachbereichen

**[Dokumentationswegweiser](docs/README.md)** · [UI-Konzept](docs/product/ui/README.md) · [CIRQA-Protokollentwicklung](docs/protocol/README.md) · [Wissenschaftliche Messwerte und Berechnungen](docs/science/README.md). Die früheren Markdown-Weiterleitungen wurden nach der Linkmigration entfernt; maßgeblich sind die kanonischen Pfade im Dokumentationswegweiser.

## Einstieg und Dokumentation

**Für alle Chats und Entwicklungsagenten verbindlicher Einstieg:** [Zentraler Projektstatus und Übergabeprotokoll](docs/project/PROJECT_STATUS.md). Änderungen und neue Erkenntnisse aus anderen Chats müssen dort explizit dokumentiert werden; Chatverläufe synchronisieren sich nicht automatisch.

- [Architektur, Code-Review und Ablaufdiagramme](docs/archive/2026-11-24/ARCHITECTURE_AND_CODE_REVIEW.md)
- [Priorisierte Roadmap bis zu echten CIRQA-Messwerten](docs/archive/2026-11-24/ROADMAP_TO_CIRQA_DATA.md)
- [Android-Gerätekompatibilität, Updates und Play-Store-Vorbereitung](docs/product/release/ANDROID_RELEASE_READINESS.md)
- [Geräte, Einschränkungen und Sicherheitsregeln](docs/project/TARGET_DEVICES.md)
- [Garmin-Protokollrecherche](docs/archive/2026-11-24/protocol/research/GARMIN_PROTOCOL_RESEARCH.md)
- [S25-Testplan](docs/protocol/device-tests/S25_BLE_TEST_PLAN.md)
- [Projektziele und Anforderungen](docs/project/REQUIREMENTS_AND_PRINCIPLES.md)
- [Qualitätsanforderungen](docs/quality/QUALITY_GATE.md)
- [Design und Branding](docs/product/BRANDING.md)

## Was derzeit tatsächlich funktioniert

- Android-Laufzeitberechtigung für Bluetooth beim ersten Start; bei bereits erteilter Berechtigung kein erneuter Dialog.
- Benutzerinitiierter, auf 60 Sekunden begrenzter **passiver** BLE-Scan; Werbeinformationen und vorsichtige Herstellerhinweise.
- Nur lesende Android-Abfrage zu CIRQA-Kopplung und Android-GATT-Status; die verbundene App ist **unbekannt**.
- Export einer Diagnose **nur nach ausdrücklicher Teilen-Aktion**. Vorsicht: Der Text kann Gerätebezeichnungen enthalten; rohe BLE-Service-Daten werden im Standardbericht nicht mitgeteilt.
- Experimentelle, ausdrücklich vom Nutzer gestartete GATT-Dienstsuche und ML-Probe einschließlich Vordergrunddienst für wiederholte Versuche. Die Existenz dieses Codes belegt keine erfolgreich ausgelesenen CIRQA-Gesundheitswerte; mögliche Wechselwirkungen mit Garmin Connect müssen am Gerät geprüft werden.
- Offline-Datenmodell, lokale SQLite-Ablage mit Transaktionen und CSV-Export. V24/V25 haben einen begrenzten CIRQA-Dateiimport mit 294 SQLite-Metrikzeilen nachgewiesen; daraus folgt keine vollständige historische Abdeckung oder medizinische Validierung.
- Offline-Inspektion von GFDI-Headern und CRC-16-Prüfung sowie ein begrenzter Offline-FIT-Decoder (`FitHistoricalObservations`) für ausgewählte Standardfelder mit zugehörigen Unit-Tests. Dies belegt weder einen CIRQA-Historiendownload noch einen Geräteimport oder einen erfolgreichen CI-Lauf.
- CIRQA-Testlabor mit passivem Scan, Android-Verbindungsstatus, begrenzter read-only GATT-Dienstsuche, ausdrücklich bestätigter vorhandener Live-Messwertprobe und datensparsamem Testbericht. V24/V25 belegen einen begrenzten historischen Mehrdateiabruf; neue aktive/proprietäre Testbefehle benötigen eine gesonderte Freigabe; passive, offline und nichtdestruktive lesende Prüfungen dürfen Agenten ohne wiederholte Einmal-Gates durchführen (siehe AGENTS.md).
- Offline FIT-Envelope-/CRC-Prüfung und begrenzte Dekodierung ausgewählter Garmin-Profilfelder. SAF-Dateiauswahl, keine allgemeine Dateiberechtigung, keine Kopie oder Datenbankübernahme. Dies bestätigt weder CIRQA-Herkunft noch einen CIRQA-Dateidownload.

## Sicherheitsgrenzen

Keine INTERNET-Berechtigung, kein Cloud-Sync, keine Telemetrie, keine erfundenen Gesundheitswerte. Aktive GATT-Verbindungsversuche und ein experimenteller ML-Handshake sind implementiert und dürfen nur nach ausdrücklicher Nutzeraktion starten. Die separate reine GATT-Dienstsuche schreibt und abonniert nicht; dies darf nicht pauschal auf den ML-Probe-Pfad übertragen werden. Keine automatische Neu-Kopplung behauptet. Die Android-Systemanzeige „GATT verbunden“ ist **kein** Beweis für Datenzugriff durch PulseLibre.

## Build und Tests

Lokale Build-Anleitung: JDK 17 (historischer lokaler Hinweis), Android SDK 37 (compileSdk), Gradle 9.6.0. Der verbindliche GitHub-Actions-Workflow `.github/workflows/android.yml` verwendet **JDK 21**; das **Java/Kotlin-Bytecode-Ziel 17** ist davon zu unterscheiden. Für reproduzierbare CI-Parität lokal bevorzugt JDK 21 verwenden. Beispiel:

```bash
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

Starte **Android CI** in GitHub unter **Actions → Android CI → Run workflow** oder per `gh workflow run android.yml --ref main`. Dort sind zwei unabhängige Eingaben verfügbar:

- **Ausführungsort:** `LOCAL` (eigener Self-hosted Runner, Label `pulselibre`) oder `CLOUD` (GitHub-hosted `ubuntu-latest`).
- **Testumfang:** `QUICK` (Build, R8-Release, Unit-Tests, APK-Diagnose) oder `FULL` (zusätzlich Lint, Instrumentation-Test-APK und API-35-Emulator).

Pushes auf `main` und Pull Requests gegen `main` laufen automatisch mit `QUICK`; der tägliche Zeitplanlauf verwendet `FULL`. Manuelle Läufe können `QUICK` oder `FULL` wählen. Pull Requests bleiben auf GitHub-hosted Runnern, solange der lokale Runner für vertrauenswürdige PRs nicht ausdrücklich freigegeben ist. Beide Umgebungen nutzen dieselbe Pipeline (`tools/ci/run-ci.sh`), damit LOCAL und CLOUD nicht auseinanderlaufen. Die Android-CI lädt APKs und Testberichte als zeitlich begrenzte Artifacts hoch.

Dieselben Prüfungen laufen auch ohne GitHub direkt auf dem Entwicklungsrechner:

```bash
./tools/ci-local.sh --quick   # Build + Unit-Tests + APK-Diagnose
./tools/ci-local.sh --full    # zusätzlich Lint, Test-APK und API-35-Emulator
```

Voraussetzungen, Runner-Installation, Sicherheitsregeln, Umschalten zwischen LOCAL und CLOUD sowie Troubleshooting stehen in [docs/quality/ci/HYBRID_CI.md](docs/quality/ci/HYBRID_CI.md).

## Ausblick

Nächster Forschungsschwerpunkt ist die abgesicherte Klärung des Bluetooth-Classic-/RFCOMM-Transportwegs. Der erste begrenzte RFCOMM-Versuch ergab keine Gegenverbindung; weitere Geräteversuche benötigen eine eigene Freigabe. Für neue Mitwirkende sind [Dokumentationswegweiser](docs/README.md), [Entwicklungsregeln](AGENTS.md) und [aktueller CIRQA-Forschungsstand](docs/protocol/research/CIRQA_HISTORICAL_RESEARCH_STATUS_2026-11-18.md) die besten Einstiege.

**Aktuelle Aufgaben:** [GitHub-Issues](https://github.com/jochenkunkel/pulselibre/issues) · [Historischer Audit und Reparaturplan (geschlossener, nicht gemergter PR #1)](https://github.com/jochenkunkel/pulselibre/pull/1). Die Architektur-Review-Datei enthält historische Befunde und ist noch nicht vollständig aktualisiert.

[Aktuelles Visionsposter](design/pulselibre-vision-poster-2026.svg) · [Historisches Projektposter](design/pulselibre-poster.svg)

## Lizenz und Drittanbieter-Provenienz

Eigenständige PulsLibre-Beiträge stehen unter der [Apache License 2.0](LICENSE). Diese Lizenz ändert keine Rechte an Fremdcode. Die vorläufige Prüfung der AGPLv3-Protokollreferenzen und die noch offenen Herkunftsfragen stehen in [LICENSE_AGPL_AUDIT_2026-11-16.md](docs/project/compliance/LICENSE_AGPL_AUDIT_2026-11-16.md). **Die Prüfung ist noch keine vollständige rechtliche Freigabe.**
