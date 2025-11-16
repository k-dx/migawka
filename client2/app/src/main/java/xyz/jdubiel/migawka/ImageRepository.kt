package xyz.jdubiel.migawka

import android.content.ContentResolver
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

class ImageRepository(private val contentResolver: ContentResolver) {
    private val localImageProvider: LocalImageProvider = MediaStoreImageProvider(contentResolver)
    private val remoteImageProvider = RemoteImageProvider()

    suspend fun getImage(id: Sha256): LocalImage {
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

    suspend fun getRemoteImage(id: Sha256): RemoteImage {
        Log.d(TAG, "imageRepository, getRemoteImage")
        return remoteImageProvider.getImage(id)
    }
}
