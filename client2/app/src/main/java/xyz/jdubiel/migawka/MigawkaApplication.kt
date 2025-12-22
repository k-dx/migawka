package xyz.jdubiel.migawka

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import xyz.jdubiel.migawka.data.Hasher
import xyz.jdubiel.migawka.data.ImageRepository
import xyz.jdubiel.migawka.data.LocalImageProvider
import xyz.jdubiel.migawka.data.MediaStoreImageProvider
import xyz.jdubiel.migawka.data.PersistentUserSettingsRepository
import xyz.jdubiel.migawka.data.RemoteFileExplorer
import xyz.jdubiel.migawka.data.UserSettingsRepository
import xyz.jdubiel.migawka.data.Xx64Hasher
import xyz.jdubiel.migawka.data.database.ILocalMediaRepository
import xyz.jdubiel.migawka.data.database.LocalMediaDatabase
import xyz.jdubiel.migawka.data.database.LocalMediaRepository
import xyz.jdubiel.migawka.data.network.GrpcProvider
import xyz.jdubiel.migawka.data.network.IPEndpoint

// DataStore setup
private const val SETTINGS_PREFERENCE_NAME = "settings_prefs"
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = SETTINGS_PREFERENCE_NAME
)
private const val LOCAL_IMAGE_PROVIDER_PREFERENCE_NAME = "local_image_provider"
private val Context.localImageProviderDataStore: DataStore<Preferences> by preferencesDataStore(
    name = LOCAL_IMAGE_PROVIDER_PREFERENCE_NAME
)

val hasher: Hasher = Xx64Hasher()

class MigawkaApplication : Application() {
    lateinit var userSettingsRepository: UserSettingsRepository
    lateinit var imageRepository: ImageRepository
    lateinit var remoteFileExplorer: RemoteFileExplorer
    lateinit var grpcProvider: GrpcProvider
    lateinit var localImageProvider: LocalImageProvider

    // SupervisorJob ensures a failure in one task doesn't cancel the whole scope
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
//        userSettingsRepository = InMemoryUserSettingsRepository()
        userSettingsRepository = PersistentUserSettingsRepository(settingsDataStore)

        val (serverAddress, serverPort) = runBlocking {
            val serverAddressDeferred = async { userSettingsRepository.getServerAddress() }
            val serverPortDeferred = async { userSettingsRepository.getServerPort() }

            val address = serverAddressDeferred.await()
            val port = serverPortDeferred.await()
            Pair(address, port)
        }
        val endpoint = IPEndpoint(serverAddress, serverPort)
        grpcProvider = GrpcProvider(endpoint)


        Log.d("gRPC", "server address is $serverAddress:$serverPort")

        val localMediaRepo: ILocalMediaRepository = LocalMediaRepository(
            LocalMediaDatabase.getDatabase(this).localMediaDao()
            )
        localImageProvider = MediaStoreImageProvider(
            this.applicationContext,
            contentResolver,
            localMediaRepo,
            applicationScope,
            localImageProviderDataStore
        )

        imageRepository = ImageRepository(
            this.contentResolver,
            grpcProvider.getMigawkaServiceStub(),
            localImageProvider
        )

        remoteFileExplorer = RemoteFileExplorer(grpcProvider.getMigawkaServiceStub())
    }

    override fun onTerminate() {
        super.onTerminate()

        grpcProvider.shutdown()
    }
}