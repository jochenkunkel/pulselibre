# S10 terminal-evidence and history-pipeline review (2026-11-23)

## Executive result

- **BEWIESEN — terminal stop:** The saved S10 status screen says
  `120-s-Zeitlimit`. The production path arms a hard 120,000 ms timer and calls
  `finish(...)` when it expires. This, not `session_id` or transaction
  pagination exhaustion, is the visible terminal cause for the last 36/37 run.
- **NOCH UNGEKLÄRT — causal link to the measurement gap:** The timeout proves why
  that sync run stopped; it does not prove that the timeout caused the absence
  of the recent measurements. The examined originals contain none in the
  checked recent window, but the retained evidence cannot show whether the
  device offered an additional/current file that the run failed to reach.
- **BEWIESEN — meaning of 36/37:** `HistorySyncProgress.fromReport()` parses
  `successful=N` as the numerator and `selected=N` as the denominator. Thus
  36/37 means **36 selected file pipelines reported successful out of 37
  selected for pull**. It does not mean 36 of 37 device-listed items, 36 of 37
  historical days, or full-history coverage. `CirqaSyncFileCompletion` counts a
  FIT file successful only after its required storage/decode/SQLite status;
  valid non-FIT files are successful after private storage.
- **NOCH UNGEKLÄRT — last item state:** The preserved screenshot truncates the
  long report before its per-file stage totals. The durable before/after SQLite
  backups do not contain the terminal UI report. We therefore cannot state
  whether the one selected-but-not-successful file was still queued, in-flight,
  or counted failed, nor its format/size/stage. The visible `5043` transport ACK
  for request 38 confirms transport acknowledgement only; it is not an
  application-level ItemList or successful file response.
- **NOCH UNGEKLÄRT — ordering and last processing stage:** Existing reports and
  archives do not preserve a per-selected-item ordered event ledger for that
  run. They are insufficient to reconstruct which file was the 37th selected
  item or its last completed stage; no later archival state can retroactively
  prove the in-flight state at timeout.
- **NOCH UNGEKLÄRT — continuation state at timeout:** The available terminal
  excerpt does not preserve list-page count or whether a `session_id` cursor or
  `next_transaction_id` token was present/pending. The code normally requests
  another list continuation only after the current page's selected file queue
  drains, but the historical report needed to verify the exact state is
  incomplete. No extra page/transaction request is inferred from the ACK.
- **PLAUSIBEL — why this run reached the time cap:** PR #567 deliberately
  removes an unverified pre-pull skip. Current `shouldPullTargetedItem()` always
  pulls an accepted item; local content-addressed deduplication happens only
  after receiving bytes. Thus a repeated full listing can require repeated
  transfers and plausibly use the 120-second run budget. The screenshot proves
  the cap was reached; the evidence does not include per-file durations to prove
  which transfer consumed the final seconds.

## `session_id` and transaction context are not interchangeable

