package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** #628: per-file import failures keep a classifiable reason, but no values, hashes or identifiers. */
class ImportAuditReasonTest {
    @Test fun keepsTheDeveloperMessageAndMasksNumbersAndHashes() {
        val r = ImportAuditRecorder.reasonOf(IllegalStateException(
            "Active generation 3f9a0c1d2e4b5a6c7d8e9f001122334455667788990011223344556677889900aa conflicts: 1795633046078 vs 42"))
        assertTrue(r, r.startsWith("IllegalStateException: Active generation # conflicts: # vs #"))
        assertFalse(Regex("[0-9]").containsMatchIn(r))
    }

    @Test fun ordinaryWordsWithHexLettersStayReadable() {
        assertEquals("IllegalArgumentException: FIT replay output does not match the validated source (accepted, decade)",
            ImportAuditRecorder.reasonOf(IllegalArgumentException("FIT replay output does not match the validated source (accepted, decade)")))
    }

    @Test fun unusualCharactersAndLengthAreBounded() {
        val r = ImportAuditRecorder.redactReason("x{\"key\":\"secret\"}<script>" + "a".repeat(500))
        assertFalse(r.contains("{") || r.contains("<") || r.contains("\""))
        assertTrue(r.length <= 160)
    }
}
