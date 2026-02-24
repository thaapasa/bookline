package fi.pomeranssi.bookline.ui.settings

import androidx.lifecycle.ViewModel
import fi.pomeranssi.bookline.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    /** Current text in the URL field (may not yet be saved). */
    private val _urlField = MutableStateFlow(settingsRepository.feedUrl.value)
    val urlField: StateFlow<String> = _urlField.asStateFlow()

    /** Whether the last save was successful (for a brief confirmation). */
    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    fun onUrlChanged(value: String) {
        _urlField.value = value
        _saved.value = false
    }

    fun save() {
        settingsRepository.saveFeedUrl(_urlField.value)
        _saved.value = true
    }
}

