package de.pulselibre

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic bytes only; verifies #600 capture via the existing backup-covered archive. */
@RunWith(AndroidJUnit4::class)
class CirqaRawNotificationArchiveTest {
    private lateinit var db: SQLiteDatabase

    @Before fun setUp() {
        db = SQLiteDatabase.create(null)
        DownloadedFileArchive.create(db)
    }

    @After fun tearDown() { db.close() }

    @Test fun rawNotificationsAreStoredOnceAndCounted() {
        val first = byteArrayOf(0x1b, 0x10, 0x00, 1, 2, 3)
        val second = byteArrayOf(0x52, 0x11, 0x00, 9)
        assertTrue(CirqaRawNotificationArchive.archive(db, first, 1_000L))
        assertTrue(CirqaRawNotificationArchive.archive(db, first, 1_001L))
        assertTrue(CirqaRawNotificationArchive.archive(db, second, 1_002L))
        assertFalse(CirqaRawNotificationArchive.archive(db, byteArrayOf(), 1_003L))

        assertEquals(2L, CirqaRawNotificationArchive.count(db))
        db.rawQuery("SELECT COUNT(*) FROM downloaded_files", null).use { rows ->
            assertTrue(rows.moveToFirst())
            assertEquals(2, rows.getInt(0))
        }
        db.rawQuery("SELECT payload FROM downloaded_files WHERE content_sha256=?",
            arrayOf(DownloadedFileArchive.sha256(first))).use { rows ->
            assertTrue(rows.moveToFirst())
            org.junit.Assert.assertArrayEquals(first, rows.getBlob(0))
        }
    }
}
