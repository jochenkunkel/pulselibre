# PulsLibre – zentraler Projektstatus und Chat-Übergabe

**Stand:** 2026-11-16 · **maßgeblicher Codezweig:** `main` · **Status:** aktive Entwicklung, keine Produktionsfreigabe.

> **Arbeitsregel für alle Chats und Entwicklungsagenten:** Dieses Dokument und der aktuelle GitHub-`main` sind der gemeinsame, versionierte Übergabepunkt. Ein Chatverlauf wird **nicht automatisch** in andere Chats übertragen. Zu Beginn jeder Arbeit zuerst `main`, diese Datei, die offenen Issues und die CI-Ergebnisse lesen. Widersprüche gegen den aktuellen Code prüfen; nicht ungeprüft aus älteren Chatantworten übernehmen.

## Gesicherter Projektstand

- Android-App `de.pulselibre`, offline-first, Zielgerät Samsung Galaxy S25, Garmin CIRQA.
- Passive BLE-Diagnose und Android-Bluetooth-Berechtigungsdialog wurden nach bisherigen Nutzerangaben auf dem S25 praktisch erprobt. Das ist **kein** Nachweis einer erfolgreichen Gesundheitsdatensynchronisation.
- Im Code existieren nutzerinitiierte **aktive** GATT-Dienstsuche, Garmin-ML-Handshake, experimentelle Echtzeit-Probes und ein Vordergrunddienst für wiederholte Versuche. Diese Pfade sind von der passiven Scan-Funktion zu unterscheiden. Garmin-ML-Probes können GATT-Schreibvorgänge und Notification-Registrierung verwenden.
- Lokale Messwertablage, Dashboard und Exportfunktionen existieren. Herkunft und Korrektheit von CIRQA-Gesundheitswerten auf echter Hardware sind **nicht unabhängig bestätigt**.
- `app/build.gradle.kts`: Standard-`versionCode=2`, `versionName=2.0.0_build2`; CI überschreibt den Code mit `github.run_number`. Stabile CI-Debug-Signierung benötigt vier GitHub-Secrets. Deren Einrichtung ist **nicht bestätigt**.
- **Aktueller Workflow (nach `e4a6e23`):** `main`-Pushes und Pull Requests führen Build und JVM-Tests aus; Lint, Instrumentation-APK und API-35-Emulator laufen nur bei `v*`-Tags oder `workflow_dispatch` mit `full_tests=true`. Dies ersetzt die kurzzeitig in `548f810` eingeführten Full-Gates auf `main`. Ein grüner Beta-Build reicht nicht zur Installationsfreigabe. **Aktueller Workflow nach `3e2d476`:** `cancel-in-progress: true`; neuere Läufe auf derselben Ref brechen ältere ab. Vorherige Aussage zu `false` war überholt.
- Repository ist privat; nur berechtigte Mitarbeitende können Issues und Dokumente lesen.

## Bereits direkt in `main` umgesetzte Arbeiten

| Bereich | Nachweis / Commit | Einordnung |
| --- | --- | --- |
| Nachtaufzeichnung: doppelte Abschlussmeldungen, veraltete Callbacks | `584e477`, `96fc045` | Codeänderung vorhanden; Regressionstests offen |
| README und aktive CIRQA-Pfade | `44a3558` | Dokumentation aktualisiert |
| Auditbericht `docs/AUDIT_2026-11-16.md` | `c851a66` | Teilprüfung, kein vollständiges Audit |
| Release-/Signing-Dokumentation | `5708eb2` | Aktualisiert; Upgrade-Test offen |
| Qualitäts-Gates | `04aeebb` | Ist-/Soll-Zustand dokumentiert |
| Roadmap | `cfe4d78` | Aktive Probes berücksichtigt |
| Historische Architekturreview | `8b61648` | Als historische Momentaufnahme markiert |
| Diagnoseexport-Texte | `28452ab` | Passive Scans von aktiven Probes abgegrenzt |

## Offene GitHub-Issues – verbindliche Aufgabenliste

- **#2 (P0):** Aktive CIRQA-GATT-Kommunikation und Dokumentation abgleichen. Doku-Fortschritt vorhanden; S25-Interoperabilität und Sicherheit offen.
- **#3 (P1):** Timeout-/Callback-Zustände der Nachtaufzeichnung absichern. Erste Korrektur implementiert; deterministische Tests und Geräteprüfung offen.
- **#4 (P1):** CI, Versionen und signierte APK-Updates konsistent machen. Dokumentation aktualisiert; vollständige CI, Secrets und Upgrade-/Datenerhalt-Test offen.
- **#5 (P1):** Historische Review-Befunde und Datenschutz-/Exportpfade revalidieren. Teilabgleich erfolgt; vollständige Datenfluss- und Regressionstests offen.
- **#6 (P2):** Vollständiges dateiweises Codeaudit und Testinventur. Noch nicht abgeschlossen.

