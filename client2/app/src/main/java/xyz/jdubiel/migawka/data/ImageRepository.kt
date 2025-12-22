package xyz.jdubiel.migawka.data

import android.content.ContentResolver
import android.content.ContentValues
import android.provider.MediaStore
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import xyz.jdubiel.migawka.MigawkaGrpcKt
import xyz.jdubiel.migawka.TAG
import java.io.File

class ImageRepository(
    private val contentResolver: ContentResolver,
    grpcStub: MigawkaGrpcKt.MigawkaCoroutineStub,
    private val localImageProvider: LocalImageProvider,
) {
    private val remoteImageProvider = RemoteImageProvider(grpcStub)

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

    fun saveImageToGallery(img: RemoteFullImage) {
        val file = File(img.path)
        val filename = file.name
        val path = "Pictures/Migawka/${file.parent}"
        // Prepare ContentValues for MediaStore
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            // TODO: It would be best to set DATE_MODIFIED (ie the modified date on the file)
            // when downloading a photo that does not have EXIF metadata, as modified date
            // is the fallback on the server, but this doesn't seem to work - maybe needs
            // stronger permissions.
            // This is problematic since photos can "move" to the top of the timeline after
            // download.
            // put(MediaStore.Images.Media.DATE_MODIFIED, date_modified.epochSecond)
            put(MediaStore.Images.Media.RELATIVE_PATH, path)
            put(MediaStore.Images.Media.IS_PENDING, true)
        }

        // Insert to MediaStore
        val uri = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
        ) ?: throw Exception("Failed to insert image into MediaStore")

        // Write bytes to the URI
        contentResolver.openOutputStream(uri)?.use { output ->
            output.write(img.bytes)
        }

        // Mark as complete
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, false)
        contentResolver.update(uri, values, null, null)
    }
}
