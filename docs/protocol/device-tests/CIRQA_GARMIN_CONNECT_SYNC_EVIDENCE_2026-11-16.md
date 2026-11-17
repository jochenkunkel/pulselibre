# CIRQA – Garmin Connect sync evidence and diagnostic decision (2026-11-16)

## Provenance and privacy
Source: user-provided Garmin Connect 5.31 device-sync diagnostic screenshots, 2026-11-15/16, Samsung Android 17. This document intentionally omits account identifiers, device unit ID, BLE address and per-file UUIDs. Screenshots and raw bugreports are **not** committed.

## Observed file transfers (Garmin Connect terminology)
- 2026-11-15 07:40: SUCCESS; three UPLOAD files: FIT_TYPE_72 1952 B, MONITORING_B(32) 1622 B, MONITORING_B(32) 593 B; all ARCHIVED.
- 2026-11-16 06:40: AUTOMATIC_PREFERRED FAILURE; 15 file operations, 2 visible failures. UPLOAD entries include MONITORING_B(32), BIOMETRIC(44) 793 B, SLEEP_DATA(49) 950 B, FIT_TYPE_68/72/73/79; downloads BIOMETRIC(44) and ACTIVITY_SUMMARY(20) failed with SOURCE_FAILED / UNEXPECTED_SERVER_RESPONSE. Other transfers succeeded. Separate AUTOMATIC_STANDARD run at 06:40 succeeded, DOWNLOADING BIOMETRIC(44) 200 B ACKED.
- 21:28 and 21:31: MONITORING_B(32) uploads archived; TRUE_UP(39) downloads ACKED.
- 21:45: MONITORING_B(32) 1646 B archived; TRUE_UP(39) 13723 B ACKED.
- 21:57: MONITORING_B(32) 1607 B archived; TRUE_UP(39) 13838 B ACKED.
- **22:04 reference sync**: USER_INITIATED_GENERAL SUCCESS, MONITORING_B(32) 1484 B UPLOAD ARCHIVED; AUTOMATIC_ALLOWED SUCCESS, GDXml 19975 B UPLOAD CACHED; AUTOMATIC_STANDARD SUCCESS, TRUE_UP(39) 13968 B DOWNLOAD ACKED. User confirmed successful Garmin Connect sync after app reinstall and manual synchronization with PulsLibre inactive.

## Interpretation constraints
- UPLOAD and DOWNLOAD are directions in Garmin Connect's sync-report vocabulary; they are **not** a direct packet-level description of BLE directions.
- ARCHIVED, ACKED, CACHED indicate the reported application-level disposition; they do **not** prove the GFDI command sequence or FileSync V2 use.
- MONITORING_B and SLEEP_DATA show the device ecosystem exchanges monitoring/sleep file categories, but no actual FIT contents or heart-rate records were inspected.
- The 22:04 btsnooz-converted HCI log contains 6990 records, all HCI command/event, **zero ACL data packets**. It cannot establish a 5050 ACK or ATT transfers. Capture full ACL before interpreting Garmin Connect's response to 5050.
- The previous active compact FileListRequest (0x8A39) received GFDI 5000 UNSUPPORTED; do not retry speculatively.

## Engineering priorities
1. Add an offline btsnoop diagnostic that counts HCI record types and explicitly rejects traces with zero ACL payloads as insufficient for GATT/ATT analysis; add fixture tests.
2. Ensure PulsLibre opt-in GFDI report clearly distinguishes RX vs TX, timestamps, CRC validity, 5050 retry intervals, and transport/ATT handles. Default reporting must redact private payloads and identifiers.
3. Acquire **one** full bidirectional ACL/ATT Garmin Connect sync trace with the existing paired device, without reset/reinstallation; keep raw data private.
4. Correlate device 5024 -> 5050 x3 -> 5011 with verified TX, then write offline regression fixtures before any new active command.
5. Only after confirmed bootstrap, implement read-only file listing, complete retrieval, persistence and FIT decoding.

Related: [5050 research](../experiments/CIRQA_5050_RESEARCH_AND_NEXT_TEST.md), [FileSync reference comparison](../research/CIRQA_FILESYNC_REFERENCE_COMPARISON_2026-11-16.md), [safe device test](CIRQA_NEXT_DEVICE_TEST.md), [Issue #7](https://github.com/jochenkunkel/pulselibre/issues/7).
