package com.arvin.player.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

/** Cross-reference table: which songs (by MediaStore id) belong to which playlist, and in what order. */
@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long,
    val position: Int
)

@Entity(tableName = "eq_presets")
data class EqPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** 10 band gain levels in millibels, comma-separated (e.g. "300,200,0,-100,...") */
    val bandLevelsCsv: String,
    val isBuiltIn: Boolean = false
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val songId: Long,
    val addedAt: Long = System.currentTimeMillis()
)

/** Songs the user has chosen to hide from the main library (requires PIN to view/unhide). */
@Entity(tableName = "hidden_songs")
data class HiddenSongEntity(
    @PrimaryKey val songId: Long,
    val hiddenAt: Long = System.currentTimeMillis()
)
