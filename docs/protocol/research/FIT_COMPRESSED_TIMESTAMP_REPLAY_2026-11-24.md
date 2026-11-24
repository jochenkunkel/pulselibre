# FIT compressed timestamps: interim offline audit (#628)

Status: **S10 replay and isolated SQLite import/reimport verified; historical
device acquisition remains incomplete.** No private FIT bytes, identifiers or
source fingerprints are published here. `coverage=45-file-archive-only`.

## Reference and synthetic reproduction

Garmin's [FIT protocol, Figure 10](https://developer.garmin.com/fit/articles/fit-protocol/fit_protocol.html)
describes a full timestamp in one local definition and compressed records with
another definition that contains no field 253. Five-bit offsets wrap modulo 32;
equal offsets have zero difference.

On baseline `fb9ba205`, the synthetic CRC-valid case in
`FitHistoricalObservationsTest.compressedTimestampUsesDifferentDefinitionWithoutTimestampField`
failed with a null decode. Independently, `python-fitparse` at commit
`d88bb6997138c38d0c578aeaaf4108df0711d1f6` decoded the same generated
layout: one full anchor, then two local-type-1 records; offsets produced +3 s
and +3 s. This is a synthetic test, not a device-history result.

For a compressed record whose definition **does** include field 253,
`python-fitparse` reads the definition's four timestamp bytes before the other
fields; when those bytes were omitted the test ended with `FitEOFError`. The
old Garmin Java SDK mirror's `Decode.java` also processes the defined fields
after a compressed header. The previous PulsLibre test fixture instead omitted
those bytes. No backwards-compatibility claim is made for malformed files.

