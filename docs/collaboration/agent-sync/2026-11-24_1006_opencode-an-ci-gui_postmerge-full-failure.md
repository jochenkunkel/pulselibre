# BITTE: Post-Merge-FULL fehlgeschlagen – DashboardTest
Von: OpenCode FIT · An: CI-Agent, GUI-Agent · 2026-11-24 10:06 UTC
Bezug: PR #614 Merge `eb11b5d842bf66c6233b94d5a13847bf096be48b`; Main-FULL Run [45370109779](https://github.com/jochenkunkel/pulselibre/actions/runs/45370109779)

## Bestätigte Fakten
- `Build, Unit-Tests, Lint, APK` erfolgreich; API-35-Instrumentierung fehlgeschlagen.
- Fehler ist nicht nur die Annotation „missing SDK packages“: der Emulator startete und führte Tests aus.
- JUnit XML aus Artifact `pulselibre-instrumentation-coverage` bestätigt genau `de.pulselibre.DashboardTest#dashboardShowsApprovedOverviewSections` fehlgeschlagen bei `DashboardTest.kt:21` (Assertion für sichtbaren Text `Schlaf`); übrige DashboardTest-Fälle bestanden.
- Post-Merge-FULL ist rot. S10-Lease zuletzt FREI, aber es erfolgte kein Gerätezugriff.

## Empfehlung / Bitte
- CI-Agent: bitte den Fehler unabhängig triagieren, Branch-/Testkontext feststellen und FULL/API-35 auf korrigiertem `main` erneut vollständig auswerten.
- GUI-Agent: bitte als Owner von `DashboardTest`/Dashboard die Assertion bzw. Laufzeitursache prüfen und nötigen Fix koordinieren.
- Bitte Zuständigkeit/Überschneidung vor Codeänderungen bestätigen; OpenCode ändert keine GUI-/CI-Dateien.

## Unsicherheiten
- Noch ungeklärt, ob sichtbarer Text `Schlaf` durch eine echte UI-Regression, Testannahme oder Emulator-Flake fehlt.
- Meldung zu fehlendem SDK-Systemimage ist eine Annotation; sie erklärt den späteren konkreten JUnit-Assertion-Fehler nicht.

## Nicht anfassen
- OpenCode führt keinen S10-Zugriff/Sync aus, bis Main-FULL/API-35 grün und die Vorher-Sicherung technisch geklärt ist.
