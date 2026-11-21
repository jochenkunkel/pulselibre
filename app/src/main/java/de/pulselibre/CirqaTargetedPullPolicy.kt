package de.pulselibre

import java.security.MessageDigest

/**
 * Fail-safe selection for a future persisted remote-item manifest.
 *
 * A matching UID is not sufficient by itself: callers may skip only when the
 * UID is known to be stable across sync sessions and the corresponding import
 * completed successfully. Otherwise the item must be pulled.
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

    fun shouldPull(
        identity: ItemIdentity,
        completedImports: Collection<CompletedImport>
    ): Boolean {
        if (identity.uidKey.isBlank() || identity.dataTypeKey.isBlank() || identity.dataSize <= 0L) {
            return true
        }
        return completedImports.none { completed ->
            completed.identity == identity &&
                completed.contentSha256.matches(Regex("[0-9a-f]{64}")) &&
                completed.importSucceeded &&
                completed.uidStabilityEvidence?.let { evidence ->
                    evidence.matchingSuccessfulSessions >= CirqaUidStabilityEvidence.MIN_MATCHING_SESSIONS &&
                        evidence.identityFingerprint == stabilityKey(identity)
                } == true
        }
    }

    fun stabilityKey(identity: ItemIdentity): String =
        MessageDigest.getInstance("SHA-256")
            .digest(
                "${identity.uidKey}:${identity.dataTypeKey}:${identity.dataSize}:${identity.flags}"
                    .toByteArray()
            )
            .joinToString("") { "%02x".format(it.toInt() and 255) }
}
