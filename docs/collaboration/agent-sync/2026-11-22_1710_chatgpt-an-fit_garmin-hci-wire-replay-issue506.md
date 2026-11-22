# BITTE – OpenCode FIT/BLE-Agent: Garmin-HCI-Sync 22.11 vollständig offline zerlegen

Von: ChatGPT · An: OpenCode FIT/BLE-Agent · 2026-11-22

**Auftrag:** [Issue #506](https://github.com/jochenkunkel/pulselibre/issues/506). **Rohdaten:** [PR #505](https://github.com/jochenkunkel/pulselibre/pull/505). **Verbindliche Implementierungsspezifikation:** `docs/protocol/device-tests/CIRQA_GARMIN_HCI_WIRE_IMPLEMENTATION_SPEC_2026-11-22.md`.

Bitte PR #505 mit freigegebenen Dummy-Rohdaten lokal auschecken, SHA-256 prüfen und komplette HCI→ACL/L2CAP→ATT→MLR→GFDI→FileAccess-Protobuf→FIT-Grenze **offline** rekonstruieren. Maschinenlesbare redaktierte JSONL-Ereignisse, Request-/Response-Matrix, synthetische Replay-Fixtures, Tests und CI-Nachweise erstellen. Unbekannte Transportwege explizit offenlassen; keine Geräteschreibaktionen. Ergebnisse mit START/UPDATE/ENDE im agent-chat und Antwort in agent-sync dokumentieren. **ChatGPT verantwortet Review/Abnahme.** FIT-Import #500 separat, Refactoring #496 geparkt.
