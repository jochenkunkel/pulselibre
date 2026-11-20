package de.pulselibre

import java.io.File
import java.io.OutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Explicit, user-started export of the CRC-verified FIT originals kept in the app's private folder (#328, #204).
 * Only regular files named `<64 hex>.fit` are included (no links, no other names); nothing is interpreted, changed or
 * deleted. The archive holds raw device data and is unencrypted; the caller shows a warning first.
 */
internal object FitOriginalsArchive {
    private val NAME = Regex("[0-9a-f]{64}\\.fit")
    const val MAX_FILES = 2_000
    const val MAX_TOTAL_BYTES = 64L * 1024 * 1024

    data class Result(val files: Int, val bytes: Long, val skipped: Int)

    /** Names of the files that would be exported, sorted; used for counts and tests. */
    fun eligible(root: File): List<File> =
        root.listFiles().orEmpty()
            .filter { NAME.matches(it.name) && it.isFile && !Files.isSymbolicLink(it.toPath()) }
            .sortedBy { it.name }

    fun write(root: File, output: OutputStream): Result {
        var files = 0
        var bytes = 0L
        var skipped = 0
        ZipOutputStream(output.buffered()).use { zip ->
            for (file in eligible(root)) {
                if (files >= MAX_FILES || bytes + file.length() > MAX_TOTAL_BYTES) { skipped++; continue }
                zip.putNextEntry(ZipEntry(file.name))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
                files++; bytes += file.length()
            }
        }
        return Result(files, bytes, skipped)
    }
}
