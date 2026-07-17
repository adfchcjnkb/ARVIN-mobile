package com.arvin.player.data.repository

import android.content.Context
import android.provider.MediaStore
import com.arvin.player.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Scans the device's shared media store for playable audio files.
 * Supports whatever formats the OS's extractor supports on-device:
 * MP3, FLAC, WAV, OGG, M4A/AAC. WMA support depends on the device/OEM extractor.
 */
class MediaScanner(private val context: Context) {

    suspend fun scanAllSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()

        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.GENRE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.SIZE
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 5000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val genreCol = cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

            while (cursor.moveToNext()) {
                songs.add(
                    Song(
                        id = cursor.getLong(idCol),
                        title = cursor.getString(titleCol) ?: "Unknown",
                        artist = cursor.getString(artistCol) ?: "Unknown Artist",
                        album = cursor.getString(albumCol) ?: "Unknown Album",
                        albumId = cursor.getLong(albumIdCol),
                        genre = if (genreCol >= 0) cursor.getString(genreCol) else null,
                        durationMs = cursor.getLong(durationCol),
                        path = cursor.getString(dataCol) ?: "",
                        trackNumber = if (trackCol >= 0) cursor.getInt(trackCol) else 0,
                        dateAdded = cursor.getLong(dateCol),
                        size = cursor.getLong(sizeCol)
                    )
                )
            }
        }
        songs
    }

    /** Finds likely-duplicate tracks: same normalized title + artist. */
    fun findDuplicates(songs: List<Song>): List<com.arvin.player.data.model.DuplicateGroup> =
        com.arvin.player.util.DuplicateFinder.find(songs)

    private val audioExtensions = setOf("mp3", "flac", "wav", "ogg", "m4a", "wma", "aac")

    /**
     * Scans a user-picked folder tree (via Storage Access Framework) for audio files that
     * MediaStore might not have indexed — e.g. a folder outside the standard Music directory.
     * Metadata comes from [android.media.MediaMetadataRetriever] since these files aren't in
     * MediaStore. IDs are derived from a stable hash of the file's URI so they don't collide
     * with real MediaStore IDs and stay stable across rescans.
     */
    suspend fun scanCustomFolder(treeUri: android.net.Uri): List<Song> = withContext(Dispatchers.IO) {
        val root = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        val results = mutableListOf<Song>()
        fun walk(dir: androidx.documentfile.provider.DocumentFile) {
            for (child in dir.listFiles()) {
                if (child.isDirectory) {
                    walk(child)
                } else if (child.isFile) {
                    val name = child.name ?: continue
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (ext !in audioExtensions) continue
                    val retriever = android.media.MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, child.uri)
                        val title = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE) ?: name
                        val artist = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
                        val album = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown Album"
                        val genre = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_GENRE)
                        val duration = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                        if (duration < 5000) continue // same short-clip filter as the MediaStore scan
                        results.add(
                            Song(
                                id = child.uri.toString().hashCode().toLong(),
                                title = title,
                                artist = artist,
                                album = album,
                                albumId = album.hashCode().toLong(),
                                genre = genre,
                                durationMs = duration,
                                path = child.uri.toString(),
                                trackNumber = 0,
                                dateAdded = child.lastModified(),
                                size = child.length()
                            )
                        )
                    } catch (_: Exception) {
                        // Unreadable/corrupt file in a custom folder — skip it rather than crash the scan.
                    } finally {
                        retriever.release()
                    }
                }
            }
        }
        walk(root)
        results
    }
}
