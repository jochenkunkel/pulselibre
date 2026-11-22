package de.pulselibre

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CirqaSyncResponseParserTest {
    @Test fun recognizesCoreServiceSyncResponse() {
        // Synthetic Smart.core_service(field 13) -> sync_response(field 2) with
        // a valid empty protobuf body; this is not asserted as the captured body.
        assertTrue(CirqaSyncResponseParser.isSyncResponse(byteArrayOf(0x6a, 0x02, 0x12, 0x00)))
    }

    @Test fun rejectsSyncRequestAndOtherSmartMessages() {
        assertFalse(CirqaSyncResponseParser.isSyncResponse(byteArrayOf(0x6a, 0x02, 0x0a, 0x00)))
        assertFalse(CirqaSyncResponseParser.isSyncResponse(byteArrayOf(0x22, 0x00)))
    }

    @Test fun rejectsMalformedAndAmbiguousResponses() {
        assertFalse(CirqaSyncResponseParser.isSyncResponse(byteArrayOf(0x6a, 0x03, 0x12, 0x02, 0x00)))
        assertFalse(
            CirqaSyncResponseParser.isSyncResponse(
                byteArrayOf(0x6a, 0x04, 0x12, 0x00, 0x12, 0x00)
            )
        )
    }
}
