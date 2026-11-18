package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CirqaRawDeviceMetadataTest {
    @Test fun parsesOnlyWhitelistedDeviceXmlMetadata() {
        val xml = """
            <Device>
              <Model><PartNumber>private-part</PartNumber><SoftwareVersion>1.2.3</SoftwareVersion></Model>
              <Id>private-identifier</Id>
              <MassStorageMode>true</MassStorageMode>
              <Extensions><DeviceExtension><TransferDirection>private-path</TransferDirection></DeviceExtension></Extensions>
            </Device>
        """.trimIndent()

        val result = CirqaRawDeviceMetadataParser.inspect(xml.toByteArray())

        assertEquals("1.2.3", result?.softwareVersion)
        assertEquals(true, result?.massStorageMode)
        assertEquals(1, result?.transferDirectionElementCount)
    }

    @Test fun emptyFieldsRemainUnknownWhileElementCountIsRecorded() {
        val xml = "<Device><Model><SoftwareVersion/></Model><MassStorageMode/>" +
            "<TransferDirection/><TransferDirection/></Device>"

        val result = CirqaRawDeviceMetadataParser.inspect(xml.toByteArray())

        assertNull(result?.softwareVersion)
        assertNull(result?.massStorageMode)
        assertEquals(2, result?.transferDirectionElementCount)
    }

    @Test fun rejectsMalformedXmlDoctypeAndNonDeviceRoot() {
        assertNull(CirqaRawDeviceMetadataParser.inspect("<Device>".toByteArray()))
        assertNull(
            CirqaRawDeviceMetadataParser.inspect(
                """<!DOCTYPE Device [<!ENTITY x SYSTEM "file:///etc/passwd">]><Device>&x;</Device>""".toByteArray()
            )
        )
        assertNull(CirqaRawDeviceMetadataParser.inspect("<Other/>".toByteArray()))
    }

    @Test fun treatsOpaqueBinaryAsNotParsedXml() {
        assertNull(CirqaRawDeviceMetadataParser.inspect(byteArrayOf(0x00, 0xFF.toByte(), 0x10, 0x01)))
    }
}
