package de.pulselibre

import java.security.MessageDigest

/**
 * Small, offline FIT profile reader for explicitly supported measurement fields.
 *
 * It is not a complete FIT SDK implementation. It requires a valid FIT envelope
 * and parses standard definitions, developer-field lengths, and normal/compressed
 * timestamps. Unknown fields are skipped, never guessed or imported.
 */
object FitHistoricalObservations {
    private const val FIT_EPOCH_UNIX_SECONDS = 631_065_600L
    const val MAX_OBSERVATIONS = 50_000
    const val DECODER_VERSION = "1"
    const val PROFILE_VERSION = "21.222.0"
    private const val GLOBAL_RECORD = 20
    private const val GLOBAL_MONITORING = 55
    private const val GLOBAL_DEVICE_INFO = 23
    private const val GLOBAL_RESPIRATION_RATE = 297

    enum class Metric(val unit: String) {
        HEART_RATE("bpm"),
        CADENCE("rpm"),
        DISTANCE("m"),
        SPEED("m/s"),
        POWER("W"),
    }

    enum class Quality { PROFILE_VALID, MISSING_TIMESTAMP, OUTSIDE_DISPLAY_RANGE }
    enum class TimestampOrigin { DIRECT, RECONSTRUCTED_TS16, RECONSTRUCTED_COMPRESSED, UNKNOWN }

    data class Observation(
        val metric: Metric,
        val timestampEpochMillis: Long?,
        val value: Double,
        val unit: String,
        val sourceFileSha256: String,
        val recordOrdinal: Int,
        val globalMessageNumber: Int,
        val fieldNumber: Int,
        val quality: Quality,
        val timestampOrigin: TimestampOrigin = TimestampOrigin.UNKNOWN,
        /** Stable for idempotent re-import of the same original FIT file. */
        val idempotencyKey: String,
        /** Flags exact same-time/value observations across files without deleting them. */
        val duplicateFingerprint: String
    )

    /**
     * Whitelisted, non-serial FIT device_info fields. Codes are retained as
     * profile codes; they are not treated as a verified CIRQA identity.
     */
    data class DeviceMetadata(
        val sourceFileSha256: String,
        val recordOrdinal: Int,
        val timestampEpochMillis: Long?,
        val deviceTypeCode: Int?,
        val manufacturerCode: Int?,
        val productCode: Int?,
        val softwareVersion: Double?,
        val batteryVoltageVolts: Double?,
        val batteryStatusCode: Int?,
        val batteryLevelPercent: Int?
    )

    data class Result(
        val observations: List<Observation>,
        val dataRecordCount: Int,
        val unsupportedMeasurementFieldCount: Int,
        val deviceMetadata: List<DeviceMetadata> = emptyList(),
        val unknownFields: List<UnknownField> = emptyList(),
        val invalidMeasurementFieldCount: Int = 0,
        /** Offline review only; never committed by FitObservationCommitter. */
        val reviewCandidates: List<ReviewCandidate> = emptyList()
    ) {
        fun potentialDuplicates(): List<List<Observation>> =
            observations.groupBy { it.duplicateFingerprint }
                .values.filter { it.size > 1 }
    }

    /**
     * Profile-decoded candidate that is intentionally kept outside the
     * persistent observation allowlist pending the decoder-generation rollout.
     */
    data class ReviewCandidate(
        val profileFieldName: String,
        val value: Double,
        val unit: String,
        val timestampEpochMillis: Long?,
        val sourceFileSha256: String,
        val recordOrdinal: Int,
        val globalMessageNumber: Int,
        val fieldNumber: Int,
        val timestampOrigin: TimestampOrigin = TimestampOrigin.UNKNOWN
    )

    enum class UnknownKind { UNKNOWN_MESSAGE, UNKNOWN_STANDARD_FIELD, DEVELOPER_FIELD, INCOMPATIBLE_KNOWN_FIELD }
    /** Metadata only; never includes the original field value. */
    data class UnknownField(
        val sourceSha256: String, val globalMessageNumber: Int, val fieldNumber: Int,
        val baseType: Int?, val size: Int, val developerDataIndex: Int?,
        val kind: UnknownKind, val definitionCount: Int, val occurrenceCount: Int
    )
    private data class UnknownKey(
        val global: Int, val field: Int, val baseType: Int?, val size: Int,
        val developerIndex: Int?, val kind: UnknownKind
    )
    private data class UnknownCounts(var definitions: Int = 0, var occurrences: Int = 0)
    private data class DeveloperFieldDefinition(val number: Int, val size: Int, val index: Int)
    private data class FieldDefinition(val number: Int, val size: Int, val baseType: Int)
    private data class Definition(
        val globalNumber: Int,
        val bigEndian: Boolean,
        val fields: List<FieldDefinition>,
        val developerFields: List<DeveloperFieldDefinition>
    )

