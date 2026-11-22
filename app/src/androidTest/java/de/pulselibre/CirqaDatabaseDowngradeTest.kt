package de.pulselibre

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.file.Files

/** A database whose schema version was raised by another build must still open and keep its data. */
@RunWith(AndroidJUnit4::class)
class CirqaDatabaseDowngradeTest {
    private fun isolatedContext(base: Context, root: File) = object : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = root
        override fun getDatabasePath(name: String): File = File(root, name).also { it.parentFile?.mkdirs() }
        override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?): SQLiteDatabase =
            openOrCreateDatabase(name, mode, factory, null)
        override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?,
                                          errorHandler: DatabaseErrorHandler?): SQLiteDatabase =
            SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).absolutePath, factory, errorHandler)
    }

    @Test fun databaseWithNewerSchemaVersionOpensAndKeepsItsRows() {
        val root = Files.createTempDirectory("downgrade").toFile()
        try {
            val context = isolatedContext(ApplicationProvider.getApplicationContext(), root)
            CirqaDatabase(context).use { it.writableDatabase.execSQL(
                "INSERT INTO events(line_sha256, received_at_utc, kind, service, payload_json) VALUES('a', '2026-11-19T10:00:00Z', 'reading', 6, '{}')") }
            File(root, "cirqa_measurements.db").let { file ->
                SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { it.version = 8 }
            }
            val count = CirqaDatabase(context).use { db ->
                db.readableDatabase.rawQuery("SELECT COUNT(*) FROM events", null).use { it.moveToFirst(); it.getInt(0) }
            }
            assertEquals(1, count)
        } finally { root.deleteRecursively() }
    }
}
