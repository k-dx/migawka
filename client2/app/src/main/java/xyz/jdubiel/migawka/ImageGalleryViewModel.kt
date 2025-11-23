package xyz.jdubiel.migawka

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import xyz.jdubiel.migawka.data.UserSettingsRepository

class ImageGalleryViewModel(
    application: Application,
    private val userSettingsRepository: UserSettingsRepository
) : AndroidViewModel(application) {


    private val address = runBlocking {
        userSettingsRepository.getServerAddress()
    }

    private val imageRepository = ImageRepository(
        contentResolver = application.contentResolver,
        remoteEndpoint = IPEndpoint(address, 50051))

    // This is a Flow of PagingData<Uri>> provided by ImageGalleryViewModel. The
    // Paging library is responsible for creating this stream, fetching data
    // from data source (like the device's local storage) in small chunks called
    // pages.
    val imageStream: Flow<PagingData<PagedImage>> = imageRepository.getImageStream()
        .cachedIn(viewModelScope)

    suspend fun getRemoteImage(id: Sha256) = withContext(Dispatchers.IO) {
        imageRepository.getRemoteImage(id)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as Application)
                ImageGalleryViewModel(
                    application = application,
                    userSettingsRepository = (application as MigawkaApplication).userSettingsRepository
                )
            }
        }
    }
}
