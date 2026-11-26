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

/**
 * Backup 25.11.2026: device_info rows of a sub-device (type 10, no manufacturer/product) report software "1" next to
 * the band's 4.2 (product 5019). The Firmware row must show the band's version even when the sub-device row is newer.
 */
@RunWith(AndroidJUnit4::class)
class FitSoftwareVersionTest {
    @Test fun firmwareComesFromTheBandNotFromANewerSubDevice() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "fit-sw-").toFile()
        val context = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = root
            override fun getDatabasePath(name: String): File = File(root, name)
            override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?): SQLiteDatabase =
                openOrCreateDatabase(name, mode, factory, null)
            override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?,
                                              errorHandler: DatabaseErrorHandler?): SQLiteDatabase =
                SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).absolutePath, factory, errorHandler)
        }
        try {
            val sha = "e".repeat(64)
            fun meta(ordinal: Int, at: Long, type: Int?, product: Int?, sw: Double) = FitHistoricalObservations.DeviceMetadata(
                sha, ordinal, at, type, if (product != null) 1 else null, product, sw, null, null, null)
            CirqaDatabase(context).use { db ->
                db.ensureHistoricalStoreCopied()
                db.commitFitFile(
                    sourceSha256 = sha, fileBytes = 100, relativePath = "verified_fit/$sha.fit", validatedAtEpochMillis = 1_000,
                    decoderVersion = FitHistoricalObservations.DECODER_VERSION, profileVersion = FitHistoricalObservations.PROFILE_VERSION,
                    observations = emptyList(),
                    deviceMetadata = listOf(meta(1, 1_790_000_000_000L, null, 5019, 4.2), meta(2, 1_790_000_060_000L, 10, null, 1.0)),
                    reviewCandidates = emptyList()
                )
                val fw = db.latestFitSoftwareVersion()!!
                assertEquals("4.2", fw.valueText)
                assertEquals(1_790_000_000_000L, fw.observedAtMs)
            }
        } finally { root.deleteRecursively() }
    }
}
