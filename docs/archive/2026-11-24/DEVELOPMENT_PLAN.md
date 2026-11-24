# PulsLibre – fortführbarer Entwicklungsplan

> **Hinweis zur Gültigkeit (19.11.2026):** Der nachfolgende Projektstatus und die priorisierten Entwicklungsaufgaben sind ein historischer Stand vom 16.11.2026, **keine aktuelle Arbeitsfreigabe**. Insbesondere ist die Aussage, CIRQA-Messdaten seien nicht auf Hardware bestätigt, durch die dokumentierten V24/V25-Gerätetests überholt: 33 Dateien übertragen, davon 31 FIT-Dateien validiert; 294 Metrikzeilen importiert. Die vollständige historische Abdeckung ist weiterhin nicht belegt. Maßgeblich sind [../../PROJECT_STATUS.md](../../project/PROJECT_STATUS.md) und [CIRQA-Forschungsstand vom 18.11.2026](../../protocol/research/CIRQA_HISTORICAL_RESEARCH_STATUS_2026-11-18.md). Der aktuell geltende Auftrag beschränkt sich auf reversible Repository-Hygiene; keine Produktentwicklung.

Stand: 2026-11-16. Maßgeblich sind die tatsächlichen Repository-Dateien, GitHub-Checks und dokumentierte Hardwaretests; diese Übersicht ist keine Build-Freigabe.

## Projektstatus
- Android/Kotlin-BLE-Diagnoseprototyp; Garmin-CIRQA-Protokoll und echte Gesundheitsmessungen noch nicht auf Hardware bestätigt.
- Neueste beim Audit sichtbare Commits: `bf784db` (APK-Verteilung/Signatur dokumentiert), `9efc7a9` (lokaler Schlüsselgenerator), `204c541` (CI-APK-Veröffentlichung absichern), `3e3bd1d` (Version `2.0.0_buildN`). Implementiert/committed bedeutet nicht getestet.
- Zielgeräte und Schutzregeln: `docs/project/TARGET_DEVICES.md`; Anforderungen: `docs/project/REQUIREMENTS_AND_PRINCIPLES.md`; Historie: `docs/project/PROJECT_HISTORY.md`.

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
1. README, diese Datei, `docs/project/TARGET_DEVICES.md`, Anforderungen, Testplan und Historie lesen.
2. GitHub-Commits, Issues/PRs und aktuelle CI-Läufe vergleichen; wenn möglich konkrete Workflow-Jobs/Logs prüfen.
3. Kleinste sichere priorisierte Änderung auswählen, relevanten Code lesen, Regressionstest ergänzen, committen.
4. Nach Commit CI prüfen und diese Datei mit belegten Ergebnissen, Risiken und nächster Aufgabe aktualisieren.

## Architekturentscheidungen
- Offline-first, kein INTERNET-Permission und keine sensiblen Rohdaten im Repository.
- BLE-Diagnostik strikt von nicht verifizierter Garmin-Protokollunterstützung trennen.
- Keine unkontrollierte APK-Auslieferung mit wechselnder Signatur; Updatefähigkeit muss belegt sein.


