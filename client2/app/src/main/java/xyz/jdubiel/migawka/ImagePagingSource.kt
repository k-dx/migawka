package xyz.jdubiel.migawka

import android.content.ContentResolver
import android.content.ContentUris
import android.provider.MediaStore
import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import java.util.Date


class ImagePagingSource(
    private val contentResolver: ContentResolver
) : PagingSource<Date, PagedImage>() {

    // loads the next combined page of photos from local and remote storage
    // the photos are taken before the Date (given as key), not inclusive
    override suspend fun load(params: LoadParams<Date>): LoadResult<Date, PagedImage> {
        Log.d(TAG, "load with params: loadSize = ${params.loadSize}, key = ${params.key}")
        return try {
            val key = params.key
            val pageSize = params.loadSize
            val localImages = mutableListOf<PagedImage>()

            // query the local MediaStoreAPI
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_ADDED),
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC" // TODO: use EXIF data if possible
            )?.use { cursor ->
                // cursor is positioned _before_ the first row

                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                if (key != null) {
                    // TODO: optimize this
                    // Find the first image with a date <= key.date
                    // This is not very efficient, but MediaStore API doesn't seem to offer
                    // a better way to jump to a specific position based on a value.
                    // A possible optimization is to use a selection argument, but that would
                    // require more complex logic to handle paging.
                    while (cursor.moveToNext()) {
                        val date = Date(cursor.getLong(dateColumn) * 1000)
                        if (date.time < key.time) {
                            cursor.moveToPrevious() // Move back to start processing from this item
                            break
                        }
                    }
                }

                // cursor is positioned _before_ the first element that interests us

                while (cursor.moveToNext() && localImages.size < pageSize) {
                    val id = cursor.getLong(idColumn)
                    val date = Date(cursor.getLong(dateColumn) * 1000)
                    val contentUri =
                        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    localImages.add(PagedImage(contentUri, date))
                }
            }

            // TODO: query the remote server

            // TODO: combine local and remote results
            // if there are multiple photos with the exact same timestamp at the end, return all of them
            // so that the next load can include only photos after this date
            val combinedImages = localImages

            // TODO: nextKey should be the date of the last photo in combined
            val nextKey = if (combinedImages.size < pageSize) null else combinedImages.last().date

            LoadResult.Page(
                data = combinedImages,
                prevKey = null, // we only page backward in time; no "prev" (newer) pages from
                                // this source. This would be only possible if Paging3 allows
                                // separate functions for loading the previous/next page
                                // (TODO: check if it's possible)
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    // Anchor position handling for jump-to-position restore
    override fun getRefreshKey(state: PagingState<Date, PagedImage>): Date? {
        // TODO: implement this!
        return null
//        // Choose the closest page's nextBeforeTimestamp or the middle item's timestamp as refresh anchor.
//        val anchorPosition = state.anchorPosition ?: return null
//        val anchorPage = state.closestPageToPosition(anchorPosition) ?: return null
//
//        // Prefer a key from the page; if not available, use timestamp of the first item in that page
//        return anchorPage.nextKey ?: anchorPage.data.firstOrNull()?.date
    }
}
