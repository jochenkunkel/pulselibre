package de.pulselibre

import android.database.sqlite.SQLiteDatabase

/**
 * Issue #600: lossless private archiving of raw BLE notification bytes.
 *
 * Uses the existing content-addressed [DownloadedFileArchive], so the original
 * bytes are covered by the v9 backup and SQLite inventory without a new schema.
 * The reference is a fixed local label — never a device address, UID or token.
 */
internal object CirqaRawNotificationArchive {
    const val MAX_NOTIFICATIONS_PER_RUN = 256
    const val MAX_TOTAL_BYTES_PER_RUN = 8L * 1024L * 1024L
    private val REFERENCE = byteArrayOf(0x4e, 0x54, 0x46) // local marker "NTF"

    /** Caller owns the database/session and must catch storage errors. */
    fun archive(db: SQLiteDatabase, bytes: ByteArray, at: Long): Boolean {
        if (bytes.isEmpty() || bytes.size > DownloadedFileArchive.MAX_BYTES || at < 0) return false
        DownloadedFileArchive.store(db, bytes, REFERENCE, "notification", at)
        return true
    }

    fun count(db: SQLiteDatabase): Long = db.rawQuery(
        "SELECT COUNT(*) FROM downloaded_file_references WHERE type_label='notification'", null
    ).use { rows -> if (rows.moveToFirst()) rows.getLong(0) else 0L }
}
