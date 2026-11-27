package de.pulselibre

import java.nio.file.Files
import java.nio.file.LinkOption
import java.security.MessageDigest

/**
 * Skip a listed file only with proven identity (owner rule 24.11.2026, #148/#599).
 *
 * Evidence (docs/protocol/research/ISSUE148_FILE_IDENTITY_EVIDENCE_2026-11-27.md): 15 private HCI captures from
 * 21.–25.11.2026, 96 files with fully reconstructed content; 70 UIDs seen on two or more days. Every time UID,
 * type, size and the band's per-file transaction ID (ItemReference field 5) matched, the content was byte-identical
 * (0 counterexamples), and no UID ever changed content. Without a transaction ID there is no such evidence.
 *
 * A file is therefore skipped only when all of these hold; otherwise it is pulled again (never lose data):
 * the band reported a transaction ID; an earlier run of THIS identity (incl. transaction) finished a successful
 * SQLite import with a valid content SHA-256; and the verified local original with that SHA still exists.
 */
object CirqaTargetedPullPolicy {
    data class ItemIdentity(
        val uidKey: String,
        val dataTypeKey: String,
        val dataSize: Long,
        val flags: Long,
        /** Band-side per-file transaction ID (ItemReference field 5); part of the proven identity. */
        val transactionId: Long? = null
    )

    data class CompletedImport(
        val identity: ItemIdentity,
        val contentSha256: String,
        val importSucceeded: Boolean,
        val uidStabilityEvidence: CirqaUidStabilityEvidence?,
        /** The verified original with [contentSha256] is still stored locally (checked by the caller). */
        val localCopyVerified: Boolean = false
    )

    private val SHA256 = Regex("[0-9a-f]{64}")

    fun shouldPull(
        identity: ItemIdentity,
        completedImports: Collection<CompletedImport>
    ): Boolean {
        if (identity.uidKey.isEmpty() || identity.dataTypeKey.isEmpty() || identity.dataSize <= 0L) return true
        if (identity.transactionId == null) return true
        return completedImports.none {
            it.identity == identity && it.importSucceeded && it.contentSha256.matches(SHA256) && it.localCopyVerified
        }
    }

    /**
     * A content-addressed filename and matching length do not prove the bytes
     * stayed intact after the original import. Fail closed unless the regular
     * file itself still hashes to the manifest SHA.
     */
    fun localCopyMatches(file: java.io.File, expectedSize: Long, expectedSha256: String): Boolean {
        if (expectedSize !in 1..CirqaVerifiedRawFileStore.MAX_FILE_BYTES ||
            !expectedSha256.matches(SHA256)
        ) return false
        return try {
            val path = file.toPath()
            if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) ||
                Files.isSymbolicLink(path) || file.length() != expectedSize
            ) return false
            val digest = MessageDigest.getInstance("SHA-256")
            var bytesRead = 0L
            Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS).use { input ->
                val buffer = ByteArray(8 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    bytesRead += count
                    if (bytesRead > expectedSize) return false
                    digest.update(buffer, 0, count)
                }
            }
            bytesRead == expectedSize && digest.digest().lowerHex() == expectedSha256
        } catch (_: Exception) {
            false
        }
    }

    fun stabilityKey(identity: ItemIdentity): String =
        MessageDigest.getInstance("SHA-256")
            .digest(
                ("${identity.uidKey}:${identity.dataTypeKey}:${identity.dataSize}:${identity.flags}" +
                    (identity.transactionId?.let { ":tr=$it" } ?: ""))
                    .toByteArray()
            )
            .lowerHex()
}
