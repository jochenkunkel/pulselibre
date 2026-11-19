# PulsLibre – Arbeitsanweisungen für die Haupt-IDE und weitere Entwickler

Diese Datei gilt repositoryweit. **GitHub ist die verbindliche, versionierte Wissensbasis**, nicht eine Chat-Unterhaltung oder eine lokale IDE-Sitzung. Für den aktuellen Projektstand zuerst `docs/PROJECT_STATUS.md` lesen; für den vollständigen Einstieg `docs/CHAT_START_HERE.md`.

## Aktuelle Zusammenarbeit und Namenskonvention (19.11.2026)

- **Haupt-IDE: VSCodium („Codium“).** „Codium“, „VSCodium“, „Claude Code“ und „Codex“ bezeichnen in der projektinternen Aufgabenkoordination **denselben IDE-Arbeitsplatz**, nicht vier unabhängig parallel arbeitende Entwickler. Claude Code und OpenAI Codex sind alternative API-/Agenten-Zugänge in VSCodium; **es ist jeweils nur einer davon aktiv**. Die Bezeichnungen sind als Kurzform für diesen Arbeitsplatz austauschbar, soweit nicht ein konkreter Lauf oder dessen Modell technisch unterschieden werden muss.
- **IDE-Arbeitsbereich:** Dashboard/UI, wissenschaftliche Analysen und die jeweils ausdrücklich in Issues/PRs übernommenen Aufgaben. Die frühere Formulierung „Claude Code als Nachfolger von Codex“ bezeichnet einen Wechsel des verwendeten API-Zugangs, **keinen Wechsel der verantwortlichen IDE oder eine zweite parallele Entwicklerinstanz**.
- **ChatGPT-Arbeitsbereich:** CIRQA-Protokoll, Historien-Download und zugewiesene Offline-Tests; aktuelle Nachweise stehen in `docs/protocol/research/CIRQA_HISTORICAL_RESEARCH_STATUS_2026-11-18.md`. Der begrenzte V24/V25-Abruf von 33 Dateien (31 FIT, 2 Nicht-FIT) ist bestätigt; **vollständige historische Abdeckung bleibt offen**. Kein unbeauftragter aktiver BLE-Geräteversuch.
- **Andere ausdrücklich beauftragte Werkzeuge/Agenten** (z. B. OpenCode) sind separate Mitwirkende nur für ihre konkret übernommenen Aufgaben. Ihre Existenz impliziert nicht, dass Codex und Claude Code gleichzeitig laufen.
- **Gemeinsame Dateien** (`AGENTS.md`, `README.md`, `docs/PROJECT_STATUS.md`, `docs/CHAT_START_HERE.md`, CI/Gradle, Datenbankschema): vor Änderungen koordinieren, getrennte Branches/Worktrees verwenden und Konflikte nicht blind überschreiben. Die Zuordnung ist aufgabenbezogen, keine dauerhafte technische Zugriffssperre; bei geänderter Aufgabenverteilung Issue/PR und Übergabe aktualisieren.
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
2. `README.md`, `docs/PROJECT_STATUS.md`, `docs/CHAT_START_HERE.md`, `docs/REQUIREMENTS_AND_PRINCIPLES.md`, `docs/QUALITY_GATE.md` lesen.
3. Für BLE-/History-Arbeit zusätzlich `docs/protocol/sync/CIRQA_HISTORY_SYNC_PLAN.md`, `docs/protocol/device-tests/CIRQA_5050_THIRD_TEST_AND_PHASE_FIX_2026-11-17.md`, `docs/protocol/CIRQA_PROTOCOL_SPEC.md` und `docs/LICENSE_AGPL_AUDIT_2026-11-16.md` lesen.
4. Offene GitHub-Issues, Pull Requests, neueste Commits und Actions-Läufe prüfen; Änderungen anderer Agenten beachten.

## Gemeinsamer Git-Workflow

- **Mehrere Agenten arbeiten niemals ungeprüft am selben Checkout oder denselben Dateien.** Für parallele Arbeit getrennte lokale Worktrees/Branches verwenden; vor Änderungen auf aktuellem `main` aufbauen.
- **Nach erfolgreichem CI-Build und positiven Tests**: Änderungen nach erfolgreichem Review/Test selbständig über PR nach `main` mergen, um Branch-Fragmentierung zu vermeiden. `docs/PROJECT_STATUS.md` bei Statusänderungen aktualisieren.
- Vor jedem Push prüfen, ob ein anderer Agent dieselbe Datei inzwischen geändert hat; niemals Force-Push.
- Konflikte nicht blind auflösen. Nicht in fremden Worktrees, nicht in fremden uncommitted Änderungen arbeiten.
- Issue-/PR-Verweise und Evidenz in Commits/PRs dokumentieren. `docs/PROJECT_STATUS.md` bei tatsächlichen Statusänderungen aktualisieren; keine redundanten Statuskopien schaffen.
- Nur nachgewiesene Erfolge behaupten: `implementiert`, `CI grün`, `Full-CI grün`, `S25 getestet`, `Release freigegeben` sind verschiedene Aussagen.

