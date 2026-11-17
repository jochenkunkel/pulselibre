package de.pulselibre

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.security.DigestInputStream
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val BACKUP_FORMAT = "de.pulselibre.local-database-backup"
private const val BACKUP_VERSION = 1
private const val MAX_ARCHIVE_BYTES = 128L * 1024L * 1024L
private const val MAX_ENTRY_BYTES = 120L * 1024L * 1024L
private const val MAX_LINE_BYTES = 2 * 1024 * 1024
private const val MAX_RECORDS = 1_000_000L
private const val MAX_MANIFEST_BYTES = 64 * 1024
private const val EVENTS_ENTRY = "cirqa_events.jsonl"
private const val READINGS_ENTRY = "legacy_readings.jsonl"
private const val CURSORS_ENTRY = "sync_cursors.jsonl"
private const val MANIFEST_ENTRY = "manifest.json"

data class DatabaseBackupReport(
    val createdAt: String,
    val events: Long,
    val legacyReadings: Long,
    val syncCursors: Long
)

/** A validated backup staged privately before any local database can be changed. */
class PreparedDatabaseRestore internal constructor(
    internal val directory: File,
    val createdAt: String,
    val events: Long,
    val legacyReadings: Long,
    val syncCursors: Long
) : Closeable {
    override fun close() { directory.deleteRecursively() }
}

/** Versioned, checksummed archive for both local SQLite stores. The archive is not encrypted. */
object DatabaseBackup {
    private val dataEntries = setOf(EVENTS_ENTRY, READINGS_ENTRY, CURSORS_ENTRY)

    /** Stream a consistent snapshot into a temporary archive, then copy it to the chosen SAF file. */
    fun write(context: Context, destination: OutputStream): DatabaseBackupReport {
        val temp = File.createTempFile("pulselibre-backup-", ".zip", context.cacheDir)
        try {
            val report = writeArchive(context, temp)
            BufferedInputStream(FileInputStream(temp)).use { source ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var copied = 0L
                while (true) {
                    val count = source.read(buffer)
                    if (count < 0) break
                    copied += count
                    require(copied <= MAX_ARCHIVE_BYTES) { "Backup-Datei überschreitet das Größenlimit" }
                    destination.write(buffer, 0, count)
                }
                destination.flush()
            }
            return report
        } finally {
            temp.delete()
        }
    }

    /** Read, bound, checksum and validate the complete archive before showing overwrite confirmation. */
    fun prepareRestore(context: Context, source: InputStream): PreparedDatabaseRestore {
        val directory = File(context.cacheDir, "database-restore-${UUID.randomUUID()}")
        require(directory.mkdir()) { "Temporärer Wiederherstellungsbereich kann nicht erstellt werden" }
        try {
            val extracted = extractArchive(source, directory)
            val manifestFile = extracted.getValue(MANIFEST_ENTRY)
            val manifestBytes = manifestFile.readBytes()
            require(manifestBytes.size <= MAX_MANIFEST_BYTES) { "Backup-Manifest ist zu groß" }
            val manifest = JSONObject(manifestBytes.toString(Charsets.UTF_8))
            require(manifest.optString("format") == BACKUP_FORMAT) { "Das ist kein PulsLibre-Datenbankbackup" }
            require(manifest.optInt("version", -1) == BACKUP_VERSION) { "Backup-Version wird nicht unterstützt" }
            require(manifest.optString("app_id") == "de.pulselibre") { "Backup gehört zu einer anderen App" }
            val createdAt = manifest.getString("created_at")
            Instant.parse(createdAt)
            require(manifest.optString("encryption") == "none") { "Unbekannte Backup-Verschlüsselung" }

            val entryMetadata = manifest.getJSONObject("entries")
            require(entryMetadata.length() == dataEntries.size) { "Backup-Metadaten sind unvollständig" }
            val recordCounts = mutableMapOf<String, Long>()
            dataEntries.forEach { name ->
                val file = extracted.getValue(name)
                val metadata = entryMetadata.getJSONObject(name)
                require(sha256(file) == metadata.getString("sha256")) { "Backup-Prüfsumme stimmt nicht: $name" }
                require(file.length() == metadata.getLong("bytes")) { "Backup-Größe stimmt nicht: $name" }
                val count = validateJsonLines(name, file)
                require(count == metadata.getLong("records")) { "Backup-Datensatzanzahl stimmt nicht: $name" }
                recordCounts[name] = count
            }
            return PreparedDatabaseRestore(
                directory = directory,
                createdAt = createdAt,
                events = recordCounts.getValue(EVENTS_ENTRY),
                legacyReadings = recordCounts.getValue(READINGS_ENTRY),
                syncCursors = recordCounts.getValue(CURSORS_ENTRY)
            )
        } catch (error: Exception) {
            directory.deleteRecursively()
            throw error
        }
    }

