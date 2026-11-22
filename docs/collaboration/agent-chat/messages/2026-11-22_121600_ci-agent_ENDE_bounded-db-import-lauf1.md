# ENDE (Lauf 1) – 5-Minuten-Frist erreicht, Download abgebrochen

- Zeit (UTC): 2026-11-22 12:16
- Von: CI-Agent (OpenCode)
- An: Nutzer, FIT-Agent, alle
- Zeitachse: Startmeldung Nutzer 12:09:56 UTC; Fristende 12:14:56 UTC; `am force-stop de.pulselibre` 12:15:34 UTC. Kein Retry.
- Beobachtung: `uiautomator dump` meldete durchgehend `could not get idle state`, `MainActivity` blieb im Vordergrund. Kein lesbarer Statustext, kein nachgewiesener Abschluss.
- Ergebnis: kein DB-Import nachgewiesen; kein Downloadabschluss nachgewiesen. Keine von mir ausgeführten BLE-/GATT-/FileSync-Kommandos, kein Reset, keine Entkopplung, keine Löschung. App-Prozess danach beendet, `versionCode=1806` unverändert.
- Offen: zweiter SAF-Export zum Vergleich steht noch aus (Nutzerinteraktion). `coverage=device-history-unverified` bleibt gültig.
- **Neue Nutzeranfrage (nicht ausgeführt):** Garmin Connect „beim nächsten Lauf" zu deaktivieren. Es gibt keinen In-App-Schalter in PulsLibre (nur einen Hinweistext); Garmin Connect ist als `com.garmin.android.apps.connectmobile` installiert. Ich habe nichts deaktiviert. Ein solcher Eingriff (Force-Stop vs. `pm disable-user` vs. Entkopplung) ist riskant und braucht eine präzise, neue Freigabe. Ein „nächster Lauf" wäre wegen des Retry-Verbots des aktuellen Plans ohnehin ein neuer, separat freizugebender Test.
- Dokumentation: `docs/protocol/device-tests/CIRQA_S10_BOUNDED_DB_IMPORT_EXECUTION_2026-11-22.md`.
