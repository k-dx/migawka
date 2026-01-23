package xyz.jdubiel.migawka.data

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
import java.time.LocalTime
import kotlin.coroutines.cancellation.CancellationException

// This is an interface so we can inject any implementation that adheres to this
// interface in MigawkaApplication (or tests)
/**
 * serverAddress and port assumes default value if not set.
 */
interface UserSettingsRepository {
    val serverAddress: Flow<String>
    val serverPort: Flow<Int>
    val authToken: Flow<String>
    val isTLSDisabled: Flow<Boolean>
    val galleryColumnCount: Flow<UInt>
    val showOverlayIcons: Flow<Boolean>
    val syncEnabled: Flow<Boolean>
    val syncTime: Flow<LocalTime>
    val syncOverUnmeteredOnly: Flow<Boolean>
    val syncWhenChargingOnly: Flow<Boolean>

    suspend fun setServerAddress(address: String)
    suspend fun setServerPort(port: Int)
    suspend fun setAuthToken(token: String)
    suspend fun setTLSDisabled(isTLSDisabled: Boolean)
    suspend fun setGalleryColumnCount(count: UInt)
    suspend fun setShowOverlayIcons(show: Boolean)

    suspend fun getServerAddress(timeoutMs: Long = 5_000L): String
    suspend fun getServerPort(timeoutMs: Long = 5_000L): Int
    suspend fun getAuthToken(timeoutMs: Long = 5_000L): String
    suspend fun isTLSDisabled(timeoutMs: Long = 5_000L): Boolean
    suspend fun getGalleryColumnCount(timeoutMs: Long = 5_000L): UInt
    suspend fun getShowOverlayIcons(timeoutMs: Long = 5_000L): Boolean
    suspend fun setSyncEnabled(sync: Boolean)
    suspend fun setSyncTime(time: LocalTime)
    suspend fun setSyncOverUnmeteredOnly(syncOverUnmeteredOnly: Boolean)
    suspend fun setSyncWhenChargingOnly(syncWhenChargingOnly: Boolean)


    companion object {
        const val DEFAULT_SERVER_ADDRESS = "127.0.0.1"
        const val DEFAULT_SERVER_PORT = 50051
        const val DEFAULT_GALLERY_COLUMN_COUNT = 3u
        const val DEFAULT_SHOW_OVERLAY_ICONS = true
        const val DEFAULT_AUTH_TOKEN = ""
        const val DEFAULT_SYNC_ENABLED = false
        val DEFAULT_SYNC_TIME = LocalTime.of(2, 0)
        const val DEFAULT_SYNC_OVER_UNMETERED_ONLY = true
        const val DEFAULT_SYNC_WHEN_CHARGING_ONLY = true
        const val DEFAULT_TLS_DISABLED = false
    }
}

/**
 * In-memory implementation of [UserSettingsRepository].
 */
class InMemoryUserSettingsRepository : UserSettingsRepository {
    private val _serverAddress = MutableStateFlow(UserSettingsRepository.DEFAULT_SERVER_ADDRESS)
    private val _serverPort = MutableStateFlow(UserSettingsRepository.DEFAULT_SERVER_PORT)
    private val _authToken = MutableStateFlow<String>("")
    private val _tlsDisabled = MutableStateFlow(UserSettingsRepository.DEFAULT_TLS_DISABLED)
    private val _galleryColumnCount =
        MutableStateFlow<UInt>(UserSettingsRepository.DEFAULT_GALLERY_COLUMN_COUNT)
    private val _showOverlayIcons =
        MutableStateFlow(UserSettingsRepository.DEFAULT_SHOW_OVERLAY_ICONS)
    private val _syncEnabled =
        MutableStateFlow(UserSettingsRepository.DEFAULT_SYNC_ENABLED)
    private val _syncTime =
        MutableStateFlow(UserSettingsRepository.DEFAULT_SYNC_TIME)
    private val _syncOverUnmeteredOnly =
        MutableStateFlow(UserSettingsRepository.DEFAULT_SYNC_OVER_UNMETERED_ONLY)
    private val _syncWhenChargingOnly =
        MutableStateFlow(UserSettingsRepository.DEFAULT_SYNC_WHEN_CHARGING_ONLY)

