package xyz.jdubiel.migawka

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.paging.PagingSource
import androidx.paging.PagingState

class ImagePagingSource(
    private val contentResolver: ContentResolver
) : PagingSource<Int, Uri>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Uri> {
        return try {
            val pageNumber = params.key ?: 0
            val pageSize = params.loadSize
            val images = mutableListOf<Uri>()

            val query = contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID),
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                cursor.moveToPosition(pageNumber * pageSize)
                while (cursor.moveToNext() && images.size < pageSize) {
                    val id = cursor.getLong(idColumn)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    images.add(contentUri)
                }
            }

            val nextKey = if (images.size < pageSize) null else pageNumber + 1
            val prevKey = if (pageNumber == 0) null else pageNumber - 1

            LoadResult.Page(
                data = images,
                prevKey = prevKey,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Uri>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
