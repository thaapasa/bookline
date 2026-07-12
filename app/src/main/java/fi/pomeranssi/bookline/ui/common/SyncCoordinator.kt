package fi.pomeranssi.bookline.ui.common

import android.util.Log
import fi.pomeranssi.bookline.data.repository.BookRepository
import fi.pomeranssi.bookline.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Result of the most recent sync attempt.
 */
sealed interface SyncResult {
    /** No sync has been attempted yet. */
    data object None : SyncResult

    /** Last sync completed successfully. */
    data class Success(
        val bookCount: Int,
    ) : SyncResult

    /** Last sync failed with an error. */
    data class Error(
        val message: String,
    ) : SyncResult
}

/**
 * Shared sync coordinator that guarantees at most one sync runs at a time.
 *
 * All ViewModels should delegate to this singleton instead of managing sync
 * individually. Concurrent [requestSync] calls will observe the already-running
 * sync rather than starting a new one.
 */
class SyncCoordinator(
    private val settingsRepository: SettingsRepository,
    private val bookRepository: BookRepository,
) {
    private companion object {
        const val TAG = "SyncCoordinator"
    }

    private val mutex = Mutex()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _lastSyncResult = MutableStateFlow<SyncResult>(SyncResult.None)
    val lastSyncResult: StateFlow<SyncResult> = _lastSyncResult.asStateFlow()

    /** The currently running sync job, if any. */
    private var activeSyncJob: Job? = null

    /**
     * Sync if data is stale. Safe to call on every screen resume.
     * If a sync is already running, this is a no-op.
     */
    fun checkSync(scope: CoroutineScope) {
        if (settingsRepository.feedUrl.value.isBlank()) {
            Log.d(TAG, "checkSync: skipped, no feed URL")
            return
        }
        if (!bookRepository.isSyncNeeded()) {
            Log.d(TAG, "checkSync: data is fresh, skipping sync")
            return
        }
        launchSync(scope)
    }

    /**
     * Force a sync (e.g. pull-to-refresh).
     * If a sync is already running, the caller observes its completion
     * via [isRefreshing] — no duplicate sync is started.
     */
    fun requestSync(scope: CoroutineScope) {
        if (settingsRepository.feedUrl.value.isBlank()) return
        launchSync(scope)
    }

    /** Clear the last sync error (e.g. after the user has seen it). */
    fun clearError() {
        val current = _lastSyncResult.value
        if (current is SyncResult.Error) {
            _lastSyncResult.value = SyncResult.None
        }
    }

    private fun launchSync(scope: CoroutineScope) {
        // Atomic claim — at most one launch wins. Subsequent callers no-op until done.
        if (!_isRefreshing.compareAndSet(expect = false, update = true)) {
            Log.d(TAG, "launchSync: sync already in progress, skipping")
            return
        }
        activeSyncJob =
            scope.launch {
                mutex.withLock {
                    val feedUrl = settingsRepository.feedUrl.value
                    if (feedUrl.isBlank()) {
                        _isRefreshing.value = false
                        return@withLock
                    }
                    try {
                        val count = bookRepository.sync(feedUrl)
                        _lastSyncResult.value = SyncResult.Success(count)
                        Log.i(TAG, "Sync completed: $count books")
                    } catch (e: Exception) {
                        val message = e.message ?: "Unknown error"
                        _lastSyncResult.value = SyncResult.Error(message)
                        Log.e(TAG, "Sync failed", e)
                    } finally {
                        _isRefreshing.value = false
                        activeSyncJob = null
                    }
                }
            }
    }
}
