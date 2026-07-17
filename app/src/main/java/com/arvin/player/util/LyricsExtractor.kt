package com.arvin.player.util

import java.io.File
import java.io.RandomAccessFile

/**
 * Dependency-free, offline reader for embedded lyrics across the formats this player supports:
 *  - MP3: ID3v2.3/2.4 "USLT" (Unsynchronised Lyrics/Text) frame
 *  - FLAC: the VORBIS_COMMENT metadata block (a "LYRICS" or "UNSYNCEDLYRICS" comment key)
 *  - OGG (Vorbis): the same Vorbis-comment format as FLAC, but reassembled from Ogg pages/packets
 *  - M4A/MP4: the iTunes-style "©lyr" atom under moov/udta/meta/ilst
 *
 * Each parser only understands the common, typical on-disk layout for its format (e.g. ID3v2 at
 * the very start of an MP3, a single un-fragmented Vorbis comment block in FLAC, comment headers
 * within the first few Ogg pages, standard atom nesting in MP4). Unusual or exotic taggings may
 * not be found — this returns null in that case, same as if there were no lyrics at all. None of
 * this has been run against real files (no test corpus in this sandbox); if lyrics that you know
 * exist in a file's tags don't show up, this is the first place to check.
 */
object LyricsExtractor {

    fun extractEmbeddedLyrics(path: String): String? = try {
        val file = File(path)
        if (!file.exists()) null
        else when (path.substringAfterLast('.', "").lowercase()) {
            "mp3" -> extractFromMp3(file)
            "flac" -> extractFromFlac(file)
            "ogg", "oga" -> extractFromOgg(file)
            "m4a", "mp4", "m4b" -> extractFromMp4(file)
            else -> null
        }
    } catch (_: Exception) {
        null
    }

    // ---------------------------------------------------------------------------------------
    // MP3 / ID3v2 USLT
    // ---------------------------------------------------------------------------------------

    private fun extractFromMp3(file: File): String? {
        RandomAccessFile(file, "r").use { raf ->
            val header = ByteArray(10)
            raf.readFully(header)
            if (header[0] != 'I'.code.toByte() || header[1] != 'D'.code.toByte() || header[2] != '3'.code.toByte()) {
                return null // no ID3v2 tag present
            }
            val majorVersion = header[3].toInt()
            val tagSize = synchsafeToInt(header[6], header[7], header[8], header[9])

            var bytesRead = 0
            while (bytesRead < tagSize) {
                val frameHeader = ByteArray(10)
                val read = raf.read(frameHeader)
                if (read < 10) break
                bytesRead += 10

                val frameId = String(frameHeader, 0, 4, Charsets.ISO_8859_1)
                if (frameId.isBlank() || frameId[0] == '\u0000') break // padding reached

                val frameSize = if (majorVersion >= 4) {
                    synchsafeToInt(frameHeader[4], frameHeader[5], frameHeader[6], frameHeader[7])
                } else {
                    beInt(frameHeader[4], frameHeader[5], frameHeader[6], frameHeader[7])
                }
                if (frameSize <= 0 || frameSize > tagSize) break

                if (frameId == "USLT") {
                    val frameData = ByteArray(frameSize)
                    raf.readFully(frameData)
                    return parseUslt(frameData)
                } else {
                    raf.seek(raf.filePointer + frameSize)
                    bytesRead += frameSize
                }
            }
            return null
        }
    }

    private fun parseUslt(data: ByteArray): String? {
        if (data.isEmpty()) return null
        val encodingByte = data[0].toInt()
        val charset = when (encodingByte) {
            1 -> Charsets.UTF_16
            2 -> Charsets.UTF_16BE
            3 -> Charsets.UTF_8
            else -> Charsets.ISO_8859_1
        }
        val terminatorWidth = if (encodingByte == 1 || encodingByte == 2) 2 else 1
        var descriptorEnd = 4 // encoding byte + 3-byte language code
        while (descriptorEnd + terminatorWidth <= data.size) {
            val isTerminator = if (terminatorWidth == 1) {
                data[descriptorEnd] == 0.toByte()
            } else {
                data[descriptorEnd] == 0.toByte() && data.getOrNull(descriptorEnd + 1) == 0.toByte()
            }
            if (isTerminator) break
            descriptorEnd += 1
        }
        val textStart = (descriptorEnd + terminatorWidth).coerceAtMost(data.size)
        if (textStart >= data.size) return null
        val text = String(data.copyOfRange(textStart, data.size), charset).trim { it.isWhitespace() || it == '\u0000' }
        return text.ifBlank { null }
    }

