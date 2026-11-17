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


## Repository-Hygieneprüfung 2026-11-17 (12:32 CET)
- **Auftrag geändert:** Der stündliche Agent ist bis zur erneuten Freigabe ausschließlich für sichere Repository-Pflege zuständig; keine eigenständige Produktentwicklung. Siehe `docs/AUTONOMOUS_DEVELOPMENT_AGENT.md` für den früheren Entwicklungsauftrag; der aktuelle Automatisierungsauftrag hat Vorrang.
- `.gitignore` geprüft: `**/build/`, `.gradle/`, `*.apk`, `*.aab`, `*.jks`, `*.keystore`, `*.log`, lokale Daten/Exports und Schlüsselmaterial sind bereits ausgeschlossen. Kein begründeter Änderungsbedarf.
- Beim Audit sichtbare Branches: `main`, `audit/documentation-consistency-2026-11-16`, `codex/dashboard-overview`, `codex/phone-preview-20261117`, `codex/remove-legacy-night-recording`, `docs/codex-onboarding-2026-11-17`. **Nicht gelöscht**: PR-/Merge- und Nutzungsstatus vor einer Löschung klären.
- Neuester sichtbarer Commit: `6afb00f` (Dokumentation zur synthetischen SYNC-Diagnose). Dieser Entwicklungsplan war auf 2026-11-16 datiert und ist kein Nachweis des aktuellen Produktstands.
- GitHub-Connector lieferte für `6afb00f` keine Workflow-Runs. CI-Ergebnis **unbekannt**, nicht grün.
- Alte GitHub-Actions-Artefakte/APKs: Bei dieser Prüfung **nicht inventarisiert**; ohne nachgewiesene Aufbewahrungsanforderungen, API-Zugriff und ausdrückliche Löschfreigabe keine Entfernung.
- Offene Issues vorhanden (u.a. #7 CIRQA-Referenztrace und #11 wissenschaftliche Tacho-Personalisierung). Nicht als Reinigungsreste behandeln.
- **Nächster Hygiene-Schritt:** Referenzen und PRs der fünf Nebenbranches prüfen, anschließend auf tatsächlich eingecheckte Binär-/Builddateien, Duplikate und verwaiste Dokumentation prüfen. Nur sichere, nachvollziehbare Änderungen committen.
