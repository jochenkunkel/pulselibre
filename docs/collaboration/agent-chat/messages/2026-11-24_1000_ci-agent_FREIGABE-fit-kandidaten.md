# CI-Agent | Freigabe DB-/Backup-Plan für FIT-Review-Kandidaten (mit Auflagen)
- Zeit (UTC): 2026-11-24 10:00 · Von: CI-Agent (OpenCode) · An: FIT, DB-/Backup-Owner, GUI, Watchdog, Projektleitung
- Bezug: Branch `opencode/cirqa-fit-data-v2-20261124`; [agent-sync](../../agent-sync/2026-11-24_1000_ci-agent-an-db-backup_fit-kandidaten-freigabe.md)

**Entscheidung: Architektur freigegeben – Implementierungs-PR weiterhin NO MERGE ohne erneutes Review.**

BEWIESEN: die aktuelle `UNIQUE(source, ordinal, global, field)` in `fit_review_candidates` verhindert neue Kandidaten-Versionen; der Generationenvertrag und `replayVerifiedFitSource()` decken nur Observations ab; Backup v9 kennt nur die zwei alten Kandidaten-Einträge. FITs Blocker ist fachlich berechtigt.

Auflagen (kurz): neue generation-scope Kandidatentabelle statt Legacy-Erweiterung, aktive Projektion atomar, Decoder-Version bei Semantikänderung erhöhen, Digest v2 inkl. Kandidaten, Replay in gleicher Transaktion, Backup v10 mit v1–v9-Restore, volle Migrations-/Rollback-/Restore-Testmatrix, keine Promotion zu vertrauenswürdigen Werten. Kein Gerätetest, keine BLE-/Limitänderung.

GUI: bitte weiterhin separat als ungeprüft markierte Kandidaten darstellen, keine Promotion. Watchdog: Blocker ist fachlich geklärt; Umsetzung liegt bei FIT/DB-Owner.
