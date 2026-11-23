# UPDATE – GUI/CI mit P0-Arbeit beauftragt; Armbandtest ausgelassen
- Zeit (UTC): 2026-11-23 09:02
- Von: OpenCode FIT / technische Leitung
- An: Projektleitung, GUI-Agent, CI-Agent, alle
- Bezug: P0 START `2026-11-23_084400_opencode_START_p0-sync-postmortem.md`; Nutzer-Ergänzung zum zweistündigen Lead-Auftrag

## Aufträge
- GUI: P0-Historien-Dashboard und Status-/Fehlerzustände mit Vertrag `PROFILE_VALID` + Zeit/Provenienz → VERIFIED nur für `heart_rate`, `distance`, `speed`; übrige Kandidaten/fehlende Daten klar markieren; synthetischen FIT→SQLite→GUI-Pfad testen; offene GUI-PRs prüfen. #529 B1 bitte isoliert halten. S10 nur manuell/read-only und unter Lease; kein BLE.
- CI: #541 Post-Merge-FULL mit exakter Run/SHA abschließen; meine lokale Emulatorfehlstelle `AnalysenTabTest#tabShowsFiveGroupsAndHonestModuleStates` bei Zeile 44 sauber abgrenzen; danach FIT-Branch-FULL, synthetische Sync-/FIT-/SQLite-Regressionen sowie signiertes APK-Artefakt nachvollziehbar belegen. Kein S10 ohne Lease, keine BLE.
- Vollständige Einzelaufträge und Dateigrenzen: `agent-sync/2026-11-23_0900_opencode-an-gui_p0-historical-dashboard.md` und `2026-11-23_0901_opencode-an-ci_p0-full-ci-and-download-regressions.md`.

## Armbandentscheidung
- CIRQA ist laut Projektleitung nur noch kurz verfügbar, aber der letzte Build-1939-Lauf endete am bekannten drei-Folgeantwort-Limit und der Screenshot schnitt die Datei-Stufenzähler ab.
- Ein weiterer aktiver Lauf ohne neue unterscheidungskräftige Protokollhypothese würde voraussichtlich dieselbe Pagination wiederholen. Er wurde **nicht** gestartet. Kein unbekanntes GATT-Kommando und kein Risiko, einen nicht sicher beendbaren Download anzufangen.
- S10-Lease war bei der Prüfung FREI; ich halte sie nicht selbst. GUI-/CI-Gerätezugriffe müssen exklusiv unter Wrapper koordiniert werden; keine Instrumentation auf physischem Gerät.

## Eigener Fortschritt / Tests
- Postmortem mit Codepfad-/Evidenzgrenze erstellt: `docs/protocol/device-tests/CIRQA_S10_BUILD1939_SYNC_POSTMORTEM_2026-11-23.md`.
- Sync-Statusreport stellt künftig Datei-/Stufenzähler vor die lange Itemliste; fokussierte JVM-Tests für Report, Completion und Pagination bestanden.
- `ci-local.sh --full`: Python 118/118 OK; Debug/Release Build, JVM, Lint und Android-Test-APK erfolgreich; Emulator scheitert bei `AnalysenTabTest#tabShowsFiveGroupsAndHonestModuleStates` (Zeile 44). Dieses Ergebnis ist nicht als FULL-PASS gewertet und an CI zur Reproduktion übergeben.
- Aktueller upstream `main=88274935` (PR #543); mein Arbeitszweig basiert noch auf `dc4ff00a` und wird vor dem Code-PR mit neuestem `main` abgeglichen.

## Nächster Schritt
- Merge-/Head-CI-Stände der offenen PRs aktualisieren, genaue Ursache des lokalen Emulatorfails prüfen und den Postmortem-/Reportpatch auf aktuellem `main` wiederholen; dann FULL-CI lesen. #533 bleibt separate Migration-Abnahme; #499 bleibt geschlossen/superseded; #496 PARKED.
