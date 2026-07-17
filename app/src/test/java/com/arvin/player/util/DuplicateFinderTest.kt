package com.arvin.player.util

import com.arvin.player.data.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateFinderTest {

    private fun song(id: Long, title: String, artist: String) = Song(
        id = id, title = title, artist = artist, album = "Album", albumId = 1,
        genre = null, durationMs = 200_000, path = "/music/$id.mp3",
        trackNumber = 1, dateAdded = 0, size = 1000
    )

    @Test
    fun `finds exact duplicates by title and artist`() {
        val songs = listOf(
            song(1, "Yellow", "Coldplay"),
            song(2, "Yellow", "Coldplay"),
            song(3, "Clocks", "Coldplay")
        )
        val duplicates = DuplicateFinder.find(songs)
        assertEquals(1, duplicates.size)
        assertEquals(2, duplicates.first().songs.size)
    }

    @Test
    fun `is case and whitespace insensitive`() {
        val songs = listOf(
            song(1, " Yellow ", "Coldplay"),
            song(2, "yellow", "COLDPLAY")
        )
        val duplicates = DuplicateFinder.find(songs)
        assertEquals(1, duplicates.size)
        assertEquals(2, duplicates.first().songs.size)
    }

    @Test
    fun `no duplicates returns empty list`() {
        val songs = listOf(song(1, "A", "X"), song(2, "B", "Y"))
        assertTrue(DuplicateFinder.find(songs).isEmpty())
    }

    @Test
    fun `different artist same title is not a duplicate`() {
        val songs = listOf(song(1, "Hello", "Adele"), song(2, "Hello", "Lionel Richie"))
        assertTrue(DuplicateFinder.find(songs).isEmpty())
    }
}
