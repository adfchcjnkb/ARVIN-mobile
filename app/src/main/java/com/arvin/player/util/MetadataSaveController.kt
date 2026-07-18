package com.arvin.player.util

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.arvin.player.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class MetadataSaveResult {
    object Success : MetadataSaveResult()
    data class NeedsPermission(val intentSender: IntentSender) : MetadataSaveResult()
    object UnsupportedFormat : MetadataSaveResult()
    data class Failed(val message: String?) : MetadataSaveResult()
}

object MetadataSaveController {

    fun mediaUriFor(song: Song): Uri =
        if (song.path.startsWith("content://") || song.path.startsWith("file://")) {
            Uri.parse(song.path)
        } else {
            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
        }

    suspend fun save(context: Context, song: Song, edits: TagEdits): MetadataSaveResult =
        withContext(Dispatchers.IO) {
            val uri = mediaUriFor(song)
            try {
                if (Id3TagEditor.isSupportedFormat(song.path)) {
                    val error = Id3TagEditor.writeTags(context, uri, edits)
                    if (error != null) {
                        recoverableIntentSender(error)?.let { return@withContext MetadataSaveResult.NeedsPermission(it) }
                        return@withContext MetadataSaveResult.Failed(error.message)
                    }
                }

                // Keep MediaStore's own record (and thus this app's scanned Song list) in sync
                // immediately, rather than waiting on a background rescan.
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.TITLE, edits.title)
                    put(MediaStore.Audio.Media.ARTIST, edits.artist)
                    put(MediaStore.Audio.Media.ALBUM, edits.album)
                    if (edits.trackNumber > 0) put(MediaStore.Audio.Media.TRACK, edits.trackNumber)
                }
                context.contentResolver.update(uri, values, null, null)

                if (!song.path.startsWith("content://")) {
                    MediaScannerConnection.scanFile(context, arrayOf(song.path), null, null)
                }

                if (!Id3TagEditor.isSupportedFormat(song.path)) {
                    MetadataSaveResult.UnsupportedFormat
                } else {
                    MetadataSaveResult.Success
                }
            } catch (e: SecurityException) {
                recoverableIntentSender(e)?.let { return@withContext MetadataSaveResult.NeedsPermission(it) }
                MetadataSaveResult.Failed(e.message)
            } catch (e: Exception) {
                MetadataSaveResult.Failed(e.message)
            }
        }

    /** Isolated in its own method so the RecoverableSecurityException class reference (API 29+)
     *  never needs to be resolved on older OS versions where it doesn't exist. */
    private fun recoverableIntentSender(e: Throwable): IntentSender? {
        if (Build.VERSION.SDK_INT < 29) return null
        return recoverableIntentSenderApi29(e)
    }

    private fun recoverableIntentSenderApi29(e: Throwable): IntentSender? =
        (e as? android.app.RecoverableSecurityException)?.userAction?.actionIntent?.intentSender
}
