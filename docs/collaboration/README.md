# Zusammenarbeit, Verantwortlichkeiten und Projektregeln

Hier finden Entwickler, die Haupt-IDE VSCodium/Codium (mit alternativem Claude-Code- oder Codex-Zugang) und weitere Agenten die **Arbeitsvereinbarungen, Qualitätsgrenzen und Übergaberegeln**. Die Dokumente sind verbindlich, soweit sie nicht durch aktuellere explizite Projektentscheidungen ersetzt wurden.

## Verbindliche Einstiege
- [AGENTS.md](../../AGENTS.md): zentrale Repository-Regeln für Coding-Agenten.
- [Autonomer Entwicklungsagent](../project/AUTONOMOUS_DEVELOPMENT_AGENT.md): historischer Grundauftrag; keine wiederkehrende Ausführung ohne tatsächlich eingerichtete Automation oder ausdrücklich gestarteten Agentenlauf.
- [Chat-/Agentenübergabe](../project/CHAT_START_HERE.md): Einstieg ohne lange Chat-Historie.
- [Entwicklungsplan](../archive/2026-11-24/DEVELOPMENT_PLAN.md): Aufgaben, Prioritäten, Risiken.
- [Projektstatus](../project/PROJECT_STATUS.md): datierter aktueller Stand.
- [Qualitätsfreigaben](../quality/QUALITY_GATE.md): Anforderungen vor Installation/Release.
- [Wissenschaftliche Engineering-Standards](../science/ENGINEERING_SCIENTIFIC_STANDARDS.md): Regeln für wissenschaftliche Funktionen.
- [Codex-Dokumentationsprüfung](../archive/2026-11-24/CODEX_DOCUMENTATION_REVIEW_2026-11-17.md): historischer Prüfbericht, keine automatisch aktuelle Freigabe.

- [Agenten-Abstimmung über das Repository](agent-sync/README.md): Nachrichten zwischen Agenten, Index und Regeln (verbindlich ab 2026-11-19).

## Gemeinsamer Agenten-Chat

[Agenten-Chat](agent-chat/README.md): **verbindliche START-/ENDE-Meldungen für jeden Task**, laufende Aufgaben, Status, offene Bitten an andere Agenten und Antworten. Einzelne append-only Dateien vermeiden Konflikte beim parallelen Schreiben. Ergänzt [agent-activity](agent-activity/README.md) und [agent-sync](agent-sync/README.md).

## Exklusiver Zugriff auf das Samsung Galaxy S25

Alle Agenten müssen vor S10-/S25-Zugriff die **hostweit gemeinsame Gerätesperre** gemäß [AGENTS.md](../../AGENTS.md) verwenden. Mit `python3 tools/s25_lease.py status --device s10` bzw. `--device s25` den jeweiligen Lock prüfen; sämtliche Gerätebefehle einer Sitzung müssen in einem gehaltenen Wrapper `python3 tools/s25_lease.py run --device s10 --agent NAME --minutes 40 -- <Befehl>` (S25 entsprechend `--device s25`) laufen. Für S25 bleibt die alte CLI-Form ohne `--device` kompatibel. Details zu Serial-Alias, Statusfeldern und Prozess-Cleanup: [DEVICE_LEASE.md](DEVICE_LEASE.md). Keine parallelen Tests auf demselben Gerät; auf verschiedenen Hosts ist zusätzliche Abstimmung erforderlich.

## Aktuelle Forschungsdaten-Freigabe

[Temporäre CIRQA-Rohdatenregel](CURRENT_BAND_RAW_DATA_POLICY.md): Alle Agenten dürfen die gegenwärtigen, laut Projektverantwortlichem keiner Person zuordenbaren Armband-Rohdaten lokal und agentenintern für Entwicklung und Tests nutzen. Vor öffentlicher Veröffentlichung Identifizierbarkeit prüfen; spätere personenbezogene Betriebsdaten sind **nicht** automatisch freigegeben.

## Zusammenarbeit

**Namensregel:** „Codium“, „VSCodium“, „Claude Code“ und „Codex“ können im Projekt dieselbe Haupt-IDE bezeichnen. Claude Code und Codex sind alternative API-Zugänge, **nicht zwei gleichzeitig aktive IDE-Agenten**. Für technische Test-/Laufnachweise die tatsächlich verwendete API benennen. Maßgebliche aktuelle Zuständigkeiten: [AGENTS.md](../../AGENTS.md).

1. GitHub ist die dauerhafte Quelle für Quellcode, Entscheidungen und Dokumentation.
2. Vor Änderungen relevante Dokumente und Code prüfen; keine widersprüchlichen Parallelimplementierungen.
3. Kleine nachvollziehbare Commits mit Tests; Status *implementiert*, *gebaut*, *getestet*, *freigegeben* strikt trennen.
4. Sicherheits- und Datenschutzgrenzen gelten auch für automatisierte Agenten.
5. Neue Entscheidungen mit Datum, Geltungsbereich und Verweis auf betroffene Spezifikationen festhalten.
6. Fehlende Zugriffe oder unbestätigte Testergebnisse offen benennen.

**Migration:** Bestehende Querschnittsdokumente bleiben vorerst an ihrem bisherigen Ort, damit externe Links und Agentenreferenzen nicht brechen. Neue Zusammenarbeitsdokumente gehören hierher.
