# START – Drei S10-HCI-Auswertungsdateien (21.11.-Sync) in Git bereitstellen

- Zeit (UTC): 2026-11-22 22:15:00
- Von: OpenCode
- An: alle, besonders FIT-/Protokoll-Agent
- Issue/Task: Datenbereitstellung (kein neues Ticket), Ergänzung zu PR #523 und PR #505
- Status: in Arbeit (neuer Branch `docs/s10-pure-sync-hci-fixtures-20261122` von `origin/main` `091d9f76`)
- Freigabe des Nutzers (2026-11-22 22:14 UTC): „gerne bitte auch ins git" für die drei im Chat genannten `.hci`-Dateien. Ergänzend gilt `docs/collaboration/CURRENT_BAND_RAW_DATA_POLICY.md` (Herkunft, Zählprüfung, Manifest, gezielte Ausnahme). Keine Geräteaktion, keine neue BLE-Kommandos.
- Quellen: `/tmp/opencode/private_s10_20261121/s10-sync-20261121-225619/` – `s10-garmin-sync-20261122.hci` (833.857 Bytes), `s10-garmin-sync-delta.hci` (120.566 Bytes), `s10-garmin-sync-window.hci` (48.146 Bytes); alle BTSnoop v1, Datalink 1002; extrahiert aus dem Bugreport vom 21.11. 23:58 UTC (`bugreport.zip`, Sync begonnen 22:56). Keine neuen Aufnahmen vom 22.11. – die Extrakte wurden lediglich um 00:00 UTC angelegt.
- Geplante Dateieingriffe: neuer Ordner `docs/protocol/device-tests/raw/2026-11-21-s10-pure-garmin-sync/` mit den drei `.hci`-Dateien und einem `README.md` (Freigabe, Herkunft, SHA-256, zählende Prüfung, Validator-Ergebnisse, Grenzen, Verweis auf `CIRQA_S10_GARMIN_PURE_SYNC_HCI_2026-11-22.md`). Bestehende Fixtures (#505, #523) bleiben unverändert.
- Abstimmung: Kein anderer Agent bearbeitet diesen Datensatz. `*.hci` ist nicht von der `.gitignore`-Regel `*.log` betroffen; die Dateien werden bewusst und einzeln versioniert.
- Nächster Schritt / Besitzer: Prüfung und README, PR, CI, Merge nach Freigabe (OpenCode).
