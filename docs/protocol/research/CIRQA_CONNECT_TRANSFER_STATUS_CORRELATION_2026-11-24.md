# CIRQA/Garmin Connect TransferStatus correlation (2026-11-24)

## Scope and method

This is a redacted, offline-only reanalysis of the locally preserved HCI
capture. No device commands were sent. Raw captures, payload bytes, UIDs,
transfer handles, GFDI transaction IDs, hashes, and health values remain
outside Git.

The analysis used the direction-aware GATT replay and the existing COBS/GFDI
framing and strict protobuf-message reassembly. Reported results are counts,
field numbers, handle-equality results, and relative record ordering only.
Connection-epoch physical identity remains unproven.

Confidence labels:

- **OBSERVED:** validated wire structure, CRC, and complete message boundaries.
- **CORRELATED:** equality of opaque handle/transaction values checked in
  memory; the values themselves are not reported.
- **RECONSTRUCTED LABEL:** FileAccess operation names assigned by an
  independent, non-official schema reconstruction; wire field numbers remain
  the direct observation.
- **NOT ESTABLISHED:** semantics not derivable from the captured structure and
  timing alone.

## Epoch A findings

- **OBSERVED:** The route-7 stream contains 15 complete host-to-controller
  PullItem requests and 15 controller-to-host PullItem responses. Their GFDI
  request IDs pair one-to-one, and the responses have success-shaped fields
  with transfer handles.
- **OBSERVED / RECONSTRUCTED LABEL:** The same stream contains 17 complete
  controller-to-host GFDI 5043 messages with FileAccess service field 5 and 17
  host-to-controller GFDI 5044 messages with service field 21. These are
  conventionally labelled `TransferStatusRequest` and
  `TransferStatusResponse`; the capture directly establishes only the wire
  field numbers and shapes.
- **CORRELATED:** All 17 TransferStatus request GFDI IDs have a matching
  response ID. Each request has nested fields 1 and 2; the independently
  reconstructed schema labels these as item UID and transfer handle.
- **CORRELATED:** The 15 observed PullItem response handles all occur in the
  TransferStatus request-handle set. The two additional TransferStatus request
  handles do not match any of those 15 PullItem responses in this epoch; their
  origin is unresolved.
- **OBSERVED:** Of the 17 TransferStatus responses, 15 contain nested field 1
  and two are empty. For the present field-1 values, the redacted histogram is
  13 occurrences of one value and one occurrence each of two other values.
  Values are omitted. The reconstructed `next_transfer_priority` label is not
  independently validated, and the capture does not explain how the response
  values are selected.
- **CORRELATED:** Both route-5 MLR windows previously matched to PullItem
  handles (ordinals 7 and 14) have a same-handle TransferStatus request and a
  GFDI-ID-matched response during the observed window. A second in-memory
  replay tracked each notification's record index while incrementally
  decompressing the MLR chunks. For both files, the zlib end marker occurs
  before the same-handle TransferStatus request and its matched response.
  Both responses still precede the end of the broader route-5 notification
  window.
- **OBSERVED, interpretation open:** For 14 of the 15 PullItem handles whose
  matched TransferStatus response contains field 1, the response's scalar
  equals that PullItem request's priority scalar; one differs. Most captured
  PullItem requests share the same priority, so this correlation does not
  distinguish an echo of current priority from a queue/next-priority rule.
  The optional response field is therefore not modeled as a constant or as a
  proven priority echo.

## Conclusion and implementation boundary

The earlier statement that the capture did not establish whether the correlated
PullItem was accompanied by TransferStatus was too conservative. **The capture
does establish matching TransferStatus request/response exchanges for the two
PullItem-handle-correlated MLR windows**, and the request-handle set covers all
15 observed PullItem response handles.

For the two correlated windows, both TransferStatus exchanges follow the
reconstructed zlib end marker. This is **consistent with** a post-data transfer
status exchange, but does not independently establish the protocol's terminal
completion semantics or generalize to every transfer. The response also
precedes the end of the broader notification window. The capture does not
establish the meaning or selection rule for response field 1: it usually
matches the current PullItem priority, but one observed matched response does
not, and most priorities are shared across requests.

An offline search found no local Garmin APK/DEX or generated FileAccess
descriptor source to validate the proprietary callback/priority policy. The
available third-party source is an independent reconstruction rather than an
official schema; its repository snapshot has no explicit license file or
project license declaration, so no code is used or copied from it here.
Public web searches did not locate an independent primary schema.

Therefore no TransferStatus responder or completion behavior is added yet. The
next implementation gate is an evidence-backed rule for the response field and
its timing relative to the local transfer lifecycle.

This finding does not establish complete history coverage, the exact
list-item-to-file mapping for every transfer, or the physical identity of the
captured peer.
