# SCA / Abhängigkeitsprüfung (#155): Stand 2026-11-20

## Nachweise (per GitHub-API am 2026-11-20)
- **Dependency Graph:** Der Workflow „Gradle Dependency Submission“ (`submit-main-graph`) läuft auf `main` grün; die Repository-SBOM enthält jetzt **225 Pakete**, darunter die Gradle-/Maven-Abhängigkeiten (der frühere Stand mit nur 6 GitHub Actions ist überholt).
- **PR-Prüfung:** `dependency-review` und „Generate Gradle dependency graph (PR)“ laufen auf jedem PR grün.
- **Dependabot-Updates:** `.github/dependabot.yml` (Gradle + GitHub Actions, wöchentlich).
- **Dependabot-Alerts:** 8 offene (1 critical, 5 high, 2 medium): Guava, Bouncy Castle (bcprov/bcpkix), jose4j, JDOM2, commons-lang3, httpclient.

## Bewertung
Alle acht Alerts betreffen Bibliotheken des **Build-Klassenpfads** (Android Gradle Plugin: Signierung, Lint, aapt), nicht der App:
`:app:dependencies` für `releaseRuntimeClasspath`/`releaseCompileClasspath` enthält keine davon (nur den leeren Platzhalter `guava:listenablefuture:1.0`). Sie landen nicht im APK. Betroffen wäre höchstens die Entwicklungs-/CI-Umgebung beim Verarbeiten manipulierter Eingaben.

## Maßnahme
`build.gradle.kts` hebt diese sieben Bibliotheken per `buildscript`-Constraint auf die ersten gepatchten Versionen (Guava 33.7.2, bcprov 1.85, bcpkix 1.84, jose4j 0.9.6, JDOM2 2.0.6.1, commons-lang3 3.18.0, httpclient 4.5.14). Geprüft mit `gradle buildEnvironment` (aufgelöste Versionen) sowie Unit-Tests, Lint, `assembleDebugAndroidTest` und `assembleRelease`. Die Constraints können entfallen, sobald das Android Gradle Plugin selbst gepatchte Versionen mitbringt.

## Nicht erledigt / offen
- **Dependabot Security Updates** stehen in den Repository-Einstellungen auf „disabled“. Das ist eine Einstellung des Repository-Eigentümers (Settings → Code security); ich habe sie nicht verändert.
- **Secret scanning** ist ebenfalls deaktiviert (gleiche Stelle).
- Ob die Alerts nach dem Merge automatisch schließen, hängt davon ab, dass der Dependency-Submission-Lauf auf `main` die neuen Versionen meldet. Das wird nach dem Merge geprüft.
