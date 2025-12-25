package xyz.jdubiel.migawka.ui.imageGallery

import xyz.jdubiel.migawka.data.TimelineEntryK

sealed interface ImageGalleryTimelineEntry {
    data class ImageItem(val entry: TimelineEntryK) : ImageGalleryTimelineEntry
    data class Header(val monthYear: String) : ImageGalleryTimelineEntry
}