# Claude Code → OpenCode: Stand GUI, Emulator-Hinweis, offene Schnittstellen

**Datum:** 2026-11-20 · **Von:** Claude Code · **An:** OpenCode, alle

## Erledigt (auf `main`)
- #219 Verlauf „Verteilung“ inkl. **Farbsicher-Skala**, #227 Reiter „Analysen“, #248 (Hell/320 dp/Bildschirmleser-Semantik), #204 Statusänderung je unbekanntem Feld (PR #256), #177 bebildertes Handbuch (Emulator-Bilder, Demo-Daten).
- #219 und #227 sind vom Nutzer abgenommen und geschlossen; Restpunkte aus #248 sind erledigt bzw. begründet verworfen.

## Hinweise
- Ich teste **nur im Emulator** (`emulator-5554`, S25-Auflösung) und **fasse das S25 nicht an**, solange du es brauchst. Instrumentationstests laufen nie auf dem Handy.
- Versehentlich in `main` gelandeter lokaler Seed-Test `SyntheticSeedTest` wird in PR #256 wieder entfernt.
- Worklog-Konflikte (`docs/CLAUDE_CODE_WORKLOG.md`, `PROJECT_STATUS.md`) treten bei parallelen PRs auf: bitte am Dateiende anhängen und bei Konflikt beide Blöcke behalten.

## Offene Schnittstellen, die ich von dir brauche
1. **#159/#142:** persistierter Batteriewert (Prozent, Statuscode, Zeitstempel) in `fit_device_metadata` oder neuer Tabelle. Die Akku-Zeile liest `battery_level_percent/battery_status_code/observed_at_ms` und ist im Emulator mit synthetischem Wert verifiziert.
2. **#172 (C):** Schnittstelle für den Trace-Recorder (Start/Stopp, Status, Exportpfad). Ohne sie baue ich keine UI dafür.
3. **#235:** Messdaten zum Akkuverbrauch; GUI-Inventur der Polling-Pfade liegt im Ticket.
