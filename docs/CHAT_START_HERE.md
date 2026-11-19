# PulsLibre – CHAT_START_HERE

> **Historischer CIRQA-Download – aktueller Einstieg (18.11.2026):** Vor allen 5050-/FileSync-/Directory-/FIT-Arbeiten [den kanonischen Forschungsstand](protocol/research/CIRQA_HISTORICAL_RESEARCH_STATUS_2026-11-18.md) und die dort verlinkten datierten Geräteberichte lesen. Der BLE-Handshake bis 5002 wurde am Gerät erfolgreich nachgewiesen; der BLE-5004-Body blieb beim 32-Byte-Platzhalter, RFCOMM ist der nächste *unbestätigte* Transportkandidat. Die älteren „nächsten Schritte“ weiter unten sind historische Momentaufnahmen. Keine unbelegten Hardware- oder Download-Erfolge behaupten.

**Letzte redaktionelle Prüfung: 2026-11-17 (Codex-Übergabe).** **GitHub-Repository:** `jochenkunkel/pulselibre` (privat). **Verbindlicher Zweig:** `main`.

> Diese Datei ist der **Copy-&-Paste-Einstiegspunkt für neue Chats und Entwicklungsagenten**. Sie bewahrt den im Repository dokumentierten Projektkontext, ersetzt aber weder den tatsächlichen aktuellen Git-Stand noch automatisch synchronisierte Chatverläufe. Vor jeder Änderung den aktuellen `main`-Stand und die Issues erneut lesen.

> **Abstimmung zwischen Agenten (ab 19.11.2026):** Vor jeder Arbeit [docs/collaboration/agent-sync/INDEX.md](collaboration/agent-sync/INDEX.md) lesen; Absprachen laufen über dieses Verzeichnis, nicht über weitergereichte Chat-Texte (Regeln: [README](collaboration/agent-sync/README.md), [AGENTS.md](../AGENTS.md)).

**Codex-/VSCodium-Einstieg:** Zuerst [AGENTS.md](../AGENTS.md) lesen. Der aktuelle CIRQA-5050-Befund steht in [PROJECT_STATUS.md](PROJECT_STATUS.md), Abschnitt „Aktueller CIRQA- und Teamstatus (2026-11-17)“. Die nachstehenden älteren Commits, Issue-Prioritäten und Beispiele sind historische Einstiegshilfen und dürfen nicht als aktueller Git-Head interpretiert werden. Für ausdrücklich zur Prüfung bestellte Änderungen Branch + PR statt Direkt-Commit nach main verwenden.

## Aktueller Willkommensauftrag für Codex in VSCodium (2026-11-17)

> Du bist **Codex**, zusätzlicher Coding-Agent für PulsLibre. Arbeite im GitHub-Repository `jochenkunkel/pulselibre` auf Grundlage des aktuellen `main`. Lies zuerst `AGENTS.md`, `docs/PROJECT_STATUS.md`, `docs/ui/APPROVED_UI_CONCEPT_2026-11-15.md`, `docs/DASHBOARD_IMPLEMENTATION.md`, `docs/ENGINEERING_SCIENTIFIC_STANDARDS.md`, `docs/protocol/data-model/CIRQA_DASHBOARD_GROUND_RULES.md`, `docs/UNIFIED_LOCAL_STORAGE_2026-11-16.md` und die relevanten aktuellen UI-/Datenbankquellen. **Deine Zuständigkeit ist Dashboard-Gestaltung und wissenschaftliche Analysen.** Setze das genehmigte Vier-Screens-Konzept schrittweise um; prüfe vorhandene Datenquellen und verbessere Darstellung, Navigation, Zeitreihen, Leerzustände, Barrierefreiheit und Tests. Wissenschaftliche Verfahren nur mit überprüfbaren Quellen, Einheiten, Grenzen, Fehlwertregeln und Regressionstests implementieren. Keine klinisch validierten Ergebnisse, Schlafphasen, SpO₂, Kalorien oder Rohbewegungsdaten behaupten, die nicht nachgewiesen sind. Keine Demo-Werte als echte Messwerte anzeigen. Die bestehende kanonische SQLite-Datenbank, Datenherkunft, Export und Offline-first-Sicherheit erhalten. **Finger weg von Garmin-BLE/GFDI/5050/FileSync/FIT-Historien-Transport**: Dieser Bereich wird parallel vom ChatGPT-Agenten bearbeitet. Arbeite in einem eigenen Branch/Worktree, sichere Tests/CI-Evidenz, bereite einen überprüfbaren PR vor und dokumentiere Fortschritt im GitHub-Repository. Änderungen an gemeinsam genutzten Status-/Build-/Speicherdateien vorher koordinieren. Melde auf Deutsch konkrete Commits, Tests und offene Fragen.

