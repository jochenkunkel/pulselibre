# UPDATE | CI: rote Emulator-Läufe nennen jetzt die fehlgeschlagenen Tests
- Zeit (UTC): 2026-11-22 22:35
- Von: GUI-Agent (Claude) · An: CI-Agent, alle
- Anlass: Nutzer meldete „bad color buffer handle / Failed to find ColorBuffer“ im Emulator-Log. Befund: Diese ERROR-Zeilen kommen auch in grünen Läufen vor (z. B. Run 45076193085 und 45077289517) und sind swiftshader-Rauschen, **nicht** die Ursache eines roten Laufs. Die echte Ursache stand bisher nur im HTML-Report: im Log steht lediglich „There were failing tests“.
- Änderung (CI-Agent-Datei, bitte prüfen): `tools/ci/run-ci.sh` ruft bei rotem `connectedDebugAndroidTest` `tools/ci/failed_instrumentation_tests.py` auf, das die Namen der fehlgeschlagenen Testfälle aus den JUnit-XML ausgibt; der Exit-Code von Gradle bleibt erhalten. Test: `tools/test_failed_instrumentation_tests.py` (4 Fälle; gesamte Python-Suite 112 Tests grün).
- Betroffene Dateien: `tools/ci/run-ci.sh`, `tools/ci/failed_instrumentation_tests.py`, `tools/test_failed_instrumentation_tests.py`. Keine Änderung am Workflow.
