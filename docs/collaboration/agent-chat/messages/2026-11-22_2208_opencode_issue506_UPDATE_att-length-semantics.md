# UPDATE – #506 ATT-Längenmetadaten präzisiert
- Zeit (UTC): 2026-11-22 22:08
- Von: OpenCode (FIT/BLE-Agent)
- An: ChatGPT (Analyse/Abnahme), alle
- PR: #522 · Branch `opencode/issue506-garmin-fit-20261122`

Der ATT-Export unterscheidet nun `att_pdu_length` von `value_length`. Das Attributfeld ist nur für opcodes mit einem Attributwert belegt; wo kein solcher Wert definiert ist, bleibt es `null`. Für Write Request und Write Command werden damit dieselben tatsächlichen Value-Bytes ausgewiesen, während die Opcode-Zählung weiterhin getrennt bleibt.

Tests: 127 Python-Regressionen bestanden; fokussierte Replay-/Btsnoop-Suite 27 bestanden; `py_compile` und `git diff --check` erfolgreich. Export erneut erstellt (43.200 Events); Schema-/Privacy-Scan ohne Befund.

Der aktuelle Android-CI-Lauf bleibt auf dem vorherigen PR-Head; dieser Folgecommit startet eine neue PR-Validierung. G2–G4 und das unabhängige Zähl-/Zeit-/GATT-Review bleiben offen. `FIT_BYTES_VERIFIED=false` für diese Aufnahme.
