# Evidence matrix — S10 Garmin HCI replay

Source: btsnoop SHA-256
`6cc31e79112f94dd3ea1c146de8bc48d74fd2252cf6290805cb83e4226d8adf8`;
logcat SHA-256
`f6109178543b6edd39e983298c30672d15f2375c92e1243ab6d335e6e421231b`.
Parser: `tools/garmin_hci_replay.py`, version 1. All record references are
zero-based? **No:** record references are one-based, matching btsnoop iteration.

| ID | Claim | Evidence / record range | Status | Limits / counterevidence |
|---|---|---|---|---|
| E1 | The full btsnoop file is parseable as H4/datalink 1002 | 85,154 records; source SHA in `capture_summary.json` | Observed | 49 ACL start payloads exceed the L2CAP-declared length; not silently accepted |
| E2 | ATT traffic is reconstructable across ACL fragmentation | This parser: 42,970 complete L2CAP PDUs; 42,794 ATT PDUs; 23 epochs with ATT | Observed with unresolved count delta | Independent analysis in `CIRQA_S10_BTSNOOP_L2CAP_ATT_REASSEMBLY_2026-11-22.md` reports 43,019 L2CAP / 42,799 ATT and one overwritten fragment buffer. This parser rejects 49 overlong starts; exact 49-PDU/5-ATT delta and the overwritten-buffer difference require record-by-record reconciliation. Do not treat counts as cross-validated. |
| E3 | HCI direction is consistent with ATT request/response patterns | All 6,267 `0x52` Write Commands are host-to-controller; 35,465 Notifications are controller-to-host | Derived from btsnoop flag bit 0 | No controller manufacturer/device identity is inferred from HCI handle alone |
| E4 | Connection reuse is separated | 25 successful LE connection epochs; handles are scoped to epoch | Observed | This export uses one-based btsnoop record numbers; the independent analysis appears zero-based (e.g. this parser's connection record 83,552 vs its 83,551). One active epoch remains at EOF. |
| E5 | Garmin ML GATT namespace was discovered | 106 service-range responses and 84 characteristic declarations across connection epochs | Observed GATT UUIDs | 80 overlapping service-range associations are ambiguous; ambiguous service attribution is withheld |
| E6 | A complete ML registration request/response pair was proven in the sync epoch | No correlated strict pair emitted | Not observed | 68 strict-layout response candidates had no matching locally parsed request; some GATT characteristic/service associations remain ambiguous |
| E7 | CRC-validated GFDI frames (types 5024/5050/5043/5044/5000) were reconstructed | None | Not reconstructed | No MLR channel reassembly or CRC-valid GFDI frame was established from this replay |
| E8 | FileAccess pagination/pulls/flags were reconstructed | None | Not reconstructed | 5043/5044 were not safely identified; no transaction pairing is claimed |
| E9 | FIT bytes were completely reconstructed and validated | None | Not observed for this capture | No file header/signature/length/CRC and no Garmin FIT SDK file-level validation |

## Timebase qualification

The first/last converted btsnoop timestamps are
`2026-11-21T22:52:55.442731Z` and `2026-11-22T17:53:51.235996Z`.
The logcat threadtime strings span `11-22 17:50:17.413`–`17:53:04.435`
without a timezone. The raw converted HCI time appears to align with local
threadtime, while the source README describes a 16:50–16:53 UTC sync window.
This is an approximately one-hour offset disagreement. The synchronized
connection near records 83,552–84,877 is therefore reported using source-clock
conversions only; absolute UTC and exact window boundaries remain unresolved.

## Independent tooling

Neither `tshark`/Wireshark nor Scapy was installed in the execution environment.
The specified same-filter cross-tool count comparison is therefore blocked,
not passed. No external capture conversion or payload copy was created.
