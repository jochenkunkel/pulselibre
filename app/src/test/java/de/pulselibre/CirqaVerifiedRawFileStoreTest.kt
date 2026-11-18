package de.pulselibre

import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class CirqaVerifiedRawFileStoreTest {
    @Test fun savesOnlyExactSizeAndDeduplicates() {
        val root = Files.createTempDirectory("cirqa-raw-test").toFile()
        try {
            val store = CirqaVerifiedRawFileStore(root)
            val bytes = byteArrayOf(1, 2, 3, 4)
            val saved = store.saveExact(bytes, 4)
            assertFalse(saved.alreadyPresent)
            assertArrayEquals(bytes, saved.file.readBytes())
            assertTrue(store.saveExact(bytes, 4).alreadyPresent)
            assertEquals(1, root.listFiles()!!.size)
        } finally {
            root.listFiles()?.forEach { it.delete() }
            root.delete()
        }
    }

    @Test fun rejectsTruncatedOversizedAndInvalidExpectedSize() {
        val root = Files.createTempDirectory("cirqa-raw-test").toFile()
        try {
            val store = CirqaVerifiedRawFileStore(root)
            listOf(
                { store.saveExact(byteArrayOf(1, 2), 3) },
                { store.saveExact(byteArrayOf(1), CirqaVerifiedRawFileStore.MAX_FILE_BYTES + 1) },
                { store.saveExact(byteArrayOf(1), 0) }
            ).forEach { attempt ->
                try {
                    attempt()
                    fail("Expected size rejection")
                } catch (_: IllegalArgumentException) { }
            }
            assertTrue(root.listFiles()!!.isEmpty())
        } finally {
            root.listFiles()?.forEach { it.delete() }
            root.delete()
        }
    }
}
