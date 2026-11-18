# Garmin protocol research — offline-only phase

> **Historischer Standhinweis (18.11.2026):** Dieser Bericht dokumentiert ausdrücklich die Offline-/frühe BLE-Phase vom 15.11. und ist kein aktueller Implementierungsstatus. Inzwischen existieren der begrenzte FIT-Felddecoder `FitHistoricalObservations`, bestätigte CIRQA-Bootstrap-/Directory-Versuche und ein vorbereiteter Classic-RFCOMM-Testpfad. Siehe [aktuellen kanonischen Forschungsstand](CIRQA_HISTORICAL_RESEARCH_STATUS_2026-11-18.md). Die ursprünglichen Beobachtungen bleiben unverändert.

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


### Source-code audit and offline implementation (2026-11-15)

**Inspected source, not just READMEs:**
- `gwerneckp/garmin-ble/garmin_ble/transport/ble.py`: BLE scan first filters Garmin-looking advertised names (or explicit address/pattern); after connecting, it probes characteristic UUID pairs `6A4E2810..2814` (RX) with corresponding `6A4E2820..2824` (TX), then starts notifications. This is **not** passive discovery of the CIRQA and cannot be used to identify the advertising UUID `3e1d50cd-7e3e-427d-8e1c-b78aa87fe624`.
- `gwerneckp/garmin-ble/garmin_ble/protocol/mlr.py` and `constants.py`: runtime ML service registration responses (control type 1, 14-byte frame), handle-based multiplexing, and CLOSE_ALL (type 5). CLOSE_ALL can discard existing handle assignments and must **not** be sent while Garmin Connect may own a session.
- `wh1le/garmin-bridge/src/protocol/handshake.py`: GFDI device-information, configuration, auth negotiation and time-response handlers; these are watch-specific session messages, not proof of CIRQA compatibility. Some handlers enable settings, so they are inappropriate for a passive experiment.
- Gadgetbridge protocol documentation: https://gadgetbridge.org/internals/specifics/garmin-protocol/ ; ML service UUID `6a4e2800-667b-11e3-949a-0800200c9a66`, dynamic handles and multiple ML/MLR variants.

**Implemented offline in PulseLibre:** `GarminMlOffline.kt` and `GarminMlOfflineTest.kt`. Pure Kotlin logic identifies only complete documented RX/TX characteristic pairs from supplied UUID strings, decodes strictly sized ML registration responses, and extracts a routing handle. Negative tests reject the observed advertising UUID as a GATT characteristic pair, mismatched pairs, malformed packets and control messages. This is offline protocol research, not a CIRQA connector; no Bluetooth calls, pairing, GATT access, writes, subscriptions or CLOSE_ALL requests were added.

**Limitations / follow-up:** No CIRQA GATT discovery or handshake captures exist. A connected Garmin device may use a different protocol generation or proprietary UUIDs. No actual CIRQA compatibility or Garmin Connect coexistence has been established. Existing projects are AGPL-3.0; source was inspected for protocol behavior, and the small Kotlin inspection module was written independently. Verify license obligations before incorporating source-derived implementations. CI unit-test outcome must be checked separately.


### User authorization update: controlled active Garmin BLE work (2026-11-15)

The user explicitly authorizes implementing and testing active CIRQA BLE/GATT connections, Garmin Multi-Link protocol exchanges and the CLOSE_ALL command. The user is willing to close Garmin Connect before real connection tests; replacing Garmin Connect is the long-term goal. Previous restriction against any active CIRQA interaction is superseded **for controlled tests**. Do not assume Garmin Connect has already been closed; ask for confirmation before a real on-device session if interference is possible. Keep existing Android pairing and stored health data intact; no unpairing, factory resets, or destructive operations. CLOSE_ALL clears logical ML channel assignments and does not itself terminate the Android GATT link or stop the Garmin Connect app. Prefer offline tests and explicit, user-triggered actions; do not auto-connect or auto-CLOSE_ALL during passive scanning or app startup. Record any new active actions clearly in the UI and diagnostics.


