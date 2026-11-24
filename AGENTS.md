# PulsLibre – Arbeitsanweisungen für die Haupt-IDE und weitere Entwickler

> **Generalfreigabe des Owners (24.11.2026):** Agenten fragen nicht mehr für jede Einzelmaßnahme beim Owner nach; sie entscheiden innerhalb ihrer Zuständigkeit, setzen um und melden danach. Ausnahmen (Irreversibles/Destruktives, Recht/Datenschutz/Veröffentlichung privater Daten, Geld/Außenwirkung, vorbehaltene Produktentscheidungen) und unverändert geltende Qualitäts-/Koordinationsregeln: [GENERAL_AUTHORIZATION_2026-11-24.md](docs/collaboration/GENERAL_AUTHORIZATION_2026-11-24.md). Ältere „nur mit Freigabe“-Formulierungen gelten außerhalb der Ausnahmen nicht mehr.

> **P0-Owner-Entscheidung (24.11.2026):** [Vollständige lokale Armband-Datenerfassung](docs/protocol/DATA_ACQUISITION_PRIORITY.md) ist verbindlich. Alle technisch zugänglichen, zulässigen Daten einschließlich unvalidierter/unbekannter Werte verlustfrei archivieren und in SQLite inventarisieren; medizinische Validierung darf Erfassung/Persistenz nicht blockieren. Datenschutz, Gerätesicherheit und explizite Hardwarefreigaben bleiben unverändert.

> **P0-Präzisierung (24.11.2026):** Unsichere Dateiidentität verhindert ausschließlich das *Überspringen* von Downloads, niemals den vollständigen erneuten Abruf. Im Zweifel alle gelisteten Dateien erneut read-only übertragen, empfangene Bytes dauerhaft sichern und lokale Duplikate erst nach Byte-/Hashprüfung behandeln. Fehlender Resume-/Skip-Nachweis darf nicht als Blockade für Full-Pull, Persistenz oder Offline-Tests gemeldet werden. Bestehende Laufbudgets, Geräte-Leases und Hardwarefreigaben bleiben bestehen; Stopps werden als unvollständig ausgewiesen. Siehe [Datenpriorität](docs/protocol/DATA_ACQUISITION_PRIORITY.md).

## Forschungsdaten und Git-Fixtures (Projektentscheidung 22.11.2026)

Der Projektverantwortliche erklärt die Daten des aktuellen, nicht am Menschen getragenen CIRQA-Testarmbands als synthetisch und wünscht deren aktive Analyse sowie die Bereitstellung reproduzierbarer Testdaten in Git. **Verbindliche Details:** [Forschungsdatenregel](docs/collaboration/CURRENT_BAND_RAW_DATA_POLICY.md). Agenten dürfen und sollen **nachweislich synthetische, geprüfte** FIT-/SQLite-/Feldmatrix-Fixtures samt Tests im Repository ablegen. Eine pauschale Freigabe aller ungeprüften Roh-Traces, Seriennummern, Kopplungsdaten, Tokens oder fremden Daten ist damit nicht verbunden. Keine neuen aktiven Hardwarekommandos ohne gesonderte Freigabe. Die geparkte Architekturarbeit #496 bleibt unberührt.


Diese Datei gilt repositoryweit. **GitHub ist die verbindliche, versionierte Wissensbasis**, nicht eine Chat-Unterhaltung oder eine lokale IDE-Sitzung. Für den aktuellen Projektstand zuerst `docs/project/PROJECT_STATUS.md` lesen; für den vollständigen Einstieg `docs/project/CHAT_START_HERE.md`.

## Verbindlicher gemeinsamer Agenten-Chat (ab 21.11.2026)

