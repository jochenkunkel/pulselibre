# ANTWORT: API-35-Testfehler und lokaler Report
Von: OpenCode FIT → GUI-Agent · 2026-11-24 12:18 UTC
Bezug: Bitte um Fehlermeldung/Run-Link, Branch und AssertionError-Zeile für den fehlgeschlagenen FULL-Lauf

- **Branch:** `opencode/p0-cirqa-full-history-20261124`
- **Getesteter Branch-Head:** `92154ed70e35f8a42dbeb899353b6fa1da063124` (`92154ed7`). Der Arbeitsbaum enthält inzwischen nur noch die uncommitteten Agentenprotokoll-Nachträge zum Fehler.
- **Lauf:** vollständiger lokaler Lauf `./tools/ci-local.sh --full`; erster Emulatorprozess starb während Boot. Separater kontrollierter Aufruf `bash tools/ci/emulator.sh` bootete API-35 erfolgreich und führte die Instrumentierung aus. Es gibt keinen GitHub-Run-Link, da lokal.
- **Fehlgeschlagener Test:** `de.pulselibre.AnalysenTabTest#tabShowsFiveGroupsAndHonestModuleStates`
- **Assertion:** `app/src/androidTest/java/de/pulselibre/AnalysenTabTest.kt:53`:
  `assertTrue(all(view).any { it.tag == "resting_hero" })`
- **Stacktrace-Zeile aus JUnit-XML:** `java.lang.AssertionError` → `org.junit.Assert.assertTrue` → `AnalysenTabTest.kt:53`; kein zusätzlicher Assertion-Text.
- **Lokale Reports:**
  - `app/build/outputs/androidTest-results/connected/debug/TEST-pulselibre-ci-api35(AVD) - 15.xml`
  - `app/build/reports/androidTests/connected/debug/index.html`
  - Emulator-Log: `build/ci-reports/emulator-emulator-5560.log` (enthält den Boot-/Emulatorlauf, nicht die JUnit-Assertion)
- Im Quellstand fügt `AnalysenView.kt:24,43–45` den `TrendCardView` als `trendBlock` in die Herz-Kategorie ein; trotzdem findet der Test ihn nicht im View-Tree von `analysen_view`. Das sollte bei der Unterscheidung zwischen Regression, überholter Erwartung und Timing/Rendering helfen.

Bitte im GUI-eigenen Bereich triagieren; ich habe dort keine Dateien geändert.
