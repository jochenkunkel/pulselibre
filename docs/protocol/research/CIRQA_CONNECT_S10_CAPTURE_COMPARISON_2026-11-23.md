# S10 Garmin Connect capture: initial offline comparison

## Scope and confidence

The owner reports a manual Garmin Connect sync displaying the day's data. The
private HCI and logcat captures remain outside Git. This analysis did not
independently verify the displayed values, their completeness, or compare them
against local imported observations. No hardware access or commands were made
for this analysis.

The existing issue-506 offline HCI replay reconstructs ACL, L2CAP and ATT; the
selected connection epoch contains discovered Garmin ML RX/TX characteristics.
There are overlapping GATT-service-range ambiguities in the replay, and capture
timestamp absolute alignment is unverified. Thus the attribution below is to
that discovered ML connection, not an independent proof of the identity of the
physical peer. A local in-memory analysis additionally reassembled ML
registration and GFDI COBS streams. Frame counts below require exact GFDI
length and CRC-16/ARC verification; protocol-field counts require complete
single-chunk protobuf messages. No payloads or identifiers are published.

## Observed structure of the selected connection

- 39 accepted ML registration responses: one for GFDI service 1 and 38 for
  FileAccess transfer service 8216. This is not by itself proof that all 38
  file contents were complete or FIT-valid.
- GFDI: 41 host 5043 requests; one Smart field 13 request, one FileAccess
  item-list request (Smart field 43 / service field 9), and 39 FileAccess pull
  requests (service field 1).
- 44 device 5044 frames, including one four-chunk message; 43 host GFDI 5000
  responses to 5044. Transport counts alone do not establish whether the last
  frame was acknowledged after capture or whether a retransmission occurred.
- 114 device 5043 frames contain FileAccess service field 5, with 38 distinct
  request IDs appearing three times each. No host GFDI 5000 response to those
  5043 frames was observed within this connection. A repeated request ID does
  not by itself prove why it repeats or whether its application payloads match.
- The selected connection's first-to-last observed ML ATT activity spans about
  138.5 seconds according to the source clock. That is not a measurement of
  end-to-end sync duration, nor evidence that increasing our 120-second budget
  alone will complete history.

## Comparison with PulsLibre

PulsLibre already uses FileAccess item-list and pull requests over 5043 and
registers the transfer service; a wholesale switch of transport is not supported
by these observations. Garmin sent only one list request in this captured
connection; the response's session and transaction context still needs to be
reassembled and validated before comparing pagination policies. In particular,
the absence of a second list request here does not establish that additional
pages never exist or that transaction followups should be removed.

In the earlier PulsLibre Build-2021 run, local FIT decoding and import consumed
significant time on the serialized sync path. PR #591 eliminates the observed
duplicate decode without changing wire behavior or download limits. CI and
device acceptance are still pending; this capture does not independently measure
Garmin's decode/commit scheduling or prove that this is the only bottleneck.

## Next analysis

Reassemble and correlate the complete item-list response and pull responses,
then validate transfer completion, compression and FIT integrity privately.
Compare request/response ordering, ACK timing, transfer pacing and context
against the existing implementation using synthetic test fixtures only. Treat
capture completeness and timestamp alignment as explicit limits. Do not add
new transport commands or claim full coverage on the basis of frame counts.
