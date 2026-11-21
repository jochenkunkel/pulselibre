package de.pulselibre

import java.io.File
import java.io.OutputStream
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Bounded export of exact, content-addressed originals stored by
 * [CirqaVerifiedRawFileStore]. The archive contains opaque bytes only; it does
 * not decode, modify, or delete them. The caller must obtain explicit consent
 * and warn that the ZIP is unencrypted.
 */
internal object CirqaVerifiedRawArchive {
    private val NAME = Regex("[0-9a-f]{64}\\.bin")
    const val MAX_FILES = 2_000
    const val MAX_TOTAL_BYTES = 64L * 1024 * 1024

    data class Result(val files: Int, val bytes: Long, val skipped: Int)

    fun eligible(root: File): List<File> {
        if (!root.isDirectory || Files.isSymbolicLink(root.toPath())) return emptyList()
        return root.listFiles().orEmpty()
            .filter { NAME.matches(it.name) && it.isFile && !Files.isSymbolicLink(it.toPath()) }
            .sortedBy { it.name }
    }

    fun write(root: File, output: OutputStream): Result {
        var files = 0
        var bytes = 0L
        var skipped = 0
        ZipOutputStream(output.buffered()).use { zip ->
            for (file in eligible(root)) {
                val size = file.length()
                if (files >= MAX_FILES || size !in 1..CirqaVerifiedRawFileStore.MAX_FILE_BYTES ||
                    bytes + size > MAX_TOTAL_BYTES || !hasExpectedSha256(file)) {
                    skipped++
                    continue
                }
                zip.putNextEntry(ZipEntry(file.name))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
                files++
                bytes += size
            }
        }
        return Result(files, bytes, skipped)
    }

    private fun hasExpectedSha256(file: File): Boolean {
        val expected = file.name.removeSuffix(".bin")
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return actual == expected
    }
}
