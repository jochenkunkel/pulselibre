# Missing recent CIRQA history: offline freshness audit (2026-11-23)

## Confirmed defect, not yet a device-level causal proof

The latest bounded run returned 37 items, selected 16 (145770 bytes), skipped
21, and imported no new observations. Manifest-skip diagnostics are visible.
The report does not independently break all 21 skips down by reason.
It does **not** establish that the missing recent interval is absent on the band.

`CirqaMlHandshakeProbe.shouldPullTargetedItem()` checks the old local FIT hash,
length, CRC and SQLite provenance. It then asks `CirqaTargetedPullPolicy` whether
the listed UID/type/size/flags appeared in three accepted sessions.
That proves **local integrity and repeated metadata, not remote freshness**.
The remote content hash is neither requested nor compared.

`CirqaFileAccessItemListParser.Item` also discards ItemReference transaction_id
(field 5) and modified_time (field 7). The generated Connect 5.31 schema confirms
these field numbers. Two remote versions with unchanged UID/type/size/flags
therefore collapse to the same manifest key even if these fields differ.
There is no demonstrated CIRQA guarantee that an unchanged UID/size implies
immutable contents. Three repeated sightings cannot establish such a guarantee.

### Executed synthetic reproduction

`tools/research/CirqaRemoteFreshnessRepro.java` uses the actual compiled Kotlin
parser, manifest and policy, not a Python reimplementation. It constructs two
synthetic references with equal UID/type/size/flags but different transaction
and modified-time metadata, and two different same-length synthetic payloads.
After three sightings and one recorded completed import:

```text
remote_content_changed=true; remote_size_changed=false
remote_transaction_and_modified_time_changed=true
manifest_identity_changed=false; shouldPullChangedContent=false
control_changed_size_shouldPull=true
REPRODUCED: local integrity + repeated metadata do not prove remote freshness
```

This is a **reproduced unsafe selection decision**, not proof that the real
missing interval used a reused UID. No new band request or download was made.
The payloads only demonstrate content inequality; they are not claimed to be
FIT files or device measurements.

Compile gate: `:app:compileDebugKotlin --offline` passed with JDK 21 / Gradle
9.6.0 and the installed Android SDK. The first invocation without ANDROID_HOME
failed configuration; after setting the SDK path it succeeded. This is not
FULL-CI or a new APK/device test.

To run after compiling the app, use JDK 21 `javac` with the classpath
`app/build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes`
plus the cached Kotlin 2.4.21 stdlib JAR; compile the Java file to an external
temporary directory, then run `CirqaRemoteFreshnessRepro` with that directory
and the same classpath. `REPRODUCED` means the defect exists, not a correct policy.

## ChatGPT/Connect comparison: second independent gap

Reviewed the ChatGPT issue #506 analysis, including the G4 original FIT proof,
and `CIRQA_GARMIN_HCI_WIRE_IMPLEMENTATION_SPEC_2026-11-22.md`. The old statement
that no complete FIT bytes could be recovered is superseded for the two
specifically verified files; it is not a reason to stop protocol investigation.

The product `CirqaFileAccessListRequest.smartRequest()` serializes only
session_id **or** transaction_id. It cannot preserve Connect's agent-specific
exclude_all_flags/get_flags/included_data_types filter context (fields 4/5/6).
Those field meanings are confirmed by the generated schema; UUID values must
not be guessed.

A narrowly scoped independent check of the SHA-verified 5,522,737-byte capture,
restricted to complete single-ATT CRC-valid frames in the last-epoch range,
found requests at records 83937/83949 and token-bearing follow-ups at
83981/84024, carrying respectively fields 4+5 and 5+6 in addition to the token.
The script remained local at `/tmp/opencode/audit_item_reference_fields.py`;
it is **not** a full G0–G4 replay and does not supersede #506/#522 ownership.
It prints field numbers/wire types and record numbers, not UUIDs or payloads.

Crucial counterexample: six observed Connect pull references in this subset
contained only UID (field 1), not transaction_id. Thus one must **not** conclude
that adding transaction_id to PullItemRequest fixes the download. The existing
product comment claiming an “exact ItemReference” is inaccurate, but UID-only
pulls are present in the reference trace.

The token-only follow-ups differ from Connect's filtered transaction contexts.
That is a confirmed implementation mismatch, **not yet proof** of why the
recent interval is missing. Repeating the same token more times is not a
validated solution.

## Corrections to result interpretation

- `noImportableData=12` means no observations passed the current importability
  predicate in those files. It is not a counter of deduplicated observations.
- `ALREADY_PRESENT` for displayed file details proves content equality for
  those displayed downloaded files only, not for skipped remote references.
- Successful transfer of the selected subset is not a successful recent-history
  sync. Its temporal coverage was not checked in that run.

## Required remedy / verification

1. Do not skip remote files solely on repeated metadata plus a local hash.
   Until a verified immutable remote revision/content identity exists, pull
   within the existing safety budgets and deduplicate **after** hashing the
   received bytes. Keep every previous content version.
2. Preserve available item revision/modified metadata and exact provenance
   locally; absent revision metadata must never grant permission to skip.
3. Compare canonical and raw-field timestamp coverage with the requested recent
   interval locally, including status-only and unknown records. Do not infer
   absence on the band from absence in the UI.
4. Reconstruct the reference client's filter/agent transaction semantics offline
   before changing active requests. Do not guess UUIDs or invoke SetFlags.
5. The user's goal is **all available band data in the local canonical database**,
   not merely understood/displayable measurements. Preserve unknown/status/raw
   records with source/version/quality; do not present them as validated health
   measurements. File-only storage of opaque originals is not by itself proof
   that all their contents are represented in SQLite.

The user now refers to a personal recent activity. Treat newer source data as
potentially personal, not automatically synthetic: no real time series, payloads
or identifiers are published by this audit. Existing older synthetic fixtures
and generated reproduction data remain separate.
