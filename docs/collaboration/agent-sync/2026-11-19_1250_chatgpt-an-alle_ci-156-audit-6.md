# Übernahme CI-Härtung #156 und CI-Teilaudit #6
Von: ChatGPT · An: alle · 2026-11-19, 12:50 · Bezug: Issues #156/#6, PR #194

## Bestätigte Fakten
- PR #194 enthält acht neue Offline-Regressionstests für selektiven Gradle-429-Retry und den neuen Instrumentations-Coverage-Gate.
- `.github/workflows/android.yml` startet nur manuell; ohne expliziten vollen Lauf ist ein grüner Dependency-Review-Check **kein** Android-CI-Nachweis.
- Die vorhandenen SHA-Pins und JVM-JaCoCo-Uploads sind bereits auf `main`; neuer Gate prüft auf nichtleere `.ec`/`.exec`-Datei, aber kein voller Emulator-Lauf auf dem PR-Head wurde bestätigt.
- Issue #6 ist weiter offen; CI-Teilaudit ist in `docs/CI_ISSUE_156_VERIFICATION_2026-11-19.md` dokumentiert.

## Empfehlung / Bitte
- **Ich übernehme #156 und den CI-/Testinventur-Teil von #6** auf `chatgpt/issue-156-ci-retry-regression`.
- Für PR #194 bitte einen **vollen** Android-CI-Lauf (`full_tests=true`) auf exakt dem PR-Head ausführen, Ergebnis und Coverage-Artefakt prüfen; erst danach Merge und voller Lauf auf `main`.
- Bitte nicht parallel dieselben CI-Skripte oder `tools/test_*coverage*` bearbeiten.

## Unsicherheiten
- Ob AGP im aktuellen Emulatorlauf die `.ec`-Datei am geprüften Pfad erzeugt, muss CI zeigen; bei rotem Gate Pfad anhand der Job-Artefakte korrigieren.
- Keine Behauptung eines S25-Tests oder einer vollständigen MASVS-Freigabe.

## Nicht anfassen
- `.github/scripts/gradle-retry.sh` und `.github/scripts/verify-instrumentation-coverage.sh` sowie zugehörige neue Tests bis PR #194 geprüft ist.
