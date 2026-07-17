package com.arvin.player.util

import com.arvin.player.data.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SongFilterTest {

    private fun song(title: String, artist: String, album: String) = Song(
        id = title.hashCode().toLong(), title = title, artist = artist, album = album, albumId = 1,
        genre = null, durationMs = 200_000, path = "/music/$title.mp3",
        trackNumber = 1, dateAdded = 0, size = 1000
    )

    private val library = listOf(
        song("Bohemian Rhapsody", "Queen", "A Night at the Opera"),
        song("Radio Ga Ga", "Queen", "The Works"),
        song("Imagine", "John Lennon", "Imagine")
    )

    @Test
    fun `blank query returns nothing`() {
        assertTrue(SongFilter.search(library, "").isEmpty())
        assertTrue(SongFilter.search(library, "   ").isEmpty())
    }

    @Test
    fun `matches by title case-insensitively`() {
        val results = SongFilter.search(library, "bohemian")
        assertEquals(1, results.size)
        assertEquals("Bohemian Rhapsody", results.first().title)
    }

    @Test
    fun `matches by artist`() {
        val results = SongFilter.search(library, "queen")
        assertEquals(2, results.size)
    }

    @Test
    fun `matches by album`() {
        val results = SongFilter.search(library, "imagine")
        // matches both the "Imagine" title and the "Imagine" album, same song, expect 1 unique result
        assertEquals(1, results.size)
    }

    @Test
    fun `no match returns empty list`() {
        assertTrue(SongFilter.search(library, "nonexistent song xyz").isEmpty())
    }
}
