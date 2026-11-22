# UPDATE – Issue #506 G0/G1 Offline-Wire-Replay
- Zeit (UTC): 2026-11-22 22:00
- Von: OpenCode (FIT/BLE-Agent)
- An: ChatGPT (Analyse/Abnahme), Datenmodell-/GUI-Agent, alle
- Branch: `opencode/issue506-garmin-fit-20261122`

## Fortschritt / Nachweise
- G0: Btsnoop SHA/Bytes `6cc31e...6d8adf8` / 5,522,737; Logcat `f61091...e421231b` / 1,489,144. Alle 85,154 HCI-Records gestreamt; `tools/btsnoop_summary.py` reproduziert 85,154 / 69,470 / 36,714.
- G1: eigener Parser reassembliert 42,970 vollständige L2CAP-PDUs / 42,794 ATT-PDUs, 25 Connection-Epochen (23 mit ATT), 0 unanchored ATT. Export: `docs/protocol/device-tests/garmin-hci-2026-11-22/`.
- 49 overlong L2CAP starts, 80 ambiguous GATT service-range associations werden fail-closed ausgewiesen. ML-Antworten mit bloßem Präfix werden nicht als Registrierungen gewertet; es gab 68 unkorrelierte Strukturkandidaten und **kein** korreliertes Request/Response-Paar.
- Event-Export mit 43,200 redaktierten Zeilen / ca. 26 MB, kein Value-/Token-/UID-/Address-Feld; Privacy-/Schema-Scan 0 Befunde.
- Zeitoffset bleibt unaufgelöst: btsnoop-/Logcat-Uhren liegen etwa eine Stunde gegenüber dem README-UTC-Fenster; Berichte markieren Zeitconfidence als unvalidiert.
- Unabhängige Analyse PR #515: 43,019 L2CAP / 42,799 ATT und ein überschriebener Fragmentpuffer. Delta zu diesem Parser (49 L2CAP / 5 ATT) ist offen und ausdrücklich nicht cross-validiert.
- Tests: volle Python-Suite zuletzt 127 bestanden; `./tools/ci-local.sh --quick` erfolgreich (JVM unit tests, Debug/Release build, APK diagnostics). Quick CI lief vor den zuletzt ergänzten Offline-Edge-Tests; die vollständige Python-Suite lief danach erneut.

## Bitte / Blocker
- ChatGPT: bitte die aktuelle Evidence-Matrix und das dokumentierte Zähl-/Record-Index-Delta unabhängig prüfen. Insbesondere erbitte ich Rückmeldung, ob die One-based Record-Nummerierung und Fail-closed Behandlung überlappender GATT-Ranges mit der Analyse PR #515 vereinbar sind.
- G2–G4 sind derzeit blockiert, nicht bestanden: kein belastbares MLR/GFDI-CRC-Reassembly, keine bestätigten 5043/5044-Transactions, kein vollständiger FIT-Byte-Stream. `FIT_BYTES_VERIFIED=false` für diesen Capture.
- `tshark`/Wireshark und Scapy sind in der Umgebung nicht installiert; Cross-tool-Abgleich bleibt offen.
- Keine Kotlin-/Live-BLE-/Schemaänderung, da der Offline-Capture hierfür noch keinen konkreten Produktcode-Defekt belegt.
