package de.pulselibre

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Audit #6 / B7-2: manipulated archives are rejected (by the platform zip reader or by our whitelist), write nothing outside staging and leave no staging data. */
@RunWith(AndroidJUnit4::class)
class DatabaseBackupArchiveSafetyTest {
    private fun zip(vararg entries: Pair<String, String>): ByteArrayInputStream {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z ->
            entries.forEach { (name, body) -> z.putNextEntry(ZipEntry(name)); z.write(body.toByteArray()); z.closeEntry() }
        }
        return ByteArrayInputStream(out.toByteArray())
    }

    private fun withCache(block: (Context, File) -> Unit) {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val cache = Files.createTempDirectory(base.cacheDir.toPath(), "backup-safety-").toFile()
        val ctx = object : ContextWrapper(base) { override fun getCacheDir(): File = cache }
        try { block(ctx, cache) } finally { cache.deleteRecursively() }
    }

    private fun assertRejectedWithoutTrace(archive: ByteArrayInputStream) = withCache { ctx, cache ->
        // Android's ZipInputStream itself rejects '..' and absolute names with ZipException (IOException);
        // the app's own checks throw IllegalArgumentException. The UI catches any Exception.
        assertThrows(Exception::class.java) { DatabaseBackup.prepareRestore(ctx, archive) }
        assertEquals("no staging data may remain", emptyList<String>(), cache.list().orEmpty().toList())
        assertFalse(File(cache.parentFile, "evil.jsonl").exists())
    }

    @Test fun pathTraversalEntryIsRejected() =
        assertRejectedWithoutTrace(zip("../evil.jsonl" to "x"))

    @Test fun absolutePathEntryIsRejected() =
        assertRejectedWithoutTrace(zip("/data/evil.jsonl" to "x"))

    @Test fun unknownEntryNameIsRejected() =
        assertRejectedWithoutTrace(zip("manifest.json" to "{}", "extra.bin" to "x"))

    @Test fun archiveWithoutManifestIsRejected() =
        assertRejectedWithoutTrace(zip("cirqa_events.jsonl" to ""))
}
