package de.pulselibre

import java.security.MessageDigest

/**
 * Offline FIT reader. Profile mappings add semantic observations, while every
 * data-message field is also retained byte-for-byte in [Result.decodedFields].
 *
 * It is not a complete FIT SDK implementation. It requires a valid FIT envelope
 * and parses standard definitions, developer-field lengths, and normal/compressed
 * timestamps. Unknown values are not interpreted, but their original bytes are
 * retained without guessing a meaning.
 */
object FitHistoricalObservations {
    private const val FIT_EPOCH_UNIX_SECONDS = 631_065_600L
    private const val MIN_ABSOLUTE_FIT_TIME = 0x1000_0000L
    private const val INVALID_FIT_TIME = 0xffff_ffffL
    const val MAX_OBSERVATIONS = 50_000
    const val DECODER_VERSION = "6"
    /** Review candidates evolve independently from the trusted observation decoder. */
    const val REVIEW_CANDIDATE_DECODER_VERSION = "3"
    const val PROFILE_VERSION = "21.222.0"
    const val MAX_DECODED_FIELDS = 1_000_000
    private const val GLOBAL_RECORD = 20
    private const val GLOBAL_MONITORING = 55
    private const val GLOBAL_DEVICE_INFO = 23
    private const val GLOBAL_RESPIRATION_RATE = 297
    private const val GLOBAL_SPO2_DATA = 269
    private const val GLOBAL_STRESS_LEVEL = 227
    private const val GLOBAL_SLEEP_LEVEL = 275
    private const val GLOBAL_SKIN_TEMP_OVERNIGHT = 398

    enum class Metric(val unit: String) {
        HEART_RATE("bpm"),
        CADENCE("rpm"),
        DISTANCE("m"),
        SPEED("m/s"),
        POWER("W"),
        STRESS("device_scale"),
        RESPIRATION("breaths/min"),
        SPO2("percent"),
        SPO2_CONFIDENCE("profile_code"),
        SPO2_MEASUREMENT_MODE("profile_code"),
        SLEEP_LEVEL("profile_code"),
        SKIN_TEMPERATURE_DEVIATION("°C"),
        SKIN_TEMPERATURE_7_DAY_DEVIATION("°C"),
        MONITORING_CYCLES("cycles"),
        MONITORING_ACTIVITY_TYPE("profile_code"),
        /** #707: 55/24 `current_activity_type_intensity`, low 5 bits (FIT activity_type enum). */
        MONITORING_CURRENT_ACTIVITY_TYPE("profile_code"),
        /** #707: 55/24, high 3 bits. A context-dependent code, not a clinical load, MET or percentage. */
        MONITORING_INTENSITY_CODE("profile_code"),
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
        val reviewCandidates: List<ReviewCandidate> = emptyList(),
        /** Counts only; invalid values, timestamps, and source identifiers are never included. */
        val excludedFields: List<ExcludedField> = emptyList(),
        val decodedFields: List<DecodedField> = emptyList()
    ) {
        fun potentialDuplicates(): List<List<Observation>> =
            observations.groupBy { it.duplicateFingerprint }
                .values.filter { it.size > 1 }
    }

    /** Exact encoded value bytes for one field occurrence in one FIT data message. */
    data class DecodedField(
        val sourceFileSha256: String,
        val recordOrdinal: Int,
        val globalMessageNumber: Int,
        val fieldNumber: Int,
        val fieldKind: String,
        val baseTypeCode: Int?,
        val fieldSize: Int,
        val developerDataIndex: Int?,
        val bigEndian: Boolean,
        val rawBytes: ByteArray,
        val timestampEpochMillis: Long?,
        val timestampOrigin: TimestampOrigin
    ) {
        override fun equals(other: Any?): Boolean =
            other is DecodedField && sourceFileSha256 == other.sourceFileSha256 &&
                recordOrdinal == other.recordOrdinal && globalMessageNumber == other.globalMessageNumber &&
                fieldNumber == other.fieldNumber && fieldKind == other.fieldKind &&
                baseTypeCode == other.baseTypeCode && fieldSize == other.fieldSize &&
                developerDataIndex == other.developerDataIndex && bigEndian == other.bigEndian &&
                rawBytes.contentEquals(other.rawBytes) && timestampEpochMillis == other.timestampEpochMillis &&
                timestampOrigin == other.timestampOrigin

        override fun hashCode(): Int = 31 * listOf(
            sourceFileSha256, recordOrdinal, globalMessageNumber, fieldNumber, fieldKind,
            baseTypeCode, fieldSize, developerDataIndex, bigEndian, timestampEpochMillis, timestampOrigin
        ).hashCode() + rawBytes.contentHashCode()
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
        val timestampOrigin: TimestampOrigin = TimestampOrigin.UNKNOWN,
        val quality: String = "UNTRUSTED_PROFILE_CANDIDATE",
        val decoderVersion: String = REVIEW_CANDIDATE_DECODER_VERSION,
        val idempotencyKey: String = sha256(
            "$sourceFileSha256:$recordOrdinal:$globalMessageNumber:$fieldNumber:$decoderVersion"
                .toByteArray()
        )
    )

