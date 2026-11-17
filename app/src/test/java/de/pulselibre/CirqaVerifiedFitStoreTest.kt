package de.pulselibre

import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class CirqaVerifiedFitStoreTest {
    private val valid = byteArrayOf(
        0x0c, 0x20, 0x64, 0, 0, 0, 0, 0,
        0x2e, 0x46, 0x49, 0x54, 0x27, 0x29
    )

    @Test fun savesOnceAndDeduplicatesWithoutTemporaryFiles() {
        val root = Files.createTempDirectory("fit-store-test").toFile()
        try {
            val store = CirqaVerifiedFitStore(root)
            val first = store.saveVerified(valid)
            assertEquals(CirqaVerifiedFitStore.Outcome.CREATED, first.outcome)
            assertArrayEquals(valid, first.file.readBytes())
            assertEquals(64, first.sha256.length)
            val again = store.saveVerified(valid.copyOf())
            assertEquals(CirqaVerifiedFitStore.Outcome.ALREADY_PRESENT, again.outcome)
            assertEquals(first.file, again.file)
            assertEquals(listOf(first.file.name), root.listFiles()!!.map { it.name })
        } finally {
            root.listFiles()?.forEach { it.delete() }
            root.delete()
        }
    }

    @Test fun rejectsBadCrcWithoutWritingAnything() {
        val root = Files.createTempDirectory("fit-store-test").toFile()
        try {
            val corrupt = valid.copyOf()
            corrupt[13] = 0
            try {
                CirqaVerifiedFitStore(root).saveVerified(corrupt)
                fail("Expected rejection")
            } catch (_: IllegalArgumentException) { }
            assertTrue(root.listFiles()!!.isEmpty())
        } finally {
            root.listFiles()?.forEach { it.delete() }
            root.delete()
        }
    }

    @Test fun refusesCorruptExistingContent() {
        val root = Files.createTempDirectory("fit-store-test").toFile()
        try {
            val store = CirqaVerifiedFitStore(root)
            val saved = store.saveVerified(valid)
            saved.file.writeBytes(byteArrayOf(1))
            try {
                store.saveVerified(valid)
                fail("Expected corrupt-file rejection")
            } catch (_: IllegalArgumentException) { }
            assertArrayEquals(byteArrayOf(1), saved.file.readBytes())
        } finally {
            root.listFiles()?.forEach { it.delete() }
            root.delete()
        }
    }
}
