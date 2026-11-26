package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CirqaTargetedPullManifestTest {
    private class MemoryStorage : CirqaTargetedPullManifest.Storage {
        val values = mutableMapOf<String, String>()
        var failWrites = false
        override fun get(key: String): String? = values[key]
        override fun put(key: String, value: String): Boolean {
            if (failWrites) return false
            values[key] = value
            return true
        }
        override fun remove(key: String): Boolean = values.remove(key) != null
    }

    private val item = CirqaFileAccessItemListParser.Item(
        uid = byteArrayOf(1, 2, 3),
        dataType = byteArrayOf(4, 5),
        dataSize = 128L,
        flags = 0L
    )

    @Test fun onlyDistinctSessionsProduceStabilityEvidence() {
        val manifest = CirqaTargetedPullManifest(MemoryStorage())
        assertTrue(manifest.observe(item, "session-1"))
        assertTrue(manifest.observe(item, "session-1"))
        assertTrue(manifest.observe(item, "session-2"))
        assertNull(manifest.stabilityEvidence(item))
        assertTrue(manifest.observe(item, "session-3"))
        assertEquals(3, manifest.stabilityEvidence(item)?.matchingSuccessfulSessions)
    }

    @Test fun successfulImportRequiresValidContentHashAndExactIdentity() {
        val manifest = CirqaTargetedPullManifest(MemoryStorage())
        assertFalse(manifest.recordCompletedImport(item, "bad"))
        assertTrue(manifest.recordCompletedImport(item, "a".repeat(64)))
        assertEquals("a".repeat(64), manifest.completedImportSha256(item))
        assertNull(manifest.completedImportSha256(item.copy(dataSize = 129L)))
        assertTrue(manifest.forgetCompletedImport(item))
        assertNull(manifest.completedImportSha256(item))
    }

    @Test fun afterProcessRestartPreviouslyImportedRemoteItemIsPulledAgain() {
        val persistentStorage = MemoryStorage()
        val beforeRestart = CirqaTargetedPullManifest(persistentStorage)
        assertTrue(beforeRestart.recordCompletedImport(item, "a".repeat(64)))
        listOf("session-1", "session-2", "session-3").forEach {
            assertTrue(beforeRestart.observe(item, it))
        }

        // A new manifest instance represents a fresh process reading the same
        // durable preferences. Local import identity is not proof of remote
        // immutability, so safe continuation starts with another full pull.
        val afterRestart = CirqaTargetedPullManifest(persistentStorage)
        val identity = afterRestart.identity(item) ?: error("valid synthetic item must have identity")
        val completedImport = CirqaTargetedPullPolicy.CompletedImport(
            identity = identity.policyIdentity,
            contentSha256 = afterRestart.completedImportSha256(item) ?: error("stored import must survive"),
            importSucceeded = true,
            uidStabilityEvidence = afterRestart.stabilityEvidence(item)
        )
        assertEquals(3, completedImport.uidStabilityEvidence?.matchingSuccessfulSessions)
        assertTrue(
            CirqaTargetedPullPolicy.shouldPull(identity.policyIdentity, listOf(completedImport))
        )
    }

    @Test fun failedDurableObservationDoesNotCount() {
        val storage = MemoryStorage().apply { failWrites = true }
        val manifest = CirqaTargetedPullManifest(storage)
        assertFalse(manifest.observe(item, "session-1"))
        assertNull(manifest.stabilityEvidence(item))
    }

    @Test fun malformedIdentityCannotBeObservedOrImported() {
        val manifest = CirqaTargetedPullManifest(MemoryStorage())
        val invalid = item.copy(uid = byteArrayOf(), dataSize = 0)
        assertFalse(manifest.observe(invalid, "session-1"))
        assertFalse(manifest.recordCompletedImport(invalid, "a".repeat(64)))
    }
}