    enum class UnknownKind { UNKNOWN_MESSAGE, UNKNOWN_STANDARD_FIELD, DEVELOPER_FIELD, INCOMPATIBLE_KNOWN_FIELD }
    /** Metadata only; never includes the original field value. */
    data class UnknownField(
        val sourceSha256: String, val globalMessageNumber: Int, val fieldNumber: Int,
        val baseType: Int?, val size: Int, val developerDataIndex: Int?,
        val kind: UnknownKind, val definitionCount: Int, val occurrenceCount: Int
    )
    enum class ExclusionReason { UNSUPPORTED_ENCODING, INVALID_SENTINEL, NON_FINITE_VALUE }
    /** Profile structure and count only; intentionally omits the rejected value and source identity. */
    data class ExcludedField(
        val globalMessageNumber: Int,
        val fieldNumber: Int,
        val metric: Metric?,
        /** Fixed decoder allowlist label for review-only fields; null for persisted metrics. */
        val reviewFieldName: String?,
        val reason: ExclusionReason,
        val occurrenceCount: Int
    )
    private data class ExcludedFieldKey(
        val global: Int,
        val field: Int,
        val metric: Metric?,
        val reviewFieldName: String?,
        val reason: ExclusionReason
    )
    private data class UnknownKey(
        val global: Int, val field: Int, val baseType: Int?, val size: Int,
        val developerIndex: Int?, val kind: UnknownKind
    )
    private data class UnknownCounts(var definitions: Int = 0, var occurrences: Int = 0)
    private data class DeveloperFieldDefinition(val number: Int, val size: Int, val index: Int)
    private data class FieldDefinition(val number: Int, val size: Int, val baseType: Int)
    private data class CandidateField(
        val number: Int,
        val name: String,
        val baseType: Int,
        val invalid: Long,
        val unit: String
    )
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
        val decodedFields = mutableListOf<DecodedField>()
        val reviewCandidates = mutableListOf<ReviewCandidate>()
        val deviceMetadata = mutableListOf<DeviceMetadata>()
        val unknown = LinkedHashMap<UnknownKey, UnknownCounts>()
        val excluded = linkedMapOf<ExcludedFieldKey, Int>()
        fun trackExcluded(
            global: Int,
            field: Int,
            metric: Metric?,
            reviewFieldName: String?,
            reason: ExclusionReason
        ) {
            val key = ExcludedFieldKey(global, field, metric, reviewFieldName, reason)
            excluded[key] = (excluded[key] ?: 0) + 1
        }
        fun track(key: UnknownKey, definition: Boolean): Boolean {
            var count = unknown[key]
            if (count == null) {
                if (unknown.size >= MAX_UNKNOWN_FIELD_SIGNATURES) return false
                count = UnknownCounts()
                unknown[key] = count
            }
            if (definition) count.definitions++ else count.occurrences++
            return true
        }
        fun classify(global: Int, field: FieldDefinition): UnknownKey? {
            if (field.number == 253) {
                // A valid FIT timestamp is a uint32. In particular, compressed
                // timestamp headers may only reconstruct this field when its
                // definition has the expected encoding.
                return if (field.baseType and 0x1f == 6 && field.size == 4) null
                else UnknownKey(
                    global, field.number, field.baseType, field.size, null,
                    UnknownKind.INCOMPATIBLE_KNOWN_FIELD
                )
            }
            val mapped = mapping(global, field.number)
            if (mapped != null) {
                val size = when (mapped.baseType) { 2 -> 1; 4 -> 2; 6 -> 4; else -> -1 }
                return if (field.baseType and 0x1f != mapped.baseType || field.size != size)
                    UnknownKey(global, field.number, field.baseType, field.size, null,
                        UnknownKind.INCOMPATIBLE_KNOWN_FIELD) else null
            }
            if (global == GLOBAL_SKIN_TEMP_OVERNIGHT) {
                val expectedBaseType = when (field.number) {
                    0 -> 6 // local_timestamp: uint32
                    1, 2, 4 -> 8 // profile float32; F4 unit/sentinel semantics stay unpromoted
                    else -> null
                }
                if (expectedBaseType != null) {
                    return if (field.baseType and 0x1f != expectedBaseType || field.size != 4)
                        UnknownKey(global, field.number, field.baseType, field.size, null,
                            UnknownKind.INCOMPATIBLE_KNOWN_FIELD) else null
                }
            }
            if (global == GLOBAL_DEVICE_INFO && field.number in setOf(1, 2, 4, 5, 10, 11, 32)) return null
            val kind = if (global in setOf(
                    GLOBAL_RECORD, GLOBAL_MONITORING, GLOBAL_DEVICE_INFO,
                    GLOBAL_SKIN_TEMP_OVERNIGHT
                ))
                UnknownKind.UNKNOWN_STANDARD_FIELD else UnknownKind.UNKNOWN_MESSAGE
            return UnknownKey(global, field.number, field.baseType, field.size, null, kind)
        }
        var cursor = header.headerSize
        var previousFitTimestamp: Long? = null
        var recordOrdinal = 0
        var decodedRecordOrdinal = 0
        var unsupportedFields = 0
        var invalidMeasurementFields = 0
        var deviceInfoOrdinal = 0
        var respirationRecordOrdinal = 0
        var spo2RecordOrdinal = 0
        var stressRecordOrdinal = 0
        var sleepRecordOrdinal = 0
        var monitoringRecordOrdinal = 0