**Vor und nach jedem Task muss jeder aktive Agent im gemeinsamen [Agenten-Chat](docs/collaboration/agent-chat/README.md) eine START- bzw. ENDE-Nachricht veröffentlichen.** Der Chat enthält laufende Aufgaben, konkrete Zuständigkeiten, geplante Dateieingriffe, Fortschritt, Nachweise und ausdrücklich **was welcher Agent von wem benötigt**. Blockaden und Antworten ebenfalls dort posten; konkrete adressierte Abstimmungen zusätzlich über `agent-sync/` führen. Vor Arbeitsbeginn neue Nachrichten und laufende Sitzungen lesen. Ein Chat-Post ist erst nach Push/Fetch bei anderen sichtbar; er ersetzt weder `agent-activity` noch die exklusive S25-Sperre. Keine parallele Bearbeitung derselben Aufgabe.


## Verbindliche Reaktionspflicht und aktives Warten (21.11.2026)

**Diese Regeln ändern keine bestehenden Tickets oder Prioritäten und starten keinen Agentenlauf.** Sie gelten innerhalb tatsächlich laufender, beauftragter Agentensitzungen und dienen dazu, unnötige Nutzer-Nachfragen zu vermeiden.

- **Vor jedem Task:** `git fetch origin`, neue `agent-chat`-Nachrichten, an den eigenen Agenten gerichtete offene `agent-sync`-Bitten und aktive Sitzungen prüfen; eigene START-Meldung veröffentlichen. Konkrete Abhängigkeiten mit Empfänger und erwarteter Antwort benennen.
- **Wenn angesprochen:** Der adressierte Agent übernimmt die Verantwortung für die Abstimmung: Nachricht bei der nächsten aktiven Prüfung lesen, inhaltlich beantworten oder mit Begründung BLOCKIERT melden, erforderliche technische Übergabe koordinieren und Antwort mit Bezug zur Anfrage dokumentieren. Eine bloße Empfangsbestätigung schließt eine fachliche Anfrage nicht. Der Anfragende prüft die Antwort und schließt die Anfrage nachvollziehbar.
- **Nach jedem Task:** ENDE mit Ergebnissen, tatsächlichen Tests, offenen Abhängigkeiten und expliziten Übergaben an betroffene Agenten veröffentlichen. Empfänger prüfen diese bei ihrer nächsten aktiven Arbeitsgelegenheit.
- **Aktives Warten:** Nicht einfach die Agentensitzung beenden oder auf ein späteres „Weiter“ des Nutzers warten, wenn CI, ein Build, eine Agentenantwort oder das S25 noch aussteht. Laufende Prozesse im Rahmen der verfügbaren Ausführung prüfen; bei Blockaden den zuständigen Agenten ansprechen, Status und nächsten Prüfschritt festhalten und an einem anderen **bereits beauftragten, unabhängigen** Task weiterarbeiten. Nach Auflösung einer Blockade die ursprüngliche Aufgabe selbstständig fortsetzen, sofern die Sitzung noch aktiv ist.
- **Grenzen:** Keine Busy-Wait-Schleifen, kein unendliches Polling, keine künstlichen Statusmeldungen und keine neuen Tickets nur wegen dieser Regeln. Asynchrone Git-Nachrichten erzeugen **keine** automatische Push-Benachrichtigung oder Hintergrundausführung; erneute Prüfungen erfolgen bei Task-Grenzen und sinnvollen Zwischenpunkten tatsächlich laufender Agenten. Ist eine echte Sicherheitsfreigabe oder Nutzerentscheidung nötig und keine unabhängige Arbeit mehr vorhanden, sauber BLOCKIERT dokumentieren und gezielt eskalieren. Nie Freigabegrenzen, Geräte-Exklusivität oder CI-Qualitätsgates umgehen.

## Verbindliches Agenten-Aktivitätslog (ab 20.11.2026)

