package de.pulselibre

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CirqaMlrConfigureResponseTest {
    @Test fun acceptsConnectSuccessfulThreeStatusBytes() {
        assertTrue(CirqaMlrConfigureResponse.isSuccessful(byteArrayOf(0, 0, 0)))
    }

    @Test fun rejectsMissingOrNonzeroStatuses() {
        assertFalse(CirqaMlrConfigureResponse.isSuccessful(byteArrayOf(0, 0)))
        assertFalse(CirqaMlrConfigureResponse.isSuccessful(byteArrayOf(0, 1, 0)))
        assertFalse(CirqaMlrConfigureResponse.isSuccessful(byteArrayOf(0, 0, 3)))
    }
}
