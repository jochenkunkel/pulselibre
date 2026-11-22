# ENDE (Lauf 2) – 5-Minuten-Frist abgelaufen, Download abgebrochen

- Zeit (UTC): 2026-11-22 12:33
- Von: CI-Agent (OpenCode)
- An: Nutzer, FIT-Agent, alle
- Präzisierung des Nutzers 12:32 UTC: „Historie herunterladen" wurde bereits gedrückt, als er „Ich tippe jetzt Herunterladen" schrieb. Konservativer Start spätestens 12:27:30 UTC (frühestens 12:26:34 UTC), Fristende spätestens 12:32:30 UTC.
- Während des Laufs nur lesend: 12:27:30 UTC `MainActivity` im Vordergrund, UI-Dump 33 Byte (`could not get idle state`), kein lesbarer Statustext.
- 12:32:55 UTC: `am force-stop de.pulselibre` ausgeführt; danach kein App-Prozess, `versionCode=1806` unverändert. Kein Retry, keine Schleife.
- Garmin blieb genau wie freigegeben nur temporär gestoppt: kein Prozess, nicht in `pm list packages -d`, kein `pm disable-user`, kein Entkoppeln, kein Reset, kein `CLOSE_ALL`. Beim nächsten Start der Garmin-App ist sie wieder aktiv.
- Ergebnis: kein nachgewiesener Downloadabschluss, kein DB-Import nachgewiesen; kein innerer Zustand beim Abbruch beobachtet. Keine BLE-/GATT-/FileSync-Kommandos von mir.
- Offen: SAF-Export durch den Nutzer, dann offline redaktierter Vergleich mit der Baseline (alle 15 Tabellen 0 Datensätze). `coverage=device-history-unverified` bleibt gültig.
- Dokumentation: `docs/protocol/device-tests/CIRQA_S10_BOUNDED_DB_IMPORT_EXECUTION_2026-11-22.md`, Abschnitt „Lauf 2". Keine Rohdaten/Gerätekennungen/Gesundheitswerte in GitHub.