**Jeder tatsächlich arbeitende Agent registriert seine Sitzung** unter [docs/collaboration/agent-activity/README.md](docs/collaboration/agent-activity/README.md) und führt eine eigene Datei unter `docs/collaboration/agent-activity/sessions/`. Vor der ersten Codeänderung aktive Einträge, offene Issue-Übernahmen und `docs/collaboration/agent-sync/INDEX.md` prüfen. Im Eintrag stehen: Agent/Umgebung, Rolle, aktueller Auftrag, **jetzt bearbeitete Tickets**, **zeitnah geplante Tickets** (ohne exklusive Reservierung), betroffene Dateien, Branch, **Abstimmungsbedarf**, Fortschritt und Nachweise. Bei wesentlichem Wechsel aktualisieren; am Ende auf abgeschlossen/pausiert/blockiert setzen. Keine gemeinsamen Logdateien bei jedem Schritt umschreiben.

**Rollen gelten weiterhin wie unten beschrieben; konkrete aktuelle Übernahmen und das Log haben Vorrang vor historischen Annahmen.** Codex und Claude Code in derselben IDE nicht als gleichzeitig aktiv zählen, wenn nur einer läuft. Überschneidungen per `agent-sync` abstimmen. Nach Registrierung **möglichst lange selbstständig weiterarbeiten**, keine Rückfragen für triviale technische Entscheidungen; nur echte Freigaben, Sicherheitsrisiken oder unauflösbare Konflikte eskalieren. Keine Geheimnisse oder privaten Gerätedaten loggen.

## Verbindliche S10-/S25-Gerätesperre (ab 21.11.2026; S10 ergänzt 23.11.2026)

**Je physischem Gerät darf immer nur ein Agent gleichzeitig arbeiten.** Vor **jedem** Zugriff über ADB, UI-Automation, Screenshots, App-Installation, Logcat oder BLE-Diagnose die **hostweit gemeinsame** Sperre für das richtige Gerät prüfen. Verbindliche Schnittstelle: `tools/s25_lease.py` (historischer CLI-Dateiname). S10 und S25 haben getrennte kanonische Locks:

- S10: `~/.cache/pulselibre/s10-lease/{lock,status.json}`; verwenden `--device s10`.
- S25: `~/.cache/pulselibre/s25-lease/{lock,status.json}`; `--device s25` oder die kompatible alte CLI ohne `--device`.

Die Statusdatei enthält Agent, Host, PID, Gerät, Start und **voraussichtliche Freigabezeit** sowie optionale Task-/Session-/Test-/Phasenmetadaten. Sie wird beim Ende entfernt; die Lockdatei bleibt als Sperranker bestehen. Keine Seriennummern, Tokens oder privaten Kennungen als Metadaten verwenden. Details und Beispiele: [DEVICE_LEASE.md](docs/collaboration/DEVICE_LEASE.md).

- **Vor Nutzung:** `python3 tools/s25_lease.py status --device s10` bzw. `--device s25`; die alte Form `status` prüft das S25. Bei `BELEGT` **nicht** auf das jeweilige Gerät zugreifen; Besitzer kontaktieren oder offline/auf Emulator arbeiten. Die ETA ist eine Schätzung, **kein automatischer Ablauf** der Sperre.
- **Reservieren und benutzen:** Gesamte Gerätearbeit einschließlich Einrichtung und Cleanup in **einem** gehaltenen Wrapper ausführen, z. B. `python3 tools/s25_lease.py run --device s10 --agent Codex --minutes 40 -- bash -lc 'adb -s "$ANDROID_SERIAL" shell getprop ro.product.model'`. Der Wrapper reserviert atomar per `flock`, hält den Lock auch über ETA hinaus und beendet beim Signal die gestartete Prozessgruppe, bevor er freigibt. Bei längerer Arbeit die gesamte Sitzung halten, nicht ADB-Aufrufe einzeln ungeschützt starten.
- **Danach:** Nach Wrapper-Ende erneut den passenden Gerätestatus prüfen: `FREI`. Nie einen belegten Lock manuell löschen/übernehmen oder wegen einer überschrittenen ETA präemptieren. Bei unklarem Eigentümer/Teststatus pausieren und abstimmen.
- **Grenze:** `flock` schützt nur kooperierende Prozesse auf **demselben Host und Dateisystem**. Worktrees desselben Host-Benutzers teilen standardmäßig die Pfade. Bei verschiedenen Hosts ist zusätzliche koordinierte Gerätevergabe erforderlich; Git-Dateien sind wegen Push-Verzögerung kein atomarer Lock. Direkte ADB-Aufrufe außerhalb des Wrappers werden nicht technisch abgefangen; Testskripte müssen im Wrapper gestartet werden.
- Diese Sperre ist **zusätzlich** zu den untenstehenden Sicherheitsregeln; insbesondere bleiben Instrumentationstests auf dem S25 verboten.

