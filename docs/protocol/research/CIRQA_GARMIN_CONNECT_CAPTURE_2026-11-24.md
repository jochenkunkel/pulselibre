# Garmin Connect S10e capture reconstruction (2026-11-24)

## Scope and limits

This is an offline analysis of the locally preserved HCI capture. The original
capture, decoded payloads, identifiers, file hashes and private analysis outputs
remain outside Git. No device connection, BLE command or band-state change was
made. The capture is not independently tied to a physical peer by this replay;
the findings below are scoped to its connection epochs and discovered GATT
databases.

Confidence labels: **BEWIESEN** means directly validated in the capture or
synthetic parser test; **WAHRSCHEINLICH** is an interpretation of validated
structure; **NICHT NACHGEWIESEN** means the capture does not yet establish it.

## Phase A — first connection epoch and GATT attribution

- **BEWIESEN:** The replay reconstructs 2,141 complete ATT PDUs over two
  sequential connection epochs. The second epoch (records 2,869–3,190) contains
  1,331 notifications and 724 host write commands overall.
- **BEWIESEN:** Both peers perform GATT discovery during the second epoch.
  Their responses describe separate, overlapping handle spaces: the host-side
  database includes Generic Access/Attribute, while the controller-side
  database additionally includes Garmin ML, Heart Rate and Running Speed/Cadence
  services.
- **BEWIESEN:** The controller-side ML service exposes seven discovered
  characteristics. On this database, 92 notifications map to ML RX
  characteristic `6a4e2812-…`, and 64 host writes map to ML TX characteristic
  `6a4e2822-…`.
- **BEWIESEN:** The initial 12 ambiguous service-range findings were a replay
  attribution defect: the parser combined both peers' independent GATT handle
  spaces. Discovery/service/characteristic maps are now keyed by connection
  epoch and server direction. The corrected replay has no overlapping-range
  errors, and a synthetic regression test verifies simultaneous discovery and
  correct write/notification attribution.
- **NICHT NACHGEWIESEN:** The first epoch's GATT discovery is incomplete, so
  its likely `2810/2820` ML pair is not independently established. The capture
  alone also does not establish either peer's physical identity or align its
  timestamps to an external clock.

## Phase B — initial ML/GFDI reconstruction

- **BEWIESEN:** In the second connection epoch, the route-4 ML stream contains
  39 controller-to-host and 40 host-to-controller COBS-decoded GFDI frames
  whose declared lengths and CRC-16/ARC validate. The counts are scoped to
  complete frames recovered by the current offline method; invalid or
  incomplete segments are not interpreted as protocol messages.
- **BEWIESEN:** Among those validated frames are controller-to-host 5043/5044
  frames (13/5) and host-to-controller 5043/5044 frames (8/5). The current
  strict offset/length reassembly completes all 31 corresponding protobuf
  messages. Their decoded schema shapes include FileAccess service messages.
- **WAHRSCHEINLICH:** This connection carries a Garmin Connect FileAccess
  interaction, rather than only ML registration or bootstrap traffic.
- **UPDATE — BEWIESEN, now transfer-correlated:** In epoch A, a successful
  PullItem response provides a transfer handle that appears in the subsequent
  route-5 MLR configure write. The route-5 notification window from that write
  to the next configure write contains one configure-shaped callback followed
  by 67 data notifications. Removing the MLR routing byte and the two-byte
  per-notification transport header, then concatenating, gives a complete
  zlib stream. Its 13,039-byte output exactly matches (byte-for-byte; digest
  checked privately) one of the 45 private FIT originals; independent FIT
  parsing validates the file CRC. Its messages include 1,405 records of
  unmapped Global Message 397 and one Global Message 398
  (`skin_temp_overnight` in the cited FIT profile), but no standard activity
  `record`, `session` or `activity` messages. This correlates the bytes to a
  PullItem response handle and MLR transfer window, not to a specific observed
  list item or complete history coverage.
- **WAHRSCHEINLICH:** The short callback at the start of the transfer window is
  the configure response: after the routing byte it has a one-byte preamble
  followed by three zero status bytes. The capture does not expose the
  post-MLR callback boundary, so this interpretation is not used as proof of
  transfer completion. The productive assembler's two-byte per-notification
  transport-header handling is consistent with the exact reconstructed bytes.
- **NICHT NACHGEWIESEN:** The second epoch's route-5 candidate decompresses to
  19,975 bytes but has no FIT signature and matches none of the 45 private FIT
  originals. Its file format/type is unresolved. Neither epoch establishes
  complete history coverage.

## Issue #637 — chronology and comparison with PulsLibre

Times below are intentionally relative to each captured connection. The
btsnoop clock has not been anchored to wall time, and the two epochs are
sequential connections, not simultaneous channels.

