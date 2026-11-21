# PulsLibre – gemeinsames Agenten-Aktivitätslog

Dieses Verzeichnis ist das verbindliche, versionierte **Arbeitsregister** aller tatsächlich aktiven Coding- und Entwicklungsagenten. Die Regeln gelten für Codex, Claude Code, OpenCode, ChatGPT und weitere beauftragte Agenten. **Ein Agent gilt nur dann als aktiv, wenn er seine aktuelle Sitzung tatsächlich eingetragen hat.** Historische Zuständigkeiten sind keine Aktivitätsnachweise.

## Rollen und Quellen

- Die gültige Rollenverteilung steht in [AGENTS.md](../../../AGENTS.md). Eine konkrete Issue-Übernahme oder ausdrücklich erteilte Aufgabe hat Vorrang vor einer allgemeinen Rollenbeschreibung.
- Abstimmungsnachrichten und Übergaben stehen weiterhin in [agent-sync](../agent-sync/INDEX.md). Das Aktivitätslog **ersetzt keine** Nachricht an andere Agenten.
- GitHub-Issues/PRs dokumentieren fachliche Aufgaben und Ergebnisse; dieses Log zeigt **wer gerade woran arbeitet**.
- Codex und Claude Code können alternative Zugänge derselben VSCodium-IDE sein; **nicht automatisch zwei gleichzeitig aktive Agenten**. Bei einem Wechsel die bisherige Sitzung schließen und eine neue eröffnen.

## Dateien

Für jede aktive Sitzung eine eigene Markdown-Datei anlegen:

`docs/collaboration/agent-activity/sessions/YYYY-MM-DD_HHMM_<agent>_<kurzthema>.md`

Beispiel: `2026-11-20_0830_codex_hybrid-ci.md`. Verwende UTC oder eine explizit genannte Zeitzone. **Keine gemeinsame Tabelle bei jedem Fortschritt umschreiben**: Das vermeidet Merge-Konflikte zwischen Agenten.

## Verpflichtender Ablauf

1. **Vor Beginn:** aktuellen `origin/main`, `AGENTS.md`, diesen Leitfaden, die relevanten Issues/PRs und offene Nachrichten aus `agent-sync/INDEX.md` lesen. Bereits vorhandene Sitzungsdateien und offene Übernahmen prüfen.
2. **Registrieren:** eigene Sitzungsdatei mit `Status: aktiv`, Arbeitsauftrag, Ticket-IDs, nächsten geplanten Tickets, betroffenen Dateien/Bereichen und Abstimmungsbedarf erstellen und zeitnah ins Repository übertragen. Erst danach in gemeinsam beanspruchte Bereiche eingreifen.
3. **Während der Arbeit:** bei Ticketwechsel, neuem Konflikt, wesentlichem Fortschritt oder Blockade den **eigenen** Eintrag aktualisieren. Keine stündlichen Pflichtmeldungen und keine Unterbrechung der eigentlichen Arbeit für triviale Zwischenstände.
4. **Abstimmen:** Überschneidungen vor Änderungen klären; dafür eine adressierte Nachricht unter `agent-sync/` erstellen und im Log darauf verlinken. **Keine doppelte Ticketübernahme** ohne explizite Vereinbarung.
5. **Abschließen/Übergeben:** Status `abgeschlossen`, `pausiert` oder `blockiert` setzen; letzte Tätigkeit, Commits/PRs, tatsächlich durchgeführte Tests, offene Fragen und nächsten sinnvollen Schritt festhalten.
6. **Wiederaufnahme:** alte Sitzung nicht als aktiv interpretieren; bei neuer Sitzung eine neue Datei erstellen oder die eigene noch laufende Sitzung mit Datum und aktualisiertem Status fortsetzen.

## Vorlage für eine Sitzungsdatei

```markdown
# <Agent> – <Auftrag>
- Agent/Umgebung: Codex / VSCodium (Beispiel)
- Beginn: YYYY-MM-DD HH:MM UTC
- Letzte Aktualisierung: YYYY-MM-DD HH:MM UTC
- Status: aktiv | pausiert | blockiert | abgeschlossen
- Rolle/Zuständigkeit: ...
- Aktueller Auftrag: ...
- Jetzt in Bearbeitung: #123 – konkreter Teilumfang
- Zeitnah geplant: #124, #125 (Planung, **keine** exklusive Reservierung)
- Betroffene Bereiche/Dateien: ...
- Branch/Worktree: ...
- Abstimmungsbedarf: keiner | <konkrete Frage + zuständiger Agent>
- Agent-sync-Nachrichten: <Links oder Dateipfade>
- Nachweise: <Commits, PRs, CI-Läufe, Tests oder 'noch keine'>
- Nächster Schritt: ...
```

## Verbindliche Regeln

- **Aktiv**, **geplant**, **blockiert** und **abgeschlossen** sauber unterscheiden. Nur eine bestätigte, dokumentierte Übernahme beansprucht ein Ticket; eine Planung blockiert keine anderen Agenten.
- Keine Behauptung einer aktiven Sitzung oder eines Testerfolgs ohne Nachweis.
- Nur die **eigene** Sitzungsdatei bearbeiten; fremde Einträge nicht überschreiben. Veraltete fremde Aktivität über `agent-sync` klären.
- Für aktuelle CIRQA-Testarmband-Rohdaten gilt die [temporäre Forschungsdaten-Freigabe](../CURRENT_BAND_RAW_DATA_POLICY.md): lokale und agenteninterne Auswertung ist ausdrücklich erwünscht. In öffentlichen Sitzungslogs weiterhin keine identifizierenden Gerätekennungen, Geheimnisse oder ungeprüften Rohdaten veröffentlichen.
- Bei parallelen Arbeiten getrennte Branches/Worktrees und die Sicherheits-/CI-Regeln aus `AGENTS.md` einhalten.
- **Autonom arbeiten:** Nur bei echten Sicherheitsfreigaben, nicht auflösbaren Konflikten, fehlenden Berechtigungen oder erforderlichen Nutzerentscheidungen rückfragen. Ansonsten eigenständig implementieren, testen, dokumentieren und weiterarbeiten.
