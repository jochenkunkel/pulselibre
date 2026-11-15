# Garmin protocol research — offline-only phase

Source inspected: Freeyourgadget/Gadgetbridge, master branch, `GFDIMessage.java`,
`ProtobufMessage.java`, and `ProtocolBufferHandler.java` under
`app/src/main/java/nodomain/freeyourgadget/gadgetbridge/service/devices/garmin/`.

Observed in reference source:
- GFDI uses a little-endian two-byte packet length, then a two-byte message type;
  a two-byte CRC follows the payload. The reference implementation validates
  both length and CRC before dispatch.
- Message types include FIT definition (5011), FIT data (5012), protobuf request
  (5043), protobuf response (5044), and authentication negotiation (5101).
- Protobuf payloads have request ID, offset, total length and fragment length;
  fragmentation and higher-level parsing require additional work.
- Reference source handles several message families; it does not imply that
  the CIRQA advertises these frames during passive scans.

## Implementation status (updated 2026-11-15)

- `GarminFrameHeader` inspects the GFDI length and message type only.
- `GarminFrameValidator` separately validates GFDI length and CRC-16/ARC; synthetic unit tests exist.
- `FitFileHeader` now checks a complete, bounded FIT file envelope: 12/14-byte header, `.FIT` signature, little-endian data length, exact overall size (header + data + 2 trailing CRC bytes), and an 8 MiB input cap.
- **FIT header CRC and FIT file CRC are now checked by the separate `FitFileHeader.inspectVerified` entry point; record boundaries and definition structures are now inspected by `FitRecordStructure` after CRC validation; field values, timestamps and health metrics are NOT yet decoded or validated.** The FIT envelope inspector does not authenticate files or their device of origin.
- None of these parsers is connected to BLE advertisements or to a CIRQA data stream. No active GATT connection is permitted without explicit approval.

## Next protocol steps

1. Extend FIT CRC coverage with independent real-world-compatible, non-personal test fixtures and malformed inputs.
2. Extend the FIT record-structure parser with independently sourced fixtures, developer fields, local-type redefinitions and malformed-record cases.
3. Decode validated FIT field values and timestamp/unit semantics into typed, source-attributed records, using independent synthetic fixtures before connecting to local storage.
4. Investigate a safe CIRQA transport path without disrupting Garmin Connect; require separate user approval for any active interaction.

## CI verification (2026-11-15)

GitHub Actions run #143 (commit `610076fb`) completed successfully: build, unit tests, lint, and emulator instrumentation job passed. This validates the tested code paths, **not** live CIRQA transport or health-data ingestion.

## Samsung Galaxy S25 passive field test (2026-11-15)

User-installed APK from run #143 after an in-place update failed; uninstall/reinstall succeeded. Suspected debug APK signing-key mismatch remains to be verified and fixed with stable signing before future updates.

- App launched; Android nearby-device/Bluetooth permission granted.
- CIRQA present in Android bonded-device metadata; Android reports GATT `CONNECTED` (owning app unknown). PulseLibre initiated **no** connection.
- 60-second passive BLE scan completed with **10** advertising devices, **0** CIRQA bonded-address matches, **0** Garmin advertising hints, **10** unattributed advertisements.
- User manually synchronized Garmin Connect during the scan. The diagnostic snapshot does **not** establish that any advertisement belongs to CIRQA, nor that PulseLibre observed Garmin's synchronization.
- No pairing, unpairing, GATT writes, or active CIRQA session initiated by PulseLibre.

Next: improve safe passive attribution diagnostics without collecting or sharing raw BLE identifiers; preserve Garmin Connect and request explicit permission before any active CIRQA operation.
