package xyz.jdubiel.migawka.data

import android.net.Uri
import xyz.jdubiel.migawka.data.network.GrpcResult
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
     * Fetches a list of local images from the MediaStore. Tries not to return invalid entries (e.g.
     * ones that have been deleted).
     *
     * @param count The maximum number of images to fetch.
     * @param imagesBefore Optional timestamp to fetch images created before this time.
     * @return A list of [LocalImage] objects.
     */
    suspend fun getImages(count: Int, imagesBefore: Instant): List<LocalImage>

    suspend fun getImage(id: Hash): LocalImage

    /**
     * Fetches a list of local images from the MediaStore.
     */
    suspend fun getEntries(): List<LocalImage>
    fun extractExifMetadata(uri: Uri): GrpcResult<Map<MediaMetadata, String>>
}