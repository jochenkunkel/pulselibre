# Kombinierte JVM-/Emulator-Coverage (#6) – Umsetzung und Grenzen

Stand: 2026-11-22 10:20 UTC · Verantwortlich: CI-Agent (OpenCode) · Branch `ci/combined-coverage-20261122`

## Umsetzung

- `app/build.gradle.kts`: Gradle-`jacoco`-Plugin und Task `createCombinedCoverageReport`.
  - Execution Data: `outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec`
    plus alle `outputs/code_coverage/debugAndroidTest/connected/**/coverage.ec`.
  - Klassen: ausschließlich `intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes`
    **des jeweiligen Jobs**. JaCoCo-Kennungen müssen zu genau dieser Kompilierung passen.
  - Bericht: XML + HTML unter `app/build/reports/jacoco/createCombinedCoverageReport/`.
  - `doFirst` bricht ab, wenn eine der beiden Quellen oder die Klassen fehlen.
- `tools/ci/run-ci.sh`, Phase `emulator` (nur FULL): nach dem bestehenden
  Instrumentationslauf und Coverage-Gate werden `:app:testDebugUnitTest` und
  `:app:createCombinedCoverageReport` im selben Job ausgeführt. Die Build-Phase
  bleibt unverändert; es gibt bewusst **keinen neuen Gate und keine Schwelle**.
- `.github/workflows/android.yml`: neuer Artefakt-Upload
  `pulselibre-combined-jacoco` mit dem kombinierten Bericht (7 Tage Aufbewahrung).
- Sicherheits-Schranke in `run-ci.sh`: Die Emulator-Phase bricht ab, wenn ein
  physisches Android-Gerät sichtbar ist und `ANDROID_SERIAL` nicht ausdrücklich
  auf `emulator-...` gesetzt ist. `tools/ci/emulator.sh` setzt diesen Serial
  regulär; direkte Aufrufe können künftig nicht mehr still auf ein Telefon laufen.

## Lokale Prüfung – nachweisbar, aber nicht maßgeblich

Mit bereits vorhandenen, lokal gespeicherten Coverage-Dateien desselben
Quellstands und lokal kompilierten Klassen:

| Quelle | Instructions covered / total | Anteil |
|---|---:|---:|
| JVM-only (vorhandener Bericht) | 51.991 / 153.715 | 33,8 % |
| Kombiniert (neuer Task) | 107.809 / 153.715 | 70,2 % |

Der Anstieg belegt, dass beide Quellen tatsächlich verarbeitet werden. Diese
Zahlen stammen aus **lokalen Alt-Artefakten**, nicht aus dem finalen CI-Lauf;
die maßgeblichen Werte stehen im Artefakt `pulselibre-combined-jacoco` des
jeweiligen FULL-Laufs.

Weitere Prüfungen: `bash -n tools/ci/run-ci.sh` ok; 99 Python-Tests `OK`;
Gradle-Konfiguration und Tasklauf `BUILD SUCCESSFUL`.

Der erste CI-Lauf (`45012429849`) scheiterte in der neuen Phase, weil Gradle
die Abhängigkeit des Report-Tasks von `compileDebugKotlin` vermisste. Der Task
deklariert jetzt `dependsOn("compileDebugKotlin")` und
`mustRunAfter("connectedDebugAndroidTest", "testDebugUnitTest")`; ein erneuter
lokaler Tasklauf ist grün. Der erste Lauf ist damit kein Erfolgsnachweis.

## Sicherheitsvorfall / Offenlegung

Beim ersten lokalen Aufruf von `bash tools/ci/run-ci.sh emulator --scope FULL`
war **kein Emulator gestartet**, sondern nur das physische S10 per USB
angeschlossen. Ohne `ANDROID_SERIAL` lief `connectedDebugAndroidTest` daher auf
diesem Gerät (118 Tests, 8 Fehlschläge in UI-/Analysen-Assertions). Das war
nicht beabsichtigt und nicht als Gerätetest freigegeben. Es wurden keine
BLE-/GATT-/FileSync-Kommandos ausgeführt und keine Gerätedaten in GitHub
geschrieben.

Folgen und Maßnahmen:

- Rein lesender Zustandscheck danach: Displayskalierung 480 dpi, Schriftfaktor
  1.0, Nachtmodus `yes`, Bluetooth an. Diese Werte wurden **nicht** verändert;
  ein Vergleich mit dem Zustand vor dem Lauf liegt nicht vor.
- Kein weiterer Gerätezugriff aus dieser Sitzung; keine Installation, kein
  Export, kein Reset, keine Profiländerung.
- Die oben beschriebene `ANDROID_SERIAL`-Schranke wurde unmittelbar danach
  ergänzt und mit dem identischen Aufruf geprüft: Die Phase bricht jetzt sofort
  mit `Physisches Android-Gerät sichtbar ...` ab, ohne Tests zu starten.
- Der Nutzer wird in der Antwort dieser Sitzung ausdrücklich darauf hingewiesen.

## Abgrenzung

Keine Produkt-, UI-, Datenbank- oder Protokolländerung. Keine Aussage über
Vollständigkeit des CIRQA-Historiendownloads oder DB-Import;
`coverage=device-history-unverified` bleibt unverändert gültig.
