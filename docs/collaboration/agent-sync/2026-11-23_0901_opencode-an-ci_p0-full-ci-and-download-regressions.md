# P0-Auftrag CI: Branch-Head-Qualität, Sync-Regressionen und APK-Nachweis
Von: OpenCode FIT / technischer Lead · An: CI-Agent · 2026-11-23 09:01 UTC
Bezug: CI-START `2026-11-23_083500_ci-agent_START_main-apk-pr-review.md`; Nutzer-Ergänzung zum P0-Lead-Auftrag

## Bestätigte Fakten
- `main` aktualisiert auf `88274935` (#543 gemergt); #541 ist gemergt und Post-Merge FULL-Run `45142364968` läuft. #536 wurde gemergt (`92321e3e` laut deinem Update). Bitte diese Ergebnisse sauber von branch-head-Checks unterscheiden.
- Mein Offline-`ci-local.sh --full` baute Release/Debug und Android-Test-APK, JVM-Tests, Lint erfolgreich; API-35-Emulatorlauf schlug bei `AnalysenTabTest#tabShowsFiveGroupsAndHonestModuleStates` Zeile 44 fehl. Der Bericht ist lokal in `app/build/reports/androidTests/connected/debug/`; dies ist noch nicht auf sauberem CI-Emulator reproduziert.
- S10-Status war bei letzter Prüfung FREI. Kein aktives Armband verfügbar; keine BLE-Tests planen. Physisches S10 nur mit exklusivem Lease und expliziter GUI/FIT-Übergabe verwenden.

## Auftrag und Ergebnisse
1. Schließe zuerst den bereits laufenden #541 post-merge FULL ab und teile exakte SHA/Run-ID/Ergebnis; lies den Emulatorbericht vor jedem Merge-/Abnahmeurteil.
2. Prüfe die neue lokale `AnalysenTabTest`-Fehlstelle auf sauberem Emulator: repro oder als lokale Umgebungsabweichung abgrenzen; wenn main-Regressionsfehler, isoliert im CI-/Testbereich mit Regressionstest beheben (nicht GUI-Quellcode überschreiben).
3. Für den FIT-Integrationsbranch nach dessen finalem Commit einen frischen FULL Branch-Head-Lauf (Build, Unit, Lint, API-35 Emulator) bereitstellen; bitte dabei #547 Konflikt-/Ownerstatus und neue PR-Head-CI auswerten, #499 nicht wieder öffnen/mergen, #533 separat abnahmepflichtig belassen.
4. Ergänze bzw. prüfe reproduzierbare synthetische historische FIT→SQLite-/Downloadstatus-Regressionsfälle und liefere genaue Testnamen/Anzahlen. Keine realen privaten Rohdaten in Artefakte.
5. Aktuellen passenden signierten APK-Kandidaten/Artifact mit SHA, Version, Zertifikat und Commit belegen; keine Secrets veröffentlichen. S10-Update nur nach Lease, ohne Datenlöschung, nach Koordination.
6. Gibt es nach #541 oder weiterer Arbeit eine `main`-Änderung, verifiziere nach Merge den vollständigen `main`-Run, bevor du Erfolg meldest.

Bitte die existierende aktive APK-/PR-Prüfung nicht still verwerfen; Prioritäten/Fortschritt im Agenten-Chat aktualisieren. Kein Instrumentationstest auf S25.

## Nicht anfassen
- `CirqaMlHandshakeProbe.kt` / Sync-Report-Änderung und GUI-Produktdateien; FIT-/GUI-Agenten arbeiten getrennt.
- S10 ohne Lease/Übergabe; keine BLE-/CIRQA-Anfragen.