        fun requireBytes(count: Int): Boolean =
            count >= 0 && cursor <= dataEnd - count

        fun u8(index: Int): Int = file[index].toInt() and 0xff

        while (cursor < dataEnd) {
            if (!requireBytes(1)) return null
            val recordHeaderOffset = cursor
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
                for (field in fields) {
                    val key = classify(globalNumber, field)
                    if (key != null && !track(key, definition = true)) return null
                }
                for (field in developerFields) {
                    val key = UnknownKey(globalNumber, field.number, null, field.size, field.index,
                        UnknownKind.DEVELOPER_FIELD)
                    if (!track(key, definition = true)) return null
                }
                continue
            }
            if (!compressed && headerByte and 0x30 != 0) return null

            val localType = if (compressed) (headerByte ushr 5) and 0x03
                else headerByte and 0x0f
            val definition = definitions[localType] ?: return null
            val fieldBytes = LinkedHashMap<FieldDefinition, Int>()
            for (field in definition.fields) {
                if (!requireBytes(field.size)) return null
                fieldBytes[field] = cursor
                val key = classify(definition.globalNumber, field)
                if (key != null && !track(key, definition = false)) return null
                cursor += field.size
            }
            val developerFieldBytes = LinkedHashMap<DeveloperFieldDefinition, Int>()
            for (field in definition.developerFields) {
                if (!requireBytes(field.size)) return null
                developerFieldBytes[field] = cursor
                val key = UnknownKey(definition.globalNumber, field.number, null, field.size,
                    field.index, UnknownKind.DEVELOPER_FIELD)
                if (!track(key, definition = false)) return null
                cursor += field.size
            }

            val compressedTimestamp = if (compressed) {
                val old = previousFitTimestamp ?: return null
                val delta = headerByte and 0x1f
                var reconstructed = (old and -32L) + delta
                if (reconstructed < old) reconstructed += 32
                if (reconstructed >= INVALID_FIT_TIME) return null
                reconstructed
            } else null
            val directTimestamp = if (!compressed) {
                val timestampField = definition.fields.firstOrNull { it.number == 253 }
                val offset = timestampField?.let(fieldBytes::get)
                offset?.let {
                    readUnsigned32(file, it, timestampField.size, definition.bigEndian, timestampField)
                }
            } else null
            // A relative FIT clock must not reuse an older absolute clock as its anchor.
            if (directTimestamp != null && directTimestamp < MIN_ABSOLUTE_FIT_TIME) {
                previousFitTimestamp = null
            }
            val timestamp16 = if (!compressed && (directTimestamp == null || directTimestamp == INVALID_FIT_TIME))
                reconstructMonitoringTimestamp16(definition, fieldBytes, file, previousFitTimestamp)
            else null
            val fitTimestamp = compressedTimestamp
                ?: directTimestamp?.takeIf { it != INVALID_FIT_TIME }
                ?: timestamp16
            val timestampOrigin = when {
                compressedTimestamp != null -> TimestampOrigin.RECONSTRUCTED_COMPRESSED
                timestamp16 != null -> TimestampOrigin.RECONSTRUCTED_TS16
                directTimestamp != null && directTimestamp in MIN_ABSOLUTE_FIT_TIME until INVALID_FIT_TIME -> TimestampOrigin.DIRECT
                else -> TimestampOrigin.UNKNOWN
            }
            if (fitTimestamp != null && fitTimestamp in MIN_ABSOLUTE_FIT_TIME until INVALID_FIT_TIME) {
                previousFitTimestamp = fitTimestamp
            }
            val timestampMillis = fitTimestamp
                ?.takeIf { it in MIN_ABSOLUTE_FIT_TIME until INVALID_FIT_TIME }
                ?.let { (it + FIT_EPOCH_UNIX_SECONDS) * 1_000L }

