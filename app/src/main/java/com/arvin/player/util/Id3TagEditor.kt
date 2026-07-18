package com.arvin.player.util

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/** What to do with the embedded cover picture when saving. */
sealed class ArtworkAction {
    object Keep : ArtworkAction()
    data class Replace(val bytes: ByteArray, val mimeType: String) : ArtworkAction()
    object Remove : ArtworkAction()
}

data class TagEdits(
    val title: String,
    val artist: String,
    val album: String,
    val trackNumber: Int,
    val genre: String,
    val artwork: ArtworkAction
)

/**
 * Real, dependency-free ID3v2.3 read/write for MP3 files.
 *
 * This rewrites the tag as a fresh ID3v2.3 block: any frame this editor doesn't know about
 * (comments, embedded lyrics/USLT, custom frames, etc.) is read as raw bytes and copied through
 * unchanged; only TIT2/TPE1/TALB/TRCK/TCON/APIC are replaced according to [TagEdits]. Text frames
 * are written as UTF-16 (encoding byte 1, with BOM) rather than Latin-1 so titles/artists in
 * Persian, Arabic, Chinese, etc. round-trip correctly — every mainstream OS/player (Windows
 * Explorer & Groove, macOS Finder & Music, VLC, foobar2000, MediaMonkey, Linux Rhythmbox/Clementine)
 * reads standard ID3v2.3 UTF-16 text frames and a standard APIC "front cover" picture frame fine.
 *
 * Only MP3/ID3v2 is implemented here. Other formats (FLAC/OGG Vorbis-comments, MP4/M4A atoms) need
 * a different binary layout entirely and are intentionally not attempted — see [isSupportedFormat].
 */
object Id3TagEditor {

    fun isSupportedFormat(path: String): Boolean =
        path.substringAfterLast('.', "").lowercase() == "mp3"

