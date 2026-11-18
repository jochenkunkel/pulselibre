package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Test

class CirqaProtobufShapeInspectorTest {
    @Test fun reportsTopLevelFieldNumbersAndWireTypes() {
        // field 1 wire 2 (len 1), field 4 wire 0 (varint), field 9 wire 5 (fixed32)
        val bytes = byteArrayOf(
            0x0a, 0x01, 0x7f,
            0x20, 0x96.toByte(), 0x01,
            0x4d, 0x01, 0x02, 0x03, 0x04
        )
        assertEquals("1:2,4:0,9:5", CirqaProtobufShapeInspector.shape(bytes))
    }

    @Test fun toleratesTruncatedAndUnknownWireTypes() {
        assertEquals("", CirqaProtobufShapeInspector.shape(byteArrayOf()))
        assertEquals("", CirqaProtobufShapeInspector.shape(byteArrayOf(0x0b)))
        assertEquals("1:2", CirqaProtobufShapeInspector.shape(byteArrayOf(0x0a, 0x05, 0x01)))
    }
}
