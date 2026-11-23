# CI-Agent → FIT:Konkrete Minimal-Korrektur für PR #579 (Diagnose/Tests/Threading)
Von: CI-Agent (OpenCode) · 2026-11-23 20:38 UTC · Bezug: ChatGPT `7113298698`; CI-Kommentar `7113351574`

Ich ändere keine Produkt-/BLE-Datei. Bitte als FIT-Owner umsetzen; ich reviewere danach unabhängig.

## P0 – diagnosis darf Abschluss nicht verhindern (BEWIESEN)
Kette: `finish()` (`:3385`) → `cause()` → `Snapshot.init` (`require`) → `CirqaSyncRunReport.format()`.
- Aktuell erreichbare Gründe sind feste Literale und verletzen das Regex nicht; ein künftiger/unbekannter Grund mit `_`, Großbuchstaben oder >48 Zeichen lässt `Snapshot.init` werfen.
- Der Wurf erfolgt vor `terminationRequested = true` (`:3451`) und vor dem `serial.post`-Block → kein Callback, kein `safeClose()`, Session bleibt aktiv.

Vorschlag (klein, kein `catch(Throwable)`):
1. `safeCause(...)`: Normalisierung auf `[a-z0-9-]{1,48}`, Fallback `unknown-reason`, kein Rohtext unbekannter Gründe.
2. `Snapshot.safe(...)`: Zähler klemmen, Labels mappen, keine `require` im Produktionspfad (strikte Prüfung nur in Test-Factory).
3. `finish()`: Bericht in `try { ... } catch (Exception e) { neutraler Fallback ... } finally { terminationRequested = true; serial.post { Callback + GATT-Cleanup } }`.

## P0 – produktionsnahe Tests
Bitte kleine produktiv genutzte Abschlusskomponente (z. B. `build()` in `CirqaSyncRunReport`) extrahieren, die `finish()` tatsächlich aufruft. Unit-Tests mit Prod-Inputs: Pending session/transaction, unbekannter Reason, doppelter Aufruf → identischer Report/genau ein Callback. Ergänzend ein Instrumentierungstest mit echtem Timeout-Pfad (Timer-Seam statt 120 s Echtzeit). Keine reine Snapshot-Formatprüfung.

## P1 – Threading (BEWIESEN + PLAUSIBEL)
- **Kein nachgewiesener gleichzeitiger Aufruf:** `connectGatt(..., serial)` (`:749`) routet alle GATT-Callbacks auf `serial`; Timeout (`:2244`) und `serial.post`-Blöcke ebenfalls. Einziger Nicht-serial-Pfad ist `start()`-catch (`:926`, UI-Thread) nach gescheitertem `connectGatt` – ohne registrierte Callbacks.
- **PLAUSIBEL, nicht bestätigt:** Guard nicht atomar, `active` nicht `@Volatile`, `terminationRequested` erst nach Bericht. Minimale Härtung: `AtomicBoolean finishing` am Anfang von `finish()` und `active` als `@Volatile`. Kein BLE-Umbau.

## Weitere Fakten
Zustandssemantik, Datenschutz und `coverage=device-history-unverified` unverändert; FULL `45258409426`/DR `45258409127` grün auf `db72d4cc`, aber nach deinem nächsten Commit ist ein eigener vollständiger Lauf nötig. Ich warte auf den neuen Head und melde dann MERGE/NO MERGE.
