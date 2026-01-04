package xyz.jdubiel.migawka.ui.imageGallery

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.jdubiel.migawka.MigawkaApplication
import xyz.jdubiel.migawka.data.ImageRepository
import xyz.jdubiel.migawka.data.TimelineEntryK
import xyz.jdubiel.migawka.data.UserSettingsRepository
import xyz.jdubiel.migawka.data.network.GrpcResult
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit


class ImageGalleryViewModel(
    application: Application,
    private val imageRepository: ImageRepository,
    private val settingsRepository: UserSettingsRepository
) :
    AndroidViewModel(application) {

    val galleryColumnCount: StateFlow<Int> = settingsRepository.galleryColumnCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserSettingsRepository.DEFAULT_GALLERY_COLUMN_COUNT
    )

    private val _entriesWithHeaders = MutableStateFlow<List<ImageGalleryTimelineEntry>>(emptyList())
    val entriesWithHeaders: StateFlow<List<ImageGalleryTimelineEntry>> = _entriesWithHeaders.asStateFlow()

    private val _entries = MutableStateFlow<List<TimelineEntryK>>(emptyList())
    val entries: StateFlow<List<TimelineEntryK>> = _entries.asStateFlow()

    private val _fetchErr = MutableStateFlow<GrpcResult.Error?>(null)
    val fetchErr: StateFlow<GrpcResult.Error?> = _fetchErr.asStateFlow()

    init {
        viewModelScope.launch {
            val timeline = withContext(Dispatchers.IO) {
                Log.d("ImageGalleryViewModel", "loading entries")
                val (raw, err) = imageRepository.getEntries()
                _fetchErr.value = err
                Log.d("ImageGalleryViewModel", "entries loaded")

                // ensure desired order: newest first
                val sorted = raw.sortedByDescending { it.date }

                // set entries
                _entries.value = sorted

                // set entriesWithHeaders
                val result = mutableListOf<ImageGalleryTimelineEntry>()
                var lastMonthYear: Instant? = null
                for (entry in sorted) {
                    val monthYear = getStartOfMonth(entry.date)
                    if (monthYear != lastMonthYear) {
                        result.add(ImageGalleryTimelineEntry.Header(date = monthYear))
                        lastMonthYear = monthYear
                    }
                    result.add(ImageGalleryTimelineEntry.ImageItem(entry))
                }
                result
            }
            _entriesWithHeaders.value = timeline

            Log.d("ImageGalleryViewModel", "loaded ${timeline.size} entries")
        }
    }

    fun setGalleryColumnCount(count: Int) {
        viewModelScope.launch {
            settingsRepository.setGalleryColumnCount(count)
        }
    }
}

class ImageGalleryViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ImageGalleryViewModel::class.java)) {
            val settingsRepository = (application as MigawkaApplication).userSettingsRepository
            val imageRepository = application.imageRepository
            return ImageGalleryViewModel(application, imageRepository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

fun getStartOfMonth(x: Instant): Instant {
    return x.atOffset(ZoneOffset.UTC)
        .withDayOfMonth(1)
        .truncatedTo(ChronoUnit.DAYS) // Sets time to 00:00:00.000
        .toInstant()
}
