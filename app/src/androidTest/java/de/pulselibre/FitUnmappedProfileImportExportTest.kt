package de.pulselibre

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic-only SQLite/export regression for opaque and review-only FIT profile fields. */
@RunWith(AndroidJUnit4::class)
class FitUnmappedProfileImportExportTest {
    @Test fun unknown397AndKnown398CandidatesPersistRawWithoutCanonicalOrExportRows() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "fit-unknown-profile-").toFile()
        val fitDirectory = File(root, "verified_fit").also { assertTrue(it.mkdir()) }
        val context = isolatedContext(base, root)
        try {
            val fit = syntheticGlobal397And398Fit()
            File(fitDirectory, "${sha256(fit)}.fit").writeBytes(fit)

            val result = CirqaDatabase(context).use { database ->
                CirqaFitBackfill(fitDirectory, database).run()
            }

            assertEquals(1, result.filesSeen)
            assertEquals(1, result.filesImported)
            assertEquals(CirqaFitBackfill.FileStatus.IMPORTED,
                result.fileResults.single().status)
            assertEquals(0, result.importableObservations)
            assertEquals(0, result.committedObservations)
            assertEquals(2, result.reviewCandidates)

            CirqaDatabase(context).use { database ->
                val db = database.writableDatabase
                assertEquals(1, count(db, "fit_sources"))
                assertEquals(1, count(db, "fit_decoder_generations"))
                assertEquals(1, count(db, "fit_active_generations"))
                assertEquals(8, count(db, "fit_generation_fields"))
                assertEquals(2, count(db, "fit_unmapped_fields"))
                assertEquals(2, count(db, "fit_review_candidates"))
                assertEquals(2, db.rawQuery(
                    "SELECT COUNT(*) FROM fit_review_candidates WHERE decoder_version=?",
                    arrayOf(FitHistoricalObservations.REVIEW_CANDIDATE_DECODER_VERSION)
                ).use {
                    assertTrue(it.moveToFirst())
                    it.getInt(0)
                })
                assertEquals(0, count(db, "fit_generation_observations"))
                assertEquals(0, count(db, "observations"))
                assertEquals(0, count(db, "fit_observation_provenance"))
                val unified = UnifiedMeasurements(db)
                unified.refresh()
                assertTrue(unified.records(0, Long.MAX_VALUE).isEmpty())
                assertEquals("ok", db.rawQuery("PRAGMA quick_check", null).use {
                    assertTrue(it.moveToFirst())
                    it.getString(0)
                })
            }
        } finally {
            root.deleteRecursively()
        }
    }

    private data class Field(val number: Int, val size: Int, val baseType: Int)

    private fun syntheticGlobal397And398Fit(): ByteArray {
        val data = globalMessage(397, listOf(
            Field(253, 4, 0x86), Field(1, 4, 0x88)
        ), u32(0x10000000) + byteArrayOf(0, 0, 0, 0)) +
            globalMessage(398, listOf(
                Field(253, 4, 0x86), Field(0, 4, 0x86), Field(1, 4, 0x88),
                Field(2, 4, 0x88), Field(3, 1, 0x02), Field(4, 4, 0x88)
            ), u32(0x10000001) + u32(0x10000001) +
                byteArrayOf(0, 0, 0, 0) + byteArrayOf(0, 0, 0, 0) +
                byteArrayOf(0) + byteArrayOf(0, 0, 0, 0))
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

    private fun globalMessage(global: Int, fields: List<Field>, record: ByteArray): ByteArray {
        val local = if (global == 397) 0 else 1
        val output = ByteArrayOutputStream()
        output.write(byteArrayOf(
            (0x40 + local).toByte(), 0, 0,
            global.toByte(), (global ushr 8).toByte(), fields.size.toByte()
        ))
        fields.forEach { field ->
            output.write(byteArrayOf(field.number.toByte(), field.size.toByte(), field.baseType.toByte()))
        }
        output.write(local)
        output.write(record)
        return output.toByteArray()
    }

    private fun u32(value: Int) = byteArrayOf(
        value.toByte(), (value ushr 8).toByte(),
        (value ushr 16).toByte(), (value ushr 24).toByte()
    )

    private fun count(db: SQLiteDatabase, table: String): Int =
        db.rawQuery("SELECT COUNT(*) FROM $table", null).use {
            assertTrue(it.moveToFirst())
            it.getInt(0)
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
