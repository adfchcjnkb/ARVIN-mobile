package com.arvin.player.data.model

/** A song discovered on-device via MediaStore. */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val genre: String?,
    val durationMs: Long,
    val path: String,
    val trackNumber: Int,
    val dateAdded: Long,
    val size: Long
)

data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val songCount: Int
)

data class Artist(
    val name: String,
    val songCount: Int,
    val albumCount: Int
)

enum class RepeatMode { OFF, ONE, ALL }

enum class AppTheme { LIGHT, DARK, SYSTEM }

/** Result of a duplicate scan: songs that look identical (same title+artist+duration bucket). */
data class DuplicateGroup(
    val title: String,
    val artist: String,
    val songs: List<Song>
)
