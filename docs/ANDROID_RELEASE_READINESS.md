# Android-Gerätekompatibilität, Updates und Store-Vorbereitung

Stand: 2026-11-15. **Primäres Zielgerät: Samsung Galaxy S25.** Weitere Android-Geräte sind unterstützte Testziele, keine bereits bestätigten Hardwaretests.

## Geräte und Darstellung

- Android minSdk 26, targetSdk 35, compileSdk 37; BLE-Hardware laut Manifest erforderlich.
- Layout in `MainActivity` verwendet vertikalen ScrollView und drei gleich gewichtete Navigationseinträge. Dies ist eine Ausgangsbasis, **keine** Garantie für kleine Displays, große Schrift, Tablets oder Landscape.
- Prüfmatrix vor Freigabe: Galaxy S25 real (Hauptgerät), Android-Emulator API 35 Pixel 6 (CI), kleines Telefon 360dp, großes Telefon, Tablet, Hoch-/Querformat, Font-Scale 1.0/1.3/2.0, Display-Scale, Hell-/Dunkelmodus, TalkBack.
- UI-Befunde: Gerätediagnose kann lange, nicht umgebrochene Service-UUIDs enthalten; `ExportActivity` verwendet feste Pixelabstände statt dp; Bottom-Navigation hat keine explizite barrierefreie Tab-Semantik. Beides ist vor Store-Freigabe zu testen und zu verbessern.
- Screenshots, Layout-Tests und S25-Praxistests sind als Evidenz zu speichern, nicht als hypothetisch „bestanden“ zu markieren.

## Installieren, Aktualisieren, Deinstallieren

- `applicationId = de.pulselibre` muss für Updates konstant bleiben.
- Aktuell definiert `app/build.gradle.kts` `versionCode=2` als lokalen Standard und `versionName=2.0.0_build2`. In CI wird `versionCode` aus `github.run_number` über `-PpulselibreVersionCode` gesetzt; der Versionsname folgt demselben Wert. Ein Update erfordert einen **höheren** versionCode als die installierte App. **Ein höherer versionCode allein garantiert kein Update.**
- Android erlaubt ein In-Place-Update nur bei kompatibler App-Signatur. GitHub Actions kann Debug-APKs mit **stabiler Signatur** erzeugen, wenn alle vier Secrets `PULSELIBRE_SIGNING_KEY_B64`, `PULSELIBRE_SIGNING_STORE_PASSWORD`, `PULSELIBRE_SIGNING_KEY_ALIAS` und `PULSELIBRE_SIGNING_KEY_PASSWORD` gesetzt sind. Ohne diese Secrets wird keine Update-APK als Artifact veröffentlicht. Die Existenz der Secrets und ein erfolgreicher Upgrade-Test sind hier nicht nachgewiesen.
- Für einen verlässlichen Update-Pfad: dedizierten Release-Keystore sicher verwahren, Signierzugriff nur über geschützte CI-Secrets, niemals Schlüssel ins Git einchecken; zuerst signierte interne Testreleases mit stabiler Signatur testen.
- **Deinstallation löscht standardmäßig die privaten SQLite-Daten und Berechtigungen.** `allowBackup=false`; eine automatische Wiederherstellung wird nicht zugesagt. Vor Deinstallation manuell exportieren, wenn lokale Messdaten vorhanden sind.
- Testmatrix: Neuinstallation, Update gleiche Signatur (Daten erhalten), Update andere Signatur (erwartet abgelehnt), ältere Versionsnummer (erwartet abgelehnt), Deinstallation/Neuinstallation (Daten weg), Berechtigungen nach Update, Export nach Prozessneustart.

## Play-Store- und Sicherheits-Gates

1. Konsistente signierte Release-Builds und Android App Bundle (AAB), getrennt von Debug-APKs.
2. Versionsstrategie, Changelog, Upgrade- und Datenbankmigrationstests.
3. Accessibility, adaptive Layouts und Geräte-Matrix; S25 bleibt Release-Blocker.
4. Datenschutzrichtlinie und korrektes Play-Data-Safety-Formular anhand **tatsächlich** verarbeiteter Daten; Diagnose-Teilen und BLE-Werbedaten besonders prüfen.
5. Google-Play-Anforderungen zu Ziel-API, App-Signierung, Store-Listing und Testtracks unmittelbar vor Veröffentlichung anhand der aktuellen Vorgaben prüfen.
6. Keine Behauptung, die App sei bereits Play-Store-fertig; weder produktives Release-Signing noch AAB-Pipeline oder reale Geräte-Matrix sind derzeit abgenommen.
7. Keine INTERNET-Berechtigung und kein Health Connect ohne gesonderte Produktentscheidung.


## CI-Teststrategie für schnelle Beta-Entwicklung (2026-11-16)

- **Jeder Push auf `main` und jeder Pull Request:** Android-Debug-APK bauen, JVM-Unit-Tests ausführen, APK-Signatur und Paket-/Versionsmetadaten prüfen. Bei vollständig konfigurierten Signatur-Secrets wird eine signierte Update-APK aus erfolgreichen Nicht-PR-Builds veröffentlicht.
- **Nicht bei jedem Beta-Push:** vollständiges Android Lint, Instrumentation-Test-APK und API-35-Emulator. Diese Prüfungen bleiben im Workflow erhalten; sie werden nicht gelöscht.
- **Vollprüfung:** GitHub Actions → Android CI → Run workflow → `full_tests` aktivieren; außerdem automatisch für Tags `v*`. Die manuelle Option ist standardmäßig deaktiviert, damit ein normaler Beta-Test schnell bleibt.
- **Änderungsbezogene Zusatztests:** Bei Änderungen an BLE, Berechtigungen, Foreground Services, Datenmigrationen oder UI müssen die betroffenen Tests bewusst ausgewählt und gegebenenfalls die Vollprüfung manuell gestartet werden. Die JVM-Suite ist aktuell nicht dateibasiert gefiltert; sie läuft als günstiger Grundschutz vollständig.
- **Freigabegrenze:** Ein grüner Beta-Build ist kein vollständiger Release-Test und kein Nachweis für echte CIRQA-Hardwarefunktion. Vor einem Release sind Lint, Emulator, Upgrade-Test und relevante Tests am echten Galaxy S25 erforderlich.

## Reihenfolge

P0: Datenschutz des Diagnoseexports und BLE-Fehler-Cleanup; P1: S25-Regression, Upgrade-/Signaturtest, kleine Bildschirme und große Schrift; P2: AAB-Release-Workflow, Store-Compliance, FIT-Decoder und sicherer CIRQA-Datenpfad. Die Kernaufgabe bleibt die verifizierte, sichere Gewinnung **echter** CIRQA-Messwerte.
