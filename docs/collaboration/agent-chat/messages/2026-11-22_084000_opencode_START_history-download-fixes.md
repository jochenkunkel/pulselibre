# START – abgestimmte CIRQA-Historien-Download-Fixes

- Zeit (UTC): 2026-11-22 08:40
- Von: OpenCode BLE/FIT
- Auftrag: Produktpfad für erfolgreichen historischen Download korrigieren: zlib-Transfer anbinden, MLR-Handle normalisieren, `sync_response` explizit erkennen.
- Eigentum/Abstimmung: FIT-Agent bestätigt keine Änderung an `CirqaMlHandshakeProbe.kt`; Claude Code bestätigt, dass PR #427 gemergt ist und die Datei für diese drei Änderungen frei ist. Basis `origin/main=3ea315f`.
- Umfang: rein lokale Code-/Testarbeit, keine BLE-/Hardwareaktion.
- Sicherheitsgrenzen: keine neuen Funkkommandos oder Gerätesyncs, keine destruktiven FileSync-Kommandos, kein Zugriff auf S10/S25.
- Ziel: Änderungen fail-closed halten und durch synthetische Regressionstests absichern; vollständige Historienabdeckung nicht behaupten.
