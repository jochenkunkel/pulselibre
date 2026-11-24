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
import java.security.MessageDigest
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Regression guards for gaps found in the CI migration audit. Generated data only;
 * emulator/CI, never instrumentation on a physical phone. No product or backup-format
 * code is changed here: if the owner later enables status/import writes, the backup
 * contract must be extended deliberately, not by accident.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseBackupV9ArchiveGapTest {

    @Test fun tamperedV9ArchiveChunkIsRejectedBeforeAnyStoredDataChanges() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "v9-archive-tamper-").toFile()
        val context = isolated(base, root)
        val event = """{"schema":1,"type":"attempt","received_at_utc":"2026-11-23T00:00:00Z"}"""
        val original = ByteArray(DownloadedFileArchive.CHUNK_BYTES + 7) { (it % 251).toByte() }
        try {
            CirqaDatabase(context).use { database ->
                assertTrue(database.insertLine(event))
                database.archiveDownloadedFile(original, byteArrayOf(1, 2), "unknown", 1_000)
            }
            val backup = ByteArrayOutputStream().also { DatabaseBackup.write(context, it) }.toByteArray()
            assertEquals(11, DatabaseBackup.prepareRestore(context, ByteArrayInputStream(backup)).use { it.version })

            // The ZIP checksum is updated to match the tampered entry, so the rejection
            // must come from the whole-archive content validation in consume(), not from
            // the per-entry digest check.
            val tampered = rezipWithTamperedFirstArchiveChunk(backup)
            assertThrows(IllegalArgumentException::class.java) {
                DatabaseBackup.prepareRestore(context, ByteArrayInputStream(tampered))
            }

            CirqaDatabase(context).use { database ->
                assertEquals(listOf(event), database.allLines())
                val restored = mutableListOf<String>()
                DownloadedFileArchive.export(database.readableDatabase, restored::add)
                assertEquals(3, restored.size) // two chunks (CHUNK_BYTES + 7) + one reference
                assertEquals(DownloadedFileArchive.sha256(original), JSONObject(restored.first()).getString("sha256"))
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun statusAndImportTablesAreInsideBackupFromV11() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "v11-status-boundary-").toFile()
        val context = isolated(base, root)
        val event = """{"schema":1,"type":"attempt","received_at_utc":"2026-11-23T01:00:00Z"}"""
        try {
            CirqaDatabase(context).use { database ->
                assertTrue(database.insertLine(event))
                assertEquals(0, countRows(database, "observation_status_events"))
                assertEquals(0, countRows(database, "import_runs"))
                assertEquals(0, countRows(database, "import_errors"))
            }

            val backup = ByteArrayOutputStream().also { DatabaseBackup.write(context, it) }.toByteArray()
            val names = zipEntryNames(backup)
            // Since backup v11 the audit tables are part of every backup; empty tables
            // are written as empty JSONL entries, not omitted.
            assertTrue(names.any { it.startsWith("observation_status_events") })
            assertTrue(names.any { it.startsWith("import_runs") })
            assertTrue(names.any { it.startsWith("import_errors") })

            DatabaseBackup.reset(context)
            val plan = DatabaseBackup.prepareRestore(context, ByteArrayInputStream(backup))
            assertEquals(11, plan.version)
            plan.use { DatabaseBackup.restore(context, it) }

            CirqaDatabase(context).use { database ->
                assertEquals(listOf(event), database.allLines())
                assertEquals(0, countRows(database, "observation_status_events"))
                assertEquals(0, countRows(database, "import_runs"))
                assertEquals(0, countRows(database, "import_errors"))
                assertEquals(0, database.readableDatabase.rawQuery("PRAGMA foreign_key_check", null).use { rows ->
                    var violations = 0
                    while (rows.moveToNext()) violations++
                    violations
                })
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun manuallyLoweredVersionIsUpgradedBackToTenWithoutLosingRows() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "v10-version-seam-").toFile()
        val context = isolated(base, root)
        val event = """{"schema":1,"type":"attempt","received_at_utc":"2026-11-23T02:00:00Z"}"""
        try {
            CirqaDatabase(context).use { database ->
                assertTrue(database.insertLine(event))
                assertEquals(11, database.writableDatabase.version)
                database.writableDatabase.version = 9
            }
            // There is no production helper expecting version 9: with the current
            // helper (version 11) a lowered user_version is an upgrade, never a
            // downgrade. Pin that no rows are lost on this seam.
            CirqaDatabase(context).use { database ->
                assertEquals(11, database.readableDatabase.version)
                assertEquals(listOf(event), database.allLines())
                assertEquals(0, countRows(database, "downloaded_files"))
                assertEquals(0, database.readableDatabase.rawQuery("PRAGMA foreign_key_check", null).use { rows ->
                    var violations = 0
                    while (rows.moveToNext()) violations++
                    violations
                })
            }
        } finally {
            root.deleteRecursively()
        }
    }

    private fun countRows(database: CirqaDatabase, table: String): Int =
        database.readableDatabase.rawQuery("SELECT COUNT(*) FROM $table", null).use { rows ->
            rows.moveToFirst(); rows.getInt(0)
        }

    private fun zipEntryNames(bytes: ByteArray): List<String> {
        val names = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                names += entry.name
            }
        }
        return names
    }

    /** Keep the manifest's per-entry digest/bytes consistent; only the archive content is corrupted. */
    private fun rezipWithTamperedFirstArchiveChunk(bytes: ByteArray): ByteArray {
        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes()
            }
        }
        val name = DownloadedFileArchive.BACKUP_ENTRY
        val lines = entries.getValue(name).toString(Charsets.UTF_8).lineSequence().toList()
        val first = JSONObject(lines.first())
        first.put("data", Base64.getEncoder().encodeToString(ByteArray(DownloadedFileArchive.CHUNK_BYTES)))
        val tamperedLines = listOf(first.toString()) + lines.drop(1)
        val tampered = (tamperedLines.joinToString("\n") + "\n").toByteArray(Charsets.UTF_8)

        val manifest = JSONObject(entries.getValue("manifest.json").toString(Charsets.UTF_8))
        val metadata = manifest.getJSONObject("entries").getJSONObject(name)
        metadata.put("bytes", tampered.size.toLong())
        metadata.put("sha256", sha256(tampered))
        entries[name] = tampered
        entries["manifest.json"] = manifest.toString().toByteArray(Charsets.UTF_8)

        return ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { zip ->
                entries.forEach { (entryName, data) ->
                    zip.putNextEntry(ZipEntry(entryName))
                    zip.write(data)
                    zip.closeEntry()
                }
            }
        }.toByteArray()
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 255) }

    private fun isolated(base: Context, root: File): Context = object : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = root
        override fun getDatabasePath(name: String): File = File(root, name)
        override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?) =
            openOrCreateDatabase(name, mode, factory, null)
        override fun openOrCreateDatabase(
            name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?,
            errorHandler: DatabaseErrorHandler?
        ): SQLiteDatabase = SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).absolutePath, factory, errorHandler)
    }
}