    /** Replace all rows in both stores in one SQLite transaction; settings and preferences remain. */
    fun restore(context: Context, plan: PreparedDatabaseRestore) {
        val events = File(plan.directory, EVENTS_ENTRY).bufferedReader(Charsets.UTF_8)
        val readings = File(plan.directory, READINGS_ENTRY).bufferedReader(Charsets.UTF_8)
        val cursors = File(plan.directory, CURSORS_ENTRY).bufferedReader(Charsets.UTF_8)
        events.use { eventReader -> readings.use { readingReader -> cursors.use { cursorReader ->
            CirqaDatabase(context).use { database ->
                database.replaceAllStoredRows(
                    eventReader.lineSequence(),
                    readingReader.lineSequence().map(::parseReading),
                    cursorReader.lineSequence().map(::parseCursor)
                )
            }
        } } }
    }

    /** Clear only the two local data stores. Profile, appearance and test settings are retained. */
    fun reset(context: Context) {
        CirqaDatabase(context).use { it.replaceAllStoredRows(emptySequence(), emptySequence(), emptySequence()) }
    }

    private fun writeArchive(context: Context, target: File): DatabaseBackupReport {
        val now = Instant.now().toString()
        val metadata = JSONObject()
        var eventCount = 0L
        var readingCount = 0L
        var cursorCount = 0L
        ZipOutputStream(BufferedOutputStream(FileOutputStream(target))).use { zip ->
            CirqaDatabase(context).use { database ->
                database.readStoredSnapshot { db ->
                    val events = writeJsonLines(zip, EVENTS_ENTRY) { emit ->
                        db.rawQuery("SELECT payload_json FROM main.events ORDER BY received_at_utc, id", null).use { rows ->
                            while (rows.moveToNext()) emit(rows.getString(0))
                        }
                    }
                    eventCount = events.records
                    metadata.put(EVENTS_ENTRY, entryMetadata(events))

                    val readings = writeJsonLines(zip, READINGS_ENTRY) { emit ->
                        db.rawQuery(
                            """SELECT device_id, metric, measured_at_ms, value, unit, sample_id, received_at_ms
                               FROM legacy.readings ORDER BY measured_at_ms, device_id, metric, sample_id""", null
                        ).use { rows ->
                            while (rows.moveToNext()) emit(encodeReading(RawExportRecord(
                                deviceId = rows.getString(0), metric = rows.getString(1),
                                measuredAtEpochMillis = rows.getLong(2), value = rows.getDouble(3),
                                unit = rows.getString(4), sampleId = rows.getString(5).ifEmpty { null },
                                receivedAtEpochMillis = if (rows.isNull(6)) null else rows.getLong(6)
                            )))
                        }
                    }
                    readingCount = readings.records
                    metadata.put(READINGS_ENTRY, entryMetadata(readings))

                    val cursors = writeJsonLines(zip, CURSORS_ENTRY) { emit ->
                        db.rawQuery(
                            "SELECT device_id, last_committed_ms FROM legacy.sync_cursors ORDER BY device_id", null
                        ).use { rows ->
                            while (rows.moveToNext()) emit(encodeCursor(
                                LocalSyncCursor(rows.getString(0), rows.getLong(1))
                            ))
                        }
                    }
                    cursorCount = cursors.records
                    metadata.put(CURSORS_ENTRY, entryMetadata(cursors))
                    require(events.bytes + readings.bytes + cursors.bytes <= MAX_ARCHIVE_BYTES) {
                        "Lokale Datenbank ist zu groß für ein Backup"
                    }
                }
            }
            val manifest = JSONObject()
                .put("format", BACKUP_FORMAT)
                .put("version", BACKUP_VERSION)
                .put("app_id", "de.pulselibre")
                .put("created_at", now)
                .put("encryption", "none")
                .put("entries", metadata)
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        require(target.length() <= MAX_ARCHIVE_BYTES) { "Backup-Datei überschreitet das Größenlimit" }
        return DatabaseBackupReport(now, eventCount, readingCount, cursorCount)
    }

    private data class JsonLinesMetadata(val records: Long, val bytes: Long, val sha256: String)

    private fun writeJsonLines(zip: ZipOutputStream, name: String, produce: ((String) -> Unit) -> Unit): JsonLinesMetadata {
        zip.putNextEntry(ZipEntry(name))
        val digest = MessageDigest.getInstance("SHA-256")
        val writer = BufferedWriter(OutputStreamWriter(DigestOutputStream(zip, digest), Charsets.UTF_8))
        var records = 0L
        var bytes = 0L
        produce { line ->
            require(line.isNotBlank() && '\n' !in line && '\r' !in line) { "Ungültige JSONL-Daten" }
            val lineBytes = line.toByteArray(Charsets.UTF_8).size
            require(lineBytes <= MAX_LINE_BYTES) { "Ein Datensatz überschreitet das Größenlimit" }
            records++
            bytes += lineBytes + 1
            require(records <= MAX_RECORDS && bytes <= MAX_ENTRY_BYTES) { "Datenbankbackup überschreitet das Größenlimit" }
            writer.write(line)
            writer.newLine()
        }
        writer.flush()
        zip.closeEntry()
        return JsonLinesMetadata(records, bytes, digest.digest().toHex())
    }

    private fun entryMetadata(value: JsonLinesMetadata) = JSONObject()
        .put("records", value.records)
        .put("bytes", value.bytes)
        .put("sha256", value.sha256)

    private fun extractArchive(source: InputStream, directory: File): Map<String, File> {
        val allowed = dataEntries + MANIFEST_ENTRY
        val files = linkedMapOf<String, File>()
        var totalBytes = 0L
        ZipInputStream(BufferedInputStream(source)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(!entry.isDirectory && entry.name in allowed) { "Backup enthält einen unbekannten Dateieintrag" }
                require(entry.name !in files) { "Backup enthält doppelte Dateieinträge" }
                val target = File(directory, entry.name)
                var entryBytes = 0L
                FileOutputStream(target).use { out ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = zip.read(buffer)
                        if (count < 0) break
                        entryBytes += count
                        totalBytes += count
                        require(entryBytes <= MAX_ENTRY_BYTES && totalBytes <= MAX_ARCHIVE_BYTES) {
                            "Backup ist zu groß"
                        }
                        out.write(buffer, 0, count)
                    }
                }
                zip.closeEntry()
                files[entry.name] = target
            }
        }
        require(files.keys == allowed) { "Backup ist unvollständig" }
        require(files.getValue(MANIFEST_ENTRY).length() <= MAX_MANIFEST_BYTES) { "Backup-Manifest ist zu groß" }
        return files
    }

    private fun validateJsonLines(name: String, file: File): Long {
        var count = 0L
        file.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.forEach { line ->
                require(line.isNotBlank() && line.toByteArray(Charsets.UTF_8).size <= MAX_LINE_BYTES) {
                    "Ungültige oder zu große Zeile in $name"
                }
                when (name) {
                    EVENTS_ENTRY -> require(CirqaDatabase.isValidStoredLine(line)) { "Ungültiger Ereigniseintrag im Backup" }
                    READINGS_ENTRY -> parseReading(line)
                    CURSORS_ENTRY -> parseCursor(line)
                }
                count++
                require(count <= MAX_RECORDS) { "Zu viele Einträge in $name" }
            }
        }
        return count
    }

    private fun parseReading(line: String): RawExportRecord {
        val item = JSONObject(line)
        val record = RawExportRecord(
            deviceId = item.getString("device_id"), metric = item.getString("metric"),
            measuredAtEpochMillis = item.getLong("measured_at_ms"), value = item.getDouble("value"),
            unit = item.getString("unit"), sampleId = item.optString("sample_id").ifEmpty { null },
            receivedAtEpochMillis = if (!item.has("received_at_ms") || item.isNull("received_at_ms")) null
                else item.getLong("received_at_ms")
        )
        require(CirqaDatabase.isValidStoredReading(record)) { "Ungültiger Messwert im Backup" }
        return record
    }

    private fun parseCursor(line: String): LocalSyncCursor {
        val item = JSONObject(line)
        val cursor = LocalSyncCursor(item.getString("device_id"), item.getLong("last_committed_ms"))
        require(cursor.deviceId.isNotBlank() && cursor.lastCommittedEpochMillis >= 0) {
            "Ungültiger Synchronisationsstand im Backup"
        }
        return cursor
    }

    private fun encodeReading(record: RawExportRecord): String = JSONObject()
        .put("device_id", record.deviceId)
        .put("metric", record.metric)
        .put("measured_at_ms", record.measuredAtEpochMillis)
        .put("value", record.value)
        .put("unit", record.unit)
        .put("sample_id", record.sampleId ?: "")
        .put("received_at_ms", record.receivedAtEpochMillis ?: JSONObject.NULL)
        .toString()

    private fun encodeCursor(cursor: LocalSyncCursor): String = JSONObject()
        .put("device_id", cursor.deviceId)
        .put("last_committed_ms", cursor.lastCommittedEpochMillis)
        .toString()

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        DigestInputStream(BufferedInputStream(FileInputStream(file)), digest).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (input.read(buffer) >= 0) Unit
        }
        return digest.digest().toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 255) }
}
