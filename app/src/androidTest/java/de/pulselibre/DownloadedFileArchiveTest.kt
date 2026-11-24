package de.pulselibre

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files

/** Generated data only. Emulator/CI, never instrumentation on physical phones. */
@RunWith(AndroidJUnit4::class)
class DownloadedFileArchiveTest {
    private fun isolated(base: Context, root: File): Context = object : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = root
        override fun getDatabasePath(name: String): File = File(root, name)
        override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?) =
            openOrCreateDatabase(name, mode, factory, null)
        override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?,
                                         errorHandler: DatabaseErrorHandler?): SQLiteDatabase =
            SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).absolutePath, factory, errorHandler)
    }

    @Test fun changedContentUnderSameReferenceAndNewReferenceForKnownContentAreRetained() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "reference-reconciliation-").toFile()
        val context = isolated(base, root)
        val reference = byteArrayOf(4, 2, 4)
        val newReference = byteArrayOf(7, 7, 1)
        val original = byteArrayOf(1, 2, 3, 4)
        val changed = byteArrayOf(1, 2, 3, 5)
        try {
            CirqaDatabase(context).use { database ->
                // Same reference/UID, different bytes: both immutable contents survive.
                assertFalse(database.archiveDownloadedFile(original, reference, "fit", 1000))
                assertFalse(database.archiveDownloadedFile(changed, reference, "fit", 1001))
                // Already-known content, new reference: associate the new reference too.
                assertTrue(database.archiveDownloadedFile(original, newReference, "fit", 1002))
                val db = database.readableDatabase
                db.rawQuery("SELECT COUNT(*) FROM downloaded_files", null).use {
                    assertTrue(it.moveToFirst())
                    assertEquals(2, it.getInt(0))
                }
                db.rawQuery("SELECT COUNT(*) FROM downloaded_file_references", null).use {
                    assertTrue(it.moveToFirst())
                    assertEquals(3, it.getInt(0))
                }
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun unknownOriginalsKeepBothSameSizeVersionsAndRoundTripBeyondCursorWindow() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "original-archive-").toFile()
        val context = isolated(base, root)
        // Above the usual 2 MiB CursorWindow; not a FIT file and intentionally undecoded.
        val first = ByteArray(2 * 1024 * 1024 + 17) { (it % 251).toByte() }
        val second = first.copyOf().apply { this[100] = (this[100].toInt() xor 1).toByte() }
        val lines = mutableListOf<String>()
        try {
            CirqaDatabase(context).use { database ->
                assertFalse(database.archiveDownloadedFile(first, byteArrayOf(1, 2), "unknown", 1000))
                assertTrue(database.archiveDownloadedFile(first, byteArrayOf(1, 2), "unknown", 1001))
                assertFalse(database.archiveDownloadedFile(second, byteArrayOf(1, 3), "unknown", 2000))
                DownloadedFileArchive.export(database.readableDatabase, lines::add)
                database.readableDatabase.rawQuery("SELECT COUNT(*) FROM downloaded_files", null).use {
                    assertTrue(it.moveToFirst()); assertEquals(2, it.getInt(0))
                }
                assertEquals(0, database.readableDatabase.rawQuery(
                    "SELECT COUNT(*) FROM observations", null).use { it.moveToFirst(); it.getInt(0) })
            }
            DownloadedFileArchive.consume(lines.asSequence().map(DownloadedFileArchive::parse))
            val zip = ByteArrayOutputStream()
            DatabaseBackup.write(context, zip)
            DatabaseBackup.prepareRestore(context, ByteArrayInputStream(zip.toByteArray())).use {
                assertEquals(10, it.version)
                DatabaseBackup.restore(context, it)
            }
            CirqaDatabase(context).use { database ->
                val restored = mutableListOf<String>()
                DownloadedFileArchive.export(database.readableDatabase, restored::add)
                assertEquals(lines, restored)
            }
            val corrupted = JSONObject(lines.first()).put("data",
                java.util.Base64.getEncoder().encodeToString(ByteArray(DownloadedFileArchive.CHUNK_BYTES))).toString()
            assertThrows(IllegalArgumentException::class.java) {
                DownloadedFileArchive.consume((listOf(corrupted) + lines.drop(1)).asSequence()
                    .map(DownloadedFileArchive::parse))
            }
            assertThrows(IllegalArgumentException::class.java) {
                DownloadedFileArchive.consume(lines.asSequence().drop(1).map(DownloadedFileArchive::parse))
            }
        } finally { root.deleteRecursively() }
    }

    @Test fun upgradeFromNinePreservesRowsAndBackfillsExistingOpaqueOriginals() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "original-migration-").toFile()
        val context = isolated(base, root)
        val bytes = byteArrayOf(0, 127, -1, 19, 5)
        val sha = DownloadedFileArchive.sha256(bytes)
        try {
            val directory = File(root, "verified_raw").apply { mkdirs() }
            File(directory, "$sha.bin").writeBytes(bytes)
            CirqaDatabase(context).use {
                it.insertLine("""{"schema":1,"type":"attempt","received_at_utc":"2026-01-01T00:00:00Z"}""")
                it.writableDatabase.execSQL("DROP TABLE downloaded_file_references")
                it.writableDatabase.execSQL("DROP TABLE downloaded_files")
                it.writableDatabase.version = 9
            }
            CirqaDatabase(context).use {
                assertEquals(11, it.readableDatabase.version)
                assertEquals(1, it.allLines().size)
                assertEquals(1, it.archiveExistingDownloads())
                assertEquals(0, it.archiveExistingDownloads())
                it.readableDatabase.rawQuery("SELECT payload FROM downloaded_files", null).use { rows ->
                    assertTrue(rows.moveToFirst()); assertArrayEquals(bytes, rows.getBlob(0))
                }
            }
        } finally { root.deleteRecursively() }
    }
}
