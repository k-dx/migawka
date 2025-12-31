package xyz.jdubiel.migawka.ui.imageGallery

import xyz.jdubiel.migawka.data.TimelineEntryK
import java.time.Instant

sealed interface ImageGalleryTimelineEntry {
    data class ImageItem(val entry: TimelineEntryK) : ImageGalleryTimelineEntry
    data class Header(val date: Instant) : ImageGalleryTimelineEntry
}