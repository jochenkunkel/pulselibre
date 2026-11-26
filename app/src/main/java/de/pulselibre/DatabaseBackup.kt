package de.pulselibre

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
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
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val BACKUP_FORMAT = "de.pulselibre.local-database-backup"
private const val BACKUP_VERSION = 12
/**
 * Size limits. Until 25.11.2026 one 128 MiB constant bounded both the ZIP file and the sum of the *uncompressed* JSON
 * lines; real data (all FIT field generations, original files) passed it, every backup failed and left a 0-byte file.
 * Now the compressed file and the uncompressed content have separate limits; restore uses the same ones (zip-bomb guard).
 */
internal const val MAX_ARCHIVE_BYTES = 512L * 1024L * 1024L
internal const val MAX_UNCOMPRESSED_BYTES = 2L * 1024L * 1024L * 1024L
internal const val MAX_ENTRY_BYTES = 1536L * 1024L * 1024L
private const val MAX_LINE_BYTES = 2 * 1024 * 1024
internal const val MAX_RECORDS = 20_000_000L
/** Headroom kept free when writing or extracting an archive in the app cache. */
private const val MIN_FREE_BYTES = 64L * 1024L * 1024L
private const val MAX_MANIFEST_BYTES = 64 * 1024
private const val EVENTS_ENTRY = "cirqa_events.jsonl"
private const val READINGS_ENTRY = "legacy_readings.jsonl"
private const val CURSORS_ENTRY = "sync_cursors.jsonl"
private const val FIT_SOURCES_ENTRY = "fit_sources.jsonl"
private const val FIT_PROVENANCE_ENTRY = "fit_observation_provenance.jsonl"
private const val CANONICAL_READINGS_ENTRY = "canonical_observations.jsonl"
private const val CANONICAL_CURSORS_ENTRY = "canonical_sync_cursors.jsonl"
private const val CANONICAL_FIT_SOURCES_ENTRY = "canonical_fit_sources.jsonl"
private const val CANONICAL_FIT_PROVENANCE_ENTRY = "canonical_fit_observation_provenance.jsonl"
private const val CANONICAL_DEVICE_METADATA_ENTRY = "canonical_fit_device_metadata.jsonl"
private const val RAW_DEVICE_METADATA_ENTRY = "raw_device_metadata.jsonl"
private const val CANONICAL_UNMAPPED_FIT_FIELDS_ENTRY = "canonical_fit_unmapped_fields.jsonl"
private const val CANONICAL_FIT_GENERATIONS_ENTRY = "canonical_fit_decoder_generations.jsonl"
private const val CANONICAL_FIT_GENERATION_OBSERVATIONS_ENTRY = "canonical_fit_generation_observations.jsonl"
private const val CANONICAL_FIT_GENERATION_FIELDS_ENTRY = "canonical_fit_generation_fields.jsonl"
private const val CANONICAL_ACTIVE_FIT_GENERATIONS_ENTRY = "canonical_fit_active_generations.jsonl"
private const val CANONICAL_FIT_REVIEW_CANDIDATES_ENTRY = "canonical_fit_review_candidates.jsonl"
private const val LEGACY_FIT_REVIEW_CANDIDATES_ENTRY = "legacy_fit_review_candidates.jsonl"
private const val LIVE_DEVICE_METADATA_ENTRY = "live_device_metadata.jsonl"
private const val IMPORT_RUNS_ENTRY = "import_runs.jsonl"
private const val IMPORT_ERRORS_ENTRY = "import_errors.jsonl"
private const val OBSERVATION_STATUS_EVENTS_ENTRY = "observation_status_events.jsonl"
private const val MANIFEST_ENTRY = "manifest.json"
private val BACKUP_V1_ENTRIES = setOf(EVENTS_ENTRY, READINGS_ENTRY, CURSORS_ENTRY)
private val BACKUP_V2_ENTRIES =
    BACKUP_V1_ENTRIES + setOf(FIT_SOURCES_ENTRY, FIT_PROVENANCE_ENTRY)
private val BACKUP_V3_ENTRIES = BACKUP_V2_ENTRIES + setOf(
    CANONICAL_READINGS_ENTRY, CANONICAL_CURSORS_ENTRY,
    CANONICAL_FIT_SOURCES_ENTRY, CANONICAL_FIT_PROVENANCE_ENTRY
)
private val BACKUP_V4_ENTRIES = BACKUP_V3_ENTRIES + CANONICAL_DEVICE_METADATA_ENTRY
private val BACKUP_V5_ENTRIES = BACKUP_V4_ENTRIES + RAW_DEVICE_METADATA_ENTRY
private val BACKUP_V6_ENTRIES = BACKUP_V5_ENTRIES + CANONICAL_UNMAPPED_FIT_FIELDS_ENTRY
private val BACKUP_V7_ENTRIES = BACKUP_V6_ENTRIES + setOf(
    CANONICAL_FIT_GENERATIONS_ENTRY,
    CANONICAL_FIT_GENERATION_OBSERVATIONS_ENTRY,
    CANONICAL_ACTIVE_FIT_GENERATIONS_ENTRY
)
private val BACKUP_V8_ENTRIES = BACKUP_V7_ENTRIES + setOf(
    CANONICAL_FIT_REVIEW_CANDIDATES_ENTRY,
    LEGACY_FIT_REVIEW_CANDIDATES_ENTRY,
    LIVE_DEVICE_METADATA_ENTRY
)
private val BACKUP_V9_ENTRIES = BACKUP_V8_ENTRIES + DownloadedFileArchive.BACKUP_ENTRY
private val BACKUP_V10_ENTRIES = BACKUP_V9_ENTRIES + CANONICAL_FIT_GENERATION_FIELDS_ENTRY
private val BACKUP_V11_ENTRIES = BACKUP_V10_ENTRIES + setOf(
    IMPORT_RUNS_ENTRY, IMPORT_ERRORS_ENTRY, OBSERVATION_STATUS_EVENTS_ENTRY
)
/** v12: personal configuration (profile, theme, language, activity detection, auto-start, dashboard layout). */
private val BACKUP_V12_ENTRIES = BACKUP_V11_ENTRIES + AppSettingsBackup.ENTRY

data class DatabaseBackupReport(
    val createdAt: String,
    val events: Long,
    val legacyReadings: Long,
    val syncCursors: Long,
    val fitSources: Long = 0,
    val fitProvenance: Long = 0,
    val canonicalReadings: Long = 0,
    val canonicalSyncCursors: Long = 0,
    val canonicalFitSources: Long = 0,
    val canonicalFitProvenance: Long = 0,
    val canonicalDeviceMetadata: Long = 0,
    val rawDeviceMetadata: Long = 0,
    val canonicalFitGenerations: Long = 0,
    val canonicalFitGenerationObservations: Long = 0,
    val canonicalFitGenerationFields: Long = 0,
    val canonicalActiveFitGenerations: Long = 0,
    val canonicalFitReviewCandidates: Long = 0,
    val legacyFitReviewCandidates: Long = 0,
    val liveDeviceMetadata: Long = 0
)

/** A validated backup staged privately before any local database can be changed. */
class PreparedDatabaseRestore internal constructor(
    internal val directory: File,
    val createdAt: String,
    val events: Long,
    val legacyReadings: Long,
    val syncCursors: Long,
    internal val version: Int,
    val fitSources: Long,
    val fitProvenance: Long,
    val canonicalReadings: Long,
    val canonicalSyncCursors: Long,
    val canonicalFitSources: Long,
    val canonicalFitProvenance: Long,
    val canonicalDeviceMetadata: Long,
    val rawDeviceMetadata: Long,
    val canonicalFitGenerations: Long,
    val canonicalFitGenerationObservations: Long,
    val canonicalFitGenerationFields: Long,
    val canonicalActiveFitGenerations: Long,
    val canonicalFitReviewCandidates: Long,
    val legacyFitReviewCandidates: Long,
    val liveDeviceMetadata: Long,
    /** Personal settings in the backup (v12+); 0 for older backups, whose restore leaves the settings as they are. */
    val appSettings: Long = 0
) : Closeable {
    /** True for v12+ backups: their restore replaces the personal settings (even an empty set means "defaults"). */
    val containsSettings: Boolean get() = version >= 12

    override fun close() { directory.deleteRecursively() }
}

/** Versioned, checksummed archive for both local SQLite stores. The archive is not encrypted. */
object DatabaseBackup {
    /** Stream a consistent snapshot into a temporary archive, then copy it to the chosen SAF file. */
    fun write(context: Context, destination: OutputStream, progress: BackupProgress = BackupProgress.NONE): DatabaseBackupReport {
        // The archive is built in the cache first: say so clearly instead of failing halfway with a disk error.
        val database = context.getDatabasePath("cirqa_measurements.db").length()
        // Snapshot copy of the database plus the temporary archive both live in the cache.
        require(context.cacheDir.usableSpace > 2 * database + MIN_FREE_BYTES) {
            "Zu wenig freier Speicher für das temporäre Backup (frei ${context.cacheDir.usableSpace / 1_048_576} MiB)"
        }
        val temp = File.createTempFile("pulselibre-backup-", ".zip", context.cacheDir)
        try {
            val report = writeArchive(context, temp, progress)
            val size = temp.length().coerceAtLeast(1)
            progress.update(BackupProgress.Phase.COPY, 0, 100)
            BufferedInputStream(FileInputStream(temp)).use { source ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var copied = 0L
                var shown = 0
                while (true) {
                    val count = source.read(buffer)
                    if (count < 0) break
                    copied += count
                    require(copied <= MAX_ARCHIVE_BYTES) { "Backup-Datei überschreitet das Größenlimit" }
                    destination.write(buffer, 0, count)
                    val percent = (copied * 100 / size).toInt()
                    if (percent != shown) { shown = percent; progress.update(BackupProgress.Phase.COPY, percent, 100) }
                }
                destination.flush()
            }
            return report
        } finally {
            temp.delete()
        }
    }

