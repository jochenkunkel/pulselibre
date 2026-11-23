# ENDE: #529 Stillstandstest S10 (Build 1989) – S10 frei, Übergabe an FIT
Von: GUI-Agent · 2026-11-23 17:52 UTC

Lease FREI, Daueraufzeichnung gestoppt (Zustand wie vor dem Test: Aufzeichnung aus), nichts läuft mehr auf dem Gerät. FIT-Abnahmelauf (#567) kann starten; keine automatische Wiederaufnahme der Aufzeichnung.

Ergebnisse (nur Ereignisarten/Zähler, keine Gesundheitswerte):
1. Lauf 3 (Build 1958, 16:42 UTC): Aufzeichnung endete nach ca. 40 s, Logcat `SQLiteDatabaseLockedException` (PRAGMA journal_mode, Main-Thread), `CirqaLocalLog.append` wertete es als `storage_limit`. Fix #569 (Retry), gemergt.
2. Lauf 4 (Build 1989 = main 28b49fb1, 17:23–17:52 UTC): Beim App-Start lief automatisch ein historischer Download (2 von 37 Dateien, "MLR-Konfigurationsantwort ungültig", unvollständig) und belegte das Band; erst nach manuellem Start lief die Aufzeichnung ab 17:26, stabil (493 Messwerte bis 17:29), kein DB-Lock mehr.
3. Band abgelegt ab 17:29 (verbunden, in Reichweite): Es kam weiter ca. 1 Wert/Minute (493 -> 515 bis 17:52). Kein 10-Minuten-Stillstand, daher kein `stall_detected` – korrekt keine Fehlauslösung (Kriterium 2). Kriterium 3 (echter Stillstand) bleibt ungeprüft: Ablegen erzeugt keinen Stillstand.
4. Emulator B1 (#566): Stillstandslogik inkl. Reconnect und Gegenprobe grün.
Offen: Auto-Sync beim App-Start konkurriert mit Aufzeichnung (Bandverbindung) – Produktentscheidung FIT/GUI. #529 bleibt offen (B2 passiv lang).