## Aktuelle Zusammenarbeit und Namenskonvention (19.11.2026)

- **Haupt-IDE: VSCodium („Codium“).** „Codium“, „VSCodium“, „Claude Code“ und „Codex“ bezeichnen in der projektinternen Aufgabenkoordination **denselben IDE-Arbeitsplatz**, nicht vier unabhängig parallel arbeitende Entwickler. Claude Code und OpenAI Codex sind alternative API-/Agenten-Zugänge in VSCodium; **es ist jeweils nur einer davon aktiv**. Die Bezeichnungen sind als Kurzform für diesen Arbeitsplatz austauschbar, soweit nicht ein konkreter Lauf oder dessen Modell technisch unterschieden werden muss.
- **IDE-Arbeitsbereich:** Dashboard/UI, wissenschaftliche Analysen und die jeweils ausdrücklich in Issues/PRs übernommenen Aufgaben. Die frühere Formulierung „Claude Code als Nachfolger von Codex“ bezeichnet einen Wechsel des verwendeten API-Zugangs, **keinen Wechsel der verantwortlichen IDE oder eine zweite parallele Entwicklerinstanz**.
- **ChatGPT-Arbeitsbereich:** CIRQA-Protokoll, Historien-Download und zugewiesene Offline-Tests; aktuelle Nachweise stehen in `docs/protocol/research/CIRQA_HISTORICAL_RESEARCH_STATUS_2026-11-18.md`. Der begrenzte V24/V25-Abruf von 33 Dateien (31 FIT, 2 Nicht-FIT) ist bestätigt; **vollständige historische Abdeckung bleibt offen**. Kein unbeauftragter aktiver BLE-Geräteversuch.
- **Andere ausdrücklich beauftragte Werkzeuge/Agenten** (z. B. OpenCode) sind separate Mitwirkende nur für ihre konkret übernommenen Aufgaben. Ihre Existenz impliziert nicht, dass Codex und Claude Code gleichzeitig laufen.
- **Gemeinsame Dateien** (`AGENTS.md`, `README.md`, `docs/project/PROJECT_STATUS.md`, `docs/project/CHAT_START_HERE.md`, CI/Gradle, Datenbankschema): vor Änderungen koordinieren, getrennte Branches/Worktrees verwenden und Konflikte nicht blind überschreiben. Die Zuordnung ist aufgabenbezogen, keine dauerhafte technische Zugriffssperre; bei geänderter Aufgabenverteilung Issue/PR und Übergabe aktualisieren.
- Ein Agent startet nicht durch diese Dokumentation automatisch. Nur ein tatsächlich erteilter Auftrag belegt aktive Arbeit.

## Abstimmung zwischen Agenten über das Repository (verbindlich ab 2026-11-19)

