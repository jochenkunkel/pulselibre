package de.pulselibre

import android.content.Context
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.security.MessageDigest

/**
 * Re-indexes already stored, content-addressed Raw originals without BLE.
 *
 * The report intentionally exposes ordinal aliases only. A successful XML parse
 * is persisted with this pass's local verification time, not a claimed device
 * observation time.
 */
class CirqaRawDeviceMetadataBackfill(
    private val rawDirectory: File,
    private val context: Context,
    private val clockMillis: () -> Long = System::currentTimeMillis
) {
    enum class Status { INDEXED_DEVICE_XML, UNRECOGNIZED_OR_INVALID }

    data class FileResult(val alias: String, val status: Status)

    data class Result(
        val filesSeen: Int,
        val metadataIndexed: Int,
        val unrecognizedOrInvalid: Int,
        val files: List<FileResult>
    )

    fun run(): Result {
        require(rawDirectory.isDirectory && !Files.isSymbolicLink(rawDirectory.toPath())) {
            "Raw source directory must be a private, non-symlink directory"
        }
        val sources = rawDirectory.listFiles()
            ?.filter { it.name.endsWith(".bin") }
            ?.sortedBy { it.name }
            ?: error("Cannot list Raw source directory")

        var indexed = 0
        var unrecognized = 0
        val results = sources.mapIndexed { index, file ->
            val metadata = readVerifiedDeviceXml(file)
            if (metadata == null) {
                unrecognized++
                FileResult(alias(index), Status.UNRECOGNIZED_OR_INVALID)
            } else {
                CirqaDatabase(context).use { database ->
                    database.storeRawDeviceMetadata(
                        RawDeviceMetadataRecord(
                            sourceSha256 = metadata.sha256,
                            fileBytes = metadata.fileBytes,
                            verifiedAtEpochMillis = clockMillis(),
                            parserVersion = CirqaRawDeviceMetadataParser.VERSION,
                            softwareVersion = metadata.fields.softwareVersion,
                            massStorageMode = metadata.fields.massStorageMode,
                            transferDirectionElementCount = metadata.fields.transferDirectionElementCount
                        )
                    )
                }
                indexed++
                FileResult(alias(index), Status.INDEXED_DEVICE_XML)
            }
        }
        return Result(sources.size, indexed, unrecognized, results)
    }

    private data class VerifiedXml(
        val sha256: String,
        val fileBytes: Long,
        val fields: RawDeviceMetadataFields
    )

    private fun readVerifiedDeviceXml(file: File): VerifiedXml? {
        if (!file.isFile || Files.isSymbolicLink(file.toPath()) ||
            file.length() !in 1..CirqaVerifiedRawFileStore.MAX_FILE_BYTES) return null
        val bytes = try {
            file.readBytes()
        } catch (_: IOException) {
            return null
        }
        val sha256 = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        if (file.name != "$sha256.bin") return null
        val fields = CirqaRawDeviceMetadataParser.inspect(bytes) ?: return null
        return VerifiedXml(sha256, bytes.size.toLong(), fields)
    }

    private fun alias(index: Int): String = "raw-${(index + 1).toString().padStart(4, '0')}"
}
