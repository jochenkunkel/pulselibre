package de.pulselibre

import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CirqaFitOfflineInventoryTest {
    @Test
    fun inventoriesVerifiedFilesAndKeepsTheTextSummaryRedacted() {
        val root = Files.createTempDirectory("fit-offline-inventory-").toFile()
        try {
            val bytes = fitWithHeartRate(timestamp = 100_000, heartRate = 72)
            val privateName = "${CirqaFitBackfill.expectedFileName(bytes)}"
            File(root, privateName).writeBytes(bytes)
            File(root, "not-a-fit.bin").writeBytes(byteArrayOf(1, 2, 3))

            val summary = CirqaFitOfflineInventory.inspect(root)

            assertEquals(1, summary.filesSeen)
            assertEquals(1, summary.verifiedFiles)
            assertEquals(0, summary.rejectedFiles)
            assertEquals(1, summary.recordMessages)
            assertEquals(1, summary.supportedObservations)
            assertEquals(1, summary.importableObservations)
            assertEquals(0, summary.missingTimestamps)
            assertEquals(0, summary.outsideDisplayRange)
            assertEquals(1, summary.byMetric[FitHistoricalObservations.Metric.HEART_RATE])
            assertEquals(1, summary.perFile.size)
            assertEquals("FIT-0001", summary.perFile.single().alias)
            assertEquals("verifiziert", summary.perFile.single().status)
            assertEquals(1, summary.perFile.single().importableObservations)
            assertEquals(1, summary.perFile.single().byMetric[FitHistoricalObservations.Metric.HEART_RATE])
            assertTrue(summary.perFile.single().fieldCoverage.any {
                it == "g20.f3:HEART_RATE:obs1/imp1"
            })

            val report = summary.redactedText()
            assertTrue(report.contains("verifiziert: 1"))
            assertTrue(report.contains("HEART_RATE: 1"))
            assertTrue(report.contains("FIT-0001: verifiziert; Records=1; Observations=1; importierbar=1"))
            assertTrue(report.contains("g20.f3:HEART_RATE:obs1/imp1"))
            assertFalse(report.contains(privateName))
            assertFalse(report.contains(bytes.toString(Charsets.ISO_8859_1)))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectsBadEnvelopeAndSymlinkWithoutFollowingTheLink() {
        val root = Files.createTempDirectory("fit-offline-inventory-invalid-").toFile()
        try {
            val valid = fitWithHeartRate(timestamp = 100_000, heartRate = 72)
            val badCrc = valid.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 0x01).toByte() }
            File(root, CirqaFitBackfill.expectedFileName(badCrc)).writeBytes(badCrc)
            val linkedTarget = File(root, "target.bin").also { it.writeBytes(valid) }
            Files.createSymbolicLink(File(root, "linked.fit").toPath(), linkedTarget.toPath())

            val summary = CirqaFitOfflineInventory.inspect(root)

            assertEquals(2, summary.filesSeen)
            assertEquals(0, summary.verifiedFiles)
            assertEquals(2, summary.rejectedFiles)
            assertEquals(0, summary.importableObservations)
            assertEquals(0, summary.recordMessages)
            assertEquals(2, summary.perFile.size)
            assertTrue(summary.perFile.all { it.status == "abgewiesen" })
            assertEquals(listOf("FIT-0001", "FIT-0002"), summary.perFile.map { it.alias })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun capsPerFileReportAndMakesUnscannedFilesExplicit() {
        val root = Files.createTempDirectory("fit-offline-inventory-cap-").toFile()
        try {
            repeat(257) { index ->
                File(root, "%03d.fit".format(index)).writeBytes(byteArrayOf(index.toByte()))
            }

            val summary = CirqaFitOfflineInventory.inspect(root)

            assertEquals(257, summary.filesSeen)
            assertEquals(0, summary.verifiedFiles)
            assertEquals(256, summary.rejectedFiles)
            assertEquals(1, summary.filesNotScanned)
            assertEquals(256, summary.perFile.size)
            assertTrue(summary.redactedText().contains("Nicht geprüft (Sicherheitslimit): 1"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun reconcilesOnlyAggregatesAndNeverEmitsPrivateKeys() {
        val root = Files.createTempDirectory("fit-offline-reconcile-").toFile()
        try {
            val bytes = fitWithHeartRate(timestamp = 100_000, heartRate = 72)
            val sampleId = FitHistoricalObservations.decode(bytes)!!.observations.single().idempotencyKey
            val sourceHash = MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it) }
            File(root, CirqaFitBackfill.expectedFileName(bytes)).writeBytes(bytes)

            val matched = CirqaFitOfflineInventory.inspect(
                root, setOf(sampleId), setOf(sourceHash), databaseAvailable = true
            )
            assertEquals(1, matched.importableKeysInDatabase)
            assertEquals(0, matched.importableKeysMissingFromDatabase)
            assertEquals(1, matched.verifiedSourcesInDatabase)
            assertEquals(1, matched.perFile.single().databaseMatchedKeys)
            assertEquals(true, matched.perFile.single().databaseSourcePresent)
            val report = matched.redactedText()
            assertFalse(report.contains(sampleId))
            assertFalse(report.contains(sourceHash))
            assertFalse(report.contains(CirqaFitBackfill.expectedFileName(bytes)))

            val missing = CirqaFitOfflineInventory.inspect(root, databaseAvailable = true)
            assertEquals(0, missing.importableKeysInDatabase)
            assertEquals(1, missing.importableKeysMissingFromDatabase)
            assertEquals(0, missing.perFile.single().databaseMatchedKeys)
            assertEquals(false, missing.perFile.single().databaseSourcePresent)
            assertEquals(null, CirqaFitOfflineInventory.inspect(root).importableKeysMissingFromDatabase)
        } finally {
            root.deleteRecursively()
        }
    }

    private fun fitWithHeartRate(timestamp: Int, heartRate: Int): ByteArray {
        fun u32(value: Int) = byteArrayOf(
            value.toByte(), (value ushr 8).toByte(),
            (value ushr 16).toByte(), (value ushr 24).toByte()
        )

        val data = byteArrayOf(
            0x40, 0, 0, 20, 0, 2,
            253.toByte(), 4, 0x86.toByte(),
            3, 1, 2,
            0
        ) + u32(timestamp) + byteArrayOf(heartRate.toByte())
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
        file[file.lastIndex] = (crc ushr 8).toByte()
        return file
    }
}
