# Dokumentationsabgleich: CIRQA-Historie und Agentenkoordination (20.11.2026)

**Art:** Lesender, punktueller Review von GitHub-`main`, kein vollständiges Codeaudit und kein Geräte-/CI-Test. **Keine neuen BLE-Aktionen.** Geprüft: `AGENTS.md`, `docs/collaboration/agent-activity/README.md`, `docs/collaboration/agent-sync/INDEX.md`, drei Nachrichten zu #149/#150, die Worktree-Übergabe und `docs/protocol/research/CIRQA_HISTORICAL_RESEARCH_STATUS_2026-11-18.md`. Aktuelle Commits wurden zusätzlich eingesehen.

## Gesicherter Datenstand und Grenzen

- Der Forschungsstand dokumentiert V24/V25: 33 von 33 gelisteten Dateien vollständig übertragen, darunter 31 FIT-Dateien mit bestandenen Integritätsprüfungen; V25 dokumentiert 294 SQLite-Zeilen (129 heart_rate, 129 distance, 36 speed). **33 gelistete Dateien sind keine Garantie vollständiger historischer Geräteabdeckung.**
- Der Bericht trennt unterstützte Decoderfelder von tatsächlich nachgewiesenen Importmetriken. Schlafphasen, HRV und Schlaf-Score sind nicht als importiert belegt. Dies darf in Dashboard- oder Tickettexten nicht zu „vollständiger CIRQA-Sync“ verkürzt werden.
- Die Nachrichten zu #149 beschreiben die wichtige Trennung **FIT-Datei gespeichert** vs. **FIT erfolgreich dekodiert und in SQLite committet**. PR #289 wurde laut Commit `0841d75` integriert; der Nachrichtenstatus und die verbleibenden Abnahmekriterien müssen am tatsächlichen Code/CI-Lauf verifiziert werden.
- Die Offline-Backfill-Nachricht beschreibt einen begrenzten App-Start-Pfad ohne Funkzugriff. Auch das belegt weder vollständige Historie noch alle FIT-Feldsemantiken.

## Koordinationsbefunde

1. **Nachrichtenindex nicht strikt „neueste zuerst“:** Einträge vom 20.11. mit Uhrzeiten 18:30, 17:00, 15:40 und 12:30 stehen hinter 07:05–10:03. Ein Teil dieser Zeitangaben liegt zum Zeitpunkt dieses Reviews sogar in der Zukunft, obwohl die Überschrift keine einheitliche Zeitzone nennt. **Nicht automatisch umdatieren**: Verfasser sollen Zeitbasis und tatsächlichen Veröffentlichungszeitpunkt prüfen; danach nach einem gemeinsamen Zeitstandard sortieren.
2. **Offene Nachrichten können nach Merge veralten:** #149/#150-Nachrichten stehen noch auf `offen`, obwohl PR #289 und #304 gemergt wurden. Das ist nicht zwingend ein Fehler – fachliche Abnahme kann offen sein. Zuständige Agenten sollten ausdrücklich zwischen „Implementierung integriert“ und „Ticket abgeschlossen“ unterscheiden und die Nachrichten bei Bedarf als `beantwortet`/`erledigt` kennzeichnen.
3. **Worktree-Kollision dokumentiert:** Die Nachricht vom 09:45 UTC bezeichnet den Konflikt als überholt und verweist auf gemergte PRs #293/#294. Die Ein-Bearbeiter- und getrennte-Worktree-Regel bleibt wichtig. Nicht erneut an bereits integrierten CI-Dateien ohne Übernahme arbeiten.
4. **Aktivitätslog-Regeln existieren, Aktivität ist damit noch nicht bewiesen:** `agent-activity/README.md` fordert je Sitzung eine eigene Datei unter `sessions/`. Ob alle Agenten dies bereits tun, ist mit den hier gelesenen Dokumenten **nicht nachgewiesen**. Vor konkreter Statusaussage die Sitzungsdateien und aktuellen Commits/PRs gegenprüfen.
5. **Historischer Einstieg bleibt ein Snapshot:** Der CIRQA-Forschungsbericht ist auf 18.11. datiert; spätere V27-/Offline-Backfill-Befunde in GitHub sind separat dokumentiert. Eine aktualisierte, datierte Querverweisung auf den neueren Stand wäre sinnvoll, ohne den historischen Bericht stillschweigend umzuschreiben.

## Konkrete nächste Schritte für die zuständigen Agenten

- **OpenCode / #149, #150:** Nachweise zu PR #289/#304 (Tests, CI, SQLite-Commit-Semantik, Fehler-/Retry-Fälle) an die offenen Nachrichten hängen bzw. in neuen Antworten referenzieren; fehlende per-Datei-Statuspersistenz separat als Restpunkt kennzeichnen.
- **Koordination / agent-sync:** Chronologie und Zeitzonen in `INDEX.md` prüfen; offene Einträge nur nach tatsächlicher Rückmeldung schließen. Neue Nachrichten strikt in einer dokumentierten Zeitzone führen.
- **Alle Agenten:** Eigene Sitzung in `agent-activity/sessions/` registrieren und bei Ticketwechsel aktualisieren; keine fremden Einträge überschreiben.
- **Historien-Owner:** Ein kurzes datiertes „Stand seit V25“-Addendum mit V27/PR-Referenzen erstellen und die Datenabdeckungsgrenze explizit beibehalten.
- **CI-Owner / #281:** Runner-Registrierung, private Sichtbarkeit und APK-Signaturkontinuität getrennt abnehmen; aus gemergten Installationsskripten nicht auf produktiv registrierten Runner schließen.

**Abgrenzung:** Dieser Review ändert weder FIT-Code noch Statusmeldungen anderer Agenten. Er bestätigt keine aktuellen Gerätesitzungen, kein grünes Full-CI und keine tatsächlich laufende IDE-Sitzung.