### Active connection milestone: user-confirmed GATT discovery (2026-11-15)

Implemented `CirqaGattProbe.kt` and an explicit opt-in button on the Devices tab. This is **not yet a Garmin handshake or CLOSE_ALL implementation**. The UI asks the user to close Garmin Connect and confirms before calling Android `connectGatt(..., TRANSPORT_LE)` on the previously bonded CIRQA. The probe performs `discoverServices()`, counts GATT services/characteristics, checks the documented Garmin ML service UUID and a complete RX/TX characteristic pair, then disconnects and closes. A 15-second timeout and activity-destruction cleanup are included. No characteristic read/write, notification subscription, bond change, or factory reset. Test results appear locally; no device addresses or raw payloads are exported. Real CIRQA behavior and Android coexistence are unverified until the user tests the APK.

Next: confirm CI and real device GATT discovery. Only then consider an opt-in CLOSE_ALL and ML registration state machine with offline tests and robust failure handling.


### Real-device confirmation: CIRQA GATT discovery (user screenshot, 2026-11-15)

User installed and ran the opt-in GATT probe on the Samsung Galaxy S25. Screenshot shows: `GATT-Dienstsuche abgeschlossen (nur Metadaten)`, **5 services**, **15 characteristics**, `Garmin ML Service: gefunden`, `Garmin RX/TX-Paar: gefunden`, and no GATT read/write commands or subscriptions. This establishes that the Android-bonded device named CIRQA exposes the known Garmin ML service and a characteristic pair. It does **not** establish successful ML registration, CLOSE_ALL, GFDI handshake, or health data access. Next step: offline-tested, user-triggered ML registration with explicit result logging and safe cleanup. Do not infer characteristic permissions or exact RX/TX UUID from the screenshot alone.


### Garmin ML handshake prototype (2026-11-15)

Added `GarminMlHandshake.kt` pure Kotlin control packet builder and state machine; `GarminMlHandshakeTest.kt` offline tests; `CirqaMlHandshakeProbe.kt` opt-in Android BLE connector; and a second device-tab action with explicit user choice to run without or with CLOSE_ALL. It connects only to Android-bonded CIRQA, discovers Garmin ML service/characteristics, enables RX notifications via CCCD, sends a GFDI (service 1) REGISTER_ML request, awaits a validated registration response, and disconnects. Optional CLOSE_ALL waits for a type-6 acknowledgement before registration. Includes 20-second timeout, error handling and cleanup. No GFDI application payloads, file sync, or health reads. This is a protocol compatibility test, **not a completed Garmin Connect replacement**. Do not claim it works on real CIRQA until confirmed by a user device test; inspect CI build results before APK installation. Source references: `gwerneckp/garmin-ble/garmin_ble/protocol/mlr.py`, `constants.py` and `transport/ble.py` (reviewed; Kotlin implementation authored separately).


### Real-device handshake timeout, both modes (2026-11-15)

User reports Garmin Connect is disabled and cannot restart. On Galaxy S25, both explicit ML handshake attempts (with and without CLOSE_ALL) show `ML-Handshake: Zeitlimit (20 Sekunden)`. Earlier GATT-only discovery had succeeded (5 services, 15 characteristics, Garmin ML service and RX/TX pair). A timeout does not prove the device rejected the registration: the current UI did not reveal whether GATT connected, services were discovered, CCCD was written, the command was queued or a notification was received. Reviewed external `garmin_ble/protocol/mlr.py`, `constants.py` and `transport/ble.py`: service 1, client ID 2, registration request and optional CLOSE_ALL framing appear consistent with those sources, but this is not a CIRQA wire-level validation. Implemented a diagnostic improvement in `CirqaMlHandshakeProbe.kt`: timeouts now report last BLE phase, notification count, write-callback status and ML session stage, without exporting addresses or payloads. Do not ask the user to repeat tests until this build passes CI. Next investigate stage-specific results, and correct the root cause before any further protocol changes.


