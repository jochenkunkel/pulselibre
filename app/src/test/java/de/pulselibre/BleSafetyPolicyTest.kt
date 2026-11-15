package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class BleSafetyPolicyTest {
    @Test fun neverWritesOrSubscribesBeforeProtocolApproval() {
        assertFalse(BleSafetyPolicy.mayWriteWithoutProtocolApproval())
        assertFalse(BleSafetyPolicy.maySubscribeWithoutProtocolApproval())
    }

    @Test fun readPermissionOnlyForReadableCharacteristics() {
        assertTrue(BleSafetyPolicy.mayReadCharacteristic(0x02))
        assertTrue(BleSafetyPolicy.mayReadCharacteristic(0x12))
        assertFalse(BleSafetyPolicy.mayReadCharacteristic(0x08))
        assertFalse(BleSafetyPolicy.mayReadCharacteristic(0x10))
    }

    @Test fun diagnosticsBoundedAndUnsigned() {
        assertEquals("00 7F 80 FF", BleSafetyPolicy.safeHex(byteArrayOf(0, 127, -128, -1)))
        val large = ByteArray(100) { 0x7f }
        assertTrue(BleSafetyPolicy.safeHex(large).endsWith("(100 Bytes)"))
        assertEquals(64, BleSafetyPolicy.safeHex(large).substringBefore(" …").split(" ").size)
    }
}
