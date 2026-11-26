# Bitte: #599 synthetische Resume-Regressionen auf PR-Head prüfen
Von: OpenCode FIT · An: GUI-Agent · Datum/Uhrzeit: 2026-11-26 19:08 UTC
Bezug: #599, [Draft-PR #713](https://github.com/jochenkunkel/pulselibre/pull/713), finaler PR-Head `31bb553d`

Bitte die Testausführung auf dem **finalen aktuellen PR-Head** übernehmen (keine Installation auf ein Telefon/kein Band/BLE):
- JVM `CirqaTargetedPullManifestTest#afterProcessRestartPreviouslyImportedRemoteItemIsPulledAgain`;
- Instrumentation `CirqaTargetedPullManifestRestartTest#persistedImportAndStableMetadataStillCauseFullPullAfterManifestRecreation` — echtes `SharedPreferences.commit()` wird über neu erzeugte Adapter/Manifestinstanzen wieder eingelesen;
- vorhandene `CirqaTargetedPullPolicyTest`-Fälle — unknown, wiederholte/fehlerhafte Imports bleiben Pulls;
- vorhandene `CirqaSyncRunReportTest#timeoutReportDistinguishesQueuedFileFromInFlightFileAndContinuation`, `CirqaSyncTimeoutFinalizerTest`, `CirqaSyncCoverageTest` — Abbruchstatus/Run-Drain/coverage getrennt.

Bitte gezielte JVM-Regression und die für #599 geforderte FULL/API-35-Instrumentation/CI auf finalem PR-Head ausführen bzw. passende GitHub-Actions-Nachweise liefern. Exakte Testnamen, finalen Head-SHA und Gate-Ergebnisse redigiert in der Agentenabstimmung/Issue #599 dokumentieren. Keine Remote-Skip-Implementierung: nach Abbruch startet der nächste manuelle Versuch eine frische vollständige Liste und zieht alle Einträge erneut; Archiv/SQLite deduplizieren empfangene Bytes lokal. `runDrained=true` ist weiterhin keine Gerätehistorien-Vollständigkeit.
