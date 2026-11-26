package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

/** Synthetic-only regression: unknown fields stay raw and profiled values remain review-only. */
class FitUnmappedProfileFieldsTest {
    @Test fun global397And398FieldsAreRetainedWithoutInventingObservations() {
        val data = globalMessage(397, listOf(
            Field(253, 4, 0x86), Field(1, 4, 0x88)
        ), listOf(u32(0x10000000) + byteArrayOf(0, 0, 0, 0))) +
            globalMessage(398, listOf(
                Field(253, 4, 0x86), Field(0, 4, 0x86), Field(1, 4, 0x88),
                Field(2, 4, 0x88), Field(3, 1, 0x02), Field(4, 4, 0x88)
            ), listOf(
                u32(0x10000001) + u32(0x10000001) +
                    byteArrayOf(0, 0, 0, 0) + byteArrayOf(0, 0, 0, 0) +
                    byteArrayOf(0) + byteArrayOf(0, 0, 0, 0)
            ))

        val result = FitHistoricalObservations.decode(fit(data))

        assertNotNull(result)
        result!!
        assertEquals(
            listOf(
                FitHistoricalObservations.Metric.SKIN_TEMPERATURE_DEVIATION,
                FitHistoricalObservations.Metric.SKIN_TEMPERATURE_7_DAY_DEVIATION
            ),
            result.observations.map { it.metric }
        )
        assertEquals(
            listOf("average_deviation", "average_7_day_deviation"),
            result.reviewCandidates.map { it.profileFieldName }
        )
        assertTrue(result.reviewCandidates.all {
            it.globalMessageNumber == 398 &&
                it.quality == "UNTRUSTED_PROFILE_CANDIDATE" &&
                it.decoderVersion == FitHistoricalObservations.REVIEW_CANDIDATE_DECODER_VERSION
        })
        assertEquals(2, result.decodedFields.count { it.globalMessageNumber == 397 })
        assertEquals(6, result.decodedFields.count { it.globalMessageNumber == 398 })
        assertTrue(result.unknownFields.any {
            it.globalMessageNumber == 397 && it.fieldNumber == 1
        })
        assertTrue(result.unknownFields.any {
            it.globalMessageNumber == 398 && it.fieldNumber == 3
        })
        assertFalse(result.unknownFields.any {
            it.globalMessageNumber == 398 && it.fieldNumber in setOf(0, 1, 2, 4)
        })
        assertTrue(result.decodedFields.all { it.rawBytes.size == it.fieldSize })
    }

    private data class Field(val number: Int, val size: Int, val baseType: Int)

    private fun globalMessage(global: Int, fields: List<Field>, records: List<ByteArray>): ByteArray {
        val local = if (global == 397) 0 else 1
        val output = ByteArrayOutputStream()
        output.write(byteArrayOf(
            (0x40 + local).toByte(), 0, 0,
            global.toByte(), (global ushr 8).toByte(), fields.size.toByte()
        ))
        fields.forEach { field ->
            output.write(byteArrayOf(field.number.toByte(), field.size.toByte(), field.baseType.toByte()))
        }
        records.forEach { record ->
            output.write(local)
            output.write(record)
        }
        return output.toByteArray()
    }

    private fun u32(value: Int) = byteArrayOf(
        value.toByte(), (value ushr 8).toByte(),
        (value ushr 16).toByte(), (value ushr 24).toByte()
    )

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
}
