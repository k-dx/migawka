package xyz.jdubiel.migawka.ui.imageGallery

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.jdubiel.migawka.MigawkaApplication
import xyz.jdubiel.migawka.TAG
import xyz.jdubiel.migawka.data.Hash
import xyz.jdubiel.migawka.data.ImageRepository
import xyz.jdubiel.migawka.data.PagedImage
import xyz.jdubiel.migawka.data.TimelineEntry
import xyz.jdubiel.migawka.ui.singleMedia.SingleMediaViewModelI

class ImageGalleryViewModel(
    application: Application,
    private val imageRepository: ImageRepository
) :
    AndroidViewModel(application), SingleMediaViewModelI {

    // This is a Flow of PagingData<Uri>> provided by ImageGalleryViewModel. The
    // Paging library is responsible for creating this stream, fetching data
    // from data source (like the device's local storage) in small chunks called
    // pages.
    override val imageStream: Flow<PagingData<PagedImage>> = imageRepository.getImageStream()
        .cachedIn(viewModelScope)

    private val _entries = MutableStateFlow<List<TimelineEntry>>(emptyList())
    val entries: StateFlow<List<TimelineEntry>> = _entries.asStateFlow()

    init {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                imageRepository.getEntries()
            }
            _entries.value = result
        }
    }


    override suspend fun getRemoteOptimizedImage(id: Hash) = withContext(Dispatchers.IO) {
        imageRepository.getRemoteOptimizedImage(id)
    }

    override fun downloadImage(id: Hash) {
        viewModelScope.launch(Dispatchers.IO) { // TODO: change the scope
            try {
                val img = imageRepository.getRemoteFullImage(id)
                imageRepository.saveImageToGallery(img)
            } catch (e: Exception) {
                Log.d(TAG, "error when downloading image: ${e.message}")
                // TODO: Display info about the error to the user
            }
        }
    }
}

class ImageGalleryViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ImageGalleryViewModel::class.java)) {
            val imageRepository = (application as MigawkaApplication).imageRepository
            return ImageGalleryViewModel(application, imageRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
