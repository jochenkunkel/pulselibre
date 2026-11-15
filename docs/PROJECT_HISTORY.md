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

## CI-Audit und sichere nächste Schritte (2026-11-15, 16:21 CET)
- Neuester Commit `3ef794fd`: passiver Abgleich der Android-Bond-Adresse mit beobachteten BLE-Werbeadressen; **kein** Verbindungsaufbau und keine GATT-Schreibaktion durch diese Funktion. Eine Übereinstimmung ist ein Adresshinweis, kein Beweis für ein unterstütztes CIRQA-Gesundheitsprotokoll. Rotierende/private BLE-Adressen können einen Abgleich verhindern.
- [GitHub Actions #90](https://github.com/jochenkunkel/pulselibre/actions/runs/43726493230): Workflow `completed/success`, Jobs `build` und `emulator` jeweils `completed/success`; Debug-Artifact `pulselibre-debug-apk` vorhanden (bei Prüfung nicht abgelaufen). Dies sind **CI-Ergebnisse**, keine S25-Hardwareergebnisse.
- Risiko der derzeitigen Umsetzung: `refreshCirqaCard()` wird bei einem passenden Scanresultat aufgerufen und liest Android-Bond-Metadaten erneut; das ist für eine erste Diagnose vertretbar, sollte aber bei weiteren Scan-Funktionen entkoppelt und mit einem reinen, testbaren Adressabgleich versehen werden. Die Diagnose enthält weiterhin BLE-Namen und Service-Daten; vor dem Teilen auf sensible Inhalte prüfen.
- Nächste kleine Priorität: reinen, deterministisch JVM-testbaren Adressabgleich (gleiche Adresse, Groß-/Kleinschreibung, fehlende Bond-Adresse, nicht passende/rotierende Adresse) extrahieren; danach einen Emulator-/UI-Regressionstest für den sichtbaren CIRQA-Status ergänzen. **Keine** Kopplungs-, GATT-Schreib-, Descriptor- oder INTERNET-Änderungen.
- Kein bestätigter neuer S25-Scan und keine echten Gesundheitswerte. Die aktuelle Änderung dieses Audits betrifft ausschließlich Dokumentation; ein neuer CI-Lauf muss separat geprüft werden.

## Entwicklungsprüfung 2026-11-15 (17:15 CET)
- Vor der Änderung war der jüngste Workflow [#100](https://github.com/jochenkunkel/pulselibre/actions/runs/43733929899) noch **in progress**: Build-Job inklusive Unit-Tests, Lint, Debug-/Instrumentation-APK und Artifact-Upload **success**; Emulator-Job noch **in progress**. Dies ist ausdrücklich **kein vollständig grüner Workflow**. Workflow [#97](https://github.com/jochenkunkel/pulselibre/actions/runs/43733837117) war zuvor **completed/success**.
- Priorisierte kleine Änderung: `CirqaAddressMatch` als reine JVM-testbare Vergleichsfunktion ergänzt; vier Tests für Groß-/Kleinschreibung, fehlende/leere Adressen, abweichende (z. B. rotierende) Adressen und Rand-Leerzeichen. Beide bisherigen direkten Vergleiche in `MainActivity.showDevice()` verwenden jetzt dieselbe Funktion.
- Aussagegrenze: Übereinstimmende MAC-Adressen sind nur ein lokaler Diagnosehinweis, keine verifizierte CIRQA-Identität und keine Garmin-Protokollunterstützung. Bei privaten/rotierenden BLE-Adressen sind falsch-negative Ergebnisse möglich. Es wurden weder Verbindungen aufgebaut noch Kopplung, GATT-Schreibzugriffe, Berechtigungen oder Internetzugriff verändert.
- Die **neuen** Änderungen benötigen einen separaten erfolgreichen CI-Lauf; Tests wurden hier nur geschrieben, nicht lokal oder auf einem S25 ausgeführt. Vor einem Hardwaretest Build-, Emulator- und Artifact-Ergebnis des neuesten Commits prüfen.
