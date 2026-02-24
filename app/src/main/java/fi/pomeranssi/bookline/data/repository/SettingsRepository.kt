package fi.pomeranssi.bookline.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stores app settings (including the Goodreads RSS feed URL that contains an
 * access key) in [EncryptedSharedPreferences] so that sensitive values are
 * encrypted at rest.
 */
class SettingsRepository(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val _feedUrl = MutableStateFlow(prefs.getString(KEY_FEED_URL, null).orEmpty())

    /** Observable feed URL — empty string means "not configured". */
    val feedUrl: StateFlow<String> = _feedUrl.asStateFlow()

    /** Returns `true` when the user has configured a feed URL. */
    val isFeedConfigured: Boolean
        get() = _feedUrl.value.isNotBlank()

    fun saveFeedUrl(url: String) {
        prefs.edit { putString(KEY_FEED_URL, url.trim()) }
        _feedUrl.value = url.trim()
    }

    companion object {
        private const val FILE_NAME = "bookline_secure_prefs"
        private const val KEY_FEED_URL = "goodreads_feed_url"
    }
}