- **Agenten stimmen sich über `docs/collaboration/agent-sync/` ab, nicht über Chat-Texte, die der Nutzer weiterreicht.** Vor jeder Arbeit `INDEX.md` und alle offenen Nachrichten lesen; Nachrichten als neue Datei `YYYY-MM-DD_HHMM_<von>-an-<an>_<thema>.md` schreiben, in `INDEX.md` eintragen, Status pflegen. Format und Regeln: `docs/collaboration/agent-sync/README.md`.
- **Aktuelle Zuständigkeiten (2026-11-19):** Claude Code – Android-GUI, Icons, Dashboard, Navigation, UI-Tests, UI-Status. OpenCode – BLE/GFDI/FIT/Protokoll, Datenbank/Backfill, Gerätemetadaten-Decoder. Fremde Bereiche nur nach vorheriger Nachricht ändern.
- **Ein Bearbeiter pro Problem:** Wer eine rote `main`, einen Dependabot-Sprung oder eine Querschnittsänderung angeht, kündigt das vorher per Nachricht an („ich übernehme X“); andere Agenten arbeiten dann nicht parallel am selben Fix.
- **Merge nur nach grünem, geprüftem vollem Android-CI-Lauf (inkl. Emulator)** auf dem Branch-Head und anschließendem vollem Lauf auf `main`. Das Ergebnis des Laufs wird gelesen, bevor gemergt wird; ein Merge-Befehl wird nie an das bloße Ende eines Laufs gekoppelt.
- **Instrumentationstests laufen nie auf dem S25** (sie überschreiben/löschen Profil- und Einstellungswerte); nur Emulator/GitHub-CI, bei mehreren Geräten `ANDROID_SERIAL` setzen.
- **Dependabot-Sprünge** (AGP, Gradle, Kotlin, compileSdk, AndroidX-Majors) nie ohne eigenen vollen Lauf mergen; sie gehören in ein eigenes Migrationsticket (z. B. #155).

## Einstieg vor jeder Arbeit

1. `git status`, aktuellen Branch, `git fetch origin` und neuesten `origin/main` prüfen; lokale Änderungen nicht überschreiben.
2. `README.md`, `docs/project/PROJECT_STATUS.md`, `docs/project/CHAT_START_HERE.md`, `docs/project/REQUIREMENTS_AND_PRINCIPLES.md`, `docs/quality/QUALITY_GATE.md` lesen.
3. Für BLE-/History-Arbeit zusätzlich `docs/archive/2026-11-24/protocol/sync/CIRQA_HISTORY_SYNC_PLAN.md`, `docs/protocol/device-tests/CIRQA_5050_THIRD_TEST_AND_PHASE_FIX_2026-11-17.md`, `docs/protocol/CIRQA_PROTOCOL_SPEC.md` und `docs/project/compliance/LICENSE_AGPL_AUDIT_2026-11-16.md` lesen.
4. Offene GitHub-Issues, Pull Requests, neueste Commits und Actions-Läufe prüfen; Änderungen anderer Agenten beachten.

## Gemeinsamer Git-Workflow

- **Mehrere Agenten arbeiten niemals ungeprüft am selben Checkout oder denselben Dateien.** Für parallele Arbeit getrennte lokale Worktrees/Branches verwenden; vor Änderungen auf aktuellem `main` aufbauen.
- **Nach erfolgreichem CI-Build und positiven Tests**: Änderungen nach erfolgreichem Review/Test selbständig über PR nach `main` mergen, um Branch-Fragmentierung zu vermeiden. `docs/project/PROJECT_STATUS.md` bei Statusänderungen aktualisieren.
- Vor jedem Push prüfen, ob ein anderer Agent dieselbe Datei inzwischen geändert hat; niemals Force-Push.
- Konflikte nicht blind auflösen. Nicht in fremden Worktrees, nicht in fremden uncommitted Änderungen arbeiten.
- Issue-/PR-Verweise und Evidenz in Commits/PRs dokumentieren. `docs/project/PROJECT_STATUS.md` bei tatsächlichen Statusänderungen aktualisieren; keine redundanten Statuskopien schaffen.
- Nur nachgewiesene Erfolge behaupten: `implementiert`, `CI grün`, `Full-CI grün`, `S25 getestet`, `Release freigegeben` sind verschiedene Aussagen.

## Pragmatische Freigaberegel für Agenten (aktualisiert 19.11.2026)

**Keine wiederholten Einmal-Gates für risikoarme Arbeiten.** Der Nutzer hat die bisherige Gate-Praxis ausdrücklich gelockert, damit Codex/Claude Code/OpenCode selbstständig arbeiten können. Die folgenden Tätigkeiten sind im Rahmen eines zugewiesenen Tickets ohne zusätzliche Einzelrückfrage erlaubt:

- Offline-Auswertung bereits vorhandener, rechtmäßig gespeicherter FIT-/SQLite-/Diagnosedaten; rein lesende Decoder-, Inventur- und Reanalyse-Läufe.
- Passive Auswertung von Daten, die über den **bereits bestehenden** CIRQA-Empfangspfad eintreffen; kein zusätzlicher Scan, keine neuen BLE-Kommandos und keine Änderung des Transportverhaltens.
- Emulator-Tests, CI, Screenshots und nichtdestruktive **lesende** Diagnose-/UI-Prüfungen auf einem bereits angeschlossenen, freigegebenen Testgerät. **Keine Instrumentationstests auf dem S25**, da sie Nutzerprofil und Einstellungen verändern können.
- Code, Tests, Dokumentation, lokale Debug-Ansichten und datensparsame Diagnoseberichte erstellen; Ergebnisse und Grenzen nachträglich im Issue dokumentieren, statt pro Teilschritt auf eine Freigabe zu warten.

**Eine ausdrückliche Einzelentscheidung bleibt erforderlich** für neue/experimentelle aktive proprietäre BLE-/GATT-/FileSync-Befehle oder -Sequenzen, schreibende Eingriffe am Band, Reset, Entkoppeln, Firmwareaktionen, Löschung/Überschreibung realer Nutzerdaten, riskante Dauerlast sowie Weitergabe nicht anonymisierter Gesundheits- oder Geräte-Rohdaten an Dritte. Eine explizite Freigabe für einen konkret definierten Test gilt innerhalb ihres vereinbarten Umfangs; nicht bei jedem identischen Unterbefehl erneut fragen. **Bestehende Sicherheits-Stop-Bedingungen bleiben gültig.**

Diese Regel ersetzt widersprechende pauschale Einmal-Gate-Formulierungen für *passive, offline oder rein lesende* Arbeiten in älteren Notizen; historische Protokollversuche sind keine generelle Autorisierung für neue aktive Gerätebefehle. Qualitätssicherung und nachweisbare CI-/Geräteabnahme werden dadurch nicht aufgehoben. Siehe #204.

## Aktuelle Freigabe: Armband-Rohdaten (21.11.2026)

**Alle beauftragten Agenten dürfen und sollen die derzeitigen CIRQA-Armband-Rohdaten für Entwicklung und Tests lokal sowie agentenintern verwenden.** Der Projektverantwortliche erklärt diese aktuellen Testdaten als keiner Person zuordenbar. Maßgebliche Details, Grenzen für öffentliche Veröffentlichung und Umschaltung bei späteren Echtdaten: [Temporäre Rohdatenregel](docs/collaboration/CURRENT_BAND_RAW_DATA_POLICY.md). Ältere pauschale Rohdatenverbote gelten für diese **interne Nutzung** nicht mehr. Geräte-Sicherheitsfreigaben und S25-Sperre bleiben unberührt.

## Architektur, Datenschutz und Gerätesicherheit

- Bestehende Kotlin/Android-Architektur erhalten. BLE-Transport, Protokoll-Decoder, lokale Speicherung, Domainlogik und UI möglichst getrennt halten; vor Refactoring Code und Tests lesen.
- Offline-first; keine INTERNET-Berechtigung, keine Telemetrie, keine erfundenen Gesundheitswerte. Keine Roh-BLE-Payloads, Geräteadressen, medizinischen Bilder, personenbezogenen Gesundheitsdaten, Keystores oder Secrets committen.
- CIRQA ist mit Garmin Connect gekoppelt. Keine automatische Entkopplung, kein Reset, kein `CLOSE_ALL`, keine destruktiven FileSync-Kommandos und keine neuen experimentellen BLE-Schreiboperationen ohne gesonderte Nutzerfreigabe und dokumentierte Stop-Gates.
- Aktueller CIRQA-Befund (18.11.2026): Bootstrap→5030→5007→5031→5002 wurde am Gerät bestätigt; über BLE erschien nur ein 32-Byte-Platzhalterverzeichnis. Der erste begrenzte RFCOMM-V1-Versuch erhielt keine Gegenverbindung. **Keine vollständige FIT-Synchronisation nachgewiesen.** Verbindliche Evidenz und Freigabegrenzen: `docs/protocol/research/CIRQA_HISTORICAL_RESEARCH_STATUS_2026-11-18.md`. Ältere 5050-only-Befunde sind historische Zwischenstände.
- Externe Protokollimplementierungen als Recherchequellen kennzeichnen; Lizenz/Provenienz vor Codeübernahme prüfen. Wissenschaftliche Methoden: `docs/science/ENGINEERING_SCIENTIFIC_STANDARDS.md`.

## Tests und Freigaben

- Android-App `de.pulselibre`; JDK 21 im CI, Gradle 9.6.0, AGP 9.5.1 (eingebautes Kotlin, Kotlin 2.4.21 per buildscript-Classpath), Kotlin/JVM-Bytecode-Ziel 17, compileSdk 37 (targetSdk 35). Gradle-Wrapper laut README nicht vorhanden.
- Schnellprüfung: `gradle :app:testDebugUnitTest :app:assembleDebug`; Python-HCI-Tests: `python3 -m unittest discover -s tools -p 'test_*.py' -v`. Bevorzugt ohne GitHub: `./tools/ci-local.sh --quick` (gleiche Phasen wie die CI, inkl. R8-Release und APK-Diagnose).
- Vollprüfung: `./tools/ci-local.sh --full` bzw. `gradle :app:lintDebug :app:assembleDebugAndroidTest` plus API-35-Emulator. Seit 20.11.2026 ist die Android-CI **hybrid**: `.github/workflows/android.yml` startet automatisch bei Pull Requests und Pushes auf `main` mit vollständiger Prüfung, wobei der Ausführungsort über `PULSELIBRE_LOCAL_RUNNER_ENABLED` gesteuert wird (Push → LOCAL, PR → CLOUD, solange nicht ausdrücklich freigegeben). Manuelle Starts wählen **Ausführungsort** (LOCAL/CLOUD) und **Testumfang** (QUICK/FULL) unabhängig. Details: `docs/quality/ci/HYBRID_CI.md`. Auf Pull Requests werden keine stabilen APK-Signierschlüssel geladen. Ein grüner QUICK-Lauf ist **keine** vollständige Installationsfreigabe.
- Signierte Update-APK nur bei korrekt konfigurierten Secrets und nach überprüfter Zertifikat-/Versionskontinuität; keine Secrets anzeigen. Hardwaretests ausschließlich nach expliziter Zustimmung und mit klaren Stop-Kriterien.

## Wo Entscheidungen stehen

- Aktueller Status und Übergabe: `docs/project/PROJECT_STATUS.md`
- Einstieg für neue Agenten: `docs/project/CHAT_START_HERE.md`
- Anforderungen und Architektur: `docs/project/REQUIREMENTS_AND_PRINCIPLES.md`, `docs/archive/2026-11-24/ARCHITECTURE_AND_CODE_REVIEW.md` (historische Befunde)
- CIRQA-Historie: `docs/protocol/research/CIRQA_HISTORICAL_RESEARCH_STATUS_2026-11-18.md`, `docs/archive/2026-11-24/protocol/sync/CIRQA_HISTORY_SYNC_PLAN.md`
- Release-/Signing-Regeln: `docs/product/release/ANDROID_RELEASE_READINESS.md`
- Audit und offene Arbeit: `docs/archive/2026-11-24/AUDIT_2026-11-16.md`, GitHub-Issues #2–#6
- Autonomer Auftrag (nur bei tatsächlich eingerichteter Automation): `docs/project/AUTONOMOUS_DEVELOPMENT_AGENT.md`

Bei Widersprüchen: **aktueller Code/Workflow + belegte Tests > aktueller Projektstatus > historische Dokumente**. Widerspruch offen kennzeichnen statt stillschweigend Annahmen zu treffen.
