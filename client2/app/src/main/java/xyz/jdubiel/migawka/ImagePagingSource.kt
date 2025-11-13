package xyz.jdubiel.migawka

import android.content.ContentResolver
import android.content.ContentUris
import android.provider.MediaStore
import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.grpc.ManagedChannelBuilder
import java.time.Instant

class ImagePagingSource(
    private val contentResolver: ContentResolver
) : PagingSource<Instant, PagedImage>() {

    // loads the next combined page of photos from local and remote storage
    // the photos are taken before the datetime (given as key), not inclusive
    override suspend fun load(params: LoadParams<Instant>): LoadResult<Instant, PagedImage> {
        Log.d(TAG, "load with params: loadSize = ${params.loadSize}, key = ${params.key}")

        return try {
            val key = params.key
            val pageSize = params.loadSize
            val localImages = mutableListOf<PagedImage>()

            // TODO: query local and remote simultaneously
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
                        val date = Instant.ofEpochSecond(cursor.getLong(dateColumn))
                        if (date < key) {
                            cursor.moveToPrevious() // Move back to start processing from this item
                            break
                        }
                    }
                }

                // cursor is positioned _before_ the first element that interests us

                while (cursor.moveToNext() && localImages.size < pageSize) {
                    val id = cursor.getLong(idColumn)
                    val date = Instant.ofEpochSecond(cursor.getLong(dateColumn))
                    val contentUri =
                        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    localImages.add(PagedImage.FromUri(contentUri, date))
                }
            }


            val remoteImages = mutableListOf<PagedImage>()

            // TODO: query the remote server
            val serverAddress = "192.168.5.158"
            // TODO: channel should be reused probably
            val channel = ManagedChannelBuilder.forAddress(serverAddress, 50051)
                .usePlaintext() // TODO: don't use plaintext!
                .build()

            val stub = GreeterGrpcKt.GreeterCoroutineStub(channel)

            try {
                val dateForRequest = key?.toString() ?: Instant.now().toString()
                Log.d(TAG, "dateForRequest is `${dateForRequest}`")
                val request = ThumbnailsTimestampRequest.newBuilder()
                    .setTimestamp(dateForRequest)
                    .setCount(pageSize)
                    .build()

                val response = stub.getThumbnailsBeforeTimestamp(request)

                // Update the UI with the response on the main thread
                Log.i(
                    "gRPC",
                    "Response: ${response.status}"
                )

                response.thumbnailsList.forEach({
                    val date = Instant.parse(it.creationTime)
                    Log.i("gRPC", "Thumbnail: ${it.creationTime} $date ${it.id}")

                    remoteImages.add(PagedImage.FromBytes(
                        it.content.toByteArray(),
                        date))
                })

            } catch (e: Exception) {
                Log.e("gRPC", "Error: ${e.message}", e)
            }

            // combine local and remote results
            val combinedImages = mutableListOf<PagedImage>()

            // TODO: filter out remote images that we have locally

            var localImagesIndex = 0
            var remoteImagesIndex = 0
            while (combinedImages.size < pageSize && (localImagesIndex < localImages.size || remoteImagesIndex < remoteImages.size)) {
                if (localImagesIndex < localImages.size && remoteImagesIndex < remoteImages.size) {
                    if (localImages[localImagesIndex].date > remoteImages[remoteImagesIndex].date) {
                        combinedImages.add(localImages[localImagesIndex++])
                    } else {
                        combinedImages.add(remoteImages[remoteImagesIndex++])
                    }
                } else if (localImagesIndex < localImages.size) {
                    combinedImages.add(localImages[localImagesIndex++])
                } else if (remoteImagesIndex < remoteImages.size) {
                    combinedImages.add(remoteImages[remoteImagesIndex++])
                }
            }

            // if there are multiple photos with the exact same timestamp at the end, return all of
            // them so that the next load can include only photos after this date
            while (localImagesIndex < localImages.size && combinedImages.last().date == localImages[localImagesIndex].date) {
                combinedImages.add(localImages[localImagesIndex++])
            }
            while (remoteImagesIndex < remoteImages.size && combinedImages.last().date == remoteImages[remoteImagesIndex].date) {
                combinedImages.add(remoteImages[remoteImagesIndex++])
            }

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
    override fun getRefreshKey(state: PagingState<Instant, PagedImage>): Instant? {
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