            decodedRecordOrdinal++
            if (decodedRecordOrdinal > MAX_OBSERVATIONS) return null
            for ((field, offset) in fieldBytes) {
                if (decodedFields.size >= MAX_DECODED_FIELDS) return null
                decodedFields += DecodedField(
                    sourceFileSha256 = fileSha,
                    recordOrdinal = decodedRecordOrdinal,
                    globalMessageNumber = definition.globalNumber,
                    fieldNumber = field.number,
                    fieldKind = "STANDARD",
                    baseTypeCode = field.baseType,
                    fieldSize = field.size,
                    developerDataIndex = null,
                    bigEndian = definition.bigEndian,
                    rawBytes = file.copyOfRange(offset, offset + field.size),
                    timestampEpochMillis = timestampMillis,
                    timestampOrigin = timestampOrigin
                )
            }
            if (compressed) {
                if (decodedFields.size >= MAX_DECODED_FIELDS) return null
                decodedFields += DecodedField(
                    sourceFileSha256 = fileSha,
                    recordOrdinal = decodedRecordOrdinal,
                    globalMessageNumber = definition.globalNumber,
                    fieldNumber = 253,
                    fieldKind = "COMPRESSED_TIMESTAMP_HEADER",
                    baseTypeCode = null,
                    fieldSize = 1,
                    developerDataIndex = null,
                    bigEndian = definition.bigEndian,
                    rawBytes = file.copyOfRange(recordHeaderOffset, recordHeaderOffset + 1),
                    timestampEpochMillis = timestampMillis,
                    timestampOrigin = TimestampOrigin.RECONSTRUCTED_COMPRESSED
                )
            }
            for (field in definition.developerFields) {
                val offset = developerFieldBytes.getValue(field)
                if (decodedFields.size >= MAX_DECODED_FIELDS) return null
                decodedFields += DecodedField(
                    sourceFileSha256 = fileSha,
                    recordOrdinal = decodedRecordOrdinal,
                    globalMessageNumber = definition.globalNumber,
                    fieldNumber = field.number,
                    fieldKind = "DEVELOPER",
                    baseTypeCode = null,
                    fieldSize = field.size,
                    developerDataIndex = field.index,
                    bigEndian = definition.bigEndian,
                    rawBytes = file.copyOfRange(offset, offset + field.size),
                    timestampEpochMillis = timestampMillis,
                    timestampOrigin = timestampOrigin
                )
            }

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

            // Review-only candidates: never enter the production observation allowlist.
            // Field definitions from FIT Profile 21.222.0; preserve the raw profile
            // semantics and avoid guessing missing timestamps or manufacturer fields.
            if (definition.globalNumber == GLOBAL_SPO2_DATA ||
                definition.globalNumber == GLOBAL_STRESS_LEVEL) {
                if (definition.globalNumber == GLOBAL_SPO2_DATA) spo2RecordOrdinal++
                else stressRecordOrdinal++
                val ordinal = if (definition.globalNumber == GLOBAL_SPO2_DATA)
                    spo2RecordOrdinal else stressRecordOrdinal
                val candidateFields = if (definition.globalNumber == GLOBAL_SPO2_DATA)
                    listOf(
                        CandidateField(0, "reading_spo2", 2, 0xff, "percent"),
                        CandidateField(1, "reading_confidence", 2, 0xff, "profile_code"),
                        CandidateField(2, "spo2_measurement_mode", 0, 0xff, "profile_code")
                    )
                else listOf(CandidateField(0, "stress_level_value", 0x83, 0x7fff, "profile_raw"))
                val stressTime = if (definition.globalNumber == GLOBAL_STRESS_LEVEL)
                    fitDateTimeMillis(definition, fieldBytes, file) else null
                val stressTimeRelative = definition.globalNumber == GLOBAL_STRESS_LEVEL &&
                    definition.fields.firstOrNull { it.number == 1 && it.size == 4 && it.baseType and 0x1f == 6 }
                        ?.let { field -> fieldBytes[field]?.let { readU32(file, it, definition.bigEndian) } }
                        ?.let { it < MIN_ABSOLUTE_FIT_TIME } == true
                val candidateTime = if (definition.globalNumber == GLOBAL_STRESS_LEVEL) {
                    if (stressTimeRelative) null else stressTime?.first
                } else timestampMillis
                val candidateTimeOrigin = if (definition.globalNumber == GLOBAL_STRESS_LEVEL) {
                    if (stressTime != null) TimestampOrigin.DIRECT else TimestampOrigin.UNKNOWN
                } else timestampOrigin
                for (candidateField in candidateFields) {
                    val field = definition.fields.firstOrNull { it.number == candidateField.number }
                        ?: continue
                    val offset = fieldBytes[field] ?: continue
                    val raw = readCandidateValue(file, offset, field, definition.bigEndian, candidateField.baseType)
                    when {
                        raw == null -> {
                            unsupportedFields++
                            trackExcluded(
                                definition.globalNumber, candidateField.number, null,
                                candidateField.name, ExclusionReason.UNSUPPORTED_ENCODING
                            )
                        }
                        raw == candidateField.invalid -> {
                            invalidMeasurementFields++
                            trackExcluded(
                                definition.globalNumber, candidateField.number, null,
                                candidateField.name, ExclusionReason.INVALID_SENTINEL
                            )
                        }
                        else -> {
                            if (reviewCandidates.size >= MAX_OBSERVATIONS) return null
                            val scaled = if (candidateField.name == "respiration_rate")
                                raw.toDouble() / 100.0 else raw.toDouble()
                            reviewCandidates += ReviewCandidate(
                                profileFieldName = candidateField.name,
                                value = scaled,
                                unit = candidateField.unit,
                                timestampEpochMillis = candidateTime,
                                sourceFileSha256 = fileSha,
                                recordOrdinal = ordinal,
                                globalMessageNumber = definition.globalNumber,
                                fieldNumber = candidateField.number,
                                timestampOrigin = candidateTimeOrigin
                            )
                        }
                    }
                }
            }

