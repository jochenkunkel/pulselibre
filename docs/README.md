# PulsLibre – Dokumentationswegweiser

**Einstieg:** [Projektstatus](PROJECT_STATUS.md) · [Chat-/Agentenübergabe](CHAT_START_HERE.md) · [Entwicklungsplan](DEVELOPMENT_PLAN.md) · [Agentenregeln](../AGENTS.md).

**Neu im Projekt?** [Leitfaden für externe Entwickler](CONTRIBUTING_GUIDE.md) mit Einstieg, Beitragsregeln und Sicherheitsgrenzen.

**Technische Grafiken:** [Ablaufdiagramme und Architekturübersichten](diagrams/README.md) mit Quellen und Evidenzstatus.

## Fachbereiche

| Bereich | Wofür? | Verbindlicher Einstieg |
| --- | --- | --- |
| **UI und Produktdesign** | Screens, Kacheln, Navigation, Barrierefreiheit, Theme | [docs/ui/README.md](ui/README.md) |
| **CIRQA-Protokollentwicklung** | BLE, MLR/GFDI/FIT, History, Versuche und Freigabegrenzen | [docs/protocol/README.md](protocol/README.md) |
| **Wissenschaft und Messwerte** | Metriken, Formeln, Normkontext, Evidenz, Validierung | [docs/science/README.md](science/README.md) |
| **Testnachweise und Verifikation** | Testpläne, Geräteversuche, CI-Ergebnisse und nachvollziehbare Prüfberichte | [docs/test-evidence/README.md](test-evidence/README.md) |
| **Zusammenarbeit und Projektregeln** | Agenten, Zuständigkeiten, Qualitätsvorgaben und Übergaben | [docs/collaboration/README.md](collaboration/README.md) |

## Bereichsübergreifend (vorerst bewusst im Stammverzeichnis)

- [PROJECT_STATUS.md](PROJECT_STATUS.md): aktuelle, datierte Übergabe; ältere Einträge nicht automatisch gültig.
- [DEVELOPMENT_PLAN.md](DEVELOPMENT_PLAN.md): Arbeitsplanung.
- [PROJECT_HISTORY.md](PROJECT_HISTORY.md): historische Chronik, **keine** aktuelle Freigabe.
- [QUALITY_GATE.md](QUALITY_GATE.md), [CI_TEST_STRATEGY.md](CI_TEST_STRATEGY.md), [TESTPLAN.md](TESTPLAN.md): Qualität und Tests.
- [PRIVACY_INCIDENT_2026-11-16.md](PRIVACY_INCIDENT_2026-11-16.md): Datenschutzvorfall; nicht in ein Archiv verstecken.
- [ARCHITECTURE_AND_CODE_REVIEW.md](ARCHITECTURE_AND_CODE_REVIEW.md): Architektur und historische Prüfungen.
- [CIRQA_SQLITE_STORAGE.md](protocol/data-model/CIRQA_SQLITE_STORAGE.md), [LOCAL_STORAGE.md](LOCAL_STORAGE.md): gemeinsame Datenhaltung zwischen Protokoll und UI.

## Umzugsregeln (2026-11-17)

1. **Kanonische Spezifikationen** liegen in Themenordnern; alte Weiterleitungsdateien wurden nach der Linkmigration entfernt.
2. **Protokoll-Experimente** sind Evidenz, nicht automatisch Spezifikation oder Testfreigabe.
3. **Wissenschaftliche Referenzen** sind keine automatisch gültigen klinischen Schwellen.
4. Weitere Dateien erst nach Prüfung von relativen Links, Code-/Testreferenzen, offenen PRs und inhaltlichen Dubletten verschieben. Keine unkontrollierten Massenlöschungen.
5. Für neue Dokumente einen Themenordner verwenden. Im Stamm nur bereichsübergreifende Projektführung.

**Migration (2026-11-17):** 48 bisherige Markdown-Weiterleitungsdateien wurden nach Anpassung der dokumentierten Links entfernt; eine identische doppelte Protokollforschungsdatei ebenfalls. Der Hauptbereich `docs/` enthält weiterhin bereichsübergreifende Dokumente. Alte direkte GitHub-Datei-URLs auf die entfernten Dateien funktionieren nicht mehr; neue Links verwenden die kanonischen Pfade.
