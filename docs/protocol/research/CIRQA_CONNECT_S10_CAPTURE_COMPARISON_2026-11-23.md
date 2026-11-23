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

## Independent FIT SDK cross-check and missing data paths

For this offline investigation, the published Garmin FIT Python SDK 21.222.0
wheel was downloaded separately, its package-server SHA-256 checked, and the
SDK used locally on the reconstructed files; no private file was uploaded.
All 34 FIT files pass the SDK's independent integrity check and decode without
reported errors. SDK and backup comparison is by source hash, UTC timestamp
and value with occurrence counts (multisets), not just by field counts.

| Field/path | Result in the 34 files | PulsLibre consequence |
| --- | --- | --- |
| FIT record heart rate | 12,804 SDK values exactly match 12,804 canonical provenance/observation rows | This record-message path agrees on source, timestamp and value. |
| FIT record distance | 14,985 exactly match 14,985 canonical rows | Same three-way match; this does not validate daily presentation. |
| FIT enhanced speed | 95 exactly match 95 canonical rows | Same three-way match. |
| Respiration | All 979 review candidates match SDK source, timestamp and scaled value; 554 fall in the UTC day | Stored as explicitly untrusted candidates, excluded from normal observations and unified history. |
| SpO₂ | All 838 candidates match SDK source, timestamp and value; 480 fall in the UTC day | Also review-only. The original SDK messages additionally carry mode and confidence fields, which are not included in the candidate row. |
| Stress | All 979 candidate values match the SDK per source, but **all 979 stored timestamps are null** | SDK profile declares field 1 `stress_level_time` as `date_time`; SDK yields 979 times, 554 in the UTC day. An independent raw uint32 + FIT epoch check matches all 979 SDK times. A null timestamp match is not evidence that PulsLibre imported the time. |
| Sleep | SDK finds 25 sleep-level messages, 20 timestamped in the UTC day | Current historical observation mapping has no sleep-level mapping. A message count is not a sleep-duration or stage-quality validation. |
| Monitoring steps | SDK finds six messages exposing a `steps` field, four timestamped in the UTC day | Current monitoring mapping handles heart rate only, not steps; this does not establish a daily step total or summation semantics. |

The normal `UnifiedMeasurements.refreshHistorical()` copies active generation
observations and legacy imported observations, **not** review-candidate rows.
The candidate reader exists in `CirqaDatabase.readUntrustedFitCandidates()`;
read-only access must not be mistaken for production validation or UI display.
Stress requires particular migration care: existing candidate sample IDs are
stable, and `FitObservationCommitter` checks existing timestamp and decoder
version for exact agreement after `INSERT OR IGNORE`. Simply changing the
decoder timestamp and rerunning the existing import would conflict with old
rows, rather than safely repairing them. A versioned, transactional,
reproducible rebuild/migration with rollback and backup tests is required;
do not silently overwrite prior candidates.

### The 39th item and capture boundary

The unmatched item is the 39th pull and advertises 1,094 bytes. Its 5044 pull
response was received successfully about 0.291 seconds after the request; the
captured connection disconnected about 1.279 seconds after that request, without
an observed associated transfer-service registration or file bytes. The private
post-run archive has no reference with the same item UID. It does contain one
different archived reference to a file of the same length, but equality of
length is not equality of content or identity. The item is not established as
identical to the earlier PulsLibre timeout item merely because that item also
expected 1,094 bytes. The recording interruption may be relevant, but these
observations do not establish its cause or exclude another Garmin session.

### Priority and unresolved questions

1. Review PR #591 and complete its FULL/API-35 gate; the duplicate-decode fix
   addresses our measured serialized processing bottleneck, not the downstream
   missing field mappings.
2. Validate and implement independently tested, provenance-preserving paths for
   respiration, SpO₂ (including confidence/mode), stress timestamps, monitoring
   steps, and sleep-level records. Define quality/status and aggregation
   semantics before showing them as trusted health metrics. Coordinate UI work
   with its owner.
3. Design safe generation/backfill migration rather than editing immutable
   candidates in place; keep original files and test interruption/rollback.
4. Resolve the last item and list pagination with a separately planned test only
   after CI, respecting the established lease and device-safety rules. No
   complete-history or full-day claim follows from this bounded capture.
5. A direct independent comparison against the user's Garmin Connect UI/export,
   including timezone, any preexisting/cloud data and semantic presentation,
   remains outstanding. SDK agreement proves decoding agreement, not that
   Garmin's UI derived all displayed values from the 34 captured FIT files.
