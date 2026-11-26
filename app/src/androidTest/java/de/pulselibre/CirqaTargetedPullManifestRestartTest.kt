package de.pulselibre

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Generated-only persistence regression; emulator/CI, never a physical phone. */
@RunWith(AndroidJUnit4::class)
class CirqaTargetedPullManifestRestartTest {
    @Test fun persistedImportAndStableMetadataStillCauseFullPullAfterManifestRecreation() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val preferencesName = "${CirqaTargetedPullManifestPreferences.PREFERENCES_NAME}-issue599-test"
        val context = object : ContextWrapper(base) {
            override fun getSharedPreferences(name: String, mode: Int) =
                base.getSharedPreferences("$preferencesName-$name", mode)
        }
        val prefs = context.getSharedPreferences(CirqaTargetedPullManifestPreferences.PREFERENCES_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        val item = CirqaFileAccessItemListParser.Item(
            uid = byteArrayOf(0x11, 0x22, 0x33),
            dataType = byteArrayOf(0x44),
            dataSize = 128L,
            flags = 0L
        )
        try {
            val beforeRestart = CirqaTargetedPullManifest(CirqaTargetedPullManifestPreferences(context))
            assertTrue(beforeRestart.recordCompletedImport(item, "a".repeat(64)))
            listOf("session-1", "session-2", "session-3").forEach {
                assertTrue(beforeRestart.observe(item, it))
            }

            // Recreate both the preferences adapter and manifest as after a
            // process restart; commit() must make prior state readable again.
            val afterRestart = CirqaTargetedPullManifest(CirqaTargetedPullManifestPreferences(context))
            val identity = afterRestart.identity(item)
            assertNotNull(identity)
            identity!!
            val completed = CirqaTargetedPullPolicy.CompletedImport(
                identity = identity.policyIdentity,
                contentSha256 = afterRestart.completedImportSha256(item) ?: error("import hash not persisted"),
                importSucceeded = true,
                uidStabilityEvidence = afterRestart.stabilityEvidence(item)
            )
            assertEquals(3, completed.uidStabilityEvidence?.matchingSuccessfulSessions)
            assertTrue(CirqaTargetedPullPolicy.shouldPull(identity.policyIdentity, listOf(completed)))
        } finally {
            prefs.edit().clear().commit()
        }
    }
}
