package com.arvin.player.data.repository

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.arvin.player.data.local.AppDatabase
import com.arvin.player.data.local.FavoriteEntity
import com.arvin.player.data.local.HiddenSongEntity
import com.arvin.player.data.local.PlaylistEntity
import com.arvin.player.data.local.PlaylistSongCrossRef
import com.arvin.player.data.model.Album
import com.arvin.player.data.model.Artist
import com.arvin.player.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Single source of truth for library data. Holds the in-memory scanned library
 * (MediaStore + any user-added custom folders) plus delegates playlist/favorites/
 * hidden-songs/EQ persistence to Room. Also watches MediaStore for changes so newly
 * added or removed tracks show up without the user manually refreshing.
 */
class MusicRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val scanner = MediaScanner(context)
    private val settings = SettingsRepository.getInstance(context)
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val playlistDao = db.playlistDao()
    val eqPresetDao = db.eqPresetDao()
    val favoriteDao = db.favoriteDao()
    val hiddenSongDao = db.hiddenSongDao()

    private val _allSongsIncludingHidden = MutableStateFlow<List<Song>>(emptyList())

    private val _hiddenIds = MutableStateFlow<Set<Long>>(emptySet())

    /** Songs visible in the main library — hidden tracks are filtered out by default. */
    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs: StateFlow<List<Song>> = _allSongs.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var debounceJob: Job? = null
    private var observerRegistered = false

    init {
        repoScope.launch {
            hiddenSongDao.getAllHiddenIds().collect { ids ->
                _hiddenIds.value = ids.toSet()
                recomputeVisible()
            }
        }
    }

    /** Registers a MediaStore ContentObserver so adding/removing files on-device auto-refreshes
     *  the library without the user having to pull-to-refresh. Safe to call multiple times. */
    fun startWatchingMediaStore() {
        if (observerRegistered) return
        observerRegistered = true
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                // Debounce: multiple files changing at once (e.g. a bulk copy) should trigger one rescan.
                debounceJob?.cancel()
                debounceJob = repoScope.launch {
                    delay(1500)
                    refreshLibrary()
                }
            }
        }
        context.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer
        )
    }

    suspend fun refreshLibrary() {
        _isScanning.value = true
        val mediaStoreSongs = scanner.scanAllSongs()
        val customFolderUris = settings.customFolders.first()
        val customSongs = customFolderUris.flatMap { uriString ->
            runCatching { scanner.scanCustomFolder(Uri.parse(uriString)) }.getOrDefault(emptyList())
        }
        // De-dupe by path in case a custom folder overlaps with what MediaStore already indexed.
        val merged = (mediaStoreSongs + customSongs).distinctBy { it.path }
        _allSongsIncludingHidden.value = merged
        recomputeVisible()
        _isScanning.value = false
    }

    private fun recomputeVisible() {
        val hidden = _hiddenIds.value
        _allSongs.value = _allSongsIncludingHidden.value.filter { it.id !in hidden }
    }

    fun hiddenSongs(): List<Song> =
        _allSongsIncludingHidden.value.filter { it.id in _hiddenIds.value }

    /** Songs the user has liked, in the same order/shape as the normal library list —
     *  visible songs only (a hidden-and-liked song stays out of "Liked songs" too). */
    fun favoriteSongs(favoriteIds: List<Long>): List<Song> {
        val idSet = favoriteIds.toSet()
        return _allSongs.value.filter { it.id in idSet }
    }

    suspend fun hideSong(songId: Long) = hiddenSongDao.hide(HiddenSongEntity(songId))
    suspend fun unhideSong(songId: Long) = hiddenSongDao.unhide(songId)

    fun albums(): List<Album> =
        _allSongs.value.groupBy { it.albumId }.map { (id, songs) ->
            Album(id, songs.first().album, songs.first().artist, songs.size)
        }.sortedBy { it.name }

    fun artists(): List<Artist> =
        _allSongs.value.groupBy { it.artist }.map { (name, songs) ->
            Artist(name, songs.size, songs.map { it.albumId }.distinct().size)
        }.sortedBy { it.name }

    fun genres(): List<String> =
        _allSongs.value.mapNotNull { it.genre }.filter { it.isNotBlank() }.distinct().sorted()

    fun search(query: String): List<Song> =
        com.arvin.player.util.SongFilter.search(_allSongs.value, query)

    fun duplicates() = scanner.findDuplicates(_allSongs.value)

    suspend fun createPlaylist(name: String): Long = playlistDao.createPlaylist(PlaylistEntity(name = name))

    suspend fun addToPlaylist(playlistId: Long, songId: Long) {
        val pos = playlistDao.nextPosition(playlistId)
        playlistDao.addSongToPlaylist(PlaylistSongCrossRef(playlistId, songId, pos))
    }

    suspend fun toggleFavorite(songId: Long, isFavorite: Boolean) {
        if (isFavorite) favoriteDao.addFavorite(FavoriteEntity(songId)) else favoriteDao.removeFavorite(songId)
    }

    companion object {
        @Volatile private var instance: MusicRepository? = null
        fun getInstance(context: Context): MusicRepository =
            instance ?: synchronized(this) {
                instance ?: MusicRepository(context.applicationContext).also { instance = it }
            }
    }
}
