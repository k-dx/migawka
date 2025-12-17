package xyz.jdubiel.migawka.data

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException

// This is an interface so we can inject any implementation that adheres to this
// interface in MigawkaApplication (or tests)
/**
 * serverAddress and port assumes default value if not set.
 */
interface UserSettingsRepository {
    val serverAddress: Flow<String>
    val serverPort: Flow<Int>
    suspend fun setServerAddress(address: String)
    suspend fun setServerPort(port: Int)
    suspend fun getServerAddress(timeoutMs: Long = 5_000L): String
    suspend fun getServerPort(timeoutMs: Long = 5_000L): Int

    companion object {
        const val DEFAULT_SERVER_ADDRESS = "127.0.0.1"
        const val DEFAULT_SERVER_PORT = 50051
    }
}

/**
 * In-memory implementation of [UserSettingsRepository].
 */
class InMemoryUserSettingsRepository : UserSettingsRepository {
    private val _serverAddress = MutableStateFlow(UserSettingsRepository.DEFAULT_SERVER_ADDRESS)
    private val _serverPort = MutableStateFlow(UserSettingsRepository.DEFAULT_SERVER_PORT)

    override val serverAddress: Flow<String> = _serverAddress
    override val serverPort: Flow<Int> = _serverPort

    override suspend fun setServerAddress(address: String) {
        _serverAddress.value = address
    }

    override suspend fun setServerPort(port: Int) {
        _serverPort.value = port
    }

    override suspend fun getServerAddress(timeoutMs: Long): String {
        return try {
            withTimeout(timeoutMs) { _serverAddress.first() }
        } catch (e: TimeoutCancellationException) {
            // return current value (fallback) or default; choose default here
            UserSettingsRepository.DEFAULT_SERVER_ADDRESS
        }
    }

    override suspend fun getServerPort(timeoutMs: Long): Int {
        return try {
            withTimeout(timeoutMs) { _serverPort.first() }
        } catch (e: TimeoutCancellationException) {
            UserSettingsRepository.DEFAULT_SERVER_PORT
        }
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
        private val SERVER_PORT_KEY = intPreferencesKey("server_port")
        const val TAG = "UserSettingsRepository"
    }

    override val serverAddress: Flow<String> = dataStore.data
        .catch {
            if (it is IOException) {
                Log.e(TAG, "Error reading preferences.", it)
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            preferences[SERVER_ADDRESS_KEY] ?: UserSettingsRepository.DEFAULT_SERVER_ADDRESS
        }

    override val serverPort: Flow<Int> = dataStore.data
        .catch {
            if (it is IOException) {
                Log.e(TAG, "Error reading preferences.", it)
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            preferences[SERVER_PORT_KEY] ?: UserSettingsRepository.DEFAULT_SERVER_PORT
        }


    override suspend fun setServerAddress(address: String) {
        dataStore.edit { preferences ->
            preferences[SERVER_ADDRESS_KEY] = address
        }
    }

    override suspend fun setServerPort(port: Int) {
        dataStore.edit { preferences ->
            preferences[SERVER_PORT_KEY] = port
        }
    }

    /**
     * This is for only getting the server address once, it will not update like the Flow above.
     */
    override suspend fun getServerAddress(timeoutMs: Long): String {
        val result = withTimeoutOrNull(timeoutMs) {
            serverAddress.first()
        }
        if (result == null) {
            throw CancellationException("Timeout getting server address after $timeoutMs ms")
        }
        return result
    }

    override suspend fun getServerPort(timeoutMs: Long): Int {
        val result = withTimeoutOrNull(timeoutMs) {
            serverPort.first()
        }
        if (result == null) {
            throw CancellationException("Timeout getting server port after $timeoutMs ms")
        }
        return result
    }
}

