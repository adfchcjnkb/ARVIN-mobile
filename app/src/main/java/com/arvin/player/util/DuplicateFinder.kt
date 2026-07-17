package com.arvin.player.util

import com.arvin.player.data.model.DuplicateGroup
import com.arvin.player.data.model.Song

/** Pure function, no Android dependencies — easy to unit test. */
object DuplicateFinder {
    fun find(songs: List<Song>): List<DuplicateGroup> {
        return songs.groupBy { "${it.title.trim().lowercase()}|${it.artist.trim().lowercase()}" }
            .values
            .filter { it.size > 1 }
            .map { DuplicateGroup(it.first().title, it.first().artist, it) }
    }
}
