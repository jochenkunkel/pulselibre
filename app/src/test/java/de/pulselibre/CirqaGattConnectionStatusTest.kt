package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CirqaGattConnectionStatusTest {
    @Test fun identifiesAndroidConnectionTimeout() {
        assertEquals(
            "GATT_CONNECTION_TIMEOUT (remote may be out of range or not advertising as connectable)",
            CirqaGattConnectionStatus.description(147)
        )
    }

    @Test fun preservesSuccessAndUnknownStatuses() {
        assertEquals("GATT_SUCCESS", CirqaGattConnectionStatus.description(0))
        assertNull(CirqaGattConnectionStatus.description(133))
    }
}
