package de.pulselibre

import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

/**
 * Read-only, bounded inventory of existing app-private, content-addressed FIT files.
 * Run on a worker thread; never call on the Android UI thread.
 * No raw health values, filenames, or payloads are returned.
 */
internal object CirqaUnknownFitInventory {
    const val MAX_FILES = 256
    const val MAX_FILE_BYTES = 16 * 1024 * 1024
    const val MAX_TOTAL_BYTES = 64L * 1024 * 1024
    const val MAX_SIGNATURES = 50_000
    private val namePattern = Regex("^[0-9a-f]{64}\\.fit$")

    data class Scan(
        val filesChecked: Int,
        val filesRejected: Int,
        val truncated: Boolean,
        val fields: List<FitHistoricalObservations.UnknownField>,
        /** Validated source metadata grouped for idempotent SQLite replay. */
        val sources: List<VerifiedSource> = emptyList()
    )

    data class VerifiedSource(
        val sourceSha256: String,
        val fileBytes: Long,
        val fields: List<FitHistoricalObservations.UnknownField>
    )

    fun scan(root: File, cancelled: () -> Boolean = { false }): Scan {
        if (!root.isDirectory || Files.isSymbolicLink(root.toPath())) return Scan(0, 0, false, emptyList())
        val files = root.listFiles()?.asSequence()
            ?.filter { it.name.matches(namePattern) }
            ?.sortedBy { it.name }
            ?.toList() ?: return Scan(0, 0, false, emptyList())
        val result = ArrayList<FitHistoricalObservations.UnknownField>()
        val sources = ArrayList<VerifiedSource>()
        var checked = 0
        var rejected = 0
        var total = 0L
        var truncated = files.size > MAX_FILES
        for (file in files.take(MAX_FILES)) {
            if (cancelled()) { truncated = true; break }
            if (!file.isFile || Files.isSymbolicLink(file.toPath()) ||
                file.length() !in 1L..MAX_FILE_BYTES.toLong() ||
                total + file.length() > MAX_TOTAL_BYTES) {
                rejected++
                if (total + file.length() > MAX_TOTAL_BYTES) { truncated = true; break }
                continue
            }
            total += file.length()
            checked++
            val decoded = try {
                val bytes = file.readBytes()
                val hash = MessageDigest.getInstance("SHA-256").digest(bytes)
                    .lowerHex()
                if (hash != file.name.removeSuffix(".fit")) null
                else FitHistoricalObservations.decode(bytes)?.let { bytes.size.toLong() to it }
            } catch (_: java.io.IOException) {
                null
            }
            if (decoded == null) {
                rejected++
            } else {
                val (bytes, inventory) = decoded
                if (inventory.unknownFields.size > MAX_SIGNATURES - result.size) {
                    truncated = true
                    break
                }
                result.addAll(inventory.unknownFields)
                if (inventory.unknownFields.isNotEmpty()) {
                    sources.add(VerifiedSource(
                        sourceSha256 = file.name.removeSuffix(".fit"),
                        fileBytes = bytes,
                        fields = inventory.unknownFields
                    ))
                }
            }
        }
        return Scan(checked, rejected, truncated, result, sources)
    }

    /**
     * Replays a bounded scan (which can be marked partial) into the canonical FIT store.
     * Call from a worker thread. Each source is committed transactionally, so a
     * partial run can safely be repeated; review statuses survive replay.
     */
    fun persist(scan: Scan, store: FitObservationStore, validatedAtEpochMillis: Long): Int {
        require(validatedAtEpochMillis >= 0)
        require(scan.sources.size <= MAX_FILES) { "FIT inventory exceeds source-count limit" }
        require(scan.sources.sumOf { it.fileBytes } <= MAX_TOTAL_BYTES) {
            "FIT inventory exceeds aggregate byte limit"
        }
        require(scan.sources.sumOf { it.fields.size } <= MAX_SIGNATURES) {
            "FIT inventory exceeds signature-count limit"
        }
        var persisted = 0
        for (source in scan.sources) {
            require(
                source.sourceSha256.matches(Regex("[0-9a-f]{64}")) &&
                    source.fileBytes in 1..MAX_FILE_BYTES.toLong() &&
                    source.fields.all { field ->
                        field.sourceSha256 == source.sourceSha256 &&
                            field.globalMessageNumber in 0..65535 &&
                            field.fieldNumber in 0..255 && field.size in 1..255 &&
                            (field.baseType == null || field.baseType in 0..255) &&
                            (field.developerDataIndex == null || field.developerDataIndex in 0..255)
                    }
            ) { "Invalid FIT inventory source" }
            val fields = source.fields.map(::toStoredField)
            store.commitFitFile(
                sourceSha256 = source.sourceSha256,
                fileBytes = source.fileBytes,
                relativePath = "verified_fit/${source.sourceSha256}.fit",
                validatedAtEpochMillis = validatedAtEpochMillis,
                decoderVersion = FitHistoricalObservations.DECODER_VERSION,
                profileVersion = FitHistoricalObservations.PROFILE_VERSION,
                observations = emptyList(),
                unmappedFields = fields
            )
            persisted++
        }
        return persisted
    }

    /** Bounded offline scan + persistence entry point; caller owns worker scheduling. */
    fun scanAndPersist(
        root: File,
        store: FitObservationStore,
        validatedAtEpochMillis: Long,
        cancelled: () -> Boolean = { false }
    ): Pair<Scan, Int> {
        val scan = scan(root, cancelled)
        return scan to persist(scan, store, validatedAtEpochMillis)
    }

    internal fun toStoredField(field: FitHistoricalObservations.UnknownField): FitFieldInventoryRecord =
        FitFieldInventoryRecord(
            sourceSha256 = field.sourceSha256,
            globalMessageNumber = field.globalMessageNumber,
            kind = if (field.kind == FitHistoricalObservations.UnknownKind.DEVELOPER_FIELD)
                StoredFitFieldKind.DEVELOPER else StoredFitFieldKind.STANDARD,
            fieldNumber = field.fieldNumber,
            baseTypeCode = field.baseType,
            fieldSize = field.size,
            developerDataIndex = field.developerDataIndex,
            classification = when (field.kind) {
                FitHistoricalObservations.UnknownKind.UNKNOWN_MESSAGE -> "MESSAGE_NOT_DECODED_BY_APP"
                FitHistoricalObservations.UnknownKind.UNKNOWN_STANDARD_FIELD -> "FIELD_NOT_DECODED_BY_APP"
                FitHistoricalObservations.UnknownKind.DEVELOPER_FIELD -> "DEVELOPER_FIELD_NOT_DECODED_BY_APP"
                FitHistoricalObservations.UnknownKind.INCOMPATIBLE_KNOWN_FIELD -> "KNOWN_MAPPING_INCOMPATIBLE"
            },
            definitionCount = field.definitionCount.toLong().coerceIn(0L, 1_000_000L),
            observedMessageCount = field.occurrenceCount.toLong().coerceIn(0L, 1_000_000L),
            validElementCount = null,
            invalidElementCount = null,
            decoderVersion = FitHistoricalObservations.DECODER_VERSION,
            profileVersion = FitHistoricalObservations.PROFILE_VERSION
        )
}
