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
import xyz.jdubiel.migawka.ui.singleMedia.SingleMediaViewModelI
import java.time.format.DateTimeFormatter
import java.util.Locale



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

    private val _entries = MutableStateFlow<List<ImageGalleryTimelineEntry>>(emptyList())
    val entries: StateFlow<List<ImageGalleryTimelineEntry>> = _entries.asStateFlow()

    init {
        val locale = Locale.getDefault()
        val zone = java.time.ZoneId.systemDefault()
        val monthYearFormatter = DateTimeFormatter
            .ofPattern("LLLL uuuu") // LLLL gives non-conjugated month name 'listopad' instead of 'listopada'
            .withLocale(locale)
            .withZone(zone)

        viewModelScope.launch {
            val timeline = withContext(Dispatchers.IO) {
                val raw = imageRepository.getEntries()

                // ensure desired order: newest first
                val sorted = raw.sortedByDescending { it.date }

                val result = mutableListOf<ImageGalleryTimelineEntry>()
                var lastMonthYear: String? = null
                for (entry in sorted) {
                    val monthYear = monthYearFormatter.format(entry.date)
                    if (monthYear != lastMonthYear) {
                        result.add(ImageGalleryTimelineEntry.Header(monthYear))
                        lastMonthYear = monthYear
                    }
                    result.add(ImageGalleryTimelineEntry.ImageItem(entry))
                }
                result
            }
            _entries.value = timeline
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
