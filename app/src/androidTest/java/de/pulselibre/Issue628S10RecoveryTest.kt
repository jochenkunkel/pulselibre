package de.pulselibre

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Optional, private-fixture integration test. CI skips unless the caller supplies
 * `issue628FixtureDir` as a relative path under the app's private files directory.
 * The FIT originals are read-only inputs and are never copied into test source/resources.
 */
@RunWith(AndroidJUnit4::class)
class Issue628S10RecoveryTest {
    @Test fun recoveredS10FilesPersistAndReimportIdempotently() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val relativeFixtureDir = InstrumentationRegistry.getArguments()
            .getString("issue628FixtureDir")
        assumeTrue("private S10 fixture argument not supplied", !relativeFixtureDir.isNullOrBlank())
        val fixtureDir = File(base.filesDir, requireNotNull(relativeFixtureDir))
        assertTrue("fixture directory missing", fixtureDir.isDirectory)

        val originals = fixtureDir.listFiles().orEmpty().filter { it.name.endsWith(".fit") }.sortedBy { it.name }
        assertEquals(45, originals.size)
        val sourceSnapshot = originals.associate { file ->
            assertEquals(file.name, "${sha256(file.readBytes())}.fit")
            file.name to sha256(file.readBytes())
        }
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "issue628-s10-sqlite-").toFile()
        val isolated = isolatedContext(base, root)
        val deviceKey = "issue628-private-fixture"
        try {
            val first = CirqaDatabase(isolated).use { db ->
                CirqaFitBackfill(fixtureDir, db, deviceKey).run()
            }
            assertEquals(45, first.filesSeen)
            assertEquals(45, first.filesImported)
            assertEquals(0, first.invalidFiles)
            assertEquals(0, first.failedFiles)
            assertEquals(29_028, first.importableObservations)
            assertEquals(29_028, first.committedObservations)
            assertTrue(!first.truncated)

            val before = CirqaDatabase(isolated).use { db ->
                val sqlite = db.readableDatabase
                assertEquals("ok", scalar(sqlite, "PRAGMA quick_check"))
                assertEquals(45L, count(sqlite, "fit_sources"))
                assertEquals(29_028L, countForDevice(sqlite, "observations", deviceKey))
                assertEquals(29_028L, countForDevice(sqlite, "fit_observation_provenance", deviceKey))
                assertEquals(29_028L, count(sqlite, "fit_generation_observations"))
                assertEquals(171_740L, count(sqlite, "fit_generation_fields"))
                assertEquals(45L, count(sqlite, "fit_decoder_generations"))
                assertEquals(
                    "PROFILE_VALID",
                    scalar(sqlite, "SELECT quality FROM fit_observation_provenance LIMIT 1")
                )
                assertEquals(
                    FitHistoricalObservations.DECODER_VERSION,
                    scalar(sqlite, "SELECT decoder_version FROM fit_sources LIMIT 1")
                )
                assertEquals(
                    0L,
                    countRows(sqlite, "PRAGMA foreign_key_check")
                )
                generationSnapshot(sqlite)
            }

            val replay = CirqaDatabase(isolated).use { db ->
                CirqaFitBackfill(fixtureDir, db, deviceKey).run()
            }
            assertEquals(first, replay)

            val after = CirqaDatabase(isolated).use { db ->
                val sqlite = db.readableDatabase
                assertEquals("ok", scalar(sqlite, "PRAGMA quick_check"))
                generationSnapshot(sqlite)
            }
            assertEquals(before, after)
            assertEquals(sourceSnapshot, originals.associate { it.name to sha256(it.readBytes()) })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun restoredS10SnapshotAcceptsV3ReimportWithoutChangingExistingRows() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val relativeFixtureDir = InstrumentationRegistry.getArguments()
            .getString("issue628FixtureDir")
        val relativeBackup = InstrumentationRegistry.getArguments()
            .getString("issue628BackupFile")
        assumeTrue(
            "private S10 fixture and backup arguments not supplied",
            !relativeFixtureDir.isNullOrBlank() && !relativeBackup.isNullOrBlank()
        )
        val fixtureDir = File(base.filesDir, requireNotNull(relativeFixtureDir))
        val backupFile = File(base.filesDir, requireNotNull(relativeBackup))
        assertEquals(45, fixtureDir.listFiles().orEmpty().count { it.name.endsWith(".fit") })
        assertTrue(backupFile.isFile)

        val root = Files.createTempDirectory(base.cacheDir.toPath(), "issue628-s10-restore-").toFile()
        val isolated = isolatedContext(base, root)
        try {
            FileInputStream(backupFile).use { input ->
                DatabaseBackup.prepareRestore(isolated, input).use { plan ->
                    assertEquals(9, plan.version)
                    assertEquals(45L, plan.canonicalFitSources)
                    assertEquals(29_028L, plan.canonicalReadings)
                    assertEquals(29_028L, plan.canonicalFitProvenance)
                    DatabaseBackup.restore(isolated, plan)
                }
            }

            val before = CirqaDatabase(isolated).use { database ->
                val db = database.readableDatabase
                assertEquals("ok", scalar(db, "PRAGMA quick_check"))
                assertEquals(45L, count(db, "fit_sources"))
                assertEquals(29_028L, count(db, "observations"))
                assertEquals(29_028L, count(db, "fit_observation_provenance"))
                assertEquals(45L, count(db, "fit_decoder_generations"))
                assertEquals(45L, countRows(
                    db, """SELECT 1 FROM fit_active_generations a
                           JOIN fit_decoder_generations g USING(generation_id)
                           WHERE g.decoder_version = '1'"""
                ))
                generationSnapshot(db)
            }

            val first = CirqaDatabase(isolated).use { db ->
                CirqaFitBackfill(fixtureDir, db).run()
            }
            val statusCounts = first.fileResults.groupingBy { it.status.name }.eachCount()
            val failureCounts = first.fileResults
                .filter { it.failureStage != null }
                .groupingBy { it.failureStage!!.name }
                .eachCount()
            val failureAliases = first.fileResults
                .filter { it.status == CirqaFitBackfill.FileStatus.FAILED }
                .map { it.alias }
            val conflictDetailCounts = CirqaDatabase(isolated).use { db ->
                val candidates = fixtureDir.listFiles().orEmpty()
                    .filter { it.name.matches(Regex("^[0-9a-f]{64}\\.fit$")) }
                    .sortedBy { it.name }
                first.fileResults
                    .filter { it.status == CirqaFitBackfill.FileStatus.FAILED }
                    .mapNotNull { result ->
                        val index = result.alias.removePrefix("file-").toIntOrNull()?.minus(1)
                            ?: return@mapNotNull null
                        val file = candidates.getOrNull(index) ?: return@mapNotNull null
                        try {
                            CirqaFitBackfill.importVerifiedFile(file, db)
                            "RETRY_SUCCEEDED"
                        } catch (error: IllegalStateException) {
                            when {
                                error.message?.contains("review candidate", ignoreCase = true) == true ->
                                    "REVIEW_CANDIDATE_CONFLICT"
                                error.message?.contains("provenance", ignoreCase = true) == true ->
                                    "PROVENANCE_CONFLICT"
                                error.message?.contains("reading", ignoreCase = true) == true ->
                                    "OBSERVATION_CONFLICT"
                                error.message?.contains("raw field", ignoreCase = true) == true ->
                                    "DECODED_FIELD_CONFLICT"
                                error.message?.contains("device metadata", ignoreCase = true) == true ->
                                    "DEVICE_METADATA_CONFLICT"
                                error.message?.contains("generation", ignoreCase = true) == true ->
                                    "GENERATION_CONFLICT"
                                error.message?.contains("source metadata", ignoreCase = true) == true ->
                                    "SOURCE_METADATA_CONFLICT"
                                error.message == "Check failed." ->
                                    "UNSPECIFIED_CHECK_CONFLICT"
                                else -> "OTHER_ILLEGAL_STATE"
                            }
                        } catch (_: IllegalArgumentException) {
                            "SOURCE_CONTRACT_CONFLICT"
                        }
                    }
                    .groupingBy { it }
                    .eachCount()
            }
            assertEquals(mapOf("REVIEW_CANDIDATE_CONFLICT" to 5), conflictDetailCounts)
            android.util.Log.i(
                "Issue628S10Recovery",
                "restored-v1 reimport: seen=${first.filesSeen}, imported=${first.filesImported}, " +
                    "invalid=${first.invalidFiles}, failed=${first.failedFiles}, " +
                    "eligible=${first.importableObservations}, committed=${first.committedObservations}, " +
                    "statuses=$statusCounts, failureStages=$failureCounts, " +
                    "failureCount=${failureAliases.size}, conflictDetails=$conflictDetailCounts"
            )
            assertEquals(45, first.filesSeen)
            assertEquals(0, first.invalidFiles)
            assertEquals(29_028, first.importableObservations)
            assertEquals(first.failedFiles, failureCounts.values.sum())
            assertTrue(!first.truncated)

            val afterFirst = CirqaDatabase(isolated).use { database ->
                val db = database.readableDatabase
                assertEquals("ok", scalar(db, "PRAGMA quick_check"))
                assertEquals(45L, count(db, "fit_sources"))
                assertEquals(29_028L, count(db, "observations"))
                assertEquals(29_028L, count(db, "fit_observation_provenance"))
                assertEquals(45L + first.filesImported, count(db, "fit_decoder_generations"))
                assertEquals(29_028L + first.committedObservations, count(db, "fit_generation_observations"))
                assertEquals(45L, countRows(
                    db, """SELECT 1 FROM fit_active_generations a
                           JOIN fit_decoder_generations g USING(generation_id)
                           WHERE g.decoder_version = '1'"""
                ))
                assertEquals(
                    "1",
                    scalar(db, "SELECT DISTINCT decoder_version FROM fit_observation_provenance")
                )
                generationSnapshot(db)
            }
            assertTrue(before != afterFirst)

            val replay = CirqaDatabase(isolated).use { db ->
                CirqaFitBackfill(fixtureDir, db).run()
            }
            assertEquals(first, replay)
            val afterReplay = CirqaDatabase(isolated).use { database ->
                val db = database.readableDatabase
                assertEquals("ok", scalar(db, "PRAGMA quick_check"))
                assertEquals(45L, count(db, "fit_sources"))
                assertEquals(29_028L, count(db, "observations"))
                assertEquals(29_028L, count(db, "fit_observation_provenance"))
                assertEquals(45L + first.filesImported, count(db, "fit_decoder_generations"))
                assertEquals(29_028L + first.committedObservations, count(db, "fit_generation_observations"))
                assertEquals(45L, countRows(
                    db, """SELECT 1 FROM fit_active_generations a
                           JOIN fit_decoder_generations g USING(generation_id)
                           WHERE g.decoder_version = '1'"""
                ))
                generationSnapshot(db)
            }
            assertEquals(afterFirst, afterReplay)
        } finally {
            root.deleteRecursively()
        }
    }

    private fun generationSnapshot(db: SQLiteDatabase): String {
        val digest = MessageDigest.getInstance("SHA-256")
        db.rawQuery(
            """SELECT generation_id, output_digest, output_count, decoded_field_count
               FROM fit_decoder_generations ORDER BY generation_id""",
            null
        ).use { rows ->
            while (rows.moveToNext()) {
                for (column in 0 until rows.columnCount) {
                    digest.update(rows.getString(column).toByteArray(Charsets.UTF_8))
                    digest.update(0)
                }
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun count(db: SQLiteDatabase, table: String): Long =
        db.rawQuery("SELECT COUNT(*) FROM $table", null).use {
            assertTrue(it.moveToFirst())
            it.getLong(0)
        }

    private fun countForDevice(db: SQLiteDatabase, table: String, deviceKey: String): Long =
        db.rawQuery("SELECT COUNT(*) FROM $table WHERE device_id = ?", arrayOf(deviceKey)).use {
            assertTrue(it.moveToFirst())
            it.getLong(0)
        }

    private fun countRows(db: SQLiteDatabase, query: String): Long =
        db.rawQuery(query, null).use { rows ->
            var count = 0L
            while (rows.moveToNext()) count++
            count
        }

    private fun scalar(db: SQLiteDatabase, query: String): String =
        db.rawQuery(query, null).use {
            assertTrue(it.moveToFirst())
            it.getString(0)
        }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun isolatedContext(base: Context, root: File) = object : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = root
        override fun getDatabasePath(name: String): File =
            File(root, name).also { it.parentFile?.mkdirs() }

        override fun openOrCreateDatabase(
            name: String,
            mode: Int,
            factory: SQLiteDatabase.CursorFactory?
        ): SQLiteDatabase = openOrCreateDatabase(name, mode, factory, null)

        override fun openOrCreateDatabase(
            name: String,
            mode: Int,
            factory: SQLiteDatabase.CursorFactory?,
            errorHandler: DatabaseErrorHandler?
        ): SQLiteDatabase =
            SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).absolutePath, factory, errorHandler)
    }
}
