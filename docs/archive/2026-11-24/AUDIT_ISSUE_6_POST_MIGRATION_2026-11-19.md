# Codeaudit #6 – Nachprüfung der Toolchain-Migration #155 (19.11.2026)

## Geltungsbereich und Beweisgrenzen

Diese **Teilprüfung** basiert auf dem aktuellen GitHub-`main` nach Merge von PR #200 (`45515e8290d2fba54c7452399879749098373ef7`), der GitHub-Dateibaum-API, dem aktuellen Build-Skript, Manifest und CI-Workflow sowie den bisherigen Auditberichten `docs/archive/2026-11-24/TEST_INVENTORY_2026-11-18.md` und `docs/archive/2026-11-24/AUDIT_TEIL2_2026-11-18.md`. **Kein eigener Android-Build, kein S25-Test, keine erneute JaCoCo-Auswertung und kein vollständiges dateiweises Codeaudit.** Das Ticket #6 bleibt offen.

## Inventur des aktuellen GitHub-Baums

| Bereich | Dateien | Nachweis und Bewertung |
| --- | ---: | --- |
| Kotlin-Produktionscode (`app/src/main/java/`) | 205 | Dateibaum, keine Aussage zur funktionalen Testtiefe |
| Kotlin-JVM-Tests (`app/src/test/`) | 147 | Dateibaum, keine Aussage zur Zahl der Testfälle |
| Kotlin-Emulatortests (`app/src/androidTest/`) | 22 | Dateibaum, kein S25-Test |
| Ressourcen (`app/src/main/res/`) | 64 | Dateibaum, Ressourcen-Audit ausstehend |
| GitHub-Workflows | 4 | `android.yml`, `artifact-cleanup.yml`, `dependency-review.yml`, `java21-bytecode-experiment.yml` |

Die historische Inventur vom 18.11. mit 169 Produktionsdateien / 132 JVM-Testdateien / 17 Android-Testdateien ist **nicht mehr aktuell**. Ihre Methode (Namensreferenzen) war ohnehin keine Coverage-Messung. Die damaligen 31,1 % JVM-Zeilenabdeckung dürfen nicht als aktuelle Gesamt-Coverage ausgegeben werden.

## Verifizierte Migrationsergebnisse und offene Risiken

| Komponente | Befund aus aktuellem Code / CI | Risiko | Weiterer Nachweis |
| --- | --- | --- | --- |
| Gradle/AGP | CI nutzt Gradle **9.6.0**; Root-Build deklariert AGP **9.5.1**. | gering nach grünem CI | Build auf aktuellem `main` separat dokumentieren |
| Kotlin | AGP-9-Built-in-Kotlin; `org.jetbrains.kotlin.android` nicht mehr angewendet. Buildscript-Classpath Kotlin **2.4.21**. | mittel | Gradle Dependency-/Plugin-Resolution und Compiler-Version im CI-Protokoll prüfen |
| Android-SDK | `compileSdk=37`, `targetSdk=35`, `minSdk=26`; JVM-Target 17. | mittel | API-/Lint- und Gerätekompatibilität; keine Behauptung einer targetSdk-Anhebung |
| CI | PRs und `main`-Pushes lösen vollen Build, Unit-/Python-Tests, Lint und API-35-Emulator aus. GitHub Actions auf Commit `0623cf138` war erfolgreich (PR-Run **44506636115**, **44506636012**, manueller Lauf **44506633451**). | gering / Evidenz nur für bezeichneten Commit | Bei jeder Änderung neue Branch-/Main-Head-CI prüfen |
| Manifest / Privatsphäre | Kein INTERNET-Recht; BLE-Rechte und Foreground-Service deklariert; `allowBackup=false`, nur Launcher-Activity exportiert. | mittel | Laufzeitpfade, Datenexport und Backup-Inhalte dateiweise prüfen |
| Testabdeckung | `enableUnitTestCoverage=true`, `enableAndroidTestCoverage=true`; Instrumentations-Coverage-Gate verlangt nichtleere EC/EXEC-Datei. | mittel | Zusammengeführten Coverage-Bericht und komponentenbezogene Branch-/Line-Coverage erzeugen |
| Release-Sicherheit | Nur Debug-Build im `app/build.gradle.kts`; stabile CI-Signatur kann **Debug-APK** für Updates bereitstellen. Kein Release-R8/Minify-Gate. | **hoch** | **#154**: nicht debuggable Release-Build, Signatur-/Datenmigrations-Upgrade-Test |
| Dokumentation | `docs/product/release/ANDROID_RELEASE_READINESS.md` behauptet im Abschnitt „CI-Teststrategie“ noch, dass bei normalen Pushes kein Lint/Emulator läuft; aktueller Workflow führt beides aus. `docs/project/PROJECT_STATUS.md` enthält ebenfalls datierte historische CI-Aussagen. | mittel | Aktuelle Anleitung korrigieren; historische Aussagen datiert belassen |

