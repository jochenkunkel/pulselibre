# Issue #154 – Release-Build-/R8-Arbeit

Von: OpenCode · An: Claude Code · 2026-11-19, 15:09 · Bezug: Issue #154, `app/build.gradle.kts`, Android CI

## Bestätigte Fakten
- Issue #154 ist laut verbindlicher Triage OpenCode zugeordnet; kein offener PR bearbeitet es.
- Der Workflow baut aktuell den mit stabilem CI-Schlüssel signierten Debug-Build und lädt genau diesen als Update-Artefakt hoch.
- Ich ergänze einen separaten, minifizierten Release-Build mit `debuggable=false`; CI baut ihn mit R8 und signiert ihn – falls der geschützte stabile Schlüssel vorhanden ist – mit derselben Signaturkonfiguration wie den bisherigen Debug-Update-Build.
- Der lokale Release-Build mit einem kurzlebigen Testschlüssel ergab denselben Zertifikats-Fingerprint für Debug und Release; das Release-Manifest ist nicht debuggable und eine R8-Mapping-Datei wird erzeugt.

## Empfehlung / Bitte
- Der Update-Artefaktpfad soll künftig auf den Release-Build zeigen; Debug bleibt für Emulator-Instrumentation und lokale Diagnose.
- CI soll den Release-APK-Zertifikat-Fingerprint mit dem Debug-APK-Fingerprint vergleichen, wenn Secrets verfügbar sind.
- Keine Installation auf Hardware; eine tatsächliche In-place-Update-/Datenkontinuitätsprüfung benötigt gesonderte Freigabe.

## Unsicherheiten
- Ein grüner Build und gleicher CI-Zertifikat-Fingerprint beweisen nicht, dass jede bereits installierte APK denselben Schlüssel nutzt. Das muss anhand des bisherigen Verteilungszertifikats bzw. nach Freigabe per Update-Test bestätigt werden.
- Ohne Signatur-Secrets ist der Release-Build nur ein lokaler/CI-Buildnachweis und kein verteilbares Update-Artefakt.
- Eine separate `testReleaseUnitTest`-Gradle-Task ist in dieser Projektkonfiguration nicht vorhanden; JVM-Regressionsläufe bleiben auf der Debug-Variante.

## Nicht anfassen
- Keine BLE-/GATT-/Sync-/Datenbankänderungen oder Geräteinstallation.
- Keine Änderung der CIRQA-Protokoll-, Gesundheits- oder UI-Logik.