## Architektur, Datenschutz und Gerätesicherheit

- Bestehende Kotlin/Android-Architektur erhalten. BLE-Transport, Protokoll-Decoder, lokale Speicherung, Domainlogik und UI möglichst getrennt halten; vor Refactoring Code und Tests lesen.
- Offline-first; keine INTERNET-Berechtigung, keine Telemetrie, keine erfundenen Gesundheitswerte. Keine Roh-BLE-Payloads, Geräteadressen, medizinischen Bilder, personenbezogenen Gesundheitsdaten, Keystores oder Secrets committen.
- CIRQA ist mit Garmin Connect gekoppelt. Keine automatische Entkopplung, kein Reset, kein `CLOSE_ALL`, keine destruktiven FileSync-Kommandos und keine neuen experimentellen BLE-Schreiboperationen ohne gesonderte Nutzerfreigabe und dokumentierte Stop-Gates.
- Aktueller CIRQA-Befund (18.11.2026): Bootstrap→5030→5007→5031→5002 wurde am Gerät bestätigt; über BLE erschien nur ein 32-Byte-Platzhalterverzeichnis. Der erste begrenzte RFCOMM-V1-Versuch erhielt keine Gegenverbindung. **Keine vollständige FIT-Synchronisation nachgewiesen.** Verbindliche Evidenz und Freigabegrenzen: `docs/protocol/research/CIRQA_HISTORICAL_RESEARCH_STATUS_2026-11-18.md`. Ältere 5050-only-Befunde sind historische Zwischenstände.
- Externe Protokollimplementierungen als Recherchequellen kennzeichnen; Lizenz/Provenienz vor Codeübernahme prüfen. Wissenschaftliche Methoden: `docs/ENGINEERING_SCIENTIFIC_STANDARDS.md`.

## Tests und Freigaben

- Android-App `de.pulselibre`; JDK 21 im CI, Gradle 8.13, Kotlin/JVM-Bytecode-Ziel 17, Android SDK 35. Gradle-Wrapper laut README nicht vorhanden.
- Schnellprüfung: `gradle :app:testDebugUnitTest :app:assembleDebug`; Python-HCI-Tests: `python3 -m unittest discover -s tools -p 'test_*.py' -v`.
- Vollprüfung: `gradle :app:lintDebug :app:assembleDebugAndroidTest` plus API-35-Emulator. Die am 18.11.2026 geprüfte `.github/workflows/android.yml` startet **ausschließlich manuell** (`workflow_dispatch`). Ein Schnelllauf führt Python-HCI- und JVM-Tests sowie den Debug-Build aus; mit `full_tests=true` kommen Lint, Instrumentation-APK und API-35-Emulator hinzu. **Pushes, PRs und Tags lösen diesen Workflow derzeit nicht automatisch aus.** Ein grüner schneller CI-Lauf ist **keine** vollständige Installationsfreigabe.
- Signierte Update-APK nur bei korrekt konfigurierten Secrets und nach überprüfter Zertifikat-/Versionskontinuität; keine Secrets anzeigen. Hardwaretests ausschließlich nach expliziter Zustimmung und mit klaren Stop-Kriterien.

## Wo Entscheidungen stehen

- Aktueller Status und Übergabe: `docs/PROJECT_STATUS.md`
- Einstieg für neue Agenten: `docs/CHAT_START_HERE.md`
- Anforderungen und Architektur: `docs/REQUIREMENTS_AND_PRINCIPLES.md`, `docs/ARCHITECTURE_AND_CODE_REVIEW.md` (historische Befunde)
- CIRQA-Historie: `docs/protocol/research/CIRQA_HISTORICAL_RESEARCH_STATUS_2026-11-18.md`, `docs/protocol/sync/CIRQA_HISTORY_SYNC_PLAN.md`
- Release-/Signing-Regeln: `docs/ANDROID_RELEASE_READINESS.md`
- Audit und offene Arbeit: `docs/AUDIT_2026-11-16.md`, GitHub-Issues #2–#6
- Autonomer Auftrag (nur bei tatsächlich eingerichteter Automation): `docs/AUTONOMOUS_DEVELOPMENT_AGENT.md`

Bei Widersprüchen: **aktueller Code/Workflow + belegte Tests > aktueller Projektstatus > historische Dokumente**. Widerspruch offen kennzeichnen statt stillschweigend Annahmen zu treffen.
