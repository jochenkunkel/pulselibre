package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.zip.DeflaterOutputStream

/**
 * Offline proof of the complete local chain for a zlib-compressed FIT payload:
 * decode -> FIT envelope/CRC -> private content-addressed store -> decoder ->
 * backfill -> observation store. All bytes are synthetic; no device capture,
 * BLE transport or real health value is used. This test does NOT change or
 * claim a production wiring of [CirqaZlibFileDecoder].
 */
class CirqaCompressedFitImportChainTest {
    private class RecordingStore : FitObservationStore {
        var committed = 0
            private set

        override fun commitFitFile(
            sourceSha256: String,
            fileBytes: Long,
            relativePath: String,
            validatedAtEpochMillis: Long,
            decoderVersion: String,
            profileVersion: String,
            observations: List<FitHistoricalObservations.Observation>,
            deviceMetadata: List<FitHistoricalObservations.DeviceMetadata>,
            unmappedFields: List<FitFieldInventoryRecord>,
            deviceKey: String,
            reviewCandidates: List<FitHistoricalObservations.ReviewCandidate>,
            decodedFields: List<FitHistoricalObservations.DecodedField>
        ): Int {
            committed += observations.size
            return observations.size
        }
    }

    /** Synthetic definition + one record, with valid header and file CRC. */
    private fun syntheticFit(): ByteArray {
        val data = byteArrayOf(
            0x40, 0, 0, 20, 0, 2,
            253.toByte(), 4, 0x86.toByte(),
            3, 1, 2,
            0
        ) + byteArrayOf(0x10, 0x27, 0, 0x10, 72)
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

    private fun zlib(bytes: ByteArray): ByteArray = ByteArrayOutputStream().also { output ->
        DeflaterOutputStream(output).use { it.write(bytes) }
    }.toByteArray()

    @Test fun compressedFitBytesReachObservationStoreOffline() {
        val fit = syntheticFit()
        val decoded = CirqaZlibFileDecoder.decode(zlib(fit), fit.size)!!
        assertNotNull(FitFileHeader.inspectVerified(decoded))

        val root = Files.createTempDirectory("compressed-fit-chain-test").toFile()
        try {
            val saved = CirqaVerifiedFitStore(root).saveVerified(decoded)
            val store = RecordingStore()
            val result = CirqaFitBackfill(root, store).run()

            assertEquals(1, result.filesSeen)
            assertEquals(1, result.filesImported)
            assertTrue(store.committed > 0)
            assertEquals(CirqaFitBackfill.expectedFileName(decoded), saved.file.name)
        } finally {
            root.deleteRecursively()
        }
    }
}