### Real-device ML notifications received but no accepted registration (2026-11-15)

User screenshot from build #174, with Garmin Connect disabled and handshake **without CLOSE_ALL**, reports: `ML-Handshake: Zeitlimit (20 Sekunden). Letzte Phase: ML-Notification empfangen; Protokollantwort wird geprüft; Notifications: 5; GATT-Schreiben bestätigt: true; Protokollzustand: WAIT_REGISTER.` This proves the GATT write callback succeeded and 5 notifications reached PulseLibre; it does **not** prove they were registration responses or even application responses to the write. The next build adds bounded, privacy-preserving notification-shape diagnostics: length, whether control-channel byte is zero, control message type, and decoded registration service/status/handle when a strict parser matches. It does not export raw notification payloads, MAC addresses or health data. No new protocol writes or CLOSE_ALL behavior. Investigate whether notification framing or service response differs before changing handshake commands.


### Source-first implementation roadmap: reuse established Garmin protocol knowledge (2026-11-15)

**Decision / user request:** Stop reverse-engineering Garmin protocol fields by trial and error when source implementations already exist. Use real upstream source code as the primary reference, implement compatible, independently written Kotlin modules in PulseLibre, and keep the plan and evidence in Git. Distinguish upstream behavior, our assumptions, and real CIRQA evidence. Do not claim any health metric is available before device validation.

**Primary source files actually inspected:**
1. Gadgetbridge Android Garmin V2 communicator: https://github.com/Freeyourgadget/Gadgetbridge/blob/master/app/src/main/java/nodomain/freeyourgadget/gadgetbridge/service/devices/garmin/communicator/v2/CommunicatorV2.java . Uses Garmin ML UUID `6A4E2800-667B-11E3-949A-0800200C9A66`, scans RX `2810..2814` paired with TX `2820..2824`, enables notifications, sends CLOSE_ALL, registers services with client ID 2, tracks assigned handles, handles COBS-framed GFDI, and decodes realtime metrics. Crucial: its `REGISTER_ML_RESP` parser reads **an additional trailing reliable byte** after the handle.
2. garmin-ble Python MLR: https://github.com/gwerneckp/garmin-ble/blob/main/garmin_ble/protocol/mlr.py ; constants: https://github.com/gwerneckp/garmin-ble/blob/main/garmin_ble/constants.py ; transport: https://github.com/gwerneckp/garmin-ble/blob/main/garmin_ble/transport/ble.py . Provides ML framing, registration and close-all builders, handle routing, GFDI service code 1, realtime service IDs, BLE discovery, and offline simulation/tests. Its response parser accepts frames **at least 14 bytes**, and parses the 14-byte core without relying on the trailing byte. Its `encode_rx` also supports flagged handle encoding, whereas Gadgetbridge's V2 communicator compares a bare handle byte; investigate variants, do not assume one applies to CIRQA.
3. garmin-ble GFDI: https://github.com/gwerneckp/garmin-ble/blob/main/garmin_ble/protocol/gfdi.py . Defines little-endian GFDI packet framing (length, message type, payload, CRC16), device information (message 5024), Protobuf frames and time/system-event messages.
4. garmin-ble metric modules: https://github.com/gwerneckp/garmin-ble/tree/main/garmin_ble/metrics . Separate decoders for heart rate, steps, calories, stress, HRV, SpO2, respiration, body battery, intensity and accelerometer; for example `heart_rate.py` documents service 6 and payload `[padding, hr, resting_hr]` with unknown resting HR represented as absent, not zero.
5. garmin-bridge V2 transport: https://github.com/wh1le/garmin-bridge/blob/main/src/protocol/transport.py ; handshake: https://github.com/wh1le/garmin-bridge/blob/main/src/protocol/handshake.py . Registers GFDI over ML, routes notifications, uses COBS, and responds to device-information, configuration, authentication negotiation and time requests. Some handlers alter device settings; do not copy their behavior into passive or diagnostic code.
6. Gadgetbridge FIT pipeline: https://github.com/Freeyourgadget/Gadgetbridge/tree/master/app/src/main/java/nodomain/freeyourgadget/gadgetbridge/service/devices/garmin/fit . FIT definitions, record decoding, importer and file-transfer integration. Study this for historical data, rather than interpreting real-time packets as full history.

