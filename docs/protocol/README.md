# CIRQA-Protokollentwicklung – Dokumentationsindex

**Kanonische Hauptspezifikation:** [CIRQA_PROTOCOL_SPEC.md](CIRQA_PROTOCOL_SPEC.md).

**Kuratierter Einstieg (24.11.2026):** [BLE-Protokoll, Feldkatalog, Rohdaten→SQLite und Geräte-Evidenz](../knowledge/00_INDEX.md). Die verdichteten Seiten verweisen auf die Primärquellen; sie ersetzen keine Testberichte.

**Grafischer Einstieg:** [CIRQA-BLE-Sequenz, Transportentscheidung, FIT-Pipeline und Architektur](../architecture/diagrams/README.md).

## Fachliche Unterbereiche

| Ordner | Zuständigkeit | Beispiele |
| --- | --- | --- |
| [specifications/](specifications/) | Nachrichtenformate und dokumentierte Service-/FileSync-Verträge | Service Discovery, Legacy FileSync, 5050-ACK-Format |
| [research/](research/) | Protokollrecherche, Reverse Engineering und Quellenvergleiche | Garmin-Protokollrecherche, 5050 Capabilities, Garmin Connect 5.3.1 |
| [sync/](sync/) | Historische Datenübertragung und Freigabe-/Abbruchbedingungen | History Sync, File Transfer, Download Release Gate |
| [experiments/](experiments/) | datierte technische Untersuchungen, Hypothesen und beobachtete ACKs | 5024 ACK Wire Audit, Post-5050-Evidenz, A/B-Sicherheitsprüfung |

## Abgrenzung und Nachweisstatus

- **Spezifikation ≠ Hypothese:** Nicht jeder Text unter `specifications/` ist am Gerät validiert; Verifikationsstatus im Dokument beachten.
- **Forschung ≠ Implementierung:** Protokollvergleiche sind keine Freigabe zum Senden von GATT-/FileSync-Kommandos.
- **ACK ≠ Download:** Beobachtete 5024/5050-ACKs belegen nicht, dass historische FIT-Dateien erfolgreich gelesen wurden.
- **Protokoll-Experiment ≠ Geräte-Testbericht:** Reine Analyse und Reverse Engineering stehen hier; vollständige Ausführungsnachweise werden in [Testnachweise und Verifikation](../quality/evidence/README.md) katalogisiert.
- **Datenschutz:** keine Gerätekennungen, privaten Messwerte oder Rohpayloads in GitHub.

## Migration und Verlinkung

Am 24.11.2026 wurde die Dokumentation thematisch gruppiert: Protokollmaterial liegt hier; CI-/Testnachweise unter `quality/`, Projektführung unter `project/`, UI und Handbücher unter `product/`. Zusätzlich wurden auf Nutzeranweisung BLE-Rohmitschnitte aus dem aktuellen `docs/`-Baum entfernt; redigierte Analyseberichte bleiben. Interne Markdown-Verweise wurden soweit geprüft auf die neue Struktur umgestellt; alte direkte Dateipfade können sich ändern.

## Ergänzung: CIRQA-Restmigration vom 2026-11-17

Die CIRQA-Fachtexte wurden thematisch nach `docs/protocol/` und dessen Unterordnern gruppiert:

- [device-tests/](device-tests/) – 11 datierte Gerätetests, Mess- und Beobachtungsberichte. Sie sind zugleich im fachübergreifenden Bereich [Testnachweise](../quality/evidence/README.md) einzuordnen.
- [data-model/](data-model/) – 4 Dokumente zu Datenfluss, SQLite, Dashboard- und Schlaf-/Bewegungsgrundregeln; wissenschaftliche Interpretation bleibt mit [science](../science/README.md) abzustimmen.
- [research/](research/) – 4 weitere Untersuchungen, u. a. Gadgetbridge und 5050-Rohframes.
- [experiments/](experiments/) – 4 weitere Versuchsvorbereitungen, Filelist- und Testlabor-Analysen.

Für neue Links ausschließlich die kanonischen Unterordner verwenden.

**Garmin-Connect-APK/DEX-Analyse und rechtliche Leitplanken:** [Binary-Analyse zur Interoperabilität](research/GARMIN_CONNECT_BINARY_ANALYSIS.md). Technische Befunde und die Voraussetzungen aus §§ 69d/69e UrhG sind getrennt dokumentiert; keine proprietären Garmin-Binärdateien einchecken.

## Aktueller Einstieg für den historischen CIRQA-Download (18.11.2026)

**Zuerst lesen:** [Kanonischer Forschungs- und Evidenzstand](research/CIRQA_HISTORICAL_RESEARCH_STATUS_2026-11-18.md). Er unterscheidet bestätigte CIRQA-Hardwarebefunde, Offline-Code und RFCOMM-Hypothesen und verweist auf die datierten Primärberichte. Ältere Dokumente bleiben als Evidenz erhalten und sind **keine aktuellen Handlungsanweisungen**.

**Navigationsregel:** Neue Erkenntnisse zunächst in diesem Forschungsstand mit Quellen-/Gerätenachweis zusammenfassen; detaillierte Experimente weiterhin unter `experiments/`, Gerätetests unter `device-tests/`, stabile Formate unter `specifications/`, Sync-Zielarchitektur unter `sync/`. Keine parallelen, widersprüchlichen „aktueller Stand“-Dokumente anlegen.
