package com.arvin.player.ui.components

import android.content.IntentSender
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.arvin.player.R
import com.arvin.player.data.model.Song
import com.arvin.player.ui.icons.ArvinIcons
import com.arvin.player.util.ArtworkAction
import com.arvin.player.util.Id3TagEditor
import com.arvin.player.util.MetadataSaveController
import com.arvin.player.util.MetadataSaveResult
import com.arvin.player.util.TagEdits
import kotlinx.coroutines.launch

/**
 * Edit a song's title/artist/album/track/genre and cover art. Saves through
 * [MetadataSaveController], which writes real ID3v2.3 frames for MP3 (readable by any OS/player)
 * and falls back to updating MediaStore's own record for other formats.
 */
@Composable
fun EditMetadataDialog(song: Song, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember(song.id) { mutableStateOf(song.title) }
    var artist by remember(song.id) { mutableStateOf(song.artist) }
    var album by remember(song.id) { mutableStateOf(song.album) }
    var trackNumber by remember(song.id) { mutableStateOf(if (song.trackNumber > 0) song.trackNumber.toString() else "") }
    var genre by remember(song.id) { mutableStateOf(song.genre ?: "") }
    var artworkAction by remember(song.id) { mutableStateOf<ArtworkAction>(ArtworkAction.Keep) }
    var newArtworkBitmap by remember(song.id) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var saving by remember { mutableStateOf(false) }
    var pendingEdits by remember { mutableStateOf<TagEdits?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val edits = pendingEdits
        if (result.resultCode == android.app.Activity.RESULT_OK && edits != null) {
            scope.launch {
                val outcome = MetadataSaveController.save(context, song, edits)
                handleResult(outcome, onSaved, onDismiss)
            }
        } else {
            saving = false
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) {
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                artworkAction = ArtworkAction.Replace(bytes, mime)
                newArtworkBitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        }
    }

    val isMp3 = Id3TagEditor.isSupportedFormat(song.path)
    val artUri = android.net.Uri.parse("content://media/external/audio/albumart/${song.albumId}")

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(stringResource(R.string.edit_song_info)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { imagePicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        val bmp = newArtworkBitmap
                        when {
                            artworkAction is ArtworkAction.Remove -> Icon(ArvinIcons.HideImage, contentDescription = null)
                            bmp != null -> Image(bitmap = bmp.asImageBitmap(), contentDescription = null)
                            else -> AsyncImage(model = artUri, contentDescription = null)
                        }
                    }
                    Spacer(Modifier.size(12.dp))
                    Column {
                        TextButton(onClick = { imagePicker.launch("image/*") }) {
                            Text(stringResource(R.string.change_artwork))
                        }
                        TextButton(onClick = {
                            artworkAction = ArtworkAction.Remove
                            newArtworkBitmap = null
                        }) {
                            Text(stringResource(R.string.remove_artwork), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(stringResource(R.string.title_field)) }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = artist, onValueChange = { artist = it }, label = { Text(stringResource(R.string.artist_field)) }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = album, onValueChange = { album = it }, label = { Text(stringResource(R.string.album_field)) }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = trackNumber,
                    onValueChange = { v -> trackNumber = v.filter { it.isDigit() } },
                    label = { Text(stringResource(R.string.track_number_field)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = genre, onValueChange = { genre = it }, label = { Text(stringResource(R.string.genre_field)) }, modifier = Modifier.fillMaxWidth())
                if (!isMp3) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.metadata_unsupported_format),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving,
                onClick = {
                    saving = true
                    val edits = TagEdits(
                        title = title,
                        artist = artist,
                        album = album,
                        trackNumber = trackNumber.toIntOrNull() ?: 0,
                        genre = genre,
                        artwork = artworkAction
                    )
                    pendingEdits = edits
                    scope.launch {
                        when (val outcome = MetadataSaveController.save(context, song, edits)) {
                            is MetadataSaveResult.NeedsPermission -> {
                                permissionLauncher.launch(IntentSenderRequest.Builder(outcome.intentSender).build())
                            }
                            else -> handleResult(outcome, onSaved, onDismiss)
                        }
                    }
                }
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = { if (!saving) onDismiss() }) { Text(stringResource(R.string.cancel)) }
        }
    )
}

private fun handleResult(outcome: MetadataSaveResult, onSaved: () -> Unit, onDismiss: () -> Unit) {
    when (outcome) {
        is MetadataSaveResult.Success -> {
            com.arvin.player.util.AppNotifier.notify(R.string.notif_metadata_saved)
            onSaved()
            onDismiss()
        }
        is MetadataSaveResult.UnsupportedFormat -> {
            com.arvin.player.util.AppNotifier.notify(R.string.notif_metadata_saved)
            onSaved()
            onDismiss()
        }
        is MetadataSaveResult.Failed -> {
            com.arvin.player.util.AppNotifier.notify(R.string.notif_metadata_failed)
        }
        is MetadataSaveResult.NeedsPermission -> Unit // handled by the launcher path
    }
}
