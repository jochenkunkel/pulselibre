package de.pulselibre

import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler
import java.io.ByteArrayInputStream
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory

/** Extracts only non-identifying XML metadata; file names, paths and identifiers are ignored. */
internal object CirqaRawDeviceMetadataParser {
    private const val MAX_TEXT_LENGTH = 128
    private const val MAX_TRANSFER_DIRECTIONS = 10_000

    fun inspect(bytes: ByteArray): RawDeviceMetadataFields? {
        val handler = DeviceXmlHandler()
        return try {
            val factory = SAXParserFactory.newInstance().apply {
                isNamespaceAware = false
                isXIncludeAware = false
                setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            }
            val reader = factory.newSAXParser().xmlReader.apply {
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
                entityResolver = org.xml.sax.EntityResolver { _, _ ->
                    throw SAXException("External XML entities are not allowed")
                }
                contentHandler = handler
            }
            reader.parse(org.xml.sax.InputSource(ByteArrayInputStream(bytes)))
            handler.result()
        } catch (_: Exception) {
            null
        }
    }

    private class DeviceXmlHandler : DefaultHandler() {
        private var depth = 0
        private var rootIsDevice = false
        private var insideModel = false
        private var activeCapture: String? = null
        private val text = StringBuilder()
        private var softwareVersion: String? = null
        private var massStorageMode: Boolean? = null
        private var transferDirectionCount = 0

        override fun startElement(uri: String?, localName: String?, qName: String, attributes: Attributes?) {
            depth++
            val name = (if (!localName.isNullOrEmpty()) localName else qName).substringAfter(':')
            if (depth == 1) {
                rootIsDevice = name == "Device"
                return
            }
            if (!rootIsDevice) return
            if (depth == 2 && name == "Model") insideModel = true
            if (insideModel && depth == 3 && name == "SoftwareVersion") startCapture("software")
            if (depth == 2 && name == "MassStorageMode") startCapture("mass-storage")
            if (name == "TransferDirection") {
                if (transferDirectionCount >= MAX_TRANSFER_DIRECTIONS) {
                    throw SAXException("Too many transfer direction fields")
                }
                transferDirectionCount++
            }
            if (depth > 64) throw SAXException("XML nesting limit exceeded")
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            if (activeCapture == null) return
            if (text.length + length > MAX_TEXT_LENGTH) {
                activeCapture = null
                text.setLength(0)
                return
            }
            text.append(ch, start, length)
        }

        override fun endElement(uri: String?, localName: String?, qName: String) {
            val name = (if (!localName.isNullOrEmpty()) localName else qName).substringAfter(':')
            when (activeCapture) {
                "software" -> if (name == "SoftwareVersion") {
                    val value = text.toString().trim()
                    softwareVersion = value.takeIf { it.matches(Regex("[A-Za-z0-9._+-]{1,128}")) }
                    activeCapture = null
                }
                "mass-storage" -> if (name == "MassStorageMode") {
                    massStorageMode = when (text.toString().trim().lowercase()) {
                        "true", "1" -> true
                        "false", "0" -> false
                        else -> null
                    }
                    activeCapture = null
                }
            }
            if (depth == 2 && name == "Model") insideModel = false
            depth--
        }

        private fun startCapture(field: String) {
            activeCapture = field
            text.setLength(0)
        }

        fun result(): RawDeviceMetadataFields? {
            if (!rootIsDevice) return null
            return RawDeviceMetadataFields(
                softwareVersion = softwareVersion,
                massStorageMode = massStorageMode,
                transferDirectionElementCount = transferDirectionCount
            )
        }
    }
}

internal data class RawDeviceMetadataFields(
    val softwareVersion: String?,
    val massStorageMode: Boolean?,
    val transferDirectionElementCount: Int
)
