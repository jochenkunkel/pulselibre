# PulsLibre – Arbeitsanweisungen für Codex und menschliche Entwickler

Diese Datei gilt repositoryweit. **GitHub ist die verbindliche, versionierte Wissensbasis**, nicht eine Chat-Unterhaltung oder eine lokale Codex-Sitzung. Für den aktuellen Projektstand zuerst `docs/PROJECT_STATUS.md` lesen; für den vollständigen Einstieg `docs/CHAT_START_HERE.md`.

## Aktuelle Aufgabenverteilung (2026-11-17)

- **Codex in VSCodium – alleiniger Bearbeiter für Dashboard/UI und wissenschaftliche Analyse:** Umsetzung des freigegebenen Vier-Screens-Konzepts in `docs/ui/APPROVED_UI_CONCEPT_2026-11-15.md`, Prüfung vorhandener Dashboard-/Export-Datenflüsse, nachvollziehbare Visualisierung echter lokal gespeicherter Messwerte; wissenschaftliche Methoden/Quellen kritisch prüfen und nur nach `docs/ENGINEERING_SCIENTIFIC_STANDARDS.md` implementieren. Keine erfundenen Schlafphasen, SpO₂, Kalorien, Bewegungssensorwerte oder klinischen Aussagen. Bestehende Datenbank und Exportsemantik erhalten.
- **ChatGPT-Entwicklungsagent – alleiniger Bearbeiter für CIRQA-Historien-Download:** Garmin-ML/GFDI/5050-Capabilities, sichere FileSync-/FIT-Protokollforschung, Offline-Tests und gezielte, ausdrücklich freigegebene S25-Gerätetests. Keine spekulativen BLE-Schreibbefehle; Vollständiger historischer FIT-Download weiterhin unbewiesen. Einstieg: `docs/protocol/research/CIRQA_HISTORICAL_RESEARCH_STATUS_2026-11-18.md`, danach die dort verlinkten datierten Geräteberichte.
- **Gemeinsame Dateien** (`AGENTS.md`, `README.md`, `docs/PROJECT_STATUS.md`, `docs/CHAT_START_HERE.md`, CI/Gradle, Datenbankschema): vor Änderungen koordinieren, eigene Branches/Worktrees verwenden und Konflikte nicht blind überschreiben. Codex soll keine BLE-Protokolländerungen vornehmen; ChatGPT soll keine Dashboard-/Wissenschaftsänderungen vornehmen, solange diese Aufteilung gilt.
- Codex arbeitet nicht allein durch diesen Eintrag automatisch: Auftrag in der Codex-Sitzung starten. Die Zuständigkeit ist eine Arbeitsvereinbarung, kein Nachweis einer laufenden Agenten-Ausführung.

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
- Vollprüfung: `gradle :app:lintDebug :app:assembleDebugAndroidTest` plus API-35-Emulator. Die aktuelle `.github/workflows/android.yml` führt Full-Gates bei `v*`-Tags oder manuell mit `full_tests=true` aus; gewöhnliche Pushes/PRs erhalten nur den schnellen CI-Gate. Ein grüner schneller CI-Lauf ist **keine** vollständige Installationsfreigabe.
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
