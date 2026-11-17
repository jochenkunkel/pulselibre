package de.pulselibre

import java.io.File

/** Removes only the obsolete private JSONL recording, never SQLite data or user exports. */
internal object LegacyNightRecording {
    fun remove(filesDir: File): Boolean {
        val recording = File(filesDir, "cirqa_overnight.jsonl")
        return !recording.exists() || (recording.isFile && recording.delete())
    }
}