    /**
     * Decodes only Garmin FIT Profile fields with explicit mappings below.
     * Malformed file structure returns null. Invalid/sentinel fields are omitted.
     */
    fun decode(file: ByteArray): Result? {
        val header = FitFileHeader.inspectVerified(file) ?: return null
        val dataEnd = header.headerSize + header.dataSize.toInt()
        val fileSha = sha256(file)
        val definitions = arrayOfNulls<Definition>(16)
        val observations = mutableListOf<Observation>()
        val reviewCandidates = mutableListOf<ReviewCandidate>()
        val deviceMetadata = mutableListOf<DeviceMetadata>()
        val unknown = LinkedHashMap<UnknownKey, UnknownCounts>()
        fun track(key: UnknownKey, definition: Boolean) {
            val count = unknown.getOrPut(key) { UnknownCounts() }
            if (definition) count.definitions++ else count.occurrences++
        }
        fun classify(global: Int, field: FieldDefinition): UnknownKey? {
            if (field.number == 253) return null // FIT timestamp, not a novel metric
            val mapped = mapping(global, field.number)
            if (mapped != null) {
                val size = when (mapped.baseType) { 2 -> 1; 4 -> 2; 6 -> 4; else -> -1 }
                return if (field.baseType and 0x1f != mapped.baseType || field.size != size)
                    UnknownKey(global, field.number, field.baseType, field.size, null,
                        UnknownKind.INCOMPATIBLE_KNOWN_FIELD) else null
            }
            if (global == GLOBAL_DEVICE_INFO && field.number in setOf(1, 2, 4, 5, 10, 11, 32)) return null
            val kind = if (global in setOf(GLOBAL_RECORD, GLOBAL_MONITORING, GLOBAL_DEVICE_INFO))
                UnknownKind.UNKNOWN_STANDARD_FIELD else UnknownKind.UNKNOWN_MESSAGE
            return UnknownKey(global, field.number, field.baseType, field.size, null, kind)
        }
        var cursor = header.headerSize
        var previousFitTimestamp: Long? = null
        var recordOrdinal = 0
        var unsupportedFields = 0
        var invalidMeasurementFields = 0
        var deviceInfoOrdinal = 0
        var respirationRecordOrdinal = 0

        fun requireBytes(count: Int): Boolean =
            count >= 0 && cursor <= dataEnd - count

        fun u8(index: Int): Int = file[index].toInt() and 0xff

        while (cursor < dataEnd) {
            if (!requireBytes(1)) return null
            val headerByte = u8(cursor++)
            val compressed = headerByte and 0x80 != 0

            if (!compressed && headerByte and 0x40 != 0) {
                // FIT definition message. Reserved bits/bytes must be zero.
                if (headerByte and 0x10 != 0 || !requireBytes(5)) return null
                val localType = headerByte and 0x0f
                val reserved = u8(cursor++)
                val architecture = u8(cursor++)
                if (reserved != 0 || architecture !in 0..1) return null
                val globalNumber = readU16(file, cursor, architecture == 1)
                cursor += 2
                val fieldCount = u8(cursor++)
                if (!requireBytes(fieldCount * 3)) return null
                val fields = ArrayList<FieldDefinition>(fieldCount)
                repeat(fieldCount) {
                    val fieldNumber = u8(cursor++)
                    val size = u8(cursor++)
                    val baseType = u8(cursor++)
                    if (size == 0) return null
                    fields += FieldDefinition(fieldNumber, size, baseType)
                }
                val developerFields = mutableListOf<DeveloperFieldDefinition>()
                if (headerByte and 0x20 != 0) {
                    if (!requireBytes(1)) return null
                    val developerCount = u8(cursor++)
                    if (!requireBytes(developerCount * 3)) return null
                    repeat(developerCount) {
                        val number = u8(cursor)
                        val size = u8(cursor + 1)
                        val index = u8(cursor + 2)
                        if (size == 0) return null
                        developerFields += DeveloperFieldDefinition(number, size, index)
                        cursor += 3
                    }
                }
                definitions[localType] = Definition(
                    globalNumber, bigEndian = architecture == 1,
                    fields = fields, developerFields = developerFields
                )
                fields.forEach { classify(globalNumber, it)?.let { key -> track(key, true) } }
                developerFields.forEach {
                    track(UnknownKey(globalNumber, it.number, null, it.size, it.index,
                        UnknownKind.DEVELOPER_FIELD), true)
                }
                continue
            }
            if (!compressed && headerByte and 0x30 != 0) return null

            val localType = if (compressed) (headerByte ushr 5) and 0x03
                else headerByte and 0x0f
            val definition = definitions[localType] ?: return null
            val compressedTimestampField = if (compressed) {
                definition.fields.firstOrNull { it.number == 253 } ?: return null
            } else null

            val fieldBytes = LinkedHashMap<FieldDefinition, Int>()
            for (field in definition.fields) {
                if (compressed && field == compressedTimestampField) continue
                if (!requireBytes(field.size)) return null
                fieldBytes[field] = cursor
                classify(definition.globalNumber, field)?.let { track(it, false) }
                cursor += field.size
            }
            for (field in definition.developerFields) {
                if (!requireBytes(field.size)) return null
                track(UnknownKey(definition.globalNumber, field.number, null, field.size,
                    field.index, UnknownKind.DEVELOPER_FIELD), false)
                cursor += field.size
            }

            val compressedTimestamp = if (compressed) {
                val old = previousFitTimestamp ?: return null
                val delta = headerByte and 0x1f
                var reconstructed = (old and -32L) + delta
                if (reconstructed < old) reconstructed += 32
                reconstructed
            } else null
            val directTimestamp = if (!compressed) {
                val timestampField = definition.fields.firstOrNull { it.number == 253 }
                val offset = timestampField?.let(fieldBytes::get)
                offset?.let {
                    readUnsigned32(file, it, timestampField.size, definition.bigEndian, timestampField)
                }
            } else null
            val timestamp16 = if (!compressed && directTimestamp == null)
                reconstructMonitoringTimestamp16(definition, fieldBytes, file, previousFitTimestamp)
            else null
            val fitTimestamp = compressedTimestamp ?: directTimestamp ?: timestamp16
            val timestampOrigin = when {
                compressedTimestamp != null -> TimestampOrigin.RECONSTRUCTED_COMPRESSED
                timestamp16 != null -> TimestampOrigin.RECONSTRUCTED_TS16
                directTimestamp != null && directTimestamp != 0xffff_ffffL -> TimestampOrigin.DIRECT
                else -> TimestampOrigin.UNKNOWN
            }
            if (fitTimestamp != null && fitTimestamp != 0xffff_ffffL) {
                previousFitTimestamp = fitTimestamp
            }
            val timestampMillis = fitTimestamp
                ?.takeIf { it != 0xffff_ffffL }
                ?.let { (it + FIT_EPOCH_UNIX_SECONDS) * 1_000L }

            if (definition.globalNumber == GLOBAL_DEVICE_INFO) {
                deviceInfoOrdinal++
                if (deviceMetadata.size >= MAX_DEVICE_METADATA) return null
                fun fieldValue(number: Int, baseType: Int): Long? {
                    val field = definition.fields.firstOrNull { it.number == number } ?: return null
                    val offset = fieldBytes[field] ?: return null
                    val raw = readSupportedUnsigned(file, offset, field, definition.bigEndian, baseType)
                        ?: return null
                    return raw.takeUnless { it == invalidSentinel(field) }
                }
                val batteryLevel = fieldValue(32, baseType = 2)
                    ?.takeIf { it in 0L..100L }?.toInt()
                val batteryVoltage = fieldValue(10, baseType = 4)
                    ?.let { it / 256.0 }?.takeIf { it.isFinite() && it in 0.0..20.0 }
                deviceMetadata += DeviceMetadata(
                    sourceFileSha256 = fileSha,
                    recordOrdinal = deviceInfoOrdinal,
                    timestampEpochMillis = timestampMillis,
                    deviceTypeCode = fieldValue(1, baseType = 2)?.toInt(),
                    manufacturerCode = fieldValue(2, baseType = 4)?.toInt(),
                    productCode = fieldValue(4, baseType = 4)?.toInt(),
                    softwareVersion = fieldValue(5, baseType = 4)
                        ?.div(100.0)?.takeIf { it.isFinite() },
                    batteryVoltageVolts = batteryVoltage,
                    batteryStatusCode = fieldValue(11, baseType = 2)?.toInt(),
                    batteryLevelPercent = batteryLevel
                )
            }

            if (definition.globalNumber == GLOBAL_RESPIRATION_RATE) {
                respirationRecordOrdinal++
                val field = definition.fields.firstOrNull { it.number == 0 }
                val offset = field?.let(fieldBytes::get)
                if (field != null && offset != null) {
                    val raw = readSupportedSigned16(file, offset, field, definition.bigEndian)
                    when {
                        raw == null -> unsupportedFields++
                        raw == 0x7fff -> invalidMeasurementFields++
                        else -> {
                            if (reviewCandidates.size >= MAX_OBSERVATIONS) return null
                            reviewCandidates += ReviewCandidate(
                                profileFieldName = "respiration_rate",
                                value = raw.toDouble() / 100.0,
                                unit = "breaths/min",
                                timestampEpochMillis = timestampMillis,
                                sourceFileSha256 = fileSha,
                                recordOrdinal = respirationRecordOrdinal,
                                globalMessageNumber = GLOBAL_RESPIRATION_RATE,
                                fieldNumber = 0,
                                timestampOrigin = timestampOrigin
                            )
                        }
                    }
                }
            }

            if (definition.globalNumber == GLOBAL_RECORD || definition.globalNumber == GLOBAL_MONITORING) {
                recordOrdinal++
                for ((field, offset) in fieldBytes) {
                    val mapped = mapping(definition.globalNumber, field.number) ?: continue
                    val raw = readSupportedUnsigned(file, offset, field, definition.bigEndian, mapped.baseType)
                    if (raw == null) {
                        unsupportedFields++
                        continue
                    }
                    if (raw == invalidSentinel(field)) {
                        invalidMeasurementFields++
                        continue
                    }
                    // A data field without a valid timestamp is retained as an
                    // explicitly non-importable observation for quality auditing.
                    val value = mapped.scale(raw)
                    val quality = when (mapped.metric) {
                        Metric.HEART_RATE -> if (value in 1.0..240.0)
                            if (timestampMillis == null) Quality.MISSING_TIMESTAMP else Quality.PROFILE_VALID
                            else Quality.OUTSIDE_DISPLAY_RANGE
                        else -> if (timestampMillis == null) Quality.MISSING_TIMESTAMP else Quality.PROFILE_VALID
                    }
                    val semanticKey = "${mapped.metric.name}:${timestampMillis ?: "missing"}:${value.toBits()}"
                    if (observations.size >= MAX_OBSERVATIONS) return null
                    observations += Observation(
                        metric = mapped.metric,
                        timestampEpochMillis = timestampMillis,
                        value = value,
                        unit = mapped.metric.unit,
                        sourceFileSha256 = fileSha,
                        recordOrdinal = recordOrdinal,
                        globalMessageNumber = definition.globalNumber,
                        fieldNumber = field.number,
                        quality = quality,
                        timestampOrigin = timestampOrigin,
                        idempotencyKey = sha256("$fileSha:$recordOrdinal:${definition.globalNumber}:${field.number}".toByteArray()),
                        duplicateFingerprint = sha256(semanticKey.toByteArray())
                    )
                }
            }
        }

        return Result(observations, recordOrdinal, unsupportedFields, deviceMetadata,
            unknown.map { (key, count) ->
                UnknownField(fileSha, key.global, key.field, key.baseType, key.size,
                    key.developerIndex, key.kind, count.definitions, count.occurrences)
            }, invalidMeasurementFields, reviewCandidates)
    }

