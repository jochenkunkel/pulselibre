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

    @Test fun decodesSupportedRecordMeasurementsAndCompressedTimestamp() {
        val timestamp = 100_000
        val data = byteArrayOf(
            0x40, 0, 0, 20, 0, 3,
            253.toByte(), 4, 0x86.toByte(), // timestamp uint32
            3, 1, 2,                         // heart_rate uint8
            4, 1, 2,                         // cadence uint8
            0,                               // normal data record
        ) + u32(timestamp) + byteArrayOf(72, 90) + byteArrayOf(
            0x80.toByte(),                   // compressed record, local type 0, timestamp delta 0
            73, 91
        )

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
        assertTrue(result.observations.all {
            it.quality == FitHistoricalObservations.Quality.PROFILE_VALID
        })
    }

    @Test fun monitoringCycles16IsExplicitlyRepresentedAsSteps() {
        val data = byteArrayOf(
            0x40, 0, 0, 55, 0, 2,
            253.toByte(), 4, 0x86.toByte(),
            9, 2, 0x84.toByte(),
            0
        ) + u32(200_000) + byteArrayOf(0x34, 0x12)
        val result = FitHistoricalObservations.decode(fit(data))!!
        val steps = result.observations.single()
        assertEquals(FitHistoricalObservations.Metric.CUMULATIVE_STEPS, steps.metric)
        assertEquals(0x1234 * 2.0, steps.value, 0.0)
        assertEquals("steps", steps.unit)
    }

    @Test fun scalesDistanceAndSpeedAndRetainsPowerInProfileUnits() {
        val data = byteArrayOf(
            0x40, 0, 0, 20, 0, 4,
            253.toByte(), 4, 0x86.toByte(),
            5, 4, 0x86.toByte(),
            6, 2, 0x84.toByte(),
            7, 2, 0x84.toByte(),
            0
        ) + u32(210_000) + u32(12_345) + byteArrayOf(0x29, 0x09, 0x41, 0x01)
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
            it.timestampEpochMillis == (210_000L + 631_065_600L) * 1_000L
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
            0, 0x01.toByte(), 0x86.toByte(), 0xa0.toByte(), 75 // timestamp 100000, HR 75
        )
        val result = FitHistoricalObservations.decode(fit(data))!!
        assertEquals(75.0, result.observations.single().value, 0.0)
        assertEquals((100_000 + 631_065_600L) * 1_000L,
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
}
