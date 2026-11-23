package de.pulselibre

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject

/**
 * #529 Teil B1: simulates a connection that stays open but never delivers a reading. Emulator only; no Bluetooth,
 * no device. The service seams replace the band link and shorten the stall window.
 */
@RunWith(AndroidJUnit4::class)
class CirqaRecorderStallSimulationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    /** API 34+: a connectedDevice foreground service only starts with a Bluetooth runtime permission. */
    private fun grantPermissions() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        listOf(android.Manifest.permission.BLUETOOTH_CONNECT, android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.POST_NOTIFICATIONS).forEach {
            try { automation.grantRuntimePermission(context.packageName, it) } catch (_: Exception) { }
        }
    }

    private class FakeLink(private val readingEveryMs: Long?) : RecorderLink {
        @Volatile var closed = false
        override var onReading: ((Int, GarminRealtime.Reading) -> Unit)? = null
        private var thread: Thread? = null
        override fun startContinuousAllMetrics(result: (String) -> Unit) {
            result("ML-Test: Verbinde")
            val every = readingEveryMs ?: return
            thread = Thread {
                try {
                    while (!closed) { onReading?.invoke(0x2A37, GarminRealtime.Reading.HeartRate(70, null)); Thread.sleep(every) }
                } catch (_: InterruptedException) { }
            }.also { it.isDaemon = true; it.start() }
        }
        override fun close() { closed = true; thread?.interrupt() }
    }

    private val links = ArrayList<FakeLink>()

    private fun start() = ContextCompat.startForegroundService(context,
        Intent(context, CirqaContinuousRecordingService::class.java).apply { action = CirqaContinuousRecordingService.ACTION_START })

    private fun eventTypes(): List<JSONObject> = CirqaDatabase(context).use { db -> db.allLines().map { JSONObject(it) } }

    private fun await(timeoutMs: Long, cond: () -> Boolean): Boolean {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) { if (cond()) return true; Thread.sleep(200) }
        return cond()
    }

    @Before fun setUp() {
        grantPermissions()
        context.getSharedPreferences(CirqaContinuousRecordingService.PREFS, Context.MODE_PRIVATE).edit().clear().commit()
        CirqaDatabase(context).use { it.writableDatabase.execSQL("DELETE FROM events") }
        CirqaContinuousRecordingService.stallAfterMs = 3_000
        CirqaContinuousRecordingService.heartbeatIntervalMs = 500
    }

    @After fun tearDown() {
        context.startService(Intent(context, CirqaContinuousRecordingService::class.java).apply { action = CirqaContinuousRecordingService.ACTION_STOP })
        Thread.sleep(1_000)
        CirqaContinuousRecordingService.linkFactory = null
        CirqaContinuousRecordingService.stallAfterMs = RecorderStall.STALL_AFTER_MS
        CirqaContinuousRecordingService.heartbeatIntervalMs = RecorderStatus.HEARTBEAT_INTERVAL_MS
    }

    @Test fun silentConnectionIsDetectedClosedAndReconnected() {
        CirqaContinuousRecordingService.linkFactory = { FakeLink(null).also { links += it } }
        start()
        assertTrue("stall must be detected", await(20_000) { eventTypes().any { it.optString("type") == "stall_detected" } })
        assertTrue("reconnect after stall", await(20_000) {
            eventTypes().any { it.optString("type") == "reconnect_scheduled" && it.optString("reason") == "stall" }
        })
        assertTrue("second attempt", await(20_000) { eventTypes().count { it.optString("type") == "attempt_start" } >= 2 })
        assertTrue("first link closed", links.first().closed)
    }

    @Test fun connectionWithReadingsIsNeverRecycled() {
        CirqaContinuousRecordingService.linkFactory = { FakeLink(500).also { links += it } }
        start()
        assertTrue("readings stored", await(15_000) { eventTypes().any { it.optString("type") == "reading" } })
        Thread.sleep(8_000) // > 2x the shortened stall window
        val types = eventTypes().map { it.optString("type") }
        assertEquals("no stall while readings flow", 0, types.count { it == "stall_detected" })
        assertEquals("single attempt", 1, types.count { it == "attempt_start" })
    }
}
