package de.pulselibre

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.security.MessageDigest

/**
 * Offline-only, content-addressed FIT persistence. Caller MUST pass an app-private
 * directory (e.g. File(context.filesDir, "verified_fit")). No BLE or device writes.
 * No completion cursor is advanced; files are never parsed into health readings.
 */
class CirqaVerifiedFitStore(private val root: File) {
    enum class Outcome { CREATED, ALREADY_PRESENT }
    data class Saved(val file: File, val sha256: String, val outcome: Outcome)

    /**
     * Requires a completed, independently FIT-CRC-verified file. The content hash
     * is an idempotency key; filenames contain no device ID, timestamps or health data.
     * Publishes an fsynced same-directory temporary inode via atomic no-replace
     * hard link. Fails closed when unsupported; never overwrites a racing target.
     */
    fun saveVerified(fileBytes: ByteArray): Saved {
        require(FitFileHeader.inspectVerified(fileBytes) != null) { "Invalid FIT envelope or CRC" }
        require(root.isDirectory || root.mkdirs()) { "Cannot create private FIT directory" }
        require(root.isDirectory && !Files.isSymbolicLink(root.toPath())) { "Unsafe FIT directory" }
        val sha = MessageDigest.getInstance("SHA-256").digest(fileBytes)
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        val target = File(root, "$sha.fit")
        fun existing(): Saved? {
            if (!target.exists()) return null
            require(target.isFile && !Files.isSymbolicLink(target.toPath())) { "Unsafe FIT target" }
            require(target.length() == fileBytes.size.toLong() &&
                target.readBytes().contentEquals(fileBytes)) { "FIT hash collision or corrupt stored file" }
            return Saved(target, sha, Outcome.ALREADY_PRESENT)
        }
        existing()?.let { return it }
        val temp = File.createTempFile(".fit-", ".tmp", root)
        try {
            FileOutputStream(temp).use { stream ->
                stream.write(fileBytes)
                stream.fd.sync()
            }
            // ATOMIC_MOVE may overwrite an existing target on Unix even without
            // REPLACE_EXISTING. A no-replace link closes that concurrent-writer race.
            try {
                Files.createLink(target.toPath(), temp.toPath())
            } catch (e: java.nio.file.FileAlreadyExistsException) {
                return existing() ?: throw e
            }
            return Saved(target, sha, Outcome.CREATED)
        } finally {
            Files.deleteIfExists(temp.toPath())
        }
    }
}