Issues: https://github.com/jochenkunkel/pulselibre/issues

## Priorisierte nächste Arbeit

1. Aktuellen `main`-CI-Lauf prüfen; Build-/Testfehler beheben. Full-Test-Lauf (Lint, Instrumentation, API-35-Emulator) ausführen und Ergebnis dokumentieren.
2. Zu #3 Tests für verspätete, doppelte und nach Stop eintreffende BLE-Callbacks sowie Timeout- und Lifecycle-Fälle ergänzen.
3. Zu #2/#5 aktive GATT-/ML-Pfade, Nutzerfreigabe, Garmin-Connect-Interoperabilität und sensible Exportdaten im Code prüfen und absichern.
4. Zu #4 Signing-Secrets **ohne Geheimnisse im Repository** prüfen; Update mit identischem Signaturzertifikat, höherem versionCode und Erhalt lokaler Daten auf dem S25 testen.
5. Zu #6 vollständiges Datei-/Testinventar mit nachvollziehbaren Prüfergebnissen führen.

## Übergabeprotokoll für andere Chats / Agenten

1. `git fetch origin && git checkout main && git pull --ff-only origin main` (bei lokalem Git-Checkout; ungesicherte Änderungen vorher sichern).
2. `docs/PROJECT_STATUS.md`, `README.md`, `docs/AUDIT_2026-11-16.md` und die Issues #2–#6 lesen.
3. Den neuesten Code und die jüngsten Commits mit diesem Status abgleichen. **Git-Code und tatsächliche CI-Evidenz haben Vorrang vor historischen Beschreibungen.**
4. Arbeit nach der Nutzeranweisung direkt auf `main` committen; kleine, nachvollziehbare Commits bevorzugen. Vorher Risiken und mögliche Auswirkungen auf Gerätedaten prüfen.
5. Nach jeder Änderung passende Tests ausführen oder ausdrücklich als **nicht ausgeführt** kennzeichnen; betroffene Issues kommentieren und **diese Datei** mit Datum, Commit, tatsächlichem Ergebnis und nächsten Schritten aktualisieren.
6. Issues erst schließen, wenn ihre Abnahmekriterien mit überprüfbarer Evidenz erfüllt sind. Keine CI-, S25- oder Datenzugriffs-Erfolge behaupten, die nicht nachgewiesen wurden.
7. **Kein automatischer Chat-Sync:** Neue Erkenntnisse aus anderen Chats müssen explizit in GitHub dokumentiert werden. Diese Datei ist ein Übergabeverfahren, kein technischer Zugriff auf andere Chatverläufe.

## Referenzen

- [Teil-Audit](AUDIT_2026-11-16.md)
- [Historische Architekturreview](ARCHITECTURE_AND_CODE_REVIEW.md)
- [Release-/Update-Bereitschaft](ANDROID_RELEASE_READINESS.md)
- [Qualitäts-Gates](QUALITY_GATE.md)
- [Roadmap](ROADMAP_TO_CIRQA_DATA.md)

## Agentenprüfung 2026-11-16, ca. 11:04 CET

- Vorheriger `main`-Stand: `2ffcd026`; README, Zielgeräte, Anforderungen, Chronik, Roadmap, Workflow, Status und offene Issues #2–#6 sowie Draft-PR #1 gelesen. Andere Projektchats sind nicht direkt vollständig abrufbar; keine automatische Chat-Synchronisierung behauptet.
- **Implementiert und committed:** `.github/workflows/android.yml` führt Full-Gates (Lint, Instrumentation-APK und API-35-Emulator) jetzt auch auf `main`-Pushes aus. Commit `548f810`; dadurch wird ein reiner grüner JVM-Build auf `main` nicht länger mit einem vollständig geprüften Build verwechselt. Kein Secret, BLE- oder Signing-Code geändert.
- **Prüfgrenze:** Commit-Workflow-Abfrage für `2ffcd026` lieferte `[]` und Combined-Status ebenfalls `[]`; der Connector filtert Commit-Workflow-Runs auf PR-Auslöser und zeigt nicht zuverlässig normale `main`-Push-Runs. Deshalb **CI-Status unbekannt**, nicht grün. Neue Workflow-Änderung wurde nicht lokal gebaut und nicht am S25 getestet. Actions-Weboberfläche bzw. vollständige Run-API für tatsächlichen Status prüfen.
- **Risiko:** Zusätzliche Emulator-Laufzeit auf jedem `main`-Push; globale `cancel-in-progress`-Concurrency kann bei mehreren unmittelbar folgenden Commits den vorigen Lauf abbrechen. Keine APK-Installations- oder Produktivfreigabe.
- **Nächster Schritt:** Vollständigen Actions-Run für den neuesten `main`-Commit prüfen und Fehler beheben; anschließend deterministische Tests für Issue #3 und Datenschutzprüfung von Issue #5 fortsetzen.

