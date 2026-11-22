# S10 Garmin HCI replay (2026-11-22)

This directory contains a redacted, reproducible replay of the already approved
capture from `raw/2026-11-22-s10-garmin-full-sync/`. The parser exports metadata
only: no ATT value bytes, payload hashes, addresses, UIDs, tokens, or health
values are retained in these artifacts.

Reproduce from the repository root:

```sh
python3 tools/garmin_hci_replay.py \
  docs/protocol/device-tests/raw/2026-11-22-s10-garmin-full-sync/btsnoop_hci.log \
  --logcat docs/protocol/device-tests/raw/2026-11-22-s10-garmin-full-sync/logcat-all.txt \
  --summary docs/protocol/device-tests/garmin-hci-2026-11-22/capture_summary.json \
  --events docs/protocol/device-tests/garmin-hci-2026-11-22/wire_events.jsonl
```

`capture_summary.json` records the source hashes, record and opcode counts,
parser anomalies, and the unresolved absolute clock offset. `wire_events.jsonl`
contains HCI connection epochs, reconstructed ATT metadata, and GATT discovery
observations. `transactions.jsonl` deliberately records the FileAccess analysis
gap instead of inventing transactions. See [evidence_matrix.md](evidence_matrix.md)
and [fit_transfer_evidence.md](fit_transfer_evidence.md).

## Boundaries

- The conversion of btsnoop timestamp fields uses the standard 1900 epoch, but
  the absolute clock offset is not validated: converted HCI time and logcat
  threadtime align to within the capture, while the README's UTC window differs
  by about one hour. The report does not assert an absolute UTC correction.
- ATT counts include reconstructed ACL fragments and distinguish host-to-
  controller `0x52` Write Commands from `0x12` Write Requests.
- 49 overlong L2CAP starts and 80 ambiguous overlapping service-range
  associations are surfaced as parser anomalies; ambiguous characteristics are
  not attributed to Garmin ML.
- The capture does not by itself establish MLR/GFDI CRC-valid frames, FileAccess
  transactions, a complete FIT byte stream, FIT CRC, or database import.
- No Wireshark/tshark cross-check was available in this environment. No active
  device access was performed.
