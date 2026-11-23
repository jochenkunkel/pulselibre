package de.pulselibre

import android.database.sqlite.SQLiteDatabase
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Base64

/**
 * Lossless canonical SQLite archive, independent of FIT semantics/importability.
 * Contains private original bytes (including unknown/status fields), never telemetry.
 * Payloads are read in bounded slices to stay below Android's CursorWindow limit.
 */
object DownloadedFileArchive {
    const val CHUNK_BYTES = 256 * 1024
    const val MAX_BYTES = 8 * 1024 * 1024
    const val BACKUP_ENTRY = "downloaded_file_archive.jsonl"

    fun create(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS downloaded_files (
            content_sha256 TEXT PRIMARY KEY NOT NULL CHECK(length(content_sha256)=64),
            file_bytes INTEGER NOT NULL CHECK(file_bytes BETWEEN 1 AND 8388608),
            received_at_ms INTEGER NOT NULL CHECK(received_at_ms >= 0),
            payload BLOB NOT NULL CHECK(length(payload)=file_bytes)
        )""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS downloaded_file_references (
            content_sha256 TEXT NOT NULL REFERENCES downloaded_files(content_sha256) ON DELETE RESTRICT,
            reference_sha256 TEXT NOT NULL CHECK(length(reference_sha256)=64),
            reference_bytes BLOB NOT NULL,
            type_label TEXT NOT NULL,
            received_at_ms INTEGER NOT NULL CHECK(received_at_ms >= 0),
            PRIMARY KEY(content_sha256, reference_sha256)
        )""")
    }

    fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 255) }

    /** Caller owns the transaction. Never replaces a previous content version. */
    fun store(db: SQLiteDatabase, bytes: ByteArray, reference: ByteArray, label: String, at: Long): Boolean {
        require(bytes.size in 1..MAX_BYTES && reference.size <= 64 * 1024 && at >= 0)
        require(label.matches(Regex("[A-Za-z0-9_]{1,48}")))
        val sha = sha256(bytes)
        val contentAlreadyPresent = db.rawQuery(
            "SELECT 1 FROM downloaded_files WHERE content_sha256=?",
            arrayOf(sha)
        ).use { it.moveToFirst() }
        db.execSQL("""INSERT OR IGNORE INTO downloaded_files
            (content_sha256,file_bytes,received_at_ms,payload) VALUES (?,?,?,?)""",
            arrayOf(sha, bytes.size, at, bytes))
        db.rawQuery("SELECT file_bytes FROM downloaded_files WHERE content_sha256=?", arrayOf(sha)).use {
            check(it.moveToFirst() && it.getLong(0) == bytes.size.toLong())
        }
        for (offset in bytes.indices step CHUNK_BYTES) {
            db.rawQuery("SELECT substr(payload,?,?) FROM downloaded_files WHERE content_sha256=?",
                arrayOf((offset + 1).toString(), CHUNK_BYTES.toString(), sha)).use {
                check(it.moveToFirst() && it.getBlob(0).contentEquals(
                    bytes.copyOfRange(offset, minOf(offset + CHUNK_BYTES, bytes.size))))
            }
        }
        db.execSQL("""INSERT OR IGNORE INTO downloaded_file_references
            (content_sha256,reference_sha256,reference_bytes,type_label,received_at_ms)
            VALUES (?,?,?,?,?)""", arrayOf(sha, sha256(reference), reference, label, at))
        return contentAlreadyPresent
    }

    fun export(db: SQLiteDatabase, emit: (String) -> Unit) {
        db.rawQuery("""SELECT content_sha256,file_bytes,received_at_ms
            FROM downloaded_files ORDER BY content_sha256""", null).use { rows ->
            while (rows.moveToNext()) {
                val sha = rows.getString(0)
                val size = rows.getInt(1)
                for (offset in 0 until size step CHUNK_BYTES) {
                    db.rawQuery("SELECT substr(payload,?,?) FROM downloaded_files WHERE content_sha256=?",
                        arrayOf((offset + 1).toString(), CHUNK_BYTES.toString(), sha)).use { data ->
                        check(data.moveToFirst())
                        emit(JSONObject().put("kind", "chunk").put("sha256", sha).put("bytes", size)
                            .put("received_at_ms", rows.getLong(2)).put("offset", offset)
                            .put("data", Base64.getEncoder().encodeToString(data.getBlob(0))).toString())
                    }
                }
            }
        }
        db.rawQuery("""SELECT content_sha256,reference_sha256,reference_bytes,type_label,received_at_ms
            FROM downloaded_file_references ORDER BY content_sha256,reference_sha256""", null).use { rows ->
            while (rows.moveToNext()) emit(JSONObject().put("kind", "reference")
                .put("sha256", rows.getString(0)).put("reference_sha256", rows.getString(1))
                .put("data", Base64.getEncoder().encodeToString(rows.getBlob(2)))
                .put("type_label", rows.getString(3)).put("received_at_ms", rows.getLong(4)).toString())
        }
    }

    data class Row(val kind: String, val sha: String, val at: Long, val bytes: ByteArray,
                   val size: Int, val offset: Int, val referenceSha: String?, val label: String?)

    fun parse(line: String): Row {
        val json = JSONObject(line)
        val kind = json.getString("kind")
        require(kind == "chunk" || kind == "reference")
        val sha = json.getString("sha256")
        require(sha.matches(Regex("[0-9a-f]{64}")))
        val at = json.getLong("received_at_ms")
        require(at >= 0)
        val data = Base64.getDecoder().decode(json.getString("data"))
        require(data.size <= CHUNK_BYTES)
        if (kind == "reference") {
            val ref = json.getString("reference_sha256")
            val label = json.getString("type_label")
            require(data.size <= 64 * 1024 && ref == sha256(data))
            require(label.matches(Regex("[A-Za-z0-9_]{1,48}")))
            return Row(kind, sha, at, data, 0, 0, ref, label)
        }
        val size = json.getInt("bytes")
        val offset = json.getInt("offset")
        require(size in 1..MAX_BYTES && offset in 0 until size && offset % CHUNK_BYTES == 0)
        require(data.size == minOf(CHUNK_BYTES, size - offset))
        return Row(kind, sha, at, data, size, offset, null, null)
    }

    /** Validate complete byte streams before restore; optional sink runs in the restore transaction. */
    fun consume(rows: Sequence<Row>, db: SQLiteDatabase? = null) {
        var current: String? = null
        var payload = byteArrayOf()
        var at = 0L
        var offset = 0
        var previous = ""
        var referencePhase = false
        val completed = HashSet<String>()
        val references = HashSet<String>()
        for (row in rows) {
            if (row.kind == "reference") {
                require(current == null && row.sha in completed)
                referencePhase = true
                require(references.add("${row.sha}:${row.referenceSha}"))
                db?.execSQL("""INSERT INTO downloaded_file_references
                    (content_sha256,reference_sha256,reference_bytes,type_label,received_at_ms)
                    VALUES (?,?,?,?,?)""", arrayOf(row.sha, row.referenceSha, row.bytes, row.label, row.at))
                continue
            }
            require(!referencePhase)
            if (current == null) {
                require(row.offset == 0 && row.sha > previous)
                current = row.sha
                payload = ByteArray(row.size)
                offset = 0
                at = row.at
            }
            require(row.sha == current && row.offset == offset && row.size == payload.size && row.at == at)
            row.bytes.copyInto(payload, offset)
            offset += row.bytes.size
            if (offset == payload.size) {
                require(sha256(payload) == current)
                db?.execSQL("""INSERT INTO downloaded_files
                    (content_sha256,file_bytes,received_at_ms,payload) VALUES (?,?,?,?)""",
                    arrayOf(current, payload.size, at, payload))
                completed.add(row.sha)
                previous = row.sha
                current = null
                payload = byteArrayOf()
            }
        }
        require(current == null) { "Incomplete original-file archive" }
    }
}