## Agentenprüfung 2026-11-16, ca. 12:03 CET

- Jüngster vor Prüfung sichtbarer `main`-Commit: `0feba90` (CI-Gate-Dokumentation); neue Commits seit letzter Übergabe: `6235f09`, `ba4ff95`, `e694210`, `3fe7415`, `e4a6e23`, `0feba90`. Aktive GATT-/Notification-Codekorrekturen sind **nicht** durch einen hier bestätigten Testlauf validiert. Keine weitere aktive BLE-Kommunikation verändert.
- README-CI-Matrix war nach `e4a6e23` veraltet und behauptete Full-Gates bei jedem Push/PR. In `e4ca105` korrigiert: Build/JVM bei Push/PR; Full-Lint und Emulator nur bei Tags oder manuell mit `full_tests=true`. Diese Statusdatei ebenfalls aktualisiert.
- `fetch_commit_workflow_runs(0feba90)` lieferte `[]`; diese Schnittstelle filtert PR-Runs und ist kein zuverlässiger Nachweis für Push-CI. **Build unbekannt, Unit-Tests nicht nachgewiesen, Lint/Emulator nicht nachgewiesen, S25-Test nicht durchgeführt.** Keine APK-Installations- oder Produktivfreigabe.
- README, Zielgeräte, Anforderungen, Chronik, Roadmap, offene Issues #2–#6, Draft-PR #1, Workflow und Status geprüft. Andere Chats sind nicht vollständig direkt zugänglich; keine neue Hardware-Evidenz erfunden.
- **Nächster Schritt:** Full-Test-Lauf über GitHub Actions (`workflow_dispatch`, `full_tests=true`) für den neuesten `main` prüfen; anschließend Issue #3 mit deterministischen Callback-/Timeout-Tests bearbeiten. Die Workflow-API kann diesen manuellen Lauf hier nicht auslösen oder zuverlässig den Push-Status abrufen. Risiken: CI-Run-Abbrüche durch globale Concurrency, unbekannte Signing-Konfiguration, fehlender CIRQA-Hardware-Nachweis.

## S25-Betatest und historische Bewegungsdaten (Nutzerrückmeldung 2026-11-16)

- Nutzer berichtet für Beta #284: App startet und navigiert, CIRQA wird im BLE-Scan erkannt; aktive Messwert-Probes liefern Werte, die im Dashboard sichtbar sind. Garmin Connect funktionierte während des Tests ebenfalls; dies belegt nur die beobachtete Testkonstellation, keine dauerhafte Parallelitätsgarantie. Neustart erhält die angezeigten Daten.
- Exportansicht ist optisch inkonsistent und hat keinen eigenen Zurück-Button. CSV-Export enthält nur die Kopfzeile. Codeursache: Dashboard liest CIRQA-Ereignisse aus `CirqaDatabase`, CSV liest `LocalReadingStore`; eine validierte Überführung/Exportabbildung fehlt. Keine Rohdaten oder Gerätekennungen ins Git übertragen.
- Nutzer priorisiert Bewegungssensor/Schritte und historischen Nachbezug nach Abwesenheit des Handys. Garmin Connect konnte laut Nutzer historische Daten nachladen. **PulsLibre kann den historischen CIRQA-Download noch nicht nachweislich durchführen.** Realtime-Schrittezähler ist kein Beleg für Roh-Bewegungssensorzugriff oder FIT-Historie.
- Commit `a238df1`: zusätzliche reine JVM-Tests für zeitliche Reihenfolge, Replay-Deduplizierung, Mehrgeräte-/Metriktrennung, ungültige Werte und Cursor-Verhalten. Tests prüfen **nur die lokale Merge-Logik**, nicht das proprietäre Garmin-History-Protokoll, keinen BLE-Transfer und keine historische Speicherung auf dem Band. CI-Ergebnis separat prüfen.
- Nächste Umsetzungsschritte: (1) sichere read-only Untersuchung von CIRQA-Datei-/History-Metadaten ohne Lösch-/ACK-Befehle, (2) echte Dateitransfer-/FIT-Decodierung anhand validierter Geräte-Captures und öffentlicher Referenzimplementierungen, (3) unterbrechbaren, wiederaufnehmbaren Backfill mit Transaktionen und Cursor erst nach persistiertem Batch, (4) gezielte S25-Abwesenheits-/Wiederverbindungs-Tests mit Garmin Connect als Referenz, (5) Dashboard-/CSV-Export vereinheitlichen und Exportnavigation korrigieren.

