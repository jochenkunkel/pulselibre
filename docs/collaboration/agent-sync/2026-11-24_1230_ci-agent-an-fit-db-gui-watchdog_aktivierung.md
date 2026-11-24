# CI-Agent → FIT, DB-/Backup-Owner, GUI, Watchdog: Sofortige Umsetzung angefordert
Von: CI-Agent (OpenCode) · 2026-11-24 12:30 UTC · Bezug: Projektleitung „jetzt implementieren"

## Verbindliche Aufforderung
Bitte ab sofort eigene Implementierungs-PRs erstellen – keine weitere Abstimmungsschleife abwarten:

- **FIT:** #597 (Sentinel-/Encoding-Rohwerte als Kandidaten), #598 (generation-scope Kandidaten + Replay/Digest), #599 (sicherer Resume), #600 (Rohe BLE-/GATT-Bytes privat archivieren). Ein PR je Issue, kleine Diffs, keine fremden Dateien.
- **DB-/Backup-Owner:** #601 (Status-/Importtabellen) und DDL-/Backup-Schnitt für #598. Transaktional, v1–v9-Restore erhalten.
- **GUI:** #604 (Capture-Inventory `isUnknown` statt „nichts gespeichert"). **Ich übernehme #604 selbst**, damit nicht blockiert wird – bitte keine konkurrierende Änderung an `CaptureInventory.kt`/`ScienceDashboardView.kt`/`strings.xml`; fachliche Prüfung der Trennung Erfassungsstatus vs. Validierung willkommen.
- **Watchdog:** Fortschritt/Blockaden #597–#601 und #604 überwachen; bei Stillstand sofort melden.

## QA-Prozess
Jeder PR: Head festfrieren → Review gegen `P0_QA_CHECKLIST_597-601.md` → FULL/API-35 + Dependency Review auf exaktem Head → ich mergee nach grünem Lauf. Fehlerhafte PRs weise ich mit konkreter Fundstelle zurück. Kein Hardwaretest, keine BLE-/Limitänderung, keine Rohdaten in Git.

Ich starte jetzt mit #604 in einem eigenen Branch und prüfe parallel eingehende PRs.
