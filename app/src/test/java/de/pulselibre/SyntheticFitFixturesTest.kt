package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** Regression suite for the published synthetic FIT fixtures. Pure JVM, no device. */
class SyntheticFitFixturesTest {
    private class RecordingStore : FitObservationStore {
        var commits = 0
        var sourceSha256 = ""
        var fileBytes = 0L
        var committedObservations = 0
        var decoderVersion = ""
        var profileVersion = ""
        val idempotencyKeys = mutableListOf<String>()

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
            deviceKey: String
        ): Int {
            commits++
            this.sourceSha256 = sourceSha256
            this.fileBytes = fileBytes
            this.decoderVersion = decoderVersion
            this.profileVersion = profileVersion
            committedObservations += observations.size
            idempotencyKeys += observations.map { it.idempotencyKey }
            return observations.size
        }
    }

    @Test fun fixturesHavePinnedOriginLengthAndSha256() {
        assertEquals(3, SyntheticFitFixtures.all.size)
        SyntheticFitFixtures.all.forEach { fixture ->
            assertTrue(SyntheticFitFixtures.SYNTHETIC)
            assertEquals(SyntheticFitFixtures.ORIGIN, "generated-fixture")
            assertNotNull(SyntheticFitFixtures.PROVENANCE_NOTE)
            assertNotNull(SyntheticFitFixtures.VALIDATION_NOTE)
            assertEquals(14 + fixture.expectedDataSize + 2, fixture.bytes.size)
            assertEquals(fixture.expectedSha256, SyntheticFitFixtures.sha256(fixture.bytes))
            assertEquals(64, fixture.expectedSha256.length)
        }
    }

    @Test fun envelopeAndPayloadAreExactlyTheDeclaredSyntheticBytes() {
        SyntheticFitFixtures.all.forEach { fixture ->
            val header = FitFileHeader.inspect(fixture.bytes)
            assertNotNull("${fixture.id}: envelope", header)
            assertEquals(14, header!!.headerSize)
            assertEquals(SyntheticFitFixtures.PROTOCOL_VERSION, header.protocolVersion)
            assertEquals(SyntheticFitFixtures.PROFILE_VERSION, header.profileVersion)
            assertEquals(fixture.expectedDataSize.toLong(), header.dataSize)
            assertEquals(
                fixture.expectedPayload.map { it.toByte() },
                fixture.bytes.copyOfRange(14, fixture.bytes.size - 2).toList()
            )
            assertEquals(0, FitFileHeader.crc16(fixture.bytes, 0, 14))
            val fullCrc = FitFileHeader.crc16(fixture.bytes)
            if (fixture.expectedVerified) assertEquals(0, fullCrc)
            else assertTrue("${fixture.id}: file CRC must be corrupt", fullCrc != 0)
        }
    }

    @Test fun decoderExpectationsMatchDeclaredFixtures() {
        SyntheticFitFixtures.all.forEach { fixture ->
            val decoded = FitHistoricalObservations.decode(fixture.bytes)
            if (fixture.expectedDecodedObservations == null) {
                assertNull("${fixture.id}: decoder must reject corrupt fixture", decoded)
            } else {
                assertNotNull("${fixture.id}: decoder", decoded)
                assertEquals(
                    fixture.expectedDecodedObservations,
                    decoded!!.observations.size
                )
                decoded.observations.forEach { observation ->
                    assertEquals(FitHistoricalObservations.Metric.HEART_RATE, observation.metric)
                    assertEquals(
                        SyntheticFitFixtures.PLACEHOLDER_HEART_RATE_BPM.toDouble(),
                        observation.value,
                        0.0
                    )
                    assertEquals("bpm", observation.unit)
                    assertEquals(fixture.expectedSha256, observation.sourceFileSha256)
                }
            }
        }
    }

    @Test fun tamperedFileCrcIsRejectedByVerifiedInspection() {
        val tampered = SyntheticFitFixtures.fileCrcTampered
        assertNotNull(FitFileHeader.inspect(tampered.bytes))
        assertNull(FitFileHeader.inspectVerified(tampered.bytes))
        assertFalse(tampered.expectedVerified)
        assertEquals(
            SyntheticFitFixtures.heartRatePlaceholder.bytes.copyOfRange(0, tampered.bytes.size - 1).toList(),
            tampered.bytes.copyOfRange(0, tampered.bytes.size - 1).toList()
        )
    }

    @Test fun publishedBytesContainNoDeviceOrSecretMarkers() {
        val markers = listOf(
            "CIRQA", "GARMIN", "serial", "token", "secret", "pairing", "uuid",
            "device", "http", "https", "/", "key", "password"
        )
        SyntheticFitFixtures.all.forEach { fixture ->
            val ascii = String(fixture.bytes, Charsets.US_ASCII)
            markers.forEach { marker ->
                assertFalse("${fixture.id} contains marker $marker", ascii.contains(marker, ignoreCase = true))
            }
        }
    }

    @Test fun backfillImportsSyntheticFixtureWithPinnedProvenance() {
        val root = Files.createTempDirectory("synthetic-fit-fixture-test").toFile()
        try {
            val fixture = SyntheticFitFixtures.heartRatePlaceholder
            File(root, CirqaFitBackfill.expectedFileName(fixture.bytes)).writeBytes(fixture.bytes)
            val store = RecordingStore()

            val result = CirqaFitBackfill(root, store).run()

            assertEquals(1, result.filesSeen)
            assertEquals(1, result.filesImported)
            assertEquals(0, result.invalidFiles)
            assertEquals(1, result.importableObservations)
            assertEquals(1, result.committedObservations)
            assertEquals(1, store.commits)
            assertEquals(fixture.expectedSha256, store.sourceSha256)
            assertEquals(fixture.bytes.size.toLong(), store.fileBytes)
            assertEquals(FitHistoricalObservations.DECODER_VERSION, store.decoderVersion)
            assertEquals(FitHistoricalObservations.PROFILE_VERSION, store.profileVersion)
        } finally {
            root.listFiles()?.forEach { it.delete() }
            root.delete()
        }
    }

    @Test fun replayOfPublishedFixtureKeepsIdempotencyKeysStable() {
        val root = Files.createTempDirectory("synthetic-fit-fixture-replay").toFile()
        try {
            val fixture = SyntheticFitFixtures.heartRatePlaceholder
            File(root, CirqaFitBackfill.expectedFileName(fixture.bytes)).writeBytes(fixture.bytes)
            val firstStore = RecordingStore()
            val secondStore = RecordingStore()
            val backfill = CirqaFitBackfill(root, firstStore)

            val first = backfill.run()
            val second = CirqaFitBackfill(root, secondStore).run()

            assertEquals(1, first.importableObservations)
            assertEquals(1, second.importableObservations)
            val key = firstStore.idempotencyKeys.single()
            assertEquals(64, key.length)
            assertTrue(key.all { it in '0'..'9' || it in 'a'..'f' })
            assertEquals(firstStore.idempotencyKeys, secondStore.idempotencyKeys)
        } finally {
            root.listFiles()?.forEach { it.delete() }
            root.delete()
        }
    }

    @Test fun emptyAndTamperedFixturesProduceNoCommits() {
        listOf(
            SyntheticFitFixtures.emptyPayload to CirqaFitBackfill.FileStatus.NO_IMPORTABLE_OBSERVATIONS,
            SyntheticFitFixtures.fileCrcTampered to CirqaFitBackfill.FileStatus.INVALID_FILE
        ).forEach { (fixture, expectedStatus) ->
            val root = Files.createTempDirectory("synthetic-fit-fixture-invalid").toFile()
            try {
                File(root, CirqaFitBackfill.expectedFileName(fixture.bytes)).writeBytes(fixture.bytes)
                val store = RecordingStore()

                val result = CirqaFitBackfill(root, store).run()

                assertEquals("${fixture.id}: files seen", 1, result.filesSeen)
                assertEquals("${fixture.id}: committed observations", 0, result.committedObservations)
                assertEquals("${fixture.id}: file status", expectedStatus, result.fileResults.single().status)
                if (expectedStatus == CirqaFitBackfill.FileStatus.INVALID_FILE) {
                    assertEquals("${fixture.id}: invalid files", 1, result.invalidFiles)
                    assertEquals("${fixture.id}: imported files", 0, result.filesImported)
                    assertEquals("${fixture.id}: commits", 0, store.commits)
                } else {
                    assertEquals("${fixture.id}: imported files", 1, result.filesImported)
                    assertEquals("${fixture.id}: importable observations", 0, result.importableObservations)
                    // A valid empty envelope still records provenance, but no observation.
                    assertEquals("${fixture.id}: provenance commits", 1, store.commits)
                    assertEquals(fixture.expectedSha256, store.sourceSha256)
                }
            } finally {
                root.listFiles()?.forEach { it.delete() }
                root.delete()
            }
        }
    }
}
