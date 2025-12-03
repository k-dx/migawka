package xyz.jdubiel.migawka.data

import android.content.ContentResolver
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import xyz.jdubiel.migawka.TAG

class ImageRepository(private val contentResolver: ContentResolver) {
    private val localImageProvider: LocalImageProvider = MediaStoreImageProvider(contentResolver)
    private val remoteImageProvider = RemoteImageProvider()

    suspend fun getImage(id: Hash): LocalImage {
        Log.d(TAG, "imageRepository, getImage")
        return localImageProvider.getImage(id)
    }

    fun getImageStream(): Flow<PagingData<PagedImage>> = Pager(
        config = PagingConfig(
            pageSize = 30,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { ImagePagingSource(localImageProvider, remoteImageProvider) }
    ).flow

    suspend fun getRemoteOptimizedImage(id: Hash): RemoteImage {
        Log.d(TAG, "imageRepository, getRemoteImage")
        return remoteImageProvider.getOptimizedImage(id)
    }

    suspend fun getRemoteFullImage(id: Hash): RemoteFullImage {
        return remoteImageProvider.getFullImage(id)
    }
}
