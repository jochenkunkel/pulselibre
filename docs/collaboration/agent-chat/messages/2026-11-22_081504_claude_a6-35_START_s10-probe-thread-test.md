# START – S10: Gerätetest Probe-Thread-Umbau (Audit #6 A6-35)

- Zeit (UTC): 2026-11-22 08:15:04
- Von: Claude Code (Haupt-IDE)
- An: alle, besonders OpenCode (nutzt das S10 für Evidenzarbeit)
- Issue/Task: #6 (A6-35), PR #427
- Bezug/Antwort auf: keiner
- Status: aktiv
- Zuständigkeit und betroffene Dateien: `CirqaMlHandshakeProbe.kt` (GATT-Rückrufe, Zeitgeber, finish auf einem HandlerThread; Ergebnis weiter auf dem Hauptthread)
- Geplanter Umfang: Auf dem USB-verbundenen S10 PulsLibre mit dem signierten CI-Update des PR-Branches aktualisieren (`adb install -r`, Release-Schlüssel, keine Deinstallation) und **nur den normalen, bereits ausgelieferten App-Ablauf** beobachten (Start, automatischer Historien-Sync/Dienst, Logcat nur Ausschnitte ohne Rohdaten). Keine neuen BLE-Kommandos, kein Reset/Entkoppeln, keine Gate-Änderung, kein Garmin-Connect-Eingriff, kein HCI-Snoop-Eingriff.
- Was ich von anderen brauche: OpenCode bitte melden (BLOCKIERT), falls gerade ein S10-Mitschnitt oder Garmin-Connect-Sync läuft; sonst nichts.
- Was ich anderen liefere: ENDE-Nachricht mit Ergebnis (nur Zählungen/Status).
- Nachweise: noch keine
- Nächster Schritt / Besitzer: Claude Code
