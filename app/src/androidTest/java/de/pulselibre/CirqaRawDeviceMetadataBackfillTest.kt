package de.pulselibre

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CirqaRawDeviceMetadataBackfillTest {
    @Test fun platformParserRejectsDoctypeEntitiesAndMalformedXml() {
        val dtd = """<!DOCTYPE Device [<!ENTITY x SYSTEM "file:///etc/passwd">]>
            <Device><Model><SoftwareVersion>&x;</SoftwareVersion></Model></Device>"""
        assertEquals(null, CirqaRawDeviceMetadataParser.inspect(dtd.toByteArray()))
        assertEquals(null, CirqaRawDeviceMetadataParser.inspect("<Device>".toByteArray()))
    }

    @Test fun indexesVerifiedDeviceXmlOfflineAndIsIdempotentWithoutExposingSourceNames() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "raw-metadata-backfill-").toFile()
        val raw = File(root, "verified_raw").also { assertTrue(it.mkdir()) }
        val context = isolatedContext(base, root)
        val xml = """<Device><Model><SoftwareVersion>2.4.1</SoftwareVersion></Model>
            <MassStorageMode>false</MassStorageMode><TransferDirection/><TransferDirection/></Device>"""
            .toByteArray()
        assertNotNull(CirqaRawDeviceMetadataParser.inspect(xml))
        val opaque = byteArrayOf(0, 1, 0x7f, 0x55)
        writeContentAddressed(raw, xml)
        writeContentAddressed(raw, opaque)
        try {
            var now = 123_000L
            val backfill = CirqaRawDeviceMetadataBackfill(raw, context) { now }
            val first = backfill.run()
            assertEquals(2, first.filesSeen)
            assertEquals(1, first.metadataIndexed)
            assertEquals(1, first.unrecognizedOrInvalid)
            assertEquals(1, first.files.count {
                it.status == CirqaRawDeviceMetadataBackfill.Status.INDEXED_DEVICE_XML
            })
            assertEquals(1, first.files.count {
                it.status == CirqaRawDeviceMetadataBackfill.Status.UNRECOGNIZED_OR_INVALID
            })
            assertTrue(first.files.all { it.alias.startsWith("raw-") })

            now += 10_000L
            val repeated = backfill.run()
            assertEquals(first, repeated)
            CirqaDatabase(context).use { database ->
                assertEquals(1, database.rawDeviceMetadata().size)
                val record = database.rawDeviceMetadata().single()
                assertEquals(123_000L, record.verifiedAtEpochMillis)
                assertEquals("2.4.1", record.softwareVersion)
                assertEquals(false, record.massStorageMode)
                assertEquals(2, record.transferDirectionElementCount)
                assertEquals(xml.size.toLong(), record.fileBytes)
                assertEquals(sha256(xml), record.sourceSha256)
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun rejectsContentAddressMismatchWithoutPersistingMetadata() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "raw-metadata-invalid-").toFile()
        val raw = File(root, "verified_raw").also { assertTrue(it.mkdir()) }
        val context = isolatedContext(base, root)
        try {
            File(raw, "${"0".repeat(64)}.bin").writeText(
                "<Device><Model><SoftwareVersion>1.0</SoftwareVersion></Model></Device>"
            )
            val result = CirqaRawDeviceMetadataBackfill(raw, context).run()
            assertEquals(1, result.unrecognizedOrInvalid)
            CirqaDatabase(context).use { assertTrue(it.rawDeviceMetadata().isEmpty()) }
        } finally {
            root.deleteRecursively()
        }
    }

    private fun writeContentAddressed(directory: File, bytes: ByteArray) {
        File(directory, "${sha256(bytes)}.bin").writeBytes(bytes)
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun isolatedContext(base: Context, root: File): Context = object : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getDatabasePath(name: String): File =
            File(root, name).also { it.parentFile?.mkdirs() }

        override fun openOrCreateDatabase(
            name: String,
            mode: Int,
            factory: SQLiteDatabase.CursorFactory?
        ): SQLiteDatabase = openOrCreateDatabase(name, mode, factory, null)

        override fun openOrCreateDatabase(
            name: String,
            mode: Int,
            factory: SQLiteDatabase.CursorFactory?,
            errorHandler: DatabaseErrorHandler?
        ): SQLiteDatabase =
            SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).absolutePath, factory, errorHandler)
    }
}