    /**
     * Rewrites [sourceUri]'s ID3v2.3 tag in place (via a temp-file copy, then overwriting the
     * original through the same Uri) applying [edits]. Returns null on success, or a
     * human-irrelevant exception on failure (caller maps failures to a translated notice).
     */
    fun writeTags(context: Context, sourceUri: Uri, edits: TagEdits): Throwable? {
        return try {
            val tempFile = File.createTempFile("arvin_tag_", ".tmp", context.cacheDir)
            try {
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    rewriteStream(input, tempFile, edits)
                } ?: return IllegalStateException("Could not open source file for reading")

                context.contentResolver.openOutputStream(sourceUri, "wt")?.use { out ->
                    tempFile.inputStream().use { it.copyTo(out) }
                } ?: return IllegalStateException("Could not open source file for writing")
            } finally {
                tempFile.delete()
            }
            null
        } catch (e: Throwable) {
            e
        }
    }

    // -------------------------------------------------------------------------------------
    // Core rewrite: parse the existing tag (if any), keep unrelated frames, replace the
    // ones this editor manages, then stream the untouched audio bytes straight through.
    // -------------------------------------------------------------------------------------

    private fun rewriteStream(input: InputStream, tempFile: File, edits: TagEdits) {
        val header = ByteArray(10)
        val headerRead = readFully(input, header, 0, 10)

        var majorVersion = 3
        var oldTagSize = 0
        var tagBytes = ByteArray(0)
        var leftoverAfterHeader: ByteArray? = null

        if (headerRead == 10 && header[0] == 'I'.code.toByte() && header[1] == 'D'.code.toByte() && header[2] == '3'.code.toByte()) {
            majorVersion = header[3].toInt()
            oldTagSize = synchsafeToInt(header[6], header[7], header[8], header[9])
            tagBytes = ByteArray(oldTagSize)
            readFully(input, tagBytes, 0, oldTagSize)
        } else {
            // No ID3v2 tag: whatever we already read from `header` is actually the start of audio data.
            leftoverAfterHeader = header.copyOf(headerRead)
        }

        val keptFrames = ArrayList<ByteArray>()
        if (tagBytes.isNotEmpty()) {
            var offset = 0
            while (offset + 10 <= tagBytes.size) {
                val frameId = String(tagBytes, offset, 4, Charsets.ISO_8859_1)
                if (frameId.isBlank() || frameId[0] == '\u0000') break // padding reached

                val frameSize = if (majorVersion >= 4) {
                    synchsafeToInt(tagBytes[offset + 4], tagBytes[offset + 5], tagBytes[offset + 6], tagBytes[offset + 7])
                } else {
                    beInt(tagBytes[offset + 4], tagBytes[offset + 5], tagBytes[offset + 6], tagBytes[offset + 7])
                }
                val frameTotal = 10 + frameSize
                if (frameSize < 0 || offset + frameTotal > tagBytes.size) break

                val managed = frameId == "TIT2" || frameId == "TPE1" || frameId == "TALB" ||
                    frameId == "TRCK" || frameId == "TCON" ||
                    (frameId == "APIC" && edits.artwork != ArtworkAction.Keep)

                if (!managed) {
                    keptFrames.add(tagBytes.copyOfRange(offset, offset + frameTotal))
                }
                offset += frameTotal
            }
        }

        // Build the replacement frames from the edits.
        val newFrames = ByteArrayOutputStream()
        keptFrames.forEach { newFrames.write(it) }
        if (edits.title.isNotBlank()) newFrames.write(buildTextFrame("TIT2", edits.title))
        if (edits.artist.isNotBlank()) newFrames.write(buildTextFrame("TPE1", edits.artist))
        if (edits.album.isNotBlank()) newFrames.write(buildTextFrame("TALB", edits.album))
        if (edits.trackNumber > 0) newFrames.write(buildTextFrame("TRCK", edits.trackNumber.toString()))
        if (edits.genre.isNotBlank()) newFrames.write(buildTextFrame("TCON", edits.genre))
        val artworkAction = edits.artwork
        if (artworkAction is ArtworkAction.Replace) {
            newFrames.write(buildApicFrame(artworkAction.mimeType, artworkAction.bytes))
        }
        val framesBytes = newFrames.toByteArray()

        tempFile.outputStream().use { out: OutputStream ->
            out.write(byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), 3, 0, 0))
            out.write(intToSynchsafe(framesBytes.size))
            out.write(framesBytes)

            // Whatever we already buffered while probing for the header (when there was no tag)
            // is the very start of the audio stream and must be written before the rest of it.
            leftoverAfterHeader?.let { out.write(it) }
            input.copyTo(out, bufferSize = 64 * 1024)
        }
    }

    private fun readFully(input: InputStream, buffer: ByteArray, offset: Int, length: Int): Int {
        var total = 0
        while (total < length) {
            val read = input.read(buffer, offset + total, length - total)
            if (read < 0) break
            total += read
        }
        return total
    }

    // -------------------------------------------------------------------------------------
    // Frame builders
    // -------------------------------------------------------------------------------------

    private fun buildTextFrame(id: String, text: String): ByteArray {
        val bom = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) // UTF-16 LE BOM
        val textBytes = text.toByteArray(Charsets.UTF_16LE)
        val data = byteArrayOf(1) + bom + textBytes // encoding=1 -> UTF-16
        return frameHeader(id, data.size) + data
    }

    private fun buildApicFrame(mimeType: String, pictureBytes: ByteArray): ByteArray {
        val mimeBytes = mimeType.toByteArray(Charsets.ISO_8859_1) + byteArrayOf(0)
        val data = byteArrayOf(0) + mimeBytes + byteArrayOf(3) + byteArrayOf(0) + pictureBytes
        return frameHeader("APIC", data.size) + data
    }

    private fun frameHeader(id: String, dataSize: Int): ByteArray {
        val idBytes = id.toByteArray(Charsets.ISO_8859_1)
        val sizeBytes = beIntBytes(dataSize) // ID3v2.3 frame sizes are plain big-endian, not synchsafe
        val flags = byteArrayOf(0, 0)
        return idBytes + sizeBytes + flags
    }

    // -------------------------------------------------------------------------------------
    // Integer helpers
    // -------------------------------------------------------------------------------------

    private fun synchsafeToInt(b0: Byte, b1: Byte, b2: Byte, b3: Byte): Int =
        ((b0.toInt() and 0x7F) shl 21) or ((b1.toInt() and 0x7F) shl 14) or
            ((b2.toInt() and 0x7F) shl 7) or (b3.toInt() and 0x7F)

    private fun beInt(b0: Byte, b1: Byte, b2: Byte, b3: Byte): Int =
        ((b0.toInt() and 0xFF) shl 24) or ((b1.toInt() and 0xFF) shl 16) or
            ((b2.toInt() and 0xFF) shl 8) or (b3.toInt() and 0xFF)

    private fun beIntBytes(v: Int): ByteArray = byteArrayOf(
        ((v shr 24) and 0xFF).toByte(), ((v shr 16) and 0xFF).toByte(),
        ((v shr 8) and 0xFF).toByte(), (v and 0xFF).toByte()
    )

    private fun intToSynchsafe(v: Int): ByteArray = byteArrayOf(
        ((v shr 21) and 0x7F).toByte(), ((v shr 14) and 0x7F).toByte(),
        ((v shr 7) and 0x7F).toByte(), (v and 0x7F).toByte()
    )
}
