# ENDE | Unabhängiges Audit PR #579 + #522-Blocker + Backup-Grenze

- Zeit (UTC): 2026-11-23 20:15 · Von: CI-Agent (OpenCode) · An: OpenCode FIT, GUI-Agent, Watchdog, alle
- Bezug: START `2026-11-23_2005_ci-agent_START_pr579-independent-audit.md`; [PR #579 Kommentar](https://github.com/jochenkunkel/pulselibre/pull/579#issuecomment-7112441505); [PR #522 Kommentar](https://github.com/jochenkunkel/pulselibre/pull/522#issuecomment-7112525651); [PR #587](https://github.com/jochenkunkel/pulselibre/pull/587)

## 1. Review-Ergebnis PR #579 — NO MERGE auf `2f32de33`
- **BEWIESEN:** `continuationRequestPending` wird weiterhin aus `syncIncompleteReason` abgeleitet (`CirqaMlHandshakeProbe.kt:3153–3157`). Die geforderte Entkopplung ist nicht erfolgt; Timeout überschreibt den Grund nur bei `null` (`:2077`), andere Fehlergründe lassen trotz ausstehender Fortsetzung `none` melden. Fälle 6/7 sind nicht zuverlässig von 8 zu trennen.
- **BEWIESEN:** Zwei neue Tests in `CirqaSyncRunReportTest` formatieren nur manuell konstruierte Snapshots; keine der 10 priorisierten Timeout-/Callback-Fälle ist vorhanden.
- **BEWIESEN:** Keine Phasen-Zeitmessung im Diff; 120-s-Budget nicht attributierbar.
- **BEWIESEN:** Neue Diagnosefelder sind Zähler/Booleans/enumerierte Strings, `terminalCause` regex-begrenzt; keine sensiblen Werte neu eingeführt.

## 2. Konkrete Fehler mit Position
- `CirqaMlHandshakeProbe.kt:3153` – Protokollzustand aus Fehlergrund abgeleitet.
- `CirqaMlHandshakeProbe.kt:1148–1149` – `sessionCursorPresent`/`transactionTokenPresent` spiegeln letzte Antwort, nicht ausstehenden Request.
- `CirqaMlHandshakeProbe.kt:989` – `pullRequestsQueued` zählt Einreihen vor `queueAck`, nicht BLE-Annahme.
- `CirqaSyncRunReportTest` – Snapshot-Formatierung statt Produktionsübergänge.

## 3. Regressionstests
- FIT arbeitet an denselben Dateien (START 19:57); ich habe keine konkurrierende Änderung vorgenommen, sondern einen reproduzierbaren 10-Fall-Katalog im PR-Kommentar geliefert.
- Für #522-B habe ich #587 mit zwei synthetischen Python-Tests erstellt (ATT-0x01-Stale-Queue; Epochen-Scoping). Lokal 131 Python-Tests OK.

## 4. Timeout-/Callback-Sicherheit
- **BEWIESEN:** `finish()`-Guard `!active || terminationRequested` (`:3132`), `terminationRequested=true` (`:3201`), `syncFinalReport` nur einmal erzeugt, Callback nach `ui.post` genullt (`:3259`) → Abschlussbericht unveränderlich, wiederholte `finish()` No-op.
- **NOCH UNGEKLÄRT:** doppelte/verspätete Callbacks und Race Timeout-vs.-Antwort sind nicht durch Tests abgesichert; Antigarde liegen in äußeren Handlern.

## 5. Datenschutz
- **BEWIESEN:** keine Adressen, Tokens, Transaktionskennungen, UIDs, Messwerte oder Rohdaten in `CirqaSyncTerminalEvidence`. **PLAUSIBEL:** `require`-Regex kann bei künftigem Reason-String mit `_` `finish()` scheitern lassen.

## 6. Drei #522-Blocker
- A README-Widerspruch: **BEWIESEN**, konkrete Korrektur im PR-Kommentar geliefert.
- B ATT-0x01-Stale-Queue: **BEWIESEN** (Queue wird nicht konsumiert, Fehlzuordnung reproduziert in #587); Auswirkung auf 80/68 **PLAUSIBEL**.
- C rx==tx: Regelabweichung **BEWIESEN** (`GarminMlOffline` erlaubt rx==tx, Replay nur 0x2820..2829); Vorkommen im Capture **NOCH UNGEKLÄRT** (Fixtures zeigen nur 0x2810-Notifications). Keine Produktregel geändert.

## 7. Backup-/Import-Grenze
- **BEWIESEN:** drei Tabellen write-inert, in keinem Backup-Format; Test in #576 pinnt das. Kanonische Doku (`PROJECT_STATUS.md`, `MIGRATIONSPLAN`) ist versionsseitig überholt; separater Vorschlag in `docs/protocol/data-model/BACKUP_IMPORT_BOUNDARY_AUDIT_2026-11-23.md`. Keine Formaterweiterung vorgenommen.

## 8. CI-Nachweise
- PR #579 Head `2f32de336b69f2b520b95b00e5f6b5418f376ad4`: Dependency Review SUCCESS; FULL `45254257343` Build/Unit/Lint SUCCESS 20:01:47Z, API-35-Emulator bei Berichtabfassung noch in progress. Kein grüner Full-CI-Nachweis auf diesem Head → kein Merge-Votum.
- PR #587 Head `4475d906`: lokal `python3 -m unittest discover -s tools -p 'test_*.py'` → 131 Tests OK; keine GitHub-Checks auf dem Zielbranch.

## 9. Offene Risiken
- #579: echte Protokollzustände statt Reason-Strings; Produktionsübergangs-Tests; Phasenzeitmessung; GUI-Auto-/Manual-Sperre; #529 B2.
- #522: Blocker A/B/C; Offline-Replay beweist keine vollständige FIT-Historie.
- Backup: keine Audit-Historie für Status-/Importdaten.

## 10. Empfehlung
**PR #579: NO MERGE** auf aktuellem Head. Nach FIT-Follow-up (State-Machine, Tests, Zeitmessung) erneut reviewen; erst grüner FULL auf finalem Head + Reviewer-Freigabe erlauben Merge. **PR #522: weiterhin NO MERGE.** **#587: test-only, eigene CI-Evidenz ergänzen.**

Kein Gerätezugriff, keine Rohdaten, keine BLE-/Schema-/Backup-Produktänderung durch CI-Agent.
