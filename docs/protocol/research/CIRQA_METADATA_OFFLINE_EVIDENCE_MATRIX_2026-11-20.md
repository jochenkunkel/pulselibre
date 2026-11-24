# CIRQA metadata: offline evidence matrix (2026-11-20)

## Scope and evidence rules

This is a consolidation of already documented, privacy-limited offline
inventories and historical protocol observations. It does not read phone-private
data, contact CIRQA, decode unpublished raw values, or establish current device
state. The source originals, identifiers, filenames, hashes, timestamps, and
measurement values remain private.

Evidence levels used below:

- **Observed structure:** a message, field, XML element, or frame shape was
  counted or validated; this alone does not validate its meaning.
- **Historical field value:** a FIT/XML parser found a value in a saved file;
  it may describe another sensor or past state, not the paired CIRQA today.
- **CIRQA frame observed:** a CRC-valid CIRQA frame was received, without
  necessarily understanding all payload fields.
- **Code candidate:** parser or protocol code can represent a field; this is
  not evidence that CIRQA sent or accepts it.
- **Current device value:** none of the offline evidence below establishes one.

## Evidence matrix

| Attribute | Offline observation | Source / parser | Freshness and interpretation | Storage / status |
| --- | --- | --- | --- | --- |
| Physical band battery percentage | FIT `device_info` contains battery-level fields, but the V24/V25, V26, and V27 reviews report no valid stored values. | Garmin FIT Profile 21.222.0 structural inventory; V27 post-run backup review. | No percentage, charging state, low-battery state, or freshness can be inferred. This does **not** mean the band has no battery data through another channel. | No usable battery observation to persist; device UI correctly remains unavailable. |
| Physical band battery voltage/status | No valid FIT voltage or status values in the reviewed V27 metadata; prior private-copy review likewise found zero valid elements. | FIT `device_info` fields 10/11; V27 aggregate review. | Historical file metadata only. No current battery conclusion. | FIT metadata schema supports whitelisted fields, but observed values are absent. |
| Body Battery | A historical bounded realtime test produced a Body Battery metric reading. | Garmin realtime service decoder / prior user-triggered test. | A physiological/device-scale metric, **not** battery charge. Never use it as the band’s power percentage. | Stored/handled as a separate measurement metric, not battery metadata. |
| Device type / manufacturer / product | V27: 81/139 device-type values across 19 sources, 51 manufacturer values across 33 sources, 58 product values across 40 sources. Earlier copies have separate counts. | FIT `device_info`; profile-coded numeric fields. | Historical FIT metadata may describe attached sensors. It does not independently identify the paired CIRQA or supply a user-facing model name. | Whitelisted FIT metadata is persisted; exact codes remain private and require semantic/identity validation. |
| Software / firmware version | V27: 93 non-null software-version values across 40 sources. A saved XML sample has a populated `Model/SoftwareVersion` field, but its text remains private. | FIT `device_info`; verified `Device` XML parser. | Historical source-file values, not proof of currently installed CIRQA firmware. | FIT whitelist persisted; XML parser supports a bounded text whitelist and schema v5 storage. Historical XML was not replayed into the application DB. |
| Hardware / bootloader version | No usable value established in the reviewed inventories. | FIT profile field coverage and current parser whitelist. | Unknown; code/profile presence is not a value. | Not available as verified metadata. |
| Device / friendly name and model | XML schema contains model-related elements; no public value was retained. FIT identity-bearing fields exist, but exact values are private and are not equated with CIRQA identity. | Saved XML structure; FIT `file_id`/`device_info`. | No verified current name/model. BLE advertising, GATT name, Android bond name, and Garmin metadata must remain separate concepts. | No current normalized app value. |
| Serial / unit / MAC identifiers | Identifier-bearing FIT fields and a unit-ID field in the 5024 candidate parser are known structurally. | FIT profile; offline `Cirqa5024DeviceInformationParser`. | Sensitive identifiers remain private. Parser support does not establish a CIRQA response or identity mapping. | Excluded from public reports and the FIT metadata whitelist. |
| Device clock / timezone | FIT records contain historical timestamps; V24/V25 FIT metadata timestamps were structurally valid. | FIT record/device-info timestamps. | Historical timestamps are not a live band clock, timezone, or proof of clock accuracy. | No current clock observation. |
| Last contact / last successful sync / last import | Existing sync reports describe bounded transfer and local database processing separately. | App/sync reports and database provenance. | These are distinct events; a transfer request can mark buffered files as transferred. A file-transfer result is not proof of complete history or a current band clock. | Local import/source provenance exists; no unified current-device sync-state value is established by the offline metadata set. |
| Raw XML metadata | One saved raw file is well-formed `Device` XML. Its structure includes `Model`, `SoftwareVersion`, `MassStorageMode`, and file-entry groups; the analyzed private sample had 51 `TransferDirection` elements and empty mass-storage text. | Hardened `CirqaRawDeviceMetadataParser`; private V24/V25 evidence. | Structural/count evidence only. Empty text is unknown/absent, not `false`; file-entry metadata does not establish current storage or sync state. | Only bounded software-version text, nullable boolean, and element count are whitelisted; raw XML and paths/identifiers are not stored in the report. |
| Opaque raw binary / protobuf-like file | Valid protobuf wire framing was observed, including one length-delimited field in a prior analysis; schema remains unknown. | Offline framing inventory only. | No field meaning, battery, firmware, or sensor inference is justified. | Not decoded or stored as interpreted metadata. |
| GATT battery / Device Information / Current Time | Bluetooth SIG UUIDs are candidate standards; prior static code audit found the explicit GATT probe performs discovery only, without characteristic reads or subscriptions. | Static source audit; standards are candidates only. | No CIRQA service/characteristic read or live battery/time value is established by the offline evidence. | No reader or persisted live observation proven here. |
| Garmin 5024 DeviceInformation | A CRC-valid CIRQA 5024 frame was observed in a prior capture; the minimal ACK candidate was GATT-write-confirmed, but no GFDI application acknowledgement was established. | Dated CIRQA raw-frame analysis. | Frame type and timing are observed; payload semantics/identity are not. The extended 5024 field layout comes from a fenix 8 reference, not CIRQA. | Offline parser and ACK candidate code exist; do not treat parser fields or third-party layouts as CIRQA metadata. |
| Capabilities / maximum packet size | The 5024 parser can represent a protocol version and maximum packet size; foreign-device reference material contains additional fields. | Code candidate and external Garmin reference. | No CIRQA value/semantics verified from the available offline evidence. | Not persisted as verified device capabilities. |

