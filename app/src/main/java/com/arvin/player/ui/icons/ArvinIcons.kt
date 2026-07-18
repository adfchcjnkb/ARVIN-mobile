package com.arvin.player.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.HideImage
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Facade over Material's Rounded icon set. Using the standard, professionally-drawn Material icons
 * (a real gear for Settings, a proper eye for Visibility, etc.) keeps every glyph instantly
 * recognisable and crisp at any density, while this single indirection means every screen keeps
 * referring to `ArvinIcons.X` and the icon language can be swapped in one place.
 *
 * RTL note: the transport glyphs (Play/Pause/SkipNext/SkipPrevious) are the non-mirrored Rounded
 * variants — media controls must keep their direction in Persian/Arabic. Only ArrowBack, QueueMusic
 * and PlaylistAdd use the AutoMirrored variants, which is correct for those directional glyphs.
 */
object ArvinIcons {
    val Play: ImageVector get() = Icons.Rounded.PlayArrow
    val Pause: ImageVector get() = Icons.Rounded.Pause
    val SkipNext: ImageVector get() = Icons.Rounded.SkipNext
    val SkipPrevious: ImageVector get() = Icons.Rounded.SkipPrevious
    val Shuffle: ImageVector get() = Icons.Rounded.Shuffle
    val Repeat: ImageVector get() = Icons.Rounded.Repeat
    val RepeatOne: ImageVector get() = Icons.Rounded.RepeatOne
    val HeartFilled: ImageVector get() = Icons.Rounded.Favorite
    val HeartOutline: ImageVector get() = Icons.Rounded.FavoriteBorder
    val Search: ImageVector get() = Icons.Rounded.Search
    val Tune: ImageVector get() = Icons.Rounded.GraphicEq
    val Bedtime: ImageVector get() = Icons.Rounded.Bedtime
    val ChevronDown: ImageVector get() = Icons.Rounded.KeyboardArrowDown
    val ArrowBack: ImageVector get() = Icons.AutoMirrored.Rounded.ArrowBack
    val Add: ImageVector get() = Icons.Rounded.Add
    val MoreVert: ImageVector get() = Icons.Rounded.MoreVert
    val QueueMusic: ImageVector get() = Icons.AutoMirrored.Rounded.QueueMusic
    val Settings: ImageVector get() = Icons.Rounded.Settings
    val MusicNote: ImageVector get() = Icons.Rounded.MusicNote
    val Delete: ImageVector get() = Icons.Rounded.DeleteOutline
    val Lock: ImageVector get() = Icons.Rounded.Lock
    val Fingerprint: ImageVector get() = Icons.Rounded.Fingerprint
    val Visibility: ImageVector get() = Icons.Rounded.Visibility
    val VisibilityOff: ImageVector get() = Icons.Rounded.VisibilityOff
    val Folder: ImageVector get() = Icons.Rounded.CreateNewFolder
    val Speed: ImageVector get() = Icons.Rounded.Speed
    val PlaylistAdd: ImageVector get() = Icons.AutoMirrored.Rounded.PlaylistAdd
    val Share: ImageVector get() = Icons.Rounded.Share
    val Edit: ImageVector get() = Icons.Rounded.Edit
    val Image: ImageVector get() = Icons.Rounded.Image
    val HideImage: ImageVector get() = Icons.Rounded.HideImage
    val CheckCircle: ImageVector get() = Icons.Rounded.CheckCircle
    val CircleOutline: ImageVector get() = Icons.Rounded.RadioButtonUnchecked
}
