package de.pulselibre

import android.content.Context
import android.content.ContextWrapper
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.nio.file.Files
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CirqaHistoryReaderTest {
    @Test fun returnsOnlyValidStoredReadingsInsideRollingWindowInTimeOrder() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "history-reader-test-").toFile()
        val context = isolatedContext(base, root)
        val now = Instant.parse("2026-11-17T12:00:00Z")
        fun row(time: Instant, service: Int, field: String, value: Int, type: String = "reading") =
            """{"schema":1,"type":"$type","received_at_utc":"$time","service":$service,"$field":$value}"""
        try {
            CirqaDatabase(context).use { db ->
                listOf(
                    row(now, 6, "bpm", 71),
                    row(now.minusSeconds(60), 19, "spo2_percent", 97),
                    row(now.minusSeconds(3600), 6, "bpm", 0),
                    row(now.minusSeconds(3600), 6, "bpm", 75),
                    row(now.minusSeconds(HistoryRange.DAY.duration.seconds + 2), 6, "bpm", 80),
                    row(now.plusSeconds(2), 19, "spo2_percent", 98),
                    row(now, 7, "steps", 20),
                    row(now, 6, "bpm", 90, "diagnostic")
                ).forEach { assertTrue(db.insertLine(it)) }
            }
            val snapshot = CirqaHistoryReader(context).read(HistoryRange.DAY, now)
            assertEquals(now.minus(HistoryRange.DAY.duration), snapshot.start)
            assertEquals(now, snapshot.end)
            assertEquals(listOf(75, 71), snapshot.heartRate.map { it.value })
            assertEquals(listOf(now.minusSeconds(3600), now), snapshot.heartRate.map { it.receivedAt })
            assertEquals(listOf(97), snapshot.oxygen.map { it.value })
        } finally {
            root.deleteRecursively()
        }
    }

    private fun isolatedContext(base: Context, root: File) = object : ContextWrapper(base) {
        override fun getFilesDir(): File = root
        override fun getDatabasePath(name: String): File = File(root, name)
        override fun openOrCreateDatabase(name: String, mode: Int,
            factory: SQLiteDatabase.CursorFactory?): SQLiteDatabase =
            SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).absolutePath, factory)
    }
}
