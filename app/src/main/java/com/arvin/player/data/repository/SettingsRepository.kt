package com.arvin.player.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.arvin.player.data.model.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "arvin_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val LANGUAGE = stringPreferencesKey("language") // ISO code, e.g. "fa", "en"
        val CROSSFADE_MS = intPreferencesKey("crossfade_ms")
        val GAPLESS = booleanPreferencesKey("gapless")
        val BASS_BOOST = intPreferencesKey("bass_boost")
        val VIRTUALIZER = intPreferencesKey("virtualizer")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val CUSTOM_FOLDERS = androidx.datastore.preferences.core.stringSetPreferencesKey("custom_folders")
    }

    // NOTE: the PIN used to be hashed and stored here in plain DataStore. It now lives in
    // SecurePinStore, which is backed by Android Keystore-encrypted EncryptedSharedPreferences —
    // a meaningfully stronger boundary. See util/SecurePinStore.kt.

    val customFolders: Flow<Set<String>> = context.dataStore.data.map { it[Keys.CUSTOM_FOLDERS] ?: emptySet() }
    suspend fun addCustomFolder(uri: String) {
        context.dataStore.edit {
            val current = it[Keys.CUSTOM_FOLDERS] ?: emptySet()
            it[Keys.CUSTOM_FOLDERS] = current + uri
        }
    }
    suspend fun removeCustomFolder(uri: String) {
        context.dataStore.edit {
            val current = it[Keys.CUSTOM_FOLDERS] ?: emptySet()
            it[Keys.CUSTOM_FOLDERS] = current - uri
        }
    }

    val theme: Flow<AppTheme> = context.dataStore.data.map {
        AppTheme.valueOf(it[Keys.THEME] ?: AppTheme.SYSTEM.name)
    }
    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { it[Keys.THEME] = theme.name }
    }

    val language: Flow<String> = context.dataStore.data.map { it[Keys.LANGUAGE] ?: "en" }
    suspend fun setLanguage(code: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = code }
    }

    val crossfadeMs: Flow<Int> = context.dataStore.data.map { it[Keys.CROSSFADE_MS] ?: 0 }
    suspend fun setCrossfadeMs(ms: Int) {
        context.dataStore.edit { it[Keys.CROSSFADE_MS] = ms }
    }

    val gaplessEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.GAPLESS] ?: true }
    suspend fun setGapless(on: Boolean) {
        context.dataStore.edit { it[Keys.GAPLESS] = on }
    }

    companion object {
        @Volatile private var instance: SettingsRepository? = null
        fun getInstance(context: Context): SettingsRepository =
            instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also { instance = it }
            }
    }
}

/** The 15 supported languages, with their ISO code, RTL flag, and native display name. */
val SUPPORTED_LANGUAGES = listOf(
    LanguageOption("fa", "فارسی", rtl = true),
    LanguageOption("en", "English", rtl = false),
    LanguageOption("ar", "العربية", rtl = true),
    LanguageOption("tr", "Türkçe", rtl = false),
    LanguageOption("ru", "Русский", rtl = false),
    LanguageOption("de", "Deutsch", rtl = false),
    LanguageOption("fr", "Français", rtl = false),
    LanguageOption("es", "Español", rtl = false),
    LanguageOption("pt", "Português", rtl = false),
    LanguageOption("zh", "中文", rtl = false),
    LanguageOption("ja", "日本語", rtl = false),
    LanguageOption("ko", "한국어", rtl = false),
    LanguageOption("hi", "हिन्दी", rtl = false),
    LanguageOption("it", "Italiano", rtl = false),
    LanguageOption("in", "Bahasa Indonesia", rtl = false)
)

data class LanguageOption(val code: String, val nativeName: String, val rtl: Boolean)
