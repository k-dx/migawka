package xyz.jdubiel.migawka

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import xyz.jdubiel.migawka.data.Hasher
import xyz.jdubiel.migawka.data.ImageRepository
import xyz.jdubiel.migawka.data.PersistentUserSettingsRepository
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

    override fun onCreate() {
        super.onCreate()
//        userSettingsRepository = InMemoryUserSettingsRepository()
        userSettingsRepository = PersistentUserSettingsRepository(dataStore)
        imageRepository = ImageRepository(this.contentResolver)
    }
}