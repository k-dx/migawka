package xyz.jdubiel.migawka

import android.content.ContentResolver
import androidx.paging.Pager
import androidx.paging.PagingConfig

class ImageRepository(private val contentResolver: ContentResolver) {

    fun getImageStream() = Pager(
        config = PagingConfig(
            pageSize = 30,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { ImagePagingSource(contentResolver) }
    ).flow
}