## Snapshot boundaries

- The private V24/V25 archive report covers 31 FIT + 2 non-FIT originals.
  Its decoder/database reconciliation is 294/294 candidate keys; this does not
  establish semantic completeness.
- The private V26 analysis covers 40 FIT + 2 non-FIT originals. The canonical
  database has 294/294 candidate keys for that snapshot; this is not the same
  archive as V24/V25.
- The V27 result reports 45/45 verified local FIT files and 27,736/27,736
  currently importable candidate keys. Its separate metadata review reports
  139 FIT `device_info` rows from 40 sources and zero valid battery voltage,
  status, or level values.
- Counts from these snapshots must not be added together or used to imply
  complete CIRQA history. The app-private live inventory is aggregate-only and
  proves only the contents currently stored and matched at the time of that
  observation.

## Offline conclusion and safe next work

The best supported offline result is: FIT history contains timestamped,
profile-coded device metadata and some historical software-version values; the
examined FIT metadata provides no usable physical battery reading. A saved raw
XML file has a small, structurally verified metadata whitelist, while the
second raw binary remains opaque. Existing CIRQA protocol captures establish some
frame structure, not a verified battery/name/firmware/clock value.

No additional interpretation of private raw bytes is justified without a
schema or a verified parser. Do not synthesize a percentage, derive current
firmware from historical files, equate Body Battery with battery charge, or
infer current sync/clock state from file timestamps.

The next offline-only improvements can audit per-file decoder exclusions and
the coverage of the existing metadata whitelist against the already archived
aggregate evidence. A live CIRQA query or reading current app-private values is
outside this report; none was performed.

## Source documents

- [V27 bounded device result](../device-tests/CIRQA_SYNC_V27_DEVICE_RESULT_2026-11-20.md)
- [V24/V25 offline reconciliation](CIRQA_S25_OFFLINE_RECONCILIATION_2026-11-18.md)
- [V26 offline FIT reconciliation](CIRQA_OFFLINE_FIT_RECONCILIATION_2026-11-20.md)
- [V24/V25 and V26 private-copy rerun](CIRQA_LOCAL_OFFLINE_RECONCILIATION_2026-11-20.md)
- [In-app FIT inventory](CIRQA_IN_APP_FIT_INVENTORY_2026-11-20.md)
- [Active-GATT path audit](CIRQA_ACTIVE_GATT_PATH_AUDIT_2026-11-20.md)
- [CIRQA 5024 reference boundary](../../archive/2026-11-24/protocol/research/CIRQA_5024_REFERENCE_FIELD_LAYOUT_2026-11-17.md)
- [5024/5050 raw-frame analysis](../../archive/2026-11-24/protocol/research/CIRQA_5050_RAW_FRAME_ANALYSIS_2026-11-17.md)