**Observed CIRQA evidence vs hypotheses:** Android-bonded CIRQA GATT discovery succeeded (5 services, 15 characteristics, Garmin ML service and RX/TX pair). In build #174, Garmin Connect disabled, handshake without CLOSE_ALL timed out after 20s in `WAIT_REGISTER` **despite GATT write confirmation and five incoming notifications**. Their content/identity has not been established. The strict old PulseLibre parser required exactly 14 bytes; Gadgetbridge accepts a 15-byte response with `reliable`. This is a plausible parser incompatibility, **not a proven root cause**. The revised offline parser accepts 14- or 15-byte responses and validates expected client ID 2; offline fixtures were updated. Check CI before asking for another on-device test. Note that Gadgetbridge's response parser also validates client ID, and a GATT write confirmation is not a protocol-level acceptance.

**Ordered implementation backlog (commit code, tests and docs together):**
- **P0 — finish and validate ML registration:** Check latest Android CI. Compare byte-exact request and response layouts with upstream tests. Keep bounded diagnostics of packet lengths, control/data classification, service ID, status and handle without raw payloads. Ensure responses are correlated to client ID and requested service. Use single opt-in CIRQA handshake, initially without CLOSE_ALL, only after green build. Distinguish malformed, refused, timeout and success in UI. Keep Bluetooth connection cleanup and timeouts robust.
- **P1 — offline ML transport:** Extract pure Kotlin ML control/data routing and dynamic service-to-handle mapping; support observed bare/flagged variants without guessing; validate fragmentation and safe bounded reassembly. Add upstream-inspired independent fixture tests, including wrong client, wrong service, malformed/truncated packet, 14/15-byte responses and multiple notifications.
- **P2 — GFDI:** Implement/test COBS streaming decoder, little-endian length/type parsing, CRC16 validation, device-information/config/auth/time message handling. Treat all device requests as opt-in, with no settings writes until understood and authorized. Do not enable network/cloud services.
- **P3 — realtime measurements:** Register only supported ML realtime services explicitly, then decode each service into typed local values. Reference service IDs: HR 6, steps 7, calories 8, intensity 10, HRV 12, stress 13, accelerometer 16, SpO2 19, body battery 20, respiration 21. Cross-check per-metric payload layouts against Gadgetbridge and garmin-ble; add unit tests for invalid/sentinel values, endianness, bounds and absent readings. Only display genuine CIRQA values with timestamps and provenance, never fabricated measurements.
- **P4 — historical measurements:** Study Garmin file-transfer flow and FIT decoding/import. Map real records (sleep, HRV, workouts, monitoring etc.) to PulseLibre's local storage with validation, deduplication, timezone handling and retention rules; add offline fixtures and regression tests.
- **P5 — productization:** Implement user-controlled connect/disconnect, error recovery, background BLE lifecycle within Android rules, device capability detection, local persistence, privacy-preserving diagnostics, and stable APK signing/upgrades. No Garmin Connect dependency in the intended final experience.

**Engineering and licensing guardrails:** Reference upstream code and behavior but do not copy AGPL implementation text into PulseLibre without explicit licensing review. Verify each upstream repository's license and compatibility before porting. Tests should run offline without a band; hardware tests are separate and user-triggered. Keep existing Android bond and local data intact, never unpair or factory reset. Do not auto-run CLOSE_ALL or repeatedly reconnect. Continue documenting CI run numbers, outcomes and device observations in Git. **Current roadmap is a plan, not a claim that P1–P5 are implemented.**


### Real-device test of build #181: GATT status 133 (2026-11-15)