    /** Read, bound, checksum and validate the complete archive before showing overwrite confirmation. */
    fun prepareRestore(context: Context, source: InputStream, progress: BackupProgress = BackupProgress.NONE): PreparedDatabaseRestore {
        require(context.cacheDir.usableSpace > MIN_FREE_BYTES) {
            "Zu wenig freier Speicher zum Prüfen des Backups (frei ${context.cacheDir.usableSpace / 1_048_576} MiB)"
        }
        val directory = File(context.cacheDir, "database-restore-${UUID.randomUUID()}")
        require(directory.mkdir()) { "Temporärer Wiederherstellungsbereich kann nicht erstellt werden" }
        try {
            progress.update(BackupProgress.Phase.EXTRACT, 0, 0)
            val extracted = extractArchive(source, directory)
            val manifestFile = extracted.getValue(MANIFEST_ENTRY)
            val manifestBytes = manifestFile.readBytes()
            require(manifestBytes.size <= MAX_MANIFEST_BYTES) { "Backup-Manifest ist zu groß" }
            val manifest = JSONObject(manifestBytes.toString(Charsets.UTF_8))
            require(manifest.optString("format") == BACKUP_FORMAT) { "Das ist kein PulsLibre-Datenbankbackup" }
            val version = manifest.optInt("version", -1)
            val dataEntries = when (version) {
                1 -> BACKUP_V1_ENTRIES
                2 -> BACKUP_V2_ENTRIES
                3 -> BACKUP_V3_ENTRIES
                4 -> BACKUP_V4_ENTRIES
                5 -> BACKUP_V5_ENTRIES
                6 -> BACKUP_V6_ENTRIES
                7 -> BACKUP_V7_ENTRIES
                8 -> BACKUP_V8_ENTRIES
                9 -> BACKUP_V9_ENTRIES
                10 -> BACKUP_V10_ENTRIES
                11 -> BACKUP_V11_ENTRIES
                BACKUP_VERSION -> BACKUP_V12_ENTRIES
                else -> throw IllegalArgumentException("Backup-Version wird nicht unterstützt")
            }
            require(extracted.keys == dataEntries + MANIFEST_ENTRY) {
                "Backup-Einträge passen nicht zur Versionsnummer"
            }
            require(manifest.optString("app_id") == "de.pulselibre") { "Backup gehört zu einer anderen App" }
            val createdAt = manifest.getString("created_at")
            Instant.parse(createdAt)
            require(manifest.optString("encryption") == "none") { "Unbekannte Backup-Verschlüsselung" }

            val entryMetadata = manifest.getJSONObject("entries")
            require(entryMetadata.length() == dataEntries.size) { "Backup-Metadaten sind unvollständig" }
            val recordCounts = mutableMapOf<String, Long>()
            dataEntries.forEachIndexed { index, name ->
                progress.update(BackupProgress.Phase.VALIDATE, index, dataEntries.size)
                val file = extracted.getValue(name)
                val metadata = entryMetadata.getJSONObject(name)
                require(sha256(file) == metadata.getString("sha256")) { "Backup-Prüfsumme stimmt nicht: $name" }
                require(file.length() == metadata.getLong("bytes")) { "Backup-Größe stimmt nicht: $name" }
                val count = validateJsonLines(name, file)
                require(count == metadata.getLong("records")) { "Backup-Datensatzanzahl stimmt nicht: $name" }
                recordCounts[name] = count
            }
            progress.update(BackupProgress.Phase.REFERENCES, 0, 1)
            if (version >= 12) {
                File(directory, AppSettingsBackup.ENTRY).useLines { AppSettingsBackup.validateAll(it) }
            }
            if (version >= 9) {
                File(directory, DownloadedFileArchive.BACKUP_ENTRY).useLines { lines ->
                    DownloadedFileArchive.consume(lines.map(DownloadedFileArchive::parse))
                }
            }
            if (version >= 2) {
                validateFitReferences(directory, READINGS_ENTRY, FIT_SOURCES_ENTRY, FIT_PROVENANCE_ENTRY)
            }
            if (version >= 3) {
                validateFitReferences(
                    directory, CANONICAL_READINGS_ENTRY, CANONICAL_FIT_SOURCES_ENTRY,
                    CANONICAL_FIT_PROVENANCE_ENTRY
                )
            }
            if (version >= 4) {
                validateDeviceMetadataReferences(directory)
            }
            if (version >= 5) {
                validateRawDeviceMetadataReferences(directory)
            }
            if (version >= 6) validateUnmappedFitFieldReferences(directory)
            if (version >= 7) validateGenerationReferences(directory)
            if (version >= 8) {
                validateFitReviewCandidateReferences(
                    directory, FIT_SOURCES_ENTRY, LEGACY_FIT_REVIEW_CANDIDATES_ENTRY
                )
                validateFitReviewCandidateReferences(
                    directory, CANONICAL_FIT_SOURCES_ENTRY, CANONICAL_FIT_REVIEW_CANDIDATES_ENTRY
                )
            }
            return PreparedDatabaseRestore(
                directory = directory,
                createdAt = createdAt,
                events = recordCounts.getValue(EVENTS_ENTRY),
                legacyReadings = recordCounts.getValue(READINGS_ENTRY),
                syncCursors = recordCounts.getValue(CURSORS_ENTRY),
                version = version,
                fitSources = recordCounts[FIT_SOURCES_ENTRY] ?: 0,
                fitProvenance = recordCounts[FIT_PROVENANCE_ENTRY] ?: 0,
                canonicalReadings = recordCounts[CANONICAL_READINGS_ENTRY] ?: 0,
                canonicalSyncCursors = recordCounts[CANONICAL_CURSORS_ENTRY] ?: 0,
                canonicalFitSources = recordCounts[CANONICAL_FIT_SOURCES_ENTRY] ?: 0,
                canonicalFitProvenance = recordCounts[CANONICAL_FIT_PROVENANCE_ENTRY] ?: 0,
                canonicalDeviceMetadata = recordCounts[CANONICAL_DEVICE_METADATA_ENTRY] ?: 0,
                rawDeviceMetadata = recordCounts[RAW_DEVICE_METADATA_ENTRY] ?: 0,
                canonicalFitGenerations = recordCounts[CANONICAL_FIT_GENERATIONS_ENTRY] ?: 0,
                canonicalFitGenerationObservations =
                    recordCounts[CANONICAL_FIT_GENERATION_OBSERVATIONS_ENTRY] ?: 0,
                canonicalFitGenerationFields = recordCounts[CANONICAL_FIT_GENERATION_FIELDS_ENTRY] ?: 0,
                canonicalActiveFitGenerations =
                    recordCounts[CANONICAL_ACTIVE_FIT_GENERATIONS_ENTRY] ?: 0,
                canonicalFitReviewCandidates =
                    recordCounts[CANONICAL_FIT_REVIEW_CANDIDATES_ENTRY] ?: 0,
                legacyFitReviewCandidates = recordCounts[LEGACY_FIT_REVIEW_CANDIDATES_ENTRY] ?: 0,
                liveDeviceMetadata = recordCounts[LIVE_DEVICE_METADATA_ENTRY] ?: 0,
                appSettings = recordCounts[AppSettingsBackup.ENTRY] ?: 0
            )
        } catch (error: Exception) {
            directory.deleteRecursively()
            throw error
        }
    }

    /**
     * Replace all rows in both stores in one SQLite transaction. From v12 the personal settings are replaced too (after
     * the data, so a failed data restore leaves them untouched); older backups leave the current settings as they are.
     */
    fun restore(context: Context, plan: PreparedDatabaseRestore, progress: BackupProgress = BackupProgress.NONE,
                /** false = only the personal settings (v12+); the databases stay exactly as they are. */
                includeData: Boolean = true) {
        require(includeData || plan.containsSettings) { "Dieses Backup enthält keine Einstellungen" }
        try {
            if (includeData) {
                progress.update(BackupProgress.Phase.RESTORE, 0, 0)
                restoreRows(context, plan)
            }
            if (includeData && plan.version >= 11) {
                val db = CirqaDatabase(context)
                val sql = db.writableDatabase
                try {
                    sql.beginTransaction()
                    try {
                        File(plan.directory, IMPORT_RUNS_ENTRY).bufferedReader(Charsets.UTF_8).use {
                            restoreAuditRows(sql, it.lineSequence(), "import_runs")
                        }
                        File(plan.directory, IMPORT_ERRORS_ENTRY).bufferedReader(Charsets.UTF_8).use {
                            restoreAuditRows(sql, it.lineSequence(), "import_errors")
                        }
                        File(plan.directory, OBSERVATION_STATUS_EVENTS_ENTRY).bufferedReader(Charsets.UTF_8).use {
                            restoreAuditRows(sql, it.lineSequence(), "observation_status_events")
                        }
                        sql.setTransactionSuccessful()
                    } finally {
                        sql.endTransaction()
                    }
                } finally {
                    db.close()
                }
            }
            if (plan.version >= 12) {
                progress.update(BackupProgress.Phase.SETTINGS, 0, 0)
                val settings = File(plan.directory, AppSettingsBackup.ENTRY).useLines { AppSettingsBackup.validateAll(it) }
                AppSettingsBackup.restore(context, settings)
            }
        } finally { TrendCache.invalidate() } // A6-33
    }

