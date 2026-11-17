package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaLegacyDirectoryObserverTest {
    @Test fun defaultsToNoObservedMessages() {
        val observer = CirqaLegacyDirectoryObserver()
        assertEquals(0, observer.count(5007))
        assertEquals(0, observer.count(5004))
        assertTrue(observer.summary().contains("Payloads/Dateinamen=unterdrückt"))
    }

    @Test fun invalidFramesDoNotCount() {
        val observer = CirqaLegacyDirectoryObserver()
        observer.observeVerified(byteArrayOf(0, 0, 0x8f.toByte(), 0x13, 0, 0))
        observer.observeVerified(byteArrayOf())
        assertEquals(0, observer.count(5007))
    }
}
