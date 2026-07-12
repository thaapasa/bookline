package fi.pomeranssi.bookline.data.repository

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores app settings in SharedPreferences with values encrypted via an
 * AES-256-GCM key held in the Android Keystore. The Keystore key never
 * leaves the TEE/Strongbox, so the feed URL (which contains a private
 * Goodreads access key) is protected at rest.
 */
class SettingsRepository(
    context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    private val _feedUrl = MutableStateFlow(getDecrypted(KEY_FEED_URL))

    /** Observable feed URL — empty string means "not configured". */
    val feedUrl: StateFlow<String> = _feedUrl.asStateFlow()

    /** Returns `true` when the user has configured a feed URL. */
    val isFeedConfigured: Boolean
        get() = _feedUrl.value.isNotBlank()

    fun saveFeedUrl(url: String) {
        val trimmed = url.trim()
        putEncrypted(KEY_FEED_URL, trimmed)
        _feedUrl.value = trimmed
    }

    /** Epoch millis of the last successful book sync, or 0 if never synced. */
    var lastSyncEpochMs: Long
        get() = prefs.getLong(KEY_LAST_SYNC, 0L)
        set(value) = prefs.edit { putLong(KEY_LAST_SYNC, value) }

    /** Returns `true` when the cached data is older than [maxAgeMs]. */
    fun isSyncStale(maxAgeMs: Long = SYNC_MAX_AGE_MS): Boolean = System.currentTimeMillis() - lastSyncEpochMs > maxAgeMs

    // ---- encryption helpers ------------------------------------------------

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        keyStore.getEntry(KEYSTORE_ALIAS, null)?.let { entry ->
            return (entry as KeyStore.SecretKeyEntry).secretKey
        }
        val keyGen =
            KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE,
            )
        keyGen.init(
            KeyGenParameterSpec
                .Builder(
                    KEYSTORE_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(AES_KEY_SIZE)
                .build(),
        )
        return keyGen.generateKey()
    }

    private fun putEncrypted(
        key: String,
        plaintext: String,
    ) {
        if (plaintext.isEmpty()) {
            prefs.edit { remove(key) }
            return
        }
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv
        // Store as "base64(iv):base64(ciphertext)"
        val encoded =
            Base64.encodeToString(iv, Base64.NO_WRAP) +
                ":" +
                Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        prefs.edit { putString(key, encoded) }
    }

    private fun getDecrypted(key: String): String {
        val encoded = prefs.getString(key, null) ?: return ""
        return try {
            val parts = encoded.split(":")
            if (parts.size != 2) return ""
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (_: Exception) {
            // If decryption fails (e.g. key was wiped), treat as empty.
            ""
        }
    }

    companion object {
        private const val PREFS_FILE = "bookline_settings"
        private const val KEY_FEED_URL = "goodreads_feed_url"
        private const val KEY_LAST_SYNC = "last_sync_epoch_ms"
        private const val SYNC_MAX_AGE_MS = 24 * 60 * 60 * 1000L // 24 hours
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEYSTORE_ALIAS = "bookline_settings_key"
        private const val AES_KEY_SIZE = 256
        private const val GCM_TAG_LENGTH = 128
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