            if (definition.globalNumber == GLOBAL_RESPIRATION_RATE) {
                respirationRecordOrdinal++
                val field = definition.fields.firstOrNull { it.number == 0 }
                val offset = field?.let(fieldBytes::get)
                if (field != null && offset != null) {
                    val raw = readSupportedSigned16(file, offset, field, definition.bigEndian)
                    when {
                        raw == null -> {
                            unsupportedFields++
                            trackExcluded(
                                GLOBAL_RESPIRATION_RATE, 0, null, "respiration_rate",
                                ExclusionReason.UNSUPPORTED_ENCODING
                            )
                        }
                        raw == 0x7fff -> {
                            invalidMeasurementFields++
                            trackExcluded(
                                GLOBAL_RESPIRATION_RATE, 0, null, "respiration_rate",
                                ExclusionReason.INVALID_SENTINEL
                            )
                        }
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

            // Garmin FIT Profile 21.222.0 defines fields 1/2 as skin-temperature
            // deviations in °C. Keep them review-only: the raw fields are always
            // persisted, and this does not establish clinical validity or a
            // canonical health observation. Field 3 is intentionally opaque;
            // field 4's unit/sentinel semantics are not sufficiently established.
            if (definition.globalNumber == GLOBAL_SKIN_TEMP_OVERNIGHT) {
                for ((fieldNumber, name) in listOf(
                    1 to "average_deviation",
                    2 to "average_7_day_deviation"
                )) {
                    val field = definition.fields.firstOrNull { it.number == fieldNumber } ?: continue
                    val offset = fieldBytes[field] ?: continue
                    val rawBits = readCandidateUnsigned32(file, offset, field, definition.bigEndian)
                    when {
                        rawBits == null -> {
                            unsupportedFields++
                            trackExcluded(
                                GLOBAL_SKIN_TEMP_OVERNIGHT, fieldNumber, null, name,
                                ExclusionReason.UNSUPPORTED_ENCODING
                            )
                        }
                        rawBits == 0xffff_ffffL -> {
                            invalidMeasurementFields++
                            trackExcluded(
                                GLOBAL_SKIN_TEMP_OVERNIGHT, fieldNumber, null, name,
                                ExclusionReason.INVALID_SENTINEL
                            )
                        }
                        !Float.fromBits(rawBits.toInt()).isFinite() -> {
                            invalidMeasurementFields++
                            trackExcluded(
                                GLOBAL_SKIN_TEMP_OVERNIGHT, fieldNumber, null, name,
                                ExclusionReason.NON_FINITE_VALUE
                            )
                        }
                        else -> {
                            if (reviewCandidates.size >= MAX_OBSERVATIONS) return null
                            reviewCandidates += ReviewCandidate(
                                profileFieldName = name,
                                value = Float.fromBits(rawBits.toInt()).toDouble(),
                                unit = "°C",
                                timestampEpochMillis = timestampMillis,
                                sourceFileSha256 = fileSha,
                                recordOrdinal = decodedRecordOrdinal,
                                globalMessageNumber = GLOBAL_SKIN_TEMP_OVERNIGHT,
                                fieldNumber = fieldNumber,
                                timestampOrigin = timestampOrigin
                            )
                        }
                    }
                }
            }

            // Keep sleep-level codes as review-only profile candidates. A
            // timestamped level transition is not by itself a validated sleep
            // interval or a derived sleep duration.
            if (definition.globalNumber == GLOBAL_SLEEP_LEVEL) {
                sleepRecordOrdinal++
                val field = definition.fields.firstOrNull { it.number == 0 }
                val offset = field?.let(fieldBytes::get)
                if (field != null && offset != null) {
                    val raw = readCandidateValue(file, offset, field, definition.bigEndian, expectedBaseType = 0)
                    when {
                        raw == null -> {
                            unsupportedFields++
                            trackExcluded(GLOBAL_SLEEP_LEVEL, 0, null, "sleep_level", ExclusionReason.UNSUPPORTED_ENCODING)
                        }
                        raw == invalidSentinel(field) -> {
                            invalidMeasurementFields++
                            trackExcluded(GLOBAL_SLEEP_LEVEL, 0, null, "sleep_level", ExclusionReason.INVALID_SENTINEL)
                        }
                        else -> {
                            if (reviewCandidates.size >= MAX_OBSERVATIONS) return null
                            reviewCandidates += ReviewCandidate(
                                profileFieldName = "sleep_level",
                                value = raw.toDouble(),
                                unit = "profile_code",
                                timestampEpochMillis = timestampMillis,
                                sourceFileSha256 = fileSha,
                                recordOrdinal = sleepRecordOrdinal,
                                globalMessageNumber = GLOBAL_SLEEP_LEVEL,
                                fieldNumber = 0,
                                timestampOrigin = timestampOrigin
                            )
                        }
                    }
                }
            }

            // Monitoring field 3 is profile "cycles", with steps only as an
            // activity-dependent subfield (walking/running). Preserve raw
            // cycles and activity code separately; do not guess a step count.
            if (definition.globalNumber == GLOBAL_MONITORING) {
                monitoringRecordOrdinal++
                val activity = definition.fields.firstOrNull { it.number == 5 }
                    ?.let(fieldBytes::get)?.let { offset ->
                        definition.fields.first { it.number == 5 }.let { field ->
                            readCandidateValue(file, offset, field, definition.bigEndian, expectedBaseType = 0)
                        }
                    }
                val cyclesField = definition.fields.firstOrNull { it.number == 3 }
                val cyclesOffset = cyclesField?.let(fieldBytes::get)
                if (cyclesField != null && cyclesOffset != null) {
                    val cycles = readCandidateValue(file, cyclesOffset, cyclesField,
                        definition.bigEndian, expectedBaseType = 6)
                    if (cycles != null && cycles != invalidSentinel(cyclesField)) {
                        if (reviewCandidates.size >= MAX_OBSERVATIONS) return null
                        reviewCandidates += ReviewCandidate(
                            profileFieldName = "monitoring_cycles",
                            value = cycles.toDouble(),
                            unit = "cycles",
                            timestampEpochMillis = timestampMillis,
                            sourceFileSha256 = fileSha,
                            recordOrdinal = monitoringRecordOrdinal,
                            globalMessageNumber = GLOBAL_MONITORING,
                            fieldNumber = 3,
                            timestampOrigin = timestampOrigin
                        )
                    } else if (cycles == null) {
                        unsupportedFields++
                        trackExcluded(GLOBAL_MONITORING, 3, null, "monitoring_cycles",
                            ExclusionReason.UNSUPPORTED_ENCODING)
                    } else {
                        invalidMeasurementFields++
                        trackExcluded(GLOBAL_MONITORING, 3, null, "monitoring_cycles",
                            ExclusionReason.INVALID_SENTINEL)
                    }
                }
                // #707: 55/24 current_activity_type_intensity (byte). Garmin FIT SDK components:
                // activity_type = bits 0-4, intensity = bits 5-7. 0xFF is the byte invalid value.
                definition.fields.firstOrNull { it.number == 24 && it.size == 1 }?.let { field24 ->
                    val offset24 = fieldBytes[field24] ?: return@let
                    val raw = readCandidateValue(file, offset24, field24, definition.bigEndian, expectedBaseType = 13)
                    when {
                        raw == null -> {
                            unsupportedFields++
                            trackExcluded(GLOBAL_MONITORING, 24, null, "current_activity_type_intensity",
                                ExclusionReason.UNSUPPORTED_ENCODING)
                        }
                        raw == 0xffL -> {
                            invalidMeasurementFields++
                            trackExcluded(GLOBAL_MONITORING, 24, null, "current_activity_type_intensity",
                                ExclusionReason.INVALID_SENTINEL)
                        }
                        else -> for ((name, value) in MonitoringActivityIntensity.components(raw.toInt())) {
                            if (reviewCandidates.size >= MAX_OBSERVATIONS) return null
                            reviewCandidates += ReviewCandidate(
                                profileFieldName = name,
                                value = value.toDouble(),
                                unit = "profile_code",
                                timestampEpochMillis = timestampMillis,
                                sourceFileSha256 = fileSha,
                                recordOrdinal = monitoringRecordOrdinal,
                                globalMessageNumber = GLOBAL_MONITORING,
                                fieldNumber = 24,
                                timestampOrigin = timestampOrigin,
                                // Two components share field 24: the component name keeps their keys apart.
                                idempotencyKey = sha256(("$fileSha:$monitoringRecordOrdinal:$GLOBAL_MONITORING:24:$name:" +
                                    REVIEW_CANDIDATE_DECODER_VERSION).toByteArray())
                            )
                        }
                    }
                }
                if (activity != null) {
                    if (reviewCandidates.size >= MAX_OBSERVATIONS) return null
                    reviewCandidates += ReviewCandidate(
                        profileFieldName = "monitoring_activity_type",
                        value = activity.toDouble(),
                        unit = "profile_code",
                        timestampEpochMillis = timestampMillis,
                        sourceFileSha256 = fileSha,
                        recordOrdinal = monitoringRecordOrdinal,
                        globalMessageNumber = GLOBAL_MONITORING,
                        fieldNumber = 5,
                        timestampOrigin = timestampOrigin
                    )
                }
            }

            if (definition.globalNumber == GLOBAL_RECORD || definition.globalNumber == GLOBAL_MONITORING) {
                recordOrdinal++
                for ((field, offset) in fieldBytes) {
                    val mapped = mapping(definition.globalNumber, field.number) ?: continue
                    val raw = readSupportedUnsigned(file, offset, field, definition.bigEndian, mapped.baseType)
                    if (raw == null) {
                        unsupportedFields++
                        trackExcluded(
                            definition.globalNumber, field.number, mapped.metric, null,
                            ExclusionReason.UNSUPPORTED_ENCODING
                        )
                        continue
                    }
                    if (raw == invalidSentinel(field)) {
                        invalidMeasurementFields++
                        trackExcluded(
                            definition.globalNumber, field.number, mapped.metric, null,
                            ExclusionReason.INVALID_SENTINEL
                        )
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

        // These profile-confirmed fields now enter the canonical FIT observation
        // stream. Keep decodedFields as the lossless source of raw bytes and never
        // turn unknown time/status values into measurements.
        val canonicalized = reviewCandidates.mapNotNull { candidate ->
            val metric = when (candidate.profileFieldName) {
                "stress_level_value" -> Metric.STRESS
                "respiration_rate" -> Metric.RESPIRATION
                "reading_spo2" -> Metric.SPO2
                "reading_confidence" -> Metric.SPO2_CONFIDENCE
                "spo2_measurement_mode" -> Metric.SPO2_MEASUREMENT_MODE
                "sleep_level" -> Metric.SLEEP_LEVEL
                "average_deviation" -> Metric.SKIN_TEMPERATURE_DEVIATION
                "average_7_day_deviation" -> Metric.SKIN_TEMPERATURE_7_DAY_DEVIATION
                "monitoring_cycles" -> Metric.MONITORING_CYCLES
                "monitoring_activity_type" -> Metric.MONITORING_ACTIVITY_TYPE
                MonitoringActivityIntensity.ACTIVITY_TYPE -> Metric.MONITORING_CURRENT_ACTIVITY_TYPE
                MonitoringActivityIntensity.INTENSITY -> Metric.MONITORING_INTENSITY_CODE
                else -> null
            } ?: return@mapNotNull null
            // Monitoring field 3 "cycles" has profile scale 2 (the steps/strokes subfields
            // are not applied here: no step count is derived from cycles).
            val value = if (metric == Metric.MONITORING_CYCLES) candidate.value / 2.0 else candidate.value
            // #696: status codes are not measurements; they stay as review candidates and raw
            // decoded fields. Stress: 1–100 only (0, -1, -2 are status). Respiration: positive
            // only (0 is kept as status, never "apnoea"; -100/-200/-300 are status). SpO2: only
            // the physical percent bounds, no medical threshold (0xFF was excluded above).
            // Confidence/mode/sleep level/activity type stay raw profile codes.
            if ((metric == Metric.STRESS && value !in 1.0..100.0) ||
                (metric == Metric.RESPIRATION && value <= 0.0) ||
                (metric == Metric.SPO2 && value !in 1.0..100.0) ||
                (metric == Metric.MONITORING_ACTIVITY_TYPE && value >= 255.0)) return@mapNotNull null
            val quality = if (candidate.timestampEpochMillis == null)
                Quality.MISSING_TIMESTAMP else Quality.PROFILE_VALID
            val semanticKey = "${metric.name}:${candidate.timestampEpochMillis ?: "missing"}:${value.toBits()}"
            Observation(
                metric = metric,
                timestampEpochMillis = candidate.timestampEpochMillis,
                value = value,
                unit = metric.unit,
                sourceFileSha256 = candidate.sourceFileSha256,
                recordOrdinal = candidate.recordOrdinal,
                globalMessageNumber = candidate.globalMessageNumber,
                fieldNumber = candidate.fieldNumber,
                quality = quality,
                timestampOrigin = candidate.timestampOrigin,
                idempotencyKey = candidate.idempotencyKey,
                duplicateFingerprint = sha256(semanticKey.toByteArray())
            )
        }
        if (observations.size + canonicalized.size > MAX_OBSERVATIONS) return null
        observations += canonicalized
        return Result(observations, recordOrdinal, unsupportedFields, deviceMetadata,
            unknown.map { (key, count) ->
                UnknownField(fileSha, key.global, key.field, key.baseType, key.size,
                    key.developerIndex, key.kind, count.definitions, count.occurrences)
            }, invalidMeasurementFields, reviewCandidates,
            excluded.map { (key, count) ->
                ExcludedField(
                    globalMessageNumber = key.global,
                    fieldNumber = key.field,
                    metric = key.metric,
                    reviewFieldName = key.reviewFieldName,
                    reason = key.reason,
                    occurrenceCount = count
                )
            }, decodedFields)
    }

    private const val MAX_DEVICE_METADATA = 10_000
    /**
     * Bound diagnostic metadata independently of the file-size cap. A crafted,
     * CRC-valid FIT can cycle local definitions across many global messages and
     * otherwise grow this map far beyond the observation limits.
     */
    private const val MAX_UNKNOWN_FIELD_SIGNATURES = 4_096

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
        return (previous + ((raw - (previous and 0xffffL)) and 0xffffL))
            .takeIf { it < INVALID_FIT_TIME }
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

    private fun readCandidateValue(
        file: ByteArray,
        offset: Int,
        field: FieldDefinition,
        bigEndian: Boolean,
        expectedBaseType: Int
    ): Long? {
        val base = field.baseType and 0x1f
        val expected = expectedBaseType and 0x1f
        // #707 real-file check: the CIRQA band writes the profile enum 269/2 (spo2 mode) as uint8 (0x02). A one-byte
        // enum and a uint8 carry the same value (the Garmin SDK reads both), so a single byte may use either type.
        val oneByteCodes = setOf(0, 2)
        if (base != expected && !(base in oneByteCodes && expected in oneByteCodes)) return null
        return when (base) {
            0, 2, 13 -> if (field.size == 1) (file[offset].toInt() and 0xff).toLong() else null // enum, uint8, byte
            3 -> if (field.size == 2) {
                val raw = readU16(file, offset, bigEndian)
                (if (raw and 0x8000 != 0) raw - 0x1_0000 else raw).toLong()
            } else null
            4 -> if (field.size == 2) readU16(file, offset, bigEndian).toLong() else null
            6 -> if (field.size == 4) readU32(file, offset, bigEndian) else null
            else -> null
        }
    }

    private fun readCandidateUnsigned32(
        file: ByteArray,
        offset: Int,
        field: FieldDefinition,
        bigEndian: Boolean
    ): Long? {
        if (field.baseType and 0x1f != 8 || field.size != 4) return null
        return readU32(file, offset, bigEndian)
    }

    /** Stress level field 1 is the record's own FIT timestamp, not message field 253. */
    private fun fitDateTimeMillis(
        definition: Definition,
        fieldBytes: Map<FieldDefinition, Int>,
        file: ByteArray
    ): Pair<Long, TimestampOrigin>? {
        val field = definition.fields.firstOrNull { it.number == 1 && it.size == 4 } ?: return null
        if (field.baseType and 0x1f != 6) return null
        val offset = fieldBytes[field] ?: return null
        val seconds = readU32(file, offset, definition.bigEndian)
        if (seconds !in MIN_ABSOLUTE_FIT_TIME until INVALID_FIT_TIME) return null
        return (seconds + FIT_EPOCH_UNIX_SECONDS) * 1_000L to TimestampOrigin.DIRECT
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
            .lowerHex()
}

/**
 * #707: FIT monitoring field 24 `current_activity_type_intensity` is one byte with two profile components
 * (Garmin FIT SDK; confirmed by Garmin in the SDK forum and by independent decoders): `activity_type = raw & 0x1F`,
 * `intensity = (raw >> 5) & 0x07`. Example 0xA8 = sedentary (8) / intensity 5, 0x46 = walking (6) / intensity 2.
 * The intensity is a context-dependent code (cadence for walking/running, log motion strength otherwise), not a
 * clinical load; it is unrelated to the separately profiled field 55/28.
 */
internal object MonitoringActivityIntensity {
    const val ACTIVITY_TYPE = "current_activity_type"
    const val INTENSITY = "current_intensity"

    fun components(raw: Int): List<Pair<String, Int>> {
        require(raw in 0..0xff)
        return listOf(ACTIVITY_TYPE to (raw and 0x1f), INTENSITY to ((raw shr 5) and 0x07))
    }
}
