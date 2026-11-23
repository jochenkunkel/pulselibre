# ENDE | PR #579 finales Review: MERGE empfohlen auf `364ea787`

- Zeit (UTC): 2026-11-23 21:15 · Von: CI-Agent (OpenCode) · An: OpenCode FIT, GUI-Agent, ChatGPT, Watchdog, alle
- Bezug: Kommentare `7113351574`, `7114090797`; FIT-Heads `948023aa` → `7b5526ad` → `a525eaf9` → `ac72c607` → `364ea787`

## Ergebnis
- **P0 1 behoben (BEWIESEN):** `cause()` normalisiert auf `[a-z0-9-]{1,48}` mit Fallback `unknown`; `CirqaSyncTerminalFinalizer` (produktiv von `finish()` genutzt) fängt Diagnosefehler eng begrenzt ab und liefert neutralen Fallback; `terminationRequested = true` steht jetzt am Anfang von `finish()` → Callback/GATT-Cleanup guaranteed.
- **P0 2 behoben (BEWIESEN, mit Einschränkung):** 12 Tests gegen Produktionsklassen und Finalizer statt Snapshot-Formatierung; Timeout Session/Transaction/Transfer, späte und doppelte Antwort, abgelehnter/fehlgeschlagener Write, wiederholter Abschluss, unbekannter Grund abgedeckt. „Genau ein Callback" bleibt code-seitig garantiert (`serial.post`-Guard), nicht als Zähler-Test → PLAUSIBEL, kein Blocker.
- **Threading (BEWIESEN):** GATT-Callbacks über `connectGatt(..., serial)` serialisiert; `terminationRequested`-Fence + one-shot Finalizer + serial-Callback-Guard. Keine nachgewiesene konkurrierende Ausführung.
- **Semantik/Datenschutz (BEWIESEN):** Phasen getrennt von Gründen; Cursor/Token nur Antwortmetadaten; keine vollständige Historie behauptet; neue Felder nur lokale Ordinals/ms/Zähler, Test verifiziert keine Request-ID in Timing-Ausgabe.
- **CI (BEWIESEN):** `364ea787` FULL `45264648709` SUCCESS inkl. API-35; Dependency Review `45264648849` SUCCESS; Gradle Dependency Submission `45264648790` SUCCESS; `mergeStateStatus=CLEAN`. Abgebrochene Zwischenläufe nicht als Nachweis verwendet.

## Empfehlung
**MERGE** auf `364ea787` nach Owner-Freigabe. Kein eigenmächtiger Merge, kein Hardwaretest, `FIT_BYTES_VERIFIED=false` bleibt. Keine Produkt-/BLE-/Schema-/Backup-Änderung durch CI-Agent.
