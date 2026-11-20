package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipInputStream

class FitOriginalsArchiveTest {
    private fun dir() = Files.createTempDirectory("fit-originals-test").toFile()

    @Test fun exportsOnlyRegularShaNamedFitFilesWithTheirBytes() {
        val root = dir()
        try {
            val a = "a".repeat(64) + ".fit"; val b = "b".repeat(64) + ".fit"
            File(root, a).writeBytes(byteArrayOf(1, 2, 3)); File(root, b).writeBytes(byteArrayOf(9))
            File(root, "notes.txt").writeText("x")                    // wrong name
            File(root, "A".repeat(64) + ".fit").writeBytes(byteArrayOf(5)) // uppercase hex is not a sha name here
            File(root, "c".repeat(64) + ".fit.tmp").writeBytes(byteArrayOf(5))
            File(root, "d".repeat(64) + ".fit").mkdir()               // directory, not a file
            val out = ByteArrayOutputStream()
            val result = FitOriginalsArchive.write(root, out)
            assertEquals(2, result.files); assertEquals(4L, result.bytes); assertEquals(0, result.skipped)
            val names = mutableMapOf<String, Int>()
            ZipInputStream(out.toByteArray().inputStream()).use { zip ->
                while (true) { val e = zip.nextEntry ?: break; names[e.name] = zip.readBytes().size }
            }
            assertEquals(mapOf(a to 3, b to 1), names)
        } finally { root.deleteRecursively() }
    }

    @Test fun skipsSymbolicLinks() {
        val root = dir()
        try {
            val target = File(root, "target.bin").also { it.writeBytes(byteArrayOf(7)) }
            Files.createSymbolicLink(File(root, "e".repeat(64) + ".fit").toPath(), target.toPath())
            assertTrue(FitOriginalsArchive.eligible(root).isEmpty())
        } finally { root.deleteRecursively() }
    }

    @Test fun missingFolderGivesAnEmptyArchiveNotAnError() {
        val result = FitOriginalsArchive.write(File("/nonexistent-fit-folder"), ByteArrayOutputStream())
        assertEquals(0, result.files)
    }
}
