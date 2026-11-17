package de.pulselibre

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LegacyNightRecordingTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun removesOnlyTheOldRecordingAndIsIdempotent() {
        val old = folder.newFile("cirqa_overnight.jsonl").apply { writeText("obsolete test fixture") }
        val database = folder.newFile("cirqa_measurements.db").apply { writeText("keep database") }
        val export = folder.newFile("export.jsonl").apply { writeText("keep export") }
        assertTrue(LegacyNightRecording.remove(folder.root))
        assertFalse(old.exists())
        assertEquals("keep database", database.readText())
        assertEquals("keep export", export.readText())
        assertTrue(LegacyNightRecording.remove(folder.root))
    }

    @Test fun neverDeletesDirectoriesRecursively() {
        val directory = folder.newFolder("cirqa_overnight.jsonl")
        val nested = File(directory, "keep.txt").apply { writeText("keep") }
        assertFalse(LegacyNightRecording.remove(folder.root))
        assertEquals("keep", nested.readText())
    }
}
