package de.pulselibre

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.file.Files

/** #142/#159: provenance-backed storage and honest display of every observed band value. */
@RunWith(AndroidJUnit4::class)
class DeviceMetadataInstrumentedTest {
    private val context = AppLanguageSettings.wrap(ApplicationProvider.getApplicationContext<Context>())

    private fun all(v: View): List<View> =
        listOf(v) + ((v as? ViewGroup)?.let { g -> (0 until g.childCount).flatMap { all(g.getChildAt(it)) } } ?: emptyList())

    @Test fun storeKeepsNewestPerFieldAndIsIdempotent() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "live-metadata-store-").toFile()
        val ctx = isolatedContext(base, root)
        val now = 1_800_000_000_000L
        try {
            CirqaDatabase(ctx).use { db ->
                val first = LiveDeviceMetadata(
                    DeviceMetadataFields.BATTERY_LEVEL, "20", "%",
                    DeviceMetadataSources.GATT_BATTERY, now - 60_000
                )
                db.storeLiveDeviceMetadata(listOf(first), storedAtMs = now)
                db.storeLiveDeviceMetadata(listOf(first), storedAtMs = now + 1) // duplicate ignored
                db.storeLiveDeviceMetadata(listOf(
                    LiveDeviceMetadata(
                        DeviceMetadataFields.BATTERY_LEVEL, "80", "%",
                        DeviceMetadataSources.GARMIN_GDI_STATUS, now
                    ),
                    LiveDeviceMetadata(
                        DeviceMetadataFields.BATTERY_STATUS, "CHARGING", null,
                        DeviceMetadataSources.GARMIN_GDI_STATUS, now
                    ),
                    LiveDeviceMetadata(
                        DeviceMetadataFields.BATTERY_HOURS, "6", "h",
                        DeviceMetadataSources.GARMIN_GDI_STATUS, now
                    )
                ), storedAtMs = now + 2)
                val values = db.latestLiveDeviceMetadata()
                assertEquals(3, values.size)
                assertEquals("80", values.first { it.fieldKey == DeviceMetadataFields.BATTERY_LEVEL }.valueText)
                assertEquals(DeviceMetadataSources.GARMIN_GDI_STATUS,
                    values.first { it.fieldKey == DeviceMetadataFields.BATTERY_LEVEL }.source)
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun existingVersion8DatabaseGetsLiveMetadataTableWithoutDowngrade() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "foreign-v8-live-metadata-").toFile()
        val ctx = isolatedContext(base, root)
        val dbFile = File(root, "cirqa_measurements.db")
        try {
            SQLiteDatabase.openOrCreateDatabase(dbFile.absolutePath, null).use { legacy ->
                legacy.execSQL("CREATE TABLE events (id INTEGER PRIMARY KEY, line_sha256 TEXT UNIQUE, received_at_utc TEXT, dashboard_time TEXT, kind TEXT, service INTEGER, payload_json TEXT)")
                legacy.version = 8
            }
            CirqaDatabase(ctx).use { db ->
                assertEquals(11, db.readableDatabase.version)
                assertTrue(db.latestLiveDeviceMetadata().isEmpty())
                val columns = db.readableDatabase.rawQuery("PRAGMA table_info(live_device_metadata)", null).use { rows ->
                    val names = mutableListOf<String>()
                    while (rows.moveToNext()) names.add(rows.getString(1))
                    names
                }
                assertTrue(columns.containsAll(listOf(
                    "field_key", "value_text", "unit", "source", "observed_at_ms", "stored_at_ms"
                )))
                val candidateColumns = db.readableDatabase.rawQuery("PRAGMA table_info(fit_review_candidates)", null).use { rows ->
                    val names = mutableListOf<String>()
                    while (rows.moveToNext()) names.add(rows.getString(1))
                    names
                }
                assertTrue(candidateColumns.containsAll(listOf(
                    "sample_id", "metric", "value", "quality", "timestamp_origin"
                )))
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun everyObservedValueGetsItsOwnVisibleProvenanceRow() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val now = System.currentTimeMillis()
            val view = DeviceOverviewView(context)
            view.showMetadata(listOf(
                LiveDeviceMetadata(DeviceMetadataFields.BATTERY_LEVEL, "80", "%",
                    DeviceMetadataSources.GARMIN_GDI_STATUS, now),
                LiveDeviceMetadata(DeviceMetadataFields.BATTERY_STATUS, "CHARGING", null,
                    DeviceMetadataSources.GARMIN_GDI_STATUS, now),
                LiveDeviceMetadata(DeviceMetadataFields.BATTERY_HOURS, "6", "h",
                    DeviceMetadataSources.GARMIN_GDI_STATUS, now),
                LiveDeviceMetadata(DeviceMetadataFields.FIRMWARE_VERSION, "1.2.3", null,
                    DeviceMetadataSources.GATT_DEVICE_INFO, now),
                LiveDeviceMetadata(DeviceMetadataFields.SERIAL_NUMBER, "ABC123", null,
                    DeviceMetadataSources.GATT_DEVICE_INFO, now),
                LiveDeviceMetadata("gatt_2b00", "01ff", null, DeviceMetadataSources.GATT_OTHER, now)
            ))
            assertNotNull(view.findViewWithTag<View>("device_battery_row"))
            listOf(
                DeviceMetadataFields.BATTERY_STATUS, DeviceMetadataFields.BATTERY_HOURS,
                DeviceMetadataFields.FIRMWARE_VERSION, DeviceMetadataFields.SERIAL_NUMBER,
                "gatt_2b00"
            ).forEach { key ->
                val row = view.findViewWithTag<View>("device_metadata_$key")
                assertNotNull("missing visible row for $key", row)
                val texts = all(row!!).filterIsInstance<TextView>().map { it.text.toString() }
                assertTrue("no value shown for $key: $texts", texts.any { it.isNotBlank() })
                assertTrue("no provenance in content description for $key",
                    row.contentDescription.toString().contains("Quelle"))
            }
            val serialRow = view.findViewWithTag<View>("device_metadata_${DeviceMetadataFields.SERIAL_NUMBER}")!!
            assertTrue(all(serialRow).filterIsInstance<TextView>().none { it.text.toString() == "ABC123" })
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
