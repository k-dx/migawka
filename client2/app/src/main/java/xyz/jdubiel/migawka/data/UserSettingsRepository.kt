package xyz.jdubiel.migawka.data

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

// This is an interface so we can inject any implementation that adheres to this
// interface in MigawkaApplication (or tests)
interface UserSettingsRepository {
    val serverAddress: Flow<String>
    suspend fun setServerAddress(address: String)
}

/**
 * In-memory implementation of [UserSettingsRepository].
 */
class InMemoryUserSettingsRepository() : UserSettingsRepository {
    private var _serverAddress = MutableStateFlow("")
    override val serverAddress: Flow<String> = _serverAddress

    override suspend fun setServerAddress(address: String) {
        _serverAddress.value = address
    }
}

/**
 * Persistent implementation of [UserSettingsRepository] using DataStore.
 */
class PersistentUserSettingsRepository(
    private val dataStore: DataStore<Preferences>
) : UserSettingsRepository {

    private companion object {
        private val SERVER_ADDRESS_KEY = stringPreferencesKey("server_address")
        const val TAG = "UserSettingsRepository"
    }

    override val serverAddress: Flow<String> = dataStore.data
        .catch {
            if(it is IOException) {
                Log.e(TAG, "Error reading preferences.", it)
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            preferences[SERVER_ADDRESS_KEY] ?: ""
        }


    override suspend fun setServerAddress(address: String) {
        dataStore.edit { preferences ->
            preferences[SERVER_ADDRESS_KEY] = address
        }
    }
}
