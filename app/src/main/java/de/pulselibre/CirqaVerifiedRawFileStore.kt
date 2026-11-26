package de.pulselibre

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/** Exact-size, content-addressed private storage for fully received non-FIT files. */
class CirqaVerifiedRawFileStore(private val root: File) {
    data class Saved(val file: File, val sha256: String, val alreadyPresent: Boolean)

    companion object {
        const val MAX_FILE_BYTES = 8L * 1024 * 1024
        private val locks = ConcurrentHashMap<String, Any>()
    }

    fun saveExact(bytes: ByteArray, expectedSize: Long): Saved {
        require(expectedSize in 1..MAX_FILE_BYTES && bytes.size.toLong() == expectedSize) {
            "Raw file size mismatch or limit exceeded"
        }
        require(root.isDirectory || root.mkdirs()) { "Cannot create private raw-file directory" }
        require(root.isDirectory && !Files.isSymbolicLink(root.toPath())) { "Unsafe raw-file directory" }
        val lock = locks.computeIfAbsent(root.canonicalPath) { Any() }
        return synchronized(lock) {
            val sha = MessageDigest.getInstance("SHA-256").digest(bytes)
                .lowerHex()
            val target = File(root, "$sha.bin")
            fun existing(): Saved? {
                if (!target.exists()) return null
                require(target.isFile && !Files.isSymbolicLink(target.toPath()) &&
                    target.length() == expectedSize && target.readBytes().contentEquals(bytes)) {
                    "Existing raw file failed verification"
                }
                return Saved(target, sha, true)
            }
            existing()?.let { return@synchronized it }
            val temp = File.createTempFile(".raw-", ".tmp", root)
            try {
                FileOutputStream(temp).use { stream ->
                    stream.write(bytes)
                    stream.fd.sync()
                }
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
                require(target.isFile && !Files.isSymbolicLink(target.toPath()) &&
                    target.length() == expectedSize && target.readBytes().contentEquals(bytes)) {
                    "Atomic raw-file publication failed verification"
                }
                Saved(target, sha, false)
            } finally {
                Files.deleteIfExists(temp.toPath())
            }
        }
    }
}
