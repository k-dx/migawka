package xyz.jdubiel.migawka.ui.imageGallery

import android.app.Application
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
import xyz.jdubiel.migawka.data.EntriesResult
import xyz.jdubiel.migawka.data.ImageRepository
import xyz.jdubiel.migawka.data.IndexingState
import xyz.jdubiel.migawka.data.TimelineEntryK
import xyz.jdubiel.migawka.data.UserSettingsRepository
import xyz.jdubiel.migawka.data.network.GrpcResult
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit


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

    val entriesResult: StateFlow<EntriesResult> = imageRepository.getEntries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = EntriesResult(entries = emptyList(), err = null)
        )

    val entries: StateFlow<List<TimelineEntryK>> = entriesResult.map { r ->
        r.entries.sortedByDescending { it.date }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val entriesWithHeaders: StateFlow<List<ImageGalleryTimelineEntry>> = entries.map { sortedEntries ->
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
        result
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val fetchErr: StateFlow<GrpcResult.Error?> = entriesResult.map { r -> r.err }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

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
