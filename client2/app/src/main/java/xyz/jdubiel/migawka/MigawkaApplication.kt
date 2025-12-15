package xyz.jdubiel.migawka

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.runBlocking
import xyz.jdubiel.migawka.data.Hasher
import xyz.jdubiel.migawka.data.IPEndpoint
import xyz.jdubiel.migawka.data.ImageRepository
import xyz.jdubiel.migawka.data.PersistentUserSettingsRepository
import xyz.jdubiel.migawka.data.RemoteFileExplorer
import xyz.jdubiel.migawka.data.UserSettingsRepository
import xyz.jdubiel.migawka.data.Xx64Hasher

// DataStore setup
private const val SETTINGS_PREFERENCE_NAME = "settings_prefs"
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = SETTINGS_PREFERENCE_NAME
)

val hasher: Hasher = Xx64Hasher()

class MigawkaApplication : Application() {
    lateinit var userSettingsRepository: UserSettingsRepository
    lateinit var imageRepository: ImageRepository
    lateinit var remoteFileExplorer: RemoteFileExplorer

    override fun onCreate() {
        super.onCreate()
//        userSettingsRepository = InMemoryUserSettingsRepository()
        userSettingsRepository = PersistentUserSettingsRepository(dataStore)

        val serverAddress = runBlocking {
            userSettingsRepository.getServerAddress()
        }
        val endpoint = IPEndpoint(serverAddress, 50051)

        Log.d("gRPC", "server address is $serverAddress")
        imageRepository = ImageRepository(
            this.contentResolver,
            endpoint
        )

        remoteFileExplorer = RemoteFileExplorer(endpoint)
    }

    override fun onTerminate() {
        super.onTerminate()
        // TODO: Gracefully shutdown the gRPC channel
//        if (::remoteFileExplorer.isInitialized) {
//            remoteFileExplorer.shutdown()
//        }
    }
}