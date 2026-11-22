# CI-Agent – Coverage-Semantik-Regression

- Agent/Umgebung: CI-Agent (OpenCode, CI/JVM-Tests)
- Beginn: 2026-11-22 14:05 UTC
- Letzte Aktualisierung: 2026-11-22 14:20 UTC
- Status: abgeschlossen
- Rolle/Zuständigkeit: CI-/JVM-Regressionen auf synthetischen Daten
- Auftrag: P1 aus der ChatGPT-Gesamtanalyse – `runDrained` darf `coverage=device-history-unverified` nicht zu `device-history-complete` aufwerten.
- Ergebnis: Neue reine JVM-Hilfsfunktion `CirqaSyncCoverage.marker(runDrained, incompleteReason)`; `CirqaMlHandshakeProbe.finish()` nutzt sie (eine Zeile, keine Verhaltensänderung); `CirqaSyncCoverageTest` mit 2 synthetischen Tests deckt `runDrained=true/false` und mehrere `incompleteReason`-Werte ab. Kein Produktverhalten geändert.
- Zeitnah geplant (nicht exklusiv reserviert): Backup-v7-Manifest-/Prüfsummen-Tampering – braucht Emulator oder Produkt-Extraktion in `DatabaseBackup.kt`; vorher per `agent-sync` ankündigen.
- Betroffene Dateien: `app/src/main/java/de/pulselibre/CirqaSyncCoverage.kt`, `app/src/main/java/de/pulselibre/CirqaMlHandshakeProbe.kt` (eine Zeile), `app/src/test/java/de/pulselibre/CirqaSyncCoverageTest.kt`, `docs/collaboration/agent-sync/`, `docs/collaboration/agent-chat/messages/`, eigene Sitzungsdatei.
- Branch/Worktree: `ci/coverage-semantik-20261122` (PR #489, gemergt als `4bda9e1`), Follow-up-Doku `ci/coverage-followup-docs-20261122`.
- Abstimmungsbedarf: Produktzeile wurde vorher per `agent-sync/2026-11-22_1405_ci-agent-an-alle_coverage-semantik-ankuendigung.md` angekündigt; keine Meldung anderer Agenten mit Bearbeitung derselben Datei. Keine Geräteaktion.
- Nachweise: lokal `:app:testDebugUnitTest --tests de.pulselibre.CirqaSyncCoverageTest` → BUILD SUCCESSFUL, 2 Tests, 0 Failures; PR #489 CLOUD/FULL `45031141035` (Branch-Head `b4daffa`) und PR-Lauf `45031136855` (Build/Unit/Lint/APK + Emulator API 35) grün; dependency-review `45031136832` grün; FULL auf `main` `45032146900` gestartet.
- Stop-Grenzen: keine BLE-/GATT-/FileSync-Kommandos, kein Reset/Entkoppeln/`CLOSE_ALL`, keine Löschung realer Daten, keine Instrumentationstests auf physischen Geräten, keine Rohdaten/Gesundheitswerte/Gerätekennungen in GitHub.
- Übergabe: Coverage-Marke ist testgesichert; fachlich weiterhin `coverage=device-history-unverified`, da keine vollständige Historie nachgewiesen ist.
