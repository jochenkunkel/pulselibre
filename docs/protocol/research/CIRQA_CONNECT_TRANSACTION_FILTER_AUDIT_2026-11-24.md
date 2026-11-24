# CIRQA / Connect transaction-filter audit (2026-11-24)

## Scope and method

This is an offline, read-only audit of the already-authorized local S10 Garmin
Connect capture. The bounded parser accepts only complete single-ATT GFDI
5043/5044 frames with valid outer CRC and complete protobuf bodies in the
selected last-epoch record range. It is not a full connection replay, and it
does not establish what happened outside that range. No payload values, UIDs,
UUIDs, timestamps, hashes, or device identifiers are published here.

The private helper `audit_item_reference_fields.py` checked protobuf field
numbers and wire types only. Its capture digest was verified locally against
the expected source before parsing. The raw capture and helper remain outside
Git.

## Reproduced observations

- 117 complete, CRC-valid protobuf frames were in scope.
- Six host PullItemData requests were present; all six carried only Item UID
  field 1 in their ItemReference. None carried ItemReference transaction ID
  field 5 or flags field 4.
- Three device ItemList responses were in scope; one listed ItemReference had
  its own transaction-ID field 5.
- Six complete host ItemList requests were observed. Their nested request field
  shapes were:
  - fields 4 and 5;
  - fields 5 and 6;
  - transaction field 2 together with fields 4 and 5;
  - transaction field 2 together with fields 5 and 6;
  - then the first two shapes repeated.
- For the included-data-type field 6, the parsed nested shape was a data-type
  format value; the observed value is intentionally not copied here.

These observations reproduce the earlier bounded audit in
`CIRQA_RECENT_HISTORY_FRESHNESS_AUDIT_2026-11-23.md`; this run revalidated it
against the local capture. It is not evidence that the filters caused an empty
or incomplete CIRQA list.

## Comparison and implications

`CirqaFileAccessListRequest.smartRequest()` currently emits only the separate
transaction token (request field 2) for a transaction follow-up. Connect's
observed transaction requests also carried fields 4/5 or 5/6. The generated
schema identifies these as `exclude_all_flags`, `get_flags`, and
`included_data_types`; the UUIDs and originating agent-filter configuration
must not be guessed.

The mismatch is a testable hypothesis, not a root-cause finding. Connect's
static flow obtains those values from agent-specific filters, while the
PulseLibre goal is an unfiltered full inventory. Copying one captured agent's
filter could exclude categories or alter file-selection semantics. The capture
also shows UID-only pull requests, so adding item transaction IDs to pulls is
not justified by this evidence.

## Next offline checks / guardrails

1. Keep parsing and preserving every listed ItemReference and unknown field.
2. Add synthetic wire-shape tests for optional list-filter fields only after
   their value sources and all-data semantics are explicitly established.
3. Do not send guessed filter UUIDs, SetFlags, archive, delete, reset, or other
   state-changing requests.
4. Before changing the active transaction request, compare the generated schema,
   Connect filter construction, and CIRQA-specific responses; then obtain the
   required independent CI review.
5. Keep the existing page/file/byte/time limits. A limit hit remains an
   incomplete run, not proof of coverage.

No device command was sent for this audit.
