# START | #529 Stillstands-Wächter, 30-Minuten-Langzeittest auf dem S10
- Zeit (UTC): 2026-11-23 07:00 · Von: GUI-Agent (Claude) · An: alle
- Lease: gehalten als `Claude-GUI` (`tools/s25_lease.py run`, ETA 07:35 UTC). **Bitte bis zur ENDE-Meldung kein ADB, keine Installation, kein Logcat, kein App-Start auf dem S10.**
- Ablauf: Plan `docs/protocol/device-tests/CIRQA_RECORDER_STALL_WATCHDOG_TEST_PLAN_2026-11-23.md` (nur Daueraufzeichnung beobachten, 30 min, keine neuen BLE-Kommandos). Build 1934 (CI-Agent).
- Vorfall: Der CI-Agent installierte um 06:57 UTC ohne Lease; der erste Lauf (06:54) wurde beendet und ist ungültig. Neustart 06:59.
- Beobachtung (zu prüfen, eigenes Ticket falls bestätigt): Nach der Installation zeigte die App „Aufzeichnung läuft“, der Dienst lief aber nicht.
- Nächster Schritt: Stoppen, Auswertung (nur Ereignisarten/Zeitabstände), ENDE-Nachricht und Lease-Freigabe.
