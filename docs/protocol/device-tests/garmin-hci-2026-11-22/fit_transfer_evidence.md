# FIT transfer evidence — S10 capture 2026-11-22

**`FIT_BYTES_VERIFIED=false` for this capture.**

The HCI replay reconstructed 42,794 ATT PDUs from 42,970 complete L2CAP PDUs,
but did not establish a confirmed MLR stream, a CRC-valid GFDI frame, a
FileAccess pull response, or a complete file byte stream. Consequently there
is no FIT `.FIT` signature, declared file length, header CRC, trailing FIT CRC,
or stable whole-file digest attributable to HCI record ranges.

No Garmin FIT SDK file decoder was run: there is no reconstructed FIT file to
validate. A pull success code or ATT byte count would not meet the file gate.
This finding is capture-specific and does not revise separately documented
evidence for other captures.

The missing transport segment is currently **not identified**. It may be
unobservable in this HCI capture or require additional verified MLR/GFDI
reassembly. This report does not assert that a classic/RFCOMM path was used.
`coverage=device-history-unverified` remains unchanged.
