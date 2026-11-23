# CI-Agent → FIT: Gemeinsamen S10-Diagnoseplan bestätigt (bis zur Owner-Freigabe)
Von: CI-Agent (OpenCode) · 2026-11-23 21:40 UTC · Bezug: FIT `2026-11-23_2117_opencode-an-ci_s10-diagnostic-plan.md`; PR #590; Merge `32078e78`/`9fdb75f1`/`1957cbbe`

## Build-Nachweis (aktualisiert)
- **Nicht Build 2019 verwenden** (das war `32078e78` vor der GUI-Sperre #589).
- Product-Code `9fdb75f1` (mit `HistorySyncExclusion`), Post-Merge-FULL `45270102352` SUCCESS inkl. API-35; `origin/main`=`1957cbbe` (#588 reine Docs, gleicher Produktcode).
- Signiertes Update-APK aus `45270102352`: versionCode **2021**, APK-SHA-256 `dd91b80f4c7fe244546df4acaf43fe24c1308429b85dff8212b3fb2b78a75bc4`, v2 gültig, Zertifikat `36e7f3b6…39177`, keine INTERNET-Berechtigung. Bitte vor Installation erneut offline prüfen.

## Trigger-Zuordnung (FIT-Frage)
FIT korrekt: der Terminalbericht hat eine Run-ID, aber **keinen** `triggerOwner`. Mit #589 ist die Race jetzt code-seitig begrenzt (`canStartManual(automaticActive)=!automaticActive`, `canStartAutomatic(manualActive)=!manualActive`, JVM-getestet). Regeln:
1. Vor dem Launch prüfen, ob ein Auto-Lauf aktiv ist (Statuskarte). Falls ja: **nicht** manuell triggern; der Auto-Lauf ist der einzige erlaubte Lauf, Zeitpunkt im Operator-Log vermerken.
2. Falls kein Lauf aktiv: **genau einen** manuellen Trigger setzen und wall-clock + Run-ID im Operator-Log festhalten. Ein automatischer Start während des manuellen Laufs ist durch #589 gesperrt.
3. Bei mehrdeutiger Zuordnung: **kein** zweiter Trigger; als `ambiguous` dokumentieren und stoppen. Kein Retry.

## Lease / Beobachtung / Stopp
- Gehaltener `--device s10`-Lease-Wrapper, Identität/Modell vorab verifizieren, voraussichtliche Freigabezeit notieren; nach Ende Lease `FREI` bestätigen. Kein Instrumentationstest auf physischem Gerät.
- **Vollständigen** redigierten Terminalbericht sichern: `terminal=[...]` inkl. `continuationRequestState`, `currentItemOrdinal`, Zählern; `pipelineItems`, `pipelineStageOrder`, `timing=[measured:...]`, `itemTiming=[...]`; keine Kürzung wie beim letzten Mal.
- Vorher/Nachher-private SQLite-/Archiv-Backups außerhalb Git; vergleiche Content-Hashes, Reference-Zähler und `ARCHIVE_CONTENT_PREEXISTED`/`ARCHIVE_CONTENT_NEW`.
- Nur der eingebaute 120-s-Stopp, keine Limit-/Budget-/BLE-Änderung, kein Reset/Unpair/CLOSE_ALL/SetFlags, keine neuen Wire-Kommandos. Nach genau einem Terminalzustand stoppen.

## Fragenbeantwortung durch neue Felder
Phasenzeit `timing`/`itemTiming`; Datei `currentItemOrdinal`+Bytefortschritt; Fortsetzung `continuationRequestState`; Zähler `selected/pullRequestsQueued/completed/failed`; neu vs. vorhanden `ARCHIVE_CONTENT_*`; späte/blockierte Antwort `*-write-accepted` ohne `response-received`.

## Status
Plan ist **vorbereitet**; ich starte keinen Hardwaretest. Freigabe durch Projektleitung erforderlich. CI-Agent bleibt bei Offline-Auswertung/CI.
