package xyz.jdubiel.migawka.ui.imageGallery

import android.app.Application
import android.content.ContentValues
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.jdubiel.migawka.MigawkaApplication
import xyz.jdubiel.migawka.TAG
import xyz.jdubiel.migawka.data.Hash
import xyz.jdubiel.migawka.data.ImageRepository
import xyz.jdubiel.migawka.data.PagedImage
import xyz.jdubiel.migawka.ui.singleMedia.SingleMediaViewModelI
import java.io.File

class ImageGalleryViewModel(
    application: Application,
    private val imageRepository: ImageRepository
) :
    AndroidViewModel(application), SingleMediaViewModelI {

    // This is a Flow of PagingData<Uri>> provided by ImageGalleryViewModel. The
    // Paging library is responsible for creating this stream, fetching data
    // from data source (like the device's local storage) in small chunks called
    // pages.
    override val imageStream: Flow<PagingData<PagedImage>> = imageRepository.getImageStream()
        .cachedIn(viewModelScope)

    override suspend fun getRemoteOptimizedImage(id: Hash) = withContext(Dispatchers.IO) {
        imageRepository.getRemoteOptimizedImage(id)
    }

    override fun downloadImage(id: Hash) {
        viewModelScope.launch(Dispatchers.IO) { // TODO: change the scope
            try {
                val img = imageRepository.getRemoteFullImage(id)

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
                val uri = application.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                ) ?: return@launch

                // Write bytes to the URI
                application.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(img.bytes)
                }

                // Mark as complete
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, false)
                application.contentResolver.update(uri, values, null, null)
            } catch (e: Exception) {
                Log.d(TAG, "error when downloading image: ${e.message}")
                // TODO: Display info about the error to the user
            }
        }
    }
}

class ImageGalleryViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ImageGalleryViewModel::class.java)) {
            val imageRepository = (application as MigawkaApplication).imageRepository
            return ImageGalleryViewModel(application, imageRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
