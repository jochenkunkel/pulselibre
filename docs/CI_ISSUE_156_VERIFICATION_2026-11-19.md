# CI-Härtung – Abgleich mit Issue #156 (19.11.2026)

## Prüfumfang
Statische Prüfung von `.github/workflows/android.yml`, `.github/workflows/dependency-review.yml`, `.github/scripts/gradle-retry.sh` und `app/build.gradle.kts` auf dem damaligen `main`. Keine Behauptung eines neuen Android-/Emulatorlaufs durch diesen Dokumentationsstand.

| Abnahmepunkt | Nachweis im Code | Bewertung |
| --- | --- | --- |
| Actions auf unveränderliche SHA pinnen | `android.yml`: checkout, setup-java, setup-gradle, upload-artifact und Emulator-Runner mit vollständigen SHA; `dependency-review.yml` ebenso | Implementiert; regelmäßige Versions-/Provenienzprüfung weiterhin nötig |
| Maven-HTTP-429 selektiv wiederholen | `.github/scripts/gradle-retry.sh`: höchstens drei Versuche, Retry nur bei 429/Throttling-Text; sonst Exit mit Originalstatus | Implementiert; neue Regressionstests in `tools/test_gradle_retry.py` |
| JaCoCo-Unit-Testbericht | `android.yml`: `:app:createDebugUnitTestCoverageReport` und Upload `pulselibre-jvm-jacoco` | Implementiert; tatsächliches Vorliegen des Artefakts im jeweiligen Lauf prüfen |
| Instrumentationsabdeckung | `app/build.gradle.kts`: `enableAndroidTestCoverage = true`; Emulator-Workflow lädt vorhandene Coverage-/Report-Verzeichnisse hoch | **Noch nicht als messbarer Bericht nachgewiesen**; Upload mit `if-no-files-found: warn` beweist keine erzeugte Abdeckung |
| Merge-Gate | Android-CI startet derzeit ausschließlich über `workflow_dispatch` | **Kein automatisches PR-Gate**; erfolgreiche manuelle Vollprüfung erforderlich |

## Restrisiken und Folgearbeit
1. Für Instrumentationsabdeckung im Emulator einen **tatsächlich erzeugten** JaCoCo-/EC-Bericht nachweisen; Upload-Warnungen und leere Ordner nicht als Erfolg zählen. Danach Kennzahlen/Artefakt im Issue verlinken.
2. Dependency Review scheitert nach den geprüften Logs an fehlender Dependency-Graph-Unterstützung des Repositories, nicht an einem belegten gefundenen CVE. Repository-Einstellung/Unterstützung separat klären (#155).
3. `tools/test_gradle_retry.py` deckt die Shell-Steuerlogik offline mit simuliertem `gradle` und `sleep` ab; es ersetzt keinen echten Gradle-/Maven-429-End-to-End-Test.
4. Issue #156 erst schließen, wenn die Instrumentationsabdeckung belegbar ist. Die übrigen drei Kernmaßnahmen sind im Code vorhanden.

## Abgrenzung zu Issue #6
Dies ist ein gezielter Workflow-Teilbefund, **kein vollständiger Code-/MASVS-Audit**. Der ältere Teil-Audit `docs/AUDIT_TEIL2_2026-11-18.md` nennt die fehlende Instrumentationsabdeckung ausdrücklich. Weitere Dateiinventur, Ressourcen- und Datenschutzprüfung bleiben offen.
