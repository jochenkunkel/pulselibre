# PulsLibre – Dokumentationswegweiser

Dieser Ordner ist nach Arbeitsbereichen gegliedert. Die oberste Ebene enthält nur Themenordner und diesen Wegweiser; neue Inhalte gehören in den passenden Bereich statt in den Stamm.

## Einstieg

- [Aktueller Projektstatus](project/PROJECT_STATUS.md)
- [Start für Mitwirkende und Agenten](project/CHAT_START_HERE.md)
- [Kuratierte CIRQA-Wissensbasis](knowledge/00_INDEX.md)
- [Arbeitsregeln](../AGENTS.md)
- [Beitragsleitfaden](project/CONTRIBUTING_GUIDE.md)

## Verzeichnisbaum

| Ordner | Inhalt und Einstieg |
|---|---|
| [`project/`](project/README.md) | Projektstatus, Historie, Anforderungen, Mitarbeit und Compliance |
| [`product/`](product/README.md) | App, UI, Bedienung, Datenexport und Release-/Update-Hinweise |
| [`protocol/`](protocol/README.md) | CIRQA/BLE, FIT, Sync, Datenmodell, Geräteversuche und Protokollforschung |
| [`science/`](science/README.md) | Messwerte, wissenschaftliche Methoden, Referenzen und Analysen |
| [`quality/`](quality/README.md) | Qualitätsregeln, Testpläne, CI und Testnachweise |
| [`collaboration/`](collaboration/README.md) | Agentenabstimmung, Sitzungsnachweise und verbindliche Kollaborationsregeln |
| [`knowledge/`](knowledge/00_INDEX.md) | Kompakte thematische Wissensbasis mit Quellenverweisen |
| [`architecture/`](architecture/README.md) | Architekturübersicht, technische Diagramme und geparkte Architekturprüfung |
| [`archive/`](archive/2026-11-24/README.md) | Historische Dokumente; nicht mehr aktuelle Arbeitsanweisung |

## Pflegeprinzipien

- Pro Thema gibt es einen kanonischen Einstieg; Fachseiten verlinken untereinander und auf Primärbelege.
- Aktuelle Vorgaben und historische Evidenz klar trennen. Bei Konflikten gelten Code/CI, aktuelle Projektregeln und belegte Testergebnisse vor alten Notizen.
- Keine privaten Gerätekennungen, Tokens, Rohdaten oder personenbezogenen Gesundheitsdaten in die Dokumentation aufnehmen.
- Historische Dokumente werden nur mit überprüftem Inhalt und Zweck archiviert. Erforderliche Rohmitschnitte sind davon ausgenommen und werden nicht in `docs/` abgelegt.
- Ordner dürfen Unterordner für große Themenbereiche enthalten. Der `docs/`-Stamm bleibt frei von Fachdateien.
