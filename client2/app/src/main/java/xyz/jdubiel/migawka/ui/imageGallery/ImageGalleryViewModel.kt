package xyz.jdubiel.migawka.ui.imageGallery

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.jdubiel.migawka.MigawkaApplication
import xyz.jdubiel.migawka.data.ImageRepository
import xyz.jdubiel.migawka.data.IndexingState
import xyz.jdubiel.migawka.data.TimelineEntryK
import xyz.jdubiel.migawka.data.UserSettingsRepository
import xyz.jdubiel.migawka.data.network.GrpcResult
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

data class GalleryUiState(
    val entries: List<TimelineEntryK> = emptyList(),
    val entriesWithHeaders: List<ImageGalleryTimelineEntry> = emptyList(),
    val error: GrpcResult.Error? = null
)

class ImageGalleryViewModel(
    application: Application,
    imageRepository: ImageRepository,
    private val settingsRepository: UserSettingsRepository
) :
    AndroidViewModel(application) {

    val localMediaIndexingState: StateFlow<IndexingState> = imageRepository.localMediaIndexingState

    val galleryColumnCount: StateFlow<UInt> = settingsRepository.galleryColumnCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserSettingsRepository.DEFAULT_GALLERY_COLUMN_COUNT
    )
    val showOverlayIcons: StateFlow<Boolean> = settingsRepository.showOverlayIcons.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserSettingsRepository.DEFAULT_SHOW_OVERLAY_ICONS
    )

    // Using a single StateFlow here so that .collectAsStateWithLifecycle() in composable
    // can correctly unsubscribe when app goes to background for more than `stopTimeoutMillis`
    // milliseconds, so it will trigger a refresh if the user comes back.
    val uiState: StateFlow<GalleryUiState> = imageRepository.getEntries()
        .map { result ->
            val sortedEntries = result.entries.sortedByDescending { it.date }
            val headers = transformToHeaders(sortedEntries)
            GalleryUiState(entries = sortedEntries, entriesWithHeaders = headers, error = result.err)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(2_000),
            initialValue = GalleryUiState()
        )
        .also { Log.d("refresh", "uiState with entries refreshed")}

    private fun transformToHeaders(sortedEntries: List<TimelineEntryK>): List<ImageGalleryTimelineEntry> {
        val result = mutableListOf<ImageGalleryTimelineEntry>()
        var lastMonthYear: Instant? = null
        for (entry in sortedEntries) {
            val monthYear = getStartOfMonth(entry.date)
            if (monthYear != lastMonthYear) {
                result.add(ImageGalleryTimelineEntry.Header(date = monthYear))
                lastMonthYear = monthYear
            }
            result.add(ImageGalleryTimelineEntry.ImageItem(entry))
        }
        return result
    }

    fun setGalleryColumnCount(count: UInt) {
        viewModelScope.launch {
            settingsRepository.setGalleryColumnCount(count)
        }
    }

    fun setShowOverlayIcons(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowOverlayIcons(show)
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
