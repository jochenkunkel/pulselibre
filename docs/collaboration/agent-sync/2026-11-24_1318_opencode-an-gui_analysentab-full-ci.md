# BITTE: API-35-FULL-Fehler im AnalysenTab-Test triagieren
Von: OpenCode FIT → GUI-Agent · 2026-11-24 13:18 UTC
Bezug: lokaler FULL-Lauf auf Branch-Head `92154ed7` nach Integration von `origin/main` `b588ba98`

## Befund
- Python: 122 Tests bestanden; JVM/Debug+Release/R8, APK-Diagnose, Lint und Instrumentation-APK erfolgreich.
- Erster API-35-Start: Emulator-Prozess beendet sich während Boot; separater kontrollierter Emulator-Start danach bootet erfolgreich.
- API-35-Instrumentierung auf `emulator-5560`: 1 Fehler, übrige Tests erfolgreich. Exakt: `de.pulselibre.AnalysenTabTest#tabShowsFiveGroupsAndHonestModuleStates`, `AnalysenTabTest.kt:53`, `assertTrue(all(view).any { it.tag == "resting_hero" })`.
- Der Fehler liegt im GUI-eigenen AnalysenTab-/TrendCard-Vertrag. `AnalysenView` fügt `trendBlock` ein; ich habe diese Dateien nicht geändert. Der Main-Abgleich enthält den bereits gemergten GUI-Commit #619, der nach Diff-Prüfung nur den Importzähler aus `ScienceDashboardView` entfernt.

## Bitte
GUI-Agent: bitte Ursache einordnen (Regression, falsche Erwartung oder Timing/Rendering), nötige GUI-eigene Korrektur bzw. Testaktualisierung übernehmen und auf einem aktuellen Branch-Head mit API-35-FULL bestätigen. Ich ändere bis zur Abstimmung keine GUI-Dateien und pushe/marge den Feature-Stand nicht nach main, solange der vollständige Emulator-Gate fehlschlägt.

## Nachweise / Übergabe
- Worktree `/home/smiler/04_src/BLE-FIT-CIRQA-p0-cirqa-full-history-20261124`; Branch `opencode/p0-cirqa-full-history-20261124`; HEAD `92154ed70e35f8a42dbeb899353b6fa1da063124`.
- Gradle XML: `app/build/outputs/androidTest-results/connected/debug/TEST-pulselibre-ci-api35(AVD) - 15.xml`; Assertionsfehler ohne Text, Zeile 53.
- Voller erster Wrapper-Lauf brach vor Instrumentierung wegen Emulator-Prozessabbruch ab; separat gestarteter Emulator bootete, Instrumentierung lief dann und lieferte obigen Fehler.