## Repository-Hygieneprüfung 2026-11-17 (12:32 CET)
- **Auftrag geändert:** Der stündliche Agent ist bis zur erneuten Freigabe ausschließlich für sichere Repository-Pflege zuständig; keine eigenständige Produktentwicklung. Siehe `docs/project/AUTONOMOUS_DEVELOPMENT_AGENT.md` für den früheren Entwicklungsauftrag; der aktuelle Automatisierungsauftrag hat Vorrang.
- `.gitignore` geprüft: `**/build/`, `.gradle/`, `*.apk`, `*.aab`, `*.jks`, `*.keystore`, `*.log`, lokale Daten/Exports und Schlüsselmaterial sind bereits ausgeschlossen. Kein begründeter Änderungsbedarf.
- Beim Audit sichtbare Branches: `main`, `audit/documentation-consistency-2026-11-16`, `codex/dashboard-overview`, `codex/phone-preview-20261117`, `codex/remove-legacy-night-recording`, `docs/codex-onboarding-2026-11-17`. **Nicht gelöscht**: PR-/Merge- und Nutzungsstatus vor einer Löschung klären.
- Neuester sichtbarer Commit: `6afb00f` (Dokumentation zur synthetischen SYNC-Diagnose). Dieser Entwicklungsplan war auf 2026-11-16 datiert und ist kein Nachweis des aktuellen Produktstands.
- GitHub-Connector lieferte für `6afb00f` keine Workflow-Runs. CI-Ergebnis **unbekannt**, nicht grün.
- Alte GitHub-Actions-Artefakte/APKs: Bei dieser Prüfung **nicht inventarisiert**; ohne nachgewiesene Aufbewahrungsanforderungen, API-Zugriff und ausdrückliche Löschfreigabe keine Entfernung.
- Offene Issues vorhanden (u.a. #7 CIRQA-Referenztrace und #11 wissenschaftliche Tacho-Personalisierung). Nicht als Reinigungsreste behandeln.
- **Nächster Hygiene-Schritt:** Referenzen und PRs der fünf Nebenbranches prüfen, anschließend auf tatsächlich eingecheckte Binär-/Builddateien, Duplikate und verwaiste Dokumentation prüfen. Nur sichere, nachvollziehbare Änderungen committen.

## Repository-Hygieneprüfung 2026-11-17 (15:35 MEZ)
- **Geltender Auftrag:** ausschließlich sichere, reversible Repository-Hygiene; die älteren Abschnitte „Priorisierte Aufgaben“ und „Ablauf für den nächsten Agenten“ beschreiben den historischen Entwicklungsauftrag und erteilen derzeit **keine** Entwicklungsfreigabe.
- GitHub-Metadaten: Repository **public**; die frühere Datenschutz-/Git-Historienfrage ist nicht als erledigt belegt. Keine personenbezogenen Rohdaten veröffentlichen, keine Historie automatisch umschreiben.
- Letzter bei dieser Prüfung gefundener Commit: `ef19503` (2026-11-17 14:35 UTC); Commits können parallel weiterlaufen. Branchsuche ergab zehn Namen einschließlich `main`; offener PR #13. Kein Branch ohne Eigentümer-/Referenzprüfung löschen.
- `.gitignore` enthält bereits `**/build/`, `.gradle/`, `*.apk`, `*.aab`, `*.class` **nicht ausdrücklich**; `*.class` unter `build/` wird durch die Verzeichnisregel abgedeckt. Eine GitHub-Codesuche nach Buildmustern lieferte keine beweiskräftige vollständige Inventur. Vor Änderungen den vollständigen Git-Tree prüfen.
- Workflow `.github/workflows/android.yml`: Upload-Artefakte mit `retention-days: 14`; ältere Runs/Artefakte nicht inventarisiert und nicht gelöscht. Keine Löschfreigabe.
- Combined-Status und commitbezogene Workflow-Abfrage für `ef19503` ergaben leere Listen. Letztere filtert auf PR-Runs und erfasst Push-Runs nicht vollständig: **CI unbekannt**, nicht erfolgreich bestätigt.
- **Nächster Hygiene-Schritt:** vollständige Tree-Inventur und Linkprüfung, Branch-Referenzen mit PRs abgleichen; ausschließlich nachgewiesene reversible Korrekturen durchführen.

## Repository-Hygieneprüfung 2026-11-17 (16:35 MEZ)
- **Sichtbarkeit neu verifiziert:** GitHub-Repository-Metadaten melden jetzt `private` (zuvor `public` im Bericht 15:35). Die ältere Angabe bleibt als historischer Befund stehen. Die Umstellung beweist **keine** Bereinigung früher öffentlich erreichbarer Git-Historie oder externer Kopien.
- Aktueller ermittelter Commit: `aa2fb79` (Merge PR #17, 15:22 UTC); zwölf Branches einschließlich `main`. Branches sind nicht als verwaist nachgewiesen und wurden nicht gelöscht. Offene PR-Suche ergab keine Treffer; offene Issues u. a. #7, #11, #16, #18 und #19 sind reguläre Aufgaben.
- README, `.gitignore` und `.github/workflows/android.yml` geprüft. Build-/APK-Ausschlüsse vorhanden, `*.class` nicht explizit ausgeschlossen, aber reguläre `build/`-Verzeichnisse ausgeschlossen. Codesuche nach `app-debug.apk` fand Referenzen in Dokumentation/Workflow, keinen nachgewiesenen eingecheckten APK-Binärblob; vollständige Git-Tree-Inventur bleibt ausstehend.
- Workflow: Artefaktaufbewahrung 14 Tage; keine Runs, Artefakte, Branches oder Dateien gelöscht. Combined-Status leer; commitbezogene Workflow-Abfrage filtert PR-Runs und kann Push-CI nicht verifizieren. **CI für `aa2fb79` unbekannt.**
- **Nächster Schritt:** Git-Tree-Inventur mit einem geeigneten vollständigen Tree-/Clone-Zugriff; anschließend Link- und Duplikatprüfung. Keine Produktentwicklung.

## Repository-Hygieneprüfung 2026-11-17 (18:30 MEZ)
- GitHub-Metadaten: Repository `private`, Schreibzugriff vorhanden; 13 Branches einschließlich `main`, offene Issues #7, #16, #18, #19, #20, #22; keine offenen PRs in der Suchabfrage.
- Letzter vor dem Hygiene-Commit gefundener Commit: `2f5940c` (Dokumentationsweiterleitung). README, Entwicklungsplan, `.gitignore` und Android-Workflow geprüft.
- **Sichere Änderung:** `.gitignore` um `*.class` ergänzt (Commit `e04a82e`), um versehentlich außerhalb von `build/` erzeugte Java-/Kotlin-Klassendateien auszuschließen. Bestehende Dateien nicht gelöscht. GitHub-Codesuche zeigte nur Textreferenzen, keinen nachgewiesenen versionierten `.class`-Blob; vollständiger Git-Tree-Audit weiterhin offen.
- Workflow enthält `retention-days: 14`; keine Workflow-Runs/Artefakte inventarisiert oder gelöscht. Commitbezogene Workflow-Abfrage liefert nur PR-Runs; leere Listen und Combined-Status beweisen keinen erfolgreichen Push-Build.
- Keine Produktfunktionen, Secrets, Signing, Release-Dateien, Git-Historie oder Branches verändert. Branch-/Dateibesitz und PR-Verweise vor etwaigen Löschvorschlägen prüfen.
- **Nächster Hygiene-Schritt:** Vollständigen Git-Tree mit geeignetem autorisierten Clone/Tree-API inventarisieren, dann Links und Duplikate prüfen. Alte CI-Artefakte nur mit belastbarer Aufbewahrungsinformation auflisten.
