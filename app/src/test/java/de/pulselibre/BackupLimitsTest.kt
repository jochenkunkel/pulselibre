package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 25.11.2026: backups of real data failed and left 0-byte files (uncompressed content checked against the ZIP limit). */
class BackupLimitsTest {
    @Test fun uncompressedContentHasItsOwnLargerLimit() {
        // The 08:23 backup on the S10 was 5.9 MB as ZIP; JSON lines compress 10-20x, so >100 MB of content is normal.
        assertTrue(MAX_UNCOMPRESSED_BYTES >= 1024L * 1024L * 1024L)
        assertTrue(MAX_UNCOMPRESSED_BYTES > MAX_ARCHIVE_BYTES)
        assertTrue(MAX_ENTRY_BYTES <= MAX_UNCOMPRESSED_BYTES)
        assertTrue(MAX_RECORDS >= 10_000_000L)
    }

    @Test fun failureReasonIsTechnicalAndShort() {
        assertEquals("IllegalArgumentException: Lokale Datenbank ist zu groß für ein Backup",
            BackupFailure.reason(IllegalArgumentException("Lokale Datenbank ist zu groß für ein Backup")))
        assertEquals("SQLException: database is locked",
            BackupFailure.reason(RuntimeException("wrapper", java.sql.SQLException("database is locked\nsecond line"))))
        assertEquals("IllegalStateException", BackupFailure.reason(IllegalStateException()))
    }
}
