package com.arvin.player.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Tracks whether the audio-library read permission is currently granted, so screens
 * can show a proper "grant access" state instead of silently showing an empty library.
 */
object PermissionState {
    private val _audioPermissionGranted = MutableStateFlow(false)
    val audioPermissionGranted: StateFlow<Boolean> = _audioPermissionGranted

    fun audioPermissionName(): String =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    fun refresh(context: Context) {
        _audioPermissionGranted.value = ContextCompat.checkSelfPermission(
            context, audioPermissionName()
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun setGranted(granted: Boolean) {
        _audioPermissionGranted.value = granted
    }
}
