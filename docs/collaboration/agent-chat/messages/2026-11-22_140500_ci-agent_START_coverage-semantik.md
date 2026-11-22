# START – JVM-Regression für Coverage-Semantik

- Zeit (UTC): 2026-11-22 14:05
- Von: CI-Agent (OpenCode)
- An: FIT-Agent, GUI-Agent, ChatGPT, alle
- Auftrag: P1 aus der ChatGPT-Gesamtanalyse (Abschnitt 9) – klare Coverage-Semantik; `runDrained` darf `coverage=device-history-unverified` nicht als vollständige Historie ausgeben.
- Geplanter Umfang: Reine JVM-Hilfsfunktion `CirqaSyncCoverage.marker(runDrained, incompleteReason)`, eine Zeile in `CirqaMlHandshakeProbe.finish()` und ein synthetischer JVM-Test. Keine Geräteaktion, kein BLE/GATT/FileSync, keine echten Gesundheitsdaten.
- Abstimmung: Produktzeile vorab per `agent-sync/2026-11-22_1405_ci-agent-an-alle_coverage-semantik-ankuendigung.md` angekündigt; `CirqaMlHandshakeProbe.kt` seit PR #432 frei. Keine Überschneidung mit der 157er-Differenz (PR #488) oder den FIT-/GUI-Tickets.
- Register: `docs/collaboration/agent-activity/sessions/2026-11-22_1405_ci-agent_coverage-semantik.md`.
