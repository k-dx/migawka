package xyz.jdubiel.migawka.data

import android.net.Uri
import java.time.Instant

// Data class to hold processed local image information
data class LocalImage(
    val contentUri: Uri,
    val date: Instant,
    val hash: Hash
)

// Interface for our provider
interface LocalImageProvider {
    /**
     * Fetches a list of local images from the MediaStore.
     *
     * @param limit The maximum number of images to fetch.
     * @param imagesBefore Optional timestamp to fetch images created before this time.
     * @return A list of [LocalImage] objects.
     */
    suspend fun getImages(limit: Int, imagesBefore: Instant?): List<LocalImage>

    suspend fun getImage(id: Hash): LocalImage

    // TODO: add getThumbnailsBeforeTimestamp, use Android's thumbnails for this?
}