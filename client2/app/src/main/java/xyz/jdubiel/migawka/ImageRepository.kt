package xyz.jdubiel.migawka

import android.content.ContentResolver
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

class ImageRepository(private val contentResolver: ContentResolver) {

    fun getImageStream(): Flow<PagingData<PagedImage>> = Pager(
        config = PagingConfig(
            pageSize = 30,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { ImagePagingSource(contentResolver) }
    ).flow
}
