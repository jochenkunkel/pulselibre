package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Tiles and day dialogs must never wait behind the minutes-long FIT re-indexing again (S10, build 2171). */
class DashboardWorkerWatchTest {
    @Test fun dashboardWorkRunsWhileMaintenanceIsBusy() {
        val dashboard = DashboardWorkerWatch.newDashboardWorker()
        val maintenance = DashboardWorkerWatch.newMaintenanceWorker()
        val release = CountDownLatch(1)
        val done = CountDownLatch(1)
        try {
            maintenance.execute { release.await(10, TimeUnit.SECONDS) } // a long FIT backfill
            dashboard.execute { done.countDown() }                      // a day dialog load
            assertTrue("dashboard task waited behind maintenance", done.await(2, TimeUnit.SECONDS))
        } finally {
            release.countDown(); dashboard.shutdownNow(); maintenance.shutdownNow()
        }
    }

    @Test fun workerThreadsAreNamedForDiagnosis() {
        val names = java.util.Collections.synchronizedList(ArrayList<String>())
        val dashboard = DashboardWorkerWatch.newDashboardWorker()
        val maintenance = DashboardWorkerWatch.newMaintenanceWorker()
        val latch = CountDownLatch(2)
        dashboard.execute { names += Thread.currentThread().name; latch.countDown() }
        maintenance.execute { names += Thread.currentThread().name; latch.countDown() }
        latch.await(2, TimeUnit.SECONDS)
        dashboard.shutdownNow(); maintenance.shutdownNow()
        assertEquals(setOf(DashboardWorkerWatch.THREAD_NAME, DashboardWorkerWatch.MAINTENANCE_THREAD_NAME), names.toSet())
    }

    @Test fun stallReportShowsOnlyAppWorkersWithTheirFrames() {
        val worker = Thread({}, DashboardWorkerWatch.THREAD_NAME)
        val other = Thread({}, "main")
        val frame = StackTraceElement("de.pulselibre.UnifiedMeasurements", "refresh", "UnifiedMeasurements.kt", 120)
        val text = DashboardWorkerWatch.describe(mapOf(worker to arrayOf(frame), other to arrayOf(frame)))
        assertTrue(text, text.startsWith("${DashboardWorkerWatch.THREAD_NAME} "))
        assertTrue(text, text.contains("at de.pulselibre.UnifiedMeasurements.refresh(UnifiedMeasurements.kt:120)"))
        assertFalse(text, text.contains("main "))
    }

    @Test fun fitReindexingIsNotScheduledOnTheDashboardWorker() {
        val src = listOf(File("src/main/java/de/pulselibre"), File("app/src/main/java/de/pulselibre")).first { it.isDirectory }
        val main = File(src, "MainActivity.kt").readText()
        val scan = main.substringAfter("private fun scheduleUnknownFitInventoryScan()").substringBefore("override fun onResume")
        assertTrue(scan.contains("DashboardWorkerWatch.runMaintenanceOnce"))
        assertFalse(scan.contains("dashboardWorker"))
    }

    @Test fun onlyOneMaintenanceRunAtATimePerProcess() {
        val release = CountDownLatch(1)
        val started = CountDownLatch(1)
        val ran = java.util.concurrent.atomic.AtomicInteger()
        assertTrue(DashboardWorkerWatch.runMaintenanceOnce { ran.incrementAndGet(); started.countDown(); release.await(5, TimeUnit.SECONDS) })
        assertTrue(started.await(2, TimeUnit.SECONDS))
        // A second activity instance while the first scan runs: skipped, not queued behind it.
        assertFalse(DashboardWorkerWatch.runMaintenanceOnce { ran.incrementAndGet() })
        release.countDown()
        // After the first finished, a new request is accepted again.
        val second = CountDownLatch(1)
        var accepted = false
        repeat(50) { if (!accepted) { accepted = DashboardWorkerWatch.runMaintenanceOnce { ran.incrementAndGet(); second.countDown() }; if (!accepted) Thread.sleep(20) } }
        assertTrue(accepted); assertTrue(second.await(2, TimeUnit.SECONDS))
        assertEquals(2, ran.get())
    }
}