    private const val MAX_DEVICE_METADATA = 10_000

    private data class Mapping(
        val metric: Metric,
        val baseType: Int,
        val divisor: Double = 1.0,
        val multiplier: Double = 1.0
    ) {
        fun scale(raw: Long): Double = raw.toDouble() * multiplier / divisor
    }

    private fun mapping(global: Int, field: Int): Mapping? = when (global) {
        GLOBAL_RECORD -> when (field) {
            3 -> Mapping(Metric.HEART_RATE, baseType = 2) // uint8, bpm
            4 -> Mapping(Metric.CADENCE, baseType = 2) // uint8, rpm; not step cadence
            5 -> Mapping(Metric.DISTANCE, baseType = 6, divisor = 100.0) // uint32, m
            6 -> Mapping(Metric.SPEED, baseType = 4, divisor = 1_000.0) // uint16, m/s
            7 -> Mapping(Metric.POWER, baseType = 4) // uint16, W
            73 -> Mapping(Metric.SPEED, baseType = 6, divisor = 1_000.0) // enhanced_speed
            else -> null
        }
        GLOBAL_MONITORING -> when (field) {
            27 -> Mapping(Metric.HEART_RATE, baseType = 2) // uint8, bpm
            else -> null
        }
        else -> null
    }

    /**
     * Monitoring messages may carry `timestamp_16` (field 26, uint16) instead of a full timestamp. The FIT profile
     * defines the full value as the most recent full timestamp plus the 16-bit difference (mod 2^16). Without an
     * earlier full timestamp in the same file, or with the invalid marker, nothing is reconstructed (stays missing).
     */
    private fun reconstructMonitoringTimestamp16(
        definition: Definition,
        fieldBytes: Map<FieldDefinition, Int>,
        file: ByteArray,
        previous: Long?
    ): Long? {
        if (definition.globalNumber != GLOBAL_MONITORING || previous == null) return null
        val field = definition.fields.firstOrNull { it.number == 26 && it.size == 2 } ?: return null
        val offset = fieldBytes[field] ?: return null
        val raw = readSupportedUnsigned(file, offset, field, definition.bigEndian, 4) ?: return null
        if (raw == invalidSentinel(field)) return null
        return previous + ((raw - (previous and 0xffffL)) and 0xffffL)
    }

