# PulsLibre – fortführbarer Entwicklungsplan

Stand: 2026-11-16. Maßgeblich sind die tatsächlichen Repository-Dateien, GitHub-Checks und dokumentierte Hardwaretests; diese Übersicht ist keine Build-Freigabe.

## Projektstatus
- Android/Kotlin-BLE-Diagnoseprototyp; Garmin-CIRQA-Protokoll und echte Gesundheitsmessungen noch nicht auf Hardware bestätigt.
- Neueste beim Audit sichtbare Commits: `bf784db` (APK-Verteilung/Signatur dokumentiert), `9efc7a9` (lokaler Schlüsselgenerator), `204c541` (CI-APK-Veröffentlichung absichern), `3e3bd1d` (Version `2.0.0_buildN`). Implementiert/committed bedeutet nicht getestet.
- Zielgeräte und Schutzregeln: `docs/TARGET_DEVICES.md`; Anforderungen: `docs/REQUIREMENTS_AND_PRINCIPLES.md`; Historie: `docs/PROJECT_HISTORY.md`.

## Abgeschlossen / committed (noch gesondert zu verifizieren)
- BLE-Scanner, Diagnoseansicht, Hersteller-IDs und Service-UUID-Anzeige.
- CI-Konfiguration mit JVM-Tests, Lint, Debug-APK und Emulator-Test.
- Dokumentation für Android-Updatefehler, stabile Signierung und sichere APK-Verteilung.

## Priorisierte Aufgaben
1. **P1 – Build-/Testnachweis:** Aktuellsten vollständigen GitHub-Actions-Lauf inklusive Build, Unit-Tests, Lint und Emulator-Jobs prüfen. Compilerfehler anhand echter Logs beheben; bei fehlendem API-Zugriff CI-Status ausdrücklich als *unbekannt* belassen.
2. **P1 – APK-Updatepfad:** Package-ID, `versionCode`-Monotonie und Signaturgleichheit für aufeinanderfolgende Builds nachweisen. Keinen Schlüssel/Secret ändern oder committen. Keine Update-/Installationsfreigabe ohne erforderliche grüne Checks.
3. **P2 – S25-Hardwareprüfung:** Nach bestandener CI und Freigabe durch Nutzer nur sichere BLE-Scan-Diagnose; Garmin Connect nicht entkoppeln, kein GATT-Schreiben. Beobachtungen anonymisiert dokumentieren.
4. **P3 – Architektur/Produkt:** Lokale persistente Historie, Verlaufsdarstellung, Profileingaben, Ernährung/Energie und Quellen-/Formeltests schrittweise implementieren. Keine erfundenen Messwerte oder Garmin-Protokollzusagen.
5. **P4 – Qualität:** Fehlerfälle (Berechtigungen, Verbindungsabbrüche, Zeitüberschreitungen), Datenschutz, Migration und Accessibility mit Regressionstests abdecken.

## Bekannte Risiken / Grenzen
- Für den jüngsten Commit wurde bei früheren Connector-Abfragen kein vollständiger GitHub-Actions-Run geliefert; leere Ergebnisse sind weder grün noch rot. Beim nächsten Lauf erneut prüfen.
- Release-Signing ist sicherheitskritisch und erfordert ausdrückliche Genehmigung für Änderungen an Secrets/Schlüsseln.
- Keine verifizierten Garmin-CIRQA-Messdaten und kein dokumentierter erfolgreicher Samsung-S25-Hardwaretest.
- Andere Projektchats sind nur insoweit auswertbar, wie sie im aktuellen Lauf tatsächlich technisch zugänglich sind; niemals fehlende Inhalte rekonstruieren.

## Qualitätsstatus (Stand dieses Audits)
| Zustand | Nachweis |
| --- | --- |
| Implementiert/committed | Obige Commits vorhanden |
| Build erfolgreich | Nicht verifiziert |
| Unit-/Lint-/Emulatortests erfolgreich | Nicht verifiziert |
| Auf S25 manuell getestet | Nicht verifiziert |
| Zur Installation freigegeben | Nein, bis erforderliche Checks belegt sind |
| Produktiv freigegeben | Nein |

## Ablauf für den nächsten Agenten
1. README, diese Datei, `docs/TARGET_DEVICES.md`, Anforderungen, Testplan und Historie lesen.
2. GitHub-Commits, Issues/PRs und aktuelle CI-Läufe vergleichen; wenn möglich konkrete Workflow-Jobs/Logs prüfen.
3. Kleinste sichere priorisierte Änderung auswählen, relevanten Code lesen, Regressionstest ergänzen, committen.
4. Nach Commit CI prüfen und diese Datei mit belegten Ergebnissen, Risiken und nächster Aufgabe aktualisieren.

## Architekturentscheidungen
- Offline-first, kein INTERNET-Permission und keine sensiblen Rohdaten im Repository.
- BLE-Diagnostik strikt von nicht verifizierter Garmin-Protokollunterstützung trennen.
- Keine unkontrollierte APK-Auslieferung mit wechselnder Signatur; Updatefähigkeit muss belegt sein.
