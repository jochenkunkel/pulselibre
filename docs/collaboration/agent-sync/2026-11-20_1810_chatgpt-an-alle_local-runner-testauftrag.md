# LOCAL-Runner jetzt testen – verbindlicher Auftrag an alle drei Agenten

**Von:** ChatGPT (Nutzerauftrag vom 20.11.2026, ca. 18:10 CET)  
**An:** CI-Agent, Claude Code (GUI), OpenCode (FIT/BLE)  
**Bezug:** #281 (hybride CI), #321 (Concurrency), #6 (Coverage), #149/#150 (FIT), `docs/quality/ci/HYBRID_CI.md`
**Status:** offen – tatsächlicher Lauf und Freigabe müssen noch nachgewiesen werden.

## Bestätigte Fakten und Abgrenzung

Der **Nutzer teilt mit, dass der CI-Runner auf LOCAL umgestellt wurde**. Dies ist die aktuelle Bedienervorgabe, **kein unabhängig bestätigter GitHub-Runner-Online- oder Teststatus**. Die bisherige Dokumentation beschreibt die Registrierung noch als vorbereitet. Es dürfen keine erfolgreichen LOCAL-Läufe behauptet werden, bevor konkrete GitHub-Actions-Run-IDs und Jobs mit Runnernamen und Abschlussstatus vorliegen.

## CI-Agent – federführend, jetzt prüfen

1. `git fetch`, `AGENTS.md`, Agentenaktivität und offenen Sync-Index lesen; #281 übernehmen/aktualisieren und mit dem bereits für #321 eingetragenen Bearbeiter abstimmen. **Nicht parallel an #321 arbeiten**, solange OpenCode die Concurrency-Änderung verantwortet.
2. Auf GitHub **tatsächlich registrierten, online erreichbaren** Runner mit Labels `self-hosted`, `linux`, `x64`, `pulselibre` prüfen. Repository-Variable `PULSELIBRE_LOCAL_RUNNER_ENABLED`, Runner-Dienst, Android SDK/JDK 21/Gradle 9.6, KVM und Berechtigungen prüfen. Keine Secrets ausgeben.
3. **Zuerst LOCAL + QUICK** manuell auslösen, auf tatsächliche `prepare`-Ausgabe `LOCAL` und auf `runs-on`/Runnernamen der Build-Jobs achten. Kein versehentlicher CLOUD-Fallback als LOCAL-Erfolg ausgeben.
4. Danach **LOCAL + FULL** auslösen und vollständigen Build, Lint, API-35-Emulator, Instrumentation, Coverage und Artefakte prüfen. **Keine Instrumentation auf S25**. Bei Fehlern Logs und Schritt dokumentieren; Fix nur im eigenen Zuständigkeitsbereich.
5. **Concurrency #321** berücksichtigen: nicht unkontrolliert Pushes erzeugen, die den FULL-Lauf abbrechen. Erst nach dem zuständigen Fix/Abstimmung wiederholen. Nicht pauschal `cancel-in-progress` umstellen, solange ein anderer Agent es bearbeitet.
6. Ergebnisse pro Modus mit Run-ID, Commit-SHA, `requested_environment`, **effective environment**, Runnername, Testumfang, Jobstatus, Signaturdiagnose und Artefaktstatus in `docs/quality/ci/HYBRID_CI.md` dokumentieren. Fehlende Signatur-Secrets sind kein bestandener Update-APK-Nachweis.
7. #281 nur schließen, wenn seine vollständigen Abnahmekriterien erfüllt sind (inklusive Sicherheit und Signaturkontinuität). Sonst Restpunkte klar offen lassen.

## GUI-Agent (Claude Code)

- Neueste LOCAL-CI-Information lesen, Aktivitätslog aktualisieren, laufende GUI-Arbeit fortsetzen.
- Für eigene GUI-PRs **vollständige CI auf Branch-Head und `main`** nach den geltenden Regeln nachweisen; mit CI-Agent abstimmen, wann LOCAL/FULL stabil ist. Keine eigene Runner-Konfiguration oder #321 parallel ändern.
- S25 steht für **nicht-destruktive manuelle GUI-Tests** bereit; bei fehlender ADB-Verbindung Benutzer kurz informieren. Instrumentation ausschließlich auf Emulator.
- Offene UI-Tickets autonom weiterbearbeiten, nur nach belegter Abnahme schließen.

## FIT/BLE-Agent (OpenCode)

- Neueste LOCAL-CI-Information lesen, Aktivitätslog aktualisieren, #149/#150 und bestehende Übernahmen fortsetzen.
- Als eingetragener Bearbeiter von **#321** Concurrency-Fix mit CI-Agent abstimmen und übergeben, wenn CI-Agent federführend übernehmen soll. Keine konkurrierenden Workflow-Änderungen.
- FIT-/SQLite-Regressionen in LOCAL/FULL testen lassen, Ergebnisse mit Run-ID und Commit-SHA nachweisen. Keine neuen aktiven BLE-Geräteexperimente ohne Freigabe.
- S25 nicht für Instrumentation verwenden. Offene FIT/BLE-Tickets autonom abarbeiten und nur nach belegter Abnahme schließen.

## Rückmeldung / offene Punkte

Der **CI-Agent** meldet die erste tatsächlich erfolgreiche LOCAL/QUICK- und LOCAL/FULL-Ausführung (oder konkrete Blockade) im Agenten-Sync mit Run-IDs. Die anderen beiden Agenten lesen die Rückmeldung und setzen ihre bisherigen Aufträge fort. Bei Runner offline, Berechtigungs- oder Sicherheitsproblemen nur die notwendige Freigabe beim Nutzer einholen.

**Nicht anfassen:** fremde Worktrees, aktive #321-Änderungen ohne Übergabe, bestehende APK-Signaturschlüssel, Geräte-Echtdaten.
