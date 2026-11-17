# CIRQA-Protokollentwicklung – Dokumentationsindex

**Kanonische Hauptspezifikation:** [CIRQA_PROTOCOL_SPEC.md](CIRQA_PROTOCOL_SPEC.md).

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
- **Protokoll-Experiment ≠ Geräte-Testbericht:** Reine Analyse und Reverse Engineering stehen hier; vollständige Ausführungsnachweise werden in [Testnachweise und Verifikation](../test-evidence/README.md) katalogisiert.
- **Datenschutz:** keine Gerätekennungen, privaten Messwerte oder Rohpayloads in GitHub.

## Migration und Verlinkung

Am 2026-11-17 wurden **23 vorhandene Protokoll-/Forschungs-/Sync-Dokumente** in diese Unterordner kopiert und die bisherigen `docs/<Dateiname>`-Pfade zu Weiterleitungen umgewandelt. Die ursprünglichen Inhalte wurden nicht zusammengekürzt. Zusätzlich bleibt die Hauptspezifikation direkt unter `docs/protocol/`.

**Achtung bei relativen Links:** Die Dokumente wurden zunächst inhaltlich unverändert übernommen. Etwaige relative Links oder Asset-Pfade innerhalb der verschobenen Originaltexte müssen gesondert geprüft und auf die neue Verzeichnistiefe angepasst werden. Der Umzug ist **kein** Beleg für einen vollständig bestandenen Linkcheck.

## Weitere Dokumente im alten Pfad

Einige Geräte-Testberichte und weitere CIRQA-Notizen liegen noch unter `docs/`; die gesonderte Kategorie [Testnachweise und Verifikation](../test-evidence/README.md) enthält dazu eine Übersicht. Bei weiterer Migration erst Verweise/Code-Referenzen prüfen, dann mit Kompatibilitätsweiterleitung verschieben.

## Ergänzung: CIRQA-Restmigration vom 2026-11-17

Alle 23 zuvor noch vollständig unter `docs/CIRQA_*.md` liegenden Fachtexte wurden ohne Inhaltsänderung nach `docs/protocol/` verschoben:

- [device-tests/](device-tests/) – 11 datierte Gerätetests, Mess- und Beobachtungsberichte. Sie sind zugleich im fachübergreifenden Bereich [Testnachweise](../test-evidence/README.md) einzuordnen.
- [data-model/](data-model/) – 4 Dokumente zu Datenfluss, SQLite, Dashboard- und Schlaf-/Bewegungsgrundregeln; wissenschaftliche Interpretation bleibt mit [science](../science/README.md) abzustimmen.
- [research/](research/) – 4 weitere Untersuchungen, u. a. Gadgetbridge und 5050-Rohframes.
- [experiments/](experiments/) – 4 weitere Versuchsvorbereitungen, Filelist- und Testlabor-Analysen.

Die ursprünglichen `docs/CIRQA_*.md`-Pfade bleiben als kurze Weiterleitungsdateien bestehen. **Wichtig:** In relativen Links innerhalb der unverändert übernommenen Originaltexte kann noch die alte Verzeichnistiefe stecken; ein vollständiger Linkcheck steht aus. Verweise auf solche alten Pfade funktionieren als Einstieg, aber relative Links aus neuem Standort müssen gesondert überprüft werden.