**Zuständigkeit des ChatGPT-Agenten:** CIRQA-Historien-Download (Protokollrecherche, 5050-Capabilities, sichere Offline-Implementierung, begrenzte Gerätetests). Keine parallelen Dashboard-/Wissenschaftsänderungen. Maßgebliche Übergabe: `docs/PROJECT_STATUS.md` und `docs/protocol/sync/CIRQA_HISTORY_SYNC_PLAN.md`.

## 1. Auftrag an den neuen Chat / Entwicklungsagenten

Du arbeitest am Projekt **PulsLibre** (in älteren Gesprächen teilweise „PulseLibre“ genannt). Ziel ist eine zuverlässige, datenschutzfreundliche, offline-first Android-App, die **echte und nachweisbar korrekt zugeordnete** Garmin-CIRQA-Messwerte auf dem Samsung Galaxy S25 nutzbar macht.

**Arbeitsweise des Nutzers:**
- Deutsch antworten, klar und pragmatisch; möglichst selbstständig konkrete Änderungen implementieren statt nur Pläne schreiben.
- Änderungen normalerweise gemäß dokumentierter Nutzerpräferenz in kleinen nachvollziehbaren Commits bearbeiten; bei ausdrücklich gewünschter Prüfung zunächst Branch + Pull Request verwenden (siehe `AGENTS.md`). Vor riskanten Änderungen Daten- und Gerätesicherheit beachten.
- GitHub als gemeinsame Quelle für andere Chats verwenden. Jede wichtige neue Erkenntnis, Entscheidung, Codeänderung und offene Frage in GitHub festhalten.
- Keine Ergebnisse von Tests, Hardwareversuchen, CI, APK-Updates oder Datensynchronisation behaupten, die nicht überprüft wurden.
- **Keine Secrets, Signaturschlüssel, Gesundheitsdaten, persönlichen BLE-Adressen oder Rohdiagnosen in Git committen.**
- Wenn ein Auftrag „stündlich autonom“ erwähnt wird: Das geschieht **nicht allein durch eine Chat-Anweisung**. Nur eine tatsächlich eingerichtete und überprüfte Automation kann wiederkehrende Läufe auslösen.

## 2. Pflichtlektüre vor Beginn

1. `docs/PROJECT_STATUS.md` – zentraler aktueller Projektstatus und Übergaberegeln.
2. `README.md` – Funktionsumfang, Build, Sicherheit und Einstieg.
3. `docs/AUDIT_2026-11-16.md` – priorisierte statische Teilprüfung; **kein** vollständiger Audit.
4. `docs/ROADMAP_TO_CIRQA_DATA.md` – Zielbild und Reihenfolge.
5. `docs/ANDROID_RELEASE_READINESS.md` – APK-Update, Signing, Geräte-/Store-Tests.
6. `docs/QUALITY_GATE.md` – erforderliche und tatsächlich konfigurierte CI-Gates.
7. `docs/ARCHITECTURE_AND_CODE_REVIEW.md` – **historischer** Review, Befunde nicht ungeprüft übernehmen.
8. GitHub-Issues **#2–#6**, `.github/workflows/android.yml` und die neuesten Commits/CI-Läufe.

**Quellenpriorität:** aktueller Code auf `main` → tatsächlich nachgewiesene Tests/CI/Hardwarebefunde → aktuelle Issues und `PROJECT_STATUS.md` → ältere Dokumente/Chat-Aussagen. Bei Konflikten nachprüfen und dokumentieren.

## 3. Technischer Kontext und nachgewiesene Grenzen

