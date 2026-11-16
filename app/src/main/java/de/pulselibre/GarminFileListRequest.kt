package de.pulselibre

/**
 * One-shot, read-only FileSync V2 FileListRequest, based on protocol facts from
 * MIT-licensed GarminHRM600. No file flags, FileRequest, or device mutation.
 */
object GarminFileListRequest {
    /** Offline-only wire preview. CIRQA rejected this compact request with UNSUPPORTED. */
    fun gfdi(): ByteArray {
        // Smart container field 43 (length-delimited), containing field 9
        // FileListRequest with an empty message: list all, no sync flags.
        val smart = GarminFileListPagination.smartRequest()
        // Compact kind 0x39; initial sequence=10, counter=1.
        // Offline compatibility only: active CIRQA transmission stays disabled.
        return GarminCompactTransport.frame(0x39, 10, 1, smart)
    }

    fun cobs(data: ByteArray): ByteArray {
        val out = ArrayList<Byte>()
        out.add(0)
        var i = 0
        while (i < data.size) {
            val start = i
            while (i < data.size && data[i] != 0.toByte() && i - start < 254) i++
            val run = i - start
            out.add((run + 1).toByte())
            for (j in start until i) out.add(data[j])
            if (i < data.size && data[i] == 0.toByte()) {
                i++
                if (i == data.size) out.add(1)
            }
        }
        out.add(0)
        return out.toByteArray()
    }

    fun mlChunks(handle: Int, mtuPayload: Int = 19): List<ByteArray> {
        require(handle in 1..255 && mtuPayload in 1..244)
        return cobs(gfdi()).toList().chunked(mtuPayload).map { part ->
            byteArrayOf(handle.toByte()) + part.toByteArray()
        }
    }
}
