# UPDATE | Per-device Lease umgesetzt, PR #549 offen

- Zeit (UTC): 2026-11-23 07:26
- Von: CI-Agent (OpenCode)
- An: FIT-Integrationsverantwortlicher, GUI-Agent; zur Kenntnis Nutzer
- Bezug: START `2026-11-23_070300_ci-agent_START_device-lease-hardening.md`; Übergabe `docs/collaboration/agent-sync/2026-11-23_0720_ci-agent-an-fit-gui_s10-s25-lease-cli.md`
- Status: Code/Test fertig; PR-CI aktiv
- Zuständigkeit und betroffene Dateien: `tools/s25_lease.py`, `tools/test_s25_lease.py`, `AGENTS.md`, Lease-Doku
- Geplanter Umfang / tatsächlich erledigt: S10/S25 getrennte kanonische flock-Locks; alte Default-S25-CLI bleibt kompatibel; Serial-Alias nur über explizite Umgebungskonfiguration und wird nicht gespeichert; Task/Session/Test/Phase-Statusfelder; ETA bleibt Schätzung; SIGINT/SIGTERM/SIGHUP beenden gestartete Prozessgruppe und bereinigen Statusdatei. Dokumentiert, dass direkte Hardwaretools nur unter Lease laufen und dass `flock` hostlokal/kooperativ ist.
- Nachweise: Commit `ada5f5da`, Worktree-Rebase auf aktuelle main-Basis `b2369ed4`, Folgecommit `273fec19`; 6 Lease-Tests und ganzer Python-Satz **118/118** grün. PR [#549](https://github.com/jochenkunkel/pulselibre/pull/549). PR-Android-Run `45130869838` und Dependency Review laufen; Graph-Job erfolgreich.
- Geräte-Übergabe: S10e Build-1934-Check im gültigen gehaltenen Legacy-Wrapper beendet, Status FREI geprüft und GUI über Issue #529 Kommentar `7094157998` informiert. GUI hat danach selbst reserviert; keine weitere CI-Geräteaktion. #529 selbst nicht ausgeführt.
- Beobachtung: Die UI meldete `GATT verbunden (Aufzeichnung aus)`; `dumpsys activity services` ohne App-Service. Die frühere Nutzerbeobachtung „Aufzeichnung läuft“ ohne Dienst wurde in diesem Momentcheck nicht reproduziert.
- Was ich von anderen brauche: Review der CLI/Koordination durch FIT-Owner; GUI setzt seinen exklusiven #529-Lauf fort, ohne dass CI zugreift.
- Nächster Schritt / Besitzer: CI-Agent liest alle PR-Checks, wartet nicht über die GUI-Gerätesperre hinweg mit Gerätezugriff.
