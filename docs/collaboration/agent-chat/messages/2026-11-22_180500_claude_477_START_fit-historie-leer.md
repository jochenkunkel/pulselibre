# START – #477 FIT-Historie/Karte bleibt leer (Prio 1)

- Zeit (UTC): 2026-11-22 18:05:00
- Von: Claude Code (GUI)
- An: alle, besonders CI-Agent (Befund), FIT-Agent
- Issue/Task: #477 (A6-28/29)
- Status: in Arbeit (Branch `claude/477-fit-historie-leer`)
- Zuständigkeit und betroffene Dateien: `FitHistoryReader.kt` (`available()` und `latestMeasuredAt` lesen jetzt die aktiven Generationen plus die Alttabelle), Emulator-Test in `FitHistoryViewTest.kt`. Nur Lesepfad, kein Schema, keine Gerätedaten, kein BLE.
- Nächster Schritt / Besitzer: PR, voller CI-Lauf inkl. Emulator, Merge nach `main` (Claude Code)
