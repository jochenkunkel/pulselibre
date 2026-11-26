package de.pulselibre

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.file.Files

/**
 * Owner report 26.11.2026: after "Datenbank zurücksetzen" the Verlauf still showed data. Live rows stayed in the derived
 * table (the recorder wrote new events at once, so the id-based rebuild never triggered) and the band values came back
 * from the FIT originals that the app re-imports on start.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseResetTest {
    private fun reading(bpm: Int, at: String) =
        """{"schema":1,"type":"reading","service":6,"received_at_utc":"$at","bpm":$bpm}"""

    @Test fun resetEmptiesTheVerlaufAndDeletesTheOriginals() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "reset-").toFile()
        val context = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = root
            override fun getDatabasePath(name: String): File = File(root, name)
            override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?): SQLiteDatabase =
                openOrCreateDatabase(name, mode, factory, null)
            override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?,
                                              errorHandler: DatabaseErrorHandler?): SQLiteDatabase =
                SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).absolutePath, factory, errorHandler)
        }
        fun livePoints() = CirqaDatabase(context).use { db ->
            val u = UnifiedMeasurements(db.writableDatabase); u.refresh()
            u.points(MeasurementOrigin.LIVE, "bpm", 0, Long.MAX_VALUE / 4).map { it.second }
        }
        try {
            CirqaDatabase(context).use { db ->
                db.ensureHistoricalStoreCopied()
                assertTrue(db.insertLine(reading(70, "2026-11-25T10:00:00Z")))
                assertTrue(db.insertLine(reading(71, "2026-11-25T10:01:00Z")))
            }
            assertEquals(listOf(70.0, 71.0), livePoints())
            File(root, "verified_fit").apply { mkdirs() }.resolve("a".repeat(64) + ".fit").writeBytes(byteArrayOf(1, 2, 3))
            File(root, "cirqa_fit_history/session_1").apply { mkdirs() }.resolve("fit_index_1.fit").writeBytes(byteArrayOf(4))
            File(root, "verified_raw").apply { mkdirs() }.resolve("b".repeat(64) + ".bin").writeBytes(byteArrayOf(5))

            assertEquals(3, DatabaseBackup.reset(context))
            // The recorder restarts at once and writes a new event before the Verlauf refreshes.
            CirqaDatabase(context).use { assertTrue(it.insertLine(reading(80, "2026-11-26T09:00:00Z"))) }

            assertEquals("only the new reading is left", listOf(80.0), livePoints())
            DatabaseBackup.ORIGINAL_DIRECTORIES.forEach { name ->
                assertEquals("$name is empty", 0, File(root, name).listFiles().orEmpty().size)
            }
        } finally { root.deleteRecursively() }
    }
}