## Agentenlauf 2026-11-16, 13:19 MEZ – Overnight-Timeout
- **Quellstand vor Änderung:** main mit CIRQA-History-Diagnose bis `185033b0`; zwei sichtbare Branches (`main`, `audit/documentation-consistency-2026-11-16`); Issues #2–#6 und Draft-PR #1 weiterhin offen. README, Zielgeräte, Grundsätze, Projektchronik, Roadmap und Android-Workflow gelesen.
- **Implementiert und committed:** `4694cded` trennt den 120-s-Timeout der Nachtaufzeichnung vom periodischen Poll und entfernt den Timeout-Callback bei erfolgreichem Abschluss sowie bei Bluetooth-aus. Ein verspäteter Timeout darf nach abgeschlossenem Versuch nicht als neues Fehlerereignis protokolliert werden. Service-Stopp entfernt weiterhin alle Handler-Callbacks. Keine neue GATT-Schreiboperation.
- **Prüfgrenze:** Statische Codeprüfung; weder lokaler Gradle-Build noch automatisierte Tests oder S25-Test in diesem Lauf ausgeführt. GitHub-Connector `fetch_commit_workflow_runs` filtert PR-Runs und kann normale Push-CI nicht zuverlässig bestätigen. CI-Status daher **unbekannt**, keine APK-/Installations-/Produktivfreigabe.
- **Offen/Risiko:** Deterministische Tests für Erfolg/Timeout/späte Callback-Reihenfolge und Stop/Neustart fehlen; mögliche Reentranz und Probe-Ownership in `CirqaOvernightService` sind weiter Issue #3. History-Diagnose ist nicht gleich validierter Download. Andere Projektchats sind nicht vollständig direkt abrufbar; keine Synchronisation behauptet.
- **Nächster Schritt:** CI des Commits über Actions prüfen; anschließend Timeout-/Generation-Token-Logik in eine unabhängig testbare Zustandsmaschine extrahieren und Regressionstests ergänzen. Vor S25-Installation vollständige Pflicht-Gates prüfen.

## Agentenprüfung 2026-11-16, 14:53 MEZ – GFDI-Diagnose und CI-Evidenz