    private fun synchsafeToInt(b0: Byte, b1: Byte, b2: Byte, b3: Byte): Int =
        ((b0.toInt() and 0x7F) shl 21) or ((b1.toInt() and 0x7F) shl 14) or
            ((b2.toInt() and 0x7F) shl 7) or (b3.toInt() and 0x7F)

    private fun beInt(b0: Byte, b1: Byte, b2: Byte, b3: Byte): Int =
        ((b0.toInt() and 0xFF) shl 24) or ((b1.toInt() and 0xFF) shl 16) or
            ((b2.toInt() and 0xFF) shl 8) or (b3.toInt() and 0xFF)

    // ---------------------------------------------------------------------------------------
    // Shared Vorbis-comment parsing (used by both FLAC and OGG below)
    // ---------------------------------------------------------------------------------------

    private val lyricKeys = setOf("LYRICS", "UNSYNCEDLYRICS", "LYRIC")

    /** Parses a raw Vorbis comment block: 4-byte LE vendor length + vendor string,
     *  4-byte LE comment count, then [4-byte LE length + "KEY=VALUE" UTF-8 string] * count. */
    private fun parseVorbisComments(data: ByteArray): String? {
        if (data.size < 8) return null
        var offset = 0
        fun readLeInt(): Int {
            val v = (data[offset].toInt() and 0xFF) or
                ((data[offset + 1].toInt() and 0xFF) shl 8) or
                ((data[offset + 2].toInt() and 0xFF) shl 16) or
                ((data[offset + 3].toInt() and 0xFF) shl 24)
            offset += 4
            return v
        }
        val vendorLength = readLeInt()
        if (vendorLength < 0 || offset + vendorLength > data.size) return null
        offset += vendorLength // skip vendor string, we don't need it
        if (offset + 4 > data.size) return null
        val commentCount = readLeInt()
        if (commentCount < 0 || commentCount > 10_000) return null // sanity guard against garbage/corrupt data

        repeat(commentCount) {
            if (offset + 4 > data.size) return null
            val len = readLeInt()
            if (len < 0 || offset + len > data.size) return null
            val comment = String(data, offset, len, Charsets.UTF_8)
            offset += len
            val eq = comment.indexOf('=')
            if (eq > 0) {
                val key = comment.substring(0, eq).uppercase()
                if (key in lyricKeys) {
                    val value = comment.substring(eq + 1).trim()
                    if (value.isNotBlank()) return value
                }
            }
        }
        return null
    }

    // ---------------------------------------------------------------------------------------
    // FLAC
    // ---------------------------------------------------------------------------------------

    private fun extractFromFlac(file: File): String? {
        RandomAccessFile(file, "r").use { raf ->
            val magic = ByteArray(4)
            raf.readFully(magic)
            if (String(magic, Charsets.US_ASCII) != "fLaC") return null

            while (true) {
                val blockHeader = ByteArray(4)
                if (raf.read(blockHeader) < 4) return null
                val isLast = (blockHeader[0].toInt() and 0x80) != 0
                val blockType = blockHeader[0].toInt() and 0x7F
                val blockLength = beInt24(blockHeader[1], blockHeader[2], blockHeader[3])

                if (blockType == 4) { // VORBIS_COMMENT
                    val blockData = ByteArray(blockLength)
                    raf.readFully(blockData)
                    return parseVorbisComments(blockData)
                } else {
                    raf.seek(raf.filePointer + blockLength)
                }
                if (isLast) return null
            }
        }
    }

    private fun beInt24(b0: Byte, b1: Byte, b2: Byte): Int =
        ((b0.toInt() and 0xFF) shl 16) or ((b1.toInt() and 0xFF) shl 8) or (b2.toInt() and 0xFF)

    // ---------------------------------------------------------------------------------------
    // OGG (Vorbis) — reassemble packets from pages, looking for the comment header packet
    // (type byte 0x03 followed by "vorbis"). Limited to the first handful of pages, which is
    // where the comment header always lives in practice.
    // ---------------------------------------------------------------------------------------

