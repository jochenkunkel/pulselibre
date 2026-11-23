# CI-Agent → FIT: Diagnoseplan für die 120-s-S10-Frage (nach Merge #579)
Von: CI-Agent (OpenCode) · 2026-11-23 21:18 UTC · Bezug: Projektleitung, Merge `32078e78` aus #579

## Ziel
Beantwortung der sechs Fragen mit der neuen, datensparasamen Diagnostik – **ohne** Hardwaretest jetzt, ohne Timeout-/BLE-/Budgetänderung.

## Verfügbare Felder (im gemergten Code, `CirqaSyncRunReport`/`CirqaSyncTerminalEvidence`)
- `terminalCause`, `continuationRequestState` (none/unknown oder session-/transaction- mit queued/write-accepted/response-received/completed/timeout/aborted).
- `listResponses`, `listed`, `sessionPageRequestsQueued`, `transactionFollowupsQueued`, `lastTransactionResponseItems`.
- `selected`, `pullRequestsQueued`, `completed`, `failed`, `pendingQueue`, `currentItem`, `currentExpectedBytes`, `currentReceivedBytes`, `currentItemOrdinal`.
- `fileSummary`: `pipeline=[Stages]`, `pipelineItems=[lokaler Ordinal:Hex-Bitset]`, `timing=[measured:...]`, `itemTiming=[itemN[phase:ms,...]]`.
- Neu: `ARCHIVE_CONTENT_PREEXISTED` / `ARCHIVE_CONTENT_NEW` je Datei (bereits vorhanden vs. tatsächlich neu).

## Zuordnung der Fragen
1. **Dominante Phase:** `timing=[measured:...]` (runTotal, bootstrap, itemList, sessionPage, transactionFollowup, fileTransfer, validation, archive, fitImport, sqliteCommit) + `itemTiming` pro Datei.
2. **Datei beim Abbruch:** `currentItemOrdinal` + `currentExpectedBytes`/`currentReceivedBytes` (partielle Bytes) + `pendingQueue`.
3. **Ausstehende Fortsetzung:** `continuationRequestState` ( expliziter Phasenstatus), nicht mehr aus `incompleteReason` abgeleitet.
4. **Zähler:** `selected`, `pullRequestsQueued`, `completed`, `failed` plus Pipeline-Stages (transfer/completed/integrity/archived/fit-decoded/sqlite-committed).
5. **Bereits vorhanden vs. neu:** `ARCHIVE_CONTENT_PREEXISTED`/`ARCHIVE_CONTENT_NEW` in `pipeline`.
6. **Blockierte/verspätete Antwort:** `continuationRequestState` endet auf `*-write-accepted` ohne `response-received`; `lastTransactionResponseItems` und `sessionCursorPresent`/`transactionTokenPresent` als letzte Antwortmetadaten.

## Ablauf (erst nach Owner-/Lease-Freigabe)
1. Signiertes Update-APK aus dem Post-Merge-Main-Lauf bauen; SHA/Zertifikat/Konto wie gehabt offline prüfen.
2. Genau **ein** kontrollierter S10-Lauf unter Lease, GUI-Übergabe abwarten, kein Retry, kein CLOSE_ALL/SetFlags, kein 120-s-Limit geändert.
3. **Vollständigen** Terminalbericht sichern (früher war er gekürzt): `terminal=[...]`, `pipelineItems`, `timing` und `itemTiming` müssen vollständig vorliegen.
4. Vorher/Nachher-SQLite-Backups und Originalarchiv offline vergleichen (Content-Hashes, Reference-Zähler, keine Rohdaten veröffentlichen).
5. Hypothesen prüfen: (a) `fileTransfer` dominiert und viele `ARCHIVE_CONTENT_PREEXISTED` → wiederholte Übertragung bereits archivierter Inhalte nach Entfernung des unsicheren Skips; (b) `itemList`/`sessionPage`/`transactionFollowup` dominiert → Pagination/Fortsetzung; (c) `validation`/`fitImport`/`sqliteCommit` dominiert → lokale Pipeline.
6. Ergebnis als datensparsame Matrix (Phasen-ms, Zähler, Ordinals) dokumentieren; keine Aussage über vollständige Historie ohne FIT-Beleg.

## Abstimmung
Bitte bestätigen: (1) reicht die neue Instrumentierung für Frage 1–6 oder fehlt eine Phase? (2) Wer führt den kontrollierten Lauf nach Freigabe durch? Ich übernehme weiterhin nur Offline-Auswertung/CI, kein Gerät.
