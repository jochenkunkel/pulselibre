package de.pulselibre

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.file.Files

/** Synthetic FIT originals only. Exercises decoder replay directly from the DB archive. */
@RunWith(AndroidJUnit4::class)
class ArchivedFitRedecodeTest {
    private fun isolated(base: Context, root: File): Context = object : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = root
        override fun getDatabasePath(name: String): File = File(root, name)
        override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?) =
            openOrCreateDatabase(name, mode, factory, null)
        override fun openOrCreateDatabase(
            name: String,
            mode: Int,
            factory: SQLiteDatabase.CursorFactory?,
            errorHandler: DatabaseErrorHandler?
        ): SQLiteDatabase = SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).absolutePath, factory, errorHandler)
    }

    /** One synthetic monitoring record with a FIT timestamp and heart-rate field. */
    private fun syntheticMonitoringFit(): ByteArray {
        val data = byteArrayOf(
            0x40, 0, 0, 55, 0, 2,
            253.toByte(), 4, 0x86.toByte(),
            27, 1, 0x02,
            0,
            0, 0, 0, 0x50, 72
        )
        val file = ByteArray(14 + data.size + 2)
        file[0] = 14
        file[1] = 0x20
        file[2] = 0x64
        file[4] = data.size.toByte()
        file[5] = (data.size ushr 8).toByte()
        ".FIT".toByteArray().copyInto(file, 8)
        val headerCrc = FitFileHeader.crc16(file, 0, 12)
        file[12] = headerCrc.toByte()
        file[13] = (headerCrc ushr 8).toByte()
        data.copyInto(file, 14)
        val crc = FitFileHeader.crc16(file, 0, file.size - 2)
        file[file.size - 2] = crc.toByte()
        file[file.size - 1] = (crc ushr 8).toByte()
        return file
    }

    @Test fun staleActiveGenerationIsReplayedAndActivatedFromArchivedOriginal() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "archived-fit-redecode-").toFile()
        val context = isolated(base, root)
        val fit = syntheticMonitoringFit()
        val sha = DownloadedFileArchive.sha256(fit)
        val fitRoot = File(root, "verified_fit")
        try {
            CirqaDatabase(context).use { database ->
                // Establish a committed source/generation, then archive the exact
                // original as the durable restore source.
                val source = CirqaVerifiedFitStore(fitRoot).saveVerified(fit)
                assertEquals(sha, source.sha256)
                val initial = CirqaFitBackfill(fitRoot, database).run()
                assertEquals(1, initial.filesImported)
                database.archiveDownloadedFile(fit, byteArrayOf(1, 2), "fit", 123_456L)
                database.archiveDownloadedFile(byteArrayOf(1, 2, 3), byteArrayOf(3), "unknown", 123_457L)

                val oldGeneration = database.readableDatabase.rawQuery(
                    "SELECT generation_id FROM fit_active_generations WHERE source_sha256 = ?",
                    arrayOf(sha)
                ).use { rows -> assertTrue(rows.moveToFirst()); rows.getString(0) }
                // Model a legitimate active generation from the prior decoder;
                // generation rows are immutable and must not be edited in place.
                val decoded = requireNotNull(FitHistoricalObservations.decode(fit))
                val priorGeneration = database.storeFitDecoderReplay(
                    sourceSha256 = sha,
                    decoderVersion = "3",
                    profileVersion = FitHistoricalObservations.PROFILE_VERSION,
                    replayedAtEpochMillis = 123_456L,
                    observations = decoded.observations.filter {
                        it.quality == FitHistoricalObservations.Quality.PROFILE_VALID &&
                            it.timestampEpochMillis != null
                    },
                    deviceKey = "cirqa-fit",
                    decodedFields = decoded.decodedFields
                )
                database.activateFitDecoderGeneration(sha, priorGeneration)
                assertTrue(oldGeneration != priorGeneration)
                assertTrue(source.file.delete())
                assertFalse(source.file.exists())

                val result = database.recoverArchivedFitSources(fitRoot)
                assertEquals(1, result.candidatesSeen)
                assertEquals(1, result.generationsActivated)
                assertEquals(0, result.invalidCandidates)
                assertEquals(0, result.failedCandidates)
                assertFalse(result.truncated)

                val active = database.readableDatabase.rawQuery(
                    """SELECT g.decoder_version
                       FROM fit_active_generations a
                       JOIN fit_decoder_generations g ON g.generation_id = a.generation_id
                       WHERE a.source_sha256 = ?""",
                    arrayOf(sha)
                ).use { rows -> assertTrue(rows.moveToFirst()); rows.getString(0) }
                assertEquals(FitHistoricalObservations.DECODER_VERSION, active)
                database.readableDatabase.rawQuery(
                    """SELECT COUNT(*) FROM fit_generation_observations
                       WHERE generation_id = (SELECT generation_id FROM fit_active_generations
                                              WHERE source_sha256 = ?)
                         AND global_message_number = 55 AND metric = 'heart_rate'""",
                    arrayOf(sha)
                ).use { rows -> assertTrue(rows.moveToFirst()); assertEquals(1, rows.getInt(0)) }

                // Re-running is idempotent, and neither FIT nor non-FIT archive
                // objects are deleted or rewritten during decoder replay.
                val second = database.recoverArchivedFitSources(fitRoot)
                assertEquals(0, second.generationsActivated)
                database.readableDatabase.rawQuery(
                    "SELECT payload FROM downloaded_files WHERE content_sha256 = ?",
                    arrayOf(sha)
                ).use { rows -> assertTrue(rows.moveToFirst()); assertArrayEquals(fit, rows.getBlob(0)) }
                database.readableDatabase.rawQuery("SELECT COUNT(*) FROM downloaded_files", null).use {
                    assertTrue(it.moveToFirst()); assertEquals(2, it.getInt(0))
                }
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun neverImportedArchivedFitIsMaterializedForNormalBackfill() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "archived-fit-first-import-").toFile()
        val context = isolated(base, root)
        val fit = syntheticMonitoringFit()
        val sha = DownloadedFileArchive.sha256(fit)
        val fitRoot = File(root, "verified_fit")
        try {
            CirqaDatabase(context).use { database ->
                database.archiveDownloadedFile(fit, byteArrayOf(9), "fit", 234_567L)
                assertFalse(fitRoot.exists())

                val recovered = database.recoverArchivedFitSources(fitRoot)
                assertEquals(1, recovered.candidatesSeen)
                assertEquals(1, recovered.filesMaterialized)
                assertEquals(0, recovered.generationsActivated)
                val restored = File(fitRoot, "$sha.fit")
                assertArrayEquals(fit, restored.readBytes())

                val imported = CirqaFitBackfill(fitRoot, database).run()
                assertEquals(1, imported.filesImported)
                val activeDecoder = database.readableDatabase.rawQuery(
                    """SELECT g.decoder_version
                       FROM fit_active_generations a
                       JOIN fit_decoder_generations g ON g.generation_id = a.generation_id
                       WHERE a.source_sha256 = ?""",
                    arrayOf(sha)
                ).use { rows -> assertTrue(rows.moveToFirst()); rows.getString(0) }
                assertEquals(FitHistoricalObservations.DECODER_VERSION, activeDecoder)
            }
        } finally {
            root.deleteRecursively()
        }
    }
}