## Konkreter Befund aus Workflow-Review (Folge-Issue #203)

- `.github/workflows/java21-bytecode-experiment.yml` verlangt per `assert` die alte Kotlin-DSL-Zeichenkette `jvmTarget = "17"`; im aktuellen `app/build.gradle.kts` steht jedoch `jvmTarget.set(JvmTarget.JVM_17)`. Der manuelle Experiment-Workflow würde deshalb bereits beim Transformationsschritt abbrechen, bevor Gradle gestartet wird. **Statischer Nachweis, nicht ausgeführt.**
- Das Experiment nutzt `actions/checkout@v7`, `actions/setup-java@v6`, `gradle/actions/setup-gradle@v6` und `actions/upload-artifact@v7` ohne vollständige SHA-Pins. Der reguläre CI-Workflow ist dagegen gepinnt.
- Nacharbeit in [Issue #203](https://github.com/jochenkunkel/pulselibre/issues/203) dokumentiert. Das produktive JVM-17-Ziel bleibt unverändert.

## Audit-Arbeitsplan für #6

1. **Produktionscode dateiweise** nach BLE, Datenbank/Import, Recorder, UI, Export/Backup, wissenschaftlichen Auswertungen gruppieren. Pro Datei: Eingaben, Seiteneffekte, Fehlerpfade, Sicherheitsgrenzen, Testreferenzen und konkrete Belege erfassen.
2. **Testinventur** gegen aktuellen Quellbaum erneuern; Coverage aus dem *aktuellen* CI-Lauf statt historischen 31,1 % erfassen. Fehlende Regressionstests priorisieren.
3. **Persistenz / Migration**: Schema-Versionen, Transaktionen, Rollback, Backup v1–v5 und Datenverlustpfade auf Wegwerf-Datenbanken prüfen; keine echte S25-Datenbank verändern.
4. **UI und Ressourcen**: Layout, 200-%-Schrift, TalkBack, Theme, Manifest, Exportpfade und Bildschirmzustände anhand Emulator-Tests prüfen. Echte S25-Abnahme bleibt separat.
5. **OWASP MASVS**: Storage, Crypto, Auth (falls anwendbar), Network, Platform, Code, Resilience und Privacy mit spezifischen Code- und Testbelegen; nicht allein aus Manifest auf Erfüllung schließen.
6. **Abnahme**: Für jede Komponente Status/Risiko/Beleg/Regressionstest/Folge-Issue dokumentieren. #6 erst schließen, wenn Vollständigkeit und notwendige Hardware-Evidenz vorliegen.

## Zusammenarbeit

#155/PR #200 gehört dem Codex-/Claude-Code-Arbeitsplatz und ist bereits gemergt. Dieses Dokument ändert **keine** Gradle-, Kotlin-, SQLite-, BLE- oder UI-Dateien. Technische Korrekturen nur in getrennten, koordinierten PRs. Instrumentationstests ausschließlich auf Emulatoren, nie auf dem S25.
