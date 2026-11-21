package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Test

class CirqaSyncRunIdentityTest {
    @Test fun reportFieldUsesShortDeterministicTokenFromUuid() {
        assertEquals(
            "runId=1234567890ab",
            CirqaSyncRunIdentity.reportField("12345678-90ab-cdef-1234-567890abcdef")
        )
    }

    @Test fun reportFieldNormalizesUppercaseUuid() {
        assertEquals(
            "runId=abcdef012345",
            CirqaSyncRunIdentity.reportField("ABCDEF01-2345-6789-ABCD-EF0123456789")
        )
    }

    @Test fun reportFieldFailsClosedForMissingOrMalformedId() {
        assertEquals("runId=unavailable", CirqaSyncRunIdentity.reportField(null))
        assertEquals("runId=unavailable", CirqaSyncRunIdentity.reportField("not-a-uuid"))
    }
}