    private fun extractFromOgg(file: File): String? {
        RandomAccessFile(file, "r").use { raf ->
            val packetBuffer = java.io.ByteArrayOutputStream()
            var pagesRead = 0

            while (pagesRead < 20) {
                val capture = ByteArray(4)
                val read = raf.read(capture)
                if (read < 4 || String(capture, Charsets.US_ASCII) != "OggS") break
                pagesRead++

                raf.skipBytes(1 + 1 + 8 + 4 + 4 + 4) // version, header_type, granule_pos, serial, seq, crc
                val segmentCountByte = ByteArray(1)
                raf.readFully(segmentCountByte)
                val segmentCount = segmentCountByte[0].toInt() and 0xFF
                val segmentTable = ByteArray(segmentCount)
                raf.readFully(segmentTable)

                var pageDataSize = 0
                for (b in segmentTable) pageDataSize += (b.toInt() and 0xFF)
                val pageData = ByteArray(pageDataSize)
                raf.readFully(pageData)

                // Split page data into packets using the segment table's lacing values: a
                // segment value of 255 means "packet continues", less than 255 means "packet ends here".
                var dataOffset = 0
                for (segLen in segmentTable) {
                    val len = segLen.toInt() and 0xFF
                    packetBuffer.write(pageData, dataOffset, len)
                    dataOffset += len
                    if (len < 255) {
                        // Packet complete — check if it's the Vorbis comment header.
                        val packet = packetBuffer.toByteArray()
                        packetBuffer.reset()
                        if (packet.size > 7 && packet[0] == 0x03.toByte() &&
                            String(packet, 1, 6, Charsets.US_ASCII) == "vorbis"
                        ) {
                            return parseVorbisComments(packet.copyOfRange(7, packet.size))
                        }
                    }
                }
            }
            return null
        }
    }

    // ---------------------------------------------------------------------------------------
    // M4A / MP4 — walk the atom tree to moov/udta/meta/ilst/©lyr/data
    // ---------------------------------------------------------------------------------------

    private fun extractFromMp4(file: File): String? {
        RandomAccessFile(file, "r").use { raf ->
            return findAtomRecursive(raf, endOffset = raf.length(), path = listOf("moov", "udta", "meta", "ilst", "\u00A9lyr", "data"))
        }
    }

    /** Walks a chain of nested atom names (e.g. moov -> udta -> meta -> ilst -> ©lyr -> data),
     *  returning the text payload of the final "data" atom if the whole chain is found. */
    private fun findAtomRecursive(raf: RandomAccessFile, endOffset: Long, path: List<String>): String? {
        if (path.isEmpty()) return null
        var pos = raf.filePointer

        while (pos < endOffset) {
            raf.seek(pos)
            val sizeBytes = ByteArray(4)
            if (raf.read(sizeBytes) < 4) return null
            var size = beInt(sizeBytes[0], sizeBytes[1], sizeBytes[2], sizeBytes[3]).toLong() and 0xFFFFFFFFL
            val typeBytes = ByteArray(4)
            raf.readFully(typeBytes)
            val type = String(typeBytes, Charsets.ISO_8859_1)
            var headerSize = 8L

            if (size == 1L) { // 64-bit extended size follows
                val extSizeBytes = ByteArray(8)
                raf.readFully(extSizeBytes)
                size = 0
                for (b in extSizeBytes) size = (size shl 8) or (b.toLong() and 0xFF)
                headerSize = 16L
            }
            if (size < headerSize) return null // corrupt/unexpected — bail out safely

            if (type == path.first()) {
                val childStart = pos + headerSize
                val childEnd = pos + size
                if (path.size == 1) {
                    if (type == "data") {
                        // "data" atom: 4-byte type indicator + 4-byte locale, then the payload itself.
                        raf.seek(childStart + 8)
                        val payloadLen = (childEnd - (childStart + 8)).toInt()
                        if (payloadLen <= 0) return null
                        val payload = ByteArray(payloadLen)
                        raf.readFully(payload)
                        return String(payload, Charsets.UTF_8).trim().ifBlank { null }
                    }
                    return null
                }
                // 'meta' is a FullBox: 4 bytes of version/flags precede its children.
                val nextStart = if (type == "meta") childStart + 4 else childStart
                raf.seek(nextStart)
                return findAtomRecursive(raf, endOffset = childEnd, path = path.drop(1))
            }

            pos += size
        }
        return null
    }
}
