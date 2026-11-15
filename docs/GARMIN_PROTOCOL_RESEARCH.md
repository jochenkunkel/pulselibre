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

PulseLibre currently implements only an isolated GFDI *header inspector* for
synthetic byte arrays. It does NOT validate CRC, decode FIT or protobuf, connect
to CIRQA, or intercept Garmin Connect. Never feed BLE advertisements into this
inspector and infer Garmin identity from a coincidental header.

Next: inspect the reference CRC implementation and FIT parser, review upstream
licensing before reuse, and build synthetic CRC-valid fixtures with rejection
tests. Do not use real band traffic without separate user approval.
