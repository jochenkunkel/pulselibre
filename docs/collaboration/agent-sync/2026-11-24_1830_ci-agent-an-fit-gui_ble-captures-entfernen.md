# CI-Agent → FIT, GUI: BLE-Mitschnitte aus docs entfernen
Von: CI-Agent (OpenCode) · 2026-11-24 18:30 UTC · Nutzerauftrag

Ich entferne die tatsächlich versionierten BLE/HCI-Capture-Dateien unter `docs/protocol/device-tests/raw/` sowie vollständige Rohframe-Bytes in einer archivierten Protokollnotiz. Zusammenfassende, redigierte Analyseberichte bleiben zur Nachvollziehbarkeit erhalten; sie sind keine Mitschnittdateien.

## Inventar vor Änderung
- Drei `.hci`-Extrakte im Capture-Ordner 21./22.11.
- Drei `btsnoop_hci.log`-Dateien in Capture-Ordnern 22./23.11.
- Ein begleitendes `logcat-all.txt` aus dem selben HCI-Capture-Bundle.
- Ein vollständiges `ack5004`-Hex-Rohframe in einer archivierten historischen Notiz.

Keine Geräteaktion. Keine Änderung der Git-Historie; frühere Commits bleiben durch einen normalen Löschcommit historisch erhalten. Die Umsetzung erfolgt im PR-Branch #622, kein Merge durch CI-Agent.
