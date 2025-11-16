package xyz.jdubiel.migawka

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ImageGalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val imageRepository = ImageRepository(application.contentResolver)

    // This is a Flow of PagingData<Uri>> provided by ImageGalleryViewModel. The
    // Paging library is responsible for creating this stream, fetching data
    // from data source (like the device's local storage) in small chunks called
    // pages.
    val imageStream: Flow<PagingData<PagedImage>> = imageRepository.getImageStream()
        .cachedIn(viewModelScope)

    suspend fun getRemoteImage(id: Sha256) = withContext(Dispatchers.IO) {
        imageRepository.getRemoteImage(id)
    }

}
