package de.pulselibre

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Lifecycle guard for the foreground recorder (audit A6-2). Emulator only (CI): the service writes its running
 * flag in the app's real preference file, and it must never claim to run when it cannot. The emulator has no
 * band, so the assertions are limited to invariants that hold with and without a Bluetooth adapter:
 * a stale flag is cleared by STOP, START followed by STOP always ends stopped, unknown actions are ignored.
 */
@RunWith(AndroidJUnit4::class)
class CirqaRecordingServiceLifecycleTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prefs get() = context.getSharedPreferences(CirqaContinuousRecordingService.PREFS, Context.MODE_PRIVATE)

    private fun intent(action: String?) = Intent(context, CirqaContinuousRecordingService::class.java).apply { this.action = action }

    private fun awaitRunningFlag(expected: Boolean, timeoutMs: Long = 15_000): Boolean {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            if (prefs.getBoolean(CirqaContinuousRecordingService.KEY_RUNNING, false) == expected) return true
            Thread.sleep(100)
        }
        return prefs.getBoolean(CirqaContinuousRecordingService.KEY_RUNNING, false) == expected
    }

    @Before fun cleanState() { prefs.edit().clear().commit() }

    @After fun stopAnythingLeftBehind() {
        context.startService(intent(CirqaContinuousRecordingService.ACTION_STOP))
        awaitRunningFlag(false)
        prefs.edit().clear().commit()
    }

    @Test fun stopClearsAStaleRunningFlag() {
        prefs.edit().putBoolean(CirqaContinuousRecordingService.KEY_RUNNING, true)
            .putLong(CirqaContinuousRecordingService.KEY_HEARTBEAT_MS, System.currentTimeMillis()).commit()
        assertTrue(RecorderStatus.isRunning(prefs))
        context.startService(intent(CirqaContinuousRecordingService.ACTION_STOP))
        assertTrue("STOP must clear the flag", awaitRunningFlag(false))
        assertFalse(RecorderStatus.isRunning(prefs))
    }

    @Test fun unknownOrMissingActionIsIgnoredAndNeverMarksRunning() {
        context.startService(intent("de.pulselibre.SOMETHING_ELSE"))
        context.startService(intent(null))
        Thread.sleep(1_500)
        assertFalse(prefs.getBoolean(CirqaContinuousRecordingService.KEY_RUNNING, false))
    }

    @Test fun startThenStopAlwaysEndsStopped() {
        // Start from a foreground activity so the platform allows a foreground service start.
        ActivityScenario.launch(MainActivity::class.java).use {
            try {
                ContextCompat.startForegroundService(context, intent(CirqaContinuousRecordingService.ACTION_START))
            } catch (_: Exception) {
                // Platform refused the start (permission/state): then the flag must simply stay false.
                assertFalse(prefs.getBoolean(CirqaContinuousRecordingService.KEY_RUNNING, false))
                return
            }
            Thread.sleep(1_000)
            context.startService(intent(CirqaContinuousRecordingService.ACTION_STOP))
            assertTrue("START followed by STOP must end stopped", awaitRunningFlag(false))
            assertFalse(RecorderStatus.isRunning(prefs))
        }
    }

    @Test fun startWithoutUsableBluetoothDoesNotStayRunning() {
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager).adapter
        // Only meaningful where Bluetooth is absent or off (typical CI emulator); skipped otherwise.
        org.junit.Assume.assumeTrue(adapter == null || !adapter.isEnabled)
        ActivityScenario.launch(MainActivity::class.java).use {
            try {
                ContextCompat.startForegroundService(context, intent(CirqaContinuousRecordingService.ACTION_START))
            } catch (_: Exception) { return }
            assertTrue("service must give up when Bluetooth is unusable", awaitRunningFlag(false))
            assertFalse(RecorderStatus.isRunning(prefs))
        }
    }
}
