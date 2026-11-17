# CIRQA File Transfer – PulsLibre-Implementierungsplan

**Status:** Vorbereitung/Offline-Decoder. Derzeit kein verifizierter CIRQA-Dateiliste- oder Downloadpfad. Keine neue BLE-Sendeoperation in diesem Plan.

## Entscheidung zuerst

Nicht den Protobuf-`GDIFileAccess.ItemListRequest`-Pfad und Legacy-GFDI mischen. Connects nachgewiesener `v5d/hqi/a7d`-Pending-Upload-Pfad nutzt 5007/5031/5002/5004; Protobuf `ItemListRequest` gehört zum separaten CoreTransfer-Agentpfad. CIRQA hat `0x8A39` bereits als unsupported abgewiesen und nach 5050-ACK kein 5037/Filesync gezeigt. Ein aktiver Historientest ist aktuell **nicht ausreichend begründet**.

## Zustandsautomat für späteren, ausschließlich lesenden Transfer

```text
IDLE
 -> GATT/ML-SESSION-OWNED
 -> GFDI-HANDSHAKE-COMPLETE (nur bei belegtem Zustand)
 -> WAIT_DIRECTORY_FILTER_5007
 -> WAIT_SUPPORTED_TYPES_5031 (optional/cache)
 -> REQUEST_DIRECTORY_INDEX_0
 -> READ_DIRECTORY_CHUNKS
 -> VALIDATE_DIRECTORY
 -> SELECT_READABLE_SUPPORTED_ENTRIES
 -> for each file index:
      REQUEST_FILE
      READ_CHUNKS
      VALIDATE_SIZE + transfer CRC + format CRC
      ATOMIC_LOCAL_COMMIT
 -> COMPLETE

Any negative status, unexpected frame, timeout, size limit, CRC failure,
connection loss or user cancel -> ABORT; discard temp file, keep no completion cursor.
```

No transition to archive/erase/write/flag mutation. Do not send a guessed request until field widths and CIRQA route are evidenced.

## Layered implementation sequence

### Stage 0 — Current evidence and source hygiene

* Keep CIRQA vs Connect evidence labels at each API and test.
* Do not add proprietary source, APK, raw personal captures, device address, health payloads, or Garmin source excerpts.
* User-initiated BLE session must acquire the existing exclusive session gate; stop live/night recorder and resume it reliably. No parallel GATT sessions with Garmin Connect.
* Keep `CirqaLegacyDirectoryObserver` passive. Do not wire the parser into a transmitter.

### Stage 1 — Offline formats (safe to implement now)

Existing `CirqaLegacyDirectoryParser` and `CirqaLegacyFileTypesParser` are offline-only. Expand/retain tests for:

* Directory: 16-byte opaque header, entry sizes, unsigned size/timestamp, 0/1/multiple entries, truncation, unknown type/flags, high indexes and `Long` boundaries.
* 5031: count 0, many types, truncated count/tuple/name, invalid UTF-8, duplicate types and bounded count/name lengths.
* 5004: CRC-16/ARC reference vectors (DEX confirms comparison uses the updated accumulator), offset ordering, duplicate chunk, wrong offset, malformed length, CRC error, data beyond final size, no double append. Keep the verifier injected/offline until CIRQA semantics are evidenced.
* Compressed 5004: bounded inflater, sequence wrap, stream-final flag, corrupt DEFLATE, CRC over expanded content, decompression bomb limits.
* 5002: exact 14-byte request vectors (u16LE/u32LE), unsigned u32LE response, status mapping, overflow/size bound, and one 1-second `NOT_READY` retry. This remains Connect-only evidence.

All fixtures synthetic. Avoid testing on live GATT or committing real packet bytes.

### Stage 2 — Safe persistence primitives

* Stream to app-private temporary file; enforce configurable maximum per-file, total bytes, file count, and session deadline before allocations/writes.
* Track received offset, expected total size and chunk sequence in a bounded transfer object. Persist only after a validated complete transfer; resume is disabled until device/offset restart semantics are verified.
* Compute transport/file integrity checks and validate FIT header/file CRC before atomic rename.
* Derive idempotency key from stable device-local pseudonymous identity plus file index/type/subtype/size/timestamp and content hash; do not rely on file index alone across sessions.
* Store original bytes unchanged; parse/import in a separate transaction. Incomplete temp files never become visible as completed history.
* Keep provenance and UTC/device timestamp distinct; unknown timezone and unknown field values remain unknown.

### Stage 3 — CIRQA evidence gate (not currently satisfied)

