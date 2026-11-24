package de.pulselibre

import android.database.sqlite.SQLiteDatabase

/**
 * What is stored locally, counted from the database only (P0 data acquisition, docs/DATA_ACQUISITION_PRIORITY.md).
 * Strictly separate from any medical statement: these are capture counters, never health values and never a claim that
 * the band history is complete. A counter is null when its table cannot be read; it is never guessed.
 */
internal data class CaptureInventory(
    /** Original downloaded files (any type, unknown ones included) and their total size in bytes. */
    val archivedFiles: Long?, val archivedBytes: Long?,
    /** FIT files that passed validation and were imported. */
    val importedFitFiles: Long?,
    /** Regular FIT measurements of the active decoder generations. */
    val regularObservations: Long?,
    /** Stored but unverified candidates per metric (spo2, stress, respiration); not counted as measurements. */
    val candidatesByMetric: Map<String, Long>?,
    /** Distinct raw BLE notification payloads kept as original bytes (#600): identical payloads count once, the archive stops after a per-run limit; not the number of notifications received. */
    val rawNotifications: Long? = null,
    /** Every stored raw FIT field of the active generations, known or unknown (#610). */
    val rawFitFields: Long? = null,
    /** Field kinds the app does not decode (kept as raw bytes, not evaluated). */
    val undecodedFieldKinds: Long? = null
) {
    /** True only when no counter could be read at all — never report "nothing stored" for unreadable tables. */
    val isUnknown: Boolean get() = archivedFiles == null && importedFitFiles == null &&
        regularObservations == null && candidatesByMetric == null &&
        rawNotifications == null && rawFitFields == null && undecodedFieldKinds == null

    val isEmpty: Boolean get() = !isUnknown && (archivedFiles ?: 0L) == 0L && (importedFitFiles ?: 0L) == 0L &&
        (regularObservations ?: 0L) == 0L && candidatesByMetric.isNullOrEmpty() &&
        (rawNotifications ?: 0L) == 0L && (rawFitFields ?: 0L) == 0L && (undecodedFieldKinds ?: 0L) == 0L

    companion object {
        private const val NOTIFICATION_SHAS = "SELECT content_sha256 FROM downloaded_file_references WHERE type_label='notification'"
        val EMPTY = CaptureInventory(null, null, null, null, null)

        fun load(db: SQLiteDatabase): CaptureInventory {
            fun one(sql: String): Long? = try { db.rawQuery(sql, null).use { if (it.moveToFirst()) it.getLong(0) else null } } catch (_: Exception) { null }
            val candidates = try {
                val m = LinkedHashMap<String, Long>()
                db.rawQuery("SELECT metric, COUNT(*) FROM fit_review_candidates GROUP BY metric ORDER BY metric", null).use {
                    while (it.moveToNext()) m[it.getString(0)] = it.getLong(1)
                }
                m
            } catch (_: Exception) { null }
            return CaptureInventory(
                // Raw notifications share the content-addressed store; they are counted separately, not as files.
                archivedFiles = one("SELECT COUNT(*) FROM downloaded_files WHERE content_sha256 NOT IN ($NOTIFICATION_SHAS)"),
                archivedBytes = one("SELECT COALESCE(SUM(file_bytes),0) FROM downloaded_files WHERE content_sha256 NOT IN ($NOTIFICATION_SHAS)"),
                importedFitFiles = one("SELECT COUNT(*) FROM fit_sources"),
                regularObservations = one(
                    "SELECT COUNT(*) FROM fit_generation_observations g JOIN fit_active_generations a ON a.generation_id = g.generation_id"),
                candidatesByMetric = candidates,
                rawNotifications = one("SELECT COUNT(DISTINCT content_sha256) FROM downloaded_file_references WHERE type_label='notification'"),
                rawFitFields = one(
                    "SELECT COUNT(*) FROM fit_generation_fields f JOIN fit_active_generations a ON a.generation_id = f.generation_id"),
                undecodedFieldKinds = one("SELECT COUNT(*) FROM fit_unmapped_fields"))
        }
    }
}
