package xyz.jdubiel.migawka

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.time.Instant

class ImagePagingSource(
    private val localImageProvider: LocalImageProvider,
    private val remoteImageProvider: RemoteImageProvider
) : PagingSource<Instant, PagedImage>() {

    // loads the next combined page of photos from local and remote storage
    // the photos are taken before the datetime (given as key), not inclusive
    override suspend fun load(params: LoadParams<Instant>): LoadResult<Instant, PagedImage> =
        withContext(Dispatchers.IO) {
            Log.d(TAG, "load with params: loadSize = ${params.loadSize}, key = ${params.key}")
            val start = System.nanoTime()

            val key = params.key
            val pageSize = params.loadSize

            // query both APIs. `async` is used to run both queries in parallel
            // query the local MediaStoreAPI
            val localImagesResult = async {
                localImageProvider.getImages(pageSize, key)
            }

            // query the remote API
            val dateForRequest = key ?: Instant.now()
            Log.d(TAG, "dateForRequest is `${dateForRequest}`")
            val remoteImagesResult = async {
                remoteImageProvider.getThumbnailsBeforeTimestamp(dateForRequest, pageSize)
            }

            val localImages = localImagesResult.await()
                .map { PagedImage.FromUri(it.sha256, it.contentUri, it.date) }
            val remoteImages = remoteImagesResult.await()
                .map { PagedImage.FromBytes(id = it.sha256, bytes = it.bytes, date = it.date) }

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

            val end = System.nanoTime()
            Log.d("time", "ImagePagingSource took ${(end - start)/1000000}ms")

            LoadResult.Page(
                data = combinedImages,
                prevKey = null, // we only page backward in time; no "prev" (newer) pages from
                // this source. This would be only possible if Paging3 allows
                // separate functions for loading the previous/next page
                // (TODO: check if it's possible)
                nextKey = nextKey
            )
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
