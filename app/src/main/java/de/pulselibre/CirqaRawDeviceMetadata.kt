package de.pulselibre

import android.util.Xml
import java.io.ByteArrayInputStream
import org.xmlpull.v1.XmlPullParser
import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory

/** Whitelisted metadata derived from one verified raw XML original. */
data class RawDeviceMetadataRecord(
    val sourceSha256: String,
    val fileBytes: Long,
    /** Local processing time; this is not a device-observed timestamp. */
    val verifiedAtEpochMillis: Long,
    val parserVersion: String,
    val softwareVersion: String?,
    val massStorageMode: Boolean?,
    val transferDirectionElementCount: Int
)

/** Extracts only non-identifying XML metadata; file names, paths and identifiers are ignored. */
internal object CirqaRawDeviceMetadataParser {
    const val VERSION = "1"
    private const val MAX_TEXT_LENGTH = 128
    private const val MAX_TRANSFER_DIRECTIONS = 10_000
    private const val MAX_XML_DEPTH = 64

    fun inspect(bytes: ByteArray): RawDeviceMetadataFields? {
        // Android's pull parser may silently skip a DOCTYPE when processing is
        // disabled. Reject its ASCII markup first (also recognizing UTF-16 null
        // interleaving) so no platform can accept a document containing a DTD.
        if (containsDoctypeMarkup(bytes)) return null
        return try {
            val parser = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(ByteArrayInputStream(bytes), null)
            }
            parseDeviceXml(parser)
        } catch (_: RuntimeException) {
            // Local JVM unit tests run against Android API stubs. Fall back to
            // the hardened JAXP parser there; Android uses its platform pull parser.
            inspectWithSax(bytes)
        } catch (_: Exception) {
            null
        }
    }

    private fun containsDoctypeMarkup(bytes: ByteArray): Boolean {
        val markup = bytes.filter { it != 0.toByte() }.toByteArray().toString(Charsets.US_ASCII)
        return "<!DOCTYPE" in markup
    }

    private fun inspectWithSax(bytes: ByteArray): RawDeviceMetadataFields? {
        val handler = DeviceXmlHandler()
        return try {
            val factory = SAXParserFactory.newInstance().apply {
                isNamespaceAware = false
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
            if (depth > MAX_XML_DEPTH) throw SAXException("XML nesting limit exceeded")
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
                    massStorageMode = parseBoolean(text.toString())
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

        fun result(): RawDeviceMetadataFields? = if (!rootIsDevice) null else
            RawDeviceMetadataFields(softwareVersion, massStorageMode, transferDirectionCount)
    }

    private fun parseDeviceXml(parser: XmlPullParser): RawDeviceMetadataFields? {
        var depth = 0
        var rootIsDevice = false
        var insideModel = false
        var activeCapture: String? = null
        var activeTag: String? = null
        var activeDepth = -1
        var activeInvalid = false
        val text = StringBuilder()
        var softwareVersion: String? = null
        var massStorageMode: Boolean? = null
        var transferDirectionCount = 0

        while (true) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> {
                    depth++
                    if (depth > MAX_XML_DEPTH) return null
                    val name = parser.name.substringAfter(':')
                    if (depth == 1) {
                        rootIsDevice = name == "Device"
                        continue
                    }
                    if (activeCapture != null) activeInvalid = true
                    if (!rootIsDevice) continue
                    if (depth == 2 && name == "Model") insideModel = true
                    if (insideModel && depth == 3 && name == "SoftwareVersion") {
                        activeCapture = "software"
                        activeTag = name
                        activeDepth = depth
                        activeInvalid = false
                        text.setLength(0)
                    }
                    if (depth == 2 && name == "MassStorageMode") {
                        activeCapture = "mass-storage"
                        activeTag = name
                        activeDepth = depth
                        activeInvalid = false
                        text.setLength(0)
                    }
                    if (name == "TransferDirection") {
                        if (transferDirectionCount >= MAX_TRANSFER_DIRECTIONS) return null
                        transferDirectionCount++
                    }
                }
                XmlPullParser.TEXT, XmlPullParser.CDSECT -> {
                    if (activeCapture != null) {
                        val value = parser.text ?: ""
                        if (text.length + value.length > MAX_TEXT_LENGTH) {
                            activeInvalid = true
                        } else if (!activeInvalid) {
                            text.append(value)
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val name = parser.name.substringAfter(':')
                    if (activeCapture != null && depth == activeDepth && name == activeTag) {
                        if (!activeInvalid) {
                            when (activeCapture) {
                                "software" -> softwareVersion = text.toString().trim()
                                    .takeIf { it.matches(Regex("[A-Za-z0-9._+-]{1,128}")) }
                                "mass-storage" -> massStorageMode = parseBoolean(text.toString())
                            }
                        }
                        activeCapture = null
                        activeTag = null
                        activeDepth = -1
                        activeInvalid = false
                        text.setLength(0)
                    }
                    if (depth == 2 && name == "Model") insideModel = false
                    depth--
                }
                XmlPullParser.DOCDECL, XmlPullParser.ENTITY_REF -> return null
                XmlPullParser.END_DOCUMENT -> {
                    if (depth != 0) return null
                    break
                }
            }
        }
        if (!rootIsDevice) return null
        return RawDeviceMetadataFields(softwareVersion, massStorageMode, transferDirectionCount)
    }

    private fun parseBoolean(value: String): Boolean? = when (value.trim().lowercase()) {
        "true", "1" -> true
        "false", "0" -> false
        else -> null
    }
}

internal data class RawDeviceMetadataFields(
    val softwareVersion: String?,
    val massStorageMode: Boolean?,
    val transferDirectionElementCount: Int
)
