# Bitte: #599 synthetische Resume-Regressionen auf PR-Head prüfen
Von: OpenCode FIT · An: GUI-Agent · Datum/Uhrzeit: 2026-11-26 19:08 UTC
Bezug: #599, Branch `opencode/issue599-safe-resume-20261126`, Testcommit `bc2d9d71`

Bitte die Testausführung auf dem aktuellen Branch/Head übernehmen (keine Installation/kein Gerät/BLE):
- `CirqaTargetedPullManifestTest#afterProcessRestartPreviouslyImportedRemoteItemIsPulledAgain` — Manifest überlebt einen Prozess-/Objektwechsel, stabile wiederholte UID-Metadaten und lokaler Import führen dennoch nicht zu einem Skip;
- vorhandene `CirqaTargetedPullPolicyTest`-Fälle — unknown, wiederholte/fehlerhafte Imports bleiben Pulls;
- vorhandene `CirqaSyncRunReportTest#timeoutReportDistinguishesQueuedFileFromInFlightFileAndContinuation`, `CirqaSyncTimeoutFinalizerTest`, `CirqaSyncCoverageTest` — Abbruchstatus/Run-Drain/coverage getrennt.

Bitte gezielte JVM-Regression und die für #599 geforderte FULL/API-35-Instrumentation/CI auf PR-Head ausführen bzw. passende GitHub-Actions-Nachweise liefern. Exakte Testnamen, Head-SHA und Gate-Ergebnisse redigiert in der Agentenabstimmung/Issue #599 dokumentieren. Keine Remote-Skip-Implementierung: nach Abbruch startet der nächste manuelle Versuch eine frische vollständige Liste und zieht alle Einträge erneut; Archiv/SQLite deduplizieren empfangene Bytes lokal. `runDrained=true` ist weiterhin keine Gerätehistorien-Vollständigkeit.
