package de.pulselibre

import java.security.MessageDigest

/**
 * Remote metadata is not a content revision. Even repeated UID/type/size/flags
 * plus a verified LOCAL import cannot prove that the BAND contents are unchanged.
 * Pull first; content-addressed storage/import deduplicates the received bytes.
 */
object CirqaTargetedPullPolicy {
    data class ItemIdentity(
        val uidKey: String,
        val dataTypeKey: String,
        val dataSize: Long,
        val flags: Long
    )

    data class CompletedImport(
        val identity: ItemIdentity,
        val contentSha256: String,
        val importSucceeded: Boolean,
        val uidStabilityEvidence: CirqaUidStabilityEvidence?
    )

    @Suppress("UNUSED_PARAMETER")
    fun shouldPull(
        identity: ItemIdentity,
        completedImports: Collection<CompletedImport>
    ): Boolean {
        return true
    }

    fun stabilityKey(identity: ItemIdentity): String =
        MessageDigest.getInstance("SHA-256")
            .digest(
                "${identity.uidKey}:${identity.dataTypeKey}:${identity.dataSize}:${identity.flags}"
                    .toByteArray()
            )
            .lowerHex()
}
