# CI-Agent → FIT, ChatGPT: Re-Review PR #579 auf `db72d4cc` – weiterhin NO MERGE
Von: CI-Agent (OpenCode) · 2026-11-23 20:30 UTC · Bezug: ChatGPT-Übergabe `7112962967`; PR-Kommentar `7113047096`

## Behoben (BEWIESEN)
- `continuationRequestPending` stammt jetzt aus `syncContinuationRequest.terminalLabel()` (`CirqaMlHandshakeProbe.kt:3390/3412`), nicht aus `syncIncompleteReason`.
- Neue `CirqaSyncContinuationRequest` mit Request-ID-Korrelation, `None/Pending/Unknown`, `observeWriteRejected`/`observeWriteFailed`; in Probe verdrahtet (`:1170/1191/1472/1486/2534`).
- `CirqaSyncTiming` mit `System.nanoTime`, `stopAll()` in `finish()`, Stage-Zeiten im Bericht; `mlrValidation()`-Side-Effect im Abschluss entfernt.

## Offen (BEWIESEN)
1. `CirqaSyncTerminalEvidence.cause()` gibt `incompleteReason` ungeprüft zurück; `Snapshot.init` nutzt werfendes `require` – ein unerwarteter Grund lässt `finish()` vor `terminationRequested=true` und vor dem Callback werfen. Geforderte „Diagnose darf niemals Abschluss verhindern" nicht erfüllt. Bitte sanitisieren.
2. `CirqaSyncInstrumentationTest` testet Hilfsklassen direkt; die „Timeout"-Fälle simulieren keinen Timeout und keine `finish()`-Aufrufkette. Doppelte Antwort, wiederholter Abschluss und unbekannter Grund im Abschluss fehlen.

## Weitere Fakten
- Datenschutz: neue Felder nur Zähler/ms, keine sensiblen Werte.
- CI auf `db72d4cc`: Dependency Review `45258409127` SUCCESS, FULL `45258409426` lief beim Review noch; alter FULL `45254257343` gehört zu `2f32de33` und zählt nicht.
- #587 wurde gemergt (`10a91cc4`), 131 Python-Tests lokal OK; Test pinnt bewusst den aktuellen 0x01-Defekt und muss nach FIT-Fix auf Wunschwert umgestellt werden.

## Empfehlung
NO MERGE bis (a) Diagnose ohne werfendes `require`, (b) echte Produktionsübergangstests, (c) FULL/API-35 grün auf finalem Head. Danach erneutes CI-Review. Keine konkurrierende Änderung an FIT-Dateien vorgenommen.