    private fun restoreRows(context: Context, plan: PreparedDatabaseRestore) {
        val events = File(plan.directory, EVENTS_ENTRY).bufferedReader(Charsets.UTF_8)
        val readings = File(plan.directory, READINGS_ENTRY).bufferedReader(Charsets.UTF_8)
        val cursors = File(plan.directory, CURSORS_ENTRY).bufferedReader(Charsets.UTF_8)
        val sourcesFile = File(plan.directory, FIT_SOURCES_ENTRY)
        val provenanceFile = File(plan.directory, FIT_PROVENANCE_ENTRY)
        events.use { eventReader -> readings.use { readingReader -> cursors.use { cursorReader ->
            CirqaDatabase(context).use { database ->
                if (plan.version == 1) {
                    database.replaceAllStoredRows(
                        eventReader.lineSequence(),
                        readingReader.lineSequence().map(::parseReading),
                        cursorReader.lineSequence().map(::parseCursor)
                    )
                } else if (plan.version == 2) {
                    sourcesFile.bufferedReader(Charsets.UTF_8).use { sourceReader ->
                        provenanceFile.bufferedReader(Charsets.UTF_8).use { provenanceReader ->
                            database.replaceAllStoredRows(
                                eventReader.lineSequence(),
                                readingReader.lineSequence().map(::parseReading),
                                cursorReader.lineSequence().map(::parseCursor),
                                sourceReader.lineSequence().map(::parseFitSource),
                                provenanceReader.lineSequence().map(::parseFitProvenance)
                            )
                        }
                    }
                } else {
                    sourcesFile.bufferedReader(Charsets.UTF_8).use { sourceReader ->
                        provenanceFile.bufferedReader(Charsets.UTF_8).use { provenanceReader ->
                            File(plan.directory, CANONICAL_READINGS_ENTRY).bufferedReader(Charsets.UTF_8).use { canonicalReadings ->
                                File(plan.directory, CANONICAL_CURSORS_ENTRY).bufferedReader(Charsets.UTF_8).use { canonicalCursors ->
                                    File(plan.directory, CANONICAL_FIT_SOURCES_ENTRY).bufferedReader(Charsets.UTF_8).use { canonicalSources ->
                                        File(plan.directory, CANONICAL_FIT_PROVENANCE_ENTRY).bufferedReader(Charsets.UTF_8).use { canonicalProvenance ->
                                                fun replace(
                                                    metadata: Sequence<FitHistoricalObservations.DeviceMetadata>,
                                                    rawMetadata: Sequence<RawDeviceMetadataRecord>,
                                                    unmappedFields: Sequence<StoredFitUnmappedField>,
                                                    generations: Sequence<StoredFitDecoderGeneration> = emptySequence(),
                                                    generationObservations: Sequence<StoredFitGenerationObservation> = emptySequence(),
                                                    generationFields: Sequence<StoredFitGenerationField> = emptySequence(),
                                                    activeGenerations: Sequence<StoredActiveFitGeneration> = emptySequence(),
                                                    legacyReviewCandidates: Sequence<StoredFitReviewCandidate> = emptySequence(),
                                                    reviewCandidates: Sequence<StoredFitReviewCandidate> = emptySequence(),
                                                    liveDeviceMetadata: Sequence<LiveDeviceMetadata> = emptySequence(),
                                                    generationSnapshotProvided: Boolean = false
                                                ) {
                                                database.replaceAllStoredRows(
                                                    events = eventReader.lineSequence(),
                                                    readings = readingReader.lineSequence().map(::parseReading),
                                                    cursors = cursorReader.lineSequence().map(::parseCursor),
                                                    fitSources = sourceReader.lineSequence().map(::parseFitSource),
                                                    fitProvenance = provenanceReader.lineSequence().map(::parseFitProvenance),
                                                    canonicalObservations = canonicalReadings.lineSequence().map(::parseReading),
                                                    canonicalCursors = canonicalCursors.lineSequence().map(::parseCursor),
                                                    canonicalFitSources = canonicalSources.lineSequence().map(::parseFitSource),
                                                    canonicalFitProvenance = canonicalProvenance.lineSequence().map(::parseFitProvenance),
                                                    canonicalDeviceMetadata = metadata,
                                                    canonicalRawDeviceMetadata = rawMetadata,
                                                    canonicalUnmappedFitFields = unmappedFields,
                                                    canonicalFitGenerations = generations,
                                                     canonicalFitGenerationObservations = generationObservations,
                                                     canonicalFitGenerationFields = generationFields,
                                                    canonicalActiveFitGenerations = activeGenerations,
                                                    legacyFitReviewCandidates = legacyReviewCandidates,
                                                    canonicalFitReviewCandidates = reviewCandidates,
                                                    liveDeviceMetadata = liveDeviceMetadata,
                                                    generationSnapshotProvided = generationSnapshotProvided,
                                                    markHistoricalCopyComplete = true,
                                                    downloadedFiles = sequence {
                                                        if (plan.version >= 9) {
                                                            File(plan.directory, DownloadedFileArchive.BACKUP_ENTRY)
                                                                .bufferedReader().use { reader ->
                                                                    while (true) {
                                                                        val line = reader.readLine() ?: break
                                                                        yield(DownloadedFileArchive.parse(line))
                                                                    }
                                                                }
                                                        }
                                                    }
                                                )
                                            }
                                            if (plan.version >= 4) {
                                                File(plan.directory, CANONICAL_DEVICE_METADATA_ENTRY)
                                                    .bufferedReader(Charsets.UTF_8).use { metadataReader ->
                                                        if (plan.version >= 5) {
                                                            File(plan.directory, RAW_DEVICE_METADATA_ENTRY)
                                                                .bufferedReader(Charsets.UTF_8).use { rawReader ->
                                                                    if (plan.version >= 6) {
                                                                        File(plan.directory, CANONICAL_UNMAPPED_FIT_FIELDS_ENTRY)
                                                                            .bufferedReader(Charsets.UTF_8).use { unknownReader ->
                                                                            if (plan.version >= 7) {
                                                                                File(plan.directory, CANONICAL_FIT_GENERATIONS_ENTRY)
                                                                                    .bufferedReader(Charsets.UTF_8).use { generationReader ->
                                                                                    File(plan.directory, CANONICAL_FIT_GENERATION_OBSERVATIONS_ENTRY)
                                                                                        .bufferedReader(Charsets.UTF_8).use { observationReader ->
                                                                                        File(plan.directory, CANONICAL_ACTIVE_FIT_GENERATIONS_ENTRY)
                                                                                            .bufferedReader(Charsets.UTF_8).use { activeReader ->
                                                                                            if (plan.version >= 8) {
                                                                                                File(plan.directory, LEGACY_FIT_REVIEW_CANDIDATES_ENTRY)
                                                                                                    .bufferedReader(Charsets.UTF_8).use { legacyReviewReader ->
                                                                                                    File(plan.directory, CANONICAL_FIT_REVIEW_CANDIDATES_ENTRY)
                                                                                                        .bufferedReader(Charsets.UTF_8).use { reviewReader ->
                                                                                                        File(plan.directory, LIVE_DEVICE_METADATA_ENTRY)
                                                                                                            .bufferedReader(Charsets.UTF_8).use { liveReader ->
                                                                                                            replace(
                                                                                                                metadataReader.lineSequence().map(::parseDeviceMetadata),
                                                                                                                rawReader.lineSequence().map(::parseRawDeviceMetadata),
                                                                                                                unknownReader.lineSequence().map(::parseUnmappedFitField),
                                                                                                                 generations = generationReader.lineSequence().map(::parseFitGeneration),
                                                                                                                 generationObservations = observationReader.lineSequence().map(::parseFitGenerationObservation),
                                                                                                                 activeGenerations = activeReader.lineSequence().map(::parseActiveFitGeneration),
                                                                                                                legacyReviewCandidates = legacyReviewReader.lineSequence().map(::parseFitReviewCandidate),
                                                                                                                reviewCandidates = reviewReader.lineSequence().map(::parseFitReviewCandidate),
                                                                                                                liveDeviceMetadata = liveReader.lineSequence().map(::parseLiveDeviceMetadata),
                                                                                                                generationFields = if (plan.version >= 10)
                                                                                                                    File(plan.directory, CANONICAL_FIT_GENERATION_FIELDS_ENTRY)
                                                                                                                        .useLines { it.map(::parseFitGenerationField).toList().asSequence() }
                                                                                                                else emptySequence(),
                                                                                                                generationSnapshotProvided = true
                                                                                                            )
                                                                                                        }
                                                                                                    }
                                                                                                }
                                                                                            } else {
                                                                                                replace(
                                                                                                    metadataReader.lineSequence().map(::parseDeviceMetadata),
                                                                                                    rawReader.lineSequence().map(::parseRawDeviceMetadata),
                                                                                                    unknownReader.lineSequence().map(::parseUnmappedFitField),
                                                                                                     generations = generationReader.lineSequence().map(::parseFitGeneration),
                                                                                                     generationObservations = observationReader.lineSequence().map(::parseFitGenerationObservation),
                                                                                                     activeGenerations = activeReader.lineSequence().map(::parseActiveFitGeneration),
                                                                                                    generationSnapshotProvided = true
                                                                                                )
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            } else {
                                                                                replace(
                                                                                    metadataReader.lineSequence().map(::parseDeviceMetadata),
                                                                                    rawReader.lineSequence().map(::parseRawDeviceMetadata),
                                                                                    unknownReader.lineSequence().map(::parseUnmappedFitField)
                                                                                )
                                                                            }
                                                                        }
                                                                    } else replace(
                                                                        metadataReader.lineSequence().map(::parseDeviceMetadata),
                                                                        rawReader.lineSequence().map(::parseRawDeviceMetadata),
                                                                        emptySequence()
                                                                    )
                                                                }
                                                        } else {
                                                            replace(
                                                                metadataReader.lineSequence().map(::parseDeviceMetadata),
                                                                emptySequence(),
                                                                emptySequence()
                                                            )
                                                        }
                                                    }
                                            } else {
                                                replace(emptySequence(), emptySequence(), emptySequence())
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } } }
    }

    /** Clear only the two local data stores. Profile, appearance and test settings are retained. */
    fun reset(context: Context) {
        CirqaDatabase(context).use { it.replaceAllStoredRows(emptySequence(), emptySequence(), emptySequence()) }
    }

    private fun writeArchive(context: Context, target: File, progress: BackupProgress): DatabaseBackupReport {
        val now = Instant.now().toString()
        val metadata = JSONObject()
        val entriesTotal = BACKUP_V12_ENTRIES.size
        var eventCount = 0L
        var readingCount = 0L
        var cursorCount = 0L
        var fitSourceCount = 0L
        var fitProvenanceCount = 0L
        var canonicalReadingCount = 0L
        var canonicalCursorCount = 0L
        var canonicalFitSourceCount = 0L
        var canonicalFitProvenanceCount = 0L
        var canonicalDeviceMetadataCount = 0L
        var rawDeviceMetadataCount = 0L
        var canonicalFitGenerationCount = 0L
        var canonicalFitGenerationObservationCount = 0L
        var canonicalFitGenerationFieldCount = 0L
        var canonicalActiveFitGenerationCount = 0L
        var canonicalFitReviewCandidateCount = 0L
        var legacyFitReviewCandidateCount = 0L
        var liveDeviceMetadataCount = 0L
        ZipOutputStream(BufferedOutputStream(FileOutputStream(target))).use { zip ->
            CirqaDatabase(context).use { database ->
                database.ensureHistoricalStoreCopied()
                progress.update(BackupProgress.Phase.SNAPSHOT, 0, 1)
                database.readStoredSnapshot { db ->
                    progress.update(BackupProgress.Phase.WRITE, 0, entriesTotal)
                    val events = writeJsonLines(zip, EVENTS_ENTRY) { emit ->
                        db.rawQuery("SELECT payload_json FROM main.events ORDER BY received_at_utc, id", null).use { rows ->
                            while (rows.moveToNext()) emit(rows.getString(0))
                        }
                    }
                    eventCount = events.records
                    metadata.put(EVENTS_ENTRY, entryMetadata(events))
                    progress.update(BackupProgress.Phase.WRITE, metadata.length(), entriesTotal)

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
                    progress.update(BackupProgress.Phase.WRITE, metadata.length(), entriesTotal)

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
                    progress.update(BackupProgress.Phase.WRITE, metadata.length(), entriesTotal)

                    val sources = writeJsonLines(zip, FIT_SOURCES_ENTRY) { emit ->
                        db.rawQuery(
                            """SELECT source_sha256, file_bytes, relative_path, validated_at_ms,
                                      decoder_version, profile_version
                               FROM legacy.fit_sources ORDER BY source_sha256""", null
                        ).use { rows ->
                            while (rows.moveToNext()) emit(encodeFitSource(StoredFitSource(
                                sha256 = rows.getString(0), fileBytes = rows.getLong(1),
                                relativePath = rows.getString(2), validatedAtEpochMillis = rows.getLong(3),
                                decoderVersion = rows.getString(4), profileVersion = rows.getString(5)
                            )))
                        }
                    }
                    fitSourceCount = sources.records
                    metadata.put(FIT_SOURCES_ENTRY, entryMetadata(sources))
                    progress.update(BackupProgress.Phase.WRITE, metadata.length(), entriesTotal)

                    val provenance = writeJsonLines(zip, FIT_PROVENANCE_ENTRY) { emit ->
                        db.rawQuery(
                            """SELECT sample_id, device_id, metric, measured_at_ms, source_sha256,
                                      record_ordinal, global_message_number, field_number, quality, decoder_version
                               FROM legacy.fit_observation_provenance
                               ORDER BY source_sha256, record_ordinal, global_message_number, field_number""", null
                        ).use { rows ->
                            while (rows.moveToNext()) emit(encodeFitProvenance(StoredFitProvenance(
                                sampleId = rows.getString(0), deviceId = rows.getString(1), metric = rows.getString(2),
                                measuredAtEpochMillis = rows.getLong(3), sourceSha256 = rows.getString(4),
                                recordOrdinal = rows.getInt(5), globalMessageNumber = rows.getInt(6),
                                fieldNumber = rows.getInt(7), quality = rows.getString(8),
                                decoderVersion = rows.getString(9)
                            )))
                        }
                    }
                    fitProvenanceCount = provenance.records
                    metadata.put(FIT_PROVENANCE_ENTRY, entryMetadata(provenance))
                    progress.update(BackupProgress.Phase.WRITE, metadata.length(), entriesTotal)

                    val canonicalReadings = writeJsonLines(zip, CANONICAL_READINGS_ENTRY) { emit ->
                        db.rawQuery(
                            """SELECT device_id, metric, measured_at_ms, value, unit, sample_id, received_at_ms
                               FROM main.observations ORDER BY measured_at_ms, device_id, metric, sample_id""", null
                        ).use { rows ->
                            while (rows.moveToNext()) emit(encodeReading(RawExportRecord(
                                deviceId = rows.getString(0), metric = rows.getString(1),
                                measuredAtEpochMillis = rows.getLong(2), value = rows.getDouble(3),
                                unit = rows.getString(4), sampleId = rows.getString(5).ifEmpty { null },
                                receivedAtEpochMillis = if (rows.isNull(6)) null else rows.getLong(6)
                            )))
                        }
                    }
                    canonicalReadingCount = canonicalReadings.records
                    metadata.put(CANONICAL_READINGS_ENTRY, entryMetadata(canonicalReadings))
                    progress.update(BackupProgress.Phase.WRITE, metadata.length(), entriesTotal)

                    val canonicalCursors = writeJsonLines(zip, CANONICAL_CURSORS_ENTRY) { emit ->
                        db.rawQuery(
                            "SELECT device_id, last_committed_ms FROM main.sync_cursors ORDER BY device_id", null
                        ).use { rows ->
                            while (rows.moveToNext()) emit(encodeCursor(
                                LocalSyncCursor(rows.getString(0), rows.getLong(1))
                            ))
                        }
                    }
                    canonicalCursorCount = canonicalCursors.records
                    metadata.put(CANONICAL_CURSORS_ENTRY, entryMetadata(canonicalCursors))
                    progress.update(BackupProgress.Phase.WRITE, metadata.length(), entriesTotal)

                    val canonicalSources = writeJsonLines(zip, CANONICAL_FIT_SOURCES_ENTRY) { emit ->
                        db.rawQuery(
                            """SELECT source_sha256, file_bytes, relative_path, validated_at_ms,
                                      decoder_version, profile_version
                               FROM main.fit_sources ORDER BY source_sha256""", null
                        ).use { rows ->
                            while (rows.moveToNext()) emit(encodeFitSource(StoredFitSource(
                                sha256 = rows.getString(0), fileBytes = rows.getLong(1),
                                relativePath = rows.getString(2), validatedAtEpochMillis = rows.getLong(3),
                                decoderVersion = rows.getString(4), profileVersion = rows.getString(5)
                            )))
                        }
                    }
                    canonicalFitSourceCount = canonicalSources.records
                    metadata.put(CANONICAL_FIT_SOURCES_ENTRY, entryMetadata(canonicalSources))
                    progress.update(BackupProgress.Phase.WRITE, metadata.length(), entriesTotal)

                    val canonicalProvenance = writeJsonLines(zip, CANONICAL_FIT_PROVENANCE_ENTRY) { emit ->
                        db.rawQuery(
                            """SELECT sample_id, device_id, metric, measured_at_ms, source_sha256,
                                      record_ordinal, global_message_number, field_number, quality, decoder_version
                               FROM main.fit_observation_provenance
                               ORDER BY source_sha256, record_ordinal, global_message_number, field_number""", null
                        ).use { rows ->
                            while (rows.moveToNext()) emit(encodeFitProvenance(StoredFitProvenance(
                                sampleId = rows.getString(0), deviceId = rows.getString(1), metric = rows.getString(2),
                                measuredAtEpochMillis = rows.getLong(3), sourceSha256 = rows.getString(4),
                                recordOrdinal = rows.getInt(5), globalMessageNumber = rows.getInt(6),
                                fieldNumber = rows.getInt(7), quality = rows.getString(8),
                                decoderVersion = rows.getString(9)
                            )))
                        }
                    }
                    canonicalFitProvenanceCount = canonicalProvenance.records
                    metadata.put(CANONICAL_FIT_PROVENANCE_ENTRY, entryMetadata(canonicalProvenance))
                    progress.update(BackupProgress.Phase.WRITE, metadata.length(), entriesTotal)

                    val canonicalDeviceMetadata = writeJsonLines(zip, CANONICAL_DEVICE_METADATA_ENTRY) { emit ->
                        db.rawQuery(
                            """SELECT source_sha256, record_ordinal, observed_at_ms, device_type_code,
                                      manufacturer_code, product_code, software_version, battery_voltage_volts,
                                      battery_status_code, battery_level_percent
                               FROM main.fit_device_metadata
                               ORDER BY observed_at_ms, source_sha256, record_ordinal""", null
                        ).use { rows ->
                            while (rows.moveToNext()) emit(encodeDeviceMetadata(FitHistoricalObservations.DeviceMetadata(
                                sourceFileSha256 = rows.getString(0),
                                recordOrdinal = rows.getInt(1),
                                timestampEpochMillis = if (rows.isNull(2)) null else rows.getLong(2),
                                deviceTypeCode = if (rows.isNull(3)) null else rows.getInt(3),
                                manufacturerCode = if (rows.isNull(4)) null else rows.getInt(4),
                                productCode = if (rows.isNull(5)) null else rows.getInt(5),
                                softwareVersion = if (rows.isNull(6)) null else rows.getDouble(6),
                                batteryVoltageVolts = if (rows.isNull(7)) null else rows.getDouble(7),
                                batteryStatusCode = if (rows.isNull(8)) null else rows.getInt(8),
                                batteryLevelPercent = if (rows.isNull(9)) null else rows.getInt(9)
                            )))
                        }
                    }
                    canonicalDeviceMetadataCount = canonicalDeviceMetadata.records
                    metadata.put(CANONICAL_DEVICE_METADATA_ENTRY, entryMetadata(canonicalDeviceMetadata))
                    progress.update(BackupProgress.Phase.WRITE, metadata.length(), entriesTotal)
                    val rawDeviceMetadata = writeJsonLines(zip, RAW_DEVICE_METADATA_ENTRY) { emit ->
                        db.rawQuery(
                            """SELECT source_sha256, file_bytes, verified_at_ms, parser_version,
                                      software_version, mass_storage_mode, transfer_direction_count
                               FROM main.raw_device_metadata ORDER BY verified_at_ms, source_sha256""",
                            null
                        ).use { rows ->
                            while (rows.moveToNext()) emit(encodeRawDeviceMetadata(RawDeviceMetadataRecord(
                                sourceSha256 = rows.getString(0),
                                fileBytes = rows.getLong(1),
                                verifiedAtEpochMillis = rows.getLong(2),
                                parserVersion = rows.getString(3),
                                softwareVersion = if (rows.isNull(4)) null else rows.getString(4),
                                massStorageMode = if (rows.isNull(5)) null else rows.getInt(5) == 1,
                                transferDirectionElementCount = rows.getInt(6)
                            )))
                        }
                    }
                    rawDeviceMetadataCount = rawDeviceMetadata.records
                    metadata.put(RAW_DEVICE_METADATA_ENTRY, entryMetadata(rawDeviceMetadata))
                    progress.update(BackupProgress.Phase.WRITE, metadata.length(), entriesTotal)
                    val canonicalUnmappedFields = writeJsonLines(zip, CANONICAL_UNMAPPED_FIT_FIELDS_ENTRY) { emit ->
                        db.rawQuery(
                            """SELECT source_sha256, global_message_number, field_kind, field_number,
                                      base_type_code, field_size, developer_data_index, classification,
                                      definition_count, observed_message_count, valid_element_count,
                                      invalid_element_count, decoder_version, profile_version, review_status
                               FROM main.fit_unmapped_fields
                               ORDER BY source_sha256, global_message_number, field_kind, field_number""",
                            null
                        ).use { rows ->
                            while (rows.moveToNext()) emit(encodeUnmappedFitField(StoredFitUnmappedField(
                                sourceSha256 = rows.getString(0),
                                globalMessageNumber = rows.getInt(1),
                                kind = StoredFitFieldKind.valueOf(rows.getString(2)),
                                fieldNumber = rows.getInt(3),
                                baseTypeCode = rows.getInt(4).takeUnless { it == -1 },
                                fieldSize = rows.getInt(5),
                                developerDataIndex = rows.getInt(6).takeUnless { it == -1 },
                                classification = rows.getString(7),
                                definitionCount = rows.getLong(8),
                                observedMessageCount = rows.getLong(9),
                                validElementCount = if (rows.isNull(10)) null else rows.getLong(10),
                                invalidElementCount = if (rows.isNull(11)) null else rows.getLong(11),
                                decoderVersion = rows.getString(12),
                                profileVersion = rows.getString(13),
                                status = FitFieldInventoryStatus.valueOf(rows.getString(14))
                            )))
                        }
                    }
                    metadata.put(CANONICAL_UNMAPPED_FIT_FIELDS_ENTRY, entryMetadata(canonicalUnmappedFields))
                    progress.update(BackupProgress.Phase.WRITE, metadata.length(), entriesTotal)
                    val generations = writeJsonLines(zip, CANONICAL_FIT_GENERATIONS_ENTRY) { emit ->
                        db.rawQuery(
                            """SELECT generation_id, source_sha256, decoder_version, profile_version,
                                      output_digest, output_count, completed_at_ms, decoded_field_count
                               FROM main.fit_decoder_generations ORDER BY generation_id""", null
                        ).use { rows ->
                            while (rows.moveToNext()) emit(encodeFitGeneration(StoredFitDecoderGeneration(
                                generationId = rows.getString(0),
                                sourceSha256 = rows.getString(1),
                                decoderVersion = rows.getString(2),
                                profileVersion = rows.getString(3),
                                outputDigest = rows.getString(4),
                                outputCount = rows.getLong(5),
                                completedAtEpochMillis = rows.getLong(6),
                                decodedFieldCount = rows.getLong(7)
                            )))
                        }
                    }
                    canonicalFitGenerationCount = generations.records
                    metadata.put(CANONICAL_FIT_GENERATIONS_ENTRY, entryMetadata(generations))
                    progress.update(BackupProgress.Phase.WRITE, metadata.length(), entriesTotal)
                    val generationObservations = writeJsonLines(
                        zip, CANONICAL_FIT_GENERATION_OBSERVATIONS_ENTRY
                    ) { emit ->
                        db.rawQuery(
                            """SELECT generation_id, sample_id, device_id, metric, measured_at_ms,
                                      value, unit, received_at_ms, record_ordinal, global_message_number,
                                      field_number, quality, timestamp_origin
                               FROM main.fit_generation_observations
                               ORDER BY generation_id, sample_id""", null
                        ).use { rows ->
                            while (rows.moveToNext()) emit(encodeFitGenerationObservation(
                                StoredFitGenerationObservation(
                                    generationId = rows.getString(0),
                                    sampleId = rows.getString(1),
                                    deviceId = rows.getString(2),
                                    metric = rows.getString(3),
                                    measuredAtEpochMillis = rows.getLong(4),
                                    value = rows.getDouble(5),
                                    unit = rows.getString(6),
                                    receivedAtEpochMillis = if (rows.isNull(7)) null else rows.getLong(7),
                                    recordOrdinal = rows.getInt(8),
                                    globalMessageNumber = rows.getInt(9),
                                    fieldNumber = rows.getInt(10),
                                    quality = rows.getString(11),
                                    timestampOrigin = rows.getString(12)
                                )
                            ))
                        }
                    }
                    canonicalFitGenerationObservationCount = generationObservations.records
                    metadata.put(
                        CANONICAL_FIT_GENERATION_OBSERVATIONS_ENTRY,
                        entryMetadata(generationObservations)
                    )
                    val generationFields = writeJsonLines(zip, CANONICAL_FIT_GENERATION_FIELDS_ENTRY) { emit ->
                        db.rawQuery(
                            """SELECT generation_id, record_ordinal, global_message_number, field_kind,
                                      field_number, base_type_code, field_size, developer_data_index,
                                      big_endian, raw_bytes, measured_at_ms, timestamp_origin
                               FROM main.fit_generation_fields
                               ORDER BY generation_id, record_ordinal, field_kind, field_number,
                                        developer_data_index""", null
                        ).use { rows ->
                            while (rows.moveToNext()) emit(encodeFitGenerationField(
                                StoredFitGenerationField(
                                    rows.getString(0), rows.getInt(1), rows.getInt(2), rows.getString(3),
                                    rows.getInt(4), if (rows.isNull(5)) null else rows.getInt(5),
                                    rows.getInt(6), rows.getInt(7).takeIf { it >= 0 },
                                    rows.getInt(8) != 0, rows.getBlob(9),
                                    if (rows.isNull(10)) null else rows.getLong(10), rows.getString(11)
                                )
                            ))
                        }
                    }
                    canonicalFitGenerationFieldCount = generationFields.records
                    metadata.put(CANONICAL_FIT_GENERATION_FIELDS_ENTRY, entryMetadata(generationFields))
                    progress.update(BackupProgress.Phase.WRITE, metadata.length(), entriesTotal)
                    val activeGenerations = writeJsonLines(zip, CANONICAL_ACTIVE_FIT_GENERATIONS_ENTRY) { emit ->
                        db.rawQuery(
                            """SELECT source_sha256, generation_id FROM main.fit_active_generations
                               ORDER BY source_sha256""", null
                        ).use { rows ->
                            while (rows.moveToNext()) emit(encodeActiveFitGeneration(
                                StoredActiveFitGeneration(rows.getString(0), rows.getString(1))
                            ))
                        }
                    }
                    canonicalActiveFitGenerationCount = activeGenerations.records
                    metadata.put(
                        CANONICAL_ACTIVE_FIT_GENERATIONS_ENTRY,
                        entryMetadata(activeGenerations)
                    )
                    val reviewCandidates = writeJsonLines(zip, CANONICAL_FIT_REVIEW_CANDIDATES_ENTRY) { emit ->
                        db.rawQuery(
                            """SELECT sample_id, device_id, metric, measured_at_ms, value, unit,
                                      source_sha256, record_ordinal, global_message_number, field_number,
                                      quality, timestamp_origin, decoder_version
                               FROM main.fit_review_candidates
                               ORDER BY source_sha256, record_ordinal, global_message_number, field_number""",
                            null
                        ).use { rows ->
                            while (rows.moveToNext()) emit(encodeFitReviewCandidate(StoredFitReviewCandidate(
                                sampleId = rows.getString(0),
                                deviceId = rows.getString(1),
                                metric = rows.getString(2),
                                measuredAtEpochMillis = if (rows.isNull(3)) null else rows.getLong(3),
                                value = rows.getDouble(4),
                                unit = rows.getString(5),
                                sourceSha256 = rows.getString(6),
                                recordOrdinal = rows.getInt(7),
                                globalMessageNumber = rows.getInt(8),
                                fieldNumber = rows.getInt(9),
                                quality = rows.getString(10),
                                timestampOrigin = rows.getString(11),
                                decoderVersion = rows.getString(12)
                            )))
                        }
                    }
                    canonicalFitReviewCandidateCount = reviewCandidates.records
                    metadata.put(CANONICAL_FIT_REVIEW_CANDIDATES_ENTRY, entryMetadata(reviewCandidates))
                    progress.update(BackupProgress.Phase.WRITE, metadata.length(), entriesTotal)
                    val legacyReviewCandidates = writeJsonLines(zip, LEGACY_FIT_REVIEW_CANDIDATES_ENTRY) { emit ->
                        db.rawQuery(
                            """SELECT sample_id, device_id, metric, measured_at_ms, value, unit,
                                      source_sha256, record_ordinal, global_message_number, field_number,
                                      quality, timestamp_origin, decoder_version
                               FROM legacy.fit_review_candidates
                               ORDER BY source_sha256, record_ordinal, global_message_number, field_number""",
                            null
                        ).use { rows ->
                            while (rows.moveToNext()) emit(encodeFitReviewCandidate(StoredFitReviewCandidate(
                                sampleId = rows.getString(0),
                                deviceId = rows.getString(1),
                                metric = rows.getString(2),
                                measuredAtEpochMillis = if (rows.isNull(3)) null else rows.getLong(3),
                                value = rows.getDouble(4),
                                unit = rows.getString(5),
                                sourceSha256 = rows.getString(6),
                                recordOrdinal = rows.getInt(7),
                                globalMessageNumber = rows.getInt(8),
                                fieldNumber = rows.getInt(9),
                                quality = rows.getString(10),
                                timestampOrigin = rows.getString(11),
                                decoderVersion = rows.getString(12)
                            )))
                        }
                    }
                    legacyFitReviewCandidateCount = legacyReviewCandidates.records
                    metadata.put(LEGACY_FIT_REVIEW_CANDIDATES_ENTRY, entryMetadata(legacyReviewCandidates))
                    progress.update(BackupProgress.Phase.WRITE, metadata.length(), entriesTotal)

                    val liveMetadata = writeJsonLines(zip, LIVE_DEVICE_METADATA_ENTRY) { emit ->
                        db.rawQuery(
                            """SELECT field_key, value_text, unit, source, observed_at_ms
                               FROM main.live_device_metadata ORDER BY observed_at_ms, field_key, id""", null
                        ).use { rows ->
                            while (rows.moveToNext()) {
                                val item = JSONObject()
                                    .put("field_key", rows.getString(0))
                                    .put("value_text", rows.getString(1))
                                    .put("source", rows.getString(3))
                                    .put("observed_at_ms", rows.getLong(4))
                                if (rows.isNull(2)) item.put("unit", JSONObject.NULL) else item.put("unit", rows.getString(2))
                                emit(item.toString())
                            }
                        }
                    }
                    liveDeviceMetadataCount = liveMetadata.records
                    metadata.put(LIVE_DEVICE_METADATA_ENTRY, entryMetadata(liveMetadata))
                    progress.update(BackupProgress.Phase.WRITE, metadata.length(), entriesTotal)
                    val originalFiles = writeJsonLines(zip, DownloadedFileArchive.BACKUP_ENTRY) { emit ->
                        DownloadedFileArchive.export(db, emit)
                    }
                    metadata.put(DownloadedFileArchive.BACKUP_ENTRY, entryMetadata(originalFiles))
                    progress.update(BackupProgress.Phase.WRITE, metadata.length(), entriesTotal)
                    val importRuns = writeJsonLines(zip, IMPORT_RUNS_ENTRY) { emit ->
                        db.rawQuery("SELECT * FROM main.import_runs ORDER BY import_run_id", null).use { rows ->
                            while (rows.moveToNext()) emit(encodeAuditRow(rows))
                        }
                    }
                    metadata.put(IMPORT_RUNS_ENTRY, entryMetadata(importRuns))
                    progress.update(BackupProgress.Phase.WRITE, metadata.length(), entriesTotal)
                    val importErrors = writeJsonLines(zip, IMPORT_ERRORS_ENTRY) { emit ->
                        db.rawQuery("SELECT * FROM main.import_errors ORDER BY import_run_id, id", null).use { rows ->
                            while (rows.moveToNext()) emit(encodeAuditRow(rows))
                        }
                    }
                    metadata.put(IMPORT_ERRORS_ENTRY, entryMetadata(importErrors))
                    progress.update(BackupProgress.Phase.WRITE, metadata.length(), entriesTotal)
                    val statusEvents = writeJsonLines(zip, OBSERVATION_STATUS_EVENTS_ENTRY) { emit ->
                        db.rawQuery("SELECT * FROM main.observation_status_events ORDER BY id", null).use { rows ->
                            while (rows.moveToNext()) emit(encodeAuditRow(rows))
                        }
                    }
                    metadata.put(OBSERVATION_STATUS_EVENTS_ENTRY, entryMetadata(statusEvents))
                    progress.update(BackupProgress.Phase.WRITE, metadata.length(), entriesTotal)
                    val settings = writeJsonLines(zip, AppSettingsBackup.ENTRY) { emit -> AppSettingsBackup.export(context, emit) }
                    metadata.put(AppSettingsBackup.ENTRY, entryMetadata(settings))
                    progress.update(BackupProgress.Phase.WRITE, metadata.length(), entriesTotal)
                    require(
                        events.bytes + readings.bytes + cursors.bytes + sources.bytes + provenance.bytes +
                            canonicalReadings.bytes + canonicalCursors.bytes + canonicalSources.bytes +
                            canonicalProvenance.bytes + canonicalDeviceMetadata.bytes +
                            rawDeviceMetadata.bytes + canonicalUnmappedFields.bytes +
                            generations.bytes + generationObservations.bytes + activeGenerations.bytes +
                            generationFields.bytes + reviewCandidates.bytes + legacyReviewCandidates.bytes +
                            liveMetadata.bytes + originalFiles.bytes + importRuns.bytes +
                            importErrors.bytes + statusEvents.bytes + settings.bytes <= MAX_UNCOMPRESSED_BYTES
                    ) {
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
        return DatabaseBackupReport(
            now, eventCount, readingCount, cursorCount, fitSourceCount, fitProvenanceCount,
            canonicalReadingCount, canonicalCursorCount, canonicalFitSourceCount,
            canonicalFitProvenanceCount, canonicalDeviceMetadataCount, rawDeviceMetadataCount,
            canonicalFitGenerationCount, canonicalFitGenerationObservationCount,
            canonicalFitGenerationFieldCount,
            canonicalActiveFitGenerationCount, canonicalFitReviewCandidateCount,
            legacyFitReviewCandidateCount, liveDeviceMetadataCount
        )
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
        val allowed = BACKUP_V12_ENTRIES + MANIFEST_ENTRY
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
                        require(entryBytes <= MAX_ENTRY_BYTES && totalBytes <= MAX_UNCOMPRESSED_BYTES) {
                            "Backup ist zu groß"
                        }
                        out.write(buffer, 0, count)
                    }
                }
                zip.closeEntry()
                files[entry.name] = target
            }
        }
        require(MANIFEST_ENTRY in files) { "Backup-Manifest fehlt" }
        require(files.getValue(MANIFEST_ENTRY).length() <= MAX_MANIFEST_BYTES) { "Backup-Manifest ist zu groß" }
        return files
    }

    /** Generic row encoding for additive audit tables (no blobs, no guessed semantics). */
    private fun encodeAuditRow(rows: Cursor): String {
        val json = JSONObject()
        for (index in 0 until rows.columnCount) {
            val key = rows.getColumnName(index)
            if (rows.isNull(index)) {
                json.put(key, JSONObject.NULL)
            } else {
                when (rows.getType(index)) {
                    Cursor.FIELD_TYPE_INTEGER -> json.put(key, rows.getLong(index))
                    Cursor.FIELD_TYPE_FLOAT -> json.put(key, rows.getDouble(index))
                    Cursor.FIELD_TYPE_STRING -> json.put(key, rows.getString(index))
                    else -> json.put(key, JSONObject.NULL)
                }
            }
        }
        return json.toString()
    }

    private fun restoreAuditRows(db: SQLiteDatabase, lines: Sequence<String>, table: String) {
        lines.forEach { line ->
            val json = JSONObject(line)
            val values = ContentValues()
            json.keys().forEach { key ->
                val value = json.get(key)
                if (value == JSONObject.NULL) values.putNull(key)
                else when (value) {
                    is Number -> if (value is Double || value is Float)
                        values.put(key, value.toDouble()) else values.put(key, value.toLong())
                    is Boolean -> values.put(key, if (value) 1 else 0)
                    is String -> values.put(key, value)
                    else -> error("Unsupported audit value in $table")
                }
            }
            db.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        }
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
                    FIT_SOURCES_ENTRY -> parseFitSource(line)
                    FIT_PROVENANCE_ENTRY -> parseFitProvenance(line)
                    CANONICAL_READINGS_ENTRY -> parseReading(line)
                    CANONICAL_CURSORS_ENTRY -> parseCursor(line)
                    CANONICAL_FIT_SOURCES_ENTRY -> parseFitSource(line)
                    CANONICAL_FIT_PROVENANCE_ENTRY -> parseFitProvenance(line)
                    CANONICAL_DEVICE_METADATA_ENTRY -> parseDeviceMetadata(line)
                    RAW_DEVICE_METADATA_ENTRY -> parseRawDeviceMetadata(line)
                    CANONICAL_UNMAPPED_FIT_FIELDS_ENTRY -> parseUnmappedFitField(line)
                    CANONICAL_FIT_GENERATIONS_ENTRY -> parseFitGeneration(line)
                    CANONICAL_FIT_GENERATION_OBSERVATIONS_ENTRY -> parseFitGenerationObservation(line)
                    CANONICAL_FIT_GENERATION_FIELDS_ENTRY -> parseFitGenerationField(line)
                    CANONICAL_ACTIVE_FIT_GENERATIONS_ENTRY -> parseActiveFitGeneration(line)
                    CANONICAL_FIT_REVIEW_CANDIDATES_ENTRY,
                    LEGACY_FIT_REVIEW_CANDIDATES_ENTRY -> parseFitReviewCandidate(line)
                    LIVE_DEVICE_METADATA_ENTRY -> parseLiveDeviceMetadata(line)
                    IMPORT_RUNS_ENTRY, IMPORT_ERRORS_ENTRY, OBSERVATION_STATUS_EVENTS_ENTRY -> JSONObject(line)
                    AppSettingsBackup.ENTRY -> AppSettingsBackup.parse(line)
                    DownloadedFileArchive.BACKUP_ENTRY -> DownloadedFileArchive.parse(line)
                }
                count++
                require(count <= MAX_RECORDS) { "Zu viele Einträge in $name" }
            }
        }
        return count
    }

    /** Validate source/reading foreign-key relationships before restore confirmation. */
    private fun validateFitReferences(
        directory: File,
        readingsEntry: String,
        sourcesEntry: String,
        provenanceEntry: String
    ) {
        val stagingFile = File(directory, ".fit-reference-validation.db")
        val db = SQLiteDatabase.openOrCreateDatabase(stagingFile, null)
        try {
            db.setForeignKeyConstraintsEnabled(true)
            db.execSQL(
                """CREATE TABLE sources (
                   source_sha256 TEXT PRIMARY KEY NOT NULL
                )"""
            )
            db.execSQL(
                """CREATE TABLE readings (
                   device_id TEXT NOT NULL, metric TEXT NOT NULL, measured_at_ms INTEGER NOT NULL,
                   sample_id TEXT NOT NULL, PRIMARY KEY(device_id, metric, measured_at_ms, sample_id)
                )"""
            )
            db.execSQL(
                """CREATE TABLE provenance (
                   sample_id TEXT PRIMARY KEY NOT NULL, device_id TEXT NOT NULL, metric TEXT NOT NULL,
                   measured_at_ms INTEGER NOT NULL, source_sha256 TEXT NOT NULL,
                   FOREIGN KEY(source_sha256) REFERENCES sources(source_sha256),
                   FOREIGN KEY(device_id, metric, measured_at_ms, sample_id)
                       REFERENCES readings(device_id, metric, measured_at_ms, sample_id)
                )"""
            )
            db.beginTransaction()
            try {
                File(directory, sourcesEntry).bufferedReader(Charsets.UTF_8).useLines { lines ->
                    lines.forEach { line ->
                        val source = parseFitSource(line)
                        db.execSQL("INSERT INTO sources(source_sha256) VALUES(?)", arrayOf(source.sha256))
                    }
                }
                File(directory, readingsEntry).bufferedReader(Charsets.UTF_8).useLines { lines ->
                    lines.forEach { line ->
                        val reading = parseReading(line)
                        db.execSQL(
                            """INSERT INTO readings(device_id, metric, measured_at_ms, sample_id)
                               VALUES(?, ?, ?, ?)""",
                            arrayOf<Any?>(reading.deviceId, reading.metric, reading.measuredAtEpochMillis,
                                reading.sampleId ?: "")
                        )
                    }
                }
                File(directory, provenanceEntry).bufferedReader(Charsets.UTF_8).useLines { lines ->
                    lines.forEach { line ->
                        val item = parseFitProvenance(line)
                        db.execSQL(
                            """INSERT INTO provenance
                               (sample_id, device_id, metric, measured_at_ms, source_sha256)
                               VALUES(?, ?, ?, ?, ?)""",
                            arrayOf<Any?>(item.sampleId, item.deviceId, item.metric, item.measuredAtEpochMillis,
                                item.sourceSha256)
                        )
                    }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } finally {
            db.close()
            stagingFile.delete()
            File(stagingFile.path + "-journal").delete()
        }
    }

    private fun validateDeviceMetadataReferences(directory: File) {
        val stagingFile = File(directory, ".device-metadata-reference-validation.db")
        val db = SQLiteDatabase.openOrCreateDatabase(stagingFile, null)
        try {
            db.setForeignKeyConstraintsEnabled(true)
            db.execSQL("CREATE TABLE sources(source_sha256 TEXT PRIMARY KEY NOT NULL)")
            db.execSQL(
                """CREATE TABLE metadata(
                   source_sha256 TEXT NOT NULL REFERENCES sources(source_sha256),
                   record_ordinal INTEGER NOT NULL,
                   PRIMARY KEY(source_sha256, record_ordinal)
                )"""
            )
            db.beginTransaction()
            try {
                File(directory, CANONICAL_FIT_SOURCES_ENTRY).bufferedReader(Charsets.UTF_8).useLines { lines ->
                    lines.forEach { line ->
                        db.execSQL("INSERT INTO sources(source_sha256) VALUES(?)", arrayOf(parseFitSource(line).sha256))
                    }
                }
                File(directory, CANONICAL_DEVICE_METADATA_ENTRY).bufferedReader(Charsets.UTF_8).useLines { lines ->
                    lines.forEach { line ->
                        val item = parseDeviceMetadata(line)
                        db.execSQL(
                            "INSERT INTO metadata(source_sha256, record_ordinal) VALUES(?, ?)",
                            arrayOf<Any?>(item.sourceFileSha256, item.recordOrdinal)
                        )
                    }
                }
                db.rawQuery("PRAGMA foreign_key_check", null).use {
                    require(!it.moveToFirst()) { "FIT-Gerätemetadaten verweisen auf eine fehlende Quelle" }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } finally {
            db.close()
            stagingFile.delete()
            File(stagingFile.path + "-journal").delete()
        }
    }

    private fun validateRawDeviceMetadataReferences(directory: File) {
        val stagingFile = File(directory, ".raw-device-metadata-validation.db")
        val db = SQLiteDatabase.openOrCreateDatabase(stagingFile, null)
        try {
            db.execSQL("CREATE TABLE metadata(source_sha256 TEXT PRIMARY KEY NOT NULL)")
            db.beginTransaction()
            try {
                File(directory, RAW_DEVICE_METADATA_ENTRY).bufferedReader(Charsets.UTF_8).useLines { lines ->
                    lines.forEach { line ->
                        val item = parseRawDeviceMetadata(line)
                        db.execSQL("INSERT INTO metadata(source_sha256) VALUES(?)", arrayOf(item.sourceSha256))
                    }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } finally {
            db.close()
            stagingFile.delete()
            File(stagingFile.path + "-journal").delete()
        }
    }

    private fun validateUnmappedFitFieldReferences(directory: File) {
        val sources = hashSetOf<String>()
        File(directory, CANONICAL_FIT_SOURCES_ENTRY).bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.forEach { sources += parseFitSource(it).sha256 }
        }
        File(directory, CANONICAL_UNMAPPED_FIT_FIELDS_ENTRY).bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.forEach { line ->
                val field = parseUnmappedFitField(line)
                require(field.sourceSha256 in sources) { "FIT-Feldinventur verweist auf eine fehlende Quelle" }
            }
        }
    }

    private fun validateFitReviewCandidateReferences(
        directory: File,
        sourcesEntry: String,
        candidatesEntry: String
    ) {
        val sources = hashSetOf<String>()
        File(directory, sourcesEntry).bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.forEach { sources += parseFitSource(it).sha256 }
        }
        val sampleIds = hashSetOf<String>()
        val sourceFields = hashSetOf<String>()
        File(directory, candidatesEntry).bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.forEach { line ->
                val candidate = parseFitReviewCandidate(line)
                require(candidate.sourceSha256 in sources) {
                    "Untrusted FIT-Kandidat verweist auf eine fehlende Quelle"
                }
                require(sampleIds.add(candidate.sampleId)) {
                    "Doppelte sample_id bei untrusted FIT-Kandidaten"
                }
                val sourceField = listOf(
                    candidate.sourceSha256, candidate.recordOrdinal.toString(),
                    candidate.globalMessageNumber.toString(), candidate.fieldNumber.toString()
                ).joinToString(":")
                require(sourceFields.add(sourceField)) {
                    "Doppeltes FIT-Quellfeld bei untrusted FIT-Kandidaten"
                }
            }
        }
    }

    /** Validate the complete generation graph and each immutable output digest before restore. */
    private fun validateGenerationReferences(directory: File) {
        val stagingFile = File(directory, ".fit-generation-validation.db")
        val db = SQLiteDatabase.openOrCreateDatabase(stagingFile, null)
        try {
            db.setForeignKeyConstraintsEnabled(true)
            db.execSQL("CREATE TABLE sources(source_sha256 TEXT PRIMARY KEY NOT NULL)")
            db.execSQL(
                """CREATE TABLE generations(
                   generation_id TEXT PRIMARY KEY NOT NULL,
                   source_sha256 TEXT NOT NULL REFERENCES sources(source_sha256),
                    decoder_version TEXT NOT NULL, profile_version TEXT NOT NULL,
                    output_digest TEXT NOT NULL, output_count INTEGER NOT NULL, decoded_field_count INTEGER NOT NULL,
                   UNIQUE(generation_id, source_sha256),
                   UNIQUE(source_sha256, decoder_version, profile_version)
                )"""
            )
            db.execSQL("""CREATE TABLE generation_fields(
                generation_id TEXT NOT NULL REFERENCES generations(generation_id),
                record_ordinal INTEGER NOT NULL, global_message_number INTEGER NOT NULL,
                field_kind TEXT NOT NULL, field_number INTEGER NOT NULL, base_type_code INTEGER,
                field_size INTEGER NOT NULL, developer_data_index INTEGER, big_endian INTEGER NOT NULL,
                raw_bytes BLOB NOT NULL, measured_at_ms INTEGER, timestamp_origin TEXT NOT NULL,
                PRIMARY KEY(generation_id, record_ordinal, field_kind, field_number, developer_data_index)
            )""")
            db.execSQL(
                """CREATE TABLE generation_observations(
                   generation_id TEXT NOT NULL REFERENCES generations(generation_id),
                   sample_id TEXT NOT NULL, device_id TEXT NOT NULL, metric TEXT NOT NULL,
                   measured_at_ms INTEGER NOT NULL, value REAL NOT NULL, unit TEXT NOT NULL,
                   received_at_ms INTEGER, record_ordinal INTEGER NOT NULL,
                   global_message_number INTEGER NOT NULL, field_number INTEGER NOT NULL,
                   quality TEXT NOT NULL, timestamp_origin TEXT NOT NULL,
                   PRIMARY KEY(generation_id, sample_id)
                )"""
            )
            db.execSQL(
                """CREATE TABLE active_generations(
                   source_sha256 TEXT PRIMARY KEY NOT NULL REFERENCES sources(source_sha256),
                   generation_id TEXT NOT NULL UNIQUE,
                   FOREIGN KEY(generation_id, source_sha256)
                     REFERENCES generations(generation_id, source_sha256)
                )"""
            )
            db.beginTransaction()
            try {
                File(directory, CANONICAL_FIT_SOURCES_ENTRY).bufferedReader(Charsets.UTF_8).useLines { lines ->
                    lines.forEach { db.execSQL(
                        "INSERT INTO sources(source_sha256) VALUES(?)",
                        arrayOf(parseFitSource(it).sha256)
                    ) }
                }
                File(directory, CANONICAL_FIT_GENERATIONS_ENTRY).bufferedReader(Charsets.UTF_8).useLines { lines ->
                    lines.forEach { line ->
                        val generation = parseFitGeneration(line)
                        db.execSQL(
                            """INSERT INTO generations(generation_id, source_sha256, decoder_version,
                               profile_version, output_digest, output_count, decoded_field_count)
                               VALUES(?, ?, ?, ?, ?, ?, ?)""",
                            arrayOf<Any?>(
                                generation.generationId, generation.sourceSha256, generation.decoderVersion,
                                generation.profileVersion, generation.outputDigest, generation.outputCount,
                                generation.decodedFieldCount
                            )
                        )
                    }
                }
                File(directory, CANONICAL_FIT_GENERATION_OBSERVATIONS_ENTRY)
                    .bufferedReader(Charsets.UTF_8).useLines { lines ->
                        lines.forEach { line ->
                            val row = parseFitGenerationObservation(line)
                            db.execSQL(
                                """INSERT INTO generation_observations
                                   (generation_id, sample_id, device_id, metric, measured_at_ms, value, unit,
                                    received_at_ms, record_ordinal, global_message_number, field_number,
                                    quality, timestamp_origin)
                                   VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                                arrayOf<Any?>(
                                    row.generationId, row.sampleId, row.deviceId, row.metric,
                                    row.measuredAtEpochMillis, row.value, row.unit, row.receivedAtEpochMillis,
                                    row.recordOrdinal, row.globalMessageNumber, row.fieldNumber,
                                    row.quality, row.timestampOrigin
                                )
                            )
                        }
                    }
                if (File(directory, CANONICAL_FIT_GENERATION_FIELDS_ENTRY).exists()) {
                    File(directory, CANONICAL_FIT_GENERATION_FIELDS_ENTRY)
                        .bufferedReader(Charsets.UTF_8).useLines { lines ->
                            lines.forEach { line ->
                                val f = parseFitGenerationField(line)
                                db.execSQL(
                                    """INSERT INTO generation_fields VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                                    arrayOf<Any?>(
                                        f.generationId, f.recordOrdinal, f.globalMessageNumber,
                                        f.fieldKind, f.fieldNumber, f.baseTypeCode, f.fieldSize,
                                        f.developerDataIndex ?: -1, if (f.bigEndian) 1 else 0,
                                        f.rawBytes, f.measuredAtEpochMillis, f.timestampOrigin
                                    )
                                )
                            }
                        }
                }
                File(directory, CANONICAL_ACTIVE_FIT_GENERATIONS_ENTRY)
                    .bufferedReader(Charsets.UTF_8).useLines { lines ->
                        lines.forEach { line ->
                            val active = parseActiveFitGeneration(line)
                            db.execSQL(
                                "INSERT INTO active_generations(source_sha256, generation_id) VALUES(?, ?)",
                                arrayOf(active.sourceSha256, active.generationId)
                            )
                        }
                    }

                db.rawQuery(
                    "SELECT generation_id, output_digest, output_count, decoded_field_count FROM generations ORDER BY generation_id",
                    null
                ).use { generations ->
                    while (generations.moveToNext()) {
                        val generationId = generations.getString(0)
                        val expectedDigest = generations.getString(1)
                        val expectedCount = generations.getLong(2)
                        val expectedFieldCount = generations.getLong(3)
                        val accumulator = FitDecoderGeneration.OutputDigest()
                        var actualCount = 0L
                        db.rawQuery(
                            """SELECT generation_id, sample_id, device_id, metric, measured_at_ms,
                                      value, unit, received_at_ms, record_ordinal, global_message_number,
                                      field_number, quality, timestamp_origin
                               FROM generation_observations WHERE generation_id = ? ORDER BY sample_id""",
                            arrayOf(generationId)
                        ).use { rows ->
                            while (rows.moveToNext()) {
                                accumulator.add(StoredFitGenerationObservation(
                                    generationId = rows.getString(0), sampleId = rows.getString(1),
                                    deviceId = rows.getString(2), metric = rows.getString(3),
                                    measuredAtEpochMillis = rows.getLong(4), value = rows.getDouble(5),
                                    unit = rows.getString(6),
                                    receivedAtEpochMillis = if (rows.isNull(7)) null else rows.getLong(7),
                                    recordOrdinal = rows.getInt(8), globalMessageNumber = rows.getInt(9),
                                    fieldNumber = rows.getInt(10), quality = rows.getString(11),
                                    timestampOrigin = rows.getString(12)
                                ))
                                actualCount++
                            }
                        }
                        val fields = mutableListOf<StoredFitGenerationField>()
                        db.rawQuery(
                            """SELECT generation_id, record_ordinal, global_message_number, field_kind,
                                      field_number, base_type_code, field_size, developer_data_index,
                                      big_endian, raw_bytes, measured_at_ms, timestamp_origin
                               FROM generation_fields WHERE generation_id = ?
                               ORDER BY record_ordinal, field_kind, field_number, developer_data_index""",
                            arrayOf(generationId)
                        ).use { rows ->
                            while (rows.moveToNext()) fields += StoredFitGenerationField(
                                rows.getString(0), rows.getInt(1), rows.getInt(2), rows.getString(3),
                                rows.getInt(4), if (rows.isNull(5)) null else rows.getInt(5),
                                rows.getInt(6), rows.getInt(7).takeIf { it >= 0 }, rows.getInt(8) != 0,
                                rows.getBlob(9), if (rows.isNull(10)) null else rows.getLong(10),
                                rows.getString(11)
                            )
                        }
                        fields.forEach(accumulator::add)
                        require(actualCount == expectedCount && fields.size.toLong() == expectedFieldCount &&
                            accumulator.finish() == expectedDigest) {
                            "FIT-Decodergeneration ist unvollständig oder der Output-Digest stimmt nicht"
                        }
                    }
                }
                db.rawQuery("PRAGMA foreign_key_check", null).use {
                    require(!it.moveToFirst()) { "FIT-Decodergenerationen enthalten verwaiste Referenzen" }
                }
                db.rawQuery(
                    """SELECT COUNT(*) FROM sources s
                       LEFT JOIN active_generations a ON a.source_sha256 = s.source_sha256
                       WHERE a.source_sha256 IS NULL""", null
                ).use {
                    require(it.moveToFirst() && it.getLong(0) == 0L) {
                        "Mindestens eine FIT-Quelle hat keine aktive Decodergeneration"
                    }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } finally {
            db.close()
            stagingFile.delete()
            File(stagingFile.path + "-journal").delete()
        }
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

    private fun parseFitSource(line: String): StoredFitSource {
        val item = JSONObject(line)
        val source = StoredFitSource(
            sha256 = item.getString("sha256"),
            fileBytes = item.getLong("file_bytes"),
            relativePath = item.getString("relative_path"),
            validatedAtEpochMillis = item.getLong("validated_at_ms"),
            decoderVersion = item.getString("decoder_version"),
            profileVersion = item.getString("profile_version")
        )
        require(source.sha256.matches(Regex("[0-9a-f]{64}")) && source.fileBytes > 0 &&
            source.validatedAtEpochMillis >= 0 &&
            source.relativePath == "verified_fit/${source.sha256}.fit" &&
            source.decoderVersion.isNotBlank() && source.profileVersion.isNotBlank()) {
            "Ungültige FIT-Quellmetadaten im Backup"
        }
        return source
    }

    private fun parseFitGeneration(line: String): StoredFitDecoderGeneration {
        val item = JSONObject(line)
        val generation = StoredFitDecoderGeneration(
            generationId = item.getString("generation_id"),
            sourceSha256 = item.getString("source_sha256"),
            decoderVersion = item.getString("decoder_version"),
            profileVersion = item.getString("profile_version"),
            outputDigest = item.getString("output_digest"),
            outputCount = item.getLong("output_count"),
            completedAtEpochMillis = item.getLong("completed_at_ms"),
            decodedFieldCount = item.optLong("decoded_field_count", 0L)
        )
        require(
            generation.generationId.matches(Regex("[0-9a-f]{64}")) &&
                generation.sourceSha256.matches(Regex("[0-9a-f]{64}")) &&
                generation.generationId == FitDecoderGeneration.id(
                    generation.sourceSha256, generation.decoderVersion, generation.profileVersion
                ) &&
                generation.decoderVersion.matches(Regex("[A-Za-z0-9._+-]{1,32}")) &&
                generation.profileVersion.matches(Regex("[A-Za-z0-9._+-]{1,32}")) &&
                generation.outputDigest.matches(Regex("[0-9a-f]{64}")) &&
                generation.outputCount in 0..MAX_RECORDS &&
                generation.decodedFieldCount in 0..MAX_RECORDS &&
                generation.completedAtEpochMillis >= 0
        ) { "Ungültige FIT-Decodergeneration im Backup" }
        return generation
    }

    private fun parseFitGenerationObservation(line: String): StoredFitGenerationObservation {
        val item = JSONObject(line)
        val row = StoredFitGenerationObservation(
            generationId = item.getString("generation_id"),
            sampleId = item.getString("sample_id"),
            deviceId = item.getString("device_id"),
            metric = item.getString("metric"),
            measuredAtEpochMillis = item.getLong("measured_at_ms"),
            value = item.getDouble("value"),
            unit = item.getString("unit"),
            receivedAtEpochMillis = if (!item.has("received_at_ms") || item.isNull("received_at_ms"))
                null else item.getLong("received_at_ms"),
            recordOrdinal = item.getInt("record_ordinal"),
            globalMessageNumber = item.getInt("global_message_number"),
            fieldNumber = item.getInt("field_number"),
            quality = item.getString("quality"),
            timestampOrigin = item.getString("timestamp_origin")
        )
        require(
            row.generationId.matches(Regex("[0-9a-f]{64}")) &&
                row.sampleId.isNotBlank() && row.deviceId.isNotBlank() && row.metric.isNotBlank() &&
                row.measuredAtEpochMillis >= 0 && row.value.isFinite() && row.unit.isNotBlank() &&
                (row.receivedAtEpochMillis == null || row.receivedAtEpochMillis >= 0) &&
                row.recordOrdinal >= 0 && row.globalMessageNumber >= 0 && row.fieldNumber >= 0 &&
                row.quality == "PROFILE_VALID" &&
                row.timestampOrigin in setOf(
                    "DIRECT", "RECONSTRUCTED_TS16", "RECONSTRUCTED_COMPRESSED", "UNKNOWN"
                )
        ) { "Ungültige FIT-Beobachtung in einer Decodergeneration" }
        return row
    }

    private fun parseFitGenerationField(line: String): StoredFitGenerationField {
        val item = JSONObject(line)
        val raw = Base64.getDecoder().decode(item.getString("raw_bytes"))
        val field = StoredFitGenerationField(
            generationId = item.getString("generation_id"),
            recordOrdinal = item.getInt("record_ordinal"),
            globalMessageNumber = item.getInt("global_message_number"),
            fieldKind = item.getString("field_kind"),
            fieldNumber = item.getInt("field_number"),
            baseTypeCode = if (item.isNull("base_type_code")) null else item.getInt("base_type_code"),
            fieldSize = item.getInt("field_size"),
            developerDataIndex = if (item.isNull("developer_data_index")) null else item.getInt("developer_data_index"),
            bigEndian = item.getBoolean("big_endian"),
            rawBytes = raw,
            measuredAtEpochMillis = if (item.isNull("measured_at_ms")) null else item.getLong("measured_at_ms"),
            timestampOrigin = item.getString("timestamp_origin")
        )
        require(field.generationId.matches(Regex("[0-9a-f]{64}")) &&
            field.recordOrdinal > 0 && field.globalMessageNumber in 0..65535 &&
            field.fieldKind in setOf("STANDARD", "DEVELOPER", "COMPRESSED_TIMESTAMP_HEADER") &&
            field.fieldNumber in 0..255 &&
            field.fieldSize in 1..255 && raw.size == field.fieldSize &&
            (field.baseTypeCode == null || field.baseTypeCode in 0..255) &&
            (field.developerDataIndex == null || field.developerDataIndex in 0..255) &&
            (field.measuredAtEpochMillis == null || field.measuredAtEpochMillis >= 0) &&
            field.timestampOrigin in setOf("DIRECT", "RECONSTRUCTED_TS16", "RECONSTRUCTED_COMPRESSED", "UNKNOWN"))
        return field
    }

    private fun parseFitReviewCandidate(line: String): StoredFitReviewCandidate {
        val item = JSONObject(line)
        val row = StoredFitReviewCandidate(
            sampleId = item.getString("sample_id"),
            deviceId = item.getString("device_id"),
            metric = item.getString("metric"),
            measuredAtEpochMillis = if (item.isNull("measured_at_ms")) null
                else item.getLong("measured_at_ms"),
            value = item.getDouble("value"),
            unit = item.getString("unit"),
            sourceSha256 = item.getString("source_sha256"),
            recordOrdinal = item.getInt("record_ordinal"),
            globalMessageNumber = item.getInt("global_message_number"),
            fieldNumber = item.getInt("field_number"),
            quality = item.getString("quality"),
            timestampOrigin = item.getString("timestamp_origin"),
            decoderVersion = item.getString("decoder_version")
        )
        require(row.sampleId.isNotBlank() && row.deviceId.isNotBlank() &&
            row.metric in CirqaDatabase.FIT_REVIEW_CANDIDATE_METRICS &&
            (row.measuredAtEpochMillis == null || row.measuredAtEpochMillis >= 0) &&
            row.value.isFinite() && row.unit.isNotBlank() &&
            row.sourceSha256.matches(Regex("[0-9a-f]{64}")) && row.recordOrdinal > 0 &&
            row.globalMessageNumber >= 0 && row.fieldNumber >= 0 &&
            row.quality == "UNTRUSTED_PROFILE_CANDIDATE" &&
            row.timestampOrigin in setOf(
                "DIRECT", "RECONSTRUCTED_TS16", "RECONSTRUCTED_COMPRESSED", "UNKNOWN"
            ) && row.decoderVersion.isNotBlank()) {
            "Ungültiger untrusted FIT-Kandidat im Backup"
        }
        return row
    }

    private fun parseActiveFitGeneration(line: String): StoredActiveFitGeneration {
        val item = JSONObject(line)
        val active = StoredActiveFitGeneration(
            sourceSha256 = item.getString("source_sha256"),
            generationId = item.getString("generation_id")
        )
        require(
            active.sourceSha256.matches(Regex("[0-9a-f]{64}")) &&
                active.generationId.matches(Regex("[0-9a-f]{64}"))
        ) { "Ungültiger aktiver FIT-Decoderstand im Backup" }
        return active
    }

    private fun parseFitProvenance(line: String): StoredFitProvenance {
        val item = JSONObject(line)
        val provenance = StoredFitProvenance(
            sampleId = item.getString("sample_id"),
            deviceId = item.getString("device_id"),
            metric = item.getString("metric"),
            measuredAtEpochMillis = item.getLong("measured_at_ms"),
            sourceSha256 = item.getString("source_sha256"),
            recordOrdinal = item.getInt("record_ordinal"),
            globalMessageNumber = item.getInt("global_message_number"),
            fieldNumber = item.getInt("field_number"),
            quality = item.getString("quality"),
            decoderVersion = item.getString("decoder_version")
        )
        require(provenance.sampleId.isNotBlank() && provenance.deviceId.isNotBlank() &&
            provenance.metric.isNotBlank() && provenance.measuredAtEpochMillis >= 0 &&
            provenance.sourceSha256.matches(Regex("[0-9a-f]{64}")) &&
            provenance.recordOrdinal >= 0 && provenance.globalMessageNumber >= 0 &&
            provenance.fieldNumber >= 0 && provenance.quality == "PROFILE_VALID" &&
            provenance.decoderVersion.isNotBlank()) {
            "Ungültige FIT-Feldherkunft im Backup"
        }
        return provenance
    }

    private fun parseDeviceMetadata(line: String): FitHistoricalObservations.DeviceMetadata {
        val item = JSONObject(line)
        val metadata = FitHistoricalObservations.DeviceMetadata(
            sourceFileSha256 = item.getString("source_sha256"),
            recordOrdinal = item.getInt("record_ordinal"),
            timestampEpochMillis = if (!item.has("observed_at_ms") || item.isNull("observed_at_ms"))
                null else item.getLong("observed_at_ms"),
            deviceTypeCode = if (!item.has("device_type_code") || item.isNull("device_type_code"))
                null else item.getInt("device_type_code"),
            manufacturerCode = if (!item.has("manufacturer_code") || item.isNull("manufacturer_code"))
                null else item.getInt("manufacturer_code"),
            productCode = if (!item.has("product_code") || item.isNull("product_code"))
                null else item.getInt("product_code"),
            softwareVersion = if (!item.has("software_version") || item.isNull("software_version"))
                null else item.getDouble("software_version"),
            batteryVoltageVolts = if (!item.has("battery_voltage_volts") || item.isNull("battery_voltage_volts"))
                null else item.getDouble("battery_voltage_volts"),
            batteryStatusCode = if (!item.has("battery_status_code") || item.isNull("battery_status_code"))
                null else item.getInt("battery_status_code"),
            batteryLevelPercent = if (!item.has("battery_level_percent") || item.isNull("battery_level_percent"))
                null else item.getInt("battery_level_percent")
        )
        require(CirqaDatabase.isValidDeviceMetadata(metadata)) { "Ungültige FIT-Gerätemetadaten im Backup" }
        return metadata
    }

    private fun parseLiveDeviceMetadata(line: String): LiveDeviceMetadata {
        val item = JSONObject(line)
        val value = LiveDeviceMetadata(
            fieldKey = item.getString("field_key"),
            valueText = item.getString("value_text"),
            unit = if (!item.has("unit") || item.isNull("unit")) null else item.getString("unit"),
            source = item.getString("source"),
            observedAtMs = item.getLong("observed_at_ms")
        )
        require(value.fieldKey.length in 1..64 && value.valueText.length in 1..256 &&
            (value.unit == null || value.unit.length in 1..16) &&
            value.source.length in 1..32 && value.observedAtMs >= 0) {
            "Ungültiger Live-Gerätemetadatenwert im Backup"
        }
        return value
    }

    private fun parseRawDeviceMetadata(line: String): RawDeviceMetadataRecord {
        val item = JSONObject(line)
        val metadata = RawDeviceMetadataRecord(
            sourceSha256 = item.getString("source_sha256"),
            fileBytes = item.getLong("file_bytes"),
            verifiedAtEpochMillis = item.getLong("verified_at_ms"),
            parserVersion = item.getString("parser_version"),
            softwareVersion = if (!item.has("software_version") || item.isNull("software_version"))
                null else item.getString("software_version"),
            massStorageMode = if (!item.has("mass_storage_mode") || item.isNull("mass_storage_mode"))
                null else item.getBoolean("mass_storage_mode"),
            transferDirectionElementCount = item.getInt("transfer_direction_count")
        )
        require(CirqaDatabase.isValidRawDeviceMetadata(metadata)) {
            "Ungültige Raw-Gerätemetadaten im Backup"
        }
        return metadata
    }

    private fun parseUnmappedFitField(line: String): StoredFitUnmappedField {
        val json = JSONObject(line)
        val item = StoredFitUnmappedField(
            sourceSha256 = json.getString("source_sha256"),
            globalMessageNumber = json.getInt("global_message_number"),
            kind = StoredFitFieldKind.valueOf(json.getString("field_kind")),
            fieldNumber = json.getInt("field_number"),
            baseTypeCode = if (json.isNull("base_type_code")) null else json.getInt("base_type_code"),
            fieldSize = json.getInt("field_size"),
            developerDataIndex = if (json.isNull("developer_data_index")) null else json.getInt("developer_data_index"),
            classification = json.getString("classification"),
            definitionCount = json.getLong("definition_count"),
            observedMessageCount = json.getLong("observed_message_count"),
            validElementCount = if (json.isNull("valid_element_count")) null else json.getLong("valid_element_count"),
            invalidElementCount = if (json.isNull("invalid_element_count")) null else json.getLong("invalid_element_count"),
            decoderVersion = json.getString("decoder_version"),
            profileVersion = json.getString("profile_version"),
            status = FitFieldInventoryStatus.valueOf(json.getString("status"))
        )
        require(
            item.sourceSha256.matches(Regex("[0-9a-f]{64}")) &&
                item.globalMessageNumber in 0..65535 && item.fieldNumber in 0..255 &&
                item.fieldSize in 1..255 &&
                (item.baseTypeCode == null || item.baseTypeCode in 0..255) &&
                (item.developerDataIndex == null || item.developerDataIndex in 0..255) &&
                item.classification in setOf(
                    "MESSAGE_NOT_DECODED_BY_APP", "FIELD_NOT_DECODED_BY_APP",
                    "KNOWN_MAPPING_INCOMPATIBLE", "DEVELOPER_FIELD_NOT_DECODED_BY_APP"
                ) && item.definitionCount in 0..1_000_000 &&
                item.observedMessageCount in 0..1_000_000 &&
                (item.validElementCount == null || item.validElementCount in 0..1_000_000) &&
                (item.invalidElementCount == null || item.invalidElementCount in 0..1_000_000) &&
                item.decoderVersion.length in 1..32 && item.profileVersion.length in 1..32
        ) { "Ungültige FIT-Feldinventur im Backup" }
        return item
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

    private fun encodeFitSource(source: StoredFitSource): String = JSONObject()
        .put("sha256", source.sha256)
        .put("file_bytes", source.fileBytes)
        .put("relative_path", source.relativePath)
        .put("validated_at_ms", source.validatedAtEpochMillis)
        .put("decoder_version", source.decoderVersion)
        .put("profile_version", source.profileVersion)
        .toString()

    private fun encodeFitGeneration(value: StoredFitDecoderGeneration): String = JSONObject()
        .put("generation_id", value.generationId)
        .put("source_sha256", value.sourceSha256)
        .put("decoder_version", value.decoderVersion)
        .put("profile_version", value.profileVersion)
        .put("output_digest", value.outputDigest)
        .put("output_count", value.outputCount)
        .put("decoded_field_count", value.decodedFieldCount)
        .put("completed_at_ms", value.completedAtEpochMillis)
        .toString()

    private fun encodeFitGenerationObservation(value: StoredFitGenerationObservation): String = JSONObject()
        .put("generation_id", value.generationId)
        .put("sample_id", value.sampleId)
        .put("device_id", value.deviceId)
        .put("metric", value.metric)
        .put("measured_at_ms", value.measuredAtEpochMillis)
        .put("value", value.value)
        .put("unit", value.unit)
        .put("received_at_ms", value.receivedAtEpochMillis ?: JSONObject.NULL)
        .put("record_ordinal", value.recordOrdinal)
        .put("global_message_number", value.globalMessageNumber)
        .put("field_number", value.fieldNumber)
        .put("quality", value.quality)
        .put("timestamp_origin", value.timestampOrigin)
        .toString()

    private fun encodeFitGenerationField(value: StoredFitGenerationField): String = JSONObject()
        .put("generation_id", value.generationId)
        .put("record_ordinal", value.recordOrdinal)
        .put("global_message_number", value.globalMessageNumber)
        .put("field_kind", value.fieldKind)
        .put("field_number", value.fieldNumber)
        .put("base_type_code", value.baseTypeCode)
        .put("field_size", value.fieldSize)
        .put("developer_data_index", value.developerDataIndex)
        .put("big_endian", value.bigEndian)
        .put("raw_bytes", Base64.getEncoder().encodeToString(value.rawBytes))
        .put("measured_at_ms", value.measuredAtEpochMillis)
        .put("timestamp_origin", value.timestampOrigin)
        .toString()

    private fun encodeFitReviewCandidate(value: StoredFitReviewCandidate): String = JSONObject()
        .put("sample_id", value.sampleId)
        .put("device_id", value.deviceId)
        .put("metric", value.metric)
        .put("measured_at_ms", value.measuredAtEpochMillis ?: JSONObject.NULL)
        .put("value", value.value)
        .put("unit", value.unit)
        .put("source_sha256", value.sourceSha256)
        .put("record_ordinal", value.recordOrdinal)
        .put("global_message_number", value.globalMessageNumber)
        .put("field_number", value.fieldNumber)
        .put("quality", value.quality)
        .put("timestamp_origin", value.timestampOrigin)
        .put("decoder_version", value.decoderVersion)
        .toString()

    private fun encodeActiveFitGeneration(value: StoredActiveFitGeneration): String = JSONObject()
        .put("source_sha256", value.sourceSha256)
        .put("generation_id", value.generationId)
        .toString()

    private fun encodeFitProvenance(value: StoredFitProvenance): String = JSONObject()
        .put("sample_id", value.sampleId)
        .put("device_id", value.deviceId)
        .put("metric", value.metric)
        .put("measured_at_ms", value.measuredAtEpochMillis)
        .put("source_sha256", value.sourceSha256)
        .put("record_ordinal", value.recordOrdinal)
        .put("global_message_number", value.globalMessageNumber)
        .put("field_number", value.fieldNumber)
        .put("quality", value.quality)
        .put("decoder_version", value.decoderVersion)
        .toString()

    private fun encodeDeviceMetadata(value: FitHistoricalObservations.DeviceMetadata): String = JSONObject()
        .put("source_sha256", value.sourceFileSha256)
        .put("record_ordinal", value.recordOrdinal)
        .put("observed_at_ms", value.timestampEpochMillis ?: JSONObject.NULL)
        .put("device_type_code", value.deviceTypeCode ?: JSONObject.NULL)
        .put("manufacturer_code", value.manufacturerCode ?: JSONObject.NULL)
        .put("product_code", value.productCode ?: JSONObject.NULL)
        .put("software_version", value.softwareVersion ?: JSONObject.NULL)
        .put("battery_voltage_volts", value.batteryVoltageVolts ?: JSONObject.NULL)
        .put("battery_status_code", value.batteryStatusCode ?: JSONObject.NULL)
        .put("battery_level_percent", value.batteryLevelPercent ?: JSONObject.NULL)
        .toString()

    private fun encodeRawDeviceMetadata(value: RawDeviceMetadataRecord): String = JSONObject()
        .put("source_sha256", value.sourceSha256)
        .put("file_bytes", value.fileBytes)
        .put("verified_at_ms", value.verifiedAtEpochMillis)
        .put("parser_version", value.parserVersion)
        .put("software_version", value.softwareVersion ?: JSONObject.NULL)
        .put("mass_storage_mode", value.massStorageMode ?: JSONObject.NULL)
        .put("transfer_direction_count", value.transferDirectionElementCount)
        .toString()

    private fun encodeUnmappedFitField(value: StoredFitUnmappedField): String = JSONObject()
        .put("source_sha256", value.sourceSha256)
        .put("global_message_number", value.globalMessageNumber)
        .put("field_kind", value.kind.name)
        .put("field_number", value.fieldNumber)
        .put("base_type_code", value.baseTypeCode ?: JSONObject.NULL)
        .put("field_size", value.fieldSize)
        .put("developer_data_index", value.developerDataIndex ?: JSONObject.NULL)
        .put("classification", value.classification)
        .put("definition_count", value.definitionCount)
        .put("observed_message_count", value.observedMessageCount)
        .put("valid_element_count", value.validElementCount ?: JSONObject.NULL)
        .put("invalid_element_count", value.invalidElementCount ?: JSONObject.NULL)
        .put("decoder_version", value.decoderVersion)
        .put("profile_version", value.profileVersion)
        .put("status", value.status.name)
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

/** Technical, data-free reason of a failed backup for the user and support (class and message only, no values). */
internal object BackupFailure {
    fun reason(e: Throwable): String {
        val root = generateSequence(e) { it.cause }.last()
        val message = root.message?.lineSequence()?.firstOrNull()?.take(160)
        return if (message.isNullOrBlank()) root.javaClass.simpleName else "${root.javaClass.simpleName}: $message"
    }
}
