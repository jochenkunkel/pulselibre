package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class FitHistoricalObservationsTest {
    private fun fit(data: ByteArray): ByteArray {
        val file = ByteArray(14 + data.size + 2)
        file[0] = 14
        file[1] = 0x20
        file[2] = 0x64
        file[4] = data.size.toByte()
        file[5] = (data.size ushr 8).toByte()
        ".FIT".toByteArray().copyInto(file, 8)
        val headerCrc = FitFileHeader.crc16(file, 0, 12)
        file[12] = headerCrc.toByte()
        file[13] = (headerCrc ushr 8).toByte()
        data.copyInto(file, 14)
        val crc = FitFileHeader.crc16(file, 0, file.size - 2)
        file[file.size - 2] = crc.toByte()
        file[file.size - 1] = (crc ushr 8).toByte()
        return file
    }

    private fun u32(value: Int) = byteArrayOf(
        value.toByte(), (value ushr 8).toByte(),
        (value ushr 16).toByte(), (value ushr 24).toByte()
    )

    private fun u16(value: Int) = byteArrayOf(value.toByte(), (value ushr 8).toByte())
    private fun s16(value: Int) = u16(value and 0xffff)
    private fun f32(value: Float) = u32(java.lang.Float.floatToRawIntBits(value))

    @Test fun extractsWhitelistedTimestampedDeviceMetadataAndIgnoresSerialNumber() {
        val data = byteArrayOf(
            0x40, 0, 0, 23, 0, 9,
            253.toByte(), 4, 0x86.toByte(), // timestamp
            1, 1, 2,                        // device_type
            2, 2, 0x84.toByte(),            // manufacturer
            3, 4, 0x8c.toByte(),            // serial_number (never exposed)
            4, 2, 0x84.toByte(),            // product
            5, 2, 0x84.toByte(),            // software_version / 100
            10, 2, 0x84.toByte(),           // battery_voltage / 256
            11, 1, 2,                       // battery_status code
            32, 1, 2,                       // battery_level
            0
        ) + u32(0x10000000 + 100_000) + byteArrayOf(7) + u16(15) + u32(123456) +
            u16(200) + u16(1234) + u16(3000) + byteArrayOf(2, 88)

        val result = FitHistoricalObservations.decode(fit(data))!!
        val metadata = result.deviceMetadata.single()
        assertEquals((0x10000000L + 100_000L + 631_065_600L) * 1_000L, metadata.timestampEpochMillis)
        assertEquals(7, metadata.deviceTypeCode)
        assertEquals(15, metadata.manufacturerCode)
        assertEquals(200, metadata.productCode)
        assertEquals(12.34, metadata.softwareVersion!!, 0.0)
        assertEquals(3000.0 / 256.0, metadata.batteryVoltageVolts!!, 0.0)
        assertEquals(2, metadata.batteryStatusCode)
        assertEquals(88, metadata.batteryLevelPercent)
        assertFalse(FitHistoricalObservations.DeviceMetadata::class.java.declaredFields
            .any { it.name.contains("serial", ignoreCase = true) })
    }

    @Test fun invalidBatterySentinelsAndOutOfRangeLevelsAreNotPromotedToMetadata() {
        val data = byteArrayOf(
            0x40, 0, 0, 23, 0, 3,
            10, 2, 0x84.toByte(),
            32, 1, 2,
            11, 1, 2,
            0
        ) + u16(0xffff) + byteArrayOf(120, 0xff.toByte())
        val metadata = FitHistoricalObservations.decode(fit(data))!!.deviceMetadata.single()
        assertNull(metadata.batteryVoltageVolts)
        assertNull(metadata.batteryLevelPercent)
        assertNull(metadata.batteryStatusCode)
    }

    @Test fun skinTemperatureProfileFieldsRemainRawAndKnownDeviationsAreReviewOnly() {
        val timestamp = 0x10000000 + 100_000
        val data = byteArrayOf(
            0x40, 0, 0, 398.toByte(), 1, 6,
            253.toByte(), 4, 0x86.toByte(), // timestamp
            0, 4, 0x86.toByte(),            // local_timestamp
            1, 4, 0x88.toByte(),            // average_deviation, float32
            2, 4, 0x88.toByte(),            // average_7_day_deviation, float32
            3, 1, 2,                        // profile-unmapped field; preserve opaque
            4, 4, 0x88.toByte(),            // nightly_value; no candidate unit inferred
            0
        ) + u32(timestamp) + u32(timestamp + 1) + f32(1.25f) + f32(-0.5f) +
            byteArrayOf(7) + f32(2.0f)

        val result = FitHistoricalObservations.decode(fit(data))!!

        assertEquals(
            listOf(FitHistoricalObservations.Metric.SKIN_TEMPERATURE_DEVIATION,
                FitHistoricalObservations.Metric.SKIN_TEMPERATURE_7_DAY_DEVIATION),
            result.observations.map { it.metric }
        )
        assertEquals(
            listOf("average_deviation", "average_7_day_deviation"),
            result.reviewCandidates.map { it.profileFieldName }
        )
        assertEquals(listOf(1.25, -0.5), result.reviewCandidates.map { it.value })
        assertTrue(result.reviewCandidates.all { it.unit == "°C" })
        assertTrue(result.reviewCandidates.all {
            it.timestampEpochMillis == (timestamp + 631_065_600L) * 1_000L
        })
        assertEquals(listOf(3), result.unknownFields.map { it.fieldNumber })
        assertEquals(listOf(253, 0, 1, 2, 3, 4), result.decodedFields.map { it.fieldNumber })
        assertArrayEquals(f32(1.25f), result.decodedFields[2].rawBytes)
        assertArrayEquals(f32(-0.5f), result.decodedFields[3].rawBytes)
        assertArrayEquals(byteArrayOf(7), result.decodedFields[4].rawBytes)
        assertArrayEquals(f32(2.0f), result.decodedFields[5].rawBytes)
    }

    @Test fun skinTemperatureReviewCandidatesRejectNonFiniteAndFitFloatSentinel() {
        val data = byteArrayOf(
            0x40, 0, 0, 398.toByte(), 1, 3,
            253.toByte(), 4, 0x86.toByte(),
            1, 4, 0x88.toByte(),
            2, 4, 0x88.toByte(),
            0
        ) + u32(0x10000000 + 100_000) + f32(Float.POSITIVE_INFINITY) + u32(-1)

        val result = FitHistoricalObservations.decode(fit(data))!!

        assertTrue(result.reviewCandidates.isEmpty())
        assertEquals(2, result.invalidMeasurementFieldCount)
        assertEquals(
            mapOf(FitHistoricalObservations.ExclusionReason.INVALID_SENTINEL to 1,
                FitHistoricalObservations.ExclusionReason.NON_FINITE_VALUE to 1),
            result.excludedFields.associate { it.reason to it.occurrenceCount }
        )
        assertEquals(3, result.decodedFields.size)
        assertArrayEquals(f32(Float.POSITIVE_INFINITY), result.decodedFields[1].rawBytes)
        assertArrayEquals(u32(-1), result.decodedFields[2].rawBytes)
    }

    @Test fun decodesSupportedRecordMeasurementsAndCompressedTimestamp() {
        val timestamp = 0x10000000 + 100_000
        val data = byteArrayOf(
            0x40, 0, 0, 20, 0, 3,
            253.toByte(), 4, 0x86.toByte(), // timestamp uint32
            3, 1, 2,                         // heart_rate uint8
            4, 1, 2,                         // cadence uint8
            0,                               // normal data record
        ) + u32(timestamp) + byteArrayOf(72, 90) + byteArrayOf(
            0x80.toByte(),                   // compressed record, local type 0, timestamp delta 0
        ) + u32(timestamp) + byteArrayOf(73, 91)

        val result = FitHistoricalObservations.decode(fit(data))!!
        assertEquals(2, result.dataRecordCount)
        assertEquals(listOf(72.0, 73.0), result.observations
            .filter { it.metric == FitHistoricalObservations.Metric.HEART_RATE }
            .map { it.value })
        assertEquals(listOf(90.0, 91.0), result.observations
            .filter { it.metric == FitHistoricalObservations.Metric.CADENCE }
            .map { it.value })
        assertEquals(1, result.observations.map { it.timestampEpochMillis }.distinct().size)
        assertEquals(
            (timestamp + 631_065_600L) * 1_000L,
            result.observations.first().timestampEpochMillis
        )
        assertEquals(listOf(253, 3, 4, 253, 3, 4, 253), result.decodedFields.map { it.fieldNumber })
        assertArrayEquals(u32(timestamp), result.decodedFields.first().rawBytes)
        assertArrayEquals(byteArrayOf(73), result.decodedFields[4].rawBytes)
        assertEquals("COMPRESSED_TIMESTAMP_HEADER", result.decodedFields.last().fieldKind)
        assertArrayEquals(byteArrayOf(0x80.toByte()), result.decodedFields.last().rawBytes)
        assertTrue(result.observations.all {
            it.quality == FitHistoricalObservations.Quality.PROFILE_VALID
        })
    }

    @Test fun compressedTimestampUsesDifferentDefinitionWithoutTimestampField() {
        val anchor = 0x4000001e
        val data = byteArrayOf(
            0x40, 0, 0, 20, 0, 2,
            253.toByte(), 4, 0x86.toByte(), 3, 1, 2,
            0
        ) + u32(anchor) + byteArrayOf(70, 0x41, 0, 0, 20, 0, 1, 3, 1, 2,
            0xa1.toByte(), 71, 0xa1.toByte(), 72)
        val result = FitHistoricalObservations.decode(fit(data))!!
        assertEquals(listOf(70.0, 71.0, 72.0), result.observations.map { it.value })
        assertEquals(listOf(anchor.toLong(), anchor.toLong() + 3, anchor.toLong() + 3).map {
            (it + 631_065_600L) * 1000
        }, result.observations.map { it.timestampEpochMillis })
        assertEquals(2, result.decodedFields.count { it.fieldKind == "COMPRESSED_TIMESTAMP_HEADER" })
    }

    @Test fun compressedTimestampSupportsAllLocalTypesAndRejectsMissingAnchorAndBadCrc() {
        for (local in 0..3) {
            val anchor = 0x4000001e
            val definition = byteArrayOf(
                (0x40 + local).toByte(), 0, 0, 20, 0, 1, 3, 1, 2
            )
            val compressed = byteArrayOf((0x80 or (local shl 5) or 1).toByte(), 77)
            val anchorDef = byteArrayOf(0x4f, 0, 0, 20, 0, 1, 253.toByte(), 4, 0x86.toByte(), 15)
            val file = fit(anchorDef + u32(anchor) + definition + compressed)
            val result = FitHistoricalObservations.decode(file)!!
            assertEquals(77.0, result.observations.single().value, 0.0)
            assertEquals((anchor.toLong() + 3 + 631_065_600L) * 1000,
                result.observations.single().timestampEpochMillis)
            assertNull(FitHistoricalObservations.decode(fit(definition + compressed)))
            val corrupt = file.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
            assertNull(FitHistoricalObservations.decode(corrupt))
        }
    }

    @Test fun directRelativeAndInvalidTimesAreNotInterpretedAsUtc() {
        val def = byteArrayOf(0x40, 0, 0, 20, 0, 2,
            253.toByte(), 4, 0x86.toByte(), 3, 1, 2)
        val data = def + byteArrayOf(0) + u32(0x0fffffff) + byteArrayOf(70) +
            byteArrayOf(0) + u32(0x10000000) + byteArrayOf(71) +
            byteArrayOf(0) + u32(-1) + byteArrayOf(72)
        val observations = FitHistoricalObservations.decode(fit(data))!!.observations
        assertNull(observations[0].timestampEpochMillis)
        assertEquals((0x10000000L + 631_065_600L) * 1000, observations[1].timestampEpochMillis)
        assertNull(observations[2].timestampEpochMillis)
        assertEquals(FitHistoricalObservations.Quality.MISSING_TIMESTAMP, observations[0].quality)
    }

    @Test fun invalidDirectTimestampFallsBackToMonitoringTimestamp16() {
        val definition = byteArrayOf(0x40, 0, 0, 55, 0, 3,
            253.toByte(), 4, 0x86.toByte(), 26, 2, 0x84.toByte(), 27, 1, 2)
        val anchor = 0x4000fffe
        val data = definition + byteArrayOf(0) + u32(anchor) + u16(0xffff) + byteArrayOf(70) +
            byteArrayOf(0) + u32(-1) + u16(2) + byteArrayOf(71)
        val observations = FitHistoricalObservations.decode(fit(data))!!.observations
        assertEquals((anchor.toLong() + 4 + 631_065_600L) * 1000, observations[1].timestampEpochMillis)
        assertEquals(FitHistoricalObservations.TimestampOrigin.RECONSTRUCTED_TS16, observations[1].timestampOrigin)
    }

    @Test fun relativeClockDoesNotReusePreviousAbsoluteAnchor() {
        val def = byteArrayOf(0x40, 0, 0, 20, 0, 2,
            253.toByte(), 4, 0x86.toByte(), 3, 1, 2)
        val short = byteArrayOf(0x41, 0, 0, 20, 0, 1, 3, 1, 2)
        val data = def + byteArrayOf(0) + u32(0x40000000) + byteArrayOf(70) +
            byteArrayOf(0) + u32(42) + byteArrayOf(71) + short + byteArrayOf(0xa1.toByte(), 72)
        assertNull(FitHistoricalObservations.decode(fit(data)))
    }

    @Test fun relativeStressTimeDoesNotFallBackToAnUnrelatedMessageTimestamp() {
        val def = byteArrayOf(0x40, 0, 0, 0xe3.toByte(), 0, 3,
            253.toByte(), 4, 0x86.toByte(), 0, 2, 0x83.toByte(), 1, 4, 0x86.toByte())
        val data = def + byteArrayOf(0) + u32(0x40000000) + s16(42) + u32(10)
        val candidate = FitHistoricalObservations.decode(fit(data))!!.reviewCandidates.single()
        assertNull(candidate.timestampEpochMillis)
        assertEquals(FitHistoricalObservations.TimestampOrigin.UNKNOWN, candidate.timestampOrigin)
    }

    @Test fun truncatedCompressedRecordAndChainedFitAreRejectedWithoutPartialImport() {
        val anchor = byteArrayOf(0x40, 0, 0, 20, 0, 1, 253.toByte(), 4, 0x86.toByte(),
            0, 0x1e, 0, 0, 0x40)
        val definition = byteArrayOf(0x41, 0, 0, 20, 0, 1, 3, 1, 2)
        // Valid CRC but missing the defined heart-rate byte.
        assertNull(FitHistoricalObservations.decode(fit(anchor + definition + byteArrayOf(0xa1.toByte()))))
        val first = fit(anchor)
        val second = fit(anchor)
        // Current single-envelope API does not accept FIT chaining; reject, do
        // not silently claim the first member is the complete source.
        assertNull(FitHistoricalObservations.decode(first + second))
    }

    @Test fun compressedTimestampCannotRollIntoInvalidSentinel() {
        val data = byteArrayOf(
            0x40, 0, 0, 20, 0, 1, 253.toByte(), 4, 0x86.toByte(),
            0, 0xfe.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
            0x41, 0, 0, 20, 0, 1, 3, 1, 2,
            0xbf.toByte(), 70
        )
        assertNull(FitHistoricalObservations.decode(fit(data)))
    }

    @Test fun monitoringCyclesAreNotGuessedAsStepCounts() {
        val data = byteArrayOf(
            0x40, 0, 0, 55, 0, 2,
            253.toByte(), 4, 0x86.toByte(),
            3, 4, 0x86.toByte(), // FIT Profile Monitoring.cycles
            0
        ) + u32(200_000) + u32(0x1234)
        val result = FitHistoricalObservations.decode(fit(data))!!
        assertEquals(FitHistoricalObservations.Metric.MONITORING_CYCLES,
            result.observations.single().metric)
        // Profile scale 2: the stored raw count is twice the cycles.
        assertEquals(0x1234 / 2.0, result.observations.single().value, 0.0)
        assertFalse(result.observations.any { it.metric == FitHistoricalObservations.Metric.CADENCE })
    }

    @Test fun spo2AndStressBecomeCanonicalAndRespectInvalidSentinels() {
        val spo2Definition = byteArrayOf(
            0x40, 0, 0, 0x0d, 0x01, 2, // global message 269
            253.toByte(), 4, 0x86.toByte(), 0, 1, 2
        )
        val spo2 = FitHistoricalObservations.decode(fit(
            spo2Definition +
                byteArrayOf(0) + u32(200_000) + byteArrayOf(97) +
                byteArrayOf(0) + u32(200_060) + byteArrayOf(0xff.toByte())
        ))!!
        assertEquals(listOf(FitHistoricalObservations.Metric.SPO2), spo2.observations.map { it.metric })
        assertEquals(97.0, spo2.observations.single().value, 0.0)
        assertEquals(1, spo2.reviewCandidates.size)
        assertEquals("reading_spo2", spo2.reviewCandidates.single().profileFieldName)
        assertEquals(97.0, spo2.reviewCandidates.single().value, 0.0)
        assertEquals("percent", spo2.reviewCandidates.single().unit)
        assertEquals(1, spo2.invalidMeasurementFieldCount)
        assertEquals(
            FitHistoricalObservations.ExcludedField(
                globalMessageNumber = 269,
                fieldNumber = 0,
                metric = null,
                reviewFieldName = "reading_spo2",
                reason = FitHistoricalObservations.ExclusionReason.INVALID_SENTINEL,
                occurrenceCount = 1
            ),
            spo2.excludedFields.single()
        )

        val stressDefinition = byteArrayOf(
            0x40, 0, 0, 0xe3.toByte(), 0, 1, // global message 227
            0, 2, 0x83.toByte()
        )
        val stress = FitHistoricalObservations.decode(fit(
            stressDefinition +
                byteArrayOf(0) + s16(42) +
                byteArrayOf(0) + s16(0x7fff)
        ))!!
        assertEquals(listOf(FitHistoricalObservations.Metric.STRESS), stress.observations.map { it.metric })
        assertEquals(1, stress.reviewCandidates.size)
        assertEquals("stress_level_value", stress.reviewCandidates.single().profileFieldName)
        assertEquals(42.0, stress.reviewCandidates.single().value, 0.0)
        assertEquals("profile_raw", stress.reviewCandidates.single().unit)
        assertNull(stress.reviewCandidates.single().timestampEpochMillis)
        assertEquals(FitHistoricalObservations.Quality.MISSING_TIMESTAMP,
            stress.observations.single().quality)
        assertEquals(1, stress.invalidMeasurementFieldCount)
    }

    @Test fun spo2ModeWrittenAsUint8IsAcceptedLikeTheGarminSdk() {
        // #707: on all 8 real CIRQA spo2_data definitions field 2 (mode) is uint8 (0x02), not enum (0x00).
        val definition = byteArrayOf(0x40, 0, 0, 0x0d, 0x01, 4, 253.toByte(), 4, 0x86.toByte(), 0, 1, 2, 1, 1, 2, 2, 1, 2)
        val result = FitHistoricalObservations.decode(fit(definition + byteArrayOf(0) + u32(0x10000000 + 1) + byteArrayOf(95, 3, 1)))!!
        assertEquals(listOf("reading_spo2", "reading_confidence", "spo2_measurement_mode"), result.reviewCandidates.map { it.profileFieldName })
        assertEquals(1.0, result.observations.single { it.metric == FitHistoricalObservations.Metric.SPO2_MEASUREMENT_MODE }.value, 0.0)
        assertEquals(0, result.unsupportedMeasurementFieldCount)
    }

    @Test fun spo2ConfidenceAndModeAreCanonicalSeparateProfileCodes() {
        val definition = byteArrayOf(
            0x40, 0, 0, 0x0d, 0x01, 4,
            253.toByte(), 4, 0x86.toByte(),
            0, 1, 2, // reading_spo2
            1, 1, 2, // reading_confidence
            2, 1, 0 // spo2_measurement_mode enum
        )
        val result = FitHistoricalObservations.decode(fit(
            definition + byteArrayOf(0) + u32(200_000) + byteArrayOf(96, 3, 1)
        ))!!

        assertEquals(
            listOf(FitHistoricalObservations.Metric.SPO2, FitHistoricalObservations.Metric.SPO2_CONFIDENCE,
                FitHistoricalObservations.Metric.SPO2_MEASUREMENT_MODE),
            result.observations.map { it.metric }
        )
        assertEquals(
            listOf("reading_spo2", "reading_confidence", "spo2_measurement_mode"),
            result.reviewCandidates.map { it.profileFieldName }
        )
        assertEquals(listOf(96.0, 3.0, 1.0), result.reviewCandidates.map { it.value })
        assertEquals(listOf(0, 1, 2), result.reviewCandidates.map { it.fieldNumber })
    }

    @Test fun sleepLevelAndMonitoringCyclesAreCanonicalWithoutStepInference() {
        val sleepDefinition = byteArrayOf(
            0x40, 0, 0, 0x13, 0x01, 2, // global 275
            253.toByte(), 4, 0x86.toByte(),
            0, 1, 0 // enum sleep level
        )
        val monitoringDefinition = byteArrayOf(
            0x40, 0, 0, 55, 0, 3,
            253.toByte(), 4, 0x86.toByte(),
            3, 4, 0x86.toByte(), // cycles
            5, 1, 0 // activity_type enum
        )
        val result = FitHistoricalObservations.decode(fit(
            sleepDefinition + byteArrayOf(0) + u32(200_000) + byteArrayOf(2) +
                monitoringDefinition + byteArrayOf(0) + u32(200_060) + u32(120) + byteArrayOf(6)
        ))!!

        assertEquals(
            listOf(FitHistoricalObservations.Metric.SLEEP_LEVEL,
                FitHistoricalObservations.Metric.MONITORING_CYCLES,
                FitHistoricalObservations.Metric.MONITORING_ACTIVITY_TYPE),
            result.observations.map { it.metric }
        )
        assertFalse(result.observations.any { it.metric.name.contains("STEP") })
        assertEquals(
            listOf("sleep_level", "monitoring_cycles", "monitoring_activity_type"),
            result.reviewCandidates.map { it.profileFieldName }
        )
        assertEquals(listOf(2.0, 120.0, 6.0), result.reviewCandidates.map { it.value })
        assertEquals(listOf(275, 55, 55), result.reviewCandidates.map { it.globalMessageNumber })
    }

    @Test fun stressLevelTimeBecomesReviewCandidateTimestampUsingFitEpoch() {
        val definition = byteArrayOf(
            0x40, 0, 0, 0xe3.toByte(), 0, 2, // global message 227
            0, 2, 0x83.toByte(), // stress_level_value: sint16
            1, 4, 0x86.toByte()  // stress_level_time: uint32 seconds, origin unverified
        )
        val result = FitHistoricalObservations.decode(fit(
            definition + byteArrayOf(0) + s16(42) + u32(0x10000000 + 200_000)
        ))!!

        assertEquals(FitHistoricalObservations.Metric.STRESS, result.observations.single().metric)
        val candidate = result.reviewCandidates.single()
        assertEquals("stress_level_value", candidate.profileFieldName)
        assertEquals(42.0, candidate.value, 0.0)
        assertEquals(fitTime(0x10000000L + 200_000), candidate.timestampEpochMillis)
        assertEquals(FitHistoricalObservations.TimestampOrigin.DIRECT, candidate.timestampOrigin)
        assertEquals(candidate.timestampEpochMillis, result.observations.single().timestampEpochMillis)
    }

    @Test fun statusCodesAndZerosAreNeverCanonicalAndSpo2HasNoMedicalThreshold() {
        // #696: stress 0/-1/-2 and respiration 0/-1/-2/-3 (scaled) are statuses; they stay review candidates only.
        val stressDefinition = byteArrayOf(
            0x40, 0, 0, 0xe3.toByte(), 0, 2, // global 227
            0, 2, 0x83.toByte(), 1, 4, 0x86.toByte() // stress_level_value sint16, stress_level_time
        )
        val stress = FitHistoricalObservations.decode(fit(stressDefinition +
            byteArrayOf(0) + s16(0) + u32(0x10000000 + 1) +
            byteArrayOf(0) + s16(-1) + u32(0x10000000 + 2) +
            byteArrayOf(0) + s16(-2) + u32(0x10000000 + 3) +
            byteArrayOf(0) + s16(1) + u32(0x10000000 + 4) +
            byteArrayOf(0) + s16(100) + u32(0x10000000 + 5)))!!
        assertEquals(listOf(1.0, 100.0), stress.observations.map { it.value })
        assertEquals(listOf(0.0, -1.0, -2.0, 1.0, 100.0), stress.reviewCandidates.map { it.value })

        val respDefinition = byteArrayOf(0x40, 0, 0, 0x29, 0x01, 2, 253.toByte(), 4, 0x86.toByte(), 0, 2, 0x83.toByte())
        val resp = FitHistoricalObservations.decode(fit(respDefinition +
            byteArrayOf(0) + u32(0x10000000 + 10) + s16(0) +
            byteArrayOf(0) + u32(0x10000000 + 11) + s16(-100) +
            byteArrayOf(0) + u32(0x10000000 + 12) + s16(-300) +
            byteArrayOf(0) + u32(0x10000000 + 13) + s16(1_550)))!!
        assertEquals(listOf(15.5), resp.observations.map { it.value })
        assertEquals(4, resp.reviewCandidates.size)

        // SpO2: no 70 % decoder threshold (owner: no medical limits as filters); only physical percent bounds.
        val spo2Definition = byteArrayOf(0x40, 0, 0, 0x0d, 0x01, 2, 253.toByte(), 4, 0x86.toByte(), 0, 1, 2)
        val spo2 = FitHistoricalObservations.decode(fit(spo2Definition +
            byteArrayOf(0) + u32(200_000) + byteArrayOf(65) +
            byteArrayOf(0) + u32(200_060) + byteArrayOf(0) +
            byteArrayOf(0) + u32(200_120) + byteArrayOf(101)))!!
        assertEquals(listOf(65.0), spo2.observations.map { it.value })
        assertEquals(listOf(65.0, 0.0, 101.0), spo2.reviewCandidates.map { it.value })
    }

    @Test fun respirationRateIsCanonicalAndNegativeStatusesAreNotMeasurements() {
        val definition = byteArrayOf(
            0x40, 0, 0, 0x29, 0x01, 2, // global message 297
            253.toByte(), 4, 0x86.toByte(), // FIT timestamp
            0, 2, 0x83.toByte() // respiration_rate: sint16
        )
        val data = definition +
            byteArrayOf(0) + u32(0x10000000 + 210_000) + s16(1_234) +
            byteArrayOf(0) + u32(0x10000000 + 210_060) + s16(-125) +
            byteArrayOf(0) + u32(0x10000000 + 210_120) + s16(0x7fff)

        val result = FitHistoricalObservations.decode(fit(data))!!

        assertEquals(listOf(12.34), result.observations.map { it.value })
        assertEquals(FitHistoricalObservations.Metric.RESPIRATION, result.observations.single().metric)
        assertEquals(listOf(12.34, -1.25), result.reviewCandidates.map { it.value })
        assertEquals(listOf("breaths/min", "breaths/min"), result.reviewCandidates.map { it.unit })
        assertEquals(
            listOf(fitTime(0x10000000L + 210_000), fitTime(0x10000000L + 210_060)),
            result.reviewCandidates.map { it.timestampEpochMillis }
        )
        assertEquals(listOf(1, 2), result.reviewCandidates.map { it.recordOrdinal })
        assertEquals(1, result.invalidMeasurementFieldCount)
        assertTrue(result.reviewCandidates.all {
            it.profileFieldName == "respiration_rate" &&
                it.globalMessageNumber == 297 && it.fieldNumber == 0
        })
        assertFalse(result.observations.any { it.value < 0.0 })
    }

    @Test fun respirationRateCandidateRejectsWrongBaseTypeAndRetainsMissingTimestamp() {
        val wrongType = byteArrayOf(
            0x40, 0, 0, 0x29, 0x01, 1,
            0, 2, 0x84.toByte(), // profile field with incompatible uint16 type
            0
        ) + u16(1_234)
        val unsupported = FitHistoricalObservations.decode(fit(wrongType))!!
        assertTrue(unsupported.reviewCandidates.isEmpty())
        assertEquals(1, unsupported.unsupportedMeasurementFieldCount)
        assertEquals(
            FitHistoricalObservations.ExclusionReason.UNSUPPORTED_ENCODING,
            unsupported.excludedFields.single().reason
        )
        assertEquals("respiration_rate", unsupported.excludedFields.single().reviewFieldName)

        val noTimestamp = byteArrayOf(
            0x40, 0, 0, 0x29, 0x01, 1,
            0, 2, 0x83.toByte(),
            0
        ) + s16(1_234)
        val candidate = FitHistoricalObservations.decode(fit(noTimestamp))!!.reviewCandidates.single()
        assertEquals(12.34, candidate.value, 0.0)
        assertNull(candidate.timestampEpochMillis)
    }

    // FIT profile: monitoring.timestamp_16 (field 26) = last full timestamp + 16-bit difference (mod 2^16).
    private val monitoringFull = byteArrayOf(0x40, 0, 0, 55, 0, 2, 253.toByte(), 4, 0x86.toByte(), 27, 1, 2)
    private val monitoringShort = byteArrayOf(0x41, 0, 0, 55, 0, 2, 26, 2, 0x84.toByte(), 27, 1, 2)
    private fun fitTime(seconds: Long) = (seconds + 631_065_600L) * 1_000L

    @Test fun monitoringHeartRateWithTimestamp16IsReconstructedFromTheLastFullTimestamp() {
        val base = 0x10000000 + 131_056 // low 16 bits 0xFFF0: the next value wraps around
        val data = monitoringFull + monitoringShort +
            byteArrayOf(0) + u32(base) + byteArrayOf(60) +
            byteArrayOf(1) + u16(0x0010) + byteArrayOf(70) +        // wrap: base + 32
            byteArrayOf(1) + u16(0x0030) + byteArrayOf(72)          // continues from the reconstructed value: base + 64
        val result = FitHistoricalObservations.decode(fit(data))!!.observations
        assertEquals(listOf(fitTime(base.toLong()), fitTime(base + 32L), fitTime(base + 64L)),
            result.map { it.timestampEpochMillis })
        assertEquals(listOf(60.0, 70.0, 72.0), result.map { it.value })
        assertTrue(result.all { it.quality == FitHistoricalObservations.Quality.PROFILE_VALID })
        assertEquals(
            listOf("DIRECT", "RECONSTRUCTED_TS16", "RECONSTRUCTED_TS16"),
            result.map { it.timestampOrigin.name }
        )
    }

    @Test fun timestamp16WithoutEarlierFullTimestampOrWithInvalidMarkerStaysMissing() {
        val noBase = FitHistoricalObservations.decode(fit(monitoringShort + byteArrayOf(1) + u16(500) + byteArrayOf(70)))!!
            .observations.single()
        assertNull(noBase.timestampEpochMillis)
        assertEquals(FitHistoricalObservations.Quality.MISSING_TIMESTAMP, noBase.quality)

        val invalid = FitHistoricalObservations.decode(fit(monitoringFull + monitoringShort +
            byteArrayOf(0) + u32(0x10000000 + 200_000) + byteArrayOf(60) +
            byteArrayOf(1) + u16(0xFFFF) + byteArrayOf(70)))!!.observations
        assertEquals(FitHistoricalObservations.Quality.PROFILE_VALID, invalid[0].quality)
        assertEquals(FitHistoricalObservations.Quality.MISSING_TIMESTAMP, invalid[1].quality)
    }

    // ---- #707 Abschnitt X: Testvektoren für 55/24, 55/26, 55/27 ----

    @Test fun issue707ActivityTypeIntensityComponentsForAll256Bytes() {
        assertEquals(listOf(MonitoringActivityIntensity.ACTIVITY_TYPE to 8, MonitoringActivityIntensity.INTENSITY to 5),
            MonitoringActivityIntensity.components(0xA8)) // sedentary, intensity 5
        assertEquals(listOf(MonitoringActivityIntensity.ACTIVITY_TYPE to 6, MonitoringActivityIntensity.INTENSITY to 2),
            MonitoringActivityIntensity.components(0x46)) // walking, intensity 2
        for (raw in 0..255) {
            val (type, intensity) = MonitoringActivityIntensity.components(raw).map { it.second }
            assertEquals("roundtrip $raw", raw, type + 32 * intensity)
            assertTrue(type in 0..31 && intensity in 0..7)
        }
    }

    @Test fun issue707Field24IsDecodedIntoTwoCanonicalCodesAndInvalidByteIsExcluded() {
        val definition = byteArrayOf(0x40, 0, 0, 55, 0, 2, 253.toByte(), 4, 0x86.toByte(), 24, 1, 0x0D) // byte
        val result = FitHistoricalObservations.decode(fit(definition +
            byteArrayOf(0) + u32(0x10000000 + 100) + byteArrayOf(0xA8.toByte()) +
            byteArrayOf(0) + u32(0x10000000 + 160) + byteArrayOf(0x46) +
            byteArrayOf(0) + u32(0x10000000 + 220) + byteArrayOf(0xFF.toByte())))!!
        val byMetric = result.observations.groupBy { it.metric }
        assertEquals(listOf(8.0, 6.0), byMetric.getValue(FitHistoricalObservations.Metric.MONITORING_CURRENT_ACTIVITY_TYPE).map { it.value })
        assertEquals(listOf(5.0, 2.0), byMetric.getValue(FitHistoricalObservations.Metric.MONITORING_INTENSITY_CODE).map { it.value })
        assertTrue(result.observations.all { it.fieldNumber == 24 && it.quality == FitHistoricalObservations.Quality.PROFILE_VALID })
        // Two components of one field need distinct keys (idempotent import).
        assertEquals(result.observations.size, result.observations.map { it.idempotencyKey }.distinct().size)
        assertEquals(1, result.invalidMeasurementFieldCount)
        assertFalse("no step or heart-rate inference", result.observations.any {
            it.metric == FitHistoricalObservations.Metric.HEART_RATE || it.metric.name.contains("STEP") })
    }

    @Test fun issue707Timestamp16TicketVectorAddsSixtySeconds() {
        val anchor = 0x1000A6D4
        val result = FitHistoricalObservations.decode(fit(monitoringFull + monitoringShort +
            byteArrayOf(0) + u32(anchor) + byteArrayOf(60) +
            byteArrayOf(1) + u16(0xA710) + byteArrayOf(61)))!!.observations
        assertEquals(fitTime(anchor + 60L), result[1].timestampEpochMillis)
    }

    @Test fun issue707HeartRateZeroInvalidAndMissingAreNeverMeasurements() {
        val onlyTime = byteArrayOf(0x42, 0, 0, 55, 0, 1, 26, 2, 0x84.toByte()) // timestamp_16 without field 27
        val result = FitHistoricalObservations.decode(fit(monitoringFull + monitoringShort + onlyTime +
            byteArrayOf(0) + u32(0x10000000 + 1_000) + byteArrayOf(72) +
            byteArrayOf(1) + u16((0x10000000 + 1_060) and 0xffff) + byteArrayOf(0) +            // HR 0: no valid HR
            byteArrayOf(1) + u16((0x10000000 + 1_120) and 0xffff) + byteArrayOf(0xFF.toByte()) + // invalid
            byteArrayOf(2) + u16((0x10000000 + 1_180) and 0xffff)))!!                             // field 27 missing
        val valid = result.observations.filter { it.quality == FitHistoricalObservations.Quality.PROFILE_VALID }
        assertEquals(listOf(72.0), valid.map { it.value })
        assertTrue("0 bpm is never a valid measurement", result.observations.none {
            it.value == 0.0 && it.quality == FitHistoricalObservations.Quality.PROFILE_VALID })
        assertEquals("no heart rate invented for the missing field", 2, result.observations.size)
    }

    @Test fun timestamp16IsOnlyReconstructedForMonitoringMessages() {
        // record (global 20) with a field 26 must not be treated as timestamp_16
        val data = byteArrayOf(0x40, 0, 0, 20, 0, 2, 253.toByte(), 4, 0x86.toByte(), 3, 1, 2) +
            byteArrayOf(0) + u32(300_000) + byteArrayOf(60) +
            byteArrayOf(0x41, 0, 0, 20, 0, 2, 26, 2, 0x84.toByte(), 3, 1, 2) +
            byteArrayOf(1) + u16(300_050) + byteArrayOf(80)
        val result = FitHistoricalObservations.decode(fit(data))!!.observations
        assertEquals(FitHistoricalObservations.Quality.MISSING_TIMESTAMP, result[1].quality)
    }

    @Test fun incompatibleTimestampDefinitionIsInventoriedAndCannotBackCompressedTimestamp() {
        val incompatibleTimestampDefinition = byteArrayOf(
            0x41, 0, 0, 20, 0, 2,
            253.toByte(), 1, 2, // timestamp incorrectly defined as uint8
            3, 1, 2
        )
        val direct = FitHistoricalObservations.decode(fit(
            incompatibleTimestampDefinition + byteArrayOf(1, 10, 72)
        ))!!
        assertEquals(FitHistoricalObservations.Quality.MISSING_TIMESTAMP,
            direct.observations.single().quality)
        assertTrue(direct.unknownFields.any {
            it.fieldNumber == 253 &&
                it.kind == FitHistoricalObservations.UnknownKind.INCOMPATIBLE_KNOWN_FIELD
        })

        val validDefinition = byteArrayOf(
            0x40, 0, 0, 20, 0, 2,
            253.toByte(), 4, 0x86.toByte(),
            3, 1, 2
        )
        val data = validDefinition + byteArrayOf(0) + u32(200_000) + byteArrayOf(60) +
            incompatibleTimestampDefinition + byteArrayOf(1, 10, 72) +
            byteArrayOf(0xA1.toByte(), 73) // compressed local type 1; must not trust bad timestamp definition
        assertNull(FitHistoricalObservations.decode(fit(data)))
    }

    @Test fun scalesDistanceAndSpeedAndRetainsPowerInProfileUnits() {
        val data = byteArrayOf(
            0x40, 0, 0, 20, 0, 4,
            253.toByte(), 4, 0x86.toByte(),
            5, 4, 0x86.toByte(),
            6, 2, 0x84.toByte(),
            7, 2, 0x84.toByte(),
            0
        ) + u32(0x10000000 + 210_000) + u32(12_345) + byteArrayOf(0x29, 0x09, 0x41, 0x01)
        val observations = FitHistoricalObservations.decode(fit(data))!!.observations
        assertEquals(123.45, observations.single {
            it.metric == FitHistoricalObservations.Metric.DISTANCE
        }.value, 0.0001)
        assertEquals(2.345, observations.single {
            it.metric == FitHistoricalObservations.Metric.SPEED
        }.value, 0.0001)
        assertEquals(321.0, observations.single {
            it.metric == FitHistoricalObservations.Metric.POWER
        }.value, 0.0)
        assertTrue(observations.all {
            it.timestampEpochMillis == (0x10000000L + 210_000L + 631_065_600L) * 1_000L
        })
    }

    @Test fun recordsMissingTimestampAndOutOfDisplayRangeHeartRateExplicitly() {
        val missingTimestamp = byteArrayOf(
            0x40, 0, 0, 20, 0, 1,
            3, 1, 2,
            0, 72
        )
        val missing = FitHistoricalObservations.decode(fit(missingTimestamp))!!.observations.single()
        assertNull(missing.timestampEpochMillis)
        assertEquals(FitHistoricalObservations.Quality.MISSING_TIMESTAMP, missing.quality)

        val outOfRangeData = byteArrayOf(
            0x40, 0, 0, 20, 0, 2,
            253.toByte(), 4, 0x86.toByte(),
            3, 1, 2,
            0
        ) + u32(220_000) + byteArrayOf(250.toByte())
        val outOfRange = FitHistoricalObservations.decode(fit(outOfRangeData))!!.observations.single()
        assertEquals(FitHistoricalObservations.Quality.OUTSIDE_DISPLAY_RANGE, outOfRange.quality)
    }

    @Test fun developerFieldsAreSkippedUsingTheirDeclaredLengths() {
        val data = byteArrayOf(
            0x60, 0, 0, 20, 0, 2,
            253.toByte(), 4, 0x86.toByte(),
            3, 1, 2,
            1, 0, 2, 0, // one developer field: size 2
            0
        ) + u32(250_000) + byteArrayOf(66, 0x55, 0x66)
        val result = FitHistoricalObservations.decode(fit(data))!!
        assertEquals(1, result.observations.size)
        assertEquals(66.0, result.observations.single().value, 0.0)
    }

    @Test fun bigEndianDefinitionUsesBigEndianForMultibyteValues() {
        val data = byteArrayOf(
            0x40, 0, 1, 0, 20, 2, // architecture 1, global message 20
            253.toByte(), 4, 0x86.toByte(),
            3, 1, 2,
            0,
            0x10, 0x01.toByte(), 0x86.toByte(), 0xa0.toByte(), 75 // absolute timestamp, HR 75
        )
        val result = FitHistoricalObservations.decode(fit(data))!!
        assertEquals(75.0, result.observations.single().value, 0.0)
        assertEquals((0x10000000L + 100_000 + 631_065_600L) * 1_000L,
            result.observations.single().timestampEpochMillis)
    }

    @Test fun invalidSentinelsAreSkippedAndReimportsAreIdempotent() {
        val data = byteArrayOf(
            0x40, 0, 0, 20, 0, 2,
            253.toByte(), 4, 0x86.toByte(),
            3, 1, 2,
            0
        ) + u32(300_000) + byteArrayOf(0xff.toByte())
        val file = fit(data)
        val first = FitHistoricalObservations.decode(file)!!
        val again = FitHistoricalObservations.decode(file)!!
        assertTrue(first.observations.isEmpty())
        assertEquals(1, first.invalidMeasurementFieldCount)
        assertEquals(
            listOf(FitHistoricalObservations.ExcludedField(
                globalMessageNumber = 20,
                fieldNumber = 3,
                metric = FitHistoricalObservations.Metric.HEART_RATE,
                reviewFieldName = null,
                reason = FitHistoricalObservations.ExclusionReason.INVALID_SENTINEL,
                occurrenceCount = 1
            )),
            first.excludedFields
        )
        assertEquals(first, again)
    }

    @Test fun badCrcAndMalformedRecordAreRejected() {
        val valid = fit(byteArrayOf(0x40, 0, 0, 20, 0, 0))
        assertNotNull(FitHistoricalObservations.decode(valid))
        valid[14] = 0x41
        assertNull(FitHistoricalObservations.decode(valid))
        val badCrc = fit(byteArrayOf(0x40, 0, 0, 20, 0, 0))
        badCrc[14] = 1
        assertNull(FitHistoricalObservations.decode(badCrc))
    }

    @Test fun identicalObservationsAcrossFilesAreFlaggedNotDiscarded() {
        val data = byteArrayOf(
            0x40, 0, 0, 20, 0, 2,
            253.toByte(), 4, 0x86.toByte(),
            3, 1, 2,
            0
        ) + u32(400_000) + byteArrayOf(80)
        val a = FitHistoricalObservations.decode(fit(data))!!
        val b = FitHistoricalObservations.decode(fit(data))!!
        assertEquals(a.observations.single().idempotencyKey, b.observations.single().idempotencyKey)
        assertTrue(a.potentialDuplicates().isEmpty())
        val identical = (a.observations + b.observations)
            .groupBy { it.duplicateFingerprint }.values.single()
        assertEquals(2, identical.size)
    }
    @Test fun unknownStandardFieldCountsDefinitionsAndActualRecordsSeparately() {
        val data = byteArrayOf(
            0x40, 0, 0, 20, 0, 3,
            253.toByte(), 4, 0x86.toByte(),
            3, 1, 2,
            85.toByte(), 2, 0x84.toByte(),
            0
        ) + u32(0x10000000 + 100_000) + byteArrayOf(72) + u16(123) +
            byteArrayOf(0x80.toByte()) + u32(0x10000000 + 100_000) + byteArrayOf(73) + u16(124)
        val result = FitHistoricalObservations.decode(fit(data))!!
        val unknown = result.unknownFields.single()
        assertEquals(FitHistoricalObservations.UnknownKind.UNKNOWN_STANDARD_FIELD, unknown.kind)
        assertEquals(85, unknown.fieldNumber)
        assertEquals(1, unknown.definitionCount)
        assertEquals(2, unknown.occurrenceCount)
        assertEquals(2, result.observations.size)
        assertEquals(result, FitHistoricalObservations.decode(fit(data)))
    }

    @Test fun rejectsFileWhenUnknownFieldDiagnosticMetadataExceedsBound() {
        val data = java.io.ByteArrayOutputStream().also { output ->
            repeat(4_097) { index ->
                val global = 1_000 + index
                output.write(byteArrayOf(
                    0x40, 0, 0, // definition, local message 0, reserved, little endian
                    global.toByte(), (global ushr 8).toByte(),
                    1, // one standard field
                    100, 1, 2, // unknown uint8 field
                    0, 1 // one data message to record the field occurrence
                ))
            }
        }.toByteArray()

        // Refuse the whole file rather than importing partial data after
        // dropping the bounded unknown-field diagnostic catalog.
        assertNull(FitHistoricalObservations.decode(fit(data)))
    }

    @Test fun developerFieldAndUnknownGlobalMessageAreNotGuessedAsMeasurements() {
        val data = byteArrayOf(
            0x60, 0, 0, 99, 0, 1,
            9, 1, 2,
            1, 7, 2, 4,
            0, 55, 66, 77
        )
        val result = FitHistoricalObservations.decode(fit(data))!!
        assertTrue(result.observations.isEmpty())
        assertEquals(2, result.unknownFields.size)
        assertTrue(result.unknownFields.any {
            it.kind == FitHistoricalObservations.UnknownKind.UNKNOWN_MESSAGE &&
                it.fieldNumber == 9 && it.occurrenceCount == 1
        })
        assertTrue(result.unknownFields.any {
            it.kind == FitHistoricalObservations.UnknownKind.DEVELOPER_FIELD &&
                it.fieldNumber == 7 && it.developerDataIndex == 4 &&
                it.size == 2 && it.occurrenceCount == 1
        })
    }

    @Test fun incompatibleKnownFieldIsInventoriedWithoutImportingIt() {
        val data = byteArrayOf(
            0x40, 0, 0, 20, 0, 1,
            3, 2, 0x84.toByte(),
            0
        ) + u16(72)
        val result = FitHistoricalObservations.decode(fit(data))!!
        assertTrue(result.observations.isEmpty())
        assertEquals(1, result.unsupportedMeasurementFieldCount)
        assertEquals(FitHistoricalObservations.UnknownKind.INCOMPATIBLE_KNOWN_FIELD,
            result.unknownFields.single().kind)
        assertEquals(
            FitHistoricalObservations.ExclusionReason.UNSUPPORTED_ENCODING,
            result.excludedFields.single().reason
        )
        assertEquals(FitHistoricalObservations.Metric.HEART_RATE, result.excludedFields.single().metric)
    }

    @Test fun crcAndTruncationDoNotYieldAnInventory() {
        val data = byteArrayOf(0x40, 0, 0, 20, 0, 1, 85.toByte(), 2, 0x84.toByte(), 0, 42)
        val valid = fit(data)
        assertNull(FitHistoricalObservations.decode(valid))
        val corrupted = fit(data + byteArrayOf(11))
        corrupted[corrupted.size - 1] = (corrupted.last().toInt() xor 1).toByte()
        assertNull(FitHistoricalObservations.decode(corrupted))
    }


    /** Audit #6 A6-30: documents the current cap; a file above it is rejected as a whole (null), not truncated. */
    private fun heartRateFile(records: Int): ByteArray {
        val definition = byteArrayOf(0x40, 0, 0, 20, 0, 1, 3, 1, 2) // record, one uint8 heart_rate field
        val data = ByteArray(definition.size + records * 2)
        definition.copyInto(data)
        for (i in 0 until records) { data[definition.size + i * 2] = 0; data[definition.size + i * 2 + 1] = 60 }
        val file = ByteArray(14 + data.size + 2)
        file[0] = 14; file[1] = 0x20; file[2] = 0x64
        u32(data.size).copyInto(file, 4)
        ".FIT".toByteArray().copyInto(file, 8)
        val headerCrc = FitFileHeader.crc16(file, 0, 12)
        file[12] = headerCrc.toByte(); file[13] = (headerCrc ushr 8).toByte()
        data.copyInto(file, 14)
        val crc = FitFileHeader.crc16(file, 0, file.size - 2)
        file[file.size - 2] = crc.toByte(); file[file.size - 1] = (crc ushr 8).toByte()
        return file
    }

    @Test fun observationCapAcceptsExactlyTheLimitAndRejectsTheWholeFileAboveIt() {
        val atLimit = FitHistoricalObservations.decode(heartRateFile(FitHistoricalObservations.MAX_OBSERVATIONS))
        assertEquals(FitHistoricalObservations.MAX_OBSERVATIONS, atLimit!!.observations.size)
        assertNull(FitHistoricalObservations.decode(heartRateFile(FitHistoricalObservations.MAX_OBSERVATIONS + 1)))
    }
}
