package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Test

class CirqaLegacy5007ResponseObserverTest {
    @Test fun classifiesOnlyCrcValidResponseToThe5007Request() {
        val observer = CirqaLegacy5007ResponseObserver()
        val frame = GarminFileListRequest.cobs(CirqaOfflineGfdiStatusCandidate.ackFrame(5007))
        assertEquals(
            CirqaLegacy5007ResponseObserver.Result.SUPPORTED,
            observer.observeMlPayload(frame)
        )
        assertEquals(5000, observer.responseType)
        assertEquals(0, observer.responseStatus)
    }

    @Test fun handlesFragmentedNotificationsAndNegativeStatus() {
        val observer = CirqaLegacy5007ResponseObserver()
        val frame = GarminFileListRequest.cobs(CirqaOfflineGfdiStatusCandidate.ackFrame(5007).let {
            val changed = it.clone()
            changed[6] = 1
            val prefix = changed.copyOfRange(0, changed.size - 2)
            val crc = GarminFrameValidator.crc16Arc(prefix)
            prefix + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
        })
        assertEquals(
            CirqaLegacy5007ResponseObserver.Result.PENDING,
            observer.observeMlPayload(frame.copyOfRange(0, 3))
        )
        assertEquals(
            CirqaLegacy5007ResponseObserver.Result.NOT_SUPPORTED,
            observer.observeMlPayload(frame.copyOfRange(3, frame.size))
        )
        assertEquals(1, observer.responseStatus)
    }

    @Test fun rejectsCorruptAndUnrelatedResponses() {
        val badCrcObserver = CirqaLegacy5007ResponseObserver()
        val corrupt = CirqaOfflineGfdiStatusCandidate.ackFrame(5007).also {
            it[it.lastIndex] = (it.last().toInt() xor 1).toByte()
        }
        assertEquals(
            CirqaLegacy5007ResponseObserver.Result.MALFORMED,
            badCrcObserver.observeMlPayload(GarminFileListRequest.cobs(corrupt))
        )

        val unrelatedObserver = CirqaLegacy5007ResponseObserver()
        val unrelated = GarminFileListRequest.cobs(CirqaOfflineGfdiStatusCandidate.ackFrame(5031))
        assertEquals(
            CirqaLegacy5007ResponseObserver.Result.UNEXPECTED,
            unrelatedObserver.observeMlPayload(unrelated)
        )
    }

    @Test fun abortsWhenMoreThanOneFrameArrivesTogether() {
        val observer = CirqaLegacy5007ResponseObserver()
        val one = GarminFileListRequest.cobs(CirqaOfflineGfdiStatusCandidate.ackFrame(5007))
        val two = GarminFileListRequest.cobs(CirqaOfflineGfdiStatusCandidate.ackFrame(5007))
        assertEquals(
            CirqaLegacy5007ResponseObserver.Result.UNEXPECTED,
            observer.observeMlPayload(one + two.drop(1).toByteArray())
        )
    }
}
