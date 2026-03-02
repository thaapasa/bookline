package fi.pomeranssi.bookline.ui.common

import android.util.Log
import fi.pomeranssi.bookline.data.repository.BookRepository
import fi.pomeranssi.bookline.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shared sync logic for ViewModels that need to sync the RSS feed.
 *
 * Each ViewModel creates its own instance and delegates [checkSync] / [refresh].
 */
class SyncHelper(
    private val settingsRepository: SettingsRepository,
    private val bookRepository: BookRepository,
    private val tag: String,
) {
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /** Sync if data is stale. Safe to call on every screen resume. */
    fun checkSync(scope: CoroutineScope) {
        if (settingsRepository.feedUrl.value.isBlank()) {
            Log.d(tag, "syncIfNeeded: skipped, no feed URL")
            return
        }
        if (bookRepository.isSyncNeeded()) {
            Log.i(tag, "syncIfNeeded: sync is needed, starting")
            syncFeed(scope)
        } else {
            Log.d(tag, "syncIfNeeded: data is fresh, skipping sync")
        }
    }

    /** Force a sync (e.g. pull-to-refresh). */
    fun syncFeed(scope: CoroutineScope) {
        val feedUrl = settingsRepository.feedUrl.value
        if (feedUrl.isBlank()) return

        _isRefreshing.value = true
        scope.launch {
            try {
                val count = bookRepository.sync(feedUrl)
                Log.i(tag, "syncFeed: completed, $count books synced")
            } catch (e: Exception) {
                Log.e(tag, "syncFeed: failed", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
