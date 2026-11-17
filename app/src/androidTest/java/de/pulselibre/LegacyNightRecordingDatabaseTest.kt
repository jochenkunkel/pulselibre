package de.pulselibre

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.file.Files

@RunWith(AndroidJUnit4::class)
class LegacyNightRecordingDatabaseTest {
    @Test fun openingDatabaseRemovesOnlyLegacyFileAndPreservesStoredEvents() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "legacy-night-test-").toFile()
        // Isolate both files and database; never touch the app's actual measurement database.
        val context = object : ContextWrapper(base) {
            override fun getFilesDir(): File = root
            override fun getDatabasePath(name: String): File = File(root, name)
            override fun openOrCreateDatabase(name: String, mode: Int,
                factory: SQLiteDatabase.CursorFactory?): SQLiteDatabase =
                openOrCreateDatabase(name, mode, factory, null)
            override fun openOrCreateDatabase(name: String, mode: Int,
                factory: SQLiteDatabase.CursorFactory?, errorHandler: DatabaseErrorHandler?): SQLiteDatabase =
                SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).absolutePath, factory, errorHandler)
        }
        try {
            val stored = """{"schema":1,"type":"reading","received_at_utc":"2026-11-17T08:00:00Z","service":6,"bpm":71}"""
            CirqaDatabase(context).use { assertTrue(it.insertLine(stored)) }
            val old = File(root, "cirqa_overnight.jsonl").apply {
                writeText("""{"schema":1,"type":"reading","received_at_utc":"2026-11-16T08:00:00Z","service":6,"bpm":72}""")
            }
            val other = File(root, "export.jsonl").apply { writeText("keep export") }
            repeat(2) {
                CirqaDatabase(context).use { db ->
                    assertEquals(listOf(stored), db.readingLines())
                    assertEquals(1L, db.eventCount())
                }
                assertFalse(old.exists())
                assertEquals("keep export", other.readText())
            }
        } finally {
            root.deleteRecursively()
        }
    }
}