User screenshot from the opt-in CIRQA Garmin-ML handshake screen after successful CI build #181: `ML-Verbindung beendet (GATT 133, Zustand 0).` The screen also shows zero devices from the separate passive BLE scan; that passive result is **not** proof that the previously bonded CIRQA is missing or that the GATT probe used scan results. Android GATT 133 is a generic Bluetooth stack/connection error, state 0 means disconnected. **Do not infer** that the ML registration parser was exercised, that the CIRQA rejected registration, or that a particular BLE phase failed: the error path omitted phase, notification and write-callback context. Earlier build #174 did connect, confirm a write and receive five notifications, so the present failure is not a demonstrated permanent inability to connect.

**Implemented next diagnostic change:** include last BLE phase, notification count, GATT write confirmation, ML state and bounded notification-shape summaries on any GATT disconnect/error, not only on the 20-second timeout. This is a diagnostics-only change, no extra BLE writes, no automatic retries, no CLOSE_ALL changes, no raw packet/address/health data logging. Wait for green CI before another opt-in test; ask user for one screenshot. If failure occurs before connecting, investigate Android BLE state and competing connections; if after notifications, inspect control frame shapes and handle registration. Do not recommend unpairing, factory reset or deleting health data.


### CIRQA ML packet-shape breakthrough: 16-byte registration-type frame (2026-11-15)

Second user screenshot after testing (version not independently verified): `ML-Handshake: Zeitlimit (20 Sekunden). Letzte Phase: ML-Notification empfangen; Protokollantwort wird geprüft; Notifications: 5; GATT-Schreiben bestätigt: true; Protokollzustand: WAIT_REGISTER; Antwortformen: L=16, control=true, type=1, reg=nein | L=58, control=false, type=-, reg=nein` repeated four times. **Direct observation:** one 16-byte control-channel message with type 1 and four 58-byte non-control messages; GATT write callback success; no accepted registration under the previous exact-length parser. This does not establish the service code, client ID, status, assigned handle, or the content of data frames.

**Source-backed correction:** Gadgetbridge V2 reads a 14-byte fixed control core plus a `reliable` field; garmin-ble's ML parser accepts frames >=14 bytes. PulseLibre previously rejected all frames longer than 15 bytes. The parser now accepts bounded 14..64-byte type-1 control frames, checks client ID 2, then decodes service/status/handle at documented offsets. Added a 16-byte positive offline fixture and an oversized-frame rejection test. **Hypothesis, not yet proven:** the 16-byte frame may be a valid GFDI registration response with additional trailing extension bytes. If still not accepted, next diagnostic must distinguish wrong client, service mismatch, nonzero status, or unexpected layout, using only non-sensitive header fields; do not blindly accept control type 1 or treat 58-byte data as health values. Test with one opt-in attempt after green CI; no auto-retry, unpairing, reset, or unsolicited CLOSE_ALL.


### Real CIRQA GFDI registration success, build #186 (2026-11-15)

User-provided first screenshot after installing successful build #186 shows exactly: `Garmin ML: GFDI-Service erfolgreich registriert! Ohne CLOSE_ALL. Verbindung beendet; keine Gesundheitsdaten gelesen.` This is the **first on-device confirmed successful Garmin ML GFDI service registration** from PulseLibre against the Android-bonded CIRQA. The app's strict parser accepted a type-1 response after support for 16-byte control frames was added; success requires expected client ID 2, GFDI service 1, status 0 and a valid nonzero handle. Earlier field diagnostics saw a 16-byte control type 1 frame, making the parser-length issue the strongly supported cause of the former WAIT_REGISTER timeouts. No CLOSE_ALL was used. The one-shot diagnostic deliberately disconnected after registration and read no health data.

