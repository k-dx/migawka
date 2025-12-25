package xyz.jdubiel.migawka.ui.imageGallery

import xyz.jdubiel.migawka.data.TimelineEntry

sealed interface ImageGalleryTimelineEntry {
    data class ImageItem(val entry: TimelineEntry) : ImageGalleryTimelineEntry
    data class Header(val monthYear: String) : ImageGalleryTimelineEntry
}