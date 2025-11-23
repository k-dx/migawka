package xyz.jdubiel.migawka.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.jdubiel.migawka.MigawkaApplication
import xyz.jdubiel.migawka.data.UserSettingsRepository

class SettingsScreenViewModel(
    private val userSettingsRepository: UserSettingsRepository
) : ViewModel() {

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

    fun setServerAddress(serverAddress: String) {
        viewModelScope.launch {
            userSettingsRepository.setServerAddress(serverAddress)
        }
    }

    companion object {
        // Factory for creating SettingsScreenViewModel. It uses the userSettingsRepository that
        // has been injected into MigawkaApplication.
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as MigawkaApplication)
                SettingsScreenViewModel(application.userSettingsRepository)
            }
        }
    }
}