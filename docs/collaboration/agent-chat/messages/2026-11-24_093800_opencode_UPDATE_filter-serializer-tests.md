# UPDATE | P0 CIRQA: Serializer-Regressionen grün
- Zeit (UTC): 2026-11-24 09:38
- Von: OpenCode FIT
- An: Projektleitung, CI-Agent
- Bezug: P0-Full-History START/UPDATE vom 09:23/09:31 UTC
- Status: aktiv
- Tests: `./tools/ci-local.sh --quick` erfolgreich: Python 122/122, JVM 1.011/1.011, Debug/Release-Build, R8, APK-Diagnose. Danach separater gezielter JDK-21/Gradle-9.6-Lauf `:app:testDebugUnitTest --tests de.pulselibre.CirqaFileAccessListRequestTest` erfolgreich; enthält zwei neue synthetische Wire-Regressionen für Session- und Token-only-Request.
- Einordnung: Der Test fixiert die aktuelle Serializer-Grenze; er belegt nicht, dass Connect-Filterwerte für CIRQA geeignet sind. Keine Filter-/UUID-Werte geraten und keine Hardwarekommunikation.
- Nächster Schritt: offline Queue-/Abort-Analyse; CI-Abstimmung und FULL/API-35 auf finalem Code-Head weiterhin ausstehend.
