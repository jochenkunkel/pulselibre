# START – JVM-Regression für FIT-Provenienz-Metadaten

- Zeit (UTC): 2026-11-22 13:46
- Von: CI-Agent (OpenCode)
- An: alle, besonders FIT-Agent, GUI-Agent, ChatGPT
- Auftrag: P1 aus der ChatGPT-Gesamtanalyse (Abschnitt 9) – Backup-/Import-/Provenienz-Regression auf synthetischen Daten.
- Gewählter Umfang: Lücke 2 aus der lesenden Testlückenprüfung – `CirqaFitBackfillTest.kt` prüfte Idempotenz, Cancel und Retry, aber nicht, dass `sourceSha256`, `fileBytes`, `relativePath`, `decoderVersion`, `profileVersion` und `deviceKey` unverändert am Store ankommen. Neuer JVM-Test mit bestehender synthetischer FIT-Fixture, **kein** Produktcode, **kein** Gerät.
- Nicht in diesem Schritt: Coverage-Semantik (`runDrained` darf `coverage=device-history-unverified` nicht aufwerten) und Backup-Manifest-Tampering – beide bleiben als nächste Kandidaten dokumentiert; erstere bräuchte eine kleine Helper-Extraktion, letztere einen Emulator-Test.
- Abstimmung: 157er CSV/DB-Differenz bleibt FIT-Agent; #477 ist geschlossen; keine Geräteaktion ohne neue Freigabe.
- Nachweis/Register: `docs/collaboration/agent-activity/sessions/2026-11-22_1340_ci-agent_s10-nachbereitung.md`.
