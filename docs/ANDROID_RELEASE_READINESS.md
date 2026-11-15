# Android-Gerätekompatibilität, Updates und Store-Vorbereitung

Stand: 2026-11-15. **Primäres Zielgerät: Samsung Galaxy S25.** Weitere Android-Geräte sind unterstützte Testziele, keine bereits bestätigten Hardwaretests.

## Geräte und Darstellung

- Android minSdk 26, targetSdk 35, compileSdk 35; BLE-Hardware laut Manifest erforderlich.
- Layout in `MainActivity` verwendet vertikalen ScrollView und drei gleich gewichtete Navigationseinträge. Dies ist eine Ausgangsbasis, **keine** Garantie für kleine Displays, große Schrift, Tablets oder Landscape.
- Prüfmatrix vor Freigabe: Galaxy S25 real (Hauptgerät), Android-Emulator API 35 Pixel 6 (CI), kleines Telefon 360dp, großes Telefon, Tablet, Hoch-/Querformat, Font-Scale 1.0/1.3/2.0, Display-Scale, Hell-/Dunkelmodus, TalkBack.
- UI-Befunde: Gerätediagnose kann lange, nicht umgebrochene Service-UUIDs enthalten; `ExportActivity` verwendet feste Pixelabstände statt dp; Bottom-Navigation hat keine explizite barrierefreie Tab-Semantik. Beides ist vor Store-Freigabe zu testen und zu verbessern.
- Screenshots, Layout-Tests und S25-Praxistests sind als Evidenz zu speichern, nicht als hypothetisch „bestanden“ zu markieren.

## Installieren, Aktualisieren, Deinstallieren

- `applicationId = de.pulselibre` muss für Updates konstant bleiben.
- Ab Version **0.1.1 / versionCode 2** ist die Versionsnummer gegenüber 0.1.0 / code 1 erhöht. **Ein höherer versionCode allein garantiert kein Update.**
- Android erlaubt ein In-Place-Update nur bei kompatibler App-Signatur. GitHub Actions erzeugt aktuell Debug-APKs mit **nicht dauerhaft gesicherter CI-Debug-Signatur**. APKs aus verschiedenen Workflow-Runs können deshalb mit unterschiedlichen Schlüsseln signiert sein. **Nicht versprechen, dass eine CI-APK die vorherige CI-APK überschreiben kann.**
- Für einen verlässlichen Update-Pfad: dedizierten Release-Keystore sicher verwahren, Signierzugriff nur über geschützte CI-Secrets, niemals Schlüssel ins Git einchecken; zuerst signierte interne Testreleases mit stabiler Signatur testen.
- **Deinstallation löscht standardmäßig die privaten SQLite-Daten und Berechtigungen.** `allowBackup=false`; eine automatische Wiederherstellung wird nicht zugesagt. Vor Deinstallation manuell exportieren, wenn lokale Messdaten vorhanden sind.
- Testmatrix: Neuinstallation, Update gleiche Signatur (Daten erhalten), Update andere Signatur (erwartet abgelehnt), ältere Versionsnummer (erwartet abgelehnt), Deinstallation/Neuinstallation (Daten weg), Berechtigungen nach Update, Export nach Prozessneustart.

## Play-Store- und Sicherheits-Gates

1. Konsistente signierte Release-Builds und Android App Bundle (AAB), getrennt von Debug-APKs.
2. Versionsstrategie, Changelog, Upgrade- und Datenbankmigrationstests.
3. Accessibility, adaptive Layouts und Geräte-Matrix; S25 bleibt Release-Blocker.
4. Datenschutzrichtlinie und korrektes Play-Data-Safety-Formular anhand **tatsächlich** verarbeiteter Daten; Diagnose-Teilen und BLE-Werbedaten besonders prüfen.
5. Google-Play-Anforderungen zu Ziel-API, App-Signierung, Store-Listing und Testtracks unmittelbar vor Veröffentlichung anhand der aktuellen Vorgaben prüfen.
6. Keine Behauptung, die App sei bereits Play-Store-fertig; weder Release-Signing noch AAB-Pipeline oder reale Geräte-Matrix sind derzeit abgenommen.
7. Keine INTERNET-Berechtigung und kein Health Connect ohne gesonderte Produktentscheidung.

## Reihenfolge

P0: Datenschutz des Diagnoseexports und BLE-Fehler-Cleanup; P1: S25-Regression, Upgrade-/Signaturtest, kleine Bildschirme und große Schrift; P2: AAB-Release-Workflow, Store-Compliance, FIT-Decoder und sicherer CIRQA-Datenpfad. Die Kernaufgabe bleibt die verifizierte, sichere Gewinnung **echter** CIRQA-Messwerte.