**Next steps:** Do not repeat handshake tests unnecessarily. Keep the now-proven registration implementation stable with regression tests for 16-byte responses. Implement a bounded, explicit opt-in GFDI session to inspect message framing and the initial device-information exchange using existing Gadgetbridge/garmin-ble references. Preserve BLE handle routing, COBS reassembly and CRC checks, with offline fixtures before live data access. Then add realtime HR/steps as independently registered services and local-only data capture; never fabricate health measurements. Review source licenses before copying code. The GFDI registration milestone does not yet demonstrate successful GFDI application-level communication, metric subscriptions, historical FIT transfers, or long-lived background sync.


### Source-backed realtime metrics foundation (2026-11-15)

After the successful CIRQA GFDI registration, reviewed actual upstream source in `gwerneckp/garmin-ble/garmin_ble/protocol/mlr.py`, `garmin_ble/metrics/heart_rate.py`, `garmin_ble/metrics/steps.py`, `garmin_ble/protocol/gfdi.py`, and Gadgetbridge's `CommunicatorV2.java`. Confirmed that Garmin ML routes messages through **runtime-assigned handles**, not fixed handles per metric. HR service code is 6, steps is 7. HR service payload has three bytes `[type/padding, bpm, resting_bpm]`; steps is two unsigned little-endian 32-bit integers `[steps, goal]`. MLR inbound routing may use a flagged handle (bits 6:4) or a bare handle; the first routing byte is not part of metric payload. A zero resting HR means unknown; a missing or zero current HR must not be presented as a real measurement.

**Implemented and committed:** `GarminRealtime.kt` pure Kotlin offline routing and service-specific HR/steps decoders with explicit dynamic handle registry. `GarminRealtimeTest.kt` tests malformed input, both routing formats, signed/unsigned conversions, absent readings, registration requirement and clearing mappings. No Bluetooth operations or app UI integration in this commit; **the device has not yet yielded verified HR/steps readings to PulseLibre**. This module is groundwork, not a live health data reader.

**Next implementation gate:** Confirm CI for new Kotlin module. Then implement a separate opt-in on-device test that registers HR and steps service handles after GFDI, waits for bounded notifications and displays only actual parsed measurements, disconnecting cleanly. Do not assume notifications arrive spontaneously; check upstream subscription/activation behavior. Separately build offline COBS/GFDI framing and CRC validation before attempting device-info, historical FIT transfers or application-level message writes. Avoid copying AGPL source text without licensing review. Maintain offline/no-cloud design.


### Opt-in CIRQA realtime HR/steps BLE integration (2026-11-15)

Implemented live user-triggered diagnostic in `CirqaMlHandshakeProbe.kt` and a new Devices-tab button `CIRQA: Puls und Schritte live testen`. On confirmation and with Garmin Connect closed, the one-shot session connects to the bonded CIRQA, enables ML notifications, registers GFDI (service 1), then sequentially registers realtime heart rate (service 6) and steps (service 7). The existing validated ML registration response parser supplies dynamic handles; the independently implemented `GarminRealtime.Registry` routes inbound notifications to the correct service. Only parsed live readings are shown; no fabricated values, no local persistence, no cloud, no CLOSE_ALL. The test ends when both metrics arrive or after 20 seconds; it closes GATT and reports which measurements were actually received. No continuous monitoring or historical FIT transfer yet.

**Caveats and validation gate:** Registration alone does not guarantee the CIRQA emits these realtime services. If the band declines registration or sends no data, the UI reports that result rather than inventing measurements. Existing Android callbacks and BLE concurrency need CI and actual-device validation; do not claim a live reading until screenshot confirmation. This is a controlled test, not a finished production-grade background connector. The next development step is an offline-tested GFDI COBS/CRC decoder and bounded session management.


### First real CIRQA HR + steps success, build #193 (2026-11-15)

User-provided screenshot from the Devices tab after selecting **CIRQA: Puls und Schritte live testen** shows the app's exact output:

`CIRQA Echtzeit-Test: Beide Messwerte empfangen. Puls: 87; Ruhepuls: 66; Schritte: 10244; Ziel: 8830; gültige Messpakete: 2; Phase: ML-Notification empfangen; Protokollantwort wird geprüft. Keine Daten gespeichert.`

