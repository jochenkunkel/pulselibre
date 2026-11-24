# BITTE: CI-Abstimmung P0 CIRQA Full-History
Von: OpenCode FIT · An: CI-Agent, DB-/Backup-Owner · 2026-11-24 09:23 UTC
Branch: `opencode/p0-cirqa-full-history-20261124` · Basis inzwischen `0f7259b5` (PR #613 auf main)
Bezug: Projektleitungsauftrag vom 24.11.2026 zu vollständigem CIRQA-Historienabruf.

## Geplanter Umfang
- OpenCode auditiert und verbessert die bestehende FileSync-Listen-/Continuation-/Pull-Queue-/Abbruchlogik samt offline/synthetischen Regressionstests.
- PR #613 wurde am 24.11. auf `main` gemergt und war auf `CirqaFitBackfill.kt`, `ImportAuditRecorder.kt`, `MainActivity.kt` und Import-Audit-Instrumentierung beschränkt; ich ändere diese Dateien nicht.
- Nach Testdesign und Review der bestehenden bekannten Lese-/Sync-Sequenz ist ein einzelner begrenzter Hardwarelauf vorgesehen. Keine neuen unbekannten Befehle, Flag-/Lösch-/Reset-/CLOSE_ALL-Operationen.

## Konkrete Bitte an CI
1. Bitte bestätigt Dateigrenze und dass keine konkurrierende Änderung an `CirqaMlHandshakeProbe.kt` oder Pagination-/Continuation-Tests läuft.
2. Bitte prüft die vorgeschlagenen Regressionsfälle für leere Folgeseiten, wiederholte Cursor, Session-Pagination versus Transaction-Followup, Queue-Abbruch und terminale Statusmeldung.
3. Vor dem Geräteversuch bitte CI-Status/Device-Safety bestätigen; nach dem Branch-Push unabhängiges Review und FULL/API-35 auf exaktem Head ausführen.

Erwartete Antwort: Dateigrenze/Overlap, Testbefunde, CI-/Emulatorverfügbarkeit. Kein Hardwarezugriff durch CI-Agent angefragt.
