package xyz.jdubiel.migawka.ui.singleMedia

import kotlinx.coroutines.flow.StateFlow
import xyz.jdubiel.migawka.data.Hash
import xyz.jdubiel.migawka.data.RemoteImage
import xyz.jdubiel.migawka.data.TimelineEntryK

interface SingleMediaViewModelForTimelineI {
    val entries: StateFlow<List<TimelineEntryK>>
    suspend fun getRemoteOptimizedImage(
        id: Hash
    ): RemoteImage
    fun downloadImage(
        id: Hash
    ): Unit
}