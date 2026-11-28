package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BandActivityStoreTest {
    @Test fun reportValues() {
        assertEquals(3, BandActivityStore.fromReport("battery=60%,bandActivity=3,activityEnd=no,"))
        assertEquals(BandActivityStore.NONE, BandActivityStore.fromReport("x,bandActivity=subscribed,y"))
        assertNull(BandActivityStore.fromReport("x,bandActivity=none,y"))
        assertNull(BandActivityStore.fromReport("no field"))
    }

    @Test fun activeOnlyForUnfinishedAndFreshStates() {
        val now = 10_000_000_000L
        assertTrue(BandActivityStore.isActive(3, now - 60_000, now))
        assertTrue(BandActivityStore.isActive(1, now, now))
        assertTrue(BandActivityStore.isActive(5, now, now))
        assertFalse(BandActivityStore.isActive(4, now, now))
        assertFalse(BandActivityStore.isActive(BandActivityStore.NONE, now, now))
        assertFalse(BandActivityStore.isActive(null, now, now))
        assertFalse(BandActivityStore.isActive(3, now - BandActivityStore.MAX_AGE_MS - 1, now))
    }
}
