package de.pulselibre

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CirqaFitBackfillInstrumentedTest {
    @Test fun existingVerifiedFitFilesBackfillIdempotentlyIntoCanonicalSqliteOffline() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "fit-backfill-e2e-").toFile()
        val fitDirectory = File(root, "verified_fit").also { assertTrue(it.mkdir()) }
        val context = isolatedContext(base, root)
        try {
            val fit = syntheticHeartRateFit()
            File(fitDirectory, "${sha256(fit)}.fit").writeBytes(fit)

            val first = CirqaDatabase(context).use { database ->
                CirqaFitBackfill(fitDirectory, database).run()
            }
            val replay = CirqaDatabase(context).use { database ->
                CirqaFitBackfill(fitDirectory, database).run()
            }

            assertEquals(1, first.filesSeen)
            assertEquals(1, first.filesImported)
            assertEquals(1, first.importableObservations)
            assertEquals(1, replay.filesImported)
            assertEquals(1, replay.importableObservations)
            assertTrue(!first.truncated && !replay.truncated)
            CirqaDatabase(context).use { database ->
                val db = database.readableDatabase
                assertEquals("ok", db.rawQuery("PRAGMA quick_check", null).use {
                    assertTrue(it.moveToFirst())
                    it.getString(0)
                })
                assertEquals(1, db.rawQuery("SELECT COUNT(*) FROM observations", null).use {
                    assertTrue(it.moveToFirst())
                    it.getInt(0)
                })
                assertEquals(1, db.rawQuery("SELECT COUNT(*) FROM fit_sources", null).use {
                    assertTrue(it.moveToFirst())
                    it.getInt(0)
                })
                assertEquals(1, db.rawQuery(
                    "SELECT COUNT(*) FROM fit_observation_provenance", null
                ).use {
                    assertTrue(it.moveToFirst())
                    it.getInt(0)
                })
                assertEquals(0, db.rawQuery("PRAGMA foreign_key_check", null).use {
                    var count = 0
                    while (it.moveToNext()) count++
                    count
                })
            }
        } finally {
            root.deleteRecursively()
        }
    }

    private fun syntheticHeartRateFit(): ByteArray {
        val data = byteArrayOf(
            0x40, 0, 0, 20, 0, 2,
            253.toByte(), 4, 0x86.toByte(),
            3, 1, 2,
            0
        ) + byteArrayOf(0x10, 0x27, 0, 0, 72)
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

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun isolatedContext(base: Context, root: File): Context = object : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
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
