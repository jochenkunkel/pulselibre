# PulseLibre — Projektchronik und Entscheidungen

## Ursprung
Die Projekte Mein Weg 2 und PulseLibre entstanden im persönlichen Kontext eines Knochenbruchs, einer Operation und der Genesung. Das vom Projektinhaber bereitgestellte Röntgenbild zeigt die rechte Schulter/den Oberarm mit operativer Stabilisierung. **Das Original ist sensibles medizinisches Bildmaterial und wird nicht ohne gesonderte ausdrückliche Freigabe ins Git-Repository übernommen.** Stattdessen wird ein abstrahiertes grafisches Motiv verwendet.

## Projektauftrag
- Eigenständige, private Android-App als datenschutzorientierte Alternative zur Garmin-Cloud.
- Zielgerät: Garmin CIRQA Smartband; aktuell bereits mit Garmin Connect gekoppelt.
- Vorläufig keine Entkopplung, kein Reset und keine Beeinträchtigung vorhandener Daten.
- Direkte BLE-Erkennung, GATT-Diagnose, später verifizierte Sensordaten und lokale Historie.
- Qualitätsstandard nach Mein Weg 2: Build, Unit-Tests, Android Lint, Emulator-Tests und CI vor Geräteinstallation.

## Umgesetzte Schritte
1. Privates Repository `jochenkunkel/pulselibre` eingerichtet.
2. Android/Kotlin-Prototyp: BLE-Scan, Verbindung, GATT-Dienste und Diagnoseansicht.
3. Android-CI, JVM-Unit-Test, Instrumentation-Starttest und Prüfung fehlender INTERNET-Berechtigung.
4. Qualitätsplan in `docs/QUALITY_GATE.md` und Testplan in `docs/TESTPLAN.md`.
5. Fehlgeschlagener CI-Lauf #7 analysiert: `android-actions/setup-android@v3` scheiterte beim veralteten SDK-Paket `tools`; Schritt entfernt, CI-Erfolg muss erneut geprüft werden.
6. Design aus Mein Weg 2 aufgegriffen: Primärfarbe `#5127C9`, helle Flächen, großzügige Rundungen.
7. Eigenständiges PulseLibre-Motiv mit abstrahiertem Oberarm-Fixationsmaterial und Pulskurve sowie Poster gestaltet.

## Grenzen und Wahrhaftigkeit
- Die gezeigten Dashboard-Messwerte auf einem Konzeptposter sind illustrative Beispieldaten, **keine echten Garmin-Messwerte**.
- Die Garmin-CIRQA-Protokollunterstützung ist nicht implementiert oder verifiziert.
- Eine bestandene Emulatorpipeline kann echte Bluetooth-Hardware nicht vollständig simulieren.
- Keine APK-Freigabe, bevor die obligatorischen CI-Prüfungen erfolgreich sind und offene kritische Tests bearbeitet wurden.
- Das originale Röntgenbild und andere personenbezogene Gesundheitsdaten bleiben außerhalb des Repositories.

## Reproduzierbare Designquellen
- `design/pulselibre-icon.svg`: editierbares Vektorlogo.
- `design/pulselibre-poster.svg`: editierbares, datensparsames Poster mit ausschließlich belegbaren Projektzielen.
- `docs/BRANDING.md`: Farbpalette, Claim und Markenbeschreibung.

## Entwicklungsprüfung 2026-11-15 (13:19 CET)
- Neuester beim Audit sichtbarer Commit: `ddba3f8` (doppelte BLE-`showDevice`-Methode entfernt, um Kotlin-Kompilierung wiederherzustellen). Zuvor wurden Hersteller-IDs und Service-UUIDs in der BLE-Geräteliste ergänzt sowie Tests für Garmin-Advertisement-Hinweise committed.
- GitHub-Connector lieferte für `ddba3f8` keine zugehörigen Pull-Request-Workflow-Runs und keine Commit-Statuschecks. **Das ist kein Nachweis eines erfolgreichen oder fehlgeschlagenen CI-Laufs**; vollständige Actions-Runs und APK-Artefakte müssen gesondert überprüft werden.
- Keine echten CIRQA-BLE-Daten, S25-Hardwaretests oder Garmin-Protokollkompatibilität bestätigt.
- Priorität: (1) Build/Unit-Tests/Lint/Emulator-CI verifizieren und Kompilierungsfehler beheben, (2) installierbare Debug-APK und sicheren BLE-Scan auf S25 testen, (3) danach lokale Speicherung/Verlauf sowie wissenschaftlich belegte Gesundheitsfunktionen inkrementell implementieren.
- Die stündliche Projektprüfung wurde auf sichere kleine Änderungen, Tests, Dokumentation und Git-Commits erweitert. Eine Automatisierung ersetzt keine echte Geräteprüfung oder bestandene CI-Pipeline.

## CI- und S25-Diagnoseprüfung 2026-11-15 (15:16 CET)
- Letzter geprüfter Commit: `32317628` (Bluetooth-Berechtigungsrückmeldung und Scan-Status im Diagnoseexport). [Actions #88](https://github.com/jochenkunkel/pulselibre/actions/runs/43722472003): Workflow **success**, Jobs `build` und `emulator` jeweils **success**; Artifact `pulselibre-debug-apk` vorhanden und nicht abgelaufen. Dies belegt nur CI, nicht einen erfolgreichen Funk-Scan.
- Vorliegende S25-Rückmeldung: Android-Kopplungsliste enthält `CIRQA Smart Band`; passive Diagnose fand zunächst ein nicht identifiziertes Werbegerät, später meldete die Oberfläche 0 Geräte und die Bluetooth-Freigabe war nach Nutzerangabe nicht möglich. Der genaue Berechtigungszustand und die Ursache der 0 Ergebnisse sind **nicht bestätigt**.
- Sicherheitsentscheidung: kein automatischer GATT-Zugriff, keine Entkopplung, keine proprietären Schreibzugriffe. Der neue Berechtigungs-/Fehlertext muss auf dem echten S25 getestet werden; CI-Emulator kann das Samsung-Berechtigungsverhalten nicht verifizieren.
- Nächste priorisierte kleine Aufgabe: Berechtigungszustand und Scan-Callback-Fehler in einer klaren, lokal sichtbaren Diagnose trennen; danach Regressionstests für abgelehnte Berechtigungen und Scan-Fehler ergänzen. Keine weiteren Geräteprotokollannahmen ohne Hardwarebeleg.