| Order | Garmin Connect capture | Response / acknowledgement | PulsLibre productive code |
| --- | --- | --- | --- |
| Connect, epoch A | GATT discovery is incomplete; `2810/2820` remains a candidate only. Effective MTU is 150. | The recovered GFDI 5000 status responses are separate from ATT write completion; no unverified CCCD result is inferred for this epoch. | `GarminMlOffline.findCharacteristicPair()` selects the first discovered RX candidate in `2810..2819` and its `+0x10` TX counterpart when present. No comparable PulsLibre wire trace is identified. |
| Epoch A | GFDI bootstrap includes 5024/5050 and subsequent 5030/5031/5026/5036 traffic. | Captured 5000 responses to bootstrap and 5043/5044 frames carry status 0. These are protocol ACKs, not file-completion proof. | The productive FileAccess path waits for the validated sync response before listing. Do not infer a byte-for-byte bootstrap equivalence from the candidate characteristic mapping. |
| Epoch A | Two ItemList requests have nested field shapes 4+5, then 5+6; the latter includes an `included_data_types` submessage. The observed response has status 0, one item, no `session_id`, and a `next_transaction_id`. Fifteen host PullItem requests and fifteen device PullItem responses are in scope. | Each observed PullItem response is success-shaped (`result=0`, `transport=0`, transfer handle present). One response handle is directly correlated to the configure write and exact FIT transfer window below. GFDI 5000 ACKs to observed 5043/5044 frames have status 0; ACKs are not completion proof. | Initial list is unfiltered; continuation uses only `session_id` or a separate `transaction_id`. Returned items are queued without a date/type filter (subject to verified-local-import and existing count/size/time safety budgets). |
| Epoch A | One MLR transfer window reconstructs the exact private Daily-History-type FIT described above and correlates to PullItem response handle ordinal 7. Another transfer window (ordinal 14) reconstructs a 19,975-byte non-FIT object. | Ordinal 7's 13,039-byte output does not match the size of the sole observed list item; that list item's UID instead matches PullItem ordinal 14. Thus exact transfer-to-PullItem-handle correlation is established, but the FIT's originating list item/expected-size metadata is not. The configure response/completion semantics remain unverified. | The pull builder includes UID, data type and size in its item reference and emits zero-valued priority/transport/offset fields; it does not emit PullItem request field 5. |
| Reconnect, epoch B | GATT discovery verifies ML RX `2812` and TX `2822`; effective MTU remains 150. Service Changed indication and ML RX notification subscriptions receive ATT Write Responses. | No ATT Handle Value Confirmation was observed. This is distinct from GFDI 5000 status ACKs. | The current selector is compatible with the discovered pair. This alone does not establish different application behavior. |
| Epoch B | The same observed ItemList field shapes occur; one item is listed, status 0, no session cursor, and a next-transaction value. One host PullItem request and one device PullItem response are present. | The response is success-shaped (`result=0`, `transport=0`, transfer handle present). Its route-5 window decompresses to a non-FIT 19,975-byte payload byte-identical to epoch A's ordinal-14 transfer; no full-history conclusion follows. | Same unfiltered selection and bounded download pipeline as epoch A. |

**BEWIESEN:** The capture's ItemList wire shapes differ from PulsLibre's
productive initial empty request and token-only continuation: Connect includes
additional request fields, including `included_data_types` on one request.
**NICHT NACHGEWIESEN:** The captured filter values' intended category coverage,
whether the filters caused any missing data, or whether Connect's behavior
explains PulsLibre's observed CSV delta. Copying those filters is not justified.

The Connect PullItem requests carry an item field containing UID bytes, with no
ItemReference transaction-ID field. They also contain additional scalar fields
not emitted in PulsLibre's current request. Field 5 is present with value 15 in
the captured request; field 2 varies. This is a verified wire/code difference,
not proof that either field controls history coverage. No speculative request
change is recommended.

## FIT, SQLite and CSV evidence boundaries

- The private archive contains 45 FIT originals, structurally/CRC checked
  without parse errors; across that set are activity/session files and
  Daily-History-related message types (including monitoring, stress, SpO₂,
  respiration and sleep). Their source-run correspondence to the whole HCI
  capture is not established.
- The exact FIT reconstructed from epoch A is present in the private downloaded
  file archive. Its content digest has no matching source row in the inspected
  PR-side SQLite observation, provenance or unknown-field comparison exports.
  This proves absence from those inspected snapshots only; it does not
  distinguish decoder omission from snapshot scope or a later export filter.
- The reported before/after CSV delta of 680 observations (340 heart-rate and
  340 distance, source `cirqa-fit`) proves a recent activity became visible in
  that export. The available evidence does not map those rows to this HCI
  capture's FIT transfer. CSV row counts therefore do not measure the number
  or categories of downloaded originals.
- **Conclusion:** The captured HCI stream contains a complete MLR transfer
  window, correlated by transfer handle to a successful PullItem response,
  reconstructing at least one Daily-History-type FIT. Thus the observation
  that Connect's data here is activity-only is disproved. Correlation to the
  exact originating list item/expected-size metadata and verified MLR
  completion semantics remain open. Whether Connect
  requests broader coverage than PulsLibre is **not established**: list filters
  differ on the wire, but no equivalent PulsLibre wire trace, complete list, or
  common run/coverage denominator is available.

## Parser correction and verification

`tools/garmin_hci_replay.py` now scopes services and characteristic handles by
the direction of server-to-client discovery responses in addition to
connection epoch. ATT notifications/indications resolve against their sender's
database; writes resolve against the peer's database. A synthetic-only test
covers simultaneous discoveries using identical handle numbers.

Validation run: `python3 -m unittest tools.test_garmin_hci_replay -v` — 22
tests passed. No local raw payloads, content hashes or device identifiers are
included in this report.

## Remaining gaps

1. Resolve the epoch-A GATT UUID pair. The route-5 MLR prefix and PullItem
   handle for the exact FIT are correlated, but the FIT's originating list
   item/expected-size metadata and an independent transfer-completion signal
   remain unresolved.
2. Identify the epoch-B non-FIT payload and establish whether it is a complete
   object.
3. Map the captured Connect filter values to their actual agent configuration
   and intended data coverage; do not infer meanings from field presence alone.
4. Obtain a comparable PulsLibre trace and common-run archive/SQLite/CSV
   manifest before attributing the 680-row delta to a specific download,
   decoder, persistence or export stage.
5. Do not change live transport behavior from this capture alone.
