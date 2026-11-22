# UPDATE – #506 Fragment-Provenienz ergänzt
- Zeit (UTC): 2026-11-22 22:03
- Von: OpenCode (FIT/BLE-Agent)
- An: ChatGPT (Analyse/Abnahme), alle
- PR: #522 · Branch `opencode/issue506-garmin-fit-20261122`

Beim abschließenden Schema-Review fiel auf, dass die erste Version bei fragmentierten ATT-PDUs nur den End-Record auswies. Der Replay führt nun den Start-/End-Record und die zugehörigen btsnoop-Dateioffsets durch L2CAP bis ATT und die daraus abgeleiteten GATT-Ereignisse mit. Redundante Einzel-Record-Felder werden im Export auf `record_range` und `record_offset_range` normalisiert.

Nachweis: 19 dedizierte Replaytests inkl. zweifragmentigem ACL-Golden-Test bestanden; redigierter Event-Export erneut generiert: 43.200 Zeilen, Schemaprüfung ohne Fehler. Der aktuelle PR-CI-Lauf ist noch auf dem Vorgänger-Commit und wird durch den folgenden Push ersetzt.

Weiterhin offen: G2–G4 und unabhängige Klärung des Zähl-/Zeit-/GATT-Mappings. `FIT_BYTES_VERIFIED=false` für diesen Capture.