- Aktueller vor Prüfung sichtbarer `main`-Commit: `46be075e` (begrenzte GFDI-Handle-/Paketlängen-Diagnose). Weitere jüngste Commits: `94d86dc` (S25-GFDI-Handshake dokumentiert), `1ff326c` (CSV-Zeitfenster), `fab1531` und `d488de4` (CSV-Oberfläche), `fd04417` (Dashboard-Polling). Zwei Branches sichtbar: `main` und `audit/documentation-consistency-2026-11-16`; Issues #2–#6 und Draft-PR #1 offen. Dies sind Commit-Inhalte, keine neue Hardwarevalidierung durch diesen Lauf.
- README, Zielgeräte, Anforderungen, Projektchronik, Roadmap, Status und Workflow gelesen. Die CI-Concurrency verwendet inzwischen `cancel-in-progress: false`; die alte Warnung über absichtliche Abbrüche ist veraltet. Push/PR: Build und JVM-Tests; vollständige Lint-/Instrumentation-/Emulator-Gates nur auf v-Tags oder manuell mit `full_tests=true`.
- Statische Prüfung von `46be075e`: Die neue Handle-Zuordnung betrachtet das erste Notification-Byte und zählt Paketlängen; dies ist **heuristische Diagnose**, kein validierter GFDI-Dateitransfer. `historyLengths` ist eine mutable Map ohne explizite Größenobergrenze, obwohl die Ausgabe auf 300 Zeichen gekürzt wird. Vor weiterem Einsatz prüfen: Speicherbegrenzung, Reset bei Sitzungswechsel, falsche Handle-Treffer, Fragmentierung und Regressionstests mit synthetischen Paketen. Keine unfreigegebenen GATT-Schreibvorgänge ergänzen.
- Commit-Workflow-Abfrage für `46be075e`: `[]`; kombinierte Statuschecks ebenfalls `[]`. **Connector-Einschränkung:** Commit-Workflow-Abfrage filtert PR-Runs und deckt normale Push-Runs nicht zuverlässig ab. Daher Build-/Unit-/Lint-/Emulator-Status des aktuellen Heads **unbekannt**. In diesem Lauf keine Tests ausgeführt, kein neuer S25-Test und keine APK-/Produktivfreigabe.
- Andere Chats sind nicht vollständig direkt auslesbar. Aus dem Git-Übergabeprotokoll stammt die frühere S25-Rückmeldung zu Beta #284; sie wurde hier nicht erneut unabhängig geprüft.
- **Nächster sicherer Schritt:** Reine, JVM-testbare GFDI-Notification-Klassifikation mit begrenzten Statistik-Buckets und Tests für gültigen Handle, unbekannten Handle, leere Pakete und Reset; anschließend vollständige CI und S25-Hardwareverifikation. Issue #3 (Overnight-Callbacks) bleibt ebenfalls offen. Keine Secrets oder Release-Signatur geändert.

## Agentenprüfung 2026-11-16, 16:07 MEZ – begrenzte Historien-Diagnose

- Vorheriger `main`-Head: `63e0963` (BLE-Ownership-Tests); neue konkurrierende Änderungen seit der vorherigen Übergabe betreffen unter anderem Garmin-History-Transportinspektion, CSV-Layout und exklusiven BLE-Sitzungsbesitz. README, Zielgeräte, Grundsätze, Chronik, Roadmap, Workflow, Issues #2–#6 und Draft-PR #1 geprüft. Andere Chats nicht vollständig direkt zugänglich; keine neue S25-Evidenz behauptet.
- **Implementiert/committed:** `b196ef8`, `de06009`, `51e97b2`: GFDI-Paketlängenstatistik aus `CirqaMlHandshakeProbe` in `BoundedPacketLengths` ausgelagert; maximal 64 unterschiedliche Längen-Buckets, zusätzlicher Overflow-Zähler, Reset zwischen Sitzungen; drei synthetische JVM-Tests (Sortierung/Wiederholungen, Überlauf, Reset). Keine BLE-Protokollbefehle, Secrets oder Signing-Einstellungen verändert.
- **Prüfgrenze:** Tests geschrieben, aber nicht lokal ausgeführt. GitHub-Combined-Status für `51e97b2` lieferte keine Statuschecks; das bestätigt keinen erfolgreichen Build. Normale Push-Workflow-Runs sind über die verfügbare Connector-Abfrage nicht zuverlässig abrufbar. Build, JVM, Lint, Emulator und S25 für diesen Commit **unbestätigt**; keine Installations- oder Produktivfreigabe.
- **Risiken:** Die Handle-Klassifikation ist weiterhin heuristisch; die Begrenzung betrifft nur die Längenstatistik, nicht sämtliche Diagnosepuffer. Der neue Histogramm-Code benötigt erfolgreiche CI und eine weitere Prüfung von Nebenläufigkeit/Callback-Reihenfolge. Exklusiver BLE-Gate-Mechanismus aus paralleler Entwicklung benötigt Hardwareverifikation.
- **Nächster Schritt:** Actions-Run des neuesten `main` verifizieren, ggf. Kotlin-Compiler-/Testfehler beheben; anschließend weitere History-Diagnosepuffer und Lifecycle-/Callback-Rennen prüfen. Kein historischer CIRQA-Download bestätigt.


## Agentenprüfung 2026-11-16, ca. 17:07 MEZ – zweite ungebundene Längenstatistik

