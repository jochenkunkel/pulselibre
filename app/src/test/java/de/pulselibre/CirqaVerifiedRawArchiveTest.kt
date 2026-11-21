package de.pulselibre

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.ZipInputStream

class CirqaVerifiedRawArchiveTest {
    private fun dir() = Files.createTempDirectory("cirqa-raw-archive").toFile()
    private fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    @Test fun exportsOnlyHashNamedVerifiedRawFilesAndPreservesBytes() {
        val root = dir()
        try {
            val contents = byteArrayOf(0, 1, 2, -1)
            val name = "${sha(contents)}.bin"
            File(root, name).writeBytes(contents)
            File(root, "notes.txt").writeBytes(contents)
            File(root, "${"a".repeat(64)}.bin").writeBytes(contents)

            val output = ByteArrayOutputStream()
            val result = CirqaVerifiedRawArchive.write(root, output)
            assertEquals(1, result.files)
            assertEquals(contents.size.toLong(), result.bytes)
            assertEquals(1, result.skipped)

            ZipInputStream(output.toByteArray().inputStream()).use { zip ->
                val entry = zip.nextEntry
                assertEquals(name, entry.name)
                assertArrayEquals(contents, zip.readBytes())
                assertEquals(null, zip.nextEntry)
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun rejectsSymlinksAndMissingFolder() {
        val root = dir()
        try {
            val contents = byteArrayOf(4, 5, 6)
            val target = File(root, "target").also { it.writeBytes(contents) }
            Files.createSymbolicLink(File(root, "${sha(contents)}.bin").toPath(), target.toPath())
            assertEquals(0, CirqaVerifiedRawArchive.eligible(root).size)
            assertEquals(0, CirqaVerifiedRawArchive.write(File("/missing-cirqa-raw"), ByteArrayOutputStream()).files)
        } finally {
            root.deleteRecursively()
        }
    }
}
