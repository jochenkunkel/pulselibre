package de.pulselibre

import java.nio.ByteBuffer
import java.security.MessageDigest

/** Immutable decoder output set for one source and one decoder/profile identity. */
internal data class StoredFitDecoderGeneration(
    val generationId: String,
    val sourceSha256: String,
    val decoderVersion: String,
    val profileVersion: String,
    val outputDigest: String,
    val outputCount: Long,
    val completedAtEpochMillis: Long,
    val decodedFieldCount: Long = 0
)

/** One normalized FIT observation inside an immutable decoder generation. */
internal data class StoredFitGenerationObservation(
    val generationId: String,
    val sampleId: String,
    val deviceId: String,
    val metric: String,
    val measuredAtEpochMillis: Long,
    val value: Double,
    val unit: String,
    val receivedAtEpochMillis: Long?,
    val recordOrdinal: Int,
    val globalMessageNumber: Int,
    val fieldNumber: Int,
    val quality: String,
    val timestampOrigin: String
)

/** Exact FIT data-field bytes and source coordinates in one immutable generation. */
internal data class StoredFitGenerationField(
    val generationId: String,
    val recordOrdinal: Int,
    val globalMessageNumber: Int,
    val fieldKind: String,
    val fieldNumber: Int,
    val baseTypeCode: Int?,
    val fieldSize: Int,
    val developerDataIndex: Int?,
    val bigEndian: Boolean,
    val rawBytes: ByteArray,
    val measuredAtEpochMillis: Long?,
    val timestampOrigin: String
)

/** Explicit per-source projection pointer. */
internal data class StoredActiveFitGeneration(
    val sourceSha256: String,
    val generationId: String
)

internal object FitDecoderGeneration {
    fun id(sourceSha256: String, decoderVersion: String, profileVersion: String): String =
        sha256("$sourceSha256\u0000$decoderVersion\u0000$profileVersion".toByteArray(Charsets.UTF_8))

    /** Stable digest over output content, independent of cursor/SQLite row order. */
    fun outputDigest(
        rows: Iterable<StoredFitGenerationObservation>,
        fields: Iterable<StoredFitGenerationField> = emptyList()
    ): String {
        val accumulator = OutputDigest()
        rows.sortedBy { it.sampleId }.forEach(accumulator::add)
        fields.sortedWith(compareBy(
            StoredFitGenerationField::recordOrdinal,
            StoredFitGenerationField::fieldKind,
            StoredFitGenerationField::fieldNumber,
            { it.developerDataIndex ?: -1 }
        )).forEach(accumulator::add)
        return accumulator.finish()
    }

    class OutputDigest {
        private val digest = MessageDigest.getInstance("SHA-256")

        fun add(row: StoredFitGenerationObservation) {
            addStrings(listOf(
                row.sampleId, row.deviceId, row.metric, row.measuredAtEpochMillis.toString(),
                java.lang.Double.doubleToLongBits(row.value).toString(), row.unit,
                row.recordOrdinal.toString(), row.globalMessageNumber.toString(),
                row.fieldNumber.toString(), row.quality, row.timestampOrigin
            ))
        }

        fun add(row: StoredFitGenerationField) {
            addStrings(listOf(
                "FIELD", row.recordOrdinal.toString(), row.globalMessageNumber.toString(),
                row.fieldKind, row.fieldNumber.toString(), row.baseTypeCode?.toString().orEmpty(),
                row.fieldSize.toString(), row.developerDataIndex?.toString().orEmpty(),
                row.bigEndian.toString(), row.measuredAtEpochMillis?.toString().orEmpty(),
                row.timestampOrigin
            ))
            digest.update(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(row.rawBytes.size).array())
            digest.update(row.rawBytes)
        }

        private fun addStrings(values: List<String>) {
            values.forEach { value ->
                val bytes = value.toByteArray(Charsets.UTF_8)
                digest.update(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(bytes.size).array())
                digest.update(bytes)
            }
        }

        fun finish(): String = digest.digest().toHex()
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
