# CIRQA FileSync V2: source-level comparison (2026-11-16)

## Device evidence
CIRQA reports GFDI service registration accepted, validates COBS/GFDI CRC, then responds three times with GFDI RESPONSE 5000: original type 35385 (0x8A39), status 2 (UNSUPPORTED). No FileListResponse was observed. The probe stopped after three negative replies.

## Reference code inspected
- https://github.com/mskerwiderski/GarminHRM600/blob/main/hrm600/gfdi.py
- https://github.com/mskerwiderski/GarminHRM600/blob/main/hrm600/filesync.py
- https://github.com/mskerwiderski/GarminHRM600/blob/main/hrm600/client.py
- https://github.com/mskerwiderski/GarminHRM600/blob/main/docs/gfdi-filetransfer.md

## Findings
1. GarminHRM600 sends FileListRequest as Smart field 43 containing FileSync field 9, in compact kind 0x39. PulsLibre uses the same structural encoding and a fixed sequence 10/counter 1. This does NOT prove that CIRQA accepts this command or that its bootstrap is complete.
2. GarminHRM600 maintains incrementing compact counters/sequences (client.next_compact_ids), queues device-information and core-feature acknowledgements, and sends EventSharing subscriptions during bootstrap. PulsLibre does not yet reproduce this verified device-specific handshake.
3. GarminHRM600 handles compact kinds 0x2b/0x2c and acknowledges chunks, but CIRQA has not produced any such frames in the recorded tests. Implementing or sending chunk ACKs now would be speculative.
4. Status 2 in GFDI RESPONSE 5000 is UNSUPPORTED. The reported original type is 0x8A39, the exact outgoing compact type. The rejection could concern the message type, capability negotiation, or missing bootstrap; the data cannot distinguish these hypotheses.

## Safety and next engineering steps
- Active FileListRequest disabled on CIRQA pending verification. Passive decoder remains available.
- Compare GarminHRM600 bootstrap against the actual CIRQA GFDI message sequence and distinguish required acknowledgements from device-specific watch emulation.
- Add offline protocol fixtures/tests for GFDI-5000 response, compact sequence/counter, COBS framing, and expected outbound bytes.
- Add a gated, explicit user-opt-in bootstrap diagnostic only after protocol assumptions have been verified; no FileRequest, FileSetFlags, CLOSE_ALL or device-data logging.
- Do not claim historical data access until a CRC-verified FileListResponse is observed.