Evidence hierarchy: generated Connect 5.31 schema/call graph and observed
Connect HCI, cross-checked against Issue [#148](https://github.com/jochenkunkel/pulselibre/issues/148)
and CIRQA V25.

| Term | Wire field / meaning | Current implementation |
| --- | --- | --- |
| Response `session_id` | `ItemListResponse` field 2. Cursor for another page in the **same** listing session. | `CirqaItemListProgress.next()` returns `SESSION_PAGE` when present; `CirqaItemListContinuationPolicy.next()` gives it precedence, even if a transaction token is also present. `SessionPage` emits request field 1 only. |
| Response `next_transaction_id` | `ItemListResponse` field 3. Context for a **separate later transaction**, not a page cursor. It is not a boolean. | With no `session_id`, progress is `TRANSACTION_PENDING`; policy emits a separate bounded `TransactionFollowup`, encoded as request `transaction_id` field 2. Request builder prohibits sending session and transaction identifiers together. |
| Request `transaction_id` | `ItemListRequest` field 2; Connect supplies the response's `next_transaction_id` in a later filtered request. | Separate follow-up path; at most three observed repeated-token requests per sync, then stop incomplete if the token remains. This bound is not a proof that the transaction is drained. |
| `ItemReference.transaction_id` | ItemReference field 5; a per-item property, not the response's `next_transaction_id`. | Parsed separately from item metadata; never used as the session cursor. |

The V25 S10 run had one successful page with 33 items, no `session_id`, and a
`next_transaction_id`. V25 did **not** send a transaction follow-up; it pulled
all 33 listed items successfully. This is evidence that the token was not used
as a page cursor, not evidence that no later transaction could exist.

The later Garmin Connect capture showed a populated response and three empty
successful responses that continued to advertise a transaction token. The
current synthetic replay reproduces that bounded wire shape and ends at
`TransactionLimitReached`/incomplete. An empty `status=0` response is successful
but is not proof of completion while the corresponding continuation field
remains. Conversely, a token alone does not prove more files exist. Neither
reference proves CIRQA's full historical coverage.

## Stage reconstruction for the last 36/37 run

| Pipeline stage | Evidence | Finding |
| --- | --- | --- |
| Device-reported list entries | Terminal report excerpt is truncated; displayed denominator is selected count, not raw list count. | **NOCH UNGEKLÄRT** |
| Selected | UI parser uses `selected=37`. | **BEWIESEN: 37 selected** |
| Pull requests queued | Not shown in preserved terminal excerpt. Request 38 ACK alone is insufficient. | **NOCH UNGEKLÄRT** |
| Full bytes received / file transfer complete | The UI's `successful=36` is an end-to-end success counter; per-file transfer stage totals are not preserved. | **BEWIESEN: 36 successful pipelines; exact receive total unknown** |
| Integrity verified / private archive | Stage totals not preserved for this run. The post-run archive is valid, but its content hashes were already present. | **NOCH UNGEKLÄRT per-run count** |
| FIT decode / SQLite commit | Stage totals not preserved for this run. | **NOCH UNGEKLÄRT per-run count** |
| Terminal | On-screen terminal message explicitly identifies the 120-s session time limit. | **BEWIESEN** |

The product code's other stopping conditions are 256 selected files, 8 MiB per
file, 64 MiB selected bytes and 64 ItemList responses. The observed 37 selected
files are below the count limit; the explicit terminal message is the time
limit, not the file-count or page-limit path. The private archive contains 38
originals totaling 642,709 bytes; its largest original is 366,095 bytes, well
below the 8 MiB per-file archive ceiling. A selected item must also pass the
per-file size check before queueing. No evidence points to the archive ceiling
as the terminal cause. The precise selected-byte total is not available in the
preserved excerpt.

## Freshness, deduplication, and repeated-run budgets

- **BEWIESEN:** `CirqaTargetedPullPolicy.shouldPull()` currently returns true.
  The freshness fix avoids assuming that repeated UID/type/size/flags or a
  previous local hash establish that remote content is unchanged.
- **BEWIESEN:** `DownloadedFileArchive.store()` computes SHA-256 after a
  complete received payload, uses content hash as the original-content key, and
  adds a separate reference association. It cannot avoid radio transfer.
- **BEWIESEN, aggregate before/after:** 38 original content hashes in both
  backups; 0 new hashes, 0 removed hashes; 642,709 original bytes in each;
  SHA-256 checks passed. Reference associations changed 40 → 74 (+34).
  Therefore +34 reference rows do **not** mean +34 new unique contents; they
  associate listed references with content already archived. They do not by
  themselves establish how many payloads were transferred in the run.
- **BEWIESEN:** the 8 MiB archive limit is per original file, not a cumulative
  archive budget. All archived originals were below it. The run's file/byte
  counters reset for a new sync; there is no cumulative 64 MiB budget across
  runs.
- **PLAUSIBEL:** repeated app-entry/manual runs can each re-pull previously
  archived contents and each consume their own bounded session. `MainActivity`
  retries an incomplete automatic run on a later app resume after the
  five-minute attempt cooldown because only a drained run records success.
  This can repeat radio work; it does not cumulatively add bytes to a
  cross-session counter.
- **BEWIESEN, coordination risk in current UI code:** `onResume()` can start the
  automatic sync; the manual action handler does not check
  `automaticHistoricalSyncActive` before closing the current probe and starting
  another. This can replace/cancel an automatic run and makes a manual test's
  request count ambiguous. No evidence establishes that this occurred during
  the FIT session. GUI owner was asked to review a small mutual-exclusion guard;
  this branch does not change UI code.

## Offline before/after data coverage

All analysis used the private app-exported backups outside Git. No raw bytes,
health values, file identifiers, or screenshots are published here.

| Item | Before / after result |
| --- | --- |
| Archived originals | 38 / 38; 34 FIT, 4 opaque non-FIT; 642,709 bytes / 642,709 bytes |
| Largest archived original | 366,095 bytes; no original approaches the 8 MiB archive ceiling |
| FIT decode | 34 / 34 decoded; 16,056 FIT data records |
| Active decoder generations | 34 / 34 FIT sources have an active generation; decoder version 1 |
| Canonical observations | 28,668 / 28,668; 0 added, 0 removed |
| Profile candidates | 28,668 timestamped `PROFILE_VALID` observations; 3 outside display range; 2,183 invalid-sentinel field occurrences excluded |
| Review-only candidates | 2,796; 979 lack a measurement timestamp; not in the canonical dashboard observation set |

| Unknown fields | 109,960 field occurrences retained in/recoverable from originals; they are not guessed into measurements |
| Measurement-time range | 2026-11-15 12:56:41 UTC through 2026-11-23 09:15:00 UTC |
| Receive/validation time | Observation `received_at_ms` extends to 2026-11-23 12:26 UTC; archive reception extends to 18:08 UTC. This is later than measurement time and is not evidence of fresh measurements. |
| Checked recent interval | 2026-11-23 15:13:47–18:17 UTC: 0 canonical observations, 0 recent review candidates, 0 FIT files with recent candidates |

**Answer: no examined stored original contains a measurement in the missing
three-hour interval.** The newest decoded measurement is 09:15 UTC, while later
receive/validation timestamps refer to processing or receipt time. Therefore the
missing recent observations were not filtered out of the inspected originals
by SQLite or Dashboard time selection. The existing Dashboard repository
filters by `measured_at_ms` and reads active FIT generations; it does not
substitute `received_at_ms` for measurement time.

This does not prove the band had the missing measurements or that the S10
listing was exhaustive. It makes file selection/transfer the priority for any
future investigation; the local decoder/database cannot import bytes that are
not in the originals.

## Diagnostic change in this branch

The supplied file `pulselibre-fit-terminal-evidence.patch` was not present in
the checkout, `/tmp/opencode`, the local Git refs, or PR #575's comments during
this review. It could not be integrated verbatim. This branch implements a
reviewed equivalent, limited to terminal evidence:

- add stable `session-time-limit`/`diagnostic-time-limit` terminal reasons;
- report redacted counts for list responses/items, session-page requests,
  transaction follow-ups, selected/pull-requested/completed/failed files,
  remaining queue/current item and byte progress;
- report session cursor / transaction token presence and which kind of list
  continuation request was waiting, without exposing opaque IDs/tokens;
- add synthetic regression coverage for separate continuation kinds and
  timeout states.

No BLE command, retry behavior, timeout, page limit, file-count limit, or byte
limit is changed. These new terminal fields cannot retroactively recover the
missing per-file state from the old truncated S10 screen.

## Findings and remaining risks

- **BEWIESEN:** terminal condition is the existing 120-second session timeout;
  36/37 is successful selected-pipeline count, not history coverage.
- **PLAUSIBEL:** redundant full-item pulls after the freshness fix consumed the
  available time; no transfer-duration evidence attributes the last seconds
  to a particular file.
- **BEWIESEN:** new reference links map to already-held original hashes; no new
  content or canonical observations were created.
- **BEWIESEN:** the missing three-hour measurements are absent from all
  inspected stored FIT originals, so SQLite/dashboard filtering did not lose
  those specific measurements.
- **NOCH UNGEKLÄRT:** raw device-list total, exact request count, stage totals
  for the last run, queue-vs-in-flight-vs-failed state for the 37th selected
  item, and whether a session cursor/transaction token remained at timeout.
- **NOCH UNGEKLÄRT:** whether the device actually held newer measurements,
  whether the final listed item would have carried such data, and complete
  history/retention semantics.

## Exactly one future S10 acceptance run

Only after a new explicit user authorization and valid S10 lease:

1. Install one reviewed signed build that includes the terminal evidence and a
   GUI-confirmed auto/manual mutual-exclusion behavior. Verify build/signature;
   preserve the current user database.
2. Choose exactly one trigger for the test: **app-entry automatic sync only**;
   do not tap the manual button. Confirm the selected policy is due and that
   the activity/session gate allows a single run. No second app resume/retry,
   no repeat, and no separate BLE probe.
3. Export one private pre-run backup, start once, and save the complete
   redacted terminal report (including terminal snapshot) locally. Stop
   observation at the terminal result; no retry if incomplete.
4. Export one private post-run backup. Reconcile counts, new content hashes,
   reference associations, FIT measurement timestamps, active generations and
   canonical observations; keep raw traces, screenshots, hashes and values
   outside Git.
5. Release and verify the S10 lease. A timeout or outstanding state remains
    incomplete; only the measured evidence can determine whether the next work
    belongs in transfer throughput, queue recovery, transaction continuation,
    decoder, or SQLite.

## PR #579 review follow-up: terminal correlation and measured timing

- The runtime no longer derives continuation state from the diagnostic
  `syncIncompleteReason` or last-response cursor/token. A production state
  machine records queued, API-write-accepted, matching response received,
  completed, timed out, aborted, or unknown, keyed by the internal 5043 request
  ID and matching fully reassembled 5044 response ID. A late response cannot
  rewrite a timeout; duplicate transitions are idempotent. Request IDs and
  continuation tokens remain redacted.
- Pull accounting now distinguishes selected and queued files from API-accepted
  BLE writes, correlated application responses, completed transfers, verified
  integrity, original archive, FIT decode/import, SQLite commit, pipeline
  success, and explicit failures. Per-item event counts are idempotent under
  repeated callbacks. An ordered per-run local ordinal and an LSB-first
  stage-bitset preserve file order/progress without emitting UIDs. A timed-out
  in-flight file remains visibly in-flight, with its ordinal and byte progress,
  and is not guessed to be successful or failed. The archive stage distinguishes
  newly archived content from content already present before this transfer, so
  per-item transfer time can be attributed to repeated payloads without
  exposing content hashes.
- Monotonic elapsed time is emitted for the total run, bootstrap, each ItemList response,
  session-page/transaction-follow-up waits, transfer, validation, private
  storage, original archive, FIT decode/import, and SQLite commit. These are
  measured intervals; no estimates are substituted. Active intervals are
  closed at terminal time so timeouts are represented. The 120-second limit is
  unchanged.
- **Historical budget attribution remains unknown:** the retained S10 report
  has no per-stage timing values, so it cannot show which operation consumed
  most of the 120 seconds. Source analysis shows the current freshness policy
  intentionally re-pulls listed content rather than trusting a local hash;
  duplicate content can therefore consume transfer time. The archive evidence
  establishes no new unique content, but cannot establish how many repeated
  payloads crossed BLE in that run.
- **Causation is not established:** the 120-second timeout proves why that
  synchronization run stopped. It does not prove why newer measurements are
  absent. No inspected stored original has a measurement in the checked recent
  interval, but that cannot establish whether the device offered a newer file.
- The preserved S10 evidence is insufficient to recover exact file ordering or
  the 37th selected item's last completed processing stage. In particular, the
  transport ACK is not an application response, and no durable per-item event
  ledger exists for that historical run.
- For the next explicitly authorized run, retain the random run UUID (already
  emitted) and trigger owner (automatic/manual; pending GUI ownership review);
  monotonic total-run duration and terminal cause; listing
  page ordinal and response status/count/cursor-presence flags; a local
  per-run file ordinal (not UID), reached-stage markers for selected, queued,
  BLE API accepted/status, correlated response, transfer complete, integrity,
  archive plus content-new/content-preexisting outcome, FIT decode, SQLite
  commit, success/failure, and expected/received byte totals. Retain monotonic
  durations for bootstrap, list/page waits, per-item transfer, validation,
  storage/archive and FIT/SQLite import;
  and continuation request kind/phase transitions with only request-match
  outcome (never the token). Snapshot queued and current ordinals/byte counts
  at timeout. Keep payloads, file IDs, hashes, session IDs, tokens, and health
  values out of diagnostic output.
- Offline coverage: continuation tracker and callback idempotence unit tests;
  existing empty-session/empty-transaction policy replays; archive tests for
  changed bytes under the same reference and known bytes under a new reference.
  No S10/device access was performed for this follow-up.