- Android-Anwendung: `applicationId = de.pulselibre`; `minSdk=26`, `targetSdk=35`, `compileSdk=35`; Kotlin/Gradle, Java 17.
- Zielgerät Samsung Galaxy S25; passiver BLE-Scan und Berechtigungsdialog wurden laut bisherigem Projektstand praktisch getestet. Das beweist **keine** vollständige CIRQA-Kompatibilität.
- Im Code existieren passive BLE-Diagnose, aktive nutzerinitiierte GATT-Dienstsuche, Garmin-ML-/GFDI-Handshake-Probes, experimentelle Echtzeitmessungen und ein Nacht-/Vordergrunddienst. **Passive Scans sind nicht mit aktiven ML-Probes gleichzusetzen**: Letztere können Notifications abonnieren und GATT-Schreibbefehle senden.
- Lokale Messwert-/Dashboard-/Exportfunktionen sind vorhanden. **Kein unabhängig bestätigter, produktionsreifer CIRQA-Gesundheitsdatensync.** Insbesondere Schlaf, SpO₂, Puls und Schritte nicht ohne überprüfte Quelle als erfolgreich ausgelesen deklarieren.
- Offline-first, keine INTERNET-Berechtigung laut dokumentiertem Design; Datenschutz und Exportpfade trotzdem codebezogen prüfen.
- Android-App-Updates benötigen **gleiche kompatible Signatur, identische applicationId und höheren versionCode**. Lokaler Standard in `app/build.gradle.kts`: `versionCode=2`, `versionName=2.0.0_build2`; CI setzt versionCode über `github.run_number`.
- Stabile CI-Debug-Signierung erfordert vier GitHub-Secrets: `PULSELIBRE_SIGNING_KEY_B64`, `PULSELIBRE_SIGNING_STORE_PASSWORD`, `PULSELIBRE_SIGNING_KEY_ALIAS`, `PULSELIBRE_SIGNING_KEY_PASSWORD`. **Ob sie eingerichtet sind, ist nicht bestätigt.** Niemals ihre Werte ausgeben oder ins Repo schreiben.
- `.github/workflows/android.yml`: `main`-Pushes bauen Debug und führen JVM-Tests aus; Lint und API-35-Emulator laufen laut aktueller Workflow-Konfiguration **nicht bei gewöhnlichen Pull Requests**, sondern nur bei `v*`-Tags oder manuellem Dispatch mit `full_tests=true`. PRs und `main`-Pushes führen Build und JVM-Tests aus. **Erfolg aktueller Läufe nicht unterstellen.**
- Aktuell kein Gradle-Wrapper laut README. Lokaler Build, falls SDK/Gradle verfügbar: `gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest`. Ein Emulator-/Hardwaretest ist damit nicht ersetzt.

## 4. Was bereits auf `main` umgesetzt wurde (historische Commit-Referenzen)

- `584e477` und `96fc045`: erste Absicherung der Nachtaufzeichnung gegen doppelte Abschlüsse, falsche Timeouts und verspätete Callbacks; **noch nicht durch ausreichende Tests bestätigt**.
- `44a3558`: README auf experimentelle aktive GATT-/ML-Probes korrigiert.
- `c851a66`: statischer Teil-Audit unter `docs/AUDIT_2026-11-16.md`.
- `5708eb2`: Release-/Signing-Dokumentation korrigiert.
- `04aeebb`: tatsächliche CI-Abdeckung und Quality-Gates beschrieben.
- `cfe4d78`: Roadmap mit existierenden aktiven Probes abgeglichen.
- `8b61648`: historische Architekturreview entsprechend gekennzeichnet.
- `28452ab`: Diagnoseexport-Texte unterscheiden passive Scans von aktiven Probes.
- `af301a9`: `docs/PROJECT_STATUS.md` als gemeinsamer Übergabepunkt erstellt.
- `7553926`: README mit zentralem Status verlinkt.

**Wichtig:** Commits sind Referenzen zum bisherigen Stand, nicht die Behauptung, dass die genannten Probleme vollständig gelöst wurden.

## 5. Offene Tickets und Prioritäten

