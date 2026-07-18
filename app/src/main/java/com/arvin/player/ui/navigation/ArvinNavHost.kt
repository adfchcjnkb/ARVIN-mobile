package com.arvin.player.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.arvin.player.ui.screens.equalizer.EqualizerScreen
import com.arvin.player.ui.screens.hidden.HiddenSongsScreen
import com.arvin.player.ui.screens.library.LibraryScreen
import com.arvin.player.ui.screens.player.PlayerScreen
import com.arvin.player.ui.screens.playlist.PlaylistDetailScreen
import com.arvin.player.ui.screens.playlist.PlaylistsScreen
import com.arvin.player.ui.screens.search.SearchScreen
import com.arvin.player.ui.screens.settings.SettingsScreen

object Routes {
    const val LIBRARY = "library"
    const val PLAYER = "player"
    const val PLAYLISTS = "playlists"
    const val PLAYLIST_DETAIL = "playlist_detail/{playlistId}"
    const val EQUALIZER = "equalizer"
    const val SETTINGS = "settings"
    const val SEARCH = "search"
    const val HIDDEN_SONGS = "hidden_songs"
    const val FAVORITES = "favorites"

    fun playlistDetail(id: Long) = "playlist_detail/$id"
}

private const val ANIM = 320

@Composable
fun ArvinNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Routes.LIBRARY,
        // Default push/pop: a gentle horizontal slide + fade for pages that stack on the library.
        enterTransition = { slideInHorizontally(tween(ANIM)) { it / 6 } + fadeIn(tween(ANIM)) },
        exitTransition = { fadeOut(tween(ANIM / 2)) },
        popEnterTransition = { fadeIn(tween(ANIM)) },
        popExitTransition = { slideOutHorizontally(tween(ANIM)) { it / 6 } + fadeOut(tween(ANIM)) }
    ) {
        composable(Routes.LIBRARY) { LibraryScreen(navController) }
        composable(
            Routes.PLAYER,
            // The player rises from the bottom like a "now playing" sheet and drops back down.
            enterTransition = { slideInVertically(tween(ANIM)) { it } + fadeIn(tween(ANIM)) },
            exitTransition = { fadeOut(tween(ANIM / 2)) },
            popEnterTransition = { fadeIn(tween(ANIM)) },
            popExitTransition = { slideOutVertically(tween(ANIM)) { it } + fadeOut(tween(ANIM)) }
        ) { PlayerScreen(navController) }
        composable(Routes.PLAYLISTS) { PlaylistsScreen(navController) }
        composable(Routes.PLAYLIST_DETAIL) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("playlistId")?.toLongOrNull() ?: 0L
            PlaylistDetailScreen(navController, id)
        }
        composable(Routes.EQUALIZER) { EqualizerScreen(navController) }
        composable(Routes.SETTINGS) { SettingsScreen(navController) }
        composable(Routes.SEARCH) { SearchScreen(navController) }
        composable(Routes.HIDDEN_SONGS) { HiddenSongsScreen(navController) }
        composable(Routes.FAVORITES) { com.arvin.player.ui.screens.favorites.FavoritesScreen(navController) }
    }
}
