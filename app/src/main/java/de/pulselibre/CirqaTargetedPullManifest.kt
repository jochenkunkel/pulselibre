package de.pulselibre

import java.security.MessageDigest

/**
 * App-private, redacted mapping between a repeated FileAccess identity and a
 * FIT import that completed in SQLite. The backing store must not be exported.
 */
class CirqaTargetedPullManifest(private val storage: Storage) {
    interface Storage {
        fun get(key: String): String?
        /** Return false when the value could not be durably committed. */
        fun put(key: String, value: String): Boolean
        fun remove(key: String): Boolean
    }

    data class Identity(
        val policyIdentity: CirqaTargetedPullPolicy.ItemIdentity,
        val key: String
    )

    fun identity(item: CirqaFileAccessItemListParser.Item): Identity? {
        if (item.uid.isEmpty() || item.dataType.isEmpty() || item.dataSize <= 0L) return null
        val uidKey = sha256(item.uid)
        val typeKey = sha256(item.dataType)
        val policyIdentity = CirqaTargetedPullPolicy.ItemIdentity(
            uidKey, typeKey, item.dataSize, item.flags
        )
        return Identity(policyIdentity, CirqaTargetedPullPolicy.stabilityKey(policyIdentity))
    }

    /** Records only successful, accepted-list sightings; each session counts once. */
    fun observe(item: CirqaFileAccessItemListParser.Item, sessionId: String): Boolean {
        if (!sessionId.matches(SESSION_ID)) return false
        val identity = identity(item) ?: return false
        val key = observationsKey(identity.key)
        val sessions = (storage.get(key).orEmpty().split(',')
            .filter { it.matches(SESSION_ID) } + sessionId).distinct().takeLast(MAX_SESSIONS)
        return storage.put(key, sessions.joinToString(","))
    }

    fun stabilityEvidence(item: CirqaFileAccessItemListParser.Item): CirqaUidStabilityEvidence? {
        val identity = identity(item) ?: return null
        val sessions = storage.get(observationsKey(identity.key)).orEmpty().split(',')
            .filter { it.matches(SESSION_ID) }.distinct()
        return CirqaUidStabilityEvidence.from(sessions.map { session ->
            CirqaUidStabilityEvidence.SessionObservation(
                sessionId = session,
                accepted = true,
                itemListReceived = true,
                identityFingerprint = identity.key
            )
        })
    }

    /** Call only after verified FIT storage and a successful SQLite import. */
    fun recordCompletedImport(item: CirqaFileAccessItemListParser.Item, contentSha256: String): Boolean {
        val identity = identity(item) ?: return false
        if (!contentSha256.matches(SHA256)) return false
        return storage.put(importKey(identity.key), contentSha256)
    }

    fun completedImportSha256(item: CirqaFileAccessItemListParser.Item): String? {
        val identity = identity(item) ?: return null
        return storage.get(importKey(identity.key))?.takeIf { it.matches(SHA256) }
    }

    fun forgetCompletedImport(item: CirqaFileAccessItemListParser.Item): Boolean {
        val identity = identity(item) ?: return false
        return storage.remove(importKey(identity.key))
    }

    companion object {
        const val MAX_SESSIONS = 3
        private val SESSION_ID = Regex("[A-Za-z0-9_-]{1,80}")
        private val SHA256 = Regex("[0-9a-f]{64}")

        private fun observationsKey(identityKey: String) = "observed:$identityKey"
        private fun importKey(identityKey: String) = "imported:$identityKey"

        private fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes)
                .lowerHex()
    }
}
