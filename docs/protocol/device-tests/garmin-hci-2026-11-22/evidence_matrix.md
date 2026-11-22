# Evidence matrix — S10 Garmin HCI replay

Source: btsnoop SHA-256
`6cc31e79112f94dd3ea1c146de8bc48d74fd2252cf6290805cb83e4226d8adf8`;
logcat SHA-256
`f6109178543b6edd39e983298c30672d15f2375c92e1243ab6d335e6e421231b`.
Parser: `tools/garmin_hci_replay.py`, version 1. All record references are
zero-based? **No:** record references are one-based, matching btsnoop iteration.
The Bluetooth Core 5.4 HCI specification defines `Connection_Handle` range
`0x0000–0x0EFF` ([HCI functional specification](https://www.bluetooth.com/wp-content/uploads/Files/Specification/HTML/Core-54/out/en/host-controller-interface/host-controller-interface-functional-specification.html));
`0xFF1` is outside that range.

| ID | Claim | Evidence / record range | Status | Limits / counterevidence |
|---|---|---|---|---|
| E1 | The full btsnoop file is parseable as H4/datalink 1002 | 85,154 records; source SHA in `capture_summary.json` | Observed | 49 ACL records on reserved HCI handle `0xFF1` are excluded before L2CAP parsing; they are controller-to-host and have declared L2CAP lengths shorter than their ACL bodies. One-based record ranges by CID: 0x0001 `1031–1035`; 0x0002 `16859–16863`; 0x0003 `16864–16865, 16867–16869`; 0x0004 `17427–17431`; 0x0005 `17432–17434, 17436–17437`; 0x0006 `49277–49281`; 0x0007 `49282–49286`; 0x0008 `49298–49302`; 0x0009 `49303–49304, 49306–49308`; 0x000A `49319–49320`; 0x000B `49321–49322`. |
| E2 | ATT traffic is reconstructable across ACL fragmentation | This parser: 42,970 length-consistent L2CAP PDUs; 42,794 ATT PDUs; 23 epochs with ATT | Observed; count delta explained | Independent analysis reports 43,019 L2CAP / 42,799 ATT. Record-level reconciliation found the exact difference: all 49 additional ACL records are controller-to-host `PB=2` starts on reserved HCI handle `0xFF1`; their declared L2CAP lengths are shorter than their ACL bodies. Five use CID 0x0004 (one-based records 17,427–17,431); the remaining 44 use non-ATT CIDs. The handle is outside the valid connection-handle range and has no LE connection epoch, so those five are ATT-shaped candidates, not accepted ATT PDUs. Thus the delta is a count-method difference (candidate L2CAP headers vs. valid, length-consistent PDUs), not 49 recoverable payloads or an ATT-parser loss. The independent report's separate “one overwritten fragment buffer” remains unreproduced: this replay reports no `second_start_before_completion` and zero incomplete assemblies at EOF; its record-level collision check found no valid-handle/direction collision. The 49 invalid-handle PB=2 records are a possible source of a non-filtering parser's apparent overwrite, but the independent implementation/rules are unavailable, so that causal link is unconfirmed. |
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
without a timezone. At the connection start, btsnoop record 83,552 converts to
`17:52:23.297305Z`; logcat lines at `17:52:23.298` contain Bluetooth-stack
connection activity, followed by `GATTC_Discover` at `17:52:23.324` and the
service-discovery callback at `17:52:23.381`. This is a close numeric
cross-source correlation (about 1–84 ms at these markers), not an independent
timezone/UTC validation. It differs from the README's 16:50–16:53 UTC window;
the reason for that discrepancy and absolute UTC attribution remain
unresolved. Keep source-clock values and confidence labels; do not apply an
assumed offset.

## Independent tooling

Neither `tshark`/Wireshark nor Scapy was installed in the execution environment.
The specified same-filter cross-tool count comparison is therefore blocked,
not passed. No external capture conversion or payload copy was created.
