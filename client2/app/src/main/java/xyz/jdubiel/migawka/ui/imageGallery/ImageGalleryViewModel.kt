package xyz.jdubiel.migawka.ui.imageGallery

import android.app.Application
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.jdubiel.migawka.data.Hash
import xyz.jdubiel.migawka.data.ImageRepository
import xyz.jdubiel.migawka.data.PagedImage

class ImageGalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val imageRepository = ImageRepository(application.contentResolver)

    // This is a Flow of PagingData<Uri>> provided by ImageGalleryViewModel. The
    // Paging library is responsible for creating this stream, fetching data
    // from data source (like the device's local storage) in small chunks called
    // pages.
    val imageStream: Flow<PagingData<PagedImage>> = imageRepository.getImageStream()
        .cachedIn(viewModelScope)

    suspend fun getRemoteOptimizedImage(id: Hash) = withContext(Dispatchers.IO) {
        imageRepository.getRemoteOptimizedImage(id)
    }

    fun downloadImage(id: Hash) {
        viewModelScope.launch(Dispatchers.IO) { // TODO: change the scope
            try {
                // Download image bytes
                val img = imageRepository.getRemoteFullImage(id)

                val filename = "${img.hash.toHex()}.jpg" // TODO
                val path = "Pictures/Migawka" // TODO
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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, path)
                        put(MediaStore.Images.Media.IS_PENDING, true)
                    }
                }

                // Insert to MediaStore
                val uri = application.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                ) ?: return@launch

                // Write bytes to the URI
                application.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(img.bytes)
                }

                // Mark as complete (Android 10+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, false)
                    application.contentResolver.update(uri, values, null, null)
                }

                // Optional: Notify UI on main thread
//                withContext(Dispatchers.Main) {
//                    // Show success toast or update UI
//                }
            } catch (e: Exception) {
                // TODO: Handle error
            }
        }
    }

}