    override val serverAddress: Flow<String> = _serverAddress
    override val serverPort: Flow<Int> = _serverPort
    override val authToken: Flow<String> = _authToken
    override val isTLSDisabled: Flow<Boolean> = _tlsDisabled
    override val galleryColumnCount: Flow<UInt> = _galleryColumnCount
    override val showOverlayIcons: Flow<Boolean> = _showOverlayIcons
    override val syncEnabled: Flow<Boolean> = _syncEnabled
    override val syncTime: Flow<LocalTime> = _syncTime
    override val syncOverUnmeteredOnly: Flow<Boolean> = _syncOverUnmeteredOnly
    override val syncWhenChargingOnly: Flow<Boolean> = _syncWhenChargingOnly

    override suspend fun setServerAddress(address: String) {
        _serverAddress.value = address
    }

    override suspend fun setServerPort(port: Int) {
        _serverPort.value = port
    }

    override suspend fun setAuthToken(token: String) {
        _authToken.value = token
    }

    override suspend fun setTLSDisabled(isTLSDisabled: Boolean) {
        _tlsDisabled.value = isTLSDisabled
    }

    override suspend fun setGalleryColumnCount(count: UInt) {
        _galleryColumnCount.value = count
    }

    override suspend fun setShowOverlayIcons(show: Boolean) {
        _showOverlayIcons.value = show
    }

    override suspend fun setSyncEnabled(sync: Boolean) {
        _syncEnabled.value = sync
    }

    override suspend fun setSyncTime(time: LocalTime) {
        _syncTime.value = time
    }

    override suspend fun setSyncOverUnmeteredOnly(syncOverUnmeteredOnly: Boolean) {
        _syncOverUnmeteredOnly.value = syncOverUnmeteredOnly
    }

