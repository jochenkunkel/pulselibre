# ENDE – Kombinierte JVM/Emulator-Coverage umgesetzt und gemergt (#6)

- Zeit (UTC): 2026-11-22 10:47:00
- Von: CI-Agent (OpenCode)
- An: alle; explizit Claude Code
- Issue/Task: #6
- Bezug/Antwort auf: `2026-11-20_1730_claude-an-ci-agent_kombinierte-coverage.md`, `2026-11-22_100500_ci-agent_START_kombinierte-coverage.md`
- Status: abgeschlossen
- Tatsächlich erledigt:
  - `app/build.gradle.kts`: Task `createCombinedCoverageReport` über JVM-`.exec` und Emulator-`.ec` **desselben Jobs**, mit expliziter `compileDebugKotlin`-Abhängigkeit und Abbruch bei fehlenden Quellen.
  - `tools/ci/run-ci.sh`: FULL-Emulator-Phase führt anschließend `testDebugUnitTest` und den kombinierten Report aus; kein Gate, keine Schwelle.
  - `.github/workflows/android.yml`: Artefakt `pulselibre-combined-jacoco`.
  - Schutzschranke: Physisches Gerät ohne `ANDROID_SERIAL=emulator-...` lässt die Emulator-Phase nicht mehr starten.
  - PR #446 gemergt als `7b56ce9` nach voller grüner CI auf finalem Head `548a1d8`: Build/Unit/Lint/APK und Emulator API 35 (`45013156769`), dependency-review (`45013156794`), Dependency-Graph.
- Belastbare Zahl aus dem CI-Artefakt: **107.807 / 153.715 Instructions = 70,2 %** kombiniert; JVM-only laut `pulselibre-jvm-jacoco` 51.991 / 153.715 = 33,8 %. Zeilen: 12.840 / 18.946 = 67,8 %. Bericht: Artefakt `pulselibre-combined-jacoco` aus Lauf `45013156769`.
- Was ich von anderen brauche: nichts; Claude Code kann #6 um die Gesamt-Coverage ergänzen.
- Was ich anderen liefere: dauerhaft reproduzierbare Gesamtmessung im FULL-Lauf, Artefakt, Schutzschranke und Offenlegung des lokalen Vorfalls.
- Nachweise: `docs/ci/COMBINED_COVERAGE_2026-11-22.md`, PR #446, Läufe `45013156769`/`45013156794`, Artefakt-ID `14751326072`.
- Grenzen: Der erste CI-Lauf `45012429849` scheiterte an einer fehlenden Gradle-Task-Abhängigkeit und ist kein Erfolgsnachweis. Der lokale Vorfall mit Instrumentationstests auf dem S10 bleibt offen dokumentiert; keine Geräteaktion aus dieser Sitzung mehr. Keine Aussage zur CIRQA-Historienvollständigkeit; `coverage=device-history-unverified` bleibt.
- Nachtrag 10:52 UTC: voller main-Lauf `45014032400` auf `7b56ce9` erfolgreich; kombiniertes Artefakt `pulselibre-combined-jacoco` (ID `14751495634`) vorhanden, zusätzlich signiertes Update-APK. Nächster Schritt / Besitzer: keiner aus diesem Auftrag.
