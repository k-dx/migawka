package xyz.jdubiel.migawka

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
class ImageGalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val imageRepository = ImageRepository(application.contentResolver)

    // This is a Flow of PagingData<Uri>> provided by ImageGalleryViewModel. The
    // Paging library is responsible for creating this stream, fetching data
    // from data source (like the device's local storage) in small chunks called
    // pages.
    val imageStream: Flow<PagingData<PagedImage>> = imageRepository.getImageStream()
        .cachedIn(viewModelScope)

    private val _selectedImage = MutableStateFlow<LocalImage?>(null)
    val selectedImage: StateFlow<LocalImage?> = _selectedImage.asStateFlow()

    fun loadImageById(id: Sha256) {
        Log.d(TAG, "viewModel, loadImageById")
        viewModelScope.launch {
            // Set to null first to show a loading indicator
            _selectedImage.value = null
            // Fetch the image from the repository
            val imageDetails = imageRepository.getImage(id)
            _selectedImage.value = imageDetails
        }
    }
}
