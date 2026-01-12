package xyz.jdubiel.migawka.ui.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.jdubiel.migawka.MigawkaApplication
import xyz.jdubiel.migawka.data.LocalImageDataStoreKeys
import xyz.jdubiel.migawka.data.UserSettingsRepository

class SettingsScreenViewModel(
    private val userSettingsRepository: UserSettingsRepository,
    private val localImageProviderDataStore: DataStore<Preferences>
) : ViewModel() {

    private val _settingsModified = MutableStateFlow(false)
    val settingsModified: StateFlow<Boolean> = _settingsModified.asStateFlow()

    private val _databaseCleared = MutableStateFlow(false)
    val databaseCleared: StateFlow<Boolean> = _databaseCleared

    // stateIn call converts (cold) Flow to (hot) StateFlow, so it's immediately available
    // about the started parameter:
    // the `WhileSubscribed(5_000)` passed to started parameter means that when
    // the current activity is stopped, the upstream flows will be stopped after
    // 5 more seconds, providing a smooth transition: they will be stopped if
    // the user goes to the home screen or another app, but they will not be
    // stopped during a device rotation (as the activity will be restarted with
    // onResume in less than 5 seconds)
    // https://medium.com/androiddevelopers/migrating-from-livedata-to-kotlins-flow-379292f419fb
    // https://www.youtube.com/watch?v=fSB6_KE95bU
    val serverAddress: StateFlow<String> = userSettingsRepository.serverAddress.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ""
    )
    val serverPort: StateFlow<Int> = userSettingsRepository.serverPort.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 0
    )

    val authToken: StateFlow<String> = userSettingsRepository.authToken.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ""
    )

    fun setServerAddress(serverAddress: String) {
        viewModelScope.launch {
            userSettingsRepository.setServerAddress(serverAddress)
        }
        _settingsModified.value = true
    }

    fun setServerPort(port: Int) {
        viewModelScope.launch {
            userSettingsRepository.setServerPort(port)
        }
        _settingsModified.value = true
    }

    fun setAuthToken(token: String) {
        viewModelScope.launch {
            userSettingsRepository.setAuthToken(token)
        }
        _settingsModified.value = true
    }


    fun clearLocalMediaDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            localImageProviderDataStore.edit { preferences ->
                preferences.remove(LocalImageDataStoreKeys.LAST_MODIFIED_GENERATION)
                preferences.remove(LocalImageDataStoreKeys.DB_MEDIA_STORE_VERSION)
            }
        }
        _databaseCleared.value = true
        _settingsModified.value = true
    }

    companion object {
        // Factory for creating SettingsScreenViewModel. It uses the userSettingsRepository that
        // has been injected into MigawkaApplication.
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as MigawkaApplication)
                val settingsRepository = application.userSettingsRepository
                val localImageProviderDataStore = application.localImageProviderDataStore_
                SettingsScreenViewModel(settingsRepository, localImageProviderDataStore)
            }
        }
    }
}