# CI-Agent – Coverage-Semantik-Regression

- Agent/Umgebung: CI-Agent (OpenCode, CI/Tests)
- Beginn: 2026-11-22 14:05 UTC
- Letzte Aktualisierung: 2026-11-22 14:05 UTC
- Status: aktiv (Vorbereitung; Produktänderung nur nach Ankündigung)
- Rolle/Zuständigkeit: CI-/JVM-Regressionen auf synthetischen Daten
- Aktueller Auftrag: P1 aus der ChatGPT-Gesamtanalyse – klare Coverage-Semantik; `runDrained` darf `coverage=device-history-unverified` nicht zu `device-history-complete` aufwerten.
- Jetzt in Bearbeitung: Ankündigung in `agent-sync`, pure Helper-Extraktion `CirqaSyncCoverage`, JVM-Test `CirqaSyncCoverageTest`.
- Zeitnah geplant: Backup-v7-Manifest-/Prüfsummen-Tampering (nur mit Emulator oder Produkt-Extraktion; vorher separate Ankündigung). Keine exklusive Reservierung.
- Betroffene Dateien: `app/src/main/java/de/pulselibre/CirqaSyncCoverage.kt` (neu), `app/src/main/java/de/pulselibre/CirqaMlHandshakeProbe.kt` (eine Zeile), `app/src/test/java/de/pulselibre/CirqaSyncCoverageTest.kt` (neu), `docs/collaboration/agent-sync/`, eigene Sitzungsdatei.
- Branch/Worktree: `ci/coverage-semantik-20261122`, `/tmp/opencode/pulselibre-coverage-semantik-20261122`
- Abstimmungsbedarf: Produktzeile in `CirqaMlHandshakeProbe.kt` wurde per `agent-sync` angekündigt; Meldungen anderer Agenten prüfen. 157er CSV/DB-Differenz ist mit PR #488 erledigt (FIT-Agent); keine Geräteaktion.
- Nachweise: ausstehend (lokaler JVM-Lauf und voller PR-CI).
- Stop-Grenzen: keine BLE-/GATT-/FileSync-Kommandos, kein Reset/Entkoppeln/`CLOSE_ALL`, keine Löschung realer Daten, keine Instrumentationstests auf physischen Geräten, keine Rohdaten oder Gesundheitswerte in GitHub.
- Nächster Schritt: Helper und Test implementieren, lokal mit Gradle 9.6.0 prüfen, danach PR mit vollem CLOUD/FULL-CI; Merge erst nach gelesenem grünem Lauf.
