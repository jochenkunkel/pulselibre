package de.pulselibre

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

    fun stabilityKey(identity: ItemIdentity): String =
        MessageDigest.getInstance("SHA-256")
            .digest(
                ("${identity.uidKey}:${identity.dataTypeKey}:${identity.dataSize}:${identity.flags}" +
                    (identity.transactionId?.let { ":tr=$it" } ?: ""))
                    .toByteArray()
            )
            .lowerHex()
}
