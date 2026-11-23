# S10 Build 1939 historical-sync postmortem — 2026-11-23

## Evidence boundary

The installed app was Build 1939 from `main` commit `b784e56c`; the S10 run was
manually confirmed once. The versioned UI evidence is the redacted report
recorded in
[`agent-chat ENDE`](../../collaboration/agent-chat/messages/2026-11-23_083400_opencode_ENDE_s10-build1939-sync-and-unknown-fields.md)
and its local screenshot. It shows:

- `accepted=true`, `itemListSent=true`, `pullSent=true`
- 34 list items, 14 selected (53,425 expected bytes), 20 skipped
- three `nextTransaction` follow-up responses, each with zero items
- final reason `next-transaction-followup-empty`, `runDrained=false`,
  `coverage=device-history-unverified`
- the screenshot cuts off before `files=[...]`, where per-file stage totals
  are printed; exact successful transfer, CRC, FIT decode, and SQLite counts
  therefore are not directly recorded for this run

No failure, timeout, or FIT decoder error was visible at the stopping point.
The final reason is produced by the bounded transaction-pagination policy.

## Code-path reconstruction

Relevant product flow in `CirqaMlHandshakeProbe.kt`:

1. The verified FileAccess `sync_response` sets `syncAccepted`.
2. The item-list response is parsed by `CirqaFileAccessItemListParser` and
   passed to `appendSyncItemListPage`.
3. That method records the 34 entries, enqueues 14 items and skips 20.
4. `sendNextSyncFile` removes a queued item and emits its FileAccess pull.
   A non-success pull response ends the run immediately; it does not proceed
   to later pagination.
5. The file queue returns to pagination through `completeSyncFile`, which is
   called only after `CirqaMlrFileTransferAssembler` reports a complete
   expected-size byte sequence. `completeSyncFile` then runs `mlrValidation()`
   and records transfer/integrity/storage/decode/SQLite stage outcomes.
6. Only after the queue is empty does `requestNextSyncItemListPage` follow the
   pending `nextTransaction` token. Three zero-item replies hit the configured
   follow-up limit and stop with the explicit incomplete reason.

**Control-flow inference, not a directly captured counter:** given the
reported 14 queued selections and the final pagination-limit reason, this
normal product path must have drained the selected queue and invoked file
completion/validation for the selected items. That implies complete expected-
size assemblies were reached. It does **not** establish that all file integrity
checks passed, that any FIT decoded/imported, or how many observations were
committed; the missing `files=[...]` tail contains those outcomes.

## Technical decision

- The immediate ending was the intentional three-follow-up pagination bound,
  not an observed GATT/MLR timeout or decoder exception. The bound follows the
  redacted Garmin Connect HCI shape already documented in
  `CirqaItemListContinuationPolicy.kt`; removing it or issuing more
  transactions without a distinguishing protocol hypothesis would be
  unjustified.
- This run may have completed the selected file queue, but its visible evidence
  is insufficient to state successful CRC/FIT/SQLite counts. Do not classify it
  as “download failed” or “download succeeded” without that tail.
- The operational evidence defect is report discoverability: the verbose
  34-item list precedes the decisive per-file stage totals, and the screenshot
  ended before those totals. This change moves redacted file-stage totals ahead
  of verbose item-list details; it does not change transport, parsing,
  pagination, or persistence behavior.
- Unknown FIT fields are metadata within downloaded files, not separate remote
  objects. The 952-group count was unchanged around this run; no incremental
  inventory result is established.

## Next offline and integration steps

1. Compare synthetic replay of the 34-item/14-selected/three-empty-follow-up
   shape against actual parser, queue, completion and report policies; add
   regression coverage for any discrepancy.
2. Reconcile the (redacted) HCI transaction token/reply semantics with the
   reference-client evidence before changing follow-up behavior.
3. GUI-Agent: confirm the existing data contract and whether the compact
   reordered report is sufficient for user-visible progress/failure reporting.
4. CI-Agent: run full branch-head CI, including API-35 emulator, after the
   report change. No hardware access is part of this work item.
5. Any further S10 run requires a distinct user authorization, a checked
   canonical device lease, and an exact bounded scope.

## Authorized follow-up on Build 1958 — 2026-11-23

The user explicitly requested an immediate repeat after CI updated the installed
app to Build 1958. Under the exclusive S10 lease, the product warning was
verified and exactly one positive confirmation was tapped. The warning stated
that Sync may mark band data transferred; no delete/archive/SetFlags/reset
commands are sent. Product limits were 120 seconds, 64 list pages, 256 files,
8 MiB per file, and 64 MiB total. No retry was issued; the lease was released
after the run and subsequent read-only report inspection.

Redacted final report evidence:

- `accepted=true`, `itemListSent=true`, `pullSent=true`
- 37 list items; 16 selected (145,770 bytes); 21 skipped; 4 pages
- 3 transaction follow-ups, all returning zero items; no further transaction
  was automatically requested
- `runDrained=false`, `incompleteReason=next-transaction-followup-empty`,
  `coverage=device-history-unverified`
- 16/16 selected files transferred, integrity-verified, and privately stored;
  12 decoded and SQLite-committed; 0 new imports, 12 `noImportableData`;
  unsupported=0, failed=0
- 1006 MLR chunks assembled to 145,770 bytes; `configureResponse=success`,
  `fileAssembly=complete`, MLR mode `RAW`

The report tail included `manifest-skip:verified-local-import` markers. Thus
the 12 non-importable results must not be described as failed imports or as
newly imported observations; this run recorded no new FIT imports. This bounded
run downloaded and verified its selected files but did **not** establish full
device-history coverage. The three empty transaction responses still carried a
pending transaction indicator in the report. Do not increase the continuation
limit or guess a new filter/token without protocol evidence and a separately
authorized test.

The UI initially shows only a four-line report preview. Tapping the report
expands it; scrolling the expanded text exposed the stage totals. Screenshots
were inspected locally only and are not committed. No report hashes, raw
identifiers, or health values are included here.