| Issue | Priorität | Thema | Nächster Nachweis |
| --- | --- | --- | --- |
| [#2](https://github.com/jochenkunkel/pulselibre/issues/2) | P0 | Aktive CIRQA-GATT-Kommunikation und Dokumentation | Code-/Sicherheitsprüfung und S25-Interoperabilität |
| [#3](https://github.com/jochenkunkel/pulselibre/issues/3) | P1 | Timeout, Callback, Nachtaufzeichnung | Deterministische Race-/Lifecycle-Tests |
| [#4](https://github.com/jochenkunkel/pulselibre/issues/4) | P1 | CI, Versionierung, Signierung und APK-Updates | Grüne Full-CI; Update mit Datenerhalt und stabiler Signatur |
| [#5](https://github.com/jochenkunkel/pulselibre/issues/5) | P1 | Historische Review-Befunde, Datenschutz, Exporte | Vollständige Datenfluss- und Regressionstests |
| [#6](https://github.com/jochenkunkel/pulselibre/issues/6) | P2 | Vollständiges Codeaudit und Testinventur | Dateiweises Prüfprotokoll mit Evidenz |

Issues nur bei erfüllten Abnahmekriterien schließen. Eine dokumentierte Teilkorrektur allein reicht nicht.

## 6. Nächste sinnvolle Arbeitssequenz

1. Neuesten `main`-Commit, Issues, Workflow-Konfiguration und verfügbare CI-Ergebnisse prüfen.
2. Falls möglich Full-CI (Lint, JVM, Emulator) ausführen, tatsächliche Fehler beheben und Ergebnisse festhalten.
3. #3 mit gezielten Tests für doppelte, verspätete, parallele und nach Stop eintreffende Callbacks absichern.
4. #2 und #5: aktive BLE-Probes, Nutzerfreigabe, Garmin-Connect-Interoperabilität, Cleanup und Exportdaten überprüfen.
5. #4: Signing-/Update-Prozess ohne Secret-Leak validieren; S25-Update inklusive Datenerhalt nur mit realem Gerät als bestanden markieren.
6. #6: vollständige Datei-/Testinventur aufbauen, Findings priorisieren und nachweisbar abarbeiten.

## 7. Ablauf nach jeder Arbeitseinheit

- Codeänderungen mit Pfad und Commit-SHA benennen.
- Tests dokumentieren als **bestanden / fehlgeschlagen / nicht ausgeführt**; Hardware-/CI-Ergebnisse separat.
- Betroffene Issues aktualisieren; neue Probleme als neue Issues anlegen.
- `docs/PROJECT_STATUS.md` aktualisieren, falls sich der Projektstand geändert hat.
- Bei Änderung dieser Einstiegsvorgaben auch **diese Datei** aktualisieren.
- In der Antwort an den Nutzer kurz berichten: **Was ist wirklich geändert? Was ist getestet? Was bleibt offen?**

## 8. Copy-&-Paste-Startauftrag für einen neuen Chat

> Du bist Entwicklungsagent für **PulsLibre**, privates GitHub-Repo `jochenkunkel/pulselibre`. Arbeite auf `main` und committe nach Nutzerwunsch direkt dorthin. **Lies zuerst** `docs/CHAT_START_HERE.md`, `docs/PROJECT_STATUS.md`, `README.md`, `docs/AUDIT_2026-11-16.md`, die Issues #2–#6 und den neuesten CI-/Git-Stand. Verwende GitHub als gemeinsame Wahrheit für alle Chats; Chatverläufe sind nicht automatisch synchronisiert. Prüfe, welche Angaben noch aktuell sind. Bearbeite anschließend die höchstpriorisierte sichere Aufgabe selbstständig, implementiere und teste soweit möglich, dokumentiere Ergebnisse und offene Risiken in den Issues und aktualisiere `docs/PROJECT_STATUS.md`. Behaupte keine unbestätigten CIRQA-Messwerte, bestandenen Tests oder funktionierenden APK-Updates. Keine Secrets oder privaten Gerätedaten committen. Antworte auf Deutsch mit konkreten Commits und ehrlichem Teststatus.

---

**Grenze dieses Einstiegspunkts:** Er enthält die derzeit dokumentierten wesentlichen Entscheidungen, Arbeitsregeln, Implementierungen und offenen Punkte; nicht jedes Detail aus allen vergangenen Chatnachrichten ist technisch zugänglich. Neues Wissen muss explizit in GitHub übernommen werden.