    private fun readSupportedUnsigned(
        file: ByteArray,
        offset: Int,
        field: FieldDefinition,
        bigEndian: Boolean,
        expectedBaseType: Int
    ): Long? {
        val base = field.baseType and 0x1f
        if (base != expectedBaseType) return null
        val expectedSize = when (base) {
            2 -> 1 // uint8
            4 -> 2 // uint16
            6 -> 4 // uint32
            else -> return null
        }
        if (field.size != expectedSize) return null
        return when (expectedSize) {
            1 -> (file[offset].toInt() and 0xff).toLong()
            2 -> readU16(file, offset, bigEndian).toLong()
            else -> readU32(file, offset, bigEndian)
        }
    }

    private fun readSupportedSigned16(
        file: ByteArray,
        offset: Int,
        field: FieldDefinition,
        bigEndian: Boolean
    ): Int? {
        if (field.baseType and 0x1f != 3 || field.size != 2) return null
        val raw = readU16(file, offset, bigEndian)
        return if (raw and 0x8000 != 0) raw - 0x1_0000 else raw
    }

    private fun readUnsigned32(
        file: ByteArray,
        offset: Int,
        size: Int,
        bigEndian: Boolean,
        field: FieldDefinition
    ): Long? {
        if (size != 4 || field.baseType and 0x1f != 6) return null
        return readU32(file, offset, bigEndian)
    }

    private fun invalidSentinel(field: FieldDefinition): Long = when (field.baseType and 0x1f) {
        2 -> 0xffL
        4 -> 0xffffL
        6 -> 0xffff_ffffL
        else -> Long.MIN_VALUE
    }

    private fun readU16(bytes: ByteArray, offset: Int, bigEndian: Boolean): Int {
        val a = bytes[offset].toInt() and 0xff
        val b = bytes[offset + 1].toInt() and 0xff
        return if (bigEndian) (a shl 8) or b else a or (b shl 8)
    }

    private fun readU32(bytes: ByteArray, offset: Int, bigEndian: Boolean): Long {
        var value = 0L
        if (bigEndian) {
            for (i in 0..3) value = (value shl 8) or (bytes[offset + i].toLong() and 0xff)
        } else {
            for (i in 3 downTo 0) value = (value shl 8) or (bytes[offset + i].toLong() and 0xff)
        }
        return value
    }

    private fun sha256(value: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(value)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
