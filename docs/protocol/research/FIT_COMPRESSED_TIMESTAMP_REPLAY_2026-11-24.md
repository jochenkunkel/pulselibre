# FIT compressed timestamps: interim offline audit (#628)

Status: **work in progress; no recovered SQLite values claimed.** No device
access, BLE commands, private FIT bytes, identifiers or source fingerprints
are published here. `coverage=device-history-unverified`.

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
under `/tmp/opencode/all-data-s10-acceptance-20261123/`, and the 24 November
pre-run backup under `/tmp/opencode/p0-s10-run-20261124/`, did not survive the
host restart. A bounded search of project workspaces, Downloads, Documents and
Desktop found no replacement S10 backup; the two located S25 backups are not
S10 evidence. No post-run backup was made for the 24 November run. Consequently
the requested S10 before/after FIT replay and the additional recovered values
and time ranges are **not verified**.

The 24 November terminal ledger documented 32 FIT decodes, 12 successful
commits with no importable data, and 20 failures in the subsequent
import/commit path. The recorded aggregate does not separate SQLite execution,
source conflicts, or other import failures, and no per-error exception details
or post-run audit snapshot have been found. Thus decoder failures attributable
to those 20, timestamp failures, quality-filter failures, individual SQLite
failures and other causes are all **unknown**, not zero. No one of those 20
has yet been demonstrated fixed by this PR.

## Integration gate

Generation identities include the decoder version and are immutable. The normal
commit path also checks existing observation provenance, including decoder
version. The candidate uses version 3 and preserves existing provenance when
an unchanged sample is reimported; changed sample identities still fail
closed. Replay filters non-importable observations and remains inactive until
explicit activation. The new SQLite instrumentation test and backup/restore
effects require GitHub CI before integration. SQLite commit evidence for
private data, S10 error classification and GitHub CI remain **unverified**.
