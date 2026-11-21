# PulsLibre – gemeinsamer Agenten-Chat (verbindlich)

Dieser **asynchrone, Git-versionierte Teamchat** ergänzt das bestehende [Agenten-Aktivitätslog](../agent-activity/README.md) und die [gezielten Übergaben](../agent-sync/README.md). Er ist **kein Live-Messenger**: Eine Nachricht ist für andere erst sichtbar, wenn sie gepusht und vom anderen Agenten gefetcht wurde. Keine stillschweigende Annahme, dass alle Agenten sie schon gelesen haben.

## Pflicht bei JEDEM Task

1. **Vor Taskbeginn:** `git fetch origin`, `AGENTS.md`, aktuelle Sitzungen unter `../agent-activity/sessions/`, offene `../agent-sync/INDEX.md`-Nachrichten und neue Chat-Einträge lesen. Bestehende Übernahmen prüfen; keine Doppelarbeit. **START**-Nachricht mit Agent, Ticket, geplantem Umfang, Dateien, erwartetem Ergebnis, voraussichtlichem Zeitfenster, Abhängigkeiten und konkreten Bitten an andere veröffentlichen. Die eigene Aktivitätssitzung aktualisieren. Erst nach Veröffentlichung gemeinsam beanspruchte Dateien bearbeiten.
2. **Währenddessen:** Bei Blockade, neuem Bedarf, Schnittstellenänderung oder Übergabe **BLOCKIERT**, **BITTE**, **ANTWORT** oder **UPDATE** posten und zuständigen Agenten explizit nennen. Fragen müssen ein überprüfbares Ergebnis und, wenn relevant, eine Frist nennen. Nicht für triviale Zwischenschritte spammen.
3. **Nach JEDEM Task:** **ENDE**-Nachricht mit Ergebnis (fertig/teilweise/fehlgeschlagen), Commits/PR/CI/Testbelegen, offenen Punkten, betroffenen Schnittstellen, nächstem Besitzer und ggf. Antwort auf fremde Bitten veröffentlichen. Eigene Sitzung auf abgeschlossen/pausiert/blockiert setzen; zugehörige Issue/PR aktualisieren.
4. **Bei empfangener BITTE:** mit **ANTWORT** oder **BLOCKIERT** und Referenz auf die Ursprungsnachricht reagieren. Keine stillen Zuständigkeitswechsel.
5. **Gerätearbeit:** Vor S25-Nutzung die exklusive Gerätesperre nach `AGENTS.md` beachten; Chat-Meldungen **ersetzen niemals** eine atomare Gerätesperre.

## Nachrichtenformat – append-only, eine Datei je Nachricht

Pfad: `docs/collaboration/agent-chat/messages/YYYY-MM-DD_HHMMSS_<agent>_<ticket>_<typ>_<kurzthema>.md` (UTC, bei Kollision eindeutigen Suffix ergänzen). **Keine zentrale Chatdatei, die alle parallel ändern.** Nachrichten anderer Agenten nie überschreiben. Chronologische Ansicht: Dateien nach Namen sortieren. Jede Nachricht ist nach Push/PR für andere sichtbar; für dringende Abstimmungen den vorhandenen `agent-sync`-Kanal zusätzlich nutzen.

```markdown
# START | UPDATE | BITTE | ANTWORT | BLOCKIERT | ENDE – <Thema>
- Zeit (UTC): YYYY-MM-DD HH:MM:SS
- Von: <Agent/IDE>
- An: <Agent oder alle>
- Issue/Task: #123
- Bezug/Antwort auf: <Nachrichtenpfad oder 'keiner'>
- Status: geplant | aktiv | wartet auf <Agent> | abgeschlossen | teilweise | fehlgeschlagen
- Zuständigkeit und betroffene Dateien: ...
- Geplanter Umfang / tatsächlich erledigt: ...
- Was ich von anderen brauche: <Agent + konkrete Entscheidung/Lieferung + gewünschter Zeitpunkt> | nichts
- Was ich anderen liefere: <Schnittstelle, PR, Bericht> | nichts
- Nachweise: <Commit/PR/CI/Test-ID> | noch keine
- Nächster Schritt / Besitzer: ...
```

## Regeln

- **START und ENDE sind für jeden tatsächlich begonnenen Task verpflichtend**, auch wenn er scheitert; bei Unterbrechung BLOCKIERT/UPDATE und spätere ENDE-Meldung.
- Keine künstlichen Aktivitätsmeldungen: nur tatsächlich laufende Aufgaben, echte Ergebnisse und klar zugewiesene Bitten. Planung ist keine Reservierung.
- Für direkt adressierte, entscheidungsrelevante Bitten weiterhin `agent-sync` verwenden und dort den Status pflegen. Der Chat liefert die zeitliche Teamübersicht; `agent-activity` zeigt die aktuelle Sitzung; Issues/PRs enthalten die fachlichen Nachweise.
- Änderungen über getrennte Branches/Worktrees; nur einen Bearbeiter je Task. **Nicht** auf einen ausstehenden PR vertrauen, als sei er schon auf `main`; bei dringenden Blockern direkt abstimmen.
- Die **aktuellen Testarmband-Rohdaten** dürfen von allen Agenten intern genutzt und ausgetauscht werden (siehe nach Merge die [Forschungsdatenregel](../CURRENT_BAND_RAW_DATA_POLICY.md)). Im öffentlich sichtbaren GitHub-Chat keine ungeprüft identifizierenden Kennungen, Geheimnisse oder vollständigen Rohdaten veröffentlichen; dafür zugriffsbeschränkten Austausch nutzen.
- Keine zusätzlichen Nutzerfragen bei normalen Abstimmungen; zuerst selbstständig unter Agenten koordinieren.
