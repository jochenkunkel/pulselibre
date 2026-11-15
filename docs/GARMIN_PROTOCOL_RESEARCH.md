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


## Samsung Galaxy S25 field test — CI #150 (2026-11-15)

User-provided passive BLE diagnostics from installed #150 APK:

- Scan completed; **15 unique advertisers**, **102 advertisement observations**.
- 6 devices with visible names, 10 with manufacturer-data fields, 6 with service UUID fields (these sets may overlap).
- CIRQA Android bond present; Android reports GATT connected (owning app unknown), one manual status observation of CONNECTED.
- **0 bonded-address matches**, **0 Garmin hints**, **15 unclassified advertisers**. No reliable CIRQA identification.
- Prior scan reported 10 advertisers; the increase to 15 does not establish improved CIRQA identification, as radio conditions may differ.
- No BLE connection, pairing or writes initiated by PulseLibre. No conclusion about Garmin Connect synchronization can be drawn from these aggregate counters.

Next safe investigation: evaluate whether privacy-preserving, locally computed advertisement feature combinations and signal-strength trends can narrow candidate groups without labeling any device as CIRQA. Avoid exporting raw addresses, payloads or device names; no active GATT access without explicit approval.


### Follow-up: distance-correlated BLE candidate (user observation, 2026-11-15)

The user visually observed a single unnamed BLE advertiser with service UUID `3e1d50cd-7e3e-427d-8e1c-b78aa87fe624`, no manufacturer ID, and displayed RSSI around -66/-68 dBm in screenshots. When moving the CIRQA band farther away, the user's observed signal weakened from approximately -50 to -70 dBm; on bringing the band closer, it strengthened again. The user confirmed this direction of change and said no other visible device behaved similarly.

**Interpretation:** This is a promising *candidate* based on correlated movement, not a verified CIRQA identity, not evidence that the UUID is Garmin-specific, and not proof of a particular GATT service. RSSI can fluctuate due to antenna orientation, body attenuation and multipath. Continue passive observation only; do not connect or subscribe to the device without explicit user authorization. Do not hard-code the UUID as confirmed CIRQA.


### Passive RSSI field test, 2026-11-15 (development build #157)

User-provided shared diagnostics after a 60-second passive BLE scan:
- 10 unique advertisers, 84 advertisements; 3 advertisers with names, 7 with manufacturer data, 3 with service UUIDs.
- CIRQA bonded; Android reports GATT connected (owning app unknown). No address match and no Garmin manufacturer hints.
- Candidate service UUID `3e1d50cd-7e3e-427d-8e1c-b78aa87fe624` seen on 1 advertising address.
- 22 RSSI samples: minimum -84 dBm, maximum -68 dBm, median -76 dBm.
- RSSI buckets: >= -60 dBm: 0; -75..-61 dBm: 10; < -75 dBm: 12.
- Chronological sample-window medians (10, 10, 2 samples): -77, -74, -71 dBm. The last window has only two samples and is less reliable.

**Interpretation:** The new passive tracker and privacy-preserving diagnostic export work in the field. The signal appears to strengthen over the recorded samples, but the log has no time-stamped band-distance annotations, so this scan alone cannot demonstrate distance correlation. The earlier user-observed movement correlation remains separate supporting evidence. No active connection, pairing or writes were performed by PulseLibre. The UUID remains an *unconfirmed* CIRQA candidate.


### Open-source Garmin BLE implementations to study before more RSSI field tests (2026-11-15)

User requested prioritizing existing implementations rather than repeating distance/RSSI tests. External references:
- Gadgetbridge Garmin protocol documentation: https://freeyourgadget.codeberg.page/gadgetbridge_org/internals/specifics/garmin-protocol/ . Describes several Garmin BLE protocol generations, the Multi-Link service `6a4e2800-667b-11e3-949a-0800200c9a66`, registration, characteristic pairs, and Garmin data channels. Device/version applicability must be checked; this is not CIRQA confirmation.
- Gadgetbridge Garmin devices: https://gadgetbridge.org/basics/topics/garmin/ and https://gadgetbridge.org/gadgets/wearables/garmin/ . Existing Android implementation and compatibility details.
- Python Garmin BLE protocol library: https://github.com/gwerneckp/garmin-ble . Reports BLE handshake and live telemetry support; examine code and test coverage before adapting protocol concepts.
- Linux Garmin bridge: https://github.com/wh1le/garmin-bridge . Fenix-focused BLE implementation, explicitly acknowledges Gadgetbridge protocol work.

**Research-first plan:** (1) Inspect existing transport/handshake code and device discovery strategies; (2) compare documented Garmin service UUIDs with our passive observations (candidate UUID `3e1d50cd-7e3e-427d-8e1c-b78aa87fe624` is not the documented Multi-Link service); (3) build offline protocol fixtures/unit tests from public, non-sensitive examples; (4) design an opt-in CIRQA connection path only after explicit user approval. Do not initiate pairing, GATT connections, reads, writes or subscriptions during research. Check license obligations (Gadgetbridge and derived projects use AGPL) before copying code; prefer independent implementation of documented protocol concepts.