Require a newly reviewed, specific hypothesis and separate user approval before any device command. Needed prerequisites:

1. CIRQA-specific route evidence (observed incoming message or independently validated reference).
2. Correct 5050/handshake state and exclusive GATT owner.
3. Exact request serialization, response status and negative-ACK handling from source/fixture or passive CIRQA evidence.
4. Stop-on-first-unexpected/error, bounded one-shot request, strict time/size limits, no automatic fallback to alternate protocol.
5. Demonstrate read-only semantics; do not issue 5007 unless filter-state side effects are ruled out.

No user authorization in this document activates BLE behavior.

### Stage 4 — Only after one file succeeds

* Start with one small synthetic/explicitly authorized test file only.
* Confirm received length equals declared size, transport integrity and FIT CRC; record only redacted metadata.
* Repeat once to verify local idempotency and no device mutation.
* Then test interruption and recovery; no resumable requests unless offset semantics are proven.
* Expand to historical file classes only after subtype meanings and schema mappings are evidenced.

## Test and integration order

1. `:app:testDebugUnitTest` for pure parser/state logic.
2. `:app:lintDebug` and `:app:assembleDebug`.
3. Review actual GATT call sites, session ownership, stop gates and no-mutation invariant.
4. CI outcome verified for exact commit; this does not establish CIRQA compatibility.
5. Separate user-approved device experiment only after Stage 3 passes; no FileSetFlags/archive/erase/CLOSE_ALL.

## Failure policy / secure defaults

* Reject malformed frames, unknown response statuses and unsupported command types.
* No retry for CRC/offset errors except peer-driven retransmission handled idempotently; 5002 `NOT_READY` one retry after 1 s in Connect, not automatically CIRQA.
* On three consecutive invalid uncompressed data chunks Connect aborts; implementation should abort conservatively and close the session.
* Do not resume based on stale session/counter. Discard partial output on disconnect unless durable, validated resume protocol exists.
* Never mark a partial file complete, never send archive/erase/write/flags, never log raw health content.

## Acceptance criteria before claiming historical download

* One CIRQA directory response decoded from validated frames.
* One real explicitly authorized CIRQA file fully read, expected length matched, file-level CRC validated, and persisted atomically.
* No device-mutating operation in the captured/reviewed send path.
* Repeat download yields one local item (idempotent); disconnect does not create a completed partial.
* Tests and CI status for exact commit recorded. A successful GFDI registration, 5050 ACK, 5037 signal or Connect source analysis alone does not meet this bar.

## Offline-Integritätsschritt (2026-11-17)

`CirqaLegacyOfflineFileSession.completedVerifiedFit()` gibt einen vollständigen, nicht als Directory-Index 0 behandelten Dateiinhalt ausschließlich dann als defensive Kopie heraus, wenn die 5004-Chunks durch den **injizierten** Transport-CRC-Prüfer akzeptiert wurden und zusätzlich `FitFileHeader.inspectVerified` den FIT-Header und die FIT-Datei-CRC validiert. Tests umfassen eine synthetische FIT-Datei mit gültiger CRC, korrupte Datei-CRC und den Directory-Ausschluss. **Wichtig:** Die injizierte Test-CRC ist nicht der nachgewiesene Garmin-Transportalgorithmus; der Baustein ist weder an BLE noch an Dateipersistenz angeschlossen. FIT-CRC ist eine Integritätsprüfung, kein Herkunfts-/Authentizitätsbeweis. Die Stufe `ATOMIC_LOCAL_COMMIT` bleibt offen.

## Offline FIT-Persistenz (2026-11-17)

`CirqaVerifiedFitStore` nimmt nur vollständig FIT-CRC-validierte Bytes an und legt sie in einem **vom Aufrufer vorgegebenen app-privaten Verzeichnis** unter SHA-256-Content-Hash-Namen ab. Die Datei wird zunächst als temporäre Datei im selben Verzeichnis geschrieben und synchronisiert und anschließend mit `ATOMIC_MOVE` veröffentlicht; ohne atomaren Move gibt es keinen Copy-Fallback. Bestehende Dateien werden vor Wiederverwendung bytegenau geprüft, defekte bestehende Inhalte führen zum Fehler. Die Ablage ist derzeit bewusst **nicht** an den BLE-Pfad, eine UI oder `LocalReadingStore` angeschlossen. Die FIT-CRC beweist keine Geräteherkunft; eine echte Transport-CRC und CIRQA-Evidenz fehlen weiterhin. Kein Resume, kein Archivieren, kein Sync-Cursor-Advance.