**Device-verified milestone:** Live BLE registration of GFDI, realtime HR (service 6) and realtime steps (service 7), dynamic ML handle routing and service-specific payload decoding produced one valid HR and one valid steps packet on the actual Garmin CIRQA. These are actual observed values, not synthetic data. Build #193 was successful in GitHub Actions. The screenshot establishes successful one-shot reading, **not** continuous monitoring, persistence, historical FIT sync, sensor accuracy, or repeated reliability. Do not overwrite these values with invented readings. Current test explicitly does not persist health data.

**Next:** integrate typed live readings into the Overview with local-only persistence and timestamps, explicit user-controlled reading sessions, deduplication and lifecycle-safe disconnect. Then implement GFDI/COBS/CRC and FIT history using source-reviewed implementations and offline fixture tests. Keep privacy guarantees and avoid requiring Garmin Connect or cloud.


### Expanded realtime service probing (2026-11-15)

Reviewed exact upstream `gwerneckp/garmin-ble/garmin_ble/metrics/{spo2,hrv,stress,body_battery,respiration,calories,intensity,accelerometer}.py` source. Implemented independent pure Kotlin decoders for SpO2 (service 19, 255 means unavailable), RR interval HRV (12), stress (13, negative means unavailable), Body Battery (20), respiration (21), total/active calories (8), moderate/vigorous intensity minutes (10), in addition to confirmed HR (6) and steps (7). Added offline tests. The accelerometer is a high-volume packed stream and is intentionally not subscribed by the initial all-metrics diagnostic; its format is documented upstream, but requires separate bounded sampling and lifecycle safeguards.

Added opt-in Devices-tab action **CIRQA: Alle Garmin-Messwerte testen**. It registers each of the nine supported decoder services sequentially after GFDI, maps only device-confirmed handles, continues when individual registrations are refused, and reports actual received values after a bounded 20-second session. This is capability discovery, **not** proof that CIRQA supports all Garmin services. No fabricated measurements, CLOSE_ALL, cloud, or persistence. The original two-metric diagnostic remains available.

**Validation:** Wait for CI and actual device output before claiming additional measurements. Realtime registration may succeed without the watch sending data. Historical sleep, overnight SpO2, activity and FIT history are a separate GFDI/FIT sync milestone and are not yet implemented.


### Real CIRQA expanded realtime success, build #203 (2026-11-15)

User screenshot of **CIRQA: Alle Garmin-Messwerte testen** after build #203 shows the exact app output:

`CIRQA Echtzeit-Test: Zeitlimit (20 Sekunden). Puls: 83; Ruhepuls: 66; Schritte: 10244; Ziel: 8830; gültige Messpakete: 42; Weitere Werte: Calories(total=3637, active=1314), Intensity(moderate=115, vigorous=89), Stress(level=62), BodyBattery(level=5), Respiration(breathsPerMinute=18), SpO2(percent=97), Hrv(rrMs=702); abgelehnte Dienste: ; Phase: ML-Notification empfangen; Protokollantwort wird geprüft. Keine Daten gespeichert.`

**Device-verified:** Nine realtime service decoders produced values in a single bounded session: HR (6), steps (7), calories (8), intensity (10), HRV/RR (12), stress (13), SpO2 (19), Body Battery (20), respiration (21). 42 valid metric packets; no registration rejection shown. The 20-second timeout is the diagnostic's designed collection window, **not** a failure. RR interval 702 ms is not a computed HRV score. Neither medical accuracy, unit scaling across firmware, continuous monitoring, persistence nor FIT history is verified. Do not treat untested historical metrics or accelerometer as confirmed.

**Next engineering gates:** Persist validated timestamped samples locally with source/service identity and timezone context; make overview show real measurements and unavailable states; add lifecycle/deduplication tests; then GFDI/COBS/CRC and read-only FIT history. Retain no-internet/no-cloud/no-CLOSE_ALL guarantees.
