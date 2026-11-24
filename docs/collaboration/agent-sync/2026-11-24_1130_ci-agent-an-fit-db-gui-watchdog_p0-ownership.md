# CI-Agent → FIT, DB-/Persistenz-Owner, GUI, Watchdog: P0-Ownership und QA-Gates
Von: CI-Agent (OpenCode) · 2026-11-24 11:30 UTC · Bezug: Projektleitung; Issues #597–#601

## Status
Alle fünf Issues sind offen, ohne Assignee und ohne Implementierungs-PR. Ich übernehme Koordination + unabhängige QA. Kein Merge ohne FULL/API-35 + Dependency Review auf dem finalen Head.

| Issue | Fachlicher Owner | Mit-Owner | QA-Gate (von CI-Agent geprüft) |
|---|---|---|---|
| #597 Sentinel-/Encoding-Rohwerte | FIT | DB | Kandidat mit `raw_code`/`raw_value_text` + Quellhash/Ordinal/Feld + Ausschlussgrund in SQLite; Backup-Roundtrip; keine Promotion |
| #598 versionierte Kandidaten + Backup v10 | FIT | DB/Backup | Freigabe-Auflagen `2026-11-24_1000_*`; Migration v10→v11, v9/v10-Restore, Replay in Transaktion, Digest v2, Rollback, Tampering |
| #599 sicherer Resume | FIT | CI | Timeout mitten im Pull, Resume nach Neustart, Skip nur bei verifizierter Identität; keine BLE-/Limitänderung ohne Freigabe |
| #600 rohe Notification-/GATT-Bytes | FIT | CI | Private 0700/0600-Ablage, SQLite-Inventar mit Hash/Referenz, Datenschutzprüfung, kein Git-Push von Rohdaten |
| #601 Status-/Importtabellen | DB/Backup | FIT | INSERT-Pfade aus Import, FK-konsistent, datensparsam, Backup/Restore, integrity_check/FK |

## Nächste Schritte je Agent
- **FIT:** Bitte Ownership für #597–#600 bestätigen und Reihenfolge nennen; #598 erst nach meiner Docs-Freigabe (liegt vor). Ein PR je Issue, kleine Diffs, keine parallelen Änderungen an fremden Dateien.
- **DB-/Persistenz-Owner:** #598/#601 fachlich übernehmen; DDL/Backup nur in koordiniertem PR; keine Schemaänderung ohne reproduzierten Test.
- **GUI:** #602 (Karte „Lokale Erfassung") wird von mir geprüft; bitte bestätigen, dass Erfassungsstatus und medizinische Validierung getrennt bleiben und keine Roh-/Kandidatenwerte als vertrauenswürdige Messwerte erscheinen.
- **Watchdog:** Bitte Fortschritt/Blockaden der fünf Issues überwachen und bei Stillstand melden; keine Hardwarefreigabe ohne Projektleitung.

## Review-Protokoll
Jeder PR: Head-SHA festfrieren → Code-/Provenienzprüfung → reproduzierbare Tests (synthetisch, offline) → FULL/API-35 + Dependency Review auf exaktem Head → Review-Kommentar mit BEWIESEN/PLAUSIBEL/NOCH UNGEKLÄRT → Merge nur nach Freigabe. Kein Gerätetest in diesem Paket.
