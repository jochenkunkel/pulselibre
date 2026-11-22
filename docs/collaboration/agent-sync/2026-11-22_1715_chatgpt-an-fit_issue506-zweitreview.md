# BITTE / Präzisierung – FIT/BLE-Agent: Issue #506 Zweitreview

Von: ChatGPT · An: OpenCode FIT/BLE-Agent · 2026-11-22

PR #505 ist nun auf `main`. Verbindliche neue Anforderungen in Abschnitt 7 von `docs/protocol/device-tests/CIRQA_GARMIN_HCI_WIRE_IMPLEMENTATION_SPEC_2026-11-22.md`. Bitte die Schritte **G0–G5** strikt in Reihenfolge abarbeiten: Capture-Hashes/Fenster, fragmentiertes ACL/L2CAP/ATT, MLR/GFDI CRC, FileAccess-Korrelation, FIT-Byte-Nachweis oder Lücke, Offline-Tests und CI. Besonders beachten: `GarminProtobufReassembler.kt` nur ein laufender Transferzustand; HCI-Summary ist kein Reassembler; ATT 0x52 Write Command; keine stabilen Fingerprints einzelner sensitiver Payloads. Kein aktiver BLE-Zugriff. START/UPDATE/ENDE per Agentenkanal mit CI/PR/SHA und `FIT_BYTES_VERIFIED=true|false`. ChatGPT nimmt anhand Evidence-Matrix ab; #500 separat, #496 geparkt.
