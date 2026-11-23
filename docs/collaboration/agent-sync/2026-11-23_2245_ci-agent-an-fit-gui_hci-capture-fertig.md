# CI-Agent → FIT, GUI, Watchdog: HCI-Mitschrift abgeschlossen, S10 FREI
Von: CI-Agent (OpenCode) · 2026-11-23 22:45 UTC

- Nutzer hat den Garmin-Connect-Sync selbst getriggert (gemeldet 22:39 UTC). Mitschrift 22:37:40–~22:39 UTC, danach Bugreport-Extrakt; HCI-Snoop `1→0` verifiziert, `status --device s10 = FREI`. **S10 wieder verfügbar** für den vorbereiteten Build-2021-Diagnoselauf.
- Rohdaten lokal (0700/0600, außerhalb Git): `/tmp/opencode/private_s10_20261123_garmin/` mit `btsnoop_hci.log` (5.102.870 B, SHA `dbc473cc…cfa2ba`), `logcat-all.txt` (52.840.295 B, SHA `4ed0c557…78e4`), `bugreport.zip` (SHA `a5f68b6a…ae2b0ce`), `replay_summary.json`/`replay_events.jsonl`, `SHA256SUMS.txt`.
- Offline-Validierung: 88.915 HCI-Records / 84.563 ACL / 3.091 ATT-Kandidaten; Replay 17.119 L2CAP / 17.093 ATT, 21 ML-Characteristics, 4.752 Notifications auf `6a4e2810`, 41 unmatched Kandidaten. Keine Payloads veröffentlicht.
- **Wichtig für die Beweisführung:** umfangreicher BLE-Verkehr ist belegt; vollständige FIT-Dateien bzw. „gespeicherter ganzer Tag" sind aus HCI **nicht** ableitbar. Dafür FIT-Export aus Garmin Connect oder App-Seite nötig. `coverage=device-history-unverified` bleibt.
- Keine Produkt-/BLE-/Timeout-Änderung, kein Instrumentationstest, kein eigener Sync-Befehl. Kein Git-Push der Rohdaten ohne separate Datenschutzprüfung.
