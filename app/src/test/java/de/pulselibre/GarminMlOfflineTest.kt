package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class GarminMlOfflineTest {
    @Test fun usesConnectCounterpartWhenPresentAndIgnoresCase() {
        val rx = "6A4E2810-667B-11E3-949A-0800200C9A66"
        val tx = "6a4e2820-667b-11e3-949a-0800200c9a66"
        assertNull(GarminMlOffline.findCharacteristicPair(listOf(tx)))
        assertEquals(tx, GarminMlOffline.findCharacteristicPair(listOf(rx, tx))?.tx)
    }

    @Test fun supportsWholeConnectMlRangeAndSameCharacteristicFallback() {
        val rx = "6a4e2819-667b-11e3-949a-0800200c9a66"
        val candidates = GarminMlOffline.characteristicPairCandidates(listOf(rx))
        assertEquals(1, candidates.size)
        assertEquals(rx, candidates.single().rx)
        assertEquals(rx, candidates.single().tx)

        val tx = "6a4e2829-667b-11e3-949a-0800200c9a66"
        assertEquals(tx, GarminMlOffline.findCharacteristicPair(listOf(rx, tx))?.tx)
    }

    @Test fun returnsAllDiscoveredCandidatesInStableOrder() {
        val later = "6a4e2819-667b-11e3-949a-0800200c9a66"
        val earlier = "6a4e2815-667b-11e3-949a-0800200c9a66"
        val candidates = GarminMlOffline.characteristicPairCandidates(listOf(later, earlier))
        assertEquals(listOf(earlier, later), candidates.map { it.rx })
        assertEquals(listOf(earlier, later), candidates.map { it.tx })
    }

    @Test fun neverMatchesUnrelatedAdvertisingCandidate() {
        assertTrue(GarminMlOffline.characteristicPairCandidates(
            listOf("3e1d50cd-7e3e-427d-8e1c-b78aa87fe624")).isEmpty())
    }

    @Test fun fallsBackToSameCharacteristicWhenCounterpartIsAbsent() {
        val rx = "6a4e2811-667b-11e3-949a-0800200c9a66"
        val wrongTx = "6a4e2820-667b-11e3-949a-0800200c9a66"
        assertEquals(rx, GarminMlOffline.findCharacteristicPair(listOf(rx, wrongTx))?.tx)
        val tx = "6a4e2821-667b-11e3-949a-0800200c9a66"
        assertEquals(rx, GarminMlOffline.findCharacteristicPair(listOf(rx, wrongTx, tx))?.rx)
    }

    @Test fun decodesRegistrationResponseAndRefusal() {
        val response = byteArrayOf(0, 1, 2, 0, 0, 0, 0, 0, 0, 0, 4, 0, 0, 3)
        val parsed = GarminMlOffline.inspectRegisterResponse(response)!!
        assertEquals(4, parsed.serviceId)
        assertEquals(3, parsed.handle)
        assertTrue(parsed.accepted)
        assertEquals(4, GarminMlOffline.inspectRegisterResponse(response + byteArrayOf(1))?.serviceId)
        assertEquals(4, GarminMlOffline.inspectRegisterResponse(response + byteArrayOf(1, 0))?.serviceId)
        assertNull(GarminMlOffline.inspectRegisterResponse(response + ByteArray(51)))
        val wrongClient = response.clone()
        wrongClient[2] = 3
        assertNull(GarminMlOffline.inspectRegisterResponse(wrongClient))
        response[12] = 3
        assertFalse(GarminMlOffline.inspectRegisterResponse(response)!!.accepted)
    }

    @Test fun rejectsMalformedAndControlPackets() {
        assertNull(GarminMlOffline.inspectRegisterResponse(byteArrayOf()))
        assertNull(GarminMlOffline.inspectRegisterResponse(ByteArray(14)))
        assertNull(GarminMlOffline.inspectDataHandle(byteArrayOf(0, 1)))
        assertNull(GarminMlOffline.inspectDataHandle(byteArrayOf(0x81.toByte())))
    }

    @Test fun parsesFlaggedAndBareHandles() {
        assertEquals(3, GarminMlOffline.inspectDataHandle(byteArrayOf(0xb0.toByte(), 42)))
        assertEquals(9, GarminMlOffline.inspectDataHandle(byteArrayOf(9, 42)))
    }
}
