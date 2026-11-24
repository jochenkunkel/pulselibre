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

## Phase A — connection and GATT attribution

- **BEWIESEN:** The replay reconstructs 2,141 complete ATT PDUs over two
  connection epochs. The second epoch (records 2,869–3,190) contains 1,331
  notifications and 724 host write commands overall.
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
- **NICHT NACHGEWIESEN:** The capture alone does not establish the physical
  identity of the second connection's peer or align its timestamps to an
  external clock.

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
- **NICHT NACHGEWIESEN:** These frame counts alone do not prove the requested
  list is complete, every requested file arrived, or any FIT file was fully
  transferred and validated. Route-5 data and the FileAccess request/response
  correlation still need reconstruction; no FIT bytes or complete daily
  history are claimed here.

## Parser correction and verification

`tools/garmin_hci_replay.py` now scopes services and characteristic handles by
the direction of server-to-client discovery responses in addition to
connection epoch. ATT notifications/indications resolve against their sender's
database; writes resolve against the peer's database. A synthetic-only test
covers simultaneous discoveries using identical handle numbers.

Validation run: `python3 -m unittest tools.test_garmin_hci_replay -v` — 22
tests passed. No local raw payloads, content hashes or device identifiers are
included in this report.

## Next

1. Correlate each complete 5043/5044 message by request ID and FileAccess
   operation, without publishing opaque IDs or payloads.
2. Reconstruct the route-5 transfer stream, including MLR chunk boundaries,
   transfer completion and any compression layer.
3. Validate FIT headers, sizes and CRCs before comparing with the private local
   archive or asserting daily-history coverage.
4. Compare only validated request/response behavior with PulsLibre's existing
   offline implementation. Do not change live transport behavior based on this
   capture alone.
