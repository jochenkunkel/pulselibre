# ENDE | Passive S10-Garmin-Connect-Mitschrift fertig, Rohdaten lokal bereitgestellt
- Zeit (UTC): 2026-11-23 22:45 · Von: CI-Agent (OpenCode) · An: Projektleitung, FIT, GUI, Watchdog, alle
- Bezug: START `2026-11-23_2210_*`; Nutzer „sync ist fertig" 22:39 UTC

## Ablauf und Lease
- S10-Lease (`--device s10`) gehalten; Identität SM-G970F verifiziert; HCI-Snoop `0→1` und gelesen; Logcat aller Puffer ab 22:37:40 UTC.
- Erster Wrapper wurde durch Signal (Exit 143) nach ~120 s beendet; Lease wurde korrekt freigegeben, Logcat bis ~22:39 UTC aufgezeichnet (52.840.295 Bytes / 375.154 Zeilen, enthält den vom Nutzer gemeldeten Sync).
- Finalisierungs-Wrapper (vordergründig, 15 min): Bugreport 43.608.334 Bytes, HCI-Extrakt 5.102.870 Bytes, Snoop `1→0` verifiziert, danach `status --device s10 = FREI`. Keine Geräteschreibbefehle außer Snoop-Konfiguration, kein BLE-/GATT-/Sync-Kommando durch CI-Agent.

## Rohdaten (0700/0600, außerhalb Git)
Pfad: `/tmp/opencode/private_s10_20261123_garmin/`
- `btsnoop_hci.log` 5.102.870 B, SHA `dbc473cc54410f3a67cabfebafa374f5b616677e71f117e2864d7e9c97cfa2ba`
- `logcat-all.txt` 52.840.295 B, SHA `4ed0c5578d20c07305ace45005158144fb2505e95562d10e99dff017822d78e4`
- `bugreport.zip` 43.608.334 B, SHA `a5f68b6a453da7eb1555defd59b0bc4dbf16f7ed8dec2f8151fc31d17ae2b0ce`
- `replay_summary.json` SHA `dcefaa18…df95`, `replay_events.jsonl` SHA `27a2ffc7…0cd11b` (reduzierte Metadaten, keine Payloads)
- Manifest `SHA256SUMS.txt`, Zeitfenster `capture_start_utc.txt` / `capture_end_utc.txt`, `operator.log`.
Nichts davon in Git commitiert; Veröffentlichung nur nach separater Prüfung auf Adressen/Tokens/Identifikatoren.

## Offline-Befunde (redaktiert)
- `tools/btsnoop_summary.py`: 88.915 Records, 84.563 ACL, 3.091 vollständige ATT-Kandidaten, `sufficient_for_att_analysis=true`.
- Logcat (Zählung, keine Inhalte): 2.160 `onWriteCharacteristic`, 34 `GATTC_Discover`.
- `tools/garmin_hci_replay.py` (aus #522-Branch): 17.119 L2CAP-PDUs, 17.093 ATT-PDUs, 5 Connection-Epochen (6 mit ATT), 21 Garmin-ML-Characteristics, 4.752 Notifications auf `6a4e2810` in c4, 41 unmatched ML-Kandidaten, 18 ambiguity-Konflikte; `strict_ml_control_response_counts=[]`.
- **Grenze:** HCI beweist umfangreichen Garmin-Connect-BLE-Verkehr, aber **keine** vollständigen FIT-Dateien und damit keine Bestätigung, dass „den ganzen Tag" gespeichert wurde. Dafür braucht es FIT-exportierte Dateien/Zeitstempel aus Garmin Connect oder App-Seite. `coverage=device-history-unverified` bleibt.
