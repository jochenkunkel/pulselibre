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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files

@RunWith(AndroidJUnit4::class)
class DatabaseBackupTest {
    @Test fun backupResetAndRestoreRoundTripBothLocalStoresAndCursor() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "database-backup-test-").toFile()
        val context = isolatedContext(base, root)
        try {
            val event = """{"schema":1,"type":"reading","received_at_utc":"2026-11-17T08:00:00Z","service":6,"bpm":71}"""
            CirqaDatabase(context).use { assertTrue(it.insertLine(event)) }
            val historical = RawExportRecord("test-device", "spo2", 2_000, 97.0, "%", "sample", 3_000)
            LocalReadingStore(context).use { it.commitBatch("test-device", listOf(historical)) }

            val bytes = ByteArrayOutputStream()
            val report = DatabaseBackup.write(context, bytes)
            assertEquals(1L, report.events)
            assertEquals(1L, report.legacyReadings)
            assertEquals(1L, report.syncCursors)

            DatabaseBackup.reset(context)
            CirqaDatabase(context).use { assertEquals(0L, it.eventCount()) }
            LocalReadingStore(context).use {
                assertTrue(it.readWindow(ExportWindow(0, Long.MAX_VALUE)).isEmpty())
                assertNull(it.cursorFor("test-device").lastCommittedEpochMillis)
            }

            val plan = DatabaseBackup.prepareRestore(context, ByteArrayInputStream(bytes.toByteArray()))
            try {
                DatabaseBackup.restore(context, plan)
            } finally { plan.close() }

            CirqaDatabase(context).use { db ->
                assertEquals(1L, db.eventCount())
                assertEquals(listOf(event), db.allLines())
            }
            LocalReadingStore(context).use {
                assertEquals(listOf(historical), it.readWindow(ExportWindow(0, Long.MAX_VALUE)))
                assertEquals(2_000L, it.cursorFor("test-device").lastCommittedEpochMillis)
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun invalidArchiveIsRejectedBeforeAnyStoredDataChanges() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "database-backup-invalid-test-").toFile()
        val context = isolatedContext(base, root)
        try {
            val event = """{"schema":1,"type":"attempt","received_at_utc":"2026-11-17T08:00:00Z"}"""
            CirqaDatabase(context).use { assertTrue(it.insertLine(event)) }
            assertThrows(IllegalArgumentException::class.java) {
                DatabaseBackup.prepareRestore(context, ByteArrayInputStream("not a zip".toByteArray()))
            }
            CirqaDatabase(context).use { assertEquals(listOf(event), it.allLines()) }
        } finally {
            root.deleteRecursively()
        }
    }

    private fun isolatedContext(base: Context, root: File): Context = object : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = root
        override fun getDatabasePath(name: String): File = File(root, name)
        override fun openOrCreateDatabase(name: String, mode: Int,
            factory: SQLiteDatabase.CursorFactory?): SQLiteDatabase =
            openOrCreateDatabase(name, mode, factory, null)
        override fun openOrCreateDatabase(name: String, mode: Int,
            factory: SQLiteDatabase.CursorFactory?, errorHandler: DatabaseErrorHandler?): SQLiteDatabase =
            SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).absolutePath, factory, errorHandler)
    }
}