    override suspend fun setSyncWhenChargingOnly(syncWhenChargingOnly: Boolean) {
        _syncWhenChargingOnly.value = syncWhenChargingOnly
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

    override suspend fun getAuthToken(timeoutMs: Long): String {
        return try {
            withTimeout(timeoutMs) { _serverAddress.first() }
        } catch (e: TimeoutCancellationException) {
            // return current value (fallback) or default; choose default here
            UserSettingsRepository.DEFAULT_AUTH_TOKEN
        }
    }

    override suspend fun isTLSDisabled(timeoutMs: Long): Boolean {
        return try {
            withTimeout(timeoutMs) { _tlsDisabled.first() }
        } catch (e: TimeoutCancellationException) {
            // return current value (fallback) or default; choose default here
            UserSettingsRepository.DEFAULT_TLS_DISABLED
        }
    }

    override suspend fun getGalleryColumnCount(timeoutMs: Long): UInt {
        return try {
            withTimeout(timeoutMs) { _galleryColumnCount.first() }
        } catch (e: TimeoutCancellationException) {
            UserSettingsRepository.DEFAULT_GALLERY_COLUMN_COUNT
        }
    }

    override suspend fun getShowOverlayIcons(timeoutMs: Long): Boolean {
        return try {
            withTimeout(timeoutMs) { _showOverlayIcons.first() }
        } catch (e: TimeoutCancellationException) {
            UserSettingsRepository.DEFAULT_SHOW_OVERLAY_ICONS
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
        private val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
        private val TLS_DISABLED_KEY = booleanPreferencesKey("tls_disabled")
        private val GALLERY_COLUMN_COUNT_KEY = intPreferencesKey("gallery_column_count")
        private val SHOW_OVERLAY_ICONS_KEY = booleanPreferencesKey("show_overlay_icons")
        private val SYNC_ENABLED_KEY = booleanPreferencesKey("sync_enabled")
        private val SYNC_TIME_KEY = stringPreferencesKey("sync_time")
        private val SYNC_OVER_UNMETERED_ONLY_KEY = booleanPreferencesKey("sync_over_unmetered_only")
        private val SYNC_WHEN_CHARGING_ONLY_KEY = booleanPreferencesKey("sync_when_charging_only")
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

    override val authToken: Flow<String> = dataStore.data
        .catch {
            if (it is IOException) {
                Log.e(TAG, "Error reading preferences.", it)
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            preferences[AUTH_TOKEN_KEY] ?: UserSettingsRepository.DEFAULT_AUTH_TOKEN
        }

    override val isTLSDisabled: Flow<Boolean> = dataStore.data
        .catch {
            if (it is IOException) {
                Log.e(TAG, "Error reading preferences.", it)
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            preferences[TLS_DISABLED_KEY]
                ?: UserSettingsRepository.DEFAULT_TLS_DISABLED
        }

    override val galleryColumnCount: Flow<UInt> = dataStore.data
        .catch {
            if (it is IOException) {
                Log.e(TAG, "Error reading preferences.", it)
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            preferences[GALLERY_COLUMN_COUNT_KEY]?.toUInt()
                ?: UserSettingsRepository.DEFAULT_GALLERY_COLUMN_COUNT
        }

    override val showOverlayIcons: Flow<Boolean> = dataStore.data
        .catch {
            if (it is IOException) {
                Log.e(TAG, "Error reading preferences.", it)
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            preferences[SHOW_OVERLAY_ICONS_KEY]
                ?: UserSettingsRepository.DEFAULT_SHOW_OVERLAY_ICONS
        }

    override val syncEnabled: Flow<Boolean> = dataStore.data
        .catch {
            if (it is IOException) {
                Log.e(TAG, "Error reading preferences.", it)
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            preferences[SYNC_ENABLED_KEY]
                ?: UserSettingsRepository.DEFAULT_SYNC_ENABLED
        }

    override val syncTime: Flow<LocalTime> = dataStore.data
        .catch {
            if (it is IOException) {
                Log.e(TAG, "Error reading preferences.", it)
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            val s = preferences[SYNC_TIME_KEY]
            if (s != null) {
                LocalTime.parse(s)
            } else {
                UserSettingsRepository.DEFAULT_SYNC_TIME
            }
        }

    override val syncOverUnmeteredOnly: Flow<Boolean> = dataStore.data
        .catch {
            if (it is IOException) {
                Log.e(TAG, "Error reading preferences.", it)
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            preferences[SYNC_OVER_UNMETERED_ONLY_KEY]
                ?: UserSettingsRepository.DEFAULT_SYNC_OVER_UNMETERED_ONLY
        }

    override val syncWhenChargingOnly: Flow<Boolean> = dataStore.data
        .catch {
            if (it is IOException) {
                Log.e(TAG, "Error reading preferences.", it)
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            preferences[SYNC_WHEN_CHARGING_ONLY_KEY]
                ?: UserSettingsRepository.DEFAULT_SYNC_WHEN_CHARGING_ONLY
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

    override suspend fun setAuthToken(token: String) {
        dataStore.edit { preferences ->
            preferences[AUTH_TOKEN_KEY] = token
        }
    }

    override suspend fun setTLSDisabled(isTLSDisabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[TLS_DISABLED_KEY] = isTLSDisabled
        }
    }

    override suspend fun setGalleryColumnCount(count: UInt) {
        dataStore.edit { preferences ->
            preferences[GALLERY_COLUMN_COUNT_KEY] = count.toInt()
        }
    }

    override suspend fun setShowOverlayIcons(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_OVERLAY_ICONS_KEY] = show
        }
    }

    override suspend fun setSyncEnabled(sync: Boolean) {
        dataStore.edit { preferences ->
            preferences[SYNC_ENABLED_KEY] = sync
        }
    }

    override suspend fun setSyncTime(time: LocalTime) {
        dataStore.edit { preferences ->
            preferences[SYNC_TIME_KEY] = time.toString()
        }
    }

    override suspend fun setSyncOverUnmeteredOnly(syncOverUnmeteredOnly: Boolean) {
        dataStore.edit { preferences ->
            preferences[SYNC_OVER_UNMETERED_ONLY_KEY] = syncOverUnmeteredOnly
        }
    }

    override suspend fun setSyncWhenChargingOnly(syncWhenChargingOnly: Boolean) {
        dataStore.edit { preferences ->
            preferences[SYNC_WHEN_CHARGING_ONLY_KEY] = syncWhenChargingOnly
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

    override suspend fun getAuthToken(timeoutMs: Long): String {
        val result = withTimeoutOrNull(timeoutMs) {
            authToken.first()
        }
        if (result == null) {
            throw CancellationException("Timeout getting auth token after $timeoutMs ms")
        }
        return result
    }

    override suspend fun isTLSDisabled(timeoutMs: Long): Boolean {
        val result = withTimeoutOrNull(timeoutMs) {
            isTLSDisabled.first()
        }
        if (result == null) {
            throw CancellationException("Timeout getting auth token after $timeoutMs ms")
        }
        return result
    }

    override suspend fun getGalleryColumnCount(timeoutMs: Long): UInt {
        val result = withTimeoutOrNull(timeoutMs) {
            galleryColumnCount.first()
        }
        if (result == null) {
            throw CancellationException("Timeout getting gallery column count after $timeoutMs ms")
        }
        return result
    }

    override suspend fun getShowOverlayIcons(timeoutMs: Long): Boolean {
        val result = withTimeoutOrNull(timeoutMs) {
            showOverlayIcons.first()
        }
        if (result == null) {
            throw CancellationException("Timeout getting show overlay icons after $timeoutMs ms")
        }
        return result
    }
}