Targeted local tests for missing anchor, local types 0–3, rollover, bad CRC,
the absolute/relative boundary and sentinel/`timestamp_16` fallback passed.
The FIT epoch offset remains 631065600 seconds; Garmin identifies the epoch as
1989-12-31 UTC and `date_time` below `0x10000000` as relative
([cookbook](https://developer.garmin.com/fit/articles/cookbook/datetime.html)).
Local time and a device receive time must not be substituted for UTC FIT time.
The current decoder accepts one FIT envelope and deliberately rejects chained
FIT files rather than partially importing the first member; chain support is
not implemented by this fix and is covered by a fail-closed regression.

## Existing originals: preliminary, not a rescue count

Read-only comparison used 45 distinct older S25 archived FIT files, not the
later S10 run. Baseline and local candidate each decoded all 45 and produced
29,365 observations with timestamps and 29,358 distinct quality-eligible
candidates after exact metric/time/value fingerprint deduplication. Candidate
counts by metric were identical: heart rate 14,352; distance 14,911; speed 95.
The locally compared fingerprint sets had 0 candidate-only and 0 baseline-only
entries. This does **not** prove SQLite commits or a result for other files.
No new distinct importable candidate was observed in this particular inventory.
The previously documented 1,622 timestamp-16 heart-rate values are not counted
as new recoveries.

| S25 metric | Baseline | Candidate | Difference |
| --- | ---: | ---: | ---: |
| Unique source files | 45 | 45 | 0 |
| Successfully decoded files | 45 | 45 | 0 |
| Decoded observations | 29,365 | 29,365 | 0 |
| Timestamped observations | 29,365 | 29,365 | 0 |
| Missing timestamps | 0 | 0 | 0 |
| Distinct quality-eligible candidates | 29,358 | 29,358 | 0 |
| New SQLite imports | not tested | not tested | unverified |
| Historical hours/days newly covered | not tested | not tested | unverified |

An existing S25 backup snapshot contains all 45 source identities and all
29,358 candidate sample identities in its canonical FIT observation provenance.
An additional per-sample comparison against its canonical observation and
provenance entries found zero mismatches in metric, value, unit, quality or
timestamp for those 29,358 entries. This is a read-only comparison with an
existing snapshot, not a new SQLite commit or verification of the later S10 run.

An additional per-source/per-record comparison matched all 29,365 observations
and all 178,816 emitted decoded-field occurrences between the two runs. There
were no missing or added identities and no changed observation metric, value,
unit, quality, timestamp or timestamp origin. For the field occurrences,
encoded bytes, base type, length, byte order, timestamp and timestamp origin
also matched. This comparison covers decoder-emitted fields in these 45 files,
not unsupported interpretations, review-candidate version identifiers, other
files, or device-history completeness.

The aggregate tool is `tools/Issue628Replay.java`. Compile it against each
version's Gradle `compileDebugKotlin` classes and the Kotlin standard library;
run both against the same private directory. An optional second argument writes
fingerprints with `CREATE_NEW` for set comparison: use `umask 077` and a
private local directory, and never commit or publish that file. The tool checks
FIT CRC through the production decoder and deduplicates original bytes by
SHA-256 before decoding; it does not open or modify SQLite.
`tools/Issue628FieldDiff.java` similarly emits private hashed comparison rows;
compile/run once against each decoder, then compare the identity, raw,
timestamp and origin columns. Derived hashes must remain private too.

The documented 23 November S10 before/after exports and extracted FIT copies
under `/tmp/opencode/all-data-s10-acceptance-20261123/` did not survive the host
restart. A subsequent read-only S10 inventory found 11 project backup ZIPs in
shared storage. They were copied byte-for-byte under the S10 device lease to a
permission-restricted local evidence directory outside the repository; source
and copy SHA-256 values matched, all ZIP CRC checks passed, and a complete
per-file manifest was retained. The 45 FIT originals are content-addressed,
read-only, and verified again before/after device instrumentation. No private
FIT bytes, paths, names, hashes or sample identities are published here.

The same 45 originals were independently replayed using decoder v2 from the
then-current `main` and decoder v3 from PR #630:

| S10 replay result | main v2 | PR v3 | Change |
| --- | ---: | ---: | ---: |
| Unique source files | 45 | 45 | 0 |
| Successfully decoded files | 45 | 45 | 0 |
| Decoded observations | 29,032 | 29,032 | 0 |
| Timestamped observations | 29,032 | 29,032 | 0 |
| Missing timestamps | 0 | 0 | 0 |
| Quality-eligible distinct observations | 29,028 | 29,028 | 0 |
| Field comparison rows (decoder-emitted) | 200,772 | 200,772 | 0 |
| Additional SQLite-eligible observations from v3 | — | — | **0** |

Both runs had identical eligible sample/fingerprint sets and identical
per-field/per-observation comparisons (encoded field bytes, metric/value/unit,
quality, timestamp and timestamp origin): no added, missing or changed rows.
There were 14,985 distance, 13,948 heart-rate and 95 speed candidates. The
29,028 importable observations span 2026-11-15 12:56:41 UTC through
2026-11-24 06:40:00 UTC. Among importable observations, timestamp provenance
was 27,884 `DIRECT` and 1,144 `RECONSTRUCTED_TS16`. Four further timestamped
observations were outside the profile display range. These 45 files do not
establish complete device history or any data beyond that date range.

### SQLite evidence

The recovered version-9 S10 backup created at 2026-11-24 10:16:38 UTC (before
the later 24 November app run) contains all 45 exact FIT source hashes, 45 FIT
source rows, 29,028 canonical observations and 29,028 provenance rows. A private
row-by-row comparison against the v2 replay matched every observation's source,
sample identity, metric, timestamp, value, unit and quality (29,028 matches;
zero missing, extra or mismatched). Every source/provenance row in that backup
was tagged decoder version 1. Thus **29,028 of the 29,028 v3-importable
observations were already present in that pre-run S10 SQLite backup**; the
24 November run and v3 fix added no demonstrated new measurement.

The optional `Issue628S10RecoveryTest` has two isolated-database paths, both run
on the authorized S10 with private fixture arguments. A clean empty-database
import verifies:

| Import check | Result |
| --- | ---: |
| First pass: files seen / imported / invalid / failed | 45 / 45 / 0 / 0 |
| Candidate observations / committed observations | 29,028 / 29,028 |
| SQLite `observations` / provenance rows | 29,028 / 29,028 |
| Sources / decoder generations / generation observations | 45 / 45 / 29,028 |
| Persisted decoded-field occurrences | 171,740 |
| Second pass result | exactly equal to first pass |
| Source row hashes after second pass | unchanged |
| Generation output digest/count snapshot | unchanged |
| SQLite `quick_check` / foreign-key check | `ok` / 0 violations |

That clean-database test verifies source hashes before/after and uses a
temporary database, not the app's live data store. The second path restores the
actual pre-run v9 S10 backup into another temporary database, then reimports
the same 45 files:

| Restored-v9 reimport result | Count |
| --- | ---: |
| Files seen / invalid | 45 / 0 |
| Files imported (20 with observations, 20 without) / failed | 40 / 5 |
| Failure stage | 5 `SOURCE_CONFLICT` |
| Eligible / committed observations | 29,028 / 27,884 |
| Final observations / provenance rows | 29,028 / 29,028 |
| Sources / decoder generations | 45 / 85 |
| Generation observation rows | 56,912 |
| Old active decoder-v1 generations retained | 45 |
| Second restored-v9 reimport result | exactly equal to first pass |
| SQLite `quick_check` | `ok` |

The five repeatable source conflicts were narrowed by retrying each failed file
inside the isolated test transaction: all five reach the committer's
`FIT review candidate conflicts within one decoder version` guard. The
corresponding source metadata rows matched the current replay; the available
comparison does not establish which candidate field(s) differ. The other 40
files complete without error, including 20 with no importable observations.
The five conflicts are from this controlled current reimport and are **not
identified as any particular subset of the historical 20 failures**.

Private fixtures remain outside Git and in read-only storage. CI skips the
device-evidence tests unless fixture arguments are explicitly supplied.

The 24 November terminal ledger documented 32 FIT decodes, 12 commits without
importable data and 20 import/commit failures. The recovered version-9 backups
pre-date the versioned import audit tables and contain no per-file exception
classification. The available exported event rows likewise contain no
per-file error details. Therefore the causes of those historical 20 remain
**unknown**, not zero. The five review-candidate conflicts on the separate
restored-v9 replay are confirmed only for those five controlled files; they do
not explain the historical failures. No historical failure is attributed to
decoder, timestamp, quality, source/version conflict or SQLite without
per-file evidence.

Relevant failure classes were independently exercised and passed:

- FIT unit tests reject invalid CRC, truncated/chained files, missing compressed
  time anchors, relative values reused as absolute timestamps and invalid
  timestamp sentinels.
- S10 `HistoricalObservationRepositoryTest`: 6/6 passed, including invalid
  verified bytes, unchanged-sample decoder-version reimport, conflict rejection
  and immutable provenance/generation checks.
- S10 `CirqaFitBackfillInstrumentedTest`: 2/2 passed. An injected SQLite
  trigger failure was classified `DATABASE`; the transaction left zero source,
  observation or provenance rows, and retry imported atomically.
- These synthetic/recovered-file tests do **not** identify which particular
  historical 20 failed or prove their individual causes.

## Integration gate

Generation identities include the decoder version and are immutable. The
pre-run S10 backup's decoder-v1 observations match the v2/v3 replay values
exactly, but no v3 reimport into a restored copy of that full backup has yet
been run. On these recovered originals, v2 and v3 are
field/observation-identical; v3 therefore yields zero additional measurements.
The clean isolated v3 SQLite test establishes 29,028 stored observations and
repeatable reimport. Restoring the pre-run v9 backup and reimporting v3 retained
all 29,028 existing observation/provenance rows, added 40 decoder generations,
and hit five repeatable review-candidate conflicts; it did not create new
measurements. The historical 20 exception causes remain unclassified.

At this documentation update, PR #630 was still Draft and required current-head
FULL/API-35 CI and integration gates had not yet been rechecked after the new
instrumentation test. Issue #628 remains open until those gates are met and
the documented broader-history acquisition gaps are addressed.
