package de.pulselibre

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** #528: the trace reads only archived notifications inside the window and selects bytes only with the opt-in. */
@RunWith(AndroidJUnit4::class)
class CommunicationTraceReadTest {
    @Test fun readsWindowedNotificationsAndBytesOnlyWithOptIn() {
        SQLiteDatabase.create(null).use { db ->
            DownloadedFileArchive.create(db)
            val t0 = 1_790_000_000_000L
            CirqaRawNotificationArchive.archive(db, byteArrayOf(0x05, 0x01, 0x02), t0 - 1)          // before the window
            CirqaRawNotificationArchive.archive(db, byteArrayOf(0x07, 0x11), t0 + 1_000)
            CirqaRawNotificationArchive.archive(db, ByteArray(200) { 0x09 }, t0 + 2_000)
            DownloadedFileArchive.store(db, byteArrayOf(0x7f), byteArrayOf(1), "fit_file", t0 + 3_000) // not a notification

            val hidden = CommunicationTrace.read(db, t0, t0 + 10_000, withPayloads = false)
            assertEquals(2L, hidden.total)
            assertEquals(202L, hidden.totalBytes)
            assertEquals(listOf(0x07, 0x09), hidden.entries.map { it.channel })
            hidden.entries.forEach { assertNull(it.payload) }

            val shown = CommunicationTrace.read(db, t0, t0 + 10_000, withPayloads = true)
            assertTrue(shown.entries[0].payload!!.contentEquals(byteArrayOf(0x07, 0x11)))
            assertEquals(BleSafetyPolicy.maximumDiagnosticBytes, shown.entries[1].payload!!.size)
        }
    }
}
