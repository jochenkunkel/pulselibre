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
4. Qualitätsplan in `docs/quality/QUALITY_GATE.md` und Testplan in `docs/quality/TESTPLAN.md`.
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
- `docs/product/BRANDING.md`: Farbpalette, Claim und Markenbeschreibung.

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

## Entwicklungsprüfung 2026-11-15 (18:16 CET)
- Vor dieser Änderung: Actions #110 `completed/success`; Actions #111 (Commit `8108432f`) Build-Job `success`, Emulator-Job `in_progress` – daher **nicht** als vollständig grün ausgewiesen.
- Kleine Priorität: die Erststart-Bluetooth-Berechtigungsentscheidung aus `MainActivity` in eine reine `BluetoothPermissionPromptPolicy` extrahiert und vier JVM-Regressionstests ergänzt. Der Scan startet weiterhin ausschließlich nach Nutzeraktion; keine automatische Verbindung, Kopplung oder Schreiboperation.
- S25-Testplan an die inzwischen implementierte Erststart-Abfrage angepasst. Die neue Codeänderung muss in ihrem eigenen GitHub-Actions-Lauf noch Build, Unit-Tests, Lint und Emulator bestehen. Tests wurden geschrieben, nicht lokal ausgeführt.
- Offene Risiken: Android-Systemdialog auf Samsung S25 nicht hardwareverifiziert; abgelehnte Berechtigungen erfordern ggf. manuelles erneutes Anfordern bzw. Android-Einstellungen; CIRQA-Identität und Gesundheitsdaten bleiben unbestätigt. Keine INTERNET-Permission und keine Garmin-Sitzungsänderung hinzugefügt.


## CI- und Datenschutzprüfung 2026-11-15 (19:16 CET)
- Projektgrundlagen, Geräte-/Sicherheitsvorgaben, Roadmap und Test-/Release-Gates erneut gelesen. [Actions #130](https://github.com/jochenkunkel/pulselibre/actions/runs/43745752450) ist `completed/failure`; ebenso #123–#129. Letzter beobachteter vollständig grüner Lauf: [#122](https://github.com/jochenkunkel/pulselibre/actions/runs/43744986171) auf Commit `33c0382c`. **Dieser Lauf validiert nicht die danach vorgenommenen Änderungen.**
- Die Jobs-API meldet für jüngere Fehlversuche keine Steps/Logs; ein früherer Logabruf endete mit 404 `BlobNotFound`. Die Fehlerursache bleibt unbestätigt, möglich ist ein Problem vor dem Start der Runner-Jobs. Siehe `docs/archive/2026-11-24/CI_INCIDENT_2026-11-15.md`. Kein grüner Testnachweis für die jüngsten Commits.
- Sicherheitskorrektur `9484cd51`: Beim expliziten Teilen werden BLE-Gerätedetails und Android-Kopplungseinträge nicht mehr übertragen; nur Anzahlen/aggregierte Diagnosestatus. Dies beseitigt im Standardexport die bisher noch enthaltenen Gerätenamen und möglichen Bluetooth-Adressen. Lokale Geräteansicht bleibt unverändert.
- Einschränkung: Eine zusätzliche automatisierte Prüfung des Android-Share-Intents und ein vollständiges Audit sämtlicher Freitext-Diagnosefelder stehen aus; die neue Änderung ist wegen der CI-Störung **nicht build-/testbestätigt**. Keine S25-Hardwareprüfung erfolgt.
- Nächste Priorität: CI-Runner-/Account-Fehler außerhalb des Codes klären, danach Build/Lint/JVM/Emulator grün bestätigen und einen Regressionstest für die Export-Datenminimierung ergänzen. Keine INTERNET-Permission, aktive CIRQA-GATT-Kommunikation oder Garmin-Kopplungsänderung hinzugefügt.


## CI-Audit und BLE-Regressionsschutz (2026-11-15, 20:22 CET)
- Vor der Änderung: [Actions #148](https://github.com/jochenkunkel/pulselibre/actions/runs/43750598105), Commit `195b1965`, Workflow `completed/success`; beide Jobs `build` und `emulator` erfolgreich. Der Build-Job umfasst JVM-Tests, Lint und APK-Build; Artifact `pulselibre-debug-apk` war bei der Abfrage vorhanden und nicht abgelaufen. Das ist kein S25-Hardwaretest.
- Neue Änderung [`b9d4da93`](https://github.com/jochenkunkel/pulselibre/commit/b9d4da93eb2f8277c43e277878058691e8b8f19b): zwei JVM-Regressionstests für monotone, deduplizierte BLE-Featurezählung und das Weglassen einer Geräteadresse aus der aggregierten CIRQA-Scan-Zusammenfassung. Keine Produktionscodeänderung.
- Diese Tests sind **nur committed**, nicht lokal ausgeführt und erst nach einem erfolgreichen CI-Lauf auf diesem Commit als grün anzusehen. Auch die Dokumentationsänderung benötigt ihre eigene CI-Bestätigung.
- Verbleibende Risiken: Der Scan identifiziert CIRQA nicht eindeutig; rotierende BLE-Adressen verhindern verlässliche Zuordnung. Der aggregierte Scanbericht ist nicht gleichbedeutend mit einer vollständigen Datenschutzprüfung aller UI-/Share-Felder. Kein neuer S25-Hardwarebeleg, keine Gesundheitsdaten und keine aktive Garmin-GATT-Kommunikation.
- Nächste Priorität: den CI-Lauf auf aktuellem Head vollständig prüfen; danach einen Android-Share-Intent-Regressionstest für die Datenminimierung ergänzen.
