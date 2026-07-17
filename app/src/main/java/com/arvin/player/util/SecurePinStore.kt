package com.arvin.player.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest

/**
 * Stores the app-lock PIN behind Android Keystore-backed encryption (via EncryptedSharedPreferences)
 * instead of a plain preference file. This is a meaningfully stronger boundary than the earlier
 * plain-DataStore SHA-256 hash:
 *   - the encryption key itself lives in the hardware-backed Android Keystore and never leaves it
 *   - the on-disk preferences file is AES-256 encrypted, not just a hash sitting in a readable file
 *
 * Still, be precise about what this does and doesn't protect against: on a device the user has
 * rooted, or one accessed via ADB with debugging/backup extraction enabled, the app's own process
 * (and therefore this store) can still be reached. This raises the bar significantly above a
 * "clear-text-adjacent" hash file, but it is not a substitute for full-disk encryption or a
 * dedicated security product.
 */
object SecurePinStore {

    private const val PREFS_NAME = "arvin_secure_prefs"
    private const val KEY_PIN_HASH = "pin_hash"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"

    private val _hasPin = kotlinx.coroutines.flow.MutableStateFlow<Boolean?>(null)
    /** Null until first queried (needs a Context); observe this instead of polling hasPin(). */
    val hasPinState: kotlinx.coroutines.flow.StateFlow<Boolean?> = _hasPin

    private fun prefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private fun hash(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun hasPin(context: Context): Boolean {
        val result = prefs(context).contains(KEY_PIN_HASH)
        _hasPin.value = result
        return result
    }

    fun setPin(context: Context, pin: String?) {
        prefs(context).edit().apply {
            if (pin.isNullOrBlank()) {
                remove(KEY_PIN_HASH)
                remove(KEY_BIOMETRIC_ENABLED) // no PIN means biometric-only unlock makes no sense either
            } else {
                putString(KEY_PIN_HASH, hash(pin))
            }
        }.apply()
        _hasPin.value = !pin.isNullOrBlank()
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val stored = prefs(context).getString(KEY_PIN_HASH, null) ?: return false
        return stored == hash(pin)
    }

    /** Biometric unlock is only meaningful as an alternative to an existing PIN, never a replacement
     *  for it (there's always a PIN fallback if biometrics fail or aren't enrolled). */
    fun isBiometricEnabled(context: Context): Boolean =
        hasPin(context) && prefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, false)

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }
}
