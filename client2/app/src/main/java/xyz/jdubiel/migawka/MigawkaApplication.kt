package xyz.jdubiel.migawka

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import xyz.jdubiel.migawka.data.LocalImageProvider
import xyz.jdubiel.migawka.data.MediaStoreImageProvider
import xyz.jdubiel.migawka.data.PersistentUserSettingsRepository
import xyz.jdubiel.migawka.data.UriToSha256Database
import xyz.jdubiel.migawka.data.UserSettingsRepository
import xyz.jdubiel.migawka.data.uriToSha256.UriToSha256Repository

// DataStore setup
private const val SETTINGS_PREFERENCE_NAME = "settings_prefs"
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = SETTINGS_PREFERENCE_NAME
)

class MigawkaApplication : Application() {
    lateinit var userSettingsRepository: UserSettingsRepository
    lateinit var uriToSha256Repository: UriToSha256Repository

    lateinit var localImageProvider: LocalImageProvider



    override fun onCreate() {
        super.onCreate()
//        userSettingsRepository = InMemoryUserSettingsRepository()
        userSettingsRepository = PersistentUserSettingsRepository(dataStore)
        uriToSha256Repository = UriToSha256Repository(
            UriToSha256Database.getDatabase(this).uriToSha256Dao()
        )
        localImageProvider = MediaStoreImageProvider(contentResolver, uriToSha256Repository)

    }
}