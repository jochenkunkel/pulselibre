package de.pulselibre

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Offline-only, content-addressed FIT persistence. Caller MUST pass an app-private
 * directory (e.g. File(context.filesDir, "verified_fit")). No BLE or device writes.
 * FIT decoding/import is performed separately by [CirqaFitBackfill].
 */
class CirqaVerifiedFitStore(private val root: File) {
    enum class Outcome { CREATED, ALREADY_PRESENT }
    data class Saved(val file: File, val sha256: String, val outcome: Outcome)

    companion object {
        private val publicationLocks = ConcurrentHashMap<String, Any>()
    }

    /**
     * Requires a completed, independently FIT-CRC-verified file. The content hash
     * is an idempotency key; filenames contain no device ID, timestamps or health data.
     * Publishes an fsynced same-directory temporary file with an atomic rename.
     * App-private storage plus a per-directory process lock prevents collisions
     * between concurrent writers; an existing content-addressed target is verified.
     */
    fun saveVerified(fileBytes: ByteArray): Saved {
        require(FitFileHeader.inspectVerified(fileBytes) != null) { "Invalid FIT envelope or CRC" }
        require(root.isDirectory || root.mkdirs()) { "Cannot create private FIT directory" }
        require(root.isDirectory && !Files.isSymbolicLink(root.toPath())) { "Unsafe FIT directory" }
        val lock = publicationLocks.computeIfAbsent(root.canonicalPath) { Any() }
        return synchronized(lock) {
            val sha = MessageDigest.getInstance("SHA-256").digest(fileBytes)
                .lowerHex()
            val target = File(root, "$sha.fit")
            fun existing(): Saved? {
                if (!target.exists()) return null
                require(target.isFile && !Files.isSymbolicLink(target.toPath())) { "Unsafe FIT target" }
                require(target.length() == fileBytes.size.toLong() &&
                    target.readBytes().contentEquals(fileBytes)) { "FIT hash collision or corrupt stored file" }
                return Saved(target, sha, Outcome.ALREADY_PRESENT)
            }
            existing()?.let { return@synchronized it }
            val temp = File.createTempFile(".fit-", ".tmp", root)
            try {
                FileOutputStream(temp).use { stream ->
                    stream.write(fileBytes)
                    stream.fd.sync()
                }
                // Atomic rename is supported on Android app-private filesystems;
                // the lock and content-addressed name avoid same-process replacement.
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
                require(target.isFile && !Files.isSymbolicLink(target.toPath()) &&
                    target.length() == fileBytes.size.toLong() &&
                    target.readBytes().contentEquals(fileBytes)) {
                    "Atomic FIT publication failed verification"
                }
                Saved(target, sha, Outcome.CREATED)
            } finally {
                Files.deleteIfExists(temp.toPath())
            }
        }
    }
}
