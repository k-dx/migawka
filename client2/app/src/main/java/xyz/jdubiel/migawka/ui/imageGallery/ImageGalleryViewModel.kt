package xyz.jdubiel.migawka.ui.imageGallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import xyz.jdubiel.migawka.MigawkaApplication
import xyz.jdubiel.migawka.data.ImageRepository
import xyz.jdubiel.migawka.data.PagedImage
import xyz.jdubiel.migawka.data.Sha256

class ImageGalleryViewModel(
    private val imageRepository: ImageRepository
) : ViewModel() {

//    private val imageRepository = ImageRepository(application.contentResolver)

    // This is a Flow of PagingData<Uri>> provided by ImageGalleryViewModel. The
    // Paging library is responsible for creating this stream, fetching data
    // from data source (like the device's local storage) in small chunks called
    // pages.
    val imageStream: Flow<PagingData<PagedImage>> = imageRepository.getImageStream()
        .cachedIn(viewModelScope)

    suspend fun getRemoteImage(id: Sha256) = withContext(Dispatchers.IO) {
        imageRepository.getRemoteImage(id)
    }


    // Add the Companion Object Factory here
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                // Get a reference to the MigawkaApplication instance
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MigawkaApplication)
                // Get the localImageProvider that was already created in the Application class
                val localImageProvider = application.localImageProvider

                // Create the ImageRepository with its dependency
                val imageRepository = ImageRepository(localImageProvider)

                // Create and return the ImageGalleryViewModel
                ImageGalleryViewModel(imageRepository = imageRepository)
            }
        }
    }

}