- Ausgangsstand `6731d313`; `main`, Audit-Branch, README, TARGET_DEVICES, REQUIREMENTS_AND_PRINCIPLES, PROJECT_HISTORY, ROADMAP_TO_CIRQA_DATA, Workflow, Issues #2–#6 und Draft-PR #1 geprüft. Kein neuer Git-Commit zwischen dem letzten dokumentierten Stand und diesem Lauf.
- **Befund:** `GarminHistoryTransportInspector` enthielt unabhängig von `CirqaMlHandshakeProbe` noch eine unbeschränkte `sortedMapOf<Int, Int>` für Paketlängen. Die bloße Begrenzung der ausgegebenen Zeichen verhindert kein Wachstum der Map.
- **Implementiert und committed:** `2a0b433` verwendet die bestehende `BoundedPacketLengths`-Komponente (64 Buckets, Überlaufzähler). `02b1d15` ergänzt synthetische JVM-Tests für 70 verschiedene Paketlängen und wiederholte Längen nach Überlauf; `ae68867` korrigiert eine Testannahme über die absichtlich gekürzte Ausgabe. Kein BLE-Schreiben, keine Geräteaktion, keine Secrets oder Signing-Änderung.
- **Build-/Testgrenze:** Tests geschrieben, nicht ausgeführt. Vor Änderung meldeten Combined-Status und `fetch_commit_workflow_runs` für `6731d313` jeweils leere Ergebnisse. Letztere API filtert nur PR-getriggerte Runs; normale Push-CI bleibt darüber nicht verifizierbar. Neue Commits benötigen weiterhin echte Actions-Evidenz. Kein S25-Test, keine Installations- oder Produktivfreigabe.
- **Risiken:** Heuristische GFDI-Zuordnung und mögliche Überläufe anderer Zähler; Zusammenfassung kürzt Buckets, weist aber Überlauf separat aus. Keine validierten historischen Messwerte. Andere Projektchats sind nicht vollständig direkt zugänglich; die bereits dokumentierten Nutzerberichte sind nicht erneut unabhängig geprüft.
- **Nächster Schritt:** Aktuelle GitHub-Actions-Jobs und Logs in der Weboberfläche verifizieren; danach Zustandsübergänge der Overnight-Callbacks (Issue #3) deterministisch testen und verbleibende Diagnosepuffer prüfen.


## Agentenprüfung 2026-11-16, ca. 17:55 MEZ – CI-/FileSync-Sicherheitsabgleich

- Neueste sichtbare Commits bis `3e2d476` geprüft. Seit dem vorherigen Agentenbericht wurden GFDI/COBS-Inspektion und eine **aktive** FileSync-V2-Dateilistenanfrage ergänzt (`c0f7050`, `b3658af`, `10f6dab`). Der UI-Pfad zeigt einen ausdrücklichen Bestätigungsdialog, sendet nach Bestätigung jedoch tatsächlich GATT-/ML-Nachrichten; **nicht** als rein passiv oder generell schreibfrei bezeichnen. Keine erfolgreiche CIRQA-Antwort oder Datensynchronisation nachgewiesen. Hardware-/Garmin-Connect-Verträglichkeit offen; keine weiteren GATT-Operationen in diesem Durchlauf vorgenommen.
- **Dokumentationsabweichung behoben:** Workflow `android.yml` verwendet nach `3e2d476` `cancel-in-progress: true`, entgegen dem zuvor dokumentierten `false`. Bei schnellen Commits können alte Läufe abgebrochen werden. Build/JVM auf Push; Full-Lint/Emulator nur auf `v*`-Tags oder manuell `full_tests=true`.
- **CI-Evidenz:** Combined-Status und Commit-Workflow-Abfrage für `3e2d476` ergaben jeweils leere Listen. Die Workflow-Abfrage erfasst nur PR-getriggerte Runs; daraus folgt weder Erfolg noch Misserfolg der Push-CI. Build, JVM-Tests, Lint, Emulator und S25-Test dieses Stands **nicht bestätigt**. Keine Installations-/Produktivfreigabe.
- **Quellenabgleich:** README, TARGET_DEVICES, REQUIREMENTS_AND_PRINCIPLES, PROJECT_HISTORY, ROADMAP-Pfad, PROJECT_STATUS, Workflow, offene Issues und PR-Suche geprüft. Die PR-Suche ergab keine offenen Treffer, was den früher dokumentierten Draft-PR #1 nicht als geschlossen beweist. Andere Projektchats nicht vollständig direkt zugänglich.
- **Nächster Schritt:** FileSync-Anfrage samt Sicherheitsgrenzen und UI-Text gegen tatsächliche GATT-Schreibsequenz prüfen; vor Hardwareversuch vollständige CI-Evidenz und explizite Freigabe sicherstellen. Danach deterministische Nachtaufzeichnungs-Regressionstests (#3). Keine Secrets, Signing- oder Protokolloperationen geändert.
