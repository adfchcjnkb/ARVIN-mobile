package com.arvin.player.util

import com.arvin.player.data.model.Song

/** Pure function, no Android dependencies — easy to unit test. */
object SongFilter {
    fun search(songs: List<Song>, query: String): List<Song> {
        if (query.isBlank()) return emptyList()
        val q = query.trim().lowercase()
        return songs.filter {
            it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.album.lowercase().contains(q)
        }
    }
}
