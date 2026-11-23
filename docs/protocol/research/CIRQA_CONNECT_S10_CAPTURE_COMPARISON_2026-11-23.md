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

## Deeper local reconstruction and backup comparison

An independent local analysis reassembled CRC-valid GFDI messages by direction,
request ID, length and offset, and correlated FileAccess requests and responses.
The one successful item-list response contained 39 entries, no session cursor,
and a next-transaction token. All 39 pull requests referenced an item from that
list and received success responses for transport 0. The capture contains 38
associated transfer-service registrations and 38 successful configure
responses; the remaining pull did not have an associated captured transfer.
These are capture-bound results, not a claim about what happened outside it.

For each of the 38 observed transfers, the first service callback was validated
as the successful MLR configuration response. Concatenating subsequent payloads
without stripping additional bytes produced exactly the corresponding listed
size. All 38 reconstructed byte sequences have distinct SHA-256 hashes and
match originals already in the private PulsLibre post-run archive. The earlier
pre-run backup contains 36 of those 38. Of the 38, 34 are FIT files whose
full-file CRC verifies; all 34 also have a source record in the post-run backup.
This is a byte-for-byte match, not merely a matching file size. Neither hashes
nor file content are published.

The private post-run backup stores 28,668 canonical FIT observation-provenance
rows (heart rate, distance, speed), and 2,796 review-only candidate rows
(979 stress, 838 SpO₂, 979 respiration). These candidate rows explicitly carry
`UNTRUSTED_PROFILE_CANDIDATE`; they are not ordinary canonical observations.
For the 34 reconstructed Garmin FIT sources, restricting timestamps to
2026-11-23 **UTC** yields 423 canonical heart-rate provenance rows and 554
respiration plus 480 SpO₂ review-only candidates. UTC is used solely as a
clearly defined comparison window, not an assertion about the phone's local
calendar day. Record counts do not establish whether values are valid, match
Garmin Connect's UI, or represent complete daily coverage.

**Interpretation:** For these 38 captured transfers, the main observed
discrepancy is not missing downloads: the exact files are already archived.
At least respiration and SpO₂ are instead withheld from production observations
by the intentional review-only decoder policy in `FitHistoricalObservations`.
The corresponding raw FIT originals and candidate rows are retained. The 39th
captured pull's file is not proven complete, and other files, fields, UI
derivations and Garmin's possible cloud-side data are not covered by this
comparison. No candidate should be promoted to a trusted health measurement
without independent profile, timestamp, range and semantic validation.

The private analysis script and source data remain outside Git. The local
analysis did not change BLE requests, persistence, UI, candidate trust, or the
pending #591 fix. Independent parser/fixture verification remains useful.
