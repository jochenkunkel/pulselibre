package de.pulselibre

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** P0 capture counters: counted from stored rows, null (never a guess) where a table is missing. */
@RunWith(AndroidJUnit4::class)
class CaptureInventoryTest {
    private lateinit var db: SQLiteDatabase

    @Before fun setUp() { db = SQLiteDatabase.create(null) }
    @After fun tearDown() { db.close() }

    @Test fun missingTablesAreUnknownNotEmpty() {
        val inv = CaptureInventory.load(db)
        assertNull(inv.archivedFiles)
        assertNull(inv.importedFitFiles)
        assertNull(inv.candidatesByMetric)
        assertTrue(inv.isUnknown)
        assertFalse(inv.isEmpty)
    }

    @Test fun readableEmptyTablesAreEmptyNotUnknown() {
        DownloadedFileArchive.create(db)
        db.execSQL("CREATE TABLE fit_sources (source_sha256 TEXT PRIMARY KEY NOT NULL)")
        db.execSQL("CREATE TABLE fit_generation_observations (generation_id TEXT NOT NULL)")
        db.execSQL("CREATE TABLE fit_active_generations (generation_id TEXT NOT NULL)")
        db.execSQL("CREATE TABLE fit_review_candidates (metric TEXT NOT NULL)")
        val inv = CaptureInventory.load(db)
        assertFalse(inv.isUnknown)
        assertTrue(inv.isEmpty)
        assertEquals(0L, inv.archivedFiles)
    }

    @Test fun countsArchivedFilesAndCandidates() {
        DownloadedFileArchive.create(db)
        DownloadedFileArchive.store(db, byteArrayOf(1, 2, 3), byteArrayOf(9), "unknown_type", 1000L)
        db.execSQL("CREATE TABLE fit_review_candidates (metric TEXT NOT NULL)")
        db.execSQL("INSERT INTO fit_review_candidates(metric) VALUES ('spo2'),('spo2'),('stress')")
        val inv = CaptureInventory.load(db)
        assertEquals(1L, inv.archivedFiles)
        assertEquals(3L, inv.archivedBytes)
        assertEquals(mapOf("spo2" to 2L, "stress" to 1L), inv.candidatesByMetric)
        assertNull(inv.regularObservations)
        assertFalse(inv.isEmpty)
    }
}
