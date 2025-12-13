package xyz.jdubiel.migawka.ui.singleMedia

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import xyz.jdubiel.migawka.data.Hash
import xyz.jdubiel.migawka.data.PagedImage
import xyz.jdubiel.migawka.data.RemoteImage

interface SingleMediaViewModelI {
    val imageStream: Flow<PagingData<PagedImage>>
    suspend fun getRemoteOptimizedImage(
        id: Hash
    ): RemoteImage
    fun downloadImage(
        id: Hash
    ): Unit
}