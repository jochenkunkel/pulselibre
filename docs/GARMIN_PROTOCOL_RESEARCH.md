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
- **FIT header CRC and FIT file CRC are now checked by the separate `FitFileHeader.inspectVerified` entry point; record definitions, data records, time fields and health metrics are NOT yet decoded or validated.** The FIT envelope inspector does not authenticate files or their device of origin.
- None of these parsers is connected to BLE advertisements or to a CIRQA data stream. No active GATT connection is permitted without explicit approval.

## Next protocol steps

1. Extend FIT CRC coverage with independent real-world-compatible, non-personal test fixtures and malformed inputs.
2. Build strict definition/data record parsing, with bounded lengths and unknown-field handling.
3. Validate timestamps and units using independent, synthetic fixtures before connecting to local storage.
4. Investigate a safe CIRQA transport path without disrupting Garmin Connect; require separate user approval for any active interaction.

## CI limitation

The GitHub Actions build jobs are currently blocked **before steps run** by an account payment/spending-limit warning observed in the GitHub UI. Newly committed parser code and tests are **not yet CI-verified**. Avoid equating a successful git commit with a successful build.
