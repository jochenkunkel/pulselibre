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

    // The following v7 archives are fully synthetic: 15 empty JSONL files and a
    // manifest with correct counts/hashes. No device data, no real health values.
    private val v7Entries = listOf(
        "cirqa_events.jsonl",
        "legacy_readings.jsonl",
        "sync_cursors.jsonl",
        "fit_sources.jsonl",
        "fit_observation_provenance.jsonl",
        "canonical_observations.jsonl",
        "canonical_sync_cursors.jsonl",
        "canonical_fit_sources.jsonl",
        "canonical_fit_observation_provenance.jsonl",
        "canonical_fit_device_metadata.jsonl",
        "raw_device_metadata.jsonl",
        "canonical_fit_unmapped_fields.jsonl",
        "canonical_fit_decoder_generations.jsonl",
        "canonical_fit_generation_observations.jsonl",
        "canonical_fit_active_generations.jsonl"
    )

    private val emptySha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"

    private fun validV7Archive(omitEntry: String? = null, tamper: (org.json.JSONObject) -> Unit = {}): ByteArrayInputStream {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            v7Entries.filterNot { it == omitEntry }.forEach { name ->
                zip.putNextEntry(ZipEntry(name))
                zip.closeEntry()
            }
            val entries = org.json.JSONObject()
            v7Entries.forEach { name ->
                entries.put(name, org.json.JSONObject()
                    .put("records", 0)
                    .put("bytes", 0)
                    .put("sha256", emptySha256))
            }
            val manifest = org.json.JSONObject()
                .put("format", "de.pulselibre.local-database-backup")
                .put("version", 7)
                .put("app_id", "de.pulselibre")
                .put("created_at", "2026-11-22T00:00:00Z")
                .put("encryption", "none")
                .put("entries", entries)
            tamper(manifest)
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return ByteArrayInputStream(out.toByteArray())
    }

    private fun tamperEntry(name: String, key: String, value: Any): (org.json.JSONObject) -> Unit =
        { manifest -> manifest.getJSONObject("entries").getJSONObject(name).put(key, value) }

    @Test fun validSyntheticV7ArchiveIsAccepted() = withCache { ctx, cache ->
        DatabaseBackup.prepareRestore(ctx, validV7Archive()).use { plan ->
            assertEquals(7, plan.version)
            assertEquals(0L, plan.events)
        }
        assertEquals(emptyList<String>(), cache.list().orEmpty().toList())
    }

    @Test fun manipulatedV7ChecksumIsRejected() =
        assertRejectedWithoutTrace(validV7Archive(tamper = tamperEntry("cirqa_events.jsonl", "sha256", "0".repeat(64))))

    @Test fun manipulatedV7ByteCountIsRejected() =
        assertRejectedWithoutTrace(validV7Archive(tamper = tamperEntry("canonical_observations.jsonl", "bytes", 1)))

    @Test fun manipulatedV7RecordCountIsRejected() =
        assertRejectedWithoutTrace(validV7Archive(tamper = tamperEntry("sync_cursors.jsonl", "records", 1)))

    @Test fun missingV7GenerationEntryIsRejected() =
        assertRejectedWithoutTrace(validV7Archive(omitEntry = "canonical_fit_active_generations.jsonl"))

    @Test fun unsupportedBackupVersionIsRejected() =
        assertRejectedWithoutTrace(validV7Archive { it.put("version", 8) })
}
